package ru.dagxam.forgeupgrade.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import ru.dagxam.forgeupgrade.upgrade.UpgradeApplier;
import ru.dagxam.forgeupgrade.upgrade.UpgradeManager;
import ru.dagxam.forgeupgrade.upgrade.UpgradeType;

/**
 * Обрабатывает улучшение предметов через интерфейс стола кузнеца.
 *
 * В версии 26.2 Bukkit API используются индексы слотов SmithingInventory:
 * 0 — шаблон, 1 — улучшаемый предмет, 2 — добавочный материал.
 */
public final class SmithingUpgradeListener implements Listener {

    private final UpgradeManager upgradeManager;
    private final UpgradeApplier upgradeApplier;

    public SmithingUpgradeListener(UpgradeManager upgradeManager, UpgradeApplier upgradeApplier) {
        this.upgradeManager = upgradeManager;
        this.upgradeApplier = upgradeApplier;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        SmithingInventory inventory = event.getInventory();
        ItemStack target = inventory.getItem(1);
        ItemStack upgradeItem = inventory.getItem(2);

        UpgradeType type = upgradeManager.getUpgradeType(upgradeItem);
        if (type == null || !upgradeApplier.isSupported(target)) {
            inventory.setResult(null);
            return;
        }

        if (upgradeApplier.validate(target, type) != UpgradeApplier.Result.SUCCESS) {
            inventory.setResult(null);
            return;
        }

        ItemStack result = target.clone();
        if (upgradeApplier.apply(result, type) != UpgradeApplier.Result.SUCCESS) {
            inventory.setResult(null);
            return;
        }

        inventory.setResult(result);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTakeResult(SmithItemEvent event) {
        SmithingInventory inventory = event.getInventory();
        ItemStack target = inventory.getItem(1);
        ItemStack upgradeItem = inventory.getItem(2);
        UpgradeType type = upgradeManager.getUpgradeType(upgradeItem);

        if (type == null || !upgradeApplier.isSupported(target)
                || upgradeApplier.validate(target, type) != UpgradeApplier.Result.SUCCESS) {
            event.setCancelled(true);
            return;
        }

        if (event.getWhoClicked() instanceof Player player) {
            player.sendMessage("§a[ForgeUpgrade] §fУлучшение §e" + type.getDisplayName()
                    + " §fуспешно применено через стол кузнеца!");
        }
    }
}
