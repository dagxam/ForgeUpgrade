package ru.dagxam.forgeupgrade.upgrade;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Реально изменяет характеристики предмета через AttributeModifier.
 * Уровень +10/+30/+50/+70 всегда заменяет предыдущий бонус, а не складывается с ним.
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
        markerKey = new NamespacedKey(plugin, "attribute_upgrade");
        levelKey = new NamespacedKey(plugin, "attribute_upgrade_level");
        armorKey = new NamespacedKey(plugin, "bonus_armor");
        toughnessKey = new NamespacedKey(plugin, "bonus_armor_toughness");
        knockbackKey = new NamespacedKey(plugin, "bonus_knockback_resistance");
        damageKey = new NamespacedKey(plugin, "bonus_attack_damage");
        speedKey = new NamespacedKey(plugin, "bonus_attack_speed");
    }

    public void apply(ItemStack item, UpgradeType type, int level) {
        if (item == null || item.getType().isAir()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Сначала полностью убираем старый уровень ForgeUpgrade.
        removeForgeModifiers(meta);

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(markerKey, PersistentDataType.STRING, type.getId());
        data.set(levelKey, PersistentDataType.INTEGER, level);

        // Армагедон не записывает Integer.MAX_VALUE в реальные атрибуты.
        if (!type.isInfinite() && level > 0) {
            addBonuses(meta, item.getType(), level);
        }

        item.setItemMeta(meta);
    }

    private void addBonuses(ItemMeta meta, Material material, int level) {
        // Броня: все её реальные боевые характеристики получают полный бонус уровня.
        if (isArmor(material)) {
            add(meta, Attribute.ARMOR, armorKey, level);
            add(meta, Attribute.ARMOR_TOUGHNESS, toughnessKey, level);
            add(meta, Attribute.KNOCKBACK_RESISTANCE, knockbackKey, level);
        }

        // Оружие и инструменты: урон и скорость атаки реально изменяются на полный уровень.
        if (hasMeleeAttributes(material)) {
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
        if (!meta.hasAttributeModifiers()) return;

        Map<Attribute, Collection<AttributeModifier>> modifiers = meta.getAttributeModifiers().asMap();
        List<Removal> removals = new ArrayList<>();

        for (Map.Entry<Attribute, Collection<AttributeModifier>> entry : modifiers.entrySet()) {
            for (AttributeModifier modifier : entry.getValue()) {
                if (isForgeKey(modifier.getKey())) {
                    removals.add(new Removal(entry.getKey(), modifier));
                }
            }
        }

        for (Removal removal : removals) {
            meta.removeAttributeModifier(removal.attribute(), removal.modifier());
        }
    }

    private boolean isForgeKey(NamespacedKey key) {
        return key.equals(armorKey)
                || key.equals(toughnessKey)
                || key.equals(knockbackKey)
                || key.equals(damageKey)
                || key.equals(speedKey);
    }

    private boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS");
    }

    private boolean hasMeleeAttributes(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD")
                || name.endsWith("_AXE")
                || name.endsWith("_PICKAXE")
                || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE")
                || material == Material.TRIDENT;
    }

    public int getLevel(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return 0;
        Integer level = item.getItemMeta().getPersistentDataContainer()
                .get(levelKey, PersistentDataType.INTEGER);
        return level == null ? 0 : level;
    }

    private record Removal(Attribute attribute, AttributeModifier modifier) {}
}
