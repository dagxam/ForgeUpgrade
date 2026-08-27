package ru.dagxam.forgeupgrade.recipe;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import ru.dagxam.forgeupgrade.upgrade.UpgradeManager;
import ru.dagxam.forgeupgrade.upgrade.UpgradeType;

/** Регистрирует точные рецепты кузнечных шаблонов ForgeUpgrade. */
public final class RecipeManager {
    private final JavaPlugin plugin;
    private final UpgradeManager upgradeManager;

    public RecipeManager(JavaPlugin plugin, UpgradeManager upgradeManager) {
        this.plugin = plugin;
        this.upgradeManager = upgradeManager;
    }

    public void registerRecipes() {
        registerFrameRecipe(UpgradeType.GOLD, Material.GOLD_INGOT);
        registerFrameRecipe(UpgradeType.EMERALD, Material.EMERALD);
        registerFrameRecipe(UpgradeType.DIAMOND, Material.DIAMOND);
        registerFrameRecipe(UpgradeType.NETHERITE, Material.NETHERITE_INGOT);
        registerArmageddonRecipe();
    }

    /**
     * XXX / XBX / XXX — 8 материалов вокруг бумаги.
     */
    private void registerFrameRecipe(UpgradeType type, Material material) {
        NamespacedKey key = new NamespacedKey(plugin, type.getId() + "_upgrade_template");
        ShapedRecipe recipe = new ShapedRecipe(key, upgradeManager.createUpgrade(type));
        recipe.shape("MMM", "MBM", "MMM");
        recipe.setIngredient('M', material);
        recipe.setIngredient('B', Material.PAPER);
        plugin.getServer().addRecipe(recipe);
    }

    /**
     * М Ж З
     * Л Б Р
     * И А Н
     */
    private void registerArmageddonRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "armageddon_upgrade_template");
        ShapedRecipe recipe = new ShapedRecipe(key, upgradeManager.createUpgrade(UpgradeType.ARMAGEDDON));
        recipe.shape("CIG", "LPR", "EDN");
        recipe.setIngredient('C', Material.COPPER_INGOT);
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('L', Material.LAPIS_LAZULI);
        recipe.setIngredient('P', Material.PAPER);
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('E', Material.EMERALD);
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('N', Material.NETHERITE_INGOT);
        plugin.getServer().addRecipe(recipe);
    }
}
