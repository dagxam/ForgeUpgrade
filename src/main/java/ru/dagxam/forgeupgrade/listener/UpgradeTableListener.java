package ru.dagxam.forgeupgrade.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import ru.dagxam.forgeupgrade.upgrade.UpgradeApplier;
import ru.dagxam.forgeupgrade.upgrade.UpgradeManager;
import ru.dagxam.forgeupgrade.upgrade.UpgradeType;

import java.util.Map;

/** Собственный стол улучшений ForgeUpgrade без ограничений ванильного стола кузнеца. */
public final class UpgradeTableListener implements Listener {
    private static final String TITLE = "§6Стол улучшений";
    private static final int TEMPLATE_SLOT = 10;
    private static final int TARGET_SLOT = 12;
    private static final int MATERIAL_SLOT = 14;
    private static final int RESULT_SLOT = 16;

    private final UpgradeManager upgradeManager;
    private final UpgradeApplier upgradeApplier;

    public UpgradeTableListener(JavaPlugin plugin, UpgradeManager upgradeManager, UpgradeApplier upgradeApplier) {
        this.upgradeManager = upgradeManager;
        this.upgradeApplier = upgradeApplier;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.SMITHING_TABLE) return;
        event.setCancelled(true);
        open(event.getPlayer());
    }

    private void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, TITLE);
        inventory.setItem(RESULT_SLOT, null);
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!isUpgradeTable(event.getView().getTitle())) return;
        int raw = event.getRawSlot();
        Inventory top = event.getView().getTopInventory();

        if (raw == RESULT_SLOT) {
            event.setCancelled(true);
            takeResult(event, top);
            return;
        }

        if (raw >= 0 && raw < top.getSize()) {
            if (raw != TEMPLATE_SLOT && raw != TARGET_SLOT && raw != MATERIAL_SLOT) {
                event.setCancelled(true);
                return;
            }
            if (event.isShiftClick()) {
                event.setCancelled(true);
                return;
            }
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("ForgeUpgrade"), () -> updateResult(top));
            return;
        }

        if (event.isShiftClick()) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!isUpgradeTable(event.getView().getTitle())) return;
        for (int raw : event.getRawSlots()) {
            if (raw < event.getView().getTopInventory().getSize()
                    && raw != TEMPLATE_SLOT && raw != TARGET_SLOT && raw != MATERIAL_SLOT) {
                event.setCancelled(true);
                return;
            }
        }
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("ForgeUpgrade"),
                () -> updateResult(event.getView().getTopInventory()));
    }

    private void updateResult(Inventory inventory) {
        ItemStack template = inventory.getItem(TEMPLATE_SLOT);
        ItemStack target = inventory.getItem(TARGET_SLOT);
        ItemStack material = inventory.getItem(MATERIAL_SLOT);
        UpgradeType type = upgradeManager.getUpgradeType(template);

        if (type == null || target == null || target.getType().isAir()
                || material == null || material.getType() != type.getSmithingMaterial()
                || !upgradeApplier.isSupported(target)
                || upgradeApplier.validate(target, type) != UpgradeApplier.Result.SUCCESS) {
            inventory.setItem(RESULT_SLOT, null);
            return;
        }

        ItemStack result = target.clone();
        if (upgradeApplier.apply(result, type) == UpgradeApplier.Result.SUCCESS) inventory.setItem(RESULT_SLOT, result);
        else inventory.setItem(RESULT_SLOT, null);
    }

    private void takeResult(InventoryClickEvent event, Inventory inventory) {
        ItemStack result = inventory.getItem(RESULT_SLOT);
        if (result == null || result.getType().isAir()) return;
        if (event.getCursor() != null && !event.getCursor().getType().isAir()) return;

        ItemStack template = inventory.getItem(TEMPLATE_SLOT);
        ItemStack target = inventory.getItem(TARGET_SLOT);
        ItemStack material = inventory.getItem(MATERIAL_SLOT);
        UpgradeType type = upgradeManager.getUpgradeType(template);
        if (type == null || target == null || material == null || material.getType() != type.getSmithingMaterial()) return;
        if (upgradeApplier.validate(target, type) != UpgradeApplier.Result.SUCCESS) return;

        consumeOne(inventory, TEMPLATE_SLOT);
        consumeOne(inventory, TARGET_SLOT);
        consumeOne(inventory, MATERIAL_SLOT);
        event.setCursor(result);
        inventory.setItem(RESULT_SLOT, null);
        updateResult(inventory);

        if (event.getWhoClicked() instanceof Player player) {
            player.sendMessage("§a[ForgeUpgrade] §fУспешно применено: §e" + type.getDisplayName() + "§f.");
        }
    }

    private void consumeOne(Inventory inventory, int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item == null) return;
        if (item.getAmount() <= 1) inventory.setItem(slot, null);
        else item.setAmount(item.getAmount() - 1);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!isUpgradeTable(event.getView().getTitle())) return;
        Inventory inventory = event.getInventory();
        Player player = (Player) event.getPlayer();
        for (int slot : new int[]{TEMPLATE_SLOT, TARGET_SLOT, MATERIAL_SLOT}) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) continue;
            Map<Integer, ItemStack> left = player.getInventory().addItem(item);
            left.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
        }
        inventory.clear();
    }

    private boolean isUpgradeTable(String title) {
        return TITLE.equals(title);
    }
}
