package ru.dagxam.forgeupgrade.upgrade;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Создаёт и безопасно распознаёт предметы улучшений. */
public final class UpgradeManager {
    private final NamespacedKey upgradeKey;
    private final Map<UpgradeType, Material> icons = new EnumMap<>(UpgradeType.class);

    public UpgradeManager(JavaPlugin plugin) {
        this.upgradeKey = new NamespacedKey(plugin, "upgrade_type");
        icons.put(UpgradeType.GOLD, Material.GOLD_INGOT);
        icons.put(UpgradeType.EMERALD, Material.EMERALD);
        icons.put(UpgradeType.DIAMOND, Material.DIAMOND);
        icons.put(UpgradeType.NETHERITE, Material.NETHERITE_INGOT);
        icons.put(UpgradeType.ARMAGEDDON, Material.NETHER_STAR);
    }

    public ItemStack createUpgrade(UpgradeType type) {
        ItemStack item = new ItemStack(icons.get(type));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName("§6⚒ " + type.getDisplayName());
        List<String> lore = type.isInfinite()
                ? Arrays.asList("§8Скрытое улучшение", "", "§cВсе характеристики: §lБЕСКОНЕЧНО", "§7После +70 открывает безграничную силу.")
                : Arrays.asList("§7Улучшает все характеристики предмета.", "", "§eУровень улучшения: §6+" + type.getLevel(), "§8Предыдущее улучшение заменяется.");
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
}
