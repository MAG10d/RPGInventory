package org.MAGd.rPGInventory.gui;

import org.MAGd.rPGInventory.RPGInventory;
import org.MAGd.rPGInventory.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryGUI {
    
    private static final String INVENTORY_TITLE = "§6RPG物品欄";
    private static final int INVENTORY_SIZE = 54; // 6 行
    
    // 飾品欄位置
    private static final int ORNAMENT_SLOT = 4;
    
    // 圖騰欄位置 (5個)
    private static final int[] TOTEM_SLOTS = {
            20, 22, 24, 
            38, 40
    };
    
    // 每個槽位允許的圖騰ID前綴
    private static final Map<Integer, String> SLOT_TOTEM_TYPES = new HashMap<>();
    
    // 新增：圖騰類型描述
    private static final Map<String, String> TOTEM_TYPE_DESCRIPTIONS = new HashMap<>();
    static {
        TOTEM_TYPE_DESCRIPTIONS.put("auu1", "橫掃圖騰");
        TOTEM_TYPE_DESCRIPTIONS.put("auu2", "防禦圖騰");
        TOTEM_TYPE_DESCRIPTIONS.put("auu3", "攻擊圖騰");
        TOTEM_TYPE_DESCRIPTIONS.put("auu4", "回復圖騰");
        TOTEM_TYPE_DESCRIPTIONS.put("auu5", "魔法圖騰");
        
        SLOT_TOTEM_TYPES.put(20, "auu1"); // 槽位20只允許放置auu1系列圖騰
        SLOT_TOTEM_TYPES.put(22, "auu2"); // 槽位22只允許放置auu2系列圖騰
        SLOT_TOTEM_TYPES.put(24, "auu3"); // 槽位24只允許放置auu3系列圖騰
        SLOT_TOTEM_TYPES.put(38, "auu4"); // 槽位38只允許放置auu4系列圖騰
        SLOT_TOTEM_TYPES.put(40, "auu5"); // 槽位40只允許放置auu5系列圖騰
    }
    
    // 存儲已打開的物品欄
    private static final Map<UUID, Inventory> openInventories = new HashMap<>();
    
    /**
     * 清除所有已打開的物品欄緩存
     */
    public static void clearOpenInventories() {
        openInventories.clear();
        // 可選：RPGInventory.getInstance().getLogger().info("[RPGInventory] InventoryGUI cache cleared.");
    }
    
    /**
     * 打開 RPG 物品欄
     * @param player 玩家
     */
    public static void openInventory(Player player) {
        UUID playerUUID = player.getUniqueId();
        Inventory inventory;
        
        // 檢查是否已經存在靜默加載的物品欄
        if (hasOpenInventory(playerUUID)) {
            // 使用現有的物品欄
            inventory = openInventories.get(playerUUID);
            player.sendMessage("§a[RPGInventory] §e正在打開你的物品欄...");
        } else {
            // 創建新的物品欄
            inventory = Bukkit.createInventory(null, INVENTORY_SIZE, INVENTORY_TITLE);
            
            // 設置邊框
            setInventoryBorder(inventory);
            
            // 設置飾品欄位
            inventory.setItem(ORNAMENT_SLOT, createSlotItem("§d飾品欄", Material.NETHER_STAR, 
                    "§c移除物品"));
            
            // 設置圖騰欄位
            for (int slot : TOTEM_SLOTS) {
                String totemType = SLOT_TOTEM_TYPES.getOrDefault(slot, "");
                String totemDescription = TOTEM_TYPE_DESCRIPTIONS.getOrDefault(totemType, totemType); // 獲取描述
                inventory.setItem(slot, createSlotItem("§e圖騰欄 - " + totemDescription, Material.TOTEM_OF_UNDYING, 
                        new String[0]));
            }
            
            // 保存物品欄
            openInventories.put(playerUUID, inventory);
            
            // 告知使用者我們正在初始化圖騰效果
            player.sendMessage("§a[RPGInventory] §e正在初始化圖騰效果...");
            
            // 在玩家打開物品欄後，初始化與加載數據
            RPGInventory plugin = RPGInventory.getInstance();
            
            // 加載玩家數據（使用舊方法）
            plugin.initializePlayerInventory(player);
        }
        
        // 打開物品欄
        player.openInventory(inventory);
        
        // 顯示完成信息
        Bukkit.getScheduler().runTaskLater(RPGInventory.getInstance(), () -> {
            if (player.isOnline() && player.getOpenInventory().getTitle().equals(INVENTORY_TITLE)) {
                player.sendMessage("§a[RPGInventory] §e圖騰效果初始化完成！");
            }
        }, 10L);
    }
    
    /**
     * 設置物品欄邊框
     * @param inventory 物品欄
     */
    private static void setInventoryBorder(Inventory inventory) {
        ItemStack borderItem = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setName("§7")
                .build();
        ItemStack blankSlotFiller = new ItemBuilder(Material.PAPER).setName("§7").build();
        
        // 填充邊框和空白位置
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            boolean isBorder = (i < 9 || i >= INVENTORY_SIZE - 9 || i % 9 == 0 || i % 9 == 8);
            boolean isSpecialSlot = isOrnamentSlot(i) || isTotemSlot(i);

            if (isBorder) {
                inventory.setItem(i, borderItem.clone());
            } else if (!isSpecialSlot && inventory.getItem(i) == null) {
                // 只填充原本為空且非特殊用途的格子
                inventory.setItem(i, blankSlotFiller.clone());
            }
        }
    }
    
    /**
     * 創建欄位物品
     * @param name 名稱
     * @param material 材質
     * @param lore 說明
     * @return 物品
     */
    private static ItemStack createSlotItem(String name, Material material, String... lore) {
        return new ItemBuilder(material)
                .setName(name)
                .setLore(lore)
                .build();
    }
    
    /**
     * 獲取已打開的物品欄
     * @param uuid 玩家 UUID
     * @return 物品欄
     */
    public static Inventory getOpenInventory(UUID uuid) {
        return openInventories.get(uuid);
    }
    
    /**
     * 檢查玩家是否打開了物品欄
     * @param uuid 玩家 UUID
     * @return 是否打開了物品欄
     */
    public static boolean hasOpenInventory(UUID uuid) {
        return openInventories.containsKey(uuid);
    }
    
    /**
     * 檢查是否是 RPG 物品欄
     * @param inventory 物品欄
     * @return 是否是 RPG 物品欄
     */
    public static boolean isRPGInventory(Inventory inventory) {
        return inventory != null && inventory.getViewers().size() > 0 &&
               inventory.getSize() == INVENTORY_SIZE &&
               inventory.getViewers().get(0).getOpenInventory().getTitle().equals(INVENTORY_TITLE);
    }
    
    /**
     * 檢查是否是飾品欄位
     * @param slot 欄位
     * @return 是否是飾品欄位
     */
    public static boolean isOrnamentSlot(int slot) {
        return slot == ORNAMENT_SLOT;
    }
    
    /**
     * 檢查是否是圖騰欄位
     * @param slot 欄位
     * @return 是否是圖騰欄位
     */
    public static boolean isTotemSlot(int slot) {
        for (int totemSlot : TOTEM_SLOTS) {
            if (slot == totemSlot) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 獲取玩家的飾品物品
     * @param uuid 玩家 UUID
     * @return 飾品物品
     */
    public static ItemStack getOrnamentItem(UUID uuid) {
        Inventory inventory = openInventories.get(uuid);
        if (inventory == null) return null;
        
        ItemStack item = inventory.getItem(ORNAMENT_SLOT);
        // 檢查是否是佔位符物品
        if (isPlaceholderItem(item)) {
            return null;
        }
        return item;
    }
    
    /**
     * 設置玩家的飾品物品
     * @param uuid 玩家 UUID
     * @param item 飾品物品
     */
    public static void setOrnamentItem(UUID uuid, ItemStack item) {
        Inventory inventory = openInventories.get(uuid);
        if (inventory == null) return;
        
        if (item == null) {
            inventory.setItem(ORNAMENT_SLOT, createSlotItem("§d飾品欄", Material.NETHER_STAR, 
                    "§c移除物品"));
        } else {
            inventory.setItem(ORNAMENT_SLOT, item);
        }
    }
    
    /**
     * 獲取所有圖騰欄位
     * @return 圖騰欄位數組
     */
    public static int[] getTotemSlots() {
        return TOTEM_SLOTS;
    }
    
    /**
     * 獲取玩家的圖騰物品
     * @param uuid 玩家 UUID
     * @param slotId 欄位 ID
     * @return 圖騰物品
     */
    public static ItemStack getTotemItem(UUID uuid, int slotId) {
        Inventory inventory = openInventories.get(uuid);
        if (inventory == null) return null;
        
        ItemStack item = inventory.getItem(slotId);
        // 檢查是否是佔位符物品
        if (isPlaceholderItem(item)) {
            return null;
        }
        return item;
    }
    
    /**
     * 檢查物品是否可以放在特定圖騰槽位
     * @param slot 欄位ID
     * @param item 物品
     * @return 是否允許放置
     */
    public static boolean canPlaceTotemInSlot(int slot, ItemStack item) {
        // 如果不是圖騰欄位，無需檢查
        if (!isTotemSlot(slot)) return false;
        
        // 如果物品為空，允許放置（相當於取出物品）
        if (item == null) return true;
        
        // 獲取該槽位允許的圖騰類型
        String allowedType = SLOT_TOTEM_TYPES.get(slot);
        if (allowedType == null) return false;
        
        try {
            // 獲取物品的ID
            String itemId = getExecutableItemId(item);
            
            // 如果無法獲取ID，不允許放置
            if (itemId == null) return false;
            
            // 檢查物品ID是否以允許的前綴開頭
            return itemId.startsWith(allowedType);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 從物品獲取ExecutableItems的ID
     * @param item 物品
     * @return 物品ID或null
     */
    private static String getExecutableItemId(ItemStack item) {
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
            // 忽略錯誤
        }
        
        return null;
    }
    
    /**
     * 設置玩家的圖騰物品
     * @param uuid 玩家UUID
     * @param slotId 欄位ID
     * @param item 圖騰物品
     * @return 是否成功設置
     */
    public static boolean setTotemItem(UUID uuid, int slotId, ItemStack item) {
        Inventory inventory = openInventories.get(uuid);
        if (inventory == null) return false;
        
        // 檢查是否是有效的圖騰欄位
        boolean validSlot = false;
        for (int slot : TOTEM_SLOTS) {
            if (slot == slotId) {
                validSlot = true;
                break;
            }
        }
        
        if (!validSlot) return false;
        
        // 檢查物品是否可以放在這個槽位
        if (item != null && !canPlaceTotemInSlot(slotId, item)) {
            // 如果不允許放置，返回失敗
            return false;
        }
        
        if (item == null) {
            String totemType = SLOT_TOTEM_TYPES.getOrDefault(slotId, "");
            String totemDescription = TOTEM_TYPE_DESCRIPTIONS.getOrDefault(totemType, totemType); // 新增：獲取描述
            inventory.setItem(slotId, createSlotItem("§e圖騰欄 - " + totemDescription, Material.TOTEM_OF_UNDYING, 
                    new String[0]));
        } else {
            inventory.setItem(slotId, item);
        }
        
        return true;
    }
    
    /**
     * 獲取物品欄標題
     * @return 物品欄標題
     */
    public static String getInventoryTitle() {
        return INVENTORY_TITLE;
    }
    
    /**
     * 獲取物品欄大小
     * @return 物品欄大小
     */
    public static int getInventorySize() {
        return INVENTORY_SIZE;
    }
    
    /**
     * 獲取飾品欄位置
     * @return 飾品欄位置
     */
    public static int getOrnamentSlot() {
        return ORNAMENT_SLOT;
    }
    
    /**
     * 設置物品欄基本框架（邊框和空槽位）
     * @param inventory 物品欄
     */
    public static void setupInventoryBase(Inventory inventory) {
        // 設置邊框
        setInventoryBorder(inventory);
        
        // 設置飾品欄位
        inventory.setItem(ORNAMENT_SLOT, createSlotItem("§d飾品欄", Material.NETHER_STAR, 
                "§c移除物品"));
        
        // 設置圖騰欄位
        for (int slot : TOTEM_SLOTS) {
            String totemType = SLOT_TOTEM_TYPES.getOrDefault(slot, "");
            String totemDescription = TOTEM_TYPE_DESCRIPTIONS.getOrDefault(totemType, totemType); // 新增：獲取描述
            inventory.setItem(slot, createSlotItem("§e圖騰欄 - " + totemDescription, Material.TOTEM_OF_UNDYING, 
                    new String[0]));
        }
    }
    
    /**
     * 添加已打開的物品欄（不顯示給玩家）
     * @param uuid 玩家UUID
     * @param inventory 物品欄
     */
    public static void addOpenInventory(UUID uuid, Inventory inventory) {
        openInventories.put(uuid, inventory);
    }
    
    /**
     * 檢查物品是否為佔位符物品
     * @param item 物品
     * @return 是否是佔位符物品
     */
    private static boolean isPlaceholderItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        // 檢查是否有物品元數據和顯示名稱
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String name = item.getItemMeta().getDisplayName();
            
            // 檢查飾品欄佔位符
            if (name.equals("§d飾品欄") && item.getType() == Material.NETHER_STAR) {
                return true;
            }
            
            // 檢查圖騰欄佔位符
            if (name.startsWith("§e圖騰欄") && item.getType() == Material.TOTEM_OF_UNDYING) {
                // 檢查是否包含預期的圖騰類型名稱
                for (String descPart : TOTEM_TYPE_DESCRIPTIONS.values()) {
                    if (name.contains(descPart)) {
                        return true;
                    }
                }
                
                // 後備檢查，如果描述中不含類型關鍵字但名稱符合基本前綴
                if (name.startsWith("§e圖騰欄 - ")) return true;
            }

            // 新增：檢查紙張佔位符
            if (item.getType() == Material.PAPER && name.equals("§7")) {
                return true;
            }
        }
        
        return false;
    }
} 