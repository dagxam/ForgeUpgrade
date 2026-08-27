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

/** Компактный цифровой HUD реального здоровья и брони для любого улучшения. */
public final class UpgradeStatusHudListener {
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
        if (!hasAnyUpgradedArmor(player)) return;

        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
        AttributeInstance armorAttribute = player.getAttribute(Attribute.ARMOR);
        double health = player.getHealth();
        double maxHealth = maxHealthAttribute == null ? 0.0D : maxHealthAttribute.getValue();
        double armor = armorAttribute == null ? 0.0D : armorAttribute.getValue();

        String message = "§c❤ §f" + format(health) + "§7/§f" + format(maxHealth)
                + "    §b🛡 §f" + format(armor);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    private boolean hasAnyUpgradedArmor(Player player) {
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && upgradeApplier.getAppliedType(item) != null) return true;
        }
        return false;
    }

    private String format(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "0";
        return String.format(java.util.Locale.ROOT, "%.0f", Math.max(0.0D, value));
    }
}
