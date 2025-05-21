package org.MAGd.rPGInventory.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * 物品建造器，用於簡化物品創建過程
 */
public class ItemBuilder {
    
    private final ItemStack item;
    private ItemMeta meta;
    
    /**
     * 建構子
     * @param material 物品材質
     */
    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }
    
    /**
     * 建構子
     * @param item 物品
     */
    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }
    
    /**
     * 設置物品名稱
     * @param name 名稱
     * @return this
     */
    public ItemBuilder setName(String name) {
        meta.setDisplayName(name);
        return this;
    }
    
    /**
     * 設置物品說明
     * @param lore 說明
     * @return this
     */
    public ItemBuilder setLore(String... lore) {
        if (lore == null || lore.length == 0) {
            meta.setLore(null); // 或者 meta.setLore(new ArrayList<>());
        } else {
            meta.setLore(Arrays.asList(lore));
        }
        return this;
    }
    
    /**
     * 設置物品說明
     * @param lore 說明
     * @return this
     */
    public ItemBuilder setLore(List<String> lore) {
        meta.setLore(lore);
        return this;
    }
    
    /**
     * 設置物品是否可以破壞
     * @param unbreakable 是否不可破壞
     * @return this
     */
    public ItemBuilder setUnbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }
    
    /**
     * 設置物品數量
     * @param amount 數量
     * @return this
     */
    public ItemBuilder setAmount(int amount) {
        item.setAmount(amount);
        return this;
    }
    
    /**
     * 設置物品自定義模型數據
     * @param modelData 模型數據
     * @return this
     */
    public ItemBuilder setCustomModelData(int modelData) {
        meta.setCustomModelData(modelData);
        return this;
    }
    
    /**
     * 構建物品
     * @return 物品
     */
    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
} 