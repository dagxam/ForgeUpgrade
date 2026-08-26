package ru.dagxam.forgeupgrade.listener;

import org.bukkit.Material;
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
 * Улучшение через стол кузнеца.
 * Слоты: 0 — шаблон, 1 — предмет, 2 — предмет улучшения.
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
        ItemStack result = createResult(inventory);
        inventory.setResult(result);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTakeResult(SmithItemEvent event) {
        SmithingInventory inventory = event.getInventory();
        ItemStack expectedResult = createResult(inventory);

        if (expectedResult == null || event.getCurrentItem() == null
                || !event.getCurrentItem().isSimilar(expectedResult)) {
            event.setCancelled(true);
            return;
        }

        UpgradeType type = upgradeManager.getUpgradeType(inventory.getItem(2));
        if (type == null) {
            event.setCancelled(true);
            return;
        }

        // Сервер сам атомарно расходует входные предметы стола кузнеца.
        // Мы не уменьшаем их вручную, чтобы исключить двойное списание.
        if (event.getWhoClicked() instanceof Player player) {
            player.sendMessage("§a[ForgeUpgrade] §fУлучшение §e" + type.getDisplayName()
                    + " §fуспешно применено через стол кузнеца!");
        }
    }

    private ItemStack createResult(SmithingInventory inventory) {
        ItemStack template = inventory.getItem(0);
        ItemStack target = inventory.getItem(1);
        ItemStack upgradeItem = inventory.getItem(2);
        UpgradeType type = upgradeManager.getUpgradeType(upgradeItem);

        if (!isValid(template, target, type)) return null;

        ItemStack result = target.clone();
        return upgradeApplier.apply(result, type) == UpgradeApplier.Result.SUCCESS ? result : null;
    }

    private boolean isValid(ItemStack template, ItemStack target, UpgradeType type) {
        if (type == null || !upgradeApplier.isSupported(target)) return false;
        if (type.requiresSmithingTemplate()) {
            return template != null && template.getType() == type.getSmithingTemplate()
                    && upgradeApplier.validate(target, type) == UpgradeApplier.Result.SUCCESS;
        }
        // Армагедон — скрытое улучшение: отдельный шаблон не требуется.
        return (template == null || template.getType() == Material.AIR)
                && upgradeApplier.validate(target, type) == UpgradeApplier.Result.SUCCESS;
    }
}
