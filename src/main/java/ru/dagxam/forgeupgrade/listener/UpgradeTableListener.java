package ru.dagxam.forgeupgrade.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import ru.dagxam.forgeupgrade.upgrade.UpgradeApplier;
import ru.dagxam.forgeupgrade.upgrade.UpgradeManager;
import ru.dagxam.forgeupgrade.upgrade.UpgradeType;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Отдельный крафтовый Стол улучшений. Обычные кузнечные столы не изменяются. */
public final class UpgradeTableListener implements Listener {
    private static final String TITLE = "§6Стол улучшений";
    private static final int TEMPLATE_SLOT = 0;
    private static final int TARGET_SLOT = 1;
    private static final int MATERIAL_SLOT = 2;
    private static final int RESULT_SLOT = 3;

    private final JavaPlugin plugin;
    private final UpgradeManager upgradeManager;
    private final UpgradeApplier upgradeApplier;
    private final NamespacedKey tableItemKey;
    private final File dataFile;
    private final Set<String> upgradeTables = new HashSet<>();

    public UpgradeTableListener(JavaPlugin plugin, UpgradeManager upgradeManager, UpgradeApplier upgradeApplier) {
        this.plugin = plugin;
        this.upgradeManager = upgradeManager;
        this.upgradeApplier = upgradeApplier;
        this.tableItemKey = new NamespacedKey(plugin, "upgrade_table_item");
        this.dataFile = new File(plugin.getDataFolder(), "upgrade-tables.yml");
        loadTables();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.SMITHING_TABLE || !isUpgradeTableItem(event.getItemInHand())) return;
        upgradeTables.add(locationKey(event.getBlockPlaced().getLocation()));
        saveTables();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        String key = locationKey(event.getBlock().getLocation());
        if (!upgradeTables.remove(key)) return;
        event.setDropItems(false);
        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), createUpgradeTableItem());
        saveTables();
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.SMITHING_TABLE) return;
        if (!upgradeTables.contains(locationKey(event.getClickedBlock().getLocation()))) return;
        event.setCancelled(true);
        open(event.getPlayer());
    }

    private void open(Player player) {
        player.openInventory(Bukkit.createInventory(null, InventoryType.SMITHING, TITLE));
    }

    /**
     * Главное исправление: нативный кузнечный инвентарь сам пересчитывает и очищает результат.
     * Поэтому результат создаётся именно через PrepareSmithingEvent и SmithingInventory#setResult().
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) return;
        updateResult(event.getInventory());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!isUpgradeTable(event)) return;
        int raw = event.getRawSlot();
        Inventory top = event.getView().getTopInventory();

        if (raw == RESULT_SLOT) {
            event.setCancelled(true);
            takeResult(event, (SmithingInventory) top);
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
            scheduleUpdate((SmithingInventory) top);
            return;
        }

        if (event.isShiftClick()) event.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!isUpgradeTable(event)) return;
        for (int raw : event.getRawSlots()) {
            if (raw < event.getView().getTopInventory().getSize()
                    && raw != TEMPLATE_SLOT && raw != TARGET_SLOT && raw != MATERIAL_SLOT) {
                event.setCancelled(true);
                return;
            }
        }
        scheduleUpdate((SmithingInventory) event.getView().getTopInventory());
    }

    private void scheduleUpdate(SmithingInventory inventory) {
        Bukkit.getScheduler().runTask(plugin, () -> updateResult(inventory));
    }

    private void updateResult(SmithingInventory inventory) {
        ItemStack template = inventory.getItem(TEMPLATE_SLOT);
        ItemStack target = inventory.getItem(TARGET_SLOT);
        ItemStack material = inventory.getItem(MATERIAL_SLOT);
        UpgradeType type = upgradeManager.getUpgradeType(template);

        if (type == null || target == null || target.getType().isAir()
                || material == null || material.getType() != type.getSmithingMaterial()
                || !upgradeApplier.isSupported(target)
                || upgradeApplier.validate(target, type) != UpgradeApplier.Result.SUCCESS) {
            inventory.setResult(null);
            return;
        }

        ItemStack result = target.clone();
        if (upgradeApplier.apply(result, type) == UpgradeApplier.Result.SUCCESS) {
            inventory.setResult(result);
        } else {
            inventory.setResult(null);
        }
    }

    private void takeResult(InventoryClickEvent event, SmithingInventory inventory) {
        ItemStack result = inventory.getResult();
        if (result == null || result.getType().isAir()) return;
        if (event.getCursor() != null && !event.getCursor().getType().isAir()) return;

        ItemStack template = inventory.getItem(TEMPLATE_SLOT);
        ItemStack target = inventory.getItem(TARGET_SLOT);
        ItemStack material = inventory.getItem(MATERIAL_SLOT);
        UpgradeType type = upgradeManager.getUpgradeType(template);
        if (type == null || target == null || material == null || material.getType() != type.getSmithingMaterial()) return;
        if (!upgradeApplier.isSupported(target) || upgradeApplier.validate(target, type) != UpgradeApplier.Result.SUCCESS) return;

        consumeOne(inventory, TEMPLATE_SLOT);
        consumeOne(inventory, TARGET_SLOT);
        consumeOne(inventory, MATERIAL_SLOT);
        inventory.setResult(null);
        event.setCursor(result);
        scheduleUpdate(inventory);

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
        if (!isUpgradeTable(event)) return;
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

    private boolean isUpgradeTable(InventoryClickEvent event) {
        return event.getView().getTopInventory().getType() == InventoryType.SMITHING && TITLE.equals(event.getView().getTitle());
    }

    private boolean isUpgradeTable(InventoryDragEvent event) {
        return event.getView().getTopInventory().getType() == InventoryType.SMITHING && TITLE.equals(event.getView().getTitle());
    }

    private boolean isUpgradeTable(InventoryCloseEvent event) {
        return event.getInventory().getType() == InventoryType.SMITHING && TITLE.equals(event.getView().getTitle());
    }

    private boolean isUpgradeTableItem(ItemStack item) {
        if (item == null || item.getType() != Material.SMITHING_TABLE || !item.hasItemMeta()) return false;
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(tableItemKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    private ItemStack createUpgradeTableItem() {
        ItemStack item = new ItemStack(Material.SMITHING_TABLE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Стол улучшений");
            meta.getPersistentDataContainer().set(tableItemKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String locationKey(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private void loadTables() {
        if (!dataFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        upgradeTables.addAll(config.getStringList("tables"));
    }

    private void saveTables() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("tables", upgradeTables.stream().sorted().toList());
        try {
            config.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Не удалось сохранить расположение Столов улучшений: " + exception.getMessage());
        }
    }
}
