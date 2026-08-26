package ru.dagxam.forgeupgrade;

import org.bukkit.plugin.java.JavaPlugin;
import ru.dagxam.forgeupgrade.command.ForgeUpgradeCommand;
import ru.dagxam.forgeupgrade.listener.UpgradeListener;
import ru.dagxam.forgeupgrade.recipe.RecipeManager;
import ru.dagxam.forgeupgrade.upgrade.UpgradeApplier;
import ru.dagxam.forgeupgrade.upgrade.UpgradeManager;

/** Главный класс ForgeUpgrade. */
public final class ForgeUpgrade extends JavaPlugin {

    private UpgradeManager upgradeManager;
    private UpgradeApplier upgradeApplier;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("upgrades.yml", false);
        saveResource("messages.yml", false);

        upgradeManager = new UpgradeManager(this);
        upgradeApplier = new UpgradeApplier(this);

        new RecipeManager(this, upgradeManager).registerRecipes();
        getServer().getPluginManager().registerEvents(
                new UpgradeListener(upgradeManager, upgradeApplier), this
        );

        ForgeUpgradeCommand command = new ForgeUpgradeCommand(this, upgradeManager);
        if (getCommand("forgeupgrade") != null) {
            getCommand("forgeupgrade").setExecutor(command);
            getCommand("forgeupgrade").setTabCompleter(command);
        }

        getLogger().info("ForgeUpgrade успешно включён.");
        getLogger().info("Загружено улучшений: " + upgradeManager.getTypes().length);
        getLogger().info("Крафты улучшений успешно зарегистрированы.");
        getLogger().info("Система применения улучшений успешно включена.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ForgeUpgrade успешно выключен.");
    }

    public UpgradeManager getUpgradeManager() {
        return upgradeManager;
    }

    public UpgradeApplier getUpgradeApplier() {
        return upgradeApplier;
    }
}
