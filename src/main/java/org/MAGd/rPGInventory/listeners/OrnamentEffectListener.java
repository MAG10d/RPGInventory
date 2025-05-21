package org.MAGd.rPGInventory.listeners;

import org.MAGd.rPGInventory.RPGInventory;
import org.MAGd.rPGInventory.gui.InventoryGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 監聽與處理飾品效果的觸發
 */
public class OrnamentEffectListener implements Listener {

    private final RPGInventory plugin;
    
    // 存儲玩家UUID與其飾品循環任務的映射
    private final Map<UUID, BukkitTask> playerOrnamentTasks = new ConcurrentHashMap<>();

    public OrnamentEffectListener(RPGInventory plugin) {
        this.plugin = plugin;
    }

    /**
     * 當玩家攻擊實體時觸發飾品效果
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        // 檢查攻擊者是否為玩家
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        UUID playerUUID = player.getUniqueId();

        // 檢查玩家是否有打開的RPG物品欄
        if (!InventoryGUI.hasOpenInventory(playerUUID)) return;

        // 獲取玩家的飾品
        ItemStack ornament = InventoryGUI.getOrnamentItem(playerUUID);
        if (ornament == null) return;

        // 延遲觸發飾品效果，確保在事件處理完畢後執行
        new BukkitRunnable() {
            @Override
            public void run() {
                triggerExecutableItem(player, ornament, "PLAYER_HIT_ENTITY");
            }
        }.runTaskLater(plugin, 1L);
    }

    /**
     * 當玩家移動時觸發飾品效果
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
        // 如果只是頭部轉動，不處理
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // 檢查玩家是否有打開的RPG物品欄
        if (!InventoryGUI.hasOpenInventory(playerUUID)) return;

        // 獲取玩家的飾品
        ItemStack ornament = InventoryGUI.getOrnamentItem(playerUUID);
        if (ornament == null) return;

        // 延遲觸發飾品效果，確保在事件處理完畢後執行
        new BukkitRunnable() {
            @Override
            public void run() {
                triggerExecutableItem(player, ornament, "PLAYER_MOVE");
            }
        }.runTaskLater(plugin, 1L);
    }

    /**
     * 當玩家受到傷害時觸發飾品效果
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDamaged(EntityDamageEvent event) {
        // 檢查受傷者是否為玩家
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        UUID playerUUID = player.getUniqueId();

        // 檢查玩家是否有打開的RPG物品欄
        if (!InventoryGUI.hasOpenInventory(playerUUID)) return;

        // 獲取玩家的飾品
        ItemStack ornament = InventoryGUI.getOrnamentItem(playerUUID);
        if (ornament == null) return;

        // 延遲觸發飾品效果，確保在事件處理完畢後執行
        new BukkitRunnable() {
            @Override
            public void run() {
                triggerExecutableItem(player, ornament, "PLAYER_DAMAGED");
            }
        }.runTaskLater(plugin, 1L);
    }

    /**
     * 當玩家交互時觸發飾品效果(右鍵點擊等)
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // 檢查玩家是否有打開的RPG物品欄
        if (!InventoryGUI.hasOpenInventory(playerUUID)) return;

        // 獲取玩家的飾品
        ItemStack ornament = InventoryGUI.getOrnamentItem(playerUUID);
        if (ornament == null) return;

        // 延遲觸發飾品效果，確保在事件處理完畢後執行
        new BukkitRunnable() {
            @Override
            public void run() {
                triggerExecutableItem(player, ornament, "PLAYER_INTERACT");
            }
        }.runTaskLater(plugin, 1L);
    }
    
    /**
     * 監聽物品欄點擊事件，處理飾品放入或取出
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!InventoryGUI.isRPGInventory(event.getInventory())) return;
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        
        // 檢查是否是飾品欄位
        if (InventoryGUI.isOrnamentSlot(slot)) {
            // 延遲處理，確保在事件處理完畢後執行
            new BukkitRunnable() {
                @Override
                public void run() {
                    updateOrnamentLoopTask(player);
                }
            }.runTaskLater(plugin, 1L);
        }
    }
    
    /**
     * 玩家加入伺服器時，更新飾品循環任務
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 延遲處理，確保玩家數據完全加載
        new BukkitRunnable() {
            @Override
            public void run() {
                if (InventoryGUI.hasOpenInventory(player.getUniqueId())) {
                    updateOrnamentLoopTask(player);
                }
            }
        }.runTaskLater(plugin, 20L); // 延遲1秒
    }
    
    /**
     * 玩家離開伺服器時，取消其飾品循環任務
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cancelOrnamentTask(event.getPlayer().getUniqueId());
    }
    
    /**
     * 更新玩家的飾品循環任務
     * @param player 玩家
     */
    public void updateOrnamentLoopTask(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // 先取消舊的任務
        cancelOrnamentTask(playerUUID);
        
        // 獲取玩家的飾品
        ItemStack ornament = InventoryGUI.getOrnamentItem(playerUUID);
        if (ornament == null) return;
        
        // 檢查飾品是否有LOOP激活器
        Map<String, Object> loopActivator = getLoopActivator(ornament);
        if (loopActivator != null) {
            // 獲取循環時間（秒）
            int delay = getLoopDelay(loopActivator);
            if (delay > 0) {
                // 創建循環任務
                BukkitTask task = new BukkitRunnable() {
                    @Override
                    public void run() {
                        // 檢查玩家是否仍在線以及飾品是否仍在物品欄中
                        if (player.isOnline() && InventoryGUI.hasOpenInventory(playerUUID)) {
                            ItemStack currentOrnament = InventoryGUI.getOrnamentItem(playerUUID);
                            if (currentOrnament != null && currentOrnament.equals(ornament)) {
                                // 觸發LOOP激活器
                                triggerExecutableItem(player, ornament, "LOOP");
                            } else {
                                // 飾品已更換或移除，取消任務
                                this.cancel();
                                playerOrnamentTasks.remove(playerUUID);
                            }
                        } else {
                            // 玩家下線或物品欄已關閉，取消任務
                            this.cancel();
                            playerOrnamentTasks.remove(playerUUID);
                        }
                    }
                }.runTaskTimer(plugin, delay * 20L, delay * 20L); // 轉換為遊戲刻
                
                // 保存任務引用
                playerOrnamentTasks.put(playerUUID, task);
            }
        }
    }
    
    /**
     * 獲取物品的LOOP激活器配置
     * @param item 物品
     * @return LOOP激活器配置，如果沒有則返回null
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getLoopActivator(ItemStack item) {
        if (!plugin.hasExecutableItems()) return null;
        
        try {
            // 獲取ExecutableItemsAPI類
            Class<?> apiClass = Class.forName("com.ssomar.score.api.executableitems.ExecutableItemsAPI");
            
            // 獲取ExecutableItemsManager
            Object manager = apiClass.getMethod("getExecutableItemsManager").invoke(null);
            
            // 獲取ExecutableItem對象
            Object result = manager.getClass().getMethod("getExecutableItem", ItemStack.class).invoke(manager, item);
            
            // 檢查結果是否為空
            boolean isEmpty = (boolean) result.getClass().getMethod("isEmpty").invoke(result);
            if (isEmpty) return null;
            
            // 獲取ExecutableItem
            Object executableItem = result.getClass().getMethod("get").invoke(result);
            
            // 獲取激活器列表
            Method getActivatorsMethod = null;
            for (Method method : executableItem.getClass().getMethods()) {
                if (method.getName().equals("getActivators")) {
                    getActivatorsMethod = method;
                    break;
                }
            }
            
            if (getActivatorsMethod != null) {
                Object activators = getActivatorsMethod.invoke(executableItem);
                if (activators instanceof Map) {
                    // 遍歷激活器尋找LOOP類型
                    for (Object value : ((Map<?, ?>) activators).values()) {
                        // 獲取激活器類型
                        String option = (String) value.getClass().getMethod("getOption").invoke(value);
                        if ("LOOP".equalsIgnoreCase(option)) {
                            // 找到LOOP激活器，轉換為Map返回
                            return convertActivatorToMap(value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("無法獲取LOOP激活器: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 將激活器對象轉換為Map
     * @param activator 激活器對象
     * @return 包含激活器信息的Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertActivatorToMap(Object activator) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 獲取延遲時間
            result.put("delay", activator.getClass().getMethod("getDelay").invoke(activator));
            
            // 獲取延遲是否以tick為單位
            result.put("delayInTick", activator.getClass().getMethod("isDelayInTick").invoke(activator));
            
            // 獲取命令列表
            result.put("commands", activator.getClass().getMethod("getCommands").invoke(activator));
        } catch (Exception e) {
            plugin.getLogger().warning("轉換激活器失敗: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 獲取循環激活器的延遲時間（秒）
     * @param loopActivator 循環激活器配置
     * @return 延遲時間（秒）
     */
    private int getLoopDelay(Map<String, Object> loopActivator) {
        try {
            int delay = (int) loopActivator.get("delay");
            boolean delayInTick = (boolean) loopActivator.get("delayInTick");
            
            // 如果延遲以tick為單位，轉換為秒
            if (delayInTick) {
                return Math.max(1, delay / 20);
            } else {
                return Math.max(1, delay);
            }
        } catch (Exception e) {
            return 5; // 默認5秒
        }
    }
    
    /**
     * 取消玩家的飾品循環任務
     * @param playerUUID 玩家UUID
     */
    private void cancelOrnamentTask(UUID playerUUID) {
        BukkitTask task = playerOrnamentTasks.remove(playerUUID);
        if (task != null) {
            try {
                task.cancel();
            } catch (Exception ignored) {}
        }
    }

    /**
     * 通過反射觸發ExecutableItems物品的效果
     * @param player 玩家
     * @param item 物品
     * @param activatorType 激活器類型
     */
    private void triggerExecutableItem(Player player, ItemStack item, String activatorType) {
        if (!plugin.hasExecutableItems()) return;
        
        try {
            // 獲取ExecutableItemsAPI類
            Class<?> apiClass = Class.forName("com.ssomar.score.api.executableitems.ExecutableItemsAPI");
            
            // 獲取ExecutableItemsManager
            Object manager = apiClass.getMethod("getExecutableItemsManager").invoke(null);
            
            // 獲取ExecutableItem對象
            Object result = manager.getClass().getMethod("getExecutableItem", ItemStack.class).invoke(manager, item);
            
            // 檢查結果是否為空
            boolean isEmpty = (boolean) result.getClass().getMethod("isEmpty").invoke(result);
            if (isEmpty) return;
            
            // 獲取ExecutableItem
            Object executableItem = result.getClass().getMethod("get").invoke(result);
            
            // 直接執行指令而不是使用激活器
            if ("LOOP".equals(activatorType)) {
                // 針對LOOP類型，直接執行命令
                Map<String, Object> loopActivator = getLoopActivator(item);
                if (loopActivator != null && loopActivator.containsKey("commands")) {
                    // 獲取命令列表
                    List<String> commands = (List<String>) loopActivator.get("commands");
                    
                    // 手動執行每個指令
                    for (String command : commands) {
                        // 創建一個臨時物品在玩家背包中
                        executeCommandDirectly(player, command);
                    }
                    return;
                }
            }
            
            // 對於其他激活器類型，嘗試使用executeActivator方法
            Method executeMethod = null;
            for (Method method : executableItem.getClass().getMethods()) {
                if (method.getName().equals("executeActivator") && method.getParameterCount() == 3) {
                    executeMethod = method;
                    break;
                }
            }
            
            if (executeMethod != null) {
                // 執行激活器
                executeMethod.invoke(executableItem, player, activatorType, item);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("無法觸發ExecutableItem飾品效果: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 直接執行ExecutableItems命令
     * @param player 玩家
     * @param command 命令
     */
    private void executeCommandDirectly(Player player, String command) {
        try {
            if (command.startsWith("REGAIN HEALTH")) {
                // 解析恢復生命值的數量
                String[] parts = command.split(" ");
                if (parts.length >= 3) {
                    double amount = Double.parseDouble(parts[2]);
                    // 恢復生命值
                    double currentHealth = player.getHealth();
                    double maxHealth = player.getMaxHealth();
                    player.setHealth(Math.min(currentHealth + amount, maxHealth));
                    
                    // 顯示效果
                    player.getWorld().spawnParticle(org.bukkit.Particle.HEART, 
                            player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.1);
                }
            } else if (command.startsWith("DAMAGE_RESISTANCE")) {
                // 解析抗性等級和持續時間
                String[] parts = command.split(" ");
                if (parts.length >= 3) {
                    int amplifier = Integer.parseInt(parts[1]);
                    int duration = Integer.parseInt(parts[2]);
                    
                    try {
                        // 應用抗性效果，使用新方法來隱藏圖標
                        org.bukkit.potion.PotionEffect effect = new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.RESISTANCE, 
                                duration, // ticks
                                amplifier - 1, // Minecraft中等級是從0開始的
                                false, // 不顯示粒子
                                false, // 不顯示圖標
                                false  // 不顯示為環境效果 - 嘗試隱藏
                        );
                        player.addPotionEffect(effect, true); // 強制覆蓋現有效果
                        
                        // 顯示效果 (使用1.21.4兼容的粒子效果)
                        player.getWorld().spawnParticle(org.bukkit.Particle.ENCHANTED_HIT, 
                                player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
                        
                        // 調試信息
                        plugin.getLogger().info("[RPGInventory] 為玩家 " + player.getName() + 
                                " 提供等級 " + amplifier + " 的抗性，持續 " + (duration / 20) + " 秒");
                    } catch (Exception e) {
                        plugin.getLogger().warning("應用抗性效果時出錯: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } else if (command.startsWith("DAMAGE_BOOST")) {
                // 解析攻擊力提升百分比和持續時間
                String[] parts = command.split(" ");
                if (parts.length >= 3) {
                    double percent = Double.parseDouble(parts[1]);
                    int duration = Integer.parseInt(parts[2]);
                    
                    try {
                        // 使用TotemEffectListener來設置精確的百分比增益
                        if (plugin.getTotemEffectListener() != null) {
                            plugin.getTotemEffectListener().setDamageBoost(player.getUniqueId(), percent);
                            
                            // 播放特效
                            player.getWorld().spawnParticle(org.bukkit.Particle.CRIT, 
                                    player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                            
                            // 發送提示消息
                            player.sendMessage("§6[飾品] §a你的攻擊力提升了§e " + percent + "%§a！");
                            
                            plugin.getLogger().info("為玩家 " + player.getName() + 
                                    " 提供了精確的 " + percent + "% 攻擊力增益，持續 " + (duration / 20) + " 秒");
                        } else {
                            plugin.getLogger().warning("無法獲取TotemEffectListener，無法設置攻擊增益");
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("設置攻擊力增益時出錯: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } else if (command.startsWith("SPEED")) {
                // 處理速度效果
                String[] parts = command.split(" ");
                if (parts.length >= 3) {
                    int amplifier = Integer.parseInt(parts[1]);
                    int duration = Integer.parseInt(parts[2]);
                    
                    // 應用速度效果
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.SPEED, 
                            duration, 
                            amplifier - 1, // Minecraft中等級是從0開始的
                            false, // 不顯示粒子
                            true,  // 顯示圖標 
                            true   // 是否顯示為環境效果
                    ));
                }
            } else if (command.startsWith("JUMP")) {
                // 處理跳躍提升效果
                String[] parts = command.split(" ");
                if (parts.length >= 3) {
                    int amplifier = Integer.parseInt(parts[1]);
                    int duration = Integer.parseInt(parts[2]);
                    
                    // 應用跳躍提升效果
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.JUMP_BOOST, 
                            duration, 
                            amplifier - 1, // Minecraft中等級是從0開始的
                            false, // 不顯示粒子
                            true,  // 顯示圖標
                            true   // 是否顯示為環境效果
                    ));
                }
            } else if (command.startsWith("MOB_AROUND")) {
                // 解析範圍和傷害
                String[] parts = command.split(" ");
                if (parts.length >= 3) {
                    int range = Integer.parseInt(parts[1]);
                    
                    // 解析傷害值
                    String damageStr = command.substring(command.indexOf("DAMAGE") + 7).trim();
                    double damage = 0;
                    
                    if (damageStr.contains("%")) {
                        // 處理百分比傷害
                        // 這裡用一個簡單的示範，假設是基於玩家攻擊力的百分比
                        damage = 1.0; // 假設玩家基礎攻擊為1點
                        damage *= Double.parseDouble(damageStr.replace("%", "")) / 100.0;
                    } else {
                        // 直接傷害值
                        damage = Double.parseDouble(damageStr);
                    }
                    
                    // 對周圍生物造成傷害
                    for (org.bukkit.entity.Entity entity : player.getNearbyEntities(range, range, range)) {
                        if (entity instanceof org.bukkit.entity.LivingEntity && !(entity instanceof org.bukkit.entity.Player)) {
                            ((org.bukkit.entity.LivingEntity) entity).damage(damage, player);
                        }
                    }
                    
                    // 顯示效果
                    player.getWorld().spawnParticle(org.bukkit.Particle.SWEEP_ATTACK, 
                            player.getLocation().add(0, 1, 0), 10, range/2, 0.5, range/2, 0.1);
                }
            } else if (command.startsWith("CONSOLE_COMMAND")) {
                // 執行伺服器命令
                String cmd = command.substring("CONSOLE_COMMAND".length()).trim();
                // 替換玩家名稱和位置
                cmd = cmd.replace("%player%", player.getName());
                cmd = cmd.replace("%player_location%", 
                        player.getLocation().getX() + " " + 
                        player.getLocation().getY() + " " + 
                        player.getLocation().getZ());
                
                // 執行命令
                org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), cmd);
            }
            // 可以添加更多命令的處理...
            
        } catch (Exception e) {
            plugin.getLogger().warning("執行命令時出錯: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 