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
 * ForgeUpgrade использует все три слота стола кузнеца:
 * 0 — собственный кузнечный шаблон, 1 — оружие/броня/инструмент, 2 — обязательный материал.
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
        event.getInventory().setResult(createResult(event.getInventory()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTakeResult(SmithItemEvent event) {
        SmithingInventory inventory = event.getInventory();
        ItemStack expected = createResult(inventory);
        if (expected == null || event.getCurrentItem() == null || !event.getCurrentItem().isSimilar(expected)) {
            event.setCancelled(true);
            return;
        }

        UpgradeType type = upgradeManager.getUpgradeType(inventory.getItem(0));
        if (type == null || inventory.getItem(2) == null || inventory.getItem(2).getType() != type.getSmithingMaterial()) {
            event.setCancelled(true);
            return;
        }

        // Входные предметы расходует ванильный механизм стола кузнеца.
        if (event.getWhoClicked() instanceof Player player) {
            player.sendMessage("§a[ForgeUpgrade] §fУспешно применено улучшение §e" + type.getDisplayName() + "§f!");
        }
    }

    private ItemStack createResult(SmithingInventory inventory) {
        ItemStack template = inventory.getItem(0);
        ItemStack target = inventory.getItem(1);
        ItemStack material = inventory.getItem(2);
        UpgradeType type = upgradeManager.getUpgradeType(template);

        if (!isValid(template, target, material, type)) return null;

        ItemStack result = target.clone();
        return upgradeApplier.apply(result, type) == UpgradeApplier.Result.SUCCESS ? result : null;
    }

    private boolean isValid(ItemStack template, ItemStack target, ItemStack material, UpgradeType type) {
        if (type == null || template == null || template.getType() != type.getSmithingTemplate()) return false;
        if (material == null || material.getType() != type.getSmithingMaterial()) return false;
        if (!upgradeApplier.isSupported(target)) return false;
        return upgradeApplier.validate(target, type) == UpgradeApplier.Result.SUCCESS;
    }
}
