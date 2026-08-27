package ru.dagxam.forgeupgrade.listener;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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

/** Способности улучшенной брони. */
public final class ArmageddonArmorListener implements Listener {
    private static final int EFFECT_DURATION = 2400;
    private static final int EFFECT_REFRESH_AT = 1200;

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
            @Override public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) updatePlayer(player);
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private UpgradeType getUpgrade(Player player, Material material) {
        ItemStack item = switch (material) {
            case NETHERITE_HELMET -> player.getInventory().getHelmet();
            case NETHERITE_CHESTPLATE -> player.getInventory().getChestplate();
            case NETHERITE_LEGGINGS -> player.getInventory().getLeggings();
            case NETHERITE_BOOTS -> player.getInventory().getBoots();
            default -> null;
        };
        if (item == null || item.getType() != material) return null;
        return upgradeApplier.getAppliedType(item);
    }

    private boolean hasArmageddon(Player player, Material material) {
        return getUpgrade(player, material) == UpgradeType.ARMAGEDDON;
    }

    /** Проверяет любую надетую броню с указанным улучшением. */
    private boolean hasUpgradeArmor(Player player, UpgradeType wanted) {
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && upgradeApplier.getAppliedType(item) == wanted) return true;
        }
        return false;
    }

    private boolean hasAnyArmageddonArmor(Player player) {
        return hasUpgradeArmor(player, UpgradeType.ARMAGEDDON);
    }

    /** Любая улучшенная броня золотого уровня и выше восстанавливает здоровье. */
    private boolean hasHealthRegenerationArmor(Player player) {
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item == null || !item.getType().name().matches(".*_(HELMET|CHESTPLATE|LEGGINGS|BOOTS)")) continue;
            UpgradeType type = upgradeApplier.getAppliedType(item);
            if (type == UpgradeType.GOLD
                    || type == UpgradeType.EMERALD
                    || type == UpgradeType.DIAMOND
                    || type == UpgradeType.NETHERITE
                    || type == UpgradeType.ARMAGEDDON) {
                return true;
            }
        }
        return false;
    }

    private void updatePlayer(Player player) {
        boolean helmet = hasArmageddon(player, Material.NETHERITE_HELMET);
        boolean chestplate = hasArmageddon(player, Material.NETHERITE_CHESTPLATE);
        boolean leggings = hasArmageddon(player, Material.NETHERITE_LEGGINGS);
        boolean boots = hasArmageddon(player, Material.NETHERITE_BOOTS);

        if (helmet) giveStableEffect(player, PotionEffectType.NIGHT_VISION, 0);
        if (hasHealthRegenerationArmor(player)) giveStableEffect(player, PotionEffectType.REGENERATION, 0);

        if (hasAnyArmageddonArmor(player)) {
            AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                double max = maxHealth.getValue();
                if (max > 0.0D && player.getHealth() < max) player.setHealth(max);
            }
        }

        if (boots) {
            giveStableEffect(player, PotionEffectType.JUMP_BOOST, 1);
            giveStableEffect(player, PotionEffectType.SPEED, 0);
        }

        UUID id = player.getUniqueId();
        if (chestplate) {
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
                pluginFlight.add(id);
            }
        } else if (pluginFlight.remove(id) && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }

        if (leggings && player.getRemainingAir() < player.getMaximumAir()) player.setRemainingAir(player.getMaximumAir());
    }

    private void giveStableEffect(Player player, PotionEffectType type, int amplifier) {
        PotionEffect current = player.getPotionEffect(type);
        if (current != null && current.getAmplifier() >= amplifier && current.getDuration() > EFFECT_REFRESH_AT) return;
        player.addPotionEffect(new PotionEffect(type, EFFECT_DURATION, amplifier, true, false, false), true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        boolean fireOrDrowning = cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.LAVA
                || cause == EntityDamageEvent.DamageCause.HOT_FLOOR
                || cause == EntityDamageEvent.DamageCause.DROWNING;
        if (fireOrDrowning && (hasUpgradeArmor(player, UpgradeType.DIAMOND) || hasUpgradeArmor(player, UpgradeType.NETHERITE))) {
            event.setCancelled(true);
            return;
        }

        if ((cause == EntityDamageEvent.DamageCause.LAVA || cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK || cause == EntityDamageEvent.DamageCause.HOT_FLOOR)
                && hasArmageddon(player, Material.NETHERITE_LEGGINGS)) {
            event.setCancelled(true);
            return;
        }

        if (!(event instanceof EntityDamageByEntityEvent byEntity)) return;

        if (byEntity.getDamager() instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Skeleton
                && (hasUpgradeArmor(player, UpgradeType.GOLD) || hasUpgradeArmor(player, UpgradeType.NETHERITE))) {
            event.setCancelled(true);
            return;
        }

        if (byEntity.getDamager() instanceof Creeper
                && (hasUpgradeArmor(player, UpgradeType.EMERALD) || hasUpgradeArmor(player, UpgradeType.NETHERITE))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplosionDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) return;
        if (hasUpgradeArmor(player, UpgradeType.EMERALD) || hasUpgradeArmor(player, UpgradeType.NETHERITE)) event.setCancelled(true);
    }

    @EventHandler
    public void onAirChange(EntityAirChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (hasUpgradeArmor(player, UpgradeType.DIAMOND) || hasUpgradeArmor(player, UpgradeType.NETHERITE)
                || hasArmageddon(player, Material.NETHERITE_LEGGINGS)) {
            event.setAmount(player.getMaximumAir());
        }
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) { updatePlayer(event.getPlayer()); }
    @EventHandler public void onRespawn(PlayerRespawnEvent event) { plugin.getServer().getScheduler().runTask(plugin, () -> updatePlayer(event.getPlayer())); }
    @EventHandler public void onWorldChange(PlayerChangedWorldEvent event) { updatePlayer(event.getPlayer()); }
}
