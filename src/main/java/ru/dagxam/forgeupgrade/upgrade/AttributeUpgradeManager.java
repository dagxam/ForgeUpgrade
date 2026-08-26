package ru.dagxam.forgeupgrade.upgrade;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Хранит и рассчитывает реальные бонусы улучшения без использования
 * нестабильного Attribute API. Бонусы применяются слушателем только в момент
 * нужного события, поэтому не создают постоянной нагрузки.
 */
public final class AttributeUpgradeManager {
    private final NamespacedKey markerKey;
    private final NamespacedKey levelKey;

    public AttributeUpgradeManager(JavaPlugin plugin) {
        this.markerKey = new NamespacedKey(plugin, "attribute_upgrade");
        this.levelKey = new NamespacedKey(plugin, "attribute_upgrade_level");
    }

    public void apply(ItemStack item, UpgradeType type, int level) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(markerKey, PersistentDataType.STRING, type.getId());
        data.set(levelKey, PersistentDataType.INTEGER, level);
        item.setItemMeta(meta);
    }

    public int getLevel(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return 0;
        Integer level = item.getItemMeta().getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER);
        return level == null ? 0 : level;
    }

    /** Реальный бонус урона: +10 = +1.0, +30 = +3.0, +50 = +5.0, +70 = +7.0. */
    public double getAttackBonus(ItemStack item) {
        int level = getLevel(item);
        if (level == Integer.MAX_VALUE) return 0.0D;
        return Math.min(level, 70) / 10.0D;
    }

    /** Дополнительная защита предмета брони: до 35% на +70. */
    public double getArmorReduction(ItemStack item) {
        int level = getLevel(item);
        if (level <= 0 || level == Integer.MAX_VALUE) return 0.0D;
        return Math.min(0.35D, level / 200.0D);
    }

    public boolean isWeapon(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE") || material == Material.TRIDENT;
    }
}
