package ru.dagxam.forgeupgrade.listener;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.dagxam.forgeupgrade.upgrade.UpgradeApplier;
import ru.dagxam.forgeupgrade.upgrade.UpgradeType;

/**
 * Выдаёт способности Армагедона только при надетой незеритовой броне с +∞.
 * Состояние проверяется редко, один раз в секунду, без тяжёлого постоянного сканирования.
 */
public final class ArmageddonArmorListener implements Listener {
    private final JavaPlugin plugin;
    private final UpgradeApplier upgradeApplier;

    public ArmageddonArmorListener(JavaPlugin plugin, UpgradeApplier upgradeApplier) {
        this.plugin = plugin;
        this.upgradeApplier = upgradeApplier;
        startUpdater();
    }

    private void startUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    updatePlayer(player);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private boolean hasArmageddon(Player player, Material material) {
        ItemStack item = switch (material) {
            case NETHERITE_HELMET -> player.getInventory().getHelmet();
            case NETHERITE_CHESTPLATE -> player.getInventory().getChestplate();
            case NETHERITE_LEGGINGS -> player.getInventory().getLeggings();
            case NETHERITE_BOOTS -> player.getInventory().getBoots();
            default -> null;
        };
        return item != null
                && item.getType() == material
                && upgradeApplier.getAppliedType(item) == UpgradeType.ARMAGEDDON;
    }

    private void updatePlayer(Player player) {
        boolean helmet = hasArmageddon(player, Material.NETHERITE_HELMET);
        boolean chestplate = hasArmageddon(player, Material.NETHERITE_CHESTPLATE);
        boolean leggings = hasArmageddon(player, Material.NETHERITE_LEGGINGS);
        boolean boots = hasArmageddon(player, Material.NETHERITE_BOOTS);

        if (helmet) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 220, 0, true, false, false));
        } else {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }

        if (boots) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 220, 1, true, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 220, 0, true, false, false));
        } else {
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            player.removePotionEffect(PotionEffectType.SPEED);
        }

        if (chestplate) {
            if (!player.isFlying() && !player.getAllowFlight()) {
                player.setAllowFlight(true);
            }
        } else if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            if (player.getAllowFlight()) player.setAllowFlight(false);
            if (player.isFlying()) player.setFlying(false);
        }

        if (leggings && player.getRemainingAir() < player.getMaximumAir()) {
            player.setRemainingAir(player.getMaximumAir());
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!hasArmageddon(player, Material.NETHERITE_LEGGINGS)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.LAVA
                || cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.HOT_FLOOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onAirChange(EntityAirChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (hasArmageddon(player, Material.NETHERITE_LEGGINGS)) {
            event.setAmount(player.getMaximumAir());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        updatePlayer(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> updatePlayer(event.getPlayer()));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        updatePlayer(event.getPlayer());
    }
}
