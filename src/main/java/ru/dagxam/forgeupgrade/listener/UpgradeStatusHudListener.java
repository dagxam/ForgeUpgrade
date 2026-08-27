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

/**
 * HUD улучшенной брони.
 * Не рисует вторые сердца и вторую броню: стандартный HUD Minecraft остаётся как есть.
 * Плагин выводит только точные числовые значения здоровья и брони.
 */
public final class UpgradeStatusHudListener {
    private static final double STANDARD_HEALTH_SCALE = 20.0D;

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
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    update(player);
                }
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }

    private void update(Player player) {
        // Переприменяем атрибуты старой улучшенной брони с актуальными модификаторами.
        for (ItemStack item : player.getInventory().getArmorContents()) {
            upgradeApplier.refreshAttributes(item);
        }

        UpgradeType visualType = getHighestArmorUpgrade(player);
        if (visualType == null) {
            if (player.isHealthScaled()) {
                player.setHealthScaled(false);
            }
            return;
        }

        // Оставляем обычные 10 сердец Minecraft, не создавая второй ряд сердец.
        player.setHealthScaled(true);
        player.setHealthScale(STANDARD_HEALTH_SCALE);

        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        AttributeInstance armorAttribute = player.getAttribute(Attribute.ARMOR);

        // Точные реальные значения, которые сейчас используются сервером.
        // MAX_HEALTH хранится в health points: 20 points = 10 сердечек.
        double exactHealthPoints = maxHealthAttribute == null ? 20.0D : maxHealthAttribute.getValue();
        double exactHeartCount = Math.max(0.0D, exactHealthPoints / 2.0D);
        double exactArmor = armorAttribute == null ? 0.0D : Math.max(0.0D, armorAttribute.getValue());

        String color = color(visualType);

        // ВАЖНО: здесь больше НЕТ нарисованных ❤ и 🛡.
        // На экране остаются только стандартные иконки Minecraft, а плагин выводит
        // только точные цифры: здоровье и броня.
        // Формат: +999999 HP   +999999 ARMOR
        String message = color + "+" + format(exactHeartCount)
                + " §7❤   "
                + color + "+" + format(exactArmor)
                + " §7🛡";

        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(message)
        );
    }

    private UpgradeType getHighestArmorUpgrade(Player player) {
        UpgradeType highest = null;
        for (ItemStack item : player.getInventory().getArmorContents()) {
            UpgradeType type = upgradeApplier.getAppliedType(item);
            if (type == null) continue;
            if (highest == null || type.getLevel() > highest.getLevel() || type.isInfinite()) {
                highest = type;
            }
        }
        return highest;
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
        double safe = Math.max(0.0D, value);
        if (Math.abs(safe - Math.rint(safe)) < 0.000001D) {
            return String.format(java.util.Locale.ROOT, "%.0f", safe);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", safe)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }
}
