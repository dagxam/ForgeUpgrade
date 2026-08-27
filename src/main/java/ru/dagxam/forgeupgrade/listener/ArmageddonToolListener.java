package ru.dagxam.forgeupgrade.listener;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import ru.dagxam.forgeupgrade.upgrade.UpgradeApplier;
import ru.dagxam.forgeupgrade.upgrade.UpgradeType;

/**
 * Дополнительные способности Армагедона.
 * Они работают только у НEЗЕРИТОВЫХ инструментов с улучшением ARMAGEDDON.
 */
public final class ArmageddonToolListener implements Listener {
    private final JavaPlugin plugin;
    private final UpgradeApplier upgradeApplier;

    public ArmageddonToolListener(JavaPlugin plugin, UpgradeApplier upgradeApplier) {
        this.plugin = plugin;
        this.upgradeApplier = upgradeApplier;
    }

    private boolean isArmageddonNetherite(ItemStack item, Material required) {
        return item != null && item.getType() == required
                && upgradeApplier.getAppliedType(item) == UpgradeType.ARMAGEDDON;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = event.getItemInHand();
        Material type = tool.getType();

        if (isArmageddonNetherite(tool, Material.NETHERITE_PICKAXE)) {
            event.setCancelled(true);
            breakBlocks(player, tool, event.getBlock(), 2);
            return;
        }

        if (isArmageddonNetherite(tool, Material.NETHERITE_AXE)) {
            event.setCancelled(true);
            breakBlocks(player, tool, event.getBlock(), 1);
            return;
        }

        if (isArmageddonNetherite(tool, Material.NETHERITE_SHOVEL)) {
            event.setCancelled(true);
            breakBlocks(player, tool, event.getBlock(), 3);
        }
    }

    /** Ломает указанное количество блоков по направлению взгляда за одно нажатие. */
    private void breakBlocks(Player player, ItemStack tool, Block first, int count) {
        BlockFace direction = getHorizontalFacing(player);
        for (int i = 0; i < count; i++) {
            Block block = first.getRelative(direction, i);
            if (block.getType().isAir()) continue;
            forceBreak(player, tool, block);
        }
    }

    /**
     * Кирка Армагедона ломает любой блок с одного удара, включая BEDROCK.
     * Сначала вызывается обычный BlockBreakEvent, поэтому отменённые защитными плагинами блоки не ломаются.
     */
    private void forceBreak(Player player, ItemStack tool, Block block) {
        BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
        plugin.getServer().getPluginManager().callEvent(breakEvent);
        if (breakEvent.isCancelled()) return;

        if (block.getType() == Material.BEDROCK) {
            block.setType(Material.AIR);
            return;
        }

        block.breakNaturally(tool);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHoeUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack hoe = event.getItem();
        if (!isArmageddonNetherite(hoe, Material.NETHERITE_HOE)) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        event.setCancelled(true);
        BlockFace side = getPerpendicular(getHorizontalFacing(event.getPlayer()));
        boolean changed = false;

        // Сразу три грядки: слева, по центру и справа.
        for (int offset = -1; offset <= 1; offset++) {
            Block soil = clicked.getRelative(side, offset);
            if (!canTill(soil)) continue;
            if (!soil.getRelative(BlockFace.UP).getType().isAir()) continue;

            soil.setType(Material.FARMLAND);
            changed = true;
            triggerFastGrowth(soil);
        }

        if (changed) {
            clicked.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                    clicked.getLocation().add(0.5D, 1.0D, 0.5D), 20, 1.5D, 0.5D, 1.5D, 0.02D);
        }
    }

    private boolean canTill(Block block) {
        Material type = block.getType();
        return type == Material.GRASS_BLOCK || type == Material.DIRT
                || type == Material.COARSE_DIRT || type == Material.ROOTED_DIRT
                || type == Material.DIRT_PATH;
    }

    /** Быстрый рост: несколько мгновенных попыток удобрить растения над обработанными грядками. */
    private void triggerFastGrowth(Block farmland) {
        Block plant = farmland.getRelative(BlockFace.UP);
        for (int i = 0; i < 3; i++) {
            if (!plant.applyBoneMeal(BlockFace.UP)) break;
        }
        farmland.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                farmland.getLocation().add(0.5D, 1.0D, 0.5D), 8, 0.35D, 0.25D, 0.35D, 0.01D);
    }

    private BlockFace getHorizontalFacing(Player player) {
        float yaw = player.getLocation().getYaw();
        int direction = Math.round(yaw / 90.0F) & 3;
        return switch (direction) {
            case 0 -> BlockFace.SOUTH;
            case 1 -> BlockFace.WEST;
            case 2 -> BlockFace.NORTH;
            default -> BlockFace.EAST;
        };
    }

    private BlockFace getPerpendicular(BlockFace facing) {
        return switch (facing) {
            case NORTH, SOUTH -> BlockFace.EAST;
            default -> BlockFace.NORTH;
        };
    }
}
