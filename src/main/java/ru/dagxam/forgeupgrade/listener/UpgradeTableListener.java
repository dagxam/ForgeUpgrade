package ru.dagxam.forgeupgrade.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Color;
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

/** Отдельный Стол улучшений с нативным интерфейсом кузнечного стола. */
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
        if (raw >= top.getSize() && event.isShiftClick()) {
            event.setCancelled(true);
            movePlayerItemToUpgradeSlots(event, inventory);
        }
    }

    private void handleInputSlot(InventoryClickEvent event, SmithingInventory inventory, int slot) {
        if (event.getClick() == ClickType.NUMBER_KEY || event.getClick() == ClickType.SWAP_OFFHAND
                || event.getClick() == ClickType.DOUBLE_CLICK || event.getClick() == ClickType.CREATIVE) return;

        ItemStack cursor = event.getCursor();
        ItemStack current = inventory.getItem(slot);
        if (isEmpty(cursor)) {
            if (isEmpty(current)) return;
            if (event.isRightClick() && current.getAmount() > 1) {
                int take = (current.getAmount() + 1) / 2;
                ItemStack half = current.clone();
                half.setAmount(take);
                current.setAmount(current.getAmount() - take);
                inventory.setItem(slot, current.getAmount() <= 0 ? null : current);
                event.setCursor(half);
            } else {
                inventory.setItem(slot, null);
                event.setCursor(current);
            }
            scheduleUpdate(inventory);
            return;
        }

        if (!isAllowedForSlot(slot, cursor, inventory)) return;
        if (isEmpty(current)) {
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
            inventory.setItem(slot, cursor.clone());
            event.setCursor(current);
        }
        scheduleUpdate(inventory);
    }

    private void movePlayerItemToUpgradeSlots(InventoryClickEvent event, SmithingInventory inventory) {
        ItemStack clicked = event.getCurrentItem();
        if (isEmpty(clicked)) return;
        int destination = findDestination(clicked, inventory);
        if (destination < 0 || !isEmpty(inventory.getItem(destination))) return;
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
            case MATERIAL_SLOT -> {
                UpgradeType type = upgradeManager.getUpgradeType(inventory.getItem(TEMPLATE_SLOT));
                yield type != null && item.getType() == type.getSmithingMaterial();
            }
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (!isUpgradeTable(event)) return;
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
        Player player = event.getWhoClicked() instanceof Player p ? p : null;
        if (player == null) return;
        ItemStack currentCursor = event.getCursor();
        if (!event.isShiftClick() && !isEmpty(currentCursor)) return;

        if (event.isShiftClick()) {
            Map<Integer, ItemStack> left = player.getInventory().addItem(result);
            left.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
        } else {
            event.setCursor(result);
        }

        UpgradeType type = upgradeManager.getUpgradeType(inventory.getItem(TEMPLATE_SLOT));
        consumeOne(inventory, TEMPLATE_SLOT);
        consumeOne(inventory, TARGET_SLOT);
        consumeOne(inventory, MATERIAL_SLOT);
        inventory.setResult(null);
        player.updateInventory();

        if (type != null) playUpgradeEffect(player, type);
        player.sendMessage("§a[ForgeUpgrade] §fУспешно применено: §e" + (type == null ? "улучшение" : type.getDisplayName()) + "§f.");
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

    private void playUpgradeEffect(Player player, UpgradeType type) {
        Location center = player.getLocation().add(0, 1.0, 0);
        switch (type) {
            case GOLD -> spawnSimple(center, Particle.GLOW, 20);
            case EMERALD -> spawnSimple(center, Particle.HAPPY_VILLAGER, 20);
            case DIAMOND -> spawnSimple(center, Particle.END_ROD, 24);
            case NETHERITE -> spawnSimple(center, Particle.FLAME, 24);
            case ARMAGEDDON -> startRainbowEffect(player);
        }
    }

    private void spawnSimple(Location center, Particle particle, int count) {
        center.getWorld().spawnParticle(particle, center, count, 0.55, 0.75, 0.55, 0.02);
    }

    private void startRainbowEffect(Player player) {
        // 1.5 секунды визуального радужного кольца после успешного улучшения.
        final UUID id = player.getUniqueId();
        final int[] step = {0};
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || step[0] >= 15) {
                    cancel();
                    return;
                }
                Location base = player.getLocation().add(0, 1.0, 0);
                for (int i = 0; i < 18; i++) {
                    double angle = (i / 18.0) * Math.PI * 2.0 + step[0] * 0.25;
                    double y = Math.sin(angle * 2.0) * 0.15;
                    Location point = base.clone().add(Math.cos(angle) * 0.75, y, Math.sin(angle) * 0.75);
                    Color from = rainbowColor((i + step[0]) % 6);
                    Color to = rainbowColor((i + step[0] + 1) % 6);
                    Particle.DustTransition dust = new Particle.DustTransition(from, to, 1.1f);
                    player.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, point, 2, 0, 0, 0, 0, dust);
                }
                step[0]++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private Color rainbowColor(int index) {
        return switch (index) {
            case 0 -> Color.RED;
            case 1 -> Color.ORANGE;
            case 2 -> Color.YELLOW;
            case 3 -> Color.LIME;
            case 4 -> Color.AQUA;
            default -> Color.FUCHSIA;
        };
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
