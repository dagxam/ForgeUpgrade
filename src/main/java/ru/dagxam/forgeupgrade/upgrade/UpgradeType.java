package ru.dagxam.forgeupgrade.upgrade;

public enum UpgradeType {
    GOLD("gold", "Золотое улучшение", 10),
    EMERALD("emerald", "Изумрудное улучшение", 30),
    DIAMOND("diamond", "Алмазное улучшение", 50),
    NETHERITE("netherite", "Незеритовое улучшение", 70),
    ARMAGEDDON("armageddon", "Армагедон", -1);

    private final String id;
    private final String displayName;
    private final int level;

    UpgradeType(String id, String displayName, int level) {
        this.id = id;
        this.displayName = displayName;
        this.level = level;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getLevel() { return level; }
    public boolean isInfinite() { return this == ARMAGEDDON; }

    public static UpgradeType fromId(String id) {
        for (UpgradeType type : values()) {
            if (type.id.equalsIgnoreCase(id)) return type;
        }
        return null;
    }
}
