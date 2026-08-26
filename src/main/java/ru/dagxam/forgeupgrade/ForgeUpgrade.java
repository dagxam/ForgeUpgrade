package ru.dagxam.forgeupgrade;

import org.bukkit.plugin.java.JavaPlugin;
import ru.dagxam.forgeupgrade.command.ForgeUpgradeCommand;
import ru.dagxam.forgeupgrade.recipe.RecipeManager;
import ru.dagxam.forgeupgrade.upgrade.UpgradeManager;

/** Главный класс ForgeUpgrade. */
public final class ForgeUpgrade extends JavaPlugin {

    private UpgradeManager upgradeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("upgrades.yml", false);
        saveResource("messages.yml", false);

        upgradeManager = new UpgradeManager(this);
        new RecipeManager(this, upgradeManager).registerRecipes();

        ForgeUpgradeCommand command = new ForgeUpgradeCommand(this, upgradeManager);
        if (getCommand("forgeupgrade") != null) {
            getCommand("forgeupgrade").setExecutor(command);
            getCommand("forgeupgrade").setTabCompleter(command);
        }

        getLogger().info("ForgeUpgrade успешно включён.");
        getLogger().info("Загружено улучшений: " + upgradeManager.getTypes().size());
        getLogger().info("Крафты улучшений успешно зарегистрированы.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ForgeUpgrade успешно выключен.");
    }

    public UpgradeManager getUpgradeManager() {
        return upgradeManager;
    }
}
