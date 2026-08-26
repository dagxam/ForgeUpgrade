package ru.dagxam.forgeupgrade.listener;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.dagxam.forgeupgrade.upgrade.UpgradeApplier;
import ru.dagxam.forgeupgrade.upgrade.UpgradeType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Способности Армагедона для незеритовой брони.
 * Проверка выполняется раз в секунду. Плагин не снимает чужие эффекты игрока.
 */
public final class ArmageddonArmorListener implements Listener {
    private final JavaPlugin plugin;
    private final UpgradeApplier upgradeApplier;
    private final Set<UUID> pluginFlight = new HashSet<>();

    public ArmageddonArmorListener(JavaPlugin plugin, UpgradeApplier upgradeApplier) {
        this.plugin = plugin;
        this.upgradeApplier = upgradeApplier;
        startUpdater();
    }

    private void startUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) updatePlayer(player);
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
        return item != null && item.getType() == material
                && upgradeApplier.getAppliedType(item) == UpgradeType.ARMAGEDDON;
    }

    private void updatePlayer(Player player) {
        boolean helmet = hasArmageddon(player, Material.NETHERITE_HELMET);
        boolean chestplate = hasArmageddon(player, Material.NETHERITE_CHESTPLATE);
        boolean leggings = hasArmageddon(player, Material.NETHERITE_LEGGINGS);
        boolean boots = hasArmageddon(player, Material.NETHERITE_BOOTS);

        // Короткие эффекты просто перестают обновляться после снятия брони.
        // Это не удаляет эффекты, выданные другими плагинами или зельями.
        if (helmet) giveEffect(player, PotionEffectType.NIGHT_VISION, 60, 0);
        if (boots) {
            giveEffect(player, PotionEffectType.JUMP_BOOST, 60, 1);
            giveEffect(player, PotionEffectType.SPEED, 60, 0);
        }

        UUID id = player.getUniqueId();
        if (chestplate) {
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
                pluginFlight.add(id);
            }
        } else if (pluginFlight.remove(id)
                && player.getGameMode() != GameMode.CREATIVE
                && player.getGameMode() != GameMode.SPECTATOR) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }

        if (leggings && player.getRemainingAir() < player.getMaximumAir()) {
            player.setRemainingAir(player.getMaximumAir());
        }
    }

    private void giveEffect(Player player, PotionEffectType type, int duration, int amplifier) {
        PotionEffect current = player.getPotionEffect(type);
        if (current != null && current.getDuration() > duration / 2 && current.getAmplifier() >= amplifier) return;
        player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, false));
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!hasArmageddon(player, Material.NETHERITE_LEGGINGS)) return;
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.LAVA || cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK || cause == EntityDamageEvent.DamageCause.HOT_FLOOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onAirChange(EntityAirChangeEvent event) {
        if (event.getEntity() instanceof Player player && hasArmageddon(player, Material.NETHERITE_LEGGINGS)) {
            event.setAmount(player.getMaximumAir());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) { updatePlayer(event.getPlayer()); }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> updatePlayer(event.getPlayer()));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) { updatePlayer(event.getPlayer()); }
}
