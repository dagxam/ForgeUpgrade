package ru.dagxam.forgeupgrade;

import org.bukkit.plugin.java.JavaPlugin;
import ru.dagxam.forgeupgrade.command.ForgeUpgradeCommand;
import ru.dagxam.forgeupgrade.listener.ArmageddonArmorListener;
import ru.dagxam.forgeupgrade.listener.ArmageddonWeaponListener;
import ru.dagxam.forgeupgrade.listener.SmithingUpgradeListener;
import ru.dagxam.forgeupgrade.listener.UpgradeAttributeListener;
import ru.dagxam.forgeupgrade.recipe.RecipeManager;
import ru.dagxam.forgeupgrade.upgrade.ArmorTrimUpgradeManager;
import ru.dagxam.forgeupgrade.upgrade.AttributeUpgradeManager;
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
        AttributeUpgradeManager attributeUpgradeManager = new AttributeUpgradeManager(this);
        ArmorTrimUpgradeManager armorTrimUpgradeManager = new ArmorTrimUpgradeManager(this);
        upgradeApplier = new UpgradeApplier(this, attributeUpgradeManager, armorTrimUpgradeManager);

        new RecipeManager(this, upgradeManager).registerRecipes();
        getServer().getPluginManager().registerEvents(new SmithingUpgradeListener(upgradeManager, upgradeApplier), this);
        getServer().getPluginManager().registerEvents(new UpgradeAttributeListener(), this);
        getServer().getPluginManager().registerEvents(new ArmageddonArmorListener(this, upgradeApplier), this);
        getServer().getPluginManager().registerEvents(new ArmageddonWeaponListener(this, upgradeApplier), this);

        ForgeUpgradeCommand command = new ForgeUpgradeCommand(this, upgradeManager);
        if (getCommand("forgeupgrade") != null) {
            getCommand("forgeupgrade").setExecutor(command);
            getCommand("forgeupgrade").setTabCompleter(command);
        }

        getLogger().info("ForgeUpgrade успешно включён.");
        getLogger().info("Загружено улучшений: " + upgradeManager.getTypes().length);
        getLogger().info("Крафты улучшений успешно зарегистрированы.");
        getLogger().info("Система улучшения через стол кузнеца успешно включена.");
        getLogger().info("Реальные бонусы характеристик успешно включены.");
        getLogger().info("Система отделок брони успешно включена.");
        getLogger().info("Способности Армагедона для брони успешно включены.");
        getLogger().info("Способности Армагедона для оружия успешно включены.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ForgeUpgrade успешно выключен.");
    }

    public UpgradeManager getUpgradeManager() { return upgradeManager; }
    public UpgradeApplier getUpgradeApplier() { return upgradeApplier; }
}
