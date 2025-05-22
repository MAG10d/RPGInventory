package org.MAGd.rPGInventory.listeners;

import org.MAGd.rPGInventory.RPGInventory;
// import org.MAGd.rPGInventory.data.PlayerData; // 移除未使用的導入
import org.MAGd.rPGInventory.gui.InventoryGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Optional;

public class InventoryListeners implements Listener {

    private final RPGInventory plugin;

    public InventoryListeners(RPGInventory plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory();
        Inventory bottomInventory = event.getView().getBottomInventory();

        // 檢查頂部背包是否是我們的 RPG 物品欄
        if (!InventoryGUI.isRPGInventory(topInventory)) return;

        // 情況1: 點擊的是玩家自己的背包 (底部背包)
        if (bottomInventory.equals(clickedInventory)) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) { // Shift-Click
                ItemStack itemToMove = event.getCurrentItem();
                if (itemToMove != null && itemToMove.getType() != Material.AIR) {
                    String itemId = getItemId(itemToMove);
                    if (!itemId.isEmpty()) { // 是 ExecutableItem
                        if (isExecutableItemIdPresentInRpgInv(topInventory, itemToMove, -1)) {
                            player.sendMessage("§c你的RPG背包中已經有相同ID的物品了，無法快速轉移！");
                            event.setCancelled(true);
                            return;
                        }
                        // 如果不重複，並且目標槽是特殊槽，也只放一個 (如果適用)
                        // 這個邏輯在下面處理特殊槽位時覆蓋
                    }
                }
            }
            // 對於玩家背包內的其他所有點擊 (包括右鍵拿起等)，不進行任何操作，允許默認行為
            return;
        }

        // 情況2: 點擊的是 RPGInventory GUI (頂部背包)
        if (topInventory.equals(clickedInventory)) {
            int slot = event.getRawSlot(); // rawSlot 是相對於頂部背包的
            ItemStack cursor = event.getCursor();
            ItemStack currentItem = event.getCurrentItem();
            boolean isPlaceholder = InventoryGUI.isPlaceholderItem(currentItem);

            // 防止點擊邊框 (slot < 0 已經由 topInventory.equals(clickedInventory) 隱含排除)
            // 邊框是 0-8, 45-53, 以及每行的0和8
            if (slot < 9 || slot >= topInventory.getSize() - 9 || slot % 9 == 0 || slot % 9 == 8) {
                event.setCancelled(true);
                return;
            }
            
            // 通用佔位符保護：阻止拿起任何類型的佔位符
            if (isPlaceholder && (event.getAction() == InventoryAction.PICKUP_ALL || 
                                 event.getAction() == InventoryAction.PICKUP_HALF || 
                                 event.getAction() == InventoryAction.PICKUP_ONE || 
                                 event.getAction() == InventoryAction.PICKUP_SOME || 
                                 event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY || // Shift-click out of placeholder
                                 event.getAction() == InventoryAction.COLLECT_TO_CURSOR ||
                                 event.getAction() == InventoryAction.SWAP_WITH_CURSOR)) {
                event.setCancelled(true);
                return;
            }

            // 飾品欄位邏輯
            if (InventoryGUI.isOrnamentSlot(slot)) {
                handleOrnamentSlotClick(event, player, slot, cursor, currentItem, isPlaceholder);
                return;
            }
            
            // 圖騰欄位邏輯
            if (InventoryGUI.isTotemSlot(slot)) {
                handleTotemSlotClick(event, player, slot, cursor, currentItem, isPlaceholder);
                return;
            }
            
            // 其他非特殊欄位 (已經被PAPER填滿的)
            // isPlaceholderItem 會將 PAPER 視為占位符，上面的通用保護已處理
            // 如果不是占位符 (理論上不應發生，除非 PAPER 沒填上)，則取消事件
            if (!isPlaceholder) {
                 event.setCancelled(true);
            }
            return; // 完成對頂部背包的處理
        }
    }

    private void handleOrnamentSlotClick(InventoryClickEvent event, Player player, int slot, ItemStack cursor, ItemStack currentItem, boolean isPlaceholderCurrent) {
        // 放置物品邏輯
        if (cursor != null && cursor.getType() != Material.AIR) {
            if (plugin.hasExecutableItems()) {
                try {
                    String itemIdFromCursor = getItemId(cursor);
                    // 暫時移除 isAllowedOrnament 檢查，你需要後續實現 plugin.isAllowedOrnament(itemId)
                    if (itemIdFromCursor.isEmpty() /* || !plugin.isAllowedOrnament(itemIdFromCursor) */) { 
                        player.sendMessage("§c只能放置有效的飾品！(ID不可為空)");
                        event.setCancelled(true);
                        return;
                    }
                    // 新增檢查：如果槽位中已有物品，且ID與游標物品相同，則阻止 (飾品也需要)
                    if (!isPlaceholderCurrent) { 
                        String idOfCurrentItemInSlot = getItemId(currentItem);
                        if (!idOfCurrentItemInSlot.isEmpty() && itemIdFromCursor.equals(idOfCurrentItemInSlot)) {
                            player.sendMessage("§c該飾品已存在於此槽位中，無法重複放置！");
                            event.setCancelled(true);
                            return;
                        }
                    }
                    if (isExecutableItemIdPresentInRpgInv(event.getInventory(), cursor, slot)) {
                        player.sendMessage("§c你的RPG背包中其他槽位已經有相同ID的物品了！");
                        event.setCancelled(true);
                        return;
                    }

                    event.setCancelled(true);
                    ItemStack itemToPlaceInSlot = cursor.clone();
                    itemToPlaceInSlot.setAmount(1);

                    ItemStack originalItemInSlot = (!isPlaceholderCurrent && currentItem != null) ? currentItem.clone() : null;
                    
                    ItemStack remainingOnOriginalCursor = cursor.clone();
                    remainingOnOriginalCursor.setAmount(cursor.getAmount() - 1);
                    if (remainingOnOriginalCursor.getAmount() <= 0) {
                        remainingOnOriginalCursor = null;
                    }

                    event.getInventory().setItem(slot, itemToPlaceInSlot);

                    final ItemStack finalItemForCursor;
                    if (originalItemInSlot != null) { // 如果是替換，原槽位物品上游標
                        finalItemForCursor = originalItemInSlot;
                    } else { // 否則，是原游標物品減少一個後的剩餘部分上游標
                        finalItemForCursor = remainingOnOriginalCursor;
                    }

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.setItemOnCursor(finalItemForCursor);
                            plugin.savePlayerInventory(player);
                            // 暫時註解，你需要後續實現 plugin.getOrnamentEffectListener().updateOrnamentEffects(player);
                            // plugin.getOrnamentEffectListener().updateOrnamentEffects(player); 
                            player.sendMessage("§a飾品已成功放入/替換！");
                        }
                    }.runTask(plugin);

                } catch (Exception e) {
                    plugin.getLogger().warning("處理飾品放置時出錯: " + e.getMessage());
                    event.setCancelled(true);
                }
            } else {
                 player.sendMessage("§cExecutableItems 未加載，無法放置飾品。");
                 event.setCancelled(true);
            }
        } 
        // 取出物品邏輯 (點擊非占位符物品且游標為空)
        else if (cursor == null || cursor.getType() == Material.AIR) {
            if (currentItem != null && !isPlaceholderCurrent) {
                // 允許默認的取出，但之後要放回占位符並保存
                 new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (event.getInventory().getItem(slot) == null || event.getInventory().getItem(slot).getType() == Material.AIR) {
                            InventoryGUI.setOrnamentItem(player.getUniqueId(), null); // 放回飾品占位符
                            player.sendMessage("§e飾品已取出，槽位已重置。");
                        }
                        plugin.savePlayerInventory(player);
                        // 暫時註解，你需要後續實現 plugin.getOrnamentEffectListener().updateOrnamentEffects(player);
                        // plugin.getOrnamentEffectListener().updateOrnamentEffects(player);
                    }
                }.runTaskLater(plugin, 1L);
            } else if (isPlaceholderCurrent) {
                event.setCancelled(true); // 防止與占位符進行不期望的交互 (例如用空手右鍵點擊占位符)
            }
        }
    }

    private void handleTotemSlotClick(InventoryClickEvent event, Player player, int slot, ItemStack cursor, ItemStack currentItem, boolean isPlaceholderCurrent) {
        // 新增：如果當前槽位中的物品是佔位符，進行嚴格保護
        if (isPlaceholderCurrent) {
            // 允許的操作：鼠標上有物品 (cursor != null 且非 AIR)，並且不是 Shift-Click (MOVE_TO_OTHER_INVENTORY) 將佔位符移出。
            // 換句話說，如果鼠標為空，或鼠標有物品但嘗試 Shift-Click 移出佔位符，都應取消。
            boolean tryingToPlaceWithCursor = (cursor != null && cursor.getType() != Material.AIR);
            
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) { // Shift-clicking the placeholder itself
                event.setCancelled(true);
                // player.sendMessage("§c[DEBUG] §e無法 Shift+Click 取出固定槽位的佔位符。"); // 可選調試信息
                return;
            }
            
            if (!tryingToPlaceWithCursor) { // 空手點擊或任何非放置的交互
                event.setCancelled(true);
                // player.sendMessage("§c[DEBUG] §e此為固定槽位，點擊佔位符無效。"); // 可選調試信息
                return;
            }
            // 如果鼠標上有物品，並且不是Shift-Click佔位符本身，則允許繼續執行下面的放置邏輯 (會替換佔位符)
        }

        // 放置物品邏輯
        if (cursor != null && cursor.getType() != Material.AIR) {
            if (plugin.hasExecutableItems()) {
                try {
                    String itemIdFromCursor = getItemId(cursor);
                    if (itemIdFromCursor.isEmpty() || !plugin.isAllowedTotem(itemIdFromCursor) || !InventoryGUI.canPlaceTotemInSlot(slot, cursor)) {
                        player.sendMessage("§c只能放置有效的圖騰，且類型需匹配槽位！");
                        event.setCancelled(true);
                        return;
                    }

                    // 新增檢查：如果槽位中已有物品，且ID與游標物品相同，則阻止
                    if (!isPlaceholderCurrent) { // currentItem (槽中物品) 不是占位符
                        String idOfCurrentItemInSlot = getItemId(currentItem);
                        if (!idOfCurrentItemInSlot.isEmpty() && itemIdFromCursor.equals(idOfCurrentItemInSlot)) {
                            player.sendMessage("§c該圖騰已存在於此槽位中，無法重複放置！");
                            event.setCancelled(true);
                            return;
                        }
                    }
                    
                    if (isExecutableItemIdPresentInRpgInv(event.getInventory(), cursor, slot)) {
                        player.sendMessage("§c你的RPG背包中其他槽位已經有相同ID的物品了！");
                        event.setCancelled(true);
                        return;
                    }

                    event.setCancelled(true);
                    ItemStack itemToPlaceInSlot = cursor.clone();
                    itemToPlaceInSlot.setAmount(1);
                    
                    ItemStack originalItemInSlot = (!isPlaceholderCurrent && currentItem != null) ? currentItem.clone() : null;

                    ItemStack remainingOnOriginalCursor = cursor.clone();
                    remainingOnOriginalCursor.setAmount(cursor.getAmount() - 1);
                    if (remainingOnOriginalCursor.getAmount() <= 0) {
                        remainingOnOriginalCursor = null;
                    }

                    event.getInventory().setItem(slot, itemToPlaceInSlot);

                    final ItemStack finalItemForCursor;
                    if (originalItemInSlot != null) { // 如果是替換，原槽位物品上游標
                        finalItemForCursor = originalItemInSlot;
                    } else { // 否則，是原游標物品減少一個後的剩餘部分上游標
                        finalItemForCursor = remainingOnOriginalCursor;
                    }

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.setItemOnCursor(finalItemForCursor);
                            plugin.savePlayerInventory(player);
                            plugin.getLogger().info("圖騰放入/替換(" + getItemId(itemToPlaceInSlot) + ")，立即更新效果");
                            plugin.getTotemEffectListener().updateTotemLoopTasks(player);
                            player.sendMessage("§a[圖騰] §e圖騰已成功放入/替換！");
                        }
                    }.runTask(plugin);

                } catch (Exception e) {
                    plugin.getLogger().warning("處理圖騰放置時出錯: " + e.getMessage());
                    event.setCancelled(true);
                }
            } else {
                player.sendMessage("§cExecutableItems 未加載，無法放置圖騰。");
                event.setCancelled(true);
            }
        } 
        // 取出物品邏輯
        else if (cursor == null || cursor.getType() == Material.AIR) {
            // 由於上面的新邏輯，如果 isPlaceholderCurrent 為 true，基本不會執行到這裡的取出邏輯了
            // 但保留原有 !isPlaceholderCurrent 檢查以防萬一
            if (currentItem != null && !isPlaceholderCurrent) {
                // 允許默認的取出，但之後要放回占位符並保存
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (event.getInventory().getItem(slot) == null || event.getInventory().getItem(slot).getType() == Material.AIR) {
                            InventoryGUI.setTotemItem(player.getUniqueId(), slot, null); // 放回圖騰占位符
                            player.sendMessage("§a[圖騰] §e圖騰已取出，槽位已重置");
                        }
                        plugin.savePlayerInventory(player);
                        plugin.getTotemEffectListener().updateTotemLoopTasks(player);
                    }
                }.runTaskLater(plugin, 1L);
            } 
            // 這個 else if 分支理論上因為頂部的 isPlaceholderCurrent 檢查而不再需要，
            // 但保留它不會造成傷害，只是變得冗余。
            // else if (isPlaceholderCurrent) { 
            //    event.setCancelled(true); 
            // }
        }
    }

    /**
     * 獲取 ExecutableItems 物品的 ID
     * @param item 物品
     * @return 物品 ID，如果不是 ExecutableItems 則返回空字符串
     */
    private String getItemId(ItemStack item) {
        try {
            Class<?> apiClass = Class.forName("com.ssomar.score.api.executableitems.ExecutableItemsAPI");
            Object manager = apiClass.getMethod("getExecutableItemsManager").invoke(null);
            
            // 獲取 getExecutableItem 方法
            Object result = manager.getClass().getMethod("getExecutableItem", ItemStack.class).invoke(manager, item);
            
            // 判斷結果是否為空
            boolean isEmpty = (boolean) Optional.class.getMethod("isEmpty").invoke(result);
            
            if (!isEmpty) {
                // 獲取 ExecutableItem 對象
                Object eiItem = result.getClass().getMethod("get").invoke(result);
                
                // 獲取 ID
                return (String) eiItem.getClass().getMethod("getId").invoke(eiItem);
            }
        } catch (Exception e) {
            // 處理反射錯誤
            plugin.getLogger().warning("獲取 ExecutableItem ID 時發生錯誤: " + e.getMessage());
        }
        
        return "";
    }

    private boolean isExecutableItemIdPresentInRpgInv(Inventory rpgInventory, ItemStack itemToCheck, int slotToIgnoreIfReplacing) {
        if (itemToCheck == null || itemToCheck.getType() == Material.AIR) {
            return false;
        }
        String newItemId = getItemId(itemToCheck); 
        if (newItemId.isEmpty()) {
            return false; 
        }

        // Check ornament slot
        int ornamentSlot = InventoryGUI.getOrnamentSlot();
        if (ornamentSlot != slotToIgnoreIfReplacing) { 
            ItemStack itemInOrnamentSlot = rpgInventory.getItem(ornamentSlot);
            if (itemInOrnamentSlot != null && !InventoryGUI.isPlaceholderItem(itemInOrnamentSlot)) { 
                if (newItemId.equals(getItemId(itemInOrnamentSlot))) {
                    return true; 
                }
            }
        }

        // Check totem slots
        for (int totemSlotInGUI : InventoryGUI.getTotemSlots()) {
            if (totemSlotInGUI != slotToIgnoreIfReplacing) { 
                ItemStack itemInTotemSlot = rpgInventory.getItem(totemSlotInGUI);
                if (itemInTotemSlot != null && !InventoryGUI.isPlaceholderItem(itemInTotemSlot)) {
                    if (newItemId.equals(getItemId(itemInTotemSlot))) {
                        return true; 
                    }
                }
            }
        }
        return false; 
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        // 檢查是否是我們的 RPG 物品欄
        if (!InventoryGUI.isRPGInventory(event.getInventory())) return;
        
        Player player = (Player) event.getPlayer();
        
        // 保存玩家的物品欄數據
        plugin.savePlayerInventory(player);
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 玩家進入伺服器時，可以在這裡做一些處理，例如檢查是否有新手禮包等
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // 如果玩家有打開的物品欄，保存數據
        if (InventoryGUI.hasOpenInventory(player.getUniqueId())) {
            plugin.savePlayerInventory(player);
        }
    }
}