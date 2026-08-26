package ru.dagxam.forgeupgrade.upgrade;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

/**
 * Применяет отделку брони, соответствующую уровню ForgeUpgrade.
 * Используется отражение, чтобы не привязывать компиляцию к изменённым
 * сигнатурам Bukkit API 26.2.
 */
public final class ArmorTrimUpgradeManager {
    private final JavaPlugin plugin;

    public ArmorTrimUpgradeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void apply(ItemStack item, UpgradeType type) {
        if (item == null || !isArmor(item.getType()) || type.isInfinite()) return;

        String patternKey = switch (type) {
            case GOLD -> "dune";
            case EMERALD -> "tide";
            case DIAMOND -> "flow";
            case NETHERITE -> "sentry";
            default -> null;
        };
        if (patternKey == null) return;

        try {
            Class<?> armorMetaClass = Class.forName("org.bukkit.inventory.meta.ArmorMeta");
            Class<?> armorTrimClass = Class.forName("org.bukkit.inventory.meta.trim.ArmorTrim");
            Class<?> trimMaterialClass = Class.forName("org.bukkit.inventory.meta.trim.TrimMaterial");
            Class<?> trimPatternClass = Class.forName("org.bukkit.inventory.meta.trim.TrimPattern");
            Class<?> registryClass = Class.forName("org.bukkit.Registry");
            Class<?> namespacedKeyClass = Class.forName("org.bukkit.NamespacedKey");

            Object meta = item.getItemMeta();
            if (!armorMetaClass.isInstance(meta)) return;

            Method minecraft = namespacedKeyClass.getMethod("minecraft", String.class);
            Object patternKeyObject = minecraft.invoke(null, patternKey);
            Object materialKeyObject = minecraft.invoke(null, getMaterialKey(type));

            Object patternRegistry = registryClass.getField("TRIM_PATTERN").get(null);
            Object materialRegistry = registryClass.getField("TRIM_MATERIAL").get(null);
            Method get = patternRegistry.getClass().getMethod("get", namespacedKeyClass);
            Object pattern = get.invoke(patternRegistry, patternKeyObject);
            Object material = materialRegistry.getClass().getMethod("get", namespacedKeyClass)
                    .invoke(materialRegistry, materialKeyObject);

            if (pattern == null || material == null) return;

            Object trim = armorTrimClass
                    .getConstructor(trimMaterialClass, trimPatternClass)
                    .newInstance(material, pattern);
            armorMetaClass.getMethod("setTrim", armorTrimClass).invoke(meta, trim);
            item.setItemMeta((org.bukkit.inventory.meta.ItemMeta) meta);
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning("Не удалось применить отделку брони ForgeUpgrade: " + exception.getMessage());
        }
    }

    private String getMaterialKey(UpgradeType type) {
        return switch (type) {
            case GOLD -> "gold";
            case EMERALD -> "emerald";
            case DIAMOND -> "diamond";
            case NETHERITE -> "netherite";
            default -> "iron";
        };
    }

    private boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }
}
