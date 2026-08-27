package ru.dagxam.forgeupgrade.listener;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import ru.dagxam.forgeupgrade.upgrade.UpgradeApplier;
import ru.dagxam.forgeupgrade.upgrade.UpgradeType;

/** Стандартные сердца Minecraft + компактные числовые бонусы здоровья и брони над хотбаром. */
public final class UpgradeStatusHudListener {
    private static final double STANDARD_HEALTH_SCALE = 20.0D;
    private static final String HEARTS = "❤❤❤❤❤❤❤❤❤❤";
    private static final String SHIELDS = "🛡🛡🛡🛡🛡🛡🛡🛡🛡🛡";

    private final JavaPlugin plugin;
    private final UpgradeApplier upgradeApplier;

    public UpgradeStatusHudListener(JavaPlugin plugin, UpgradeApplier upgradeApplier) {
        this.plugin = plugin;
        this.upgradeApplier = upgradeApplier;
        start();
    }

    private void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) update(player);
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }

    private void update(Player player) {
        // Автоматически переносим старую улучшенную броню на исправленные уникальные модификаторы.
        for (ItemStack item : player.getInventory().getArmorContents()) {
            upgradeApplier.refreshAttributes(item);
        }

        UpgradeType visualType = getHighestArmorUpgrade(player);
        if (visualType == null) {
            if (player.isHealthScaled()) player.setHealthScaled(false);
            return;
        }

        // Всегда оставляем ровно стандартные 10 визуальных сердец, независимо от огромного MAX_HEALTH.
        player.setHealthScaled(true);
        player.setHealthScale(STANDARD_HEALTH_SCALE);

        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        AttributeInstance armorAttribute = player.getAttribute(Attribute.ARMOR);
        double maxHealth = maxHealthAttribute == null ? 20.0D : maxHealthAttribute.getValue();
        double actualArmor = armorAttribute == null ? 0.0D : armorAttribute.getValue();

        int armorBonus = getArmorBonus(player);
        double healthBonus = Math.max(0.0D, maxHealth - 20.0D);
        double baseArmor = Math.max(0.0D, actualArmor - armorBonus);
        double totalArmorBonus = Math.max(0.0D, actualArmor - baseArmor);

        String color = color(visualType);
        String message = color + HEARTS + " §f+" + color + format(healthBonus)
                + "    " + color + SHIELDS + " §f+" + color + format(totalArmorBonus)
                + "    §8[" + color + visualType.getDisplayName() + "§8]";
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    private UpgradeType getHighestArmorUpgrade(Player player) {
        UpgradeType highest = null;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            UpgradeType type = upgradeApplier.getAppliedType(item);
            if (type == null) continue;
            if (highest == null || type.getLevel() > highest.getLevel() || type.isInfinite()) highest = type;
        }
        return highest;
    }

    private int getArmorBonus(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            UpgradeType type = upgradeApplier.getAppliedType(item);
            if (type != null) total += type.getAttributeBonus();
        }
        return total;
    }

    private String color(UpgradeType type) {
        return switch (type) {
            case GOLD -> "§6";
            case EMERALD -> "§a";
            case DIAMOND -> "§b";
            case NETHERITE -> "§6";
            case ARMAGEDDON -> "§c";
        };
    }

    private String format(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "0";
        return String.format(java.util.Locale.ROOT, "%.0f", Math.max(0.0D, value));
    }
}
