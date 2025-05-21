package org.MAGd.rPGInventory.database;

import com.zaxxer.hikari.HikariDataSource;
import org.MAGd.rPGInventory.RPGInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * 數據庫管理器抽象基類，提供共用功能實現
 */
public abstract class AbstractDatabaseManager implements DatabaseManager {
    
    protected final RPGInventory plugin;
    protected HikariDataSource dataSource;
    
    // 重試嘗試次數
    protected static final int MAX_RETRY_ATTEMPTS = 3;
    // 重試延遲時間（毫秒）
    protected static final long RETRY_DELAY_MS = 500;
    
    /**
     * 構造函數
     * @param plugin 插件實例
     */
    public AbstractDatabaseManager(RPGInventory plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 獲取數據庫連接
     * @return 數據庫連接
     * @throws SQLException SQL異常
     */
    protected Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("已關閉數據庫連接池");
        }
    }
    
    @Override
    public boolean saveInventory(UUID playerUUID, ItemStack ornament, Map<Integer, ItemStack> totems) {
        // 嘗試重試機制
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try (Connection conn = getConnection()) {
                // 開始事務
                conn.setAutoCommit(false);
                
                try {
                    // 處理飾品
                    if (ornament != null) {
                        saveOrnament(conn, playerUUID, ornament);
                    } else {
                        deleteOrnament(conn, playerUUID);
                    }
                    
                    // 處理圖騰
                    deleteTotems(conn, playerUUID);
                    if (totems != null && !totems.isEmpty()) {
                        saveTotems(conn, playerUUID, totems);
                    }
                    
                    // 提交事務
                    conn.commit();
                    return true;
                } catch (SQLException | IOException e) {
                    // 回滾事務
                    try {
                        conn.rollback();
                    } catch (SQLException rollbackEx) {
                        plugin.getLogger().log(Level.SEVERE, "回滾事務失敗: " + rollbackEx.getMessage(), rollbackEx);
                    }
                    
                    if (attempt == MAX_RETRY_ATTEMPTS) {
                        plugin.getLogger().log(Level.SEVERE, "保存物品欄數據失敗 (嘗試 " + attempt + "/" + MAX_RETRY_ATTEMPTS + "): " + e.getMessage(), e);
                        return false;
                    } else {
                        plugin.getLogger().log(Level.WARNING, "保存物品欄數據失敗，正在重試 (" + attempt + "/" + MAX_RETRY_ATTEMPTS + "): " + e.getMessage());
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } finally {
                    try {
                        conn.setAutoCommit(true);
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.WARNING, "重置自動提交失敗: " + e.getMessage(), e);
                    }
                }
            } catch (SQLException e) {
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    plugin.getLogger().log(Level.SEVERE, "無法獲取數據庫連接 (嘗試 " + attempt + "/" + MAX_RETRY_ATTEMPTS + "): " + e.getMessage(), e);
                    return false;
                } else {
                    plugin.getLogger().log(Level.WARNING, "無法獲取數據庫連接，正在重試 (" + attempt + "/" + MAX_RETRY_ATTEMPTS + "): " + e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * 保存飾品
     * @param conn 數據庫連接
     * @param playerUUID 玩家UUID
     * @param ornament 飾品
     * @throws SQLException SQL異常
     * @throws IOException IO異常
     */
    public abstract void saveOrnament(Connection conn, UUID playerUUID, ItemStack ornament) throws SQLException, IOException;
    
    /**
     * 刪除飾品
     * @param conn 數據庫連接
     * @param playerUUID 玩家UUID
     * @throws SQLException SQL異常
     */
    public abstract void deleteOrnament(Connection conn, UUID playerUUID) throws SQLException;
    
    /**
     * 刪除圖騰
     * @param conn 數據庫連接
     * @param playerUUID 玩家UUID
     * @throws SQLException SQL異常
     */
    public abstract void deleteTotems(Connection conn, UUID playerUUID) throws SQLException;
    
    /**
     * 保存圖騰
     * @param conn 數據庫連接
     * @param playerUUID 玩家UUID
     * @param totems 圖騰
     * @throws SQLException SQL異常
     * @throws IOException IO異常
     */
    public abstract void saveTotems(Connection conn, UUID playerUUID, Map<Integer, ItemStack> totems) throws SQLException, IOException;
    
    @Override
    public ItemStack loadOrnament(UUID playerUUID) {
        String sql = "SELECT item_data FROM ornaments WHERE player_uuid = ?";
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try (Connection conn = getConnection();
                 PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, playerUUID.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        String serializedItem = resultSet.getString("item_data");
                        return deserializeItemStack(serializedItem);
                    }
                    return null;
                } catch (IOException | ClassNotFoundException e) {
                    if (attempt == MAX_RETRY_ATTEMPTS) {
                        plugin.getLogger().log(Level.SEVERE, "加載飾品數據失敗: " + e.getMessage(), e);
                    } else {
                        plugin.getLogger().log(Level.WARNING, "加載飾品數據失敗，正在重試 (" + attempt + "/" + MAX_RETRY_ATTEMPTS + "): " + e.getMessage());
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        continue;
                    }
                }
            } catch (SQLException e) {
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    plugin.getLogger().log(Level.SEVERE, "加載飾品數據時數據庫錯誤: " + e.getMessage(), e);
                } else {
                    plugin.getLogger().log(Level.WARNING, "加載飾品數據時數據庫錯誤，正在重試 (" + attempt + "/" + MAX_RETRY_ATTEMPTS + "): " + e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
            }
        }
        
        return null;
    }
    
    @Override
    public Map<Integer, ItemStack> loadTotems(UUID playerUUID) {
        Map<Integer, ItemStack> totems = new HashMap<>();
        String sql = "SELECT slot_id, item_data FROM totems WHERE player_uuid = ?";
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try (Connection conn = getConnection();
                 PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, playerUUID.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        int slotId = resultSet.getInt("slot_id");
                        String serializedItem = resultSet.getString("item_data");
                        totems.put(slotId, deserializeItemStack(serializedItem));
                    }
                    return totems;
                } catch (IOException | ClassNotFoundException e) {
                    if (attempt == MAX_RETRY_ATTEMPTS) {
                        plugin.getLogger().log(Level.SEVERE, "加載圖騰數據失敗: " + e.getMessage(), e);
                    } else {
                        plugin.getLogger().log(Level.WARNING, "加載圖騰數據失敗，正在重試 (" + attempt + "/" + MAX_RETRY_ATTEMPTS + "): " + e.getMessage());
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        continue;
                    }
                }
            } catch (SQLException e) {
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    plugin.getLogger().log(Level.SEVERE, "加載圖騰數據時數據庫錯誤: " + e.getMessage(), e);
                } else {
                    plugin.getLogger().log(Level.WARNING, "加載圖騰數據時數據庫錯誤，正在重試 (" + attempt + "/" + MAX_RETRY_ATTEMPTS + "): " + e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
            }
        }
        
        return totems;
    }
    
    @Override
    public boolean hasData(UUID playerUUID) {
        String sql = "SELECT player_uuid FROM ornaments WHERE player_uuid = ? " +
                     "UNION SELECT player_uuid FROM totems WHERE player_uuid = ? LIMIT 1";
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try (Connection conn = getConnection();
                 PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, playerUUID.toString());
                statement.setString(2, playerUUID.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            } catch (SQLException e) {
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    plugin.getLogger().log(Level.SEVERE, "檢查玩家數據失敗: " + e.getMessage(), e);
                } else {
                    plugin.getLogger().log(Level.WARNING, "檢查玩家數據失敗，正在重試 (" + attempt + "/" + MAX_RETRY_ATTEMPTS + "): " + e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 序列化物品
     * @param itemStack 物品
     * @return 序列化後的字符串
     * @throws IOException IO異常
     */
    protected String serializeItemStack(ItemStack itemStack) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeObject(itemStack);
        }
        
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }
    
    /**
     * 反序列化物品
     * @param serializedItem 序列化後的字符串
     * @return 物品
     * @throws IOException IO異常
     * @throws ClassNotFoundException 類未找到異常
     */
    protected ItemStack deserializeItemStack(String serializedItem) throws IOException, ClassNotFoundException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(serializedItem));
        try (BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            return (ItemStack) dataInput.readObject();
        }
    }
} 