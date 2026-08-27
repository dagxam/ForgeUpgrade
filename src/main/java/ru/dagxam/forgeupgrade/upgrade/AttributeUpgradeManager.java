package ru.dagxam.forgeupgrade.upgrade;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
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

/** Реально изменяет характеристики предмета через AttributeModifier и улучшает скорость работы инструментов. */
public final class AttributeUpgradeManager {
    private final NamespacedKey markerKey;
    private final NamespacedKey levelKey;
    private final NamespacedKey armorKey;
    private final NamespacedKey toughnessKey;
    private final NamespacedKey knockbackKey;
    private final NamespacedKey maxHealthKey;
    private final NamespacedKey damageKey;
    private final NamespacedKey speedKey;
    private final NamespacedKey originalEfficiencyKey;

    public AttributeUpgradeManager(JavaPlugin plugin) {
        markerKey = new NamespacedKey(plugin, "attribute_upgrade");
        levelKey = new NamespacedKey(plugin, "attribute_upgrade_level");
        armorKey = new NamespacedKey(plugin, "bonus_armor");
        toughnessKey = new NamespacedKey(plugin, "bonus_armor_toughness");
        knockbackKey = new NamespacedKey(plugin, "bonus_knockback_resistance");
        maxHealthKey = new NamespacedKey(plugin, "bonus_max_health");
        damageKey = new NamespacedKey(plugin, "bonus_attack_damage");
        speedKey = new NamespacedKey(plugin, "bonus_attack_speed");
        originalEfficiencyKey = new NamespacedKey(plugin, "original_efficiency_level");
    }

    public void apply(ItemStack item, UpgradeType type, int ignoredLevel) {
        if (item == null || item.getType().isAir() || type == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        removeForgeModifiers(meta);

        int bonus = type.getAttributeBonus();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(markerKey, PersistentDataType.STRING, type.getId());
        data.set(levelKey, PersistentDataType.INTEGER, bonus);

        if (bonus > 0) addBonuses(meta, item.getType(), bonus, type);
        applyToolEfficiency(meta, item.getType(), type);
        item.setItemMeta(meta);
    }

    private void addBonuses(ItemMeta meta, Material material, int bonus, UpgradeType type) {
        EquipmentSlotGroup armorSlot = getArmorSlot(material);
        if (armorSlot != null) {
            add(meta, Attribute.ARMOR, armorKey, bonus, armorSlot);
            add(meta, Attribute.ARMOR_TOUGHNESS, toughnessKey, bonus, armorSlot);
            add(meta, Attribute.KNOCKBACK_RESISTANCE, knockbackKey, bonus, armorSlot);

            if (type == UpgradeType.ARMAGEDDON) {
                add(meta, Attribute.MAX_HEALTH, maxHealthKey, bonus, armorSlot);
            }
        }

        if (hasCombatAttributes(material)) {
            add(meta, Attribute.ATTACK_DAMAGE, damageKey, bonus, EquipmentSlotGroup.MAINHAND);
            add(meta, Attribute.ATTACK_SPEED, speedKey, bonus, EquipmentSlotGroup.MAINHAND);
        }
    }

    /**
     * Обычные улучшения повышают скорость добычи через Эффективность.
     * Армагеддон на НЕ-незеритовых инструментах даёт только +999999 к характеристикам
     * и не получает никаких дополнительных способностей или усилений инструмента.
     * Полный набор способностей Армагеддона доступен только незеритовым предметам.
     */
    private void applyToolEfficiency(ItemMeta meta, Material material, UpgradeType type) {
        if (!isMiningTool(material)) return;
        if (type == UpgradeType.ARMAGEDDON && !isNetheriteMiningTool(material)) return;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        Integer original = data.get(originalEfficiencyKey, PersistentDataType.INTEGER);
        if (original == null) {
            original = meta.getEnchantLevel(Enchantment.EFFICIENCY);
            data.set(originalEfficiencyKey, PersistentDataType.INTEGER, original);
        }

        int target = Math.max(original, type.getToolEfficiency());
        if (target > 0) meta.addEnchant(Enchantment.EFFICIENCY, target, true);
    }

    private void add(ItemMeta meta, Attribute attribute, NamespacedKey key, double amount, EquipmentSlotGroup slot) {
        meta.addAttributeModifier(attribute, new AttributeModifier(
                key,
                amount,
                AttributeModifier.Operation.ADD_NUMBER,
                slot
        ));
    }

    private void removeForgeModifiers(ItemMeta meta) {
        if (!meta.hasAttributeModifiers()) return;
        Map<Attribute, Collection<AttributeModifier>> modifiers = meta.getAttributeModifiers().asMap();
        List<Removal> removals = new ArrayList<>();
        for (Map.Entry<Attribute, Collection<AttributeModifier>> entry : modifiers.entrySet()) {
            for (AttributeModifier modifier : entry.getValue()) {
                if (isForgeKey(modifier.getKey())) removals.add(new Removal(entry.getKey(), modifier));
            }
        }
        for (Removal removal : removals) meta.removeAttributeModifier(removal.attribute(), removal.modifier());
    }

    private boolean isForgeKey(NamespacedKey key) {
        return key.equals(armorKey) || key.equals(toughnessKey) || key.equals(knockbackKey)
                || key.equals(maxHealthKey) || key.equals(damageKey) || key.equals(speedKey);
    }

    private EquipmentSlotGroup getArmorSlot(Material material) {
        String name = material.name();
        if (name.endsWith("_HELMET")) return EquipmentSlotGroup.HEAD;
        if (name.endsWith("_CHESTPLATE")) return EquipmentSlotGroup.CHEST;
        if (name.endsWith("_LEGGINGS")) return EquipmentSlotGroup.LEGS;
        if (name.endsWith("_BOOTS")) return EquipmentSlotGroup.FEET;
        return null;
    }

    private boolean hasCombatAttributes(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE") || name.endsWith("_PICKAXE")
                || name.endsWith("_SHOVEL") || name.endsWith("_HOE") || name.endsWith("_SPEAR")
                || material == Material.TRIDENT;
    }

    private boolean isMiningTool(Material material) {
        String name = material.name();
        return name.endsWith("_PICKAXE") || name.endsWith("_AXE")
                || name.endsWith("_SHOVEL") || name.endsWith("_HOE");
    }

    private boolean isNetheriteMiningTool(Material material) {
        return material == Material.NETHERITE_PICKAXE || material == Material.NETHERITE_AXE
                || material == Material.NETHERITE_SHOVEL || material == Material.NETHERITE_HOE;
    }

    public int getLevel(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return 0;
        Integer level = item.getItemMeta().getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER);
        return level == null ? 0 : level;
    }

    private record Removal(Attribute attribute, AttributeModifier modifier) {}
}
