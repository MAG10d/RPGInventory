package org.MAGd.rPGInventory.database;

import org.MAGd.rPGInventory.RPGInventory;

import java.util.logging.Level;

/**
 * 數據庫工廠類，用於創建數據庫管理器實例
 */
public class DatabaseFactory {
    
    /**
     * 獲取數據庫管理器實例
     * @param plugin 插件實例
     * @return 數據庫管理器，如果初始化失敗返回 null
     */
    public static DatabaseManager getDatabaseManager(RPGInventory plugin) {
        String dbType = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();
        DatabaseManager manager = null;
        
        plugin.getLogger().info("正在初始化數據庫...");
        
        try {
            switch (dbType) {
                case "mysql":
                    plugin.getLogger().info("配置為使用 MySQL 數據庫");
                    manager = new MySQLManager(plugin);
                    break;
                case "sqlite":
                default:
                    plugin.getLogger().info("配置為使用 SQLite 數據庫");
                    manager = new SQLiteManager(plugin);
                    break;
            }
            
            if (manager != null) {
                if (manager.initialize()) {
                    plugin.getLogger().info("數據庫初始化成功");
                    return manager;
                } else {
                    plugin.getLogger().severe("數據庫初始化失敗");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "建立數據庫管理器時發生錯誤: " + e.getMessage(), e);
        }
        
        // 如果 MySQL 失敗，嘗試回退到 SQLite
        if (manager == null || dbType.equals("mysql")) {
            plugin.getLogger().warning("嘗試回退到 SQLite 數據庫");
            try {
                manager = new SQLiteManager(plugin);
                if (manager.initialize()) {
                    plugin.getLogger().info("SQLite 備用數據庫初始化成功");
                    return manager;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "回退到 SQLite 失敗: " + e.getMessage(), e);
            }
        }
        
        plugin.getLogger().severe("無法初始化任何數據庫，插件可能無法正常工作");
        return null;
    }
} 