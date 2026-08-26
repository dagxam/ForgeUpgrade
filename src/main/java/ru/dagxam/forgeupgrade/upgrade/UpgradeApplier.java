package ru.dagxam.forgeupgrade.upgrade;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Хранит уровень улучшения на предметах и безопасно применяет новые улучшения.
 */
public final class UpgradeApplier {
    private static final String MARKER = "§8[ForgeUpgrade]";
    private static final String LEVEL_PREFIX = "§eВсе характеристики: §6+";
    private final NamespacedKey typeKey;
    private final NamespacedKey levelKey;
    private final AttributeUpgradeManager attributeUpgradeManager;
    private final ArmorTrimUpgradeManager armorTrimUpgradeManager;

    public UpgradeApplier(JavaPlugin plugin,
                          AttributeUpgradeManager attributeUpgradeManager,
                          ArmorTrimUpgradeManager armorTrimUpgradeManager) {
        this.typeKey = new NamespacedKey(plugin, "applied_upgrade_type");
        this.levelKey = new NamespacedKey(plugin, "upgrade_level");
        this.attributeUpgradeManager = attributeUpgradeManager;
        this.armorTrimUpgradeManager = armorTrimUpgradeManager;
    }

    public boolean isSupported(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (item.getMaxStackSize() != 1) return false;
        if (item.getType() == Material.SHIELD) return true;
        return item.getType().getMaxDurability() > 0;
    }

    public UpgradeType getAppliedType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        return id == null ? null : UpgradeType.fromId(id);
    }

    public int getLevel(ItemStack item) {
        UpgradeType type = getAppliedType(item);
        if (type == null) return 0;
        Integer stored = item.getItemMeta().getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER);
        if (type.isInfinite()) return stored == null ? 71 : Math.max(71, stored);
        return type.getLevel();
    }

    public Result validate(ItemStack item, UpgradeType newType) {
        if (!isSupported(item) || newType == null) return Result.UNSUPPORTED;
        UpgradeType current = getAppliedType(item);
        if (current == newType) return Result.ALREADY_APPLIED;
        if (current != null && !canReplace(current, newType)) return Result.CANNOT_DOWNGRADE;
        return Result.SUCCESS;
    }

    public Result apply(ItemStack item, UpgradeType newType) {
        Result validation = validate(item, newType);
        if (validation != Result.SUCCESS) return validation;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Result.UNSUPPORTED;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(typeKey, PersistentDataType.STRING, newType.getId());
        int storedLevel = newType.isInfinite() ? Math.max(71, getLevel(item)) : newType.getLevel();
        data.set(levelKey, PersistentDataType.INTEGER, storedLevel);

        updateDisplay(meta, item, newType, storedLevel);
        item.setItemMeta(meta);
        attributeUpgradeManager.apply(item, newType, storedLevel);
        armorTrimUpgradeManager.apply(item, newType);
        return Result.SUCCESS;
    }

    private boolean canReplace(UpgradeType current, UpgradeType next) {
        if (current.isInfinite()) return false;
        if (next.isInfinite()) return current == UpgradeType.NETHERITE;
        return next.getLevel() > current.getLevel();
    }

    private void updateDisplay(ItemMeta meta, ItemStack item, UpgradeType type, int level) {
        String originalName = meta.hasDisplayName() ? meta.getDisplayName() : getVanillaName(item.getType());
        originalName = originalName.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
        originalName = originalName.replaceAll("\\s+\\+∞$", "");
        originalName = originalName.replaceAll("\\s+\\+\\d+$", "");

        meta.setDisplayName(type.isInfinite()
                ? "§4☠ " + originalName + " §c+∞"
                : "§6⚒ " + originalName + " §e+" + level);

        List<String> lore = new ArrayList<>();
        if (meta.hasLore() && meta.getLore() != null) {
            for (String line : meta.getLore()) {
                if (!isForgeUpgradeLore(line)) lore.add(line);
            }
        }
        lore.add(MARKER);
        lore.add(type.isInfinite() ? "§4Улучшение: §cАрмагедон" : "§6Улучшение: §e" + type.getDisplayName());
        lore.add(type.isInfinite() ? "§cВсе характеристики: §lБЕСКОНЕЧНО" : LEVEL_PREFIX + level);
        lore.add("§8Новое улучшение заменяет предыдущее.");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    }

    private boolean isForgeUpgradeLore(String line) {
        if (line == null) return false;
        return line.equals(MARKER)
                || line.startsWith("§4Улучшение: §cАрмагедон")
                || line.startsWith("§6Улучшение: §e")
                || line.startsWith(LEVEL_PREFIX)
                || line.equals("§cВсе характеристики: §lБЕСКОНЕЧНО")
                || line.equals("§8Новое улучшение заменяет предыдущее.");
    }

    private String getVanillaName(Material material) {
        return material.name().toLowerCase().replace('_', ' ');
    }

    public enum Result { SUCCESS, UNSUPPORTED, ALREADY_APPLIED, CANNOT_DOWNGRADE }
}
