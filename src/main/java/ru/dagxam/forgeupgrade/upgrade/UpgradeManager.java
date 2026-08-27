package ru.dagxam.forgeupgrade.upgrade;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

/** Создаёт и безопасно распознаёт собственные кузнечные шаблоны улучшений. */
public final class UpgradeManager {
    private final NamespacedKey upgradeKey;

    public UpgradeManager(JavaPlugin plugin) {
        this.upgradeKey = new NamespacedKey(plugin, "upgrade_type");
    }

    public ItemStack createUpgrade(UpgradeType type) {
        // Каждый результат крафта является реальным предметом кузнечного шаблона.
        ItemStack item = new ItemStack(type.getSmithingTemplate());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(type.isInfinite()
                ? "§4☠ Шаблон Армагедона §c+∞"
                : "§6⚒ " + type.getDisplayName() + " §e+" + type.getLevel());
        List<String> lore = type.isInfinite()
                ? Arrays.asList("§8Скрытый кузнечный шаблон", "§7Основан на шаблоне: §fРебро", "", "§cВсе характеристики: §lБЕСКОНЕЧНО", "§7Материал: §fЗвезда Незера")
                : Arrays.asList("§8Кузнечный шаблон улучшения", "§7Улучшает оружие, броню и инструменты.", "", "§eВсе характеристики: §6+" + type.getLevel(), "§7Материал: §f" + getMaterialName(type));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(upgradeKey, PersistentDataType.STRING, type.getId());
        item.setItemMeta(meta);
        return item;
    }

    public UpgradeType getUpgradeType(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(upgradeKey, PersistentDataType.STRING);
        return id == null ? null : UpgradeType.fromId(id);
    }

    public boolean isUpgrade(ItemStack item) {
        return getUpgradeType(item) != null;
    }

    public UpgradeType[] getTypes() {
        return UpgradeType.values();
    }

    private String getMaterialName(UpgradeType type) {
        return switch (type) {
            case GOLD -> "Золотой слиток";
            case EMERALD -> "Изумруд";
            case DIAMOND -> "Алмаз";
            case NETHERITE -> "Незеритовый слиток";
            case ARMAGEDDON -> "Звезда Незера";
        };
    }
}
