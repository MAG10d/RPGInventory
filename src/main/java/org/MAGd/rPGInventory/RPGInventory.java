package org.MAGd.rPGInventory;

import org.MAGd.rPGInventory.commands.RPGInventoryCommand;
import org.MAGd.rPGInventory.database.DatabaseFactory;
import org.MAGd.rPGInventory.database.DatabaseManager;
import org.MAGd.rPGInventory.gui.InventoryGUI;
import org.MAGd.rPGInventory.listeners.InventoryListeners;
import org.MAGd.rPGInventory.listeners.TotemEffectListener;
import org.MAGd.rPGInventory.listeners.OrnamentEffectListener;
import org.MAGd.rPGInventory.listeners.PlayerJoinListener;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class RPGInventory extends JavaPlugin {

    private static RPGInventory instance;
    private boolean hasExecutableItems = false;
    private DatabaseManager databaseManager;
    private BukkitTask autoSaveTask;
    private List<String> allowedTotems; // 允許的圖騰 ID 清單
    private TotemEffectListener totemEffectListener;
    private OrnamentEffectListener ornamentEffectListener;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // 初始化 SLF4J 日誌
        try {
            // 確保 SLF4J 使用我們的配置
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
            // 手動初始化一個 logger 來觸發 SLF4J 初始化
            org.slf4j.LoggerFactory.getLogger(RPGInventory.class).info("SLF4J 日誌已初始化");
            getLogger().info("SLF4J 日誌系統已成功初始化");
        } catch (Exception e) {
            getLogger().warning("SLF4J 日誌初始化失敗，但這不會影響插件功能: " + e.getMessage());
        }
        
        // 保存默認配置
        saveDefaultConfig();
        
        // 加載允許的圖騰 ID
        loadAllowedTotems();
        
        // 檢查 ExecutableItems 是否已安裝並啟用
        Plugin executableItems = Bukkit.getPluginManager().getPlugin("ExecutableItems");
        if (executableItems != null && executableItems.isEnabled()) {
            getServer().getLogger().info("[RPGInventory] ExecutableItems 已連接！");
            hasExecutableItems = true;
        } else {
            getServer().getLogger().warning("[RPGInventory] 未找到 ExecutableItems 插件，飾品功能將被禁用！");
        }
        
        // 初始化數據庫
        databaseManager = DatabaseFactory.getDatabaseManager(this);
        if (!databaseManager.initialize()) {
            getServer().getLogger().severe("[RPGInventory] 數據庫初始化失敗，插件將被禁用！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 初始化監聽器
        this.totemEffectListener = new TotemEffectListener(this);
        this.ornamentEffectListener = new OrnamentEffectListener(this);
        
        // 註冊命令
        getCommand("rpginv").setExecutor(new RPGInventoryCommand(this));
        
        // 註冊監聽器
        getServer().getPluginManager().registerEvents(new InventoryListeners(this), this);
        getServer().getPluginManager().registerEvents(this.totemEffectListener, this);
        getServer().getPluginManager().registerEvents(this.ornamentEffectListener, this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        
        // 啟動自動保存任務
        startAutoSaveTask();
        
        // 為已經在線的玩家初始化圖騰效果（針對重載插件的情況）
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                initializePlayerInventory(player);
            }
        }, 20L); // 延遲1秒確保插件完全加載
        

        
        getServer().getLogger().info("[RPGInventory] 插件已成功啟用！");
    }

    /**
     * 加載配置文件中允許的圖騰 ID
     */
    private void loadAllowedTotems() {
        allowedTotems = getConfig().getStringList("allowed-totems");
        if (allowedTotems == null) {
            allowedTotems = new ArrayList<>();
        }
        getLogger().info("已加載 " + allowedTotems.size() + " 個允許的圖騰 ID");
    }
    
    /**
     * 檢查物品 ID 是否在允許的圖騰列表中
     * @param itemId 物品 ID
     * @return 是否允許作為圖騰
     */
    public boolean isAllowedTotem(String itemId) {
        return allowedTotems.contains(itemId);
    }

    @Override
    public void onDisable() {
        // 停止自動保存任務
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        
        // 保存所有在線玩家的數據
        for (Player player : Bukkit.getOnlinePlayers()) {
            savePlayerInventory(player);
        }
        
        // 關閉數據庫連接
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        // 清除 InventoryGUI 中的緩存
        InventoryGUI.clearOpenInventories();
        
        getServer().getLogger().info("[RPGInventory] 插件已禁用！");
    }
    
    /**
     * 啟動自動保存任務
     */
    private void startAutoSaveTask() {
        int autoSaveInterval = getConfig().getInt("database.auto-save", 300) * 20; // 轉換為 tick
        
        if (autoSaveInterval <= 0) {
            getLogger().warning("自動保存間隔設置為 0 或負數，自動保存功能已禁用！");
            return;
        }
        
        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            getLogger().info("正在自動保存玩家數據...");
            for (Player player : Bukkit.getOnlinePlayers()) {
                savePlayerInventory(player);
            }
            getLogger().info("玩家數據自動保存完成！");
        }, autoSaveInterval, autoSaveInterval);
    }
    
    /**
     * 保存玩家物品欄數據
     * @param player 玩家
     */
    public void savePlayerInventory(Player player) {
        UUID playerUUID = player.getUniqueId();
        Map<Integer, ItemStack> totems = new HashMap<>();
        
        // 獲取玩家的 RPG 物品欄
        if (InventoryGUI.hasOpenInventory(playerUUID)) {
            try {
                // 飾品欄物品
                ItemStack ornament = InventoryGUI.getOrnamentItem(playerUUID);
                
                // 圖騰欄物品
                for (int slotId : InventoryGUI.getTotemSlots()) {
                    ItemStack totem = InventoryGUI.getTotemItem(playerUUID, slotId);
                    if (totem != null) {
                        totems.put(slotId, totem);
                    }
                }
                
                // 保存到數據庫
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    databaseManager.saveInventory(playerUUID, ornament, totems);
                });
            } catch (Exception e) {
                getLogger().severe("保存玩家 " + player.getName() + " 的物品欄數據時發生錯誤: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 初始化玩家的物品欄和圖騰效果
     * @param player 玩家
     */
    public void initializePlayerInventory(Player player) {
        // 靜默加載玩家數據（不打開物品欄）
        loadPlayerInventorySilently(player);
        
        // 初始化圖騰循環任務
        if (totemEffectListener != null) {
            getServer().getLogger().info("正在為玩家 " + player.getName() + " 初始化圖騰效果...");
            totemEffectListener.updateTotemLoopTasks(player);
        }
        
        // 初始化飾品循環任務
        if (ornamentEffectListener != null && hasExecutableItems) {
            getServer().getLogger().info("正在為玩家 " + player.getName() + " 初始化飾品效果...");
            ornamentEffectListener.updateOrnamentLoopTask(player);
        }
    }
    
    /**
     * 靜默加載玩家物品欄數據（不打開界面）
     * @param player 玩家
     */
    public void loadPlayerInventorySilently(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // 檢查玩家是否有保存的數據
        if (!databaseManager.hasData(playerUUID)) {
            return;
        }
        
        try {
            // 加載飾品欄物品
            ItemStack ornament = databaseManager.loadOrnament(playerUUID);
            
            // 加載圖騰欄物品
            Map<Integer, ItemStack> totems = databaseManager.loadTotems(playerUUID);
            
            // 創建物品欄但不打開
            Inventory inventory = Bukkit.createInventory(null, InventoryGUI.getInventorySize(), InventoryGUI.getInventoryTitle());
            
            // 設置邊框和槽位
            InventoryGUI.setupInventoryBase(inventory);
            
            // 設置飾品物品（如果有）
            if (ornament != null) {
                // 直接設置物品，不使用佔位符
                inventory.setItem(InventoryGUI.getOrnamentSlot(), ornament);
                getLogger().info("已加載玩家 " + player.getName() + " 的飾品");
            }
            
            // 設置圖騰物品（如果有）
            for (Map.Entry<Integer, ItemStack> entry : totems.entrySet()) {
                int slot = entry.getKey();
                ItemStack totem = entry.getValue();
                if (totem != null) {
                    // 直接設置物品，不使用佔位符
                    inventory.setItem(slot, totem);
                    getLogger().info("已加載玩家 " + player.getName() + " 的圖騰(槽位 " + slot + ")");
                }
            }
            
            // 保存物品欄但不打開
            InventoryGUI.addOpenInventory(playerUUID, inventory);
            
            getLogger().info("已靜默加載玩家 " + player.getName() + " 的物品欄數據");
        } catch (Exception e) {
            getLogger().severe("加載玩家 " + player.getName() + " 的物品欄數據時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 加載玩家物品欄數據（打開物品欄時使用）
     * @param player 玩家
     */
    public void loadPlayerInventory(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // 檢查玩家是否有保存的數據
        if (!databaseManager.hasData(playerUUID)) {
            return;
        }
        
        // 從數據庫加載數據
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                // 加載飾品欄物品
                ItemStack ornament = databaseManager.loadOrnament(playerUUID);
                
                // 加載圖騰欄物品
                Map<Integer, ItemStack> totems = databaseManager.loadTotems(playerUUID);
                
                // 更新物品欄
                Bukkit.getScheduler().runTask(this, () -> {
                    if (InventoryGUI.hasOpenInventory(playerUUID)) {
                        InventoryGUI.setOrnamentItem(playerUUID, ornament);
                        
                        for (Map.Entry<Integer, ItemStack> entry : totems.entrySet()) {
                            InventoryGUI.setTotemItem(playerUUID, entry.getKey(), entry.getValue());
                        }
                    }
                });
            } catch (Exception e) {
                getLogger().severe("加載玩家 " + player.getName() + " 的物品欄數據時發生錯誤: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * 獲取插件實例
     * @return RPGInventory 實例
     */
    public static RPGInventory getInstance() {
        return instance;
    }
    
    /**
     * 檢查是否有 ExecutableItems
     * @return 是否有 ExecutableItems
     */
    public boolean hasExecutableItems() {
        return hasExecutableItems;
    }
    
    /**
     * 獲取數據庫管理器
     * @return 數據庫管理器
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("rpginv")) {
            if (args.length == 0) {
                sender.sendMessage("§c用法: /rpginv <open|help|debug>");
                return true;
            }

            // 處理open命令
            if (args[0].equalsIgnoreCase("open")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§c只有玩家可以使用此命令!");
                    return true;
                }
                
                Player player = (Player) sender;
                // 使用靜態方法打開物品欄
                org.MAGd.rPGInventory.gui.InventoryGUI.openInventory(player);
                return true;
            }
            
            // 處理help命令
            if (args[0].equalsIgnoreCase("help")) {
                sender.sendMessage("§6===== RPGInventory 命令幫助 =====");
                sender.sendMessage("§e/rpginv open §7- 打開RPG物品欄");
                if (sender.hasPermission("rpginventory.admin")) {
                    sender.sendMessage("§e/rpginv debug §7- 切換調試模式");
                }
                return true;
            }
            
            sender.sendMessage("§c未知命令。使用 /rpginv help 查看幫助。");
            return true;
        }
        return false;
    }

    /**
     * 獲取圖騰效果監聽器
     * @return TotemEffectListener實例
     */
    public TotemEffectListener getTotemEffectListener() {
        return totemEffectListener;
    }
    
    /**
     * 獲取飾品效果監聽器
     * @return OrnamentEffectListener實例
     */
    public OrnamentEffectListener getOrnamentEffectListener() {
        return ornamentEffectListener;
    }


}
