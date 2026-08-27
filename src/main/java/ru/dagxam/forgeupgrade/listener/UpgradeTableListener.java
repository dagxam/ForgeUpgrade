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
import org.bukkit.event.inventory.ClickType;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Отдельный крафтовый Стол улучшений.
 * Обычные кузнечные столы не изменяются. Для крафтового стола используется
 * настоящий интерфейс кузнечного стола, но его три входных слота полностью
 * обрабатываются плагином, поэтому ванильные ограничения не блокируют оружие,
 * инструменты и Звезду Незера.
 */
public final class UpgradeTableListener implements Listener {
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
    private final Map<UUID, String> activeUpgradeTables = new HashMap<>();

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
        String key = locationKey(event.getClickedBlock().getLocation());
        if (upgradeTables.contains(key)) activeUpgradeTables.put(event.getPlayer().getUniqueId(), key);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        if (!isActiveUpgradeTable(player, event.getInventory())) return;
        event.setResult(createResult(event.getInventory()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!isUpgradeTable(event)) return;
        Inventory top = event.getView().getTopInventory();
        if (!(top instanceof SmithingInventory inventory)) return;

        int raw = event.getRawSlot();
        if (raw == RESULT_SLOT) {
            event.setCancelled(true);
            takeResult(event, inventory);
            return;
        }

        if (raw >= TEMPLATE_SLOT && raw <= MATERIAL_SLOT) {
            event.setCancelled(true);
            handleInputSlot(event, inventory, raw);
            return;
        }

        // Shift-клик из инвентаря игрока: сами выбираем правильный слот.
        if (raw >= top.getSize() && event.isShiftClick()) {
            event.setCancelled(true);
            movePlayerItemToUpgradeSlots(event, inventory);
            return;
        }

        // Hotbar/number-key не должен обходить ручную проверку трёх входных слотов.
        if (raw >= TEMPLATE_SLOT && raw < top.getSize()) event.setCancelled(true);
    }

    private void handleInputSlot(InventoryClickEvent event, SmithingInventory inventory, int slot) {
        if (event.getClick() == ClickType.NUMBER_KEY || event.getClick() == ClickType.SWAP_OFFHAND) return;
        if (event.getClick() == ClickType.DOUBLE_CLICK || event.getClick() == ClickType.CREATIVE) return;

        ItemStack cursor = event.getCursor();
        ItemStack current = inventory.getItem(slot);
        boolean cursorEmpty = isEmpty(cursor);
        boolean currentEmpty = isEmpty(current);

        // Забрать предмет из слота.
        if (cursorEmpty) {
            if (currentEmpty) return;
            if (event.isRightClick() && current.getAmount() > 1) {
                ItemStack one = current.clone();
                one.setAmount((current.getAmount() + 1) / 2);
                current.setAmount(current.getAmount() - one.getAmount());
                inventory.setItem(slot, current);
                event.setCursor(one);
            } else {
                inventory.setItem(slot, null);
                event.setCursor(current);
            }
            scheduleUpdate(inventory);
            return;
        }

        // Положить только допустимый предмет.
        if (!isAllowedForSlot(slot, cursor, inventory)) return;

        if (currentEmpty) {
            if (event.isRightClick()) {
                ItemStack one = cursor.clone();
                one.setAmount(1);
                inventory.setItem(slot, one);
                decreaseCursor(event, 1);
            } else {
                inventory.setItem(slot, cursor.clone());
                event.setCursor(null);
            }
        } else if (current.isSimilar(cursor) && current.getAmount() < current.getMaxStackSize()) {
            int amount = event.isRightClick() ? 1 : Math.min(cursor.getAmount(), current.getMaxStackSize() - current.getAmount());
            current.setAmount(current.getAmount() + amount);
            inventory.setItem(slot, current);
            decreaseCursor(event, amount);
        } else if (!event.isRightClick()) {
            // ЛКМ меняет предметы местами, если новый предмет разрешён в слоте.
            inventory.setItem(slot, cursor.clone());
            event.setCursor(current);
        }

        scheduleUpdate(inventory);
    }

    private void movePlayerItemToUpgradeSlots(InventoryClickEvent event, SmithingInventory inventory) {
        ItemStack clicked = event.getCurrentItem();
        if (isEmpty(clicked)) return;

        int destination = findDestination(clicked, inventory);
        if (destination < 0) return;

        ItemStack existing = inventory.getItem(destination);
        if (!isEmpty(existing)) return;

        inventory.setItem(destination, clicked.clone());
        event.setCurrentItem(null);
        scheduleUpdate(inventory);
    }

    private int findDestination(ItemStack item, SmithingInventory inventory) {
        if (isEmpty(inventory.getItem(TEMPLATE_SLOT)) && upgradeManager.getUpgradeType(item) != null) return TEMPLATE_SLOT;
        if (isEmpty(inventory.getItem(TARGET_SLOT)) && upgradeApplier.isSupported(item)) return TARGET_SLOT;
        if (isEmpty(inventory.getItem(MATERIAL_SLOT)) && isUpgradeMaterial(item.getType())) return MATERIAL_SLOT;
        return -1;
    }

    private boolean isAllowedForSlot(int slot, ItemStack item, SmithingInventory inventory) {
        if (isEmpty(item)) return true;
        return switch (slot) {
            case TEMPLATE_SLOT -> upgradeManager.getUpgradeType(item) != null;
            case TARGET_SLOT -> upgradeApplier.isSupported(item);
            case MATERIAL_SLOT -> isUpgradeMaterial(item.getType());
            default -> false;
        };
    }

    private boolean isUpgradeMaterial(Material material) {
        for (UpgradeType type : UpgradeType.values()) {
            if (type.getSmithingMaterial() == material) return true;
        }
        return false;
    }

    private void decreaseCursor(InventoryClickEvent event, int amount) {
        ItemStack cursor = event.getCursor();
        if (isEmpty(cursor)) return;
        int left = cursor.getAmount() - amount;
        if (left <= 0) event.setCursor(null);
        else {
            cursor.setAmount(left);
            event.setCursor(cursor);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!isUpgradeTable(event)) return;
        // Ванильный drag снова применяет собственные ограничения кузнечного стола.
        // Чтобы нельзя было обойти ручную проверку, блокируем drag только в верхние слоты.
        for (int raw : event.getRawSlots()) {
            if (raw < event.getView().getTopInventory().getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void scheduleUpdate(SmithingInventory inventory) {
        Bukkit.getScheduler().runTask(plugin, () -> inventory.setResult(createResult(inventory)));
    }

    private ItemStack createResult(SmithingInventory inventory) {
        ItemStack template = inventory.getItem(TEMPLATE_SLOT);
        ItemStack target = inventory.getItem(TARGET_SLOT);
        ItemStack material = inventory.getItem(MATERIAL_SLOT);
        UpgradeType type = upgradeManager.getUpgradeType(template);
        if (type == null || isEmpty(target) || isEmpty(material)) return null;
        if (material.getType() != type.getSmithingMaterial()) return null;
        if (!upgradeApplier.isSupported(target)) return null;
        if (upgradeApplier.validate(target, type) != UpgradeApplier.Result.SUCCESS) return null;

        ItemStack result = target.clone();
        return upgradeApplier.apply(result, type) == UpgradeApplier.Result.SUCCESS ? result : null;
    }

    private void takeResult(InventoryClickEvent event, SmithingInventory inventory) {
        ItemStack result = createResult(inventory);
        if (isEmpty(result)) return;
        ItemStack template = inventory.getItem(TEMPLATE_SLOT);
        ItemStack target = inventory.getItem(TARGET_SLOT);
        ItemStack material = inventory.getItem(MATERIAL_SLOT);
        UpgradeType type = upgradeManager.getUpgradeType(template);
        if (type == null || isEmpty(target) || isEmpty(material) || material.getType() != type.getSmithingMaterial()) return;

        Player player = event.getWhoClicked() instanceof Player p ? p : null;
        if (player == null) return;

        if (event.isShiftClick()) {
            Map<Integer, ItemStack> left = player.getInventory().addItem(result);
            left.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
        } else {
            if (!isEmpty(event.getCursor())) return;
            event.setCursor(result);
        }

        consumeOne(inventory, TEMPLATE_SLOT);
        consumeOne(inventory, TARGET_SLOT);
        consumeOne(inventory, MATERIAL_SLOT);
        inventory.setResult(null);
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.updateInventory();
            inventory.setResult(createResult(inventory));
        });
        player.sendMessage("§a[ForgeUpgrade] §fУспешно применено: §e" + type.getDisplayName() + "§f.");
    }

    private void consumeOne(Inventory inventory, int slot) {
        ItemStack item = inventory.getItem(slot);
        if (isEmpty(item)) return;
        if (item.getAmount() <= 1) inventory.setItem(slot, null);
        else {
            item.setAmount(item.getAmount() - 1);
            inventory.setItem(slot, item);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player) || !isUpgradeTable(event)) return;
        activeUpgradeTables.remove(player.getUniqueId());
    }

    private boolean isUpgradeTable(InventoryClickEvent event) {
        return event.getView().getTopInventory().getType() == InventoryType.SMITHING
                && activeUpgradeTables.containsKey(event.getWhoClicked().getUniqueId());
    }

    private boolean isUpgradeTable(InventoryDragEvent event) {
        return event.getView().getTopInventory().getType() == InventoryType.SMITHING
                && event.getWhoClicked() instanceof Player player
                && activeUpgradeTables.containsKey(player.getUniqueId());
    }

    private boolean isUpgradeTable(InventoryCloseEvent event) {
        return event.getInventory().getType() == InventoryType.SMITHING
                && event.getPlayer() instanceof Player player
                && activeUpgradeTables.containsKey(player.getUniqueId());
    }

    private boolean isActiveUpgradeTable(Player player, SmithingInventory inventory) {
        String expected = activeUpgradeTables.get(player.getUniqueId());
        if (expected == null) return false;
        Location location = inventory.getLocation();
        return location != null && expected.equals(locationKey(location));
    }

    private boolean isUpgradeTableItem(ItemStack item) {
        if (isEmpty(item) || item.getType() != Material.SMITHING_TABLE || !item.hasItemMeta()) return false;
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

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
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
