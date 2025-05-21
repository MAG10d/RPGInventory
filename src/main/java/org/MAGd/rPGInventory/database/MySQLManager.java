package org.MAGd.rPGInventory.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.MAGd.rPGInventory.RPGInventory;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * MySQL 數據庫管理器
 */
public class MySQLManager extends AbstractDatabaseManager {
    
    private String host, database, username, password;
    private int port;
    
    /**
     * 構造函數
     * @param plugin 插件實例
     */
    public MySQLManager(RPGInventory plugin) {
        super(plugin);
        loadConfig();
    }
    
    /**
     * 從配置文件加載數據庫配置
     */
    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        host = config.getString("database.mysql.host", "localhost");
        port = config.getInt("database.mysql.port", 3306);
        database = config.getString("database.mysql.database", "rpginventory");
        username = config.getString("database.mysql.username", "root");
        password = config.getString("database.mysql.password", "password");
    }
    
    @Override
    public boolean initialize() {
        try {
            // 手動註冊 MySQL 驅動
            try {
                Class.forName("org.MAGd.rPGInventory.libs.mysql.cj.jdbc.Driver");
                plugin.getLogger().info("MySQL JDBC 驅動註冊成功");
            } catch (ClassNotFoundException e) {
                plugin.getLogger().severe("無法註冊 MySQL JDBC 驅動: " + e.getMessage());
                return false;
            }
            
            // 配置 HikariCP
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + 
                           "?useSSL=false&autoReconnect=true&useUnicode=true&characterEncoding=UTF-8");
            // 不再設置驅動類名，讓 HikariCP 自動檢測
            config.setUsername(username);
            config.setPassword(password);
            config.setPoolName("RPGInventoryMySQLPool");
            
            // MySQL 連接池設置
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
                        "CREATE TABLE IF NOT EXISTS `ornaments` (" +
                        "`player_uuid` VARCHAR(36) PRIMARY KEY, " +
                        "`item_data` TEXT NOT NULL" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
                );
                
                // 玩家圖騰表
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS `totems` (" +
                        "`id` INT AUTO_INCREMENT PRIMARY KEY, " +
                        "`player_uuid` VARCHAR(36) NOT NULL, " +
                        "`slot_id` INT NOT NULL, " +
                        "`item_data` TEXT NOT NULL, " +
                        "UNIQUE KEY `player_slot` (`player_uuid`, `slot_id`)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
                );
            }
            
            plugin.getLogger().info("MySQL 數據庫連接池初始化成功！");
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "無法初始化 MySQL 數據庫連接池: " + e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public void saveOrnament(Connection conn, UUID playerUUID, ItemStack ornament) throws SQLException, IOException {
        String sql = "INSERT INTO ornaments (player_uuid, item_data) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE item_data = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            String serializedItem = serializeItemStack(ornament);
            statement.setString(1, playerUUID.toString());
            statement.setString(2, serializedItem);
            statement.setString(3, serializedItem);
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