package ru.dagxam.forgeupgrade.upgrade;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Хранит технические данные реального усиления предмета.
 *
 * В Bukkit 26.2 система Attribute API была изменена: старые константы
 * GENERIC_* и старый конструктор AttributeModifier больше нельзя использовать.
 * Поэтому здесь оставлена совместимая основа хранения уровня. Реальные
 * модификаторы будут добавлены отдельным адаптером под новый API после
 * подтверждения точной версии серверного API.
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
        Integer level = item.getItemMeta().getPersistentDataContainer()
                .get(levelKey, PersistentDataType.INTEGER);
        return level == null ? 0 : level;
    }
}
