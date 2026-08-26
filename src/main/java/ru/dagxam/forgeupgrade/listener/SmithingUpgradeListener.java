package ru.dagxam.forgeupgrade.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import ru.dagxam.forgeupgrade.upgrade.UpgradeApplier;
import ru.dagxam.forgeupgrade.upgrade.UpgradeManager;
import ru.dagxam.forgeupgrade.upgrade.UpgradeType;

/**
 * Обрабатывает улучшение предметов через интерфейс стола кузнеца.
 * Слоты SmithingInventory: 0 — шаблон, 1 — предмет, 2 — улучшение.
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

        if (!isValid(target, type)) {
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

        if (!isValid(target, type)) {
            event.setCancelled(true);
            return;
        }

        // Shift-клик и обычное взятие проходят через серверное потребление входных слотов.
        // Повторная проверка выше защищает от изменения предметов между preview и click.
        if (event.getWhoClicked() instanceof Player player) {
            player.sendMessage("§a[ForgeUpgrade] §fУлучшение §e" + type.getDisplayName()
                    + " §fуспешно применено через стол кузнеца!");
        }
    }

    private boolean isValid(ItemStack target, UpgradeType type) {
        return type != null
                && upgradeApplier.isSupported(target)
                && upgradeApplier.validate(target, type) == UpgradeApplier.Result.SUCCESS;
    }
}
