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

/** Компактный цифровой HUD реального здоровья и брони для любого улучшения. */
public final class UpgradeStatusHudListener {
    // Реальное здоровье остаётся настоящим, но огромные столбцы сердец не закрывают экран.
    private static final double COMPACT_HEALTH_SCALE = 0.1D;

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
        }.runTaskTimer(plugin, 10L, 10L);
    }

    private void update(Player player) {
        UpgradeType visualType = getHighestArmorUpgrade(player);
        if (visualType == null) {
            if (player.isHealthScaled()) player.setHealthScaled(false);
            return;
        }

        player.setHealthScale(COMPACT_HEALTH_SCALE);

        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        AttributeInstance armorAttribute = player.getAttribute(Attribute.ARMOR);
        double health = player.getHealth();
        double maxHealth = maxHealthAttribute == null ? 0.0D : maxHealthAttribute.getValue();
        double actualArmor = armorAttribute == null ? 0.0D : armorAttribute.getValue();

        // Считаем бонус прямо по каждому надетому улучшенному предмету.
        // Поэтому цифры HUD всегда показывают полный прирост брони, даже если стандартная
        // шкала Minecraft визуально ограничена и не умеет рисовать сотни тысяч значков.
        int armorBonus = getArmorBonus(player);
        double baseArmor = Math.max(0.0D, actualArmor - armorBonus);
        double displayArmor = Math.max(actualArmor, baseArmor + armorBonus);

        String color = color(visualType);
        String name = visualType.getDisplayName();
        String message = color + "❤ " + format(health) + "§7/" + color + format(maxHealth)
                + "    " + color + "🛡 " + format(displayArmor)
                + " §7(+" + color + format(armorBonus) + "§7)"
                + "    §8[" + color + name + "§8]";
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
