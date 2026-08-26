package ru.dagxam.forgeupgrade.upgrade;

import org.bukkit.Material;

public enum UpgradeType {
    GOLD("gold", "Золотое улучшение", 10, Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE),
    EMERALD("emerald", "Изумрудное улучшение", 30, Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE),
    DIAMOND("diamond", "Алмазное улучшение", 50, Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE),
    NETHERITE("netherite", "Незеритовое улучшение", 70, Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE),
    ARMAGEDDON("armageddon", "Армагедон", -1, null);

    private final String id;
    private final String displayName;
    private final int level;
    private final Material smithingTemplate;

    UpgradeType(String id, String displayName, int level, Material smithingTemplate) {
        this.id = id;
        this.displayName = displayName;
        this.level = level;
        this.smithingTemplate = smithingTemplate;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getLevel() { return level; }
    public Material getSmithingTemplate() { return smithingTemplate; }
    public boolean requiresSmithingTemplate() { return smithingTemplate != null; }
    public boolean isInfinite() { return this == ARMAGEDDON; }

    public static UpgradeType fromId(String id) {
        if (id == null) return null;
        for (UpgradeType type : values()) {
            if (type.id.equalsIgnoreCase(id)) return type;
        }
        return null;
    }
}
