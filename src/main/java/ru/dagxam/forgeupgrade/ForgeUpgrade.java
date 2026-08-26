package ru.dagxam.forgeupgrade;

import org.bukkit.plugin.java.JavaPlugin;

public final class ForgeUpgrade extends JavaPlugin {

    private static ForgeUpgrade instance;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        getLogger().info("========================================");
        getLogger().info("ForgeUpgrade включён.");
        getLogger().info("Система улучшения предметов загружена.");
        getLogger().info("Все сообщения плагина используются на русском языке.");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("ForgeUpgrade выключен.");
    }

    public static ForgeUpgrade getInstance() {
        return instance;
    }
}
