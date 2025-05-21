package org.MAGd.rPGInventory.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.MAGd.rPGInventory.RPGInventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * SQLite 數據庫管理器
 */
public class SQLiteManager extends AbstractDatabaseManager {
    
    /**
     * 構造函數
     * @param plugin 插件實例
     */
    public SQLiteManager(RPGInventory plugin) {
        super(plugin);
    }
    
    @Override
    public boolean initialize() {
        File dataFolder = new File(plugin.getDataFolder(), "database");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        File databaseFile = new File(dataFolder, "rpginventory.db");
        String jdbcUrl = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
        
        try {
            // 手動註冊 SQLite 驅動
            try {
                // 嘗試原始驅動類名
                try {
                    Class.forName("org.sqlite.JDBC");
                    plugin.getLogger().info("標準 SQLite JDBC 驅動註冊成功");
                } catch (ClassNotFoundException e) {
                    // 如果標準驅動類名不存在，嘗試重定位後的驅動類名
                    try {
                        Class.forName("org.MAGd.rPGInventory.libs.sqlite.JDBC");
                        plugin.getLogger().info("重定位 SQLite JDBC 驅動註冊成功");
                    } catch (ClassNotFoundException ex) {
                        plugin.getLogger().severe("無法註冊 SQLite JDBC 驅動: " + ex.getMessage());
                        return false;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("註冊 SQLite JDBC 驅動發生未知錯誤: " + e.getMessage());
                return false;
            }
            
            // 配置 HikariCP
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            // 顯式設置驅動類名
            config.setDriverClassName("org.sqlite.JDBC");
            config.setPoolName("RPGInventorySQLitePool");
            
            // SQLite 連接池設置
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            
            // 創建連接池
            dataSource = new HikariDataSource(config);
            
            // 創建表
            try (Connection conn = getConnection();
                 Statement statement = conn.createStatement()) {
                // 玩家飾品表
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS ornaments (" +
                        "player_uuid VARCHAR(36) PRIMARY KEY, " +
                        "item_data TEXT NOT NULL" +
                        ");"
                );
                
                // 玩家圖騰表
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS totems (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "player_uuid VARCHAR(36) NOT NULL, " +
                        "slot_id INTEGER NOT NULL, " +
                        "item_data TEXT NOT NULL, " +
                        "UNIQUE(player_uuid, slot_id)" +
                        ");"
                );
            }
            
            plugin.getLogger().info("SQLite 數據庫連接池初始化成功！");
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "無法初始化 SQLite 數據庫連接池: " + e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public void saveOrnament(Connection conn, UUID playerUUID, ItemStack ornament) throws SQLException, IOException {
        String sql = "INSERT OR REPLACE INTO ornaments (player_uuid, item_data) VALUES (?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, playerUUID.toString());
            statement.setString(2, serializeItemStack(ornament));
            statement.executeUpdate();
        }
    }
    
    @Override
    public void deleteOrnament(Connection conn, UUID playerUUID) throws SQLException {
        String sql = "DELETE FROM ornaments WHERE player_uuid = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, playerUUID.toString());
            statement.executeUpdate();
        }
    }
    
    @Override
    public void deleteTotems(Connection conn, UUID playerUUID) throws SQLException {
        String sql = "DELETE FROM totems WHERE player_uuid = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, playerUUID.toString());
            statement.executeUpdate();
        }
    }
    
    @Override
    public void saveTotems(Connection conn, UUID playerUUID, Map<Integer, ItemStack> totems) throws SQLException, IOException {
        String sql = "INSERT INTO totems (player_uuid, slot_id, item_data) VALUES (?, ?, ?)";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            for (Map.Entry<Integer, ItemStack> entry : totems.entrySet()) {
                if (entry.getValue() != null) {
                    statement.setString(1, playerUUID.toString());
                    statement.setInt(2, entry.getKey());
                    statement.setString(3, serializeItemStack(entry.getValue()));
                    statement.executeUpdate();
                }
            }
        }
    }
} 