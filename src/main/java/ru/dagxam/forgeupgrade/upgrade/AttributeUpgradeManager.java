package ru.dagxam.forgeupgrade.upgrade;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/**
 * Применяет реальные бонусы ForgeUpgrade непосредственно к атрибутам предмета.
 * Старый уровень всегда удаляется перед установкой нового, поэтому +10/+30/+50/+70
 * не складываются между собой.
 */
public final class AttributeUpgradeManager {
    private final NamespacedKey markerKey;
    private final NamespacedKey levelKey;
    private final NamespacedKey armorKey;
    private final NamespacedKey toughnessKey;
    private final NamespacedKey knockbackKey;
    private final NamespacedKey damageKey;
    private final NamespacedKey speedKey;

    public AttributeUpgradeManager(JavaPlugin plugin) {
        this.markerKey = new NamespacedKey(plugin, "attribute_upgrade");
        this.levelKey = new NamespacedKey(plugin, "attribute_upgrade_level");
        this.armorKey = new NamespacedKey(plugin, "bonus_armor");
        this.toughnessKey = new NamespacedKey(plugin, "bonus_armor_toughness");
        this.knockbackKey = new NamespacedKey(plugin, "bonus_knockback_resistance");
        this.damageKey = new NamespacedKey(plugin, "bonus_attack_damage");
        this.speedKey = new NamespacedKey(plugin, "bonus_attack_speed");
    }

    public void apply(ItemStack item, UpgradeType type, int level) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        removeForgeModifiers(meta);

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(markerKey, PersistentDataType.STRING, type.getId());
        data.set(levelKey, PersistentDataType.INTEGER, level);

        // Армагедон использует отдельные специальные механики и не получает
        // числовой Integer.MAX_VALUE в AttributeModifier.
        if (!type.isInfinite() && level > 0) {
            addBonuses(meta, item, level);
        }

        item.setItemMeta(meta);
    }

    private void addBonuses(ItemMeta meta, ItemStack item, int level) {
        if (isArmor(item)) {
            add(meta, Attribute.ARMOR, armorKey, level);
            add(meta, Attribute.ARMOR_TOUGHNESS, toughnessKey, level);
            add(meta, Attribute.KNOCKBACK_RESISTANCE, knockbackKey, level);
        }

        if (isMeleeWeapon(item)) {
            add(meta, Attribute.ATTACK_DAMAGE, damageKey, level);
            add(meta, Attribute.ATTACK_SPEED, speedKey, level);
        }
    }

    private void add(ItemMeta meta, Attribute attribute, NamespacedKey key, double amount) {
        meta.addAttributeModifier(attribute, new AttributeModifier(
                key,
                amount,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.ANY
        ));
    }

    private void removeForgeModifiers(ItemMeta meta) {
        for (Map.Entry<Attribute, AttributeModifier> entry : meta.getAttributeModifiers()) {
            NamespacedKey key = entry.getValue().getKey();
            if (key.equals(armorKey) || key.equals(toughnessKey) || key.equals(knockbackKey)
                    || key.equals(damageKey) || key.equals(speedKey)) {
                meta.removeAttributeModifier(entry.getKey(), entry.getValue());
            }
        }
    }

    private boolean isArmor(ItemStack item) {
        String name = item.getType().name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    private boolean isMeleeWeapon(ItemStack item) {
        String name = item.getType().name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE") || item.getType().name().equals("TRIDENT");
    }

    public int getLevel(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return 0;
        Integer level = item.getItemMeta().getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER);
        return level == null ? 0 : level;
    }
}
