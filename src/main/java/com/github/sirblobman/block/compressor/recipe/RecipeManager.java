package com.github.sirblobman.block.compressor.recipe;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import com.github.sirblobman.api.configuration.ConfigurationManager;
import com.github.sirblobman.api.nms.ItemHandler;
import com.github.sirblobman.api.nms.MultiVersionHandler;
import com.github.sirblobman.api.utility.ConfigurationHelper;
import com.github.sirblobman.api.utility.ItemUtility;
import com.github.sirblobman.block.compressor.BlockCompressorPlugin;
import com.github.sirblobman.block.compressor.recipe.ingredient.CustomItemIngredient;
import com.github.sirblobman.block.compressor.recipe.ingredient.Ingredient;
import com.github.sirblobman.block.compressor.recipe.ingredient.IngredientType;
import com.github.sirblobman.block.compressor.recipe.ingredient.MaterialIngredient;
import com.github.sirblobman.api.shaded.xseries.XMaterial;

public final class RecipeManager {
    private final BlockCompressorPlugin plugin;
    private final Map<String, Recipe> recipeMap;

    public RecipeManager(@NotNull BlockCompressorPlugin plugin) {
        this.plugin = plugin;
        this.recipeMap = new LinkedHashMap<>();
    }

    private @NotNull BlockCompressorPlugin getPlugin() {
        return this.plugin;
    }

    private @NotNull Logger getLogger() {
        BlockCompressorPlugin plugin = getPlugin();
        return plugin.getLogger();
    }

    private void printDebug(@NotNull String message) {
        BlockCompressorPlugin plugin = getPlugin();
        YamlConfiguration configuration = plugin.getConfig();
        if (configuration.getBoolean("debug-mode", false)) {
            Logger logger = getLogger();
            logger.info("[Debug] [Recipes] " + message);
        }
    }

    public @NotNull Map<String, Recipe> getRecipes() {
        return Collections.unmodifiableMap(this.recipeMap);
    }

    public @Nullable Recipe getRecipe(@NotNull String id) {
        Map<String, Recipe> recipeMap = getRecipes();
        return recipeMap.get(id);
    }

    public void loadRecipes() {
        BlockCompressorPlugin plugin = getPlugin();
        this.recipeMap.clear();

        ConfigurationManager configurationManager = plugin.getConfigurationManager();
        YamlConfiguration configuration = configurationManager.get("recipes.yml");
        Set<String> recipeIdSet = configuration.getKeys(false);

        for (String recipeId : recipeIdSet) {
            ConfigurationSection section = configuration.getConfigurationSection(recipeId);
            if (section == null) {
                printDebug("Invalid section. Ignoring recipe '" + recipeId + "'.");
                continue;
            }

            printDebug("Loading recipe '" + recipeId + "'...");
            loadRecipe(recipeId, section);
        }

        int recipeCount = this.recipeMap.size();
        printDebug("Successfully loaded " + recipeCount + " recipe(s).");
    }

    private void loadRecipe(@NotNull String recipeId, @NotNull ConfigurationSection section) {
        ConfigurationSection inputSection = section.getConfigurationSection("input");
        if (inputSection == null) {
            printDebug("Invalid recipe '" + recipeId + "'. Missing 'input' section.");
            return;
        }

        ConfigurationSection outputSection = section.getConfigurationSection("output");
        if (outputSection == null) {
            printDebug("Invalid recipe '" + recipeId + "'. Missing 'output' section.");
            return;
        }

        Ingredient input = loadIngredient(inputSection);
        if (input == null) {
            printDebug("Invalid recipe '" + recipeId + "'. Invalid 'input' ingredient settings.");
            return;
        }

        Ingredient output = loadIngredient(outputSection);
        if (output == null) {
            printDebug("Invalid recipe '" + recipeId + "'. Invalid 'output' ingredient settings.");
            return;
        }

        Recipe recipe = new Recipe(recipeId, input, output);
        this.recipeMap.put(recipeId, recipe);
    }

    private @Nullable Ingredient loadIngredient(@NotNull ConfigurationSection section) {
        String typeName = section.getString("type");
        if (typeName == null) {
            return null;
        }

        IngredientType type = ConfigurationHelper.parseEnum(IngredientType.class, typeName, null);
        if (type == null) {
            return null;
        }

        switch (type) {
            case MATERIAL: return loadMaterial(section);
            case CUSTOM_ITEM: return loadCustomItem(section);
            default: break;
        }

        return null;
    }

    private @Nullable Ingredient loadMaterial(@NotNull ConfigurationSection section) {
        String materialName = section.getString("material");
        if (materialName == null) {
            printDebug("Missing 'material' setting.");
            return null;
        }

        Optional<XMaterial> optionalMaterial = XMaterial.matchXMaterial(materialName);
        if (optionalMaterial.isPresent()) {
            int quantity = section.getInt("quantity", 1);
            XMaterial material = optionalMaterial.get();
            return new MaterialIngredient(material, quantity);
        }

        printDebug("XMaterial value '" + materialName + "' does not exist.");
        return null;
    }

    private @Nullable Ingredient loadCustomItem(@NotNull ConfigurationSection section) {
        String base64 = section.getString("base64");
        if (base64 == null) {
            printDebug("Missing 'base64' setting.");
            return null;
        }

        BlockCompressorPlugin plugin = getPlugin();
        MultiVersionHandler multiVersionHandler = plugin.getMultiVersionHandler();
        ItemHandler itemHandler = multiVersionHandler.getItemHandler();
        ItemStack stack = itemHandler.fromBase64String(base64);
        if (ItemUtility.isAir(stack)) {
            printDebug("Failed to load item from base64 string.");
            return null;
        }

        int quantity = section.getInt("quantity", 1);
        return new CustomItemIngredient(stack, quantity);
    }
}
