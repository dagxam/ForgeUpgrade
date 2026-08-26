package ru.dagxam.forgeupgrade.listener;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import ru.dagxam.forgeupgrade.upgrade.AttributeUpgradeManager;
import ru.dagxam.forgeupgrade.upgrade.UpgradeApplier;
import ru.dagxam.forgeupgrade.upgrade.UpgradeType;

/** Применяет обычные бонусы +10/+30/+50/+70 только в момент урона. */
public final class UpgradeAttributeListener implements Listener {
    private final UpgradeApplier upgradeApplier;
    private final AttributeUpgradeManager attributes;

    public UpgradeAttributeListener(UpgradeApplier upgradeApplier, AttributeUpgradeManager attributes) {
        this.upgradeApplier = upgradeApplier;
        this.attributes = attributes;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (upgradeApplier.getAppliedType(weapon) == UpgradeType.ARMAGEDDON) return;
        double bonus = attributes.getAttackBonus(weapon);
        if (bonus > 0.0D && attributes.isWeapon(weapon.getType())) {
            event.setDamage(event.getDamage() + bonus);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) return;

        double reduction = 0.0D;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor == null || armor.getType().isAir()) continue;
            if (upgradeApplier.getAppliedType(armor) == UpgradeType.ARMAGEDDON) continue;
            reduction += attributes.getArmorReduction(armor);
        }
        if (reduction <= 0.0D) return;
        reduction = Math.min(reduction, 0.80D);
        event.setDamage(event.getDamage() * (1.0D - reduction));
    }
}
