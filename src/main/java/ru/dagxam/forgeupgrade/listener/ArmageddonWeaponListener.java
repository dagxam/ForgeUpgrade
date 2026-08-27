package ru.dagxam.forgeupgrade.listener;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import ru.dagxam.forgeupgrade.upgrade.UpgradeApplier;
import ru.dagxam.forgeupgrade.upgrade.UpgradeType;

import java.util.Map;

/** Способности Армагедона для оружия и дальнобойных предметов. */
public final class ArmageddonWeaponListener implements Listener {
    private final JavaPlugin plugin;
    private final UpgradeApplier upgradeApplier;
    private final NamespacedKey projectileKey;

    public ArmageddonWeaponListener(JavaPlugin plugin, UpgradeApplier upgradeApplier) {
        this.plugin = plugin;
        this.upgradeApplier = upgradeApplier;
        this.projectileKey = new NamespacedKey(plugin, "armageddon_projectile");
    }

    private boolean isArmageddon(ItemStack item) {
        return item != null && upgradeApplier.getAppliedType(item) == UpgradeType.ARMAGEDDON;
    }

    private boolean isMeleeWeapon(Material material) {
        return material.name().endsWith("_SWORD") || material.name().endsWith("_AXE")
                || material.name().endsWith("_SPEAR") || material == Material.TRIDENT;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        if (event.getDamager() instanceof Player player) {
            ItemStack weapon = player.getInventory().getItemInMainHand();
            if (isMeleeWeapon(weapon.getType()) && isArmageddon(weapon)) {
                event.setDamage(Math.max(target.getHealth(), target.getMaxHealth()) + 1024.0D);
            }
            return;
        }

        if (event.getDamager() instanceof AbstractArrow arrow
                && arrow.getPersistentDataContainer().has(projectileKey, PersistentDataType.BYTE)) {
            event.setDamage(Math.max(target.getHealth(), target.getMaxHealth()) + 1024.0D);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) return;
        if (!isArmageddon(event.getBow())) return;

        arrow.getPersistentDataContainer().set(projectileKey, PersistentDataType.BYTE, (byte) 1);

        // В API 26.2 старый setConsumeItem устарел и больше не является надёжным.
        // Поэтому после фактического выстрела возвращаем ровно одну использованную стрелу.
        ItemStack consumed = event.getConsumable();
        if (consumed != null && event.shouldConsumeItem()) {
            restoreOneArrow(player, consumed);
        } else if (event.getBow() != null && event.getBow().getType() == Material.CROSSBOW) {
            // Арбалет расходует стрелу при зарядке, а не при самом выстреле.
            // После выстрела возвращаем одну стрелу, поэтому одна стрела становится бесконечной.
            restoreOneArrow(player, new ItemStack(Material.ARROW));
        }
    }

    private void restoreOneArrow(Player player, ItemStack source) {
        ItemStack returned = source.clone();
        returned.setAmount(1);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Map<Integer, ItemStack> left = player.getInventory().addItem(returned);
            left.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
            player.updateInventory();
        });
    }
}
