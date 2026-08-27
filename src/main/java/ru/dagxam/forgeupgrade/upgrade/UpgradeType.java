package ru.dagxam.forgeupgrade.upgrade;

import org.bukkit.Material;

/** Типы улучшений и их реальные шаблоны/материалы для стола улучшений. */
public enum UpgradeType {
    GOLD("gold", "Золотое улучшение", 10, 3, Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, Material.GOLD_INGOT),
    EMERALD("emerald", "Изумрудное улучшение", 30, 5, Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, Material.EMERALD),
    DIAMOND("diamond", "Алмазное улучшение", 50, 7, Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, Material.DIAMOND),
    NETHERITE("netherite", "Незеритовое улучшение", 70, 9, Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, Material.NETHERITE_INGOT),
    ARMAGEDDON("armageddon", "Армагедон", 999999, 15, Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, Material.NETHER_STAR);

    private final String id;
    private final String displayName;
    private final int level;
    private final int toolEfficiency;
    private final Material smithingTemplate;
    private final Material smithingMaterial;

    UpgradeType(String id, String displayName, int level, int toolEfficiency,
                Material smithingTemplate, Material smithingMaterial) {
        this.id = id;
        this.displayName = displayName;
        this.level = level;
        this.toolEfficiency = toolEfficiency;
        this.smithingTemplate = smithingTemplate;
        this.smithingMaterial = smithingMaterial;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getLevel() { return level; }
    public int getToolEfficiency() { return toolEfficiency; }
    public Material getSmithingTemplate() { return smithingTemplate; }
    public Material getSmithingMaterial() { return smithingMaterial; }
    public boolean requiresSmithingTemplate() { return true; }
    public boolean isInfinite() { return this == ARMAGEDDON; }

    /** Безопасный конечный бонус Армагедона: +999999 ко всем поддерживаемым характеристикам. */
    public int getAttributeBonus() { return level; }

    public static UpgradeType fromId(String id) {
        if (id == null) return null;
        for (UpgradeType type : values()) {
            if (type.id.equalsIgnoreCase(id)) return type;
        }
        return null;
    }
}
