package com.github.sirblobman.block.compressor.manager;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.github.sirblobman.api.utility.ItemUtility;
import com.github.sirblobman.api.utility.Validate;
import com.github.sirblobman.api.xseries.XMaterial;
import com.github.sirblobman.block.compressor.BlockCompressorPlugin;
import com.github.sirblobman.block.compressor.object.CompressorRecipe;

public final class CompressorRecipeManager {
    private final BlockCompressorPlugin plugin;
    private final Set<CompressorRecipe> recipeSet;

    public CompressorRecipeManager(BlockCompressorPlugin plugin) {
        this.plugin = Validate.notNull(plugin, "plugin must not be null!");
        this.recipeSet = new HashSet<>();
    }

    public void reloadRecipes() {
        this.recipeSet.clear();
        Logger logger = this.plugin.getLogger();

        YamlConfiguration configuration = this.plugin.getConfig();
        ConfigurationSection sectionRecipes = configuration.getConfigurationSection("recipes");
        if(sectionRecipes == null) {
            return;
        }

        Set<String> keySet = sectionRecipes.getKeys(false);
        for(String key : keySet) {
            ConfigurationSection section = sectionRecipes.getConfigurationSection(key);
            if(section == null) {
                continue;
            }

            String inputMaterialName = section.getString("input");
            String outputMaterialName = section.getString("output");
            int amount = section.getInt("amount");

            if(amount < 1) {
                logger.warning("Invalid Compressor Recipe '" + key + "': amount must be greater than 0");
                continue;
            }

            Optional<XMaterial> optionalInputMaterial = XMaterial.matchXMaterial(inputMaterialName);
            XMaterial inputMaterial = optionalInputMaterial.orElse(XMaterial.AIR);
            if(ItemUtility.isAir(inputMaterial.parseItem())) {
                logger.warning("Invalid Compressor Recipe '" + key + "': input is not a valid XMaterial value!");
                continue;
            }

            Optional<XMaterial> optionalOutputMaterial = XMaterial.matchXMaterial(outputMaterialName);
            XMaterial outputMaterial = optionalOutputMaterial.orElse(XMaterial.AIR);
            if(ItemUtility.isAir(outputMaterial.parseItem())) {
                logger.warning("Invalid Compressor Recipe '" + key + "': output is not a valid XMaterial value!");
                continue;
            }

            CompressorRecipe compressorRecipe = new CompressorRecipe(inputMaterial, outputMaterial, amount);
            this.recipeSet.add(compressorRecipe);
        }
    }

    public Set<CompressorRecipe> getRecipes() {
        return Collections.unmodifiableSet(this.recipeSet);
    }
}
