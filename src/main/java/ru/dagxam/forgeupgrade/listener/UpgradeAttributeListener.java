package ru.dagxam.forgeupgrade.listener;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import ru.dagxam.forgeupgrade.upgrade.UpgradeApplier;

/**
 * Делает улучшение лука и арбалета реальным: бонус сохраняется на выпущенной стреле
 * и прибавляется к фактическому урону при попадании.
 */
public final class UpgradeAttributeListener implements Listener {
    private final UpgradeApplier upgradeApplier;
    private final NamespacedKey projectileBonusKey;

    public UpgradeAttributeListener(JavaPlugin plugin, UpgradeApplier upgradeApplier) {
        this.upgradeApplier = upgradeApplier;
        this.projectileBonusKey = new NamespacedKey(plugin, "upgrade_projectile_bonus");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) return;
        ItemStack weapon = event.getBow();
        if (weapon == null) return;

        int bonus = upgradeApplier.getLevel(weapon);
        if (bonus <= 0) return;
        arrow.getPersistentDataContainer().set(projectileBonusKey, PersistentDataType.INTEGER, bonus);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArrowDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof AbstractArrow arrow)) return;
        Integer bonus = arrow.getPersistentDataContainer().get(projectileBonusKey, PersistentDataType.INTEGER);
        if (bonus == null || bonus <= 0) return;
        event.setDamage(event.getDamage() + bonus);
    }
}
