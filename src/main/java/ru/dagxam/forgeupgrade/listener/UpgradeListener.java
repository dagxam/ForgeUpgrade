package ru.dagxam.forgeupgrade.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import ru.dagxam.forgeupgrade.upgrade.UpgradeApplier;
import ru.dagxam.forgeupgrade.upgrade.UpgradeManager;
import ru.dagxam.forgeupgrade.upgrade.UpgradeType;

/**
 * Применение улучшения: игрок держит предмет улучшения в основной руке
 * и нажимает ПКМ, держа целевой предмет во второй руке.
 */
public final class UpgradeListener implements Listener {
    private final UpgradeManager upgradeManager;
    private final UpgradeApplier upgradeApplier;

    public UpgradeListener(UpgradeManager upgradeManager, UpgradeApplier upgradeApplier) {
        this.upgradeManager = upgradeManager;
        this.upgradeApplier = upgradeApplier;
    }

    @EventHandler
    public void onUseUpgrade(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack upgradeItem = player.getInventory().getItemInMainHand();
        UpgradeType type = upgradeManager.getUpgradeType(upgradeItem);
        if (type == null) return;

        event.setCancelled(true);
        ItemStack target = player.getInventory().getItemInOffHand();
        if (target == null || target.getType().isAir()) {
            player.sendMessage("§c[ForgeUpgrade] §fПоместите предмет для улучшения во вторую руку.");
            return;
        }

        UpgradeApplier.Result result = upgradeApplier.apply(target, type);
        switch (result) {
            case SUCCESS -> {
                consumeOne(upgradeItem);
                player.sendMessage("§a[ForgeUpgrade] §fУлучшение §e" + type.getDisplayName() + " §fуспешно применено!");
            }
            case UNSUPPORTED -> player.sendMessage("§c[ForgeUpgrade] §fЭтот предмет нельзя улучшить.");
            case ALREADY_APPLIED -> player.sendMessage("§e[ForgeUpgrade] §fЭто улучшение уже установлено на предмете.");
            case CANNOT_DOWNGRADE -> player.sendMessage("§c[ForgeUpgrade] §fНельзя заменить более сильное улучшение более слабым.");
        }
    }

    private void consumeOne(ItemStack item) {
        if (item.getAmount() <= 1) item.setAmount(0);
        else item.setAmount(item.getAmount() - 1);
    }
}
