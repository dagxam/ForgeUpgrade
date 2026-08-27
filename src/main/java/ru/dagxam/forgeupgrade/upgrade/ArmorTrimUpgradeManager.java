package ru.dagxam.forgeupgrade.upgrade;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Применяет цветную отделку брони, соответствующую уровню ForgeUpgrade. */
public final class ArmorTrimUpgradeManager {
    private final JavaPlugin plugin;

    public ArmorTrimUpgradeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void apply(ItemStack item, UpgradeType type) {
        if (item == null || !isArmor(item.getType()) || type == null) return;

        String patternKey = switch (type) {
            case GOLD -> "dune";
            case EMERALD -> "tide";
            case DIAMOND -> "flow";
            case NETHERITE -> "sentry";
            case ARMAGEDDON -> "spire";
        };

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

            Field patternField = registryClass.getField("TRIM_PATTERN");
            Field materialField = registryClass.getField("TRIM_MATERIAL");
            Object patternRegistry = patternField.get(null);
            Object materialRegistry = materialField.get(null);

            // Получаем метод у интерфейса Registry, а не у внутренней реализации.
            // В 26.2 реализация реестра может быть package-private, из-за чего старый код
            // с patternRegistry.getClass().getMethod(...) не применял отделку вообще.
            Method get = registryClass.getMethod("get", namespacedKeyClass);
            Object pattern = get.invoke(patternRegistry, patternKeyObject);
            Object material = get.invoke(materialRegistry, materialKeyObject);
            if (pattern == null || material == null) return;

            Constructor<?> constructor = armorTrimClass.getConstructor(trimMaterialClass, trimPatternClass);
            Object trim = constructor.newInstance(material, pattern);
            armorMetaClass.getMethod("setTrim", armorTrimClass).invoke(meta, trim);
            item.setItemMeta((org.bukkit.inventory.meta.ItemMeta) meta);
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning("Не удалось применить цветную отделку ForgeUpgrade: " + exception.getMessage());
        }
    }

    private String getMaterialKey(UpgradeType type) {
        return switch (type) {
            case GOLD -> "gold";
            case EMERALD -> "emerald";
            case DIAMOND -> "diamond";
            // Пользовательский медно-оранжевый визуальный стиль для незеритового улучшения.
            case NETHERITE -> "copper";
            // Армагеддон: красная отделка + отдельный магический блеск предмета.
            case ARMAGEDDON -> "redstone";
        };
    }

    private boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }
}
