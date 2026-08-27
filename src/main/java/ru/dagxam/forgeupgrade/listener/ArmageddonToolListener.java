package ru.dagxam.forgeupgrade.listener;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import ru.dagxam.forgeupgrade.upgrade.UpgradeApplier;
import ru.dagxam.forgeupgrade.upgrade.UpgradeType;

import java.util.Map;

/** Дополнительные способности Армагедона только для незеритовых инструментов. */
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
            if (!block.getType().isAir()) forceBreak(player, tool, block);
        }
    }

    /** Кирка Армагедона ломает любой блок с одного удара, включая BEDROCK. */
    private void forceBreak(Player player, ItemStack tool, Block block) {
        Material originalType = block.getType();

        BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
        plugin.getServer().getPluginManager().callEvent(breakEvent);
        if (breakEvent.isCancelled()) return;

        if (originalType == Material.BEDROCK) {
            // BEDROCK обычно не имеет естественного дропа, поэтому после успешного
            // разрушения создаём настоящий блок BEDROCK и помещаем его в инвентарь.
            block.setType(Material.AIR, false);
            giveBedrockToPlayer(player, block);
            return;
        }

        block.breakNaturally(tool);
    }

    /**
     * Выдаёт сломанный BEDROCK напрямую в инвентарь.
     * Если инвентарь заполнен, остаток выбрасывается рядом с игроком,
     * чтобы блок никогда не исчезал без дропа.
     */
    private void giveBedrockToPlayer(Player player, Block brokenBlock) {
        ItemStack bedrock = new ItemStack(Material.BEDROCK, 1);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(bedrock);

        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(
                    brokenBlock.getLocation().add(0.5D, 0.5D, 0.5D),
                    leftover
            );
        }

        brokenBlock.getWorld().spawnParticle(
                Particle.CRIT,
                brokenBlock.getLocation().add(0.5D, 0.5D, 0.5D),
                24, 0.35D, 0.35D, 0.35D, 0.08D
        );
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

    /** Быстрый рост: после обработки ускоряются растения в зоне 3x3 вокруг каждой новой грядки. */
    private void triggerFastGrowth(Block farmland) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block plant = farmland.getRelative(x, 1, z);
                for (int attempt = 0; attempt < 3; attempt++) {
                    if (!plant.applyBoneMeal(BlockFace.UP)) break;
                }
            }
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
