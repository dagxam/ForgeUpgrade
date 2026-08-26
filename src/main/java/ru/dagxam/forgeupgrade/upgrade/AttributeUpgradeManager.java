package ru.dagxam.forgeupgrade.upgrade;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

/**
 * Применяет реальные ванильные AttributeModifier к улучшенным предметам.
 * Старые модификаторы ForgeUpgrade сначала удаляются, поэтому уровни не складываются.
 */
public final class AttributeUpgradeManager {
    private final NamespacedKey markerKey;
    private final NamespacedKey levelKey;

    public AttributeUpgradeManager(JavaPlugin plugin) {
        this.markerKey = new NamespacedKey(plugin, "attribute_upgrade");
        this.levelKey = new NamespacedKey(plugin, "attribute_upgrade_level");
    }

    public void apply(ItemStack item, UpgradeType type, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        removeOldModifiers(meta);
        if (!type.isInfinite()) {
            applyModifiers(meta, item.getType(), level);
        }

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(markerKey, PersistentDataType.STRING, type.getId());
        data.set(levelKey, PersistentDataType.INTEGER, level);
        item.setItemMeta(meta);
    }

    private void removeOldModifiers(ItemMeta meta) {
        List<Attribute> attributes = List.of(
                Attribute.GENERIC_ATTACK_DAMAGE,
                Attribute.GENERIC_ATTACK_SPEED,
                Attribute.GENERIC_ARMOR,
                Attribute.GENERIC_ARMOR_TOUGHNESS,
                Attribute.GENERIC_KNOCKBACK_RESISTANCE,
                Attribute.GENERIC_MOVEMENT_SPEED,
                Attribute.GENERIC_MAX_HEALTH
        );

        for (Attribute attribute : attributes) {
            if (!meta.hasAttributeModifiers(attribute)) continue;
            for (AttributeModifier modifier : List.copyOf(meta.getAttributeModifiers(attribute))) {
                if (modifier.getName().startsWith("forgeupgrade_")) {
                    meta.removeAttributeModifier(attribute, modifier);
                }
            }
        }
    }

    private void applyModifiers(ItemMeta meta, Material material, int level) {
        double scale = level / 10.0D;
        EquipmentSlot slot = getEquipmentSlot(material);

        if (isWeapon(material)) {
            add(meta, Attribute.GENERIC_ATTACK_DAMAGE, "forgeupgrade_damage", 0.5D * scale, slot);
            add(meta, Attribute.GENERIC_ATTACK_SPEED, "forgeupgrade_attack_speed", 0.03D * scale, slot);
        }

        if (isArmor(material)) {
            add(meta, Attribute.GENERIC_ARMOR, "forgeupgrade_armor", 0.4D * scale, slot);
            add(meta, Attribute.GENERIC_ARMOR_TOUGHNESS, "forgeupgrade_toughness", 0.2D * scale, slot);
            add(meta, Attribute.GENERIC_KNOCKBACK_RESISTANCE, "forgeupgrade_knockback", 0.02D * scale, slot);
        }
    }

    private void add(ItemMeta meta, Attribute attribute, String name, double amount, EquipmentSlot slot) {
        AttributeModifier modifier = new AttributeModifier(
                UUID.nameUUIDFromBytes((name + slot.name()).getBytes()),
                name,
                amount,
                AttributeModifier.Operation.ADD_NUMBER,
                slot
        );
        meta.addAttributeModifier(attribute, modifier);
    }

    private boolean isWeapon(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE") || material == Material.TRIDENT;
    }

    private boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    private EquipmentSlot getEquipmentSlot(Material material) {
        String name = material.name();
        if (name.endsWith("_HELMET")) return EquipmentSlot.HEAD;
        if (name.endsWith("_CHESTPLATE")) return EquipmentSlot.CHEST;
        if (name.endsWith("_LEGGINGS")) return EquipmentSlot.LEGS;
        if (name.endsWith("_BOOTS")) return EquipmentSlot.FEET;
        return EquipmentSlot.HAND;
    }
}
