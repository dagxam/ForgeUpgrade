package ru.dagxam.forgeupgrade.listener;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Оставляет реальное увеличенное MAX_HEALTH, но масштабирует только клиентскую
 * шкалу здоровья до стандартных 10 сердец. Никакой дополнительный HUD не рисуется.
 */
public final class HealthHudListener implements Listener {
    private static final double STANDARD_HEALTH_BAR = 20.0D;
    private final JavaPlugin plugin;

    public HealthHudListener(JavaPlugin plugin) {
        this.plugin = plugin;
        startUpdater();
    }

    private void startUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    applyScale(player);
                }
            }
        }.runTaskTimer(plugin, 1L, 10L);
    }

    private void applyScale(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) return;

        // Если реальное здоровье больше ванильных 20 HP, показываем его на
        // стандартных 10 сердцах. Реальный MAX_HEALTH и получаемый урон не меняются.
        if (maxHealth.getValue() > STANDARD_HEALTH_BAR) {
            if (!player.isHealthScaled()) player.setHealthScaled(true);
            if (player.getHealthScale() != STANDARD_HEALTH_BAR) {
                player.setHealthScale(STANDARD_HEALTH_BAR);
            }
        } else if (player.isHealthScaled()) {
            player.setHealthScaled(false);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> applyScale(event.getPlayer()));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> applyScale(event.getPlayer()));
    }
}
