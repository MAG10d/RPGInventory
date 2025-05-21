package org.MAGd.rPGInventory.listeners;

import org.MAGd.rPGInventory.RPGInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 監聽玩家登入事件，自動初始化圖騰效果
 */
public class PlayerJoinListener implements Listener {

    private final RPGInventory plugin;

    public PlayerJoinListener(RPGInventory plugin) {
        this.plugin = plugin;
    }

    /**
     * 當玩家登入伺服器時，自動初始化物品欄和圖騰效果
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 延遲執行，確保玩家完全加載
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    plugin.getLogger().info("玩家 " + player.getName() + " 已登入，正在自動初始化RPG物品欄和圖騰效果...");
                    plugin.initializePlayerInventory(player);
                    player.sendMessage("§6[RPG物品欄] §a系統已自動加載你的圖騰效果！輸入 §e/rpginv open §a查看物品欄");
                }
            }
        }.runTaskLater(plugin, 40L); // 延遲2秒，確保玩家完全加載
    }
} 