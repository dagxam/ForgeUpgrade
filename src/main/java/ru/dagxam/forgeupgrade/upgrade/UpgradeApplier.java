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
 * Реальное изменение боевых характеристик будет добавлено отдельным этапом.
 */
public final class UpgradeApplier {
    private final NamespacedKey typeKey;
    private final NamespacedKey levelKey;

    public UpgradeApplier(JavaPlugin plugin) {
        this.typeKey = new NamespacedKey(plugin, "applied_upgrade_type");
        this.levelKey = new NamespacedKey(plugin, "upgrade_level");
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
        if (type.isInfinite()) {
            Integer level = item.getItemMeta().getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER);
            return level == null ? 71 : level;
        }
        return type.getLevel();
    }

    public Result apply(ItemStack item, UpgradeType newType) {
        if (!isSupported(item)) return Result.UNSUPPORTED;

        UpgradeType current = getAppliedType(item);
        if (current == newType) return Result.ALREADY_APPLIED;
        if (current != null && !canReplace(current, newType)) return Result.CANNOT_DOWNGRADE;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Result.UNSUPPORTED;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(typeKey, PersistentDataType.STRING, newType.getId());
        if (newType.isInfinite()) {
            data.set(levelKey, PersistentDataType.INTEGER, Math.max(71, getLevel(item)));
        } else {
            data.set(levelKey, PersistentDataType.INTEGER, newType.getLevel());
        }

        updateDisplay(meta, item, newType, getStoredLevel(data, newType));
        item.setItemMeta(meta);
        return Result.SUCCESS;
    }

    private int getStoredLevel(PersistentDataContainer data, UpgradeType type) {
        if (!type.isInfinite()) return type.getLevel();
        Integer value = data.get(levelKey, PersistentDataType.INTEGER);
        return value == null ? 71 : value;
    }

    private boolean canReplace(UpgradeType current, UpgradeType next) {
        if (current.isInfinite()) return false;
        if (next.isInfinite()) return current == UpgradeType.NETHERITE;
        return next.getLevel() > current.getLevel();
    }

    private void updateDisplay(ItemMeta meta, ItemStack item, UpgradeType type, int level) {
        String originalName = meta.hasDisplayName() ? meta.getDisplayName() : getVanillaName(item.getType());
        originalName = originalName.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
        originalName = originalName.replaceAll(" \\+\\d+$", "");
        meta.setDisplayName(type.isInfinite()
                ? "§4☠ " + originalName + " §c+∞"
                : "§6⚒ " + originalName + " §e+" + level);

        List<String> lore = new ArrayList<>();
        if (meta.hasLore() && meta.getLore() != null) {
            for (String line : meta.getLore()) {
                if (!line.contains("§8[ForgeUpgrade]")) lore.add(line);
            }
        }
        lore.add("§8[ForgeUpgrade]");
        lore.add(type.isInfinite() ? "§4Улучшение: §cАрмагедон" : "§6Улучшение: §e" + type.getDisplayName());
        lore.add(type.isInfinite() ? "§cВсе характеристики: §lБЕСКОНЕЧНО" : "§eВсе характеристики: §6+" + level);
        lore.add("§8Новое улучшение заменяет предыдущее.");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    }

    private String getVanillaName(Material material) {
        return material.name().toLowerCase().replace('_', ' ');
    }

    public enum Result {
        SUCCESS,
        UNSUPPORTED,
        ALREADY_APPLIED,
        CANNOT_DOWNGRADE
    }
}
