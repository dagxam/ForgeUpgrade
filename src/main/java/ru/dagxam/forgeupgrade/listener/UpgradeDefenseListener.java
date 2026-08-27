package ru.dagxam.forgeupgrade.listener;

import org.bukkit.Material;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import ru.dagxam.forgeupgrade.upgrade.UpgradeApplier;
import ru.dagxam.forgeupgrade.upgrade.UpgradeType;

/** Дополнительные защитные способности всех типов улучшенной брони. */
public final class UpgradeDefenseListener implements Listener {
    private final UpgradeApplier upgradeApplier;

    public UpgradeDefenseListener(JavaPlugin plugin, UpgradeApplier upgradeApplier) {
        this.upgradeApplier = upgradeApplier;
    }

    private boolean hasUpgrade(Player player, UpgradeType type) {
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && upgradeApplier.getAppliedType(item) == type) return true;
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        EntityDamageEvent.DamageCause cause = event.getCause();

        if ((cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.LAVA
                || cause == EntityDamageEvent.DamageCause.HOT_FLOOR)
                && (hasUpgrade(player, UpgradeType.DIAMOND)
                || hasUpgrade(player, UpgradeType.NETHERITE)
                || hasUpgrade(player, UpgradeType.ARMAGEDDON))) {
            event.setCancelled(true);
            return;
        }

        if (cause == EntityDamageEvent.DamageCause.DROWNING
                && (hasUpgrade(player, UpgradeType.DIAMOND)
                || hasUpgrade(player, UpgradeType.NETHERITE)
                || hasUpgrade(player, UpgradeType.ARMAGEDDON))) {
            event.setCancelled(true);
            return;
        }

        if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                && (hasUpgrade(player, UpgradeType.EMERALD)
                || hasUpgrade(player, UpgradeType.NETHERITE)
                || hasUpgrade(player, UpgradeType.ARMAGEDDON))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectile(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof Projectile projectile)) return;
        if (!(projectile.getShooter() instanceof Skeleton)) return;

        if (hasUpgrade(player, UpgradeType.GOLD)
                || hasUpgrade(player, UpgradeType.NETHERITE)
                || hasUpgrade(player, UpgradeType.ARMAGEDDON)) {
            event.setCancelled(true);
        }
    }
}
