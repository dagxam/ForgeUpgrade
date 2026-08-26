package ru.dagxam.forgeupgrade.recipe;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import ru.dagxam.forgeupgrade.upgrade.UpgradeManager;
import ru.dagxam.forgeupgrade.upgrade.UpgradeType;

/** Регистрирует крафты улучшений. */
public final class RecipeManager {
    private final JavaPlugin plugin;
    private final UpgradeManager upgradeManager;

    public RecipeManager(JavaPlugin plugin, UpgradeManager upgradeManager) {
        this.plugin = plugin;
        this.upgradeManager = upgradeManager;
    }

    public void registerRecipes() {
        registerPaperRecipe(UpgradeType.GOLD, Material.GOLD_INGOT);
        registerPaperRecipe(UpgradeType.EMERALD, Material.EMERALD);
        registerPaperRecipe(UpgradeType.DIAMOND, Material.DIAMOND);
        registerPaperRecipe(UpgradeType.NETHERITE, Material.NETHERITE_INGOT);
        registerArmageddonRecipe();
    }

    private void registerPaperRecipe(UpgradeType type, Material center) {
        NamespacedKey key = new NamespacedKey(plugin, type.getId() + "_upgrade");
        ShapedRecipe recipe = new ShapedRecipe(key, upgradeManager.createUpgrade(type));
        recipe.shape("PPP", "PCP", "PPP");
        recipe.setIngredient('P', Material.PAPER);
        recipe.setIngredient('C', center);
        plugin.getServer().addRecipe(recipe);
    }

    private void registerArmageddonRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "armageddon_upgrade");
        ShapedRecipe recipe = new ShapedRecipe(key, upgradeManager.createUpgrade(UpgradeType.ARMAGEDDON));
        recipe.shape("CIL", "GPR", "EDN");
        recipe.setIngredient('C', Material.COPPER_INGOT);
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('L', Material.LAPIS_LAZULI);
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('P', Material.PAPER);
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('E', Material.EMERALD);
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('N', Material.NETHERITE_INGOT);
        plugin.getServer().addRecipe(recipe);
    }
}
