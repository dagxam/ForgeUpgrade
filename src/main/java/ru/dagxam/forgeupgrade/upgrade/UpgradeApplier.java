package ru.dagxam.forgeupgrade.upgrade;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/** Хранит уровень улучшения и применяет реальные атрибуты предмета. */
public final class UpgradeApplier {
    private static final String MARKER = "§8[ForgeUpgrade]";
    private static final String LEVEL_PREFIX = "§eВсе характеристики: §6+";
    private final NamespacedKey typeKey;
    private final NamespacedKey levelKey;
    private final AttributeUpgradeManager attributeUpgradeManager;

    public UpgradeApplier(JavaPlugin plugin, AttributeUpgradeManager attributeUpgradeManager,
                          ArmorTrimUpgradeManager ignoredArmorTrimUpgradeManager) {
        this.typeKey = new NamespacedKey(plugin, "applied_upgrade_type");
        this.levelKey = new NamespacedKey(plugin, "upgrade_level");
        this.attributeUpgradeManager = attributeUpgradeManager;
    }

    public boolean isSupported(ItemStack item) {
        if (item == null || item.getType().isAir() || item.getAmount() != 1) return false;
        String name = item.getType().name();
        return item.getType() == Material.SHIELD || name.endsWith("_SWORD") || name.endsWith("_AXE")
                || name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE")
                || name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS") || item.getType() == Material.TRIDENT || item.getType() == Material.BOW
                || item.getType() == Material.CROSSBOW;
    }

    public UpgradeType getAppliedType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        return id == null ? null : UpgradeType.fromId(id);
    }

    public int getLevel(ItemStack item) {
        UpgradeType type = getAppliedType(item);
        return type == null ? 0 : type.getLevel();
    }

    /**
     * Любое улучшение можно применить сразу к обычному предмету.
     * Не требуется проходить цепочку +10 -> +30 -> +50 -> +70.
     * Новое улучшение полностью заменяет предыдущее, если это другой тип.
     */
    public Result validate(ItemStack item, UpgradeType next) {
        if (!isSupported(item) || next == null) return Result.UNSUPPORTED;
        UpgradeType current = getAppliedType(item);
        if (current == next) return Result.ALREADY_APPLIED;
        return Result.SUCCESS;
    }

    public Result apply(ItemStack item, UpgradeType next) {
        Result result = validate(item, next);
        if (result != Result.SUCCESS) return result;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Result.UNSUPPORTED;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(typeKey, PersistentDataType.STRING, next.getId());
        data.set(levelKey, PersistentDataType.INTEGER, next.getLevel());
        updateDisplay(meta, item, next);
        item.setItemMeta(meta);

        // Шаблон используется только как тип улучшения. Ванильная отделка предмету не добавляется.
        attributeUpgradeManager.apply(item, next, next.getLevel());
        return Result.SUCCESS;
    }

    private void updateDisplay(ItemMeta meta, ItemStack item, UpgradeType type) {
        String original = meta.hasDisplayName() ? meta.getDisplayName() : getVanillaName(item.getType());
        original = original.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
        original = original.replaceAll("\\s+\\+(?:∞|\\d+)$", "").trim();
        meta.setDisplayName(type.isInfinite() ? "§4☠ " + original + " §c+∞" : "§6⚒ " + original + " §e+" + type.getLevel());

        List<String> lore = new ArrayList<>();
        if (meta.hasLore() && meta.getLore() != null) {
            for (String line : meta.getLore()) if (!isForgeLore(line)) lore.add(line);
        }
        lore.add(MARKER);
        lore.add(type.isInfinite() ? "§4Улучшение: §cАрмагедон" : "§6Улучшение: §e" + type.getDisplayName());
        lore.add(type.isInfinite() ? "§cВсе характеристики: §l+99999" : LEVEL_PREFIX + type.getLevel());
        lore.add("§8Новое улучшение заменяет предыдущее.");
        meta.setLore(lore);
    }

    private boolean isForgeLore(String line) {
        return line != null && (line.equals(MARKER) || line.startsWith("§4Улучшение: §cАрмагедон")
                || line.startsWith("§6Улучшение: §e") || line.startsWith(LEVEL_PREFIX)
                || line.equals("§cВсе характеристики: §l+99999")
                || line.equals("§8Новое улучшение заменяет предыдущее."));
    }

    private String getVanillaName(Material material) {
        return material.name().toLowerCase().replace('_', ' ');
    }

    public enum Result { SUCCESS, UNSUPPORTED, ALREADY_APPLIED, CANNOT_DOWNGRADE, WRONG_ORDER }
}
