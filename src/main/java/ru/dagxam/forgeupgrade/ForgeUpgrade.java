package ru.dagxam.forgeupgrade;

import org.bukkit.plugin.java.JavaPlugin;
import ru.dagxam.forgeupgrade.command.ForgeUpgradeCommand;
import ru.dagxam.forgeupgrade.listener.SmithingUpgradeListener;
import ru.dagxam.forgeupgrade.recipe.RecipeManager;
import ru.dagxam.forgeupgrade.upgrade.AttributeUpgradeManager;
import ru.dagxam.forgeupgrade.upgrade.UpgradeApplier;
import ru.dagxam.forgeupgrade.upgrade.UpgradeManager;

/** Главный класс ForgeUpgrade. */
public final class ForgeUpgrade extends JavaPlugin {

    private UpgradeManager upgradeManager;
    private AttributeUpgradeManager attributeUpgradeManager;
    private UpgradeApplier upgradeApplier;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("upgrades.yml", false);
        saveResource("messages.yml", false);

        upgradeManager = new UpgradeManager(this);
        attributeUpgradeManager = new AttributeUpgradeManager(this);
        upgradeApplier = new UpgradeApplier(this, attributeUpgradeManager);

        new RecipeManager(this, upgradeManager).registerRecipes();
        getServer().getPluginManager().registerEvents(
                new SmithingUpgradeListener(upgradeManager, upgradeApplier), this
        );

        ForgeUpgradeCommand command = new ForgeUpgradeCommand(this, upgradeManager);
        if (getCommand("forgeupgrade") != null) {
            getCommand("forgeupgrade").setExecutor(command);
            getCommand("forgeupgrade").setTabCompleter(command);
        }

        getLogger().info("ForgeUpgrade успешно включён.");
        getLogger().info("Загружено улучшений: " + upgradeManager.getTypes().length);
        getLogger().info("Крафты улучшений успешно зарегистрированы.");
        getLogger().info("Система улучшения через стол кузнеца успешно включена.");
        getLogger().info("Система хранения уровней и характеристик улучшений успешно включена.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ForgeUpgrade успешно выключен.");
    }

    public UpgradeManager getUpgradeManager() {
        return upgradeManager;
    }

    public AttributeUpgradeManager getAttributeUpgradeManager() {
        return attributeUpgradeManager;
    }

    public UpgradeApplier getUpgradeApplier() {
        return upgradeApplier;
    }
}
