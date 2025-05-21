package org.MAGd.rPGInventory.database;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

/**
 * 數據庫管理器接口
 */
public interface DatabaseManager {
    
    /**
     * 初始化數據庫
     * @return 是否成功
     */
    boolean initialize();
    
    /**
     * 關閉數據庫連接
     */
    void close();
    
    /**
     * 保存玩家的物品欄數據
     * @param playerUUID 玩家UUID
     * @param ornament 飾品欄物品
     * @param totems 圖騰欄物品
     * @return 是否成功
     */
    boolean saveInventory(UUID playerUUID, ItemStack ornament, Map<Integer, ItemStack> totems);
    
    /**
     * 加載玩家的飾品欄物品
     * @param playerUUID 玩家UUID
     * @return 飾品欄物品
     */
    ItemStack loadOrnament(UUID playerUUID);
    
    /**
     * 加載玩家的圖騰欄物品
     * @param playerUUID 玩家UUID
     * @return 圖騰欄物品
     */
    Map<Integer, ItemStack> loadTotems(UUID playerUUID);
    
    /**
     * 檢查玩家是否有保存的數據
     * @param playerUUID 玩家UUID
     * @return 是否有數據
     */
    boolean hasData(UUID playerUUID);
} 