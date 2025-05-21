package org.MAGd.rPGInventory.listeners;

import org.MAGd.rPGInventory.RPGInventory;
import org.MAGd.rPGInventory.gui.InventoryGUI;
import org.MAGd.rPGInventory.utils.TotemEffects;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 監聽與處理圖騰效果的觸發
 */
public class TotemEffectListener implements Listener {

    private final RPGInventory plugin;
    
    // 存儲玩家UUID與其圖騰循環任務的映射
    private final Map<UUID, Map<Integer, BukkitTask>> playerTotemTasks = new ConcurrentHashMap<>();
    
    // 追踪正在處理橫掃效果的玩家，防止遞歸調用
    private final Set<UUID> processingSweepPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    // 修改：playerDebugMode 改為針對每個玩家
    private final Map<UUID, Boolean> playerDebugStates = new ConcurrentHashMap<>();
    private boolean consoleDebugMode = false; // consoleDebugMode 保持全局

    // 玩家攻擊增益映射 (UUID -> 百分比)
    private final Map<UUID, Double> playerDamageBoosts = new ConcurrentHashMap<>();

    // 玩家傷害減免映射 (UUID -> 百分比)
    private final Map<UUID, Double> playerDamageResistances = new ConcurrentHashMap<>();

    // 跟蹤效果到期任務
    private final Map<UUID, BukkitTask> damageBoostExpirationTasks = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> damageResistanceExpirationTasks = new ConcurrentHashMap<>();

    public TotemEffectListener(RPGInventory plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 切換指定玩家的調試模式
     * @param player 要切換調試模式的玩家
     * @return 切換後的狀態
     */
    public boolean togglePlayerDebugMode(Player player) {
        UUID playerUUID = player.getUniqueId();
        boolean currentState = playerDebugStates.getOrDefault(playerUUID, false);
        boolean newState = !currentState;
        playerDebugStates.put(playerUUID, newState);
        // 向控制台輸出玩家調試模式的變更狀態，以便管理員知曉
        plugin.getLogger().info("[RPGInventory] 玩家 " + player.getName() + " 的調試模式已 " + (newState ? "開啟" : "關閉"));
        return newState;
    }

    /**
     * 切換控制台調試模式
     * @return 切換後的狀態
     */
    public boolean toggleConsoleDebugMode() {
        consoleDebugMode = !consoleDebugMode;
        plugin.getLogger().info("[RPGInventory] 控制台調試模式已 " + (consoleDebugMode ? "開啟" : "關閉"));
        return consoleDebugMode;
    }
    
    /**
     * 獲取指定玩家的調試模式狀態
     * @param player 玩家
     * @return 該玩家的調試模式是否開啟
     */
    public boolean isPlayerDebugMode(Player player) {
        if (player == null) return false; // 安全檢查
        return playerDebugStates.getOrDefault(player.getUniqueId(), false);
    }

    /**
     * 獲取當前控制台調試模式狀態
     * @return 控制台調試模式是否開啟
     */
    public boolean isConsoleDebugMode() {
        return consoleDebugMode;
    }
    
    /**
     * 輸出調試日誌到控制台，只有在consoleDebugMode為true時才輸出
     * @param message 日誌信息
     */
    private void debug(String message) {
        if (consoleDebugMode) {
            System.out.println("[RPGInventory-Debug] " + message);
        }
    }

    /**
     * 當玩家攻擊實體時觸發圖騰效果
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        // 檢查攻擊者是否為玩家
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        UUID playerUUID = player.getUniqueId();
        
        // 如果玩家正在處理橫掃效果，則跳過以防止遞歸
        if (processingSweepPlayers.contains(playerUUID)) {
            return;
        }

        // 檢查玩家是否有打開的RPG物品欄
        if (!InventoryGUI.hasOpenInventory(playerUUID)) return;

        // 獲取玩家的所有圖騰
        List<ItemStack> totems = getPlayerTotems(playerUUID);
        if (totems.isEmpty()) return;

        debug("玩家 " + player.getName() + " 攻擊了實體，檢查圖騰效果");
        debug("當前裝備的圖騰數量: " + totems.size());
        
        // 直接觸發ExecutableItems的原生效果
        for (ItemStack totem : totems) {
            try {
                String itemId = getItemId(totem);
                if (itemId != null && !itemId.isEmpty()) {
                    debug("觸發圖騰: " + itemId + " 的效果");
                    
                    // 特別處理auu1.x系列圖騰
                    if (itemId.startsWith("auu1")) {
                        debug("檢測到橫掃圖騰: " + itemId);
                        
                        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
                        if (mainHandItem != null && isSword(mainHandItem.getType())) {
                            debug("玩家持有劍 (" + mainHandItem.getType() + ")，觸發橫掃");
                            // 嘗試直接使用MOB_AROUND命令
                            if (itemId.equals("auu1.1")) {
                                executeMobAroundCommand(player, event.getEntity(), event.getDamage() * 0.5);
                            } else if (itemId.equals("auu1.2")) {
                                executeMobAroundCommand(player, event.getEntity(), event.getDamage() * 0.6);
                            } else if (itemId.equals("auu1.3")) {
                                executeMobAroundCommand(player, event.getEntity(), event.getDamage() * 0.7);
                            } else if (itemId.equals("auu1.4")) {
                                executeMobAroundCommand(player, event.getEntity(), event.getDamage() * 0.8);
                            } else if (itemId.equals("auu1.5")) {
                                executeMobAroundCommand(player, event.getEntity(), event.getDamage() * 1.0);
                            }
                        } else {
                            String heldItemType = (mainHandItem != null && mainHandItem.getType() != org.bukkit.Material.AIR) ? mainHandItem.getType().toString() : "空手";
                            debug("玩家未持有劍 (" + heldItemType + ")，不觸發橫掃圖騰: " + itemId);
                        }
                    }
                }
            } catch (Exception e) {
                debug("處理圖騰時發生錯誤: " + e.getMessage());
                e.printStackTrace();
            }
            
            // 仍然嘗試使用標準的ExecutableItems激活器
            triggerExecutableItem(player, totem, "PLAYER_HIT_ENTITY");
        }
    }

    /**
     * 檢查物品類型是否為劍
     * @param material 物品材料類型
     * @return 如果是劍則返回 true，否則返回 false
     */
    private boolean isSword(org.bukkit.Material material) {
        if (material == null) return false;
        String materialName = material.name();
        return materialName.endsWith("_SWORD");
    }

    /**
     * 當玩家交互時觸發圖騰效果(右鍵點擊等)
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // 檢查玩家是否有打開的RPG物品欄
        if (!InventoryGUI.hasOpenInventory(playerUUID)) return;

        // 獲取玩家的所有圖騰
        List<ItemStack> totems = getPlayerTotems(playerUUID);
        if (totems.isEmpty()) return;

        // 延遲觸發圖騰效果，確保在事件處理完畢後執行
        new BukkitRunnable() {
            @Override
            public void run() {
                for (ItemStack totem : totems) {
                    triggerExecutableItem(player, totem, "PLAYER_INTERACT");
                }
            }
        }.runTaskLater(plugin, 1L);
    }
    
    /**
     * 監聽物品欄點擊事件，處理圖騰放入或取出
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!InventoryGUI.isRPGInventory(event.getInventory())) return;
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        // 檢查是否是圖騰欄位
        if (InventoryGUI.isTotemSlot(slot)) {
            // 延遲處理，確保在事件處理完畢後執行
            new BukkitRunnable() {
                @Override
                public void run() {
                    updateTotemLoopTasks(player);
                }
            }.runTaskLater(plugin, 1L);
        }
    }
    
    /**
     * 玩家加入伺服器時，更新圖騰循環任務
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 延遲處理，確保玩家數據完全加載
        new BukkitRunnable() {
            @Override
            public void run() {
                if (InventoryGUI.hasOpenInventory(player.getUniqueId())) {
                    updateTotemLoopTasks(player);
                }
            }
        }.runTaskLater(plugin, 20L); // 延遲1秒
    }
    
    /**
     * 玩家離開伺服器時，取消其所有圖騰循環任務
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cancelTotemTasks(event.getPlayer().getUniqueId());
        // 玩家退出時移除其調試狀態，防止Map無限增大
        playerDebugStates.remove(event.getPlayer().getUniqueId());
        damageBoostExpirationTasks.remove(event.getPlayer().getUniqueId());
        damageResistanceExpirationTasks.remove(event.getPlayer().getUniqueId());
        playerDamageBoosts.remove(event.getPlayer().getUniqueId());
        playerDamageResistances.remove(event.getPlayer().getUniqueId());
    }
    
    /**
     * 更新玩家的圖騰循環任務
     * @param player 玩家
     */
    public void updateTotemLoopTasks(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        debug("正在更新玩家 " + player.getName() + " 的圖騰效果...");
        
        // 獲取所有圖騰及其所在槽位
        Map<Integer, ItemStack> currentTotems = new HashMap<>();
        for (int slot : InventoryGUI.getTotemSlots()) {
            ItemStack totem = InventoryGUI.getTotemItem(playerUUID, slot);
            if (totem != null) {
                currentTotems.put(slot, totem);
                debug("找到圖騰在槽位 " + slot);
            }
        }
        
        if (currentTotems.isEmpty()) {
            debug("玩家 " + player.getName() + " 沒有裝備任何圖騰");
        } else {
            debug("玩家 " + player.getName() + " 共有 " + currentTotems.size() + " 個圖騰");
        }
        
        debug("玩家 " + player.getName() + " 共有 " + currentTotems.size() + " 個圖騰");
        
        // 取得當前任務映射
        Map<Integer, BukkitTask> currentTasks = playerTotemTasks.computeIfAbsent(playerUUID, k -> new HashMap<>());
        
        // 記錄要移除的槽位
        Set<Integer> slotsToRemove = new HashSet<>(currentTasks.keySet());
        
        // 為每個圖騰創建循環任務
        for (Map.Entry<Integer, ItemStack> entry : currentTotems.entrySet()) {
            int slot = entry.getKey();
            ItemStack totem = entry.getValue();
            
            // 從移除列表中移除當前處理的槽位
            slotsToRemove.remove(slot);
            
            // 獲取圖騰ID
            String itemId = TotemEffects.getExecutableItemId(totem, plugin);
            debug("處理圖騰ID: " + itemId + " 在槽位 " + slot);
            
            if (itemId == null) {
                debug("無法獲取圖騰ID，跳過槽位 " + slot);
                continue;
            }
            
            // 立即應用一次圖騰效果，不等待循環
            debug("立即應用圖騰 " + itemId + " 的效果");
            boolean success = TotemEffects.applyTotemEffect(player, totem, plugin);
            if (success) {
                debug("成功立即應用圖騰 " + itemId + " 效果");
                
                // 修改：傳遞 player 對象
                if (isPlayerDebugMode(player)) {
                    player.sendMessage("§6[圖騰] §a" + itemId + " 圖騰效果已啟動！");
                }
            } else {
                debug("應用圖騰 " + itemId + " 效果失敗");
            }
            
            // 嘗試獲取循環間隔
            int intervalSeconds = getTotemInterval(itemId);
            debug("圖騰 " + itemId + " 的循環間隔: " + intervalSeconds + " 秒");
            
            if (intervalSeconds <= 0) {
                debug("圖騰 " + itemId + " 不是循環類型，跳過循環任務創建");
                continue;
            }
            
            // 檢查是否已有這個槽位的任務
            if (currentTasks.containsKey(slot)) {
                // 取消舊任務並創建新任務
                currentTasks.get(slot).cancel();
                debug("取消並重新創建槽位 " + slot + " 的圖騰任務");
                currentTasks.remove(slot);
            }
            
            // 創建循環任務
            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    // 檢查玩家是否仍在線以及圖騰是否仍在物品欄中
                    if (player.isOnline() && InventoryGUI.hasOpenInventory(playerUUID)) {
                        ItemStack currentTotem = InventoryGUI.getTotemItem(playerUUID, slot);
                        if (currentTotem != null && currentTotem.isSimilar(totem)) {
                            // 應用圖騰效果
                            debug("嘗試觸發圖騰 " + itemId + " 的效果");
                            
                            // 修改：僅靜默延長效果持續時間，不重新應用效果消息
                            boolean success = TotemEffects.applyTotemEffectSilently(player, totem, plugin);
                            if (success) {
                                // 不顯示任何消息，只在後台記錄
                                debug("靜默延長圖騰 " + itemId + " 效果持續時間");
                                
                                // 添加更詳細的日誌
                                if (itemId.startsWith("auu3")) {
                                    debug("靜默延長玩家 " + player.getName() + " 的攻擊圖騰效果: " + itemId);
                                } else if (itemId.startsWith("auu2")) {
                                    debug("靜默延長玩家 " + player.getName() + " 的防禦圖騰效果: " + itemId);
                                }
                            } else {
                                debug("圖騰效果延長失敗");
                            }
                        } else {
                            // 圖騰已更換或移除，取消任務
                            debug("圖騰已移除或更換，取消任務");
                            this.cancel();
                            removeTotemTask(playerUUID, slot);
                        }
                    } else {
                        // 玩家下線或物品欄已關閉，取消任務
                        debug("玩家已下線或物品欄已關閉，取消任務");
                        this.cancel();
                        removeTotemTask(playerUUID, slot);
                    }
                }
            }.runTaskTimer(plugin, intervalSeconds * 20L, intervalSeconds * 20L); // 轉換為遊戲刻
            
            debug("已為圖騰 " + itemId + " 創建循環任務");
            
            // 保存任務引用
            addTotemTask(playerUUID, slot, task);
        }
        
        // 取消不再有效的槽位任務
        for (int slotToRemove : slotsToRemove) {
            BukkitTask taskToRemove = currentTasks.get(slotToRemove);
            if (taskToRemove != null) {
                debug("取消槽位 " + slotToRemove + " 的無效任務");
                taskToRemove.cancel();
                removeTotemTask(playerUUID, slotToRemove);
            }
        }
        
        debug("玩家 " + player.getName() + " 的圖騰效果更新完成");
    }
    
    /**
     * 獲取圖騰的循環間隔時間（秒）
     * @param itemId 圖騰ID
     * @return 循環間隔時間，如果不是循環圖騰則返回0
     */
    private int getTotemInterval(String itemId) {
        // 回血圖騰系列
        if (itemId.equals("auu4.1") || itemId.equals("auu4.2") || 
            itemId.equals("auu4.3") || itemId.equals("auu4.4")) {
            return 5; // 每5秒
        } else if (itemId.equals("auu4.6")) {
            return 8; // 每8秒
        }
        // 攻擊圖騰系列
        else if (itemId.startsWith("auu3")) {
            return 5; // 由10秒改為每5秒，增加效果持續性
        }
        // 防禦圖騰系列
        else if (itemId.startsWith("auu2")) {
            return 5; // 由10秒改為每5秒，增加效果持續性
        }
        // 法力圖騰系列
        else if (itemId.startsWith("auu5")) {
            return 5; // 每5秒
        }
        
        return 0;
    }
    
    /**
     * 添加圖騰循環任務
     * @param playerUUID 玩家UUID
     * @param slot 槽位
     * @param task 任務
     */
    private void addTotemTask(UUID playerUUID, int slot, BukkitTask task) {
        playerTotemTasks.computeIfAbsent(playerUUID, k -> new HashMap<>()).put(slot, task);
    }
    
    /**
     * 移除圖騰循環任務
     * @param playerUUID 玩家UUID
     * @param slot 槽位
     */
    private void removeTotemTask(UUID playerUUID, int slot) {
        Map<Integer, BukkitTask> tasks = playerTotemTasks.get(playerUUID);
        if (tasks != null) {
            tasks.remove(slot);
            if (tasks.isEmpty()) {
                playerTotemTasks.remove(playerUUID);
            }
        }
    }
    
    /**
     * 取消玩家的所有圖騰循環任務
     * @param playerUUID 玩家UUID
     */
    private void cancelTotemTasks(UUID playerUUID) {
        Map<Integer, BukkitTask> tasks = playerTotemTasks.remove(playerUUID);
        if (tasks != null) {
            for (BukkitTask task : tasks.values()) {
                try {
                    task.cancel();
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * 獲取玩家的所有圖騰
     * @param playerUUID 玩家UUID
     * @return 圖騰列表
     */
    private List<ItemStack> getPlayerTotems(UUID playerUUID) {
        List<ItemStack> totems = new ArrayList<>();
        
        // 獲取所有圖騰欄位
        for (int slot : InventoryGUI.getTotemSlots()) {
            ItemStack totem = InventoryGUI.getTotemItem(playerUUID, slot);
            if (totem != null) {
                totems.add(totem);
            }
        }
        
        return totems;
    }

    /**
     * 通過反射觸發ExecutableItems物品的效果
     * @param player 玩家
     * @param item 物品
     * @param activatorType 激活器類型
     */
    private void triggerExecutableItem(Player player, ItemStack item, String activatorType) {
        if (!plugin.hasExecutableItems()) return;
        
        debug("嘗試觸發物品效果，激活器類型: " + activatorType);
        
        try {
            // 獲取ExecutableItemsAPI類
            Class<?> apiClass = Class.forName("com.ssomar.score.api.executableitems.ExecutableItemsAPI");
            
            // 獲取ExecutableItemsManager
            Object manager = apiClass.getMethod("getExecutableItemsManager").invoke(null);
            
            // 獲取ExecutableItem對象
            Object result = manager.getClass().getMethod("getExecutableItem", ItemStack.class).invoke(manager, item);
            
            // 檢查結果是否為空
            boolean isEmpty = (boolean) result.getClass().getMethod("isEmpty").invoke(result);
            if (isEmpty) {
                debug("無法獲取ExecutableItem，物品可能無效");
                return;
            }
            
            // 獲取ExecutableItem
            Object executableItem = result.getClass().getMethod("get").invoke(result);
            
            // 獲取物品ID
            String itemId = (String) executableItem.getClass().getMethod("getId").invoke(executableItem);
            debug("觸發物品 " + itemId + " 的效果");
            
            // 對於其他激活器類型，嘗試使用executeActivator方法
            Method executeMethod = null;
            for (Method method : executableItem.getClass().getMethods()) {
                if (method.getName().equals("executeActivator") && method.getParameterCount() == 3) {
                    executeMethod = method;
                    break;
                }
            }
            
            if (executeMethod != null) {
                debug("找到executeActivator方法，執行激活器: " + activatorType);
                // 執行激活器
                executeMethod.invoke(executableItem, player, activatorType, item);
                debug("已執行激活器");
            } else {
                debug("無法找到executeActivator方法");
            }
        } catch (Exception e) {
            debug("無法觸發ExecutableItem效果: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 直接執行MOB_AROUND命令
     * @param player 玩家
     * @param target 目標實體
     * @param damage 傷害值
     */
    private void executeMobAroundCommand(Player player, Entity target, double damage) {
        try {
            UUID playerUUID = player.getUniqueId();
            
            // 標記玩家正在處理橫掃效果，防止遞歸
            processingSweepPlayers.add(playerUUID);
            
            try {
                debug("嘗試執行MOB_AROUND命令，傷害值: " + damage);
                
                // 確保傷害值至少為3
                double effectiveDamage = Math.max(damage, 3.0);
                
                // 獲取目標周圍3格內的所有實體
                Collection<Entity> nearbyEntities = target.getWorld().getNearbyEntities(target.getLocation(), 3, 3, 3);
                int entityCount = 0;
                
                for (Entity entity : nearbyEntities) {
                    // 跳過玩家自身和已攻擊的目標
                    if (entity.equals(player) || entity.equals(target)) continue;
                    
                    // 只對生物實體造成傷害
                    if (entity instanceof LivingEntity) {
                        LivingEntity livingEntity = (LivingEntity) entity;
                        
                        debug("對實體 " + entity.getType() + " 造成橫掃傷害: " + effectiveDamage);
                        // 直接使用原生方法造成傷害
                        livingEntity.damage(effectiveDamage, player);
                        entityCount++;
                        
                        // 播放特效
                        Location location = livingEntity.getLocation().add(0, 1, 0);
                        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, location, 3, 0.5, 0.5, 0.5, 0);
                    }
                }
                
                if (entityCount > 0) {
                    debug("成功對 " + entityCount + " 個實體造成橫掃傷害");
                    // 播放橫掃特效
                    player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 
                            8, 2.0, 0.5, 2.0, 0);
                    // 播放音效增強效果
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
                    // 修改：傳遞 player 對象
                    if (isPlayerDebugMode(player)) {
                        player.sendMessage("§6[圖騰] §a觸發了橫掃效果，命中了 " + entityCount + " 個敵人！傷害: " + Math.round(effectiveDamage * 10) / 10.0);
                    }
                } else {
                    debug("未找到可造成傷害的實體");
                }
            } finally {
                // 確保無論如何都會移除標記
                processingSweepPlayers.remove(playerUUID);
            }
        } catch (Exception e) {
            debug("執行MOB_AROUND命令時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 獲取物品的ExecutableItems ID
     * @param item 物品
     * @return 物品ID或null
     */
    private String getItemId(ItemStack item) {
        if (item == null) return null;
        
        try {
            // 獲取ExecutableItemsAPI類
            Class<?> apiClass = Class.forName("com.ssomar.score.api.executableitems.ExecutableItemsAPI");
            
            // 獲取ExecutableItemsManager
            Object manager = apiClass.getMethod("getExecutableItemsManager").invoke(null);
            
            // 獲取ExecutableItem對象
            Object result = manager.getClass().getMethod("getExecutableItem", ItemStack.class).invoke(manager, item);
            
            // 檢查結果是否為空
            boolean isEmpty = (boolean) result.getClass().getMethod("isEmpty").invoke(result);
            
            if (!isEmpty) {
                // 獲取ExecutableItem
                Object executableItem = result.getClass().getMethod("get").invoke(result);
                
                // 獲取ID
                return (String) executableItem.getClass().getMethod("getId").invoke(executableItem);
            }
        } catch (Exception e) {
            debug("獲取物品ID時發生錯誤: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * 使用原有的方法處理攻擊增益
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerAttackEntityWithBoost(EntityDamageByEntityEvent event) {
        // 檢查攻擊者是否為玩家
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        UUID playerUUID = player.getUniqueId();
        
        // 檢查是否有攻擊增益
        Double damageBoostPercent = playerDamageBoosts.get(playerUUID);
        if (damageBoostPercent != null && damageBoostPercent > 0) {
            // 獲取原始傷害
            double originalDamage = event.getDamage();
            
            // 計算增益後的傷害
            double boostMultiplier = 1.0 + (damageBoostPercent / 100.0);
            double newDamage = originalDamage * boostMultiplier;
            
            // 添加詳細的後台日誌 - 修改：使其受 consoleDebugMode 控制
            if (isConsoleDebugMode()) {
                System.out.println("[RPGInventory-AUU3] 玩家 " + player.getName() + 
                        " 的攻擊圖騰觸發 | 原始傷害: " + originalDamage + 
                        " | 增益百分比: " + damageBoostPercent + "%" +
                        " | 乘數: " + boostMultiplier +
                        " | 最終傷害: " + newDamage);
            }
            
            // 設置新的傷害
            event.setDamage(newDamage);
            
            // 顯示調試信息
            debug("玩家 " + player.getName() + " 造成了額外傷害! 原始傷害: " + originalDamage + 
                  ", 增益: " + damageBoostPercent + "%, 新傷害: " + newDamage);
            
            // 移除檢查玩家是否裝備圖騰的邏輯，直接顯示增益信息
            double increasedDamage = newDamage - originalDamage;
            // 修改：傳遞 player 對象
            if (isPlayerDebugMode(player)) {
                player.sendMessage("§6[圖騰] §a攻擊加成圖騰生效！原始傷害: §f" + String.format("%.1f", originalDamage) + 
                                  " §a→ 新傷害: §f" + String.format("%.1f", newDamage) + 
                                  " §a(增加了 §f" + String.format("%.1f", increasedDamage) + "§a 點傷害)");
            }
            
            // 在攻擊實體上方顯示暴擊粒子效果
            if (event.getEntity() instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) event.getEntity();
                target.getWorld().spawnParticle(Particle.CRIT, 
                        target.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }
    
    // 添加新的事件處理器處理玩家受到傷害時的防禦圖騰效果
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamagedWithResistance(EntityDamageEvent event) {
        // 如果受到傷害的不是玩家，則跳過
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        UUID playerUUID = player.getUniqueId();
        
        // 檢查是否有打開的物品欄
        if (!InventoryGUI.hasOpenInventory(playerUUID)) return;
        
        // 獲取當前的傷害減免百分比
        double resistancePercent = getDamageResistance(playerUUID);
        
        if (resistancePercent > 0) {
            double originalDamage = event.getDamage();
            double newDamage = originalDamage * (1 - resistancePercent / 100.0);
            double reducedDamage = originalDamage - newDamage;
            
            // 添加詳細的後台日誌 - 修改：使其受 consoleDebugMode 控制
            if (isConsoleDebugMode()) {
                System.out.println("[RPGInventory-AUU2] 玩家 " + player.getName() + 
                        " 的防禦圖騰觸發 | 原始傷害: " + originalDamage + 
                        " | 減免百分比: " + resistancePercent + "%" +
                        " | 減免後傷害: " + newDamage +
                        " | 減免值: " + reducedDamage);
            }
            
            // 設置新的傷害值
            event.setDamage(newDamage);
            
            debug("玩家 " + player.getName() + " 減免了 " + resistancePercent + "% 的傷害，原始傷害: " + 
                  originalDamage + "，新傷害: " + newDamage);
                  
            // 移除檢查玩家是否裝備圖騰的邏輯，直接顯示減免信息
            // 只有當減免的傷害大於0.5才顯示信息，避免微小傷害也發送消息
            if (reducedDamage > 0.5) {
                // 修改：傳遞 player 對象
                if (isPlayerDebugMode(player)) {
                    player.sendMessage("§6[圖騰] §a防禦圖騰生效！原始傷害: §f" + String.format("%.1f", originalDamage) + 
                                      " §a→ 實際傷害: §f" + String.format("%.1f", newDamage) + 
                                      " §a(減免了 §f" + String.format("%.1f", reducedDamage) + "§a 點傷害)");
                }
            }
        }
    }

    /**
     * 設置攻擊增益百分比
     * @param playerUUID 玩家UUID
     * @param percent 增益百分比
     */
    public void setDamageBoost(UUID playerUUID, double percent) {
        // 取消已有的到期任務
        BukkitTask existingTask = damageBoostExpirationTasks.remove(playerUUID);
        if (existingTask != null) {
            try {
                existingTask.cancel();
            } catch (Exception ignored) {}
        }
        
        // 獲取當前增益值
        Double currentBoost = playerDamageBoosts.get(playerUUID);
        
        // 如果已經有相同或更高的增益，不重設計時器
        if (currentBoost != null && currentBoost >= percent) {
            debug("玩家已有" + currentBoost + "%的攻擊增益，不覆蓋為較低值" + percent + "%，但重新設定計時器");
            // 即使不覆蓋值，也重新設定計時器以延長現有效果
            playerDamageBoosts.put(playerUUID, currentBoost); // 確保值存在
        } else {
            playerDamageBoosts.put(playerUUID, percent);
            debug("為玩家 " + Bukkit.getPlayer(playerUUID).getName() + " 設置 " + percent + "% 的攻擊增益");
        }
        
        // 設置30秒後移除增益的計時器 (延長持續時間)
        BukkitTask newTask = new BukkitRunnable() {
            @Override
            public void run() {
                // 再次檢查任務是否仍然是當前最新的，避免舊任務錯誤地移除效果
                if (damageBoostExpirationTasks.get(playerUUID) != this) {
                    // 這不是最新的任務，可能已被取代，不做任何操作
                    return;
                }
                damageBoostExpirationTasks.remove(playerUUID); // 任務執行完畢，從追蹤中移除

                Double currentBoostCheck = playerDamageBoosts.get(playerUUID);
                // 比較時使用最初設定此任務時的 percent 值，或者如果是延長高值，則比較高值
                double valueWhenTaskScheduled = playerDamageBoosts.getOrDefault(playerUUID, 0.0);
                if (currentBoostCheck != null && Math.abs(currentBoostCheck - valueWhenTaskScheduled) < 0.1) {
                    playerDamageBoosts.remove(playerUUID);
                    debug("玩家 " + Bukkit.getPlayer(playerUUID).getName() + " 的攻擊增益已到期 (值: " + valueWhenTaskScheduled + "%)");
                    
                    Player playerInstance = Bukkit.getPlayer(playerUUID);
                    if (playerInstance != null && playerInstance.isOnline()) {
                        // 修改：傳遞 playerInstance 對象
                        if (isPlayerDebugMode(playerInstance)) {
                            playerInstance.sendMessage("§6[圖騰] §c你的攻擊力增益效果已消失");
                        }
                    }
                }
            }
        }.runTaskLater(plugin, 600L); // 30秒後過期 (600刻 = 30秒)
        damageBoostExpirationTasks.put(playerUUID, newTask);
    }
    
    /**
     * 靜默延長攻擊增益持續時間（不顯示消息）
     * @param playerUUID 玩家UUID
     * @param percent 增益百分比
     */
    public void silentlyExtendDamageBoost(UUID playerUUID, double percent) {
        // 取消已有的到期任務
        BukkitTask existingTask = damageBoostExpirationTasks.remove(playerUUID);
        if (existingTask != null) {
            try {
                existingTask.cancel();
            } catch (Exception ignored) {}
        }
        
        Double currentBoost = playerDamageBoosts.get(playerUUID);
        double valueToKeep = percent;

        if (currentBoost == null) {
            debug("玩家沒有攻擊增益，設置新值 " + percent + "% 並設定計時器");
            playerDamageBoosts.put(playerUUID, percent);
        } else {
            if (currentBoost >= percent) {
                debug("玩家已有" + currentBoost + "%的攻擊增益，只延長持續時間 (保持 " + currentBoost + "%)");
                valueToKeep = currentBoost; // 保持更高的值
            } else {
                debug("玩家的攻擊增益從 " + currentBoost + "% 更新為 " + percent + "% 並重設計時器");
                playerDamageBoosts.put(playerUUID, percent);
                valueToKeep = percent; // 更新為新的更高值
            }
        }
        
        final double finalValueToKeep = valueToKeep;
        BukkitTask newTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (damageBoostExpirationTasks.get(playerUUID) != this) {
                    return;
                }
                damageBoostExpirationTasks.remove(playerUUID);

                Double currentBoostCheck = playerDamageBoosts.get(playerUUID);
                if (currentBoostCheck != null && Math.abs(currentBoostCheck - finalValueToKeep) < 0.1) {
                    playerDamageBoosts.remove(playerUUID);
                    Player playerInstance = Bukkit.getPlayer(playerUUID);
                    String playerName = (playerInstance != null && playerInstance.isOnline()) ? playerInstance.getName() : playerUUID.toString();
                    debug("玩家 " + playerName + " 的攻擊增益已到期 (靜默延長，值: " + finalValueToKeep + "%)");
                    // 修改：使這條 System.out.println 受 consoleDebugMode 控制，或者通過 debug() 方法
                    if (isConsoleDebugMode()) {
                        System.out.println("[RPGInventory-Silent] 玩家 " + playerName + 
                                " 的攻擊增益已到期 (原值: " + finalValueToKeep + "%)");
                    }
                }
            }
        }.runTaskLater(plugin, 600L);
        damageBoostExpirationTasks.put(playerUUID, newTask);
    }

    /**
     * 獲取玩家的攻擊增益百分比
     * @param playerUUID 玩家UUID
     * @return 增益百分比，如果沒有則返回0
     */
    public double getDamageBoost(UUID playerUUID) {
        return playerDamageBoosts.getOrDefault(playerUUID, 0.0);
    }

    /**
     * 設置傷害減免百分比
     * @param playerUUID 玩家UUID
     * @param percent 減免百分比
     */
    public void setDamageResistance(UUID playerUUID, double percent) {
        // 取消已有的到期任務
        BukkitTask existingTask = damageResistanceExpirationTasks.remove(playerUUID);
        if (existingTask != null) {
            try {
                existingTask.cancel();
            } catch (Exception ignored) {}
        }
        
        Double currentResistance = playerDamageResistances.get(playerUUID);
        
        if (currentResistance != null && currentResistance >= percent) {
            debug("玩家已有" + currentResistance + "%的傷害減免，不覆蓋為較低值" + percent + "%，但重新設定計時器");
            playerDamageResistances.put(playerUUID, currentResistance); 
        } else {
            playerDamageResistances.put(playerUUID, percent);
            debug("為玩家 " + Bukkit.getPlayer(playerUUID).getName() + " 設置 " + percent + "% 的傷害減免");
        }
        
        BukkitTask newTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (damageResistanceExpirationTasks.get(playerUUID) != this) {
                    return;
                }
                damageResistanceExpirationTasks.remove(playerUUID);

                Double currentResCheck = playerDamageResistances.get(playerUUID);
                double valueWhenTaskScheduled = playerDamageResistances.getOrDefault(playerUUID, 0.0);

                if (currentResCheck != null && Math.abs(currentResCheck - valueWhenTaskScheduled) < 0.1) {
                    playerDamageResistances.remove(playerUUID);
                    debug("玩家 " + Bukkit.getPlayer(playerUUID).getName() + " 的傷害減免已到期 (值: " + valueWhenTaskScheduled + "%)");
                    
                    Player playerInstance = Bukkit.getPlayer(playerUUID);
                    if (playerInstance != null && playerInstance.isOnline()) {
                        // 修改：傳遞 playerInstance 對象
                        if (isPlayerDebugMode(playerInstance)) {
                            playerInstance.sendMessage("§6[圖騰] §c你的傷害減免效果已消失");
                        }
                    }
                }
            }
        }.runTaskLater(plugin, 600L);
        damageResistanceExpirationTasks.put(playerUUID, newTask);
    }
    
    /**
     * 靜默延長傷害減免持續時間（不顯示消息）
     * @param playerUUID 玩家UUID
     * @param percent 減免百分比
     */
    public void silentlyExtendDamageResistance(UUID playerUUID, double percent) {
        BukkitTask existingTask = damageResistanceExpirationTasks.remove(playerUUID);
        if (existingTask != null) {
            try {
                existingTask.cancel();
            } catch (Exception ignored) {}
        }

        Double currentResistance = playerDamageResistances.get(playerUUID);
        double valueToKeep = percent;

        if (currentResistance == null) {
            debug("玩家沒有傷害減免，設置新值 " + percent + "% 並設定計時器");
            playerDamageResistances.put(playerUUID, percent);
        } else {
            if (currentResistance >= percent) {
                debug("玩家已有" + currentResistance + "%的傷害減免，只延長持續時間 (保持 " + currentResistance + "%)");
                valueToKeep = currentResistance;
            } else {
                debug("玩家的傷害減免從 " + currentResistance + "% 更新為 " + percent + "% 並重設計時器");
                playerDamageResistances.put(playerUUID, percent);
                valueToKeep = percent;
            }
        }
        
        final double finalValueToKeep = valueToKeep;
        BukkitTask newTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (damageResistanceExpirationTasks.get(playerUUID) != this) {
                    return;
                }
                damageResistanceExpirationTasks.remove(playerUUID);
                
                Double currentResCheck = playerDamageResistances.get(playerUUID);
                if (currentResCheck != null && Math.abs(currentResCheck - finalValueToKeep) < 0.1) {
                    playerDamageResistances.remove(playerUUID);
                    Player playerInstance = Bukkit.getPlayer(playerUUID);
                    String playerName = (playerInstance != null && playerInstance.isOnline()) ? playerInstance.getName() : playerUUID.toString();
                    debug("玩家 " + playerName + " 的傷害減免已到期 (靜默延長，值: " + finalValueToKeep + "%)");
                    // 修改：使這條 System.out.println 受 consoleDebugMode 控制，或者通過 debug() 方法
                    if (isConsoleDebugMode()) {
                        System.out.println("[RPGInventory-Silent] 玩家 " + playerName + 
                                " 的傷害減免已到期 (原值: " + finalValueToKeep + "%)");
                    }
                }
            }
        }.runTaskLater(plugin, 600L);
        damageResistanceExpirationTasks.put(playerUUID, newTask);
    }

    /**
     * 獲取玩家的傷害減免百分比
     * @param playerUUID 玩家UUID
     * @return 減免百分比
     */
    public double getDamageResistance(UUID playerUUID) {
        return playerDamageResistances.getOrDefault(playerUUID, 0.0);
    }
} 