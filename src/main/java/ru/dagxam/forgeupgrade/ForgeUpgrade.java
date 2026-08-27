package ru.dagxam.forgeupgrade;

import org.bukkit.plugin.java.JavaPlugin;
import ru.dagxam.forgeupgrade.command.ForgeUpgradeCommand;
import ru.dagxam.forgeupgrade.listener.ArmageddonArmorListener;
import ru.dagxam.forgeupgrade.listener.ArmageddonToolListener;
import ru.dagxam.forgeupgrade.listener.ArmageddonWeaponListener;
import ru.dagxam.forgeupgrade.listener.UpgradeAttributeListener;
import ru.dagxam.forgeupgrade.listener.UpgradeStatusHudListener;
import ru.dagxam.forgeupgrade.listener.UpgradeTableListener;
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
        getServer().getPluginManager().registerEvents(new UpgradeTableListener(this, upgradeManager, upgradeApplier), this);
        getServer().getPluginManager().registerEvents(new UpgradeAttributeListener(this, upgradeApplier), this);
        getServer().getPluginManager().registerEvents(new ArmageddonArmorListener(this, upgradeApplier), this);
        getServer().getPluginManager().registerEvents(new ArmageddonWeaponListener(this, upgradeApplier), this);
        getServer().getPluginManager().registerEvents(new ArmageddonToolListener(this, upgradeApplier), this);
        new UpgradeStatusHudListener(this, upgradeApplier);

        ForgeUpgradeCommand command = new ForgeUpgradeCommand(this, upgradeManager);
        if (getCommand("forgeupgrade") != null) {
            getCommand("forgeupgrade").setExecutor(command);
            getCommand("forgeupgrade").setTabCompleter(command);
        }

        getLogger().info("ForgeUpgrade успешно включён.");
        getLogger().info("Загружено улучшений: " + upgradeManager.getTypes().length);
        getLogger().info("Крафты улучшений успешно зарегистрированы.");
        getLogger().info("Отдельный Стол улучшений успешно включён.");
        getLogger().info("Обычный кузнечный стол оставлен без изменений.");
        getLogger().info("Реальные бонусы характеристик успешно включены.");
        getLogger().info("Цифровой HUD здоровья и брони успешно включён.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ForgeUpgrade успешно выключен.");
    }

    public UpgradeManager getUpgradeManager() { return upgradeManager; }
    public UpgradeApplier getUpgradeApplier() { return upgradeApplier; }
}
