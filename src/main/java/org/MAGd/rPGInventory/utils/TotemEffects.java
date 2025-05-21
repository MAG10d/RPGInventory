package org.MAGd.rPGInventory.utils;

import org.MAGd.rPGInventory.RPGInventory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 提供直接的圖騰效果實現
 */
public class TotemEffects {

    // 不同圖騰ID對應的效果配置
    private static final Map<String, EffectConfig> TOTEM_EFFECTS = new HashMap<>();
    
    // 初始化所有已知圖騰的效果
    static {
        // 回復圖騰系列
        TOTEM_EFFECTS.put("auu4.1", new EffectConfig(EffectType.HEAL, 1.0, 5));  // 每5秒回復1點生命值 (半顆心)
        TOTEM_EFFECTS.put("auu4.2", new EffectConfig(EffectType.HEAL, 2.0, 5));  // 每5秒回復2點生命值 (1顆心)
        TOTEM_EFFECTS.put("auu4.3", new EffectConfig(EffectType.HEAL, 3.0, 5));  // 每5秒回復3點生命值 (1顆半心)
        TOTEM_EFFECTS.put("auu4.4", new EffectConfig(EffectType.HEAL, 4.0, 5));  // 每5秒回復4點生命值 (2顆心)
        TOTEM_EFFECTS.put("auu4.6", new EffectConfig(EffectType.HEAL, 1.0, 8));  // 每8秒回復1點生命值 (半顆心)
        
        // 攻擊增益圖騰系列 - 完整系列
        TOTEM_EFFECTS.put("auu3.7", new EffectConfig(EffectType.DAMAGE_BOOST, 5.0, 10));  // 每10秒提高5%攻擊力
        TOTEM_EFFECTS.put("auu3.6", new EffectConfig(EffectType.DAMAGE_BOOST, 10.0, 10)); // 每10秒提高10%攻擊力
        TOTEM_EFFECTS.put("auu3.2", new EffectConfig(EffectType.DAMAGE_BOOST, 20.0, 10)); // 每10秒提高20%攻擊力
        TOTEM_EFFECTS.put("auu3.3", new EffectConfig(EffectType.DAMAGE_BOOST, 30.0, 10)); // 每10秒提高30%攻擊力
        TOTEM_EFFECTS.put("auu3.4", new EffectConfig(EffectType.DAMAGE_BOOST, 40.0, 10)); // 每10秒提高40%攻擊力
        
        // 橫掃圖騰系列
        TOTEM_EFFECTS.put("auu1.1", new EffectConfig(EffectType.AREA_DAMAGE, 0.05, 0)); // 攻擊周圍怪物，傷害為原始傷害的5%
        TOTEM_EFFECTS.put("auu1.2", new EffectConfig(EffectType.AREA_DAMAGE, 0.1, 0));  // 攻擊周圍怪物，傷害為原始傷害的10%
        TOTEM_EFFECTS.put("auu1.3", new EffectConfig(EffectType.AREA_DAMAGE, 0.15, 0)); // 攻擊周圍怪物，傷害為原始傷害的15%
        TOTEM_EFFECTS.put("auu1.4", new EffectConfig(EffectType.AREA_DAMAGE, 0.2, 0));  // 攻擊周圍怪物，傷害為原始傷害的20%
        TOTEM_EFFECTS.put("auu1.5", new EffectConfig(EffectType.AREA_DAMAGE, 1.5, 0));  // 攻擊周圍怪物，固定傷害1.5點
        
        // 防禦圖騰系列
        TOTEM_EFFECTS.put("auu2.7", new EffectConfig(EffectType.DAMAGE_RESISTANCE, 5.0, 10));  // 每10秒抵消5%傷害
        TOTEM_EFFECTS.put("auu2.6", new EffectConfig(EffectType.DAMAGE_RESISTANCE, 10.0, 10)); // 每10秒抵消10%傷害
        TOTEM_EFFECTS.put("auu2.2", new EffectConfig(EffectType.DAMAGE_RESISTANCE, 15.0, 10)); // 每10秒抵消15%傷害
        TOTEM_EFFECTS.put("auu2.3", new EffectConfig(EffectType.DAMAGE_RESISTANCE, 20.0, 10)); // 每10秒抵消20%傷害
        TOTEM_EFFECTS.put("auu2.4", new EffectConfig(EffectType.DAMAGE_RESISTANCE, 25.0, 10)); // 每10秒抵消25%傷害
        
        // 魔法圖騰系列 - 回復法力值
        TOTEM_EFFECTS.put("auu5.5", new EffectConfig(EffectType.MANA_REGEN, 1.0, 5));  // 每5秒回復1點法力值 (基本)
        TOTEM_EFFECTS.put("auu5.1", new EffectConfig(EffectType.MANA_REGEN, 2.0, 5));  // 每5秒回復2點法力值 (第一階段)
        TOTEM_EFFECTS.put("auu5.2", new EffectConfig(EffectType.MANA_REGEN, 3.0, 5));  // 每5秒回復3點法力值 (第二階段)
        TOTEM_EFFECTS.put("auu5.3", new EffectConfig(EffectType.MANA_REGEN, 4.0, 5));  // 每5秒回復4點法力值 (第三階段)
        TOTEM_EFFECTS.put("auu5.4", new EffectConfig(EffectType.MANA_REGEN, 5.0, 5));  // 每5秒回復5點法力值 (第四階段)
    }
    
    /**
     * 從物品中獲取ExecutableItems的ID
     * @param item 物品
     * @return ExecutableItems ID，如果無法獲取則返回null
     */
    public static String getExecutableItemId(ItemStack item, RPGInventory plugin) {
        if (item == null) {
            System.out.println("[RPGInventory-Debug] 物品為空");
            return null;
        }
        
        if (!plugin.hasExecutableItems()) {
            System.out.println("[RPGInventory-Debug] ExecutableItems未安裝或未啟用");
            return null;
        }
        
        try {
            System.out.println("[RPGInventory-Debug] 嘗試獲取ExecutableItem ID，物品類型: " + item.getType());
            
            // 獲取ExecutableItemsAPI類
            Class<?> apiClass = Class.forName("com.ssomar.score.api.executableitems.ExecutableItemsAPI");
            
            // 獲取ExecutableItemsManager
            Object manager = apiClass.getMethod("getExecutableItemsManager").invoke(null);
            if (manager == null) {
                System.out.println("[RPGInventory-Debug] 無法獲取ExecutableItemsManager");
                return null;
            }
            
            // 獲取ExecutableItem對象
            Object result = manager.getClass().getMethod("getExecutableItem", ItemStack.class).invoke(manager, item);
            if (result == null) {
                System.out.println("[RPGInventory-Debug] 無法獲取ExecutableItem結果");
                return null;
            }
            
            // 檢查結果是否為空
            boolean isEmpty = (boolean) result.getClass().getMethod("isEmpty").invoke(result);
            
            if (!isEmpty) {
                // 獲取ExecutableItem
                Object executableItem = result.getClass().getMethod("get").invoke(result);
                
                // 獲取ID
                String id = (String) executableItem.getClass().getMethod("getId").invoke(executableItem);
                System.out.println("[RPGInventory-Debug] 成功獲取ExecutableItem ID: " + id);
                return id;
            } else {
                System.out.println("[RPGInventory-Debug] ExecutableItem結果為空");
            }
        } catch (ClassNotFoundException e) {
            System.out.println("[RPGInventory-Debug] 無法找到ExecutableItemsAPI類: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[RPGInventory-Debug] 獲取ExecutableItem ID時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * 應用圖騰效果
     * @param player 玩家
     * @param item 圖騰物品
     * @param plugin 插件實例
     * @return 是否成功應用效果
     */
    public static boolean applyTotemEffect(Player player, ItemStack item, RPGInventory plugin) {
        String itemId = getExecutableItemId(item, plugin);
        if (itemId == null) return false;
        
        // 獲取效果配置
        EffectConfig config = TOTEM_EFFECTS.get(itemId);
        if (config == null) return false;
        
        switch (config.type) {
            case HEAL:
                return applyHealEffect(player, config.value);
            case DAMAGE_BOOST:
                return applyDamageBoostEffect(player, config.value);
            case DAMAGE_RESISTANCE:
                return applyDamageResistanceEffect(player, config.value);
            case MANA_REGEN:
                return applyManaRegenEffect(player, config.value);
            default:
                return false;
        }
    }
    
    /**
     * 靜默應用圖騰效果（僅延長持續時間，不顯示消息）
     * @param player 玩家
     * @param item 圖騰物品
     * @param plugin 插件實例
     * @return 是否成功應用效果
     */
    public static boolean applyTotemEffectSilently(Player player, ItemStack item, RPGInventory plugin) {
        String itemId = getExecutableItemId(item, plugin);
        if (itemId == null) return false;
        
        // 獲取效果配置
        EffectConfig config = TOTEM_EFFECTS.get(itemId);
        if (config == null) return false;
        
        switch (config.type) {
            case HEAL:
                return applyHealEffect(player, config.value);
            case DAMAGE_BOOST:
                return applyDamageBoostEffectSilently(player, config.value);
            case DAMAGE_RESISTANCE:
                return applyDamageResistanceEffectSilently(player, config.value);
            case MANA_REGEN:
                return applyManaRegenEffect(player, config.value);
            default:
                return false;
        }
    }
    
    /**
     * 應用圖騰效果（基於攻擊事件）
     * @param player 玩家
     * @param item 圖騰物品
     * @param event 攻擊事件
     * @param plugin 插件實例
     * @return 是否成功應用效果
     */
    public static boolean applyTotemAttackEffect(Player player, ItemStack item, EntityDamageByEntityEvent event, RPGInventory plugin) {
        String itemId = getExecutableItemId(item, plugin);
        if (itemId == null) return false;
        
        // 獲取效果配置
        EffectConfig config = TOTEM_EFFECTS.get(itemId);
        if (config == null) return false;
        
        switch (config.type) {
            case AREA_DAMAGE:
                return applyAreaDamageEffect(player, event.getEntity(), event.getDamage(), config.value);
            default:
                return false;
        }
    }
    
    /**
     * 應用治療效果
     * @param player 玩家
     * @param amount 恢復量
     * @return 是否成功應用
     */
    private static boolean applyHealEffect(Player player, double amount) {
        double currentHealth = player.getHealth();
        // 使用不棄用的方法獲取最大生命值
        double maxHealth = 20.0; // 默認最大生命值
        try {
            // 嘗試使用反射獲取最大生命值
            maxHealth = (double) player.getClass().getMethod("getMaxHealth").invoke(player);
        } catch (Exception e) {
            System.out.println("[RPGInventory-Debug] 無法獲取玩家最大生命值，使用默認值20.0: " + e.getMessage());
        }
        
        // 輸出詳細日誌
        System.out.println("[RPGInventory-Debug] 嘗試為玩家 " + player.getName() + 
                " 恢復生命值。當前: " + currentHealth + " 最大: " + maxHealth + 
                " 恢復量: " + amount);
        
        // 調整滿血判斷條件，允許0.1的誤差
        if (currentHealth >= maxHealth - 0.1) {
            System.out.println("[RPGInventory-Debug] 玩家已接近滿血，無需恢復");
            return false;
        }
        
        try {
            // 恢復生命值
            double newHealth = Math.min(currentHealth + amount, maxHealth);
            player.setHealth(newHealth);
            
            System.out.println("[RPGInventory-Debug] 成功為玩家恢復生命值。新生命值: " + newHealth);
            return true;
        } catch (Exception e) {
            System.out.println("[RPGInventory-Debug] 恢復生命值時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 應用攻擊力增益效果
     * @param player 玩家
     * @param percent 增益百分比
     * @return 是否成功應用
     */
    private static boolean applyDamageBoostEffect(Player player, double percent) {
        try {
            // 添加全局日誌
            System.out.println("[RPGInventory] 嘗試為玩家 " + player.getName() + " 應用 " + percent + "% 攻擊增益");
            
            // 通過TotemEffectListener來設置精確的百分比增益
            RPGInventory plugin = RPGInventory.getInstance();
            if (plugin != null && plugin.getTotemEffectListener() != null) {
                plugin.getTotemEffectListener().setDamageBoost(player.getUniqueId(), percent);
                
                // 不再發送提示消息，在實際觸發傷害時顯示
                // player.sendMessage("§6[圖騰] §a你的攻擊力提升了§e " + percent + "%§a！");
                
                System.out.println("[RPGInventory] 成功為玩家 " + player.getName() + 
                        " 提供了精確的 " + percent + "% 攻擊力增益");
                
                // 獲取當前增益值以確認設置成功
                double currentBoost = plugin.getTotemEffectListener().getDamageBoost(player.getUniqueId());
                System.out.println("[RPGInventory] 檢查玩家 " + player.getName() + 
                        " 的當前攻擊增益: " + currentBoost + "%");
                
                return true;
            } else {
                System.out.println("[RPGInventory] 無法獲取TotemEffectListener，無法設置攻擊增益");
                return false;
            }
        } catch (Exception e) {
            System.out.println("[RPGInventory] 應用攻擊力增益時出錯: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 應用傷害抵抗效果
     * @param player 玩家
     * @param percent 抵抗百分比
     * @return 是否成功應用
     */
    private static boolean applyDamageResistanceEffect(Player player, double percent) {
        try {
            // 添加全局日誌
            System.out.println("[RPGInventory] 嘗試為玩家 " + player.getName() + " 應用 " + percent + "% 傷害減免");
            
            // 通過TotemEffectListener來設置精確的減免百分比
            RPGInventory plugin = RPGInventory.getInstance();
            if (plugin != null && plugin.getTotemEffectListener() != null) {
                plugin.getTotemEffectListener().setDamageResistance(player.getUniqueId(), percent);
                
                // 不再發送提示消息，在實際減免傷害時顯示
                // player.sendMessage("§6[圖騰] §a你的傷害減免提升了§e " + percent + "%§a！");
                
                System.out.println("[RPGInventory] 成功為玩家 " + player.getName() + 
                        " 提供了精確的 " + percent + "% 傷害減免");
                
                // 獲取當前減免值以確認設置成功
                double currentResistance = plugin.getTotemEffectListener().getDamageResistance(player.getUniqueId());
                System.out.println("[RPGInventory] 檢查玩家 " + player.getName() + 
                        " 的當前傷害減免: " + currentResistance + "%");
                
                return true;
            } else {
                System.out.println("[RPGInventory] 無法獲取TotemEffectListener，無法設置傷害減免");
                return false;
            }
        } catch (Exception e) {
            System.out.println("[RPGInventory] 應用傷害減免時出錯: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 應用範圍傷害效果（橫掃圖騰）
     * @param player 攻擊的玩家
     * @param target 被攻擊的實體
     * @param originalDamage 原始傷害
     * @param damageMultiplier 傷害乘數或固定值
     * @return 是否成功應用
     */
    private static boolean applyAreaDamageEffect(Player player, Entity target, double originalDamage, double damageMultiplier) {
        System.out.println("[RPGInventory-Debug] 嘗試應用橫掃效果. 原始傷害: " + originalDamage + " 乘數/固定值: " + damageMultiplier);
        
        // 獲取目標周圍3格內的所有實體
        Collection<Entity> nearbyEntities = target.getWorld().getNearbyEntities(target.getLocation(), 3, 3, 3);
        boolean hitAny = false;
        
        for (Entity nearbyEntity : nearbyEntities) {
            // 跳過玩家自身和已攻擊的目標
            if (nearbyEntity.equals(player) || nearbyEntity.equals(target)) continue;
            
            // 只對生物實體造成傷害
            if (nearbyEntity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) nearbyEntity;
                
                double damage;
                // 如果乘數小於1，視為百分比；否則視為固定傷害
                if (damageMultiplier < 1.0) {
                    damage = originalDamage * damageMultiplier;
                } else {
                    damage = damageMultiplier;
                }
                
                System.out.println("[RPGInventory-Debug] 對實體 " + nearbyEntity.getType() + " 造成橫掃傷害: " + damage);
                
                // 造成傷害
                try {
                    livingEntity.damage(damage, player);
                    
                    hitAny = true;
                } catch (Exception e) {
                    System.out.println("[RPGInventory-Debug] 造成橫掃傷害時發生錯誤: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        return hitAny;
    }
    
    /**
     * 應用法力值回復效果
     * @param player 玩家
     * @param amount 回復量
     * @return 是否成功應用
     */
    private static boolean applyManaRegenEffect(Player player, double amount) {
        try {
            // 添加全局日誌
            System.out.println("[RPGInventory] 嘗試為玩家 " + player.getName() + " 回復 " + amount + " 點法力值");
            
            // 執行命令來增加法力值（假設伺服器有mana add命令）
            boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mana add " + player.getName() + " " + (int)amount);
            
            if (success) {
                // 發送提示消息
                player.sendMessage("§6[圖騰] §a你的法力值恢復了§b " + (int)amount + " §a點！");
                
                System.out.println("[RPGInventory] 成功為玩家 " + player.getName() + " 回復 " + amount + " 點法力值");
                return true;
            } else {
                System.out.println("[RPGInventory] 無法執行法力回復命令");
                return false;
            }
        } catch (Exception e) {
            System.out.println("[RPGInventory] 應用法力回復效果時出錯: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 靜默應用攻擊力增益效果（僅延長持續時間，不顯示消息）
     * @param player 玩家
     * @param percent 增益百分比
     * @return 是否成功應用
     */
    private static boolean applyDamageBoostEffectSilently(Player player, double percent) {
        try {
            // 添加全局日誌
            System.out.println("[RPGInventory-Silent] 靜默延長玩家 " + player.getName() + " 的 " + percent + "% 攻擊增益持續時間");
            
            // 通過TotemEffectListener來設置精確的百分比增益
            RPGInventory plugin = RPGInventory.getInstance();
            if (plugin != null && plugin.getTotemEffectListener() != null) {
                plugin.getTotemEffectListener().silentlyExtendDamageBoost(player.getUniqueId(), percent);
                
                System.out.println("[RPGInventory-Silent] 成功延長玩家 " + player.getName() + 
                        " 的 " + percent + "% 攻擊力增益持續時間");
                
                return true;
            } else {
                System.out.println("[RPGInventory-Silent] 無法獲取TotemEffectListener，無法延長攻擊增益持續時間");
                return false;
            }
        } catch (Exception e) {
            System.out.println("[RPGInventory-Silent] 延長攻擊力增益持續時間時出錯: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 靜默應用傷害抵抗效果（僅延長持續時間，不顯示消息）
     * @param player 玩家
     * @param percent 抵抗百分比
     * @return 是否成功應用
     */
    private static boolean applyDamageResistanceEffectSilently(Player player, double percent) {
        try {
            // 添加全局日誌
            System.out.println("[RPGInventory-Silent] 靜默延長玩家 " + player.getName() + " 的 " + percent + "% 傷害減免持續時間");
            
            // 通過TotemEffectListener來設置精確的減免百分比
            RPGInventory plugin = RPGInventory.getInstance();
            if (plugin != null && plugin.getTotemEffectListener() != null) {
                plugin.getTotemEffectListener().silentlyExtendDamageResistance(player.getUniqueId(), percent);
                
                System.out.println("[RPGInventory-Silent] 成功延長玩家 " + player.getName() + 
                        " 的 " + percent + "% 傷害減免持續時間");
                
                return true;
            } else {
                System.out.println("[RPGInventory-Silent] 無法獲取TotemEffectListener，無法延長傷害減免持續時間");
                return false;
            }
        } catch (Exception e) {
            System.out.println("[RPGInventory-Silent] 延長傷害減免持續時間時出錯: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 效果類型枚舉
     */
    private enum EffectType {
        HEAL,               // 治療效果
        DAMAGE_BOOST,       // 攻擊力增益
        AREA_DAMAGE,        // 範圍傷害（橫掃）
        DAMAGE_RESISTANCE,  // 傷害抵抗
        MANA_REGEN          // 法力值回復
    }
    
    /**
     * 效果配置類
     */
    private static class EffectConfig {
        private final EffectType type;
        private final double value;
        
        public EffectConfig(EffectType type, double value, int intervalSeconds) {
            this.type = type;
            this.value = value;
        }
    }
} 