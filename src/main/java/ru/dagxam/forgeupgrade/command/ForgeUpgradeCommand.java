package ru.dagxam.forgeupgrade.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.dagxam.forgeupgrade.ForgeUpgrade;
import ru.dagxam.forgeupgrade.upgrade.UpgradeManager;
import ru.dagxam.forgeupgrade.upgrade.UpgradeType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Административная команда выдачи улучшений. */
public final class ForgeUpgradeCommand implements CommandExecutor, TabCompleter {
    private final ForgeUpgrade plugin;
    private final UpgradeManager upgradeManager;

    public ForgeUpgradeCommand(ForgeUpgrade plugin, UpgradeManager upgradeManager) {
        this.plugin = plugin;
        this.upgradeManager = upgradeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6§lForgeUpgrade §7— система улучшения предметов.");
            sender.sendMessage("§e/forgeupgrade give <игрок> <gold|emerald|diamond|netherite|armageddon>");
            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("forgeupgrade.admin")) {
                sender.sendMessage("§cУ вас нет прав на эту команду.");
                return true;
            }
            Player target = plugin.getServer().getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§cИгрок не найден или не находится на сервере.");
                return true;
            }
            UpgradeType type = UpgradeType.fromId(args[2].toLowerCase(Locale.ROOT));
            if (type == null) {
                sender.sendMessage("§cНеизвестное улучшение.");
                return true;
            }
            ItemStack item = upgradeManager.createUpgrade(type);
            target.getInventory().addItem(item);
            sender.sendMessage("§aИгроку §e" + target.getName() + " §aвыдано: §6" + type.getDisplayName());
            target.sendMessage("§aВы получили: §6" + type.getDisplayName());
            return true;
        }

        sender.sendMessage("§cНеверное использование команды.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(args[0], Collections.singletonList("give"));
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> names = new ArrayList<>();
            for (Player player : plugin.getServer().getOnlinePlayers()) names.add(player.getName());
            return filter(args[1], names);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return filter(args[2], Arrays.stream(UpgradeType.values()).map(UpgradeType::getId).toList());
        }
        return Collections.emptyList();
    }

    private List<String> filter(String input, List<String> values) {
        String lower = input.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
