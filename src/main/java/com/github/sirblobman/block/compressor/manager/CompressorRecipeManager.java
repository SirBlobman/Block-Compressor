package com.github.sirblobman.block.compressor.manager;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import com.github.sirblobman.api.utility.ItemUtility;
import com.github.sirblobman.api.utility.Validate;
import com.github.sirblobman.api.utility.VersionUtility;
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
        if (sectionRecipes == null) {
            return;
        }

        Set<String> keySet = sectionRecipes.getKeys(false);
        for (String key : keySet) {
            ConfigurationSection section = sectionRecipes.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            String inputMaterialName = section.getString("input");
            String outputMaterialName = section.getString("output");
            int amount = section.getInt("amount");

            if (amount < 1) {
                logger.warning("Invalid Compressor Recipe '" + key + "': amount must be greater than 0");
                continue;
            }

            Optional<XMaterial> optionalInputMaterial = XMaterial.matchXMaterial(inputMaterialName);
            XMaterial inputMaterial = optionalInputMaterial.orElse(XMaterial.AIR);
            if (ItemUtility.isAir(inputMaterial.parseItem())) {
                logger.warning("Invalid Compressor Recipe '" + key + "': input is not a valid XMaterial value!");
                continue;
            }

            Optional<XMaterial> optionalOutputMaterial = XMaterial.matchXMaterial(outputMaterialName);
            XMaterial outputMaterial = optionalOutputMaterial.orElse(XMaterial.AIR);
            if (ItemUtility.isAir(outputMaterial.parseItem())) {
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

    public boolean isAllowed(BlockState state) {
        int minorVersion = VersionUtility.getMinorVersion();
        if (minorVersion >= 11) {
            if (state instanceof ShulkerBox) {
                return true;
            }
        }

        return (state instanceof Chest || state instanceof DoubleChest);
    }

    private int removeAndCount(XMaterial material, Inventory inventory) {
        int amount = 0;
        int inventorySize = inventory.getSize();
        ItemStack air = ItemUtility.getAir();

        for (int slot = 0; slot < inventorySize; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (ItemUtility.isAir(item) || !material.isSimilar(item)) {
                continue;
            }

            amount += item.getAmount();
            inventory.setItem(slot, air);
        }

        return amount;
    }

    public boolean compressRecursive(Location dropLocation, Inventory inventory) {
        Validate.notNull(dropLocation, "dropLocation must not be null!");
        Validate.notNull(inventory, "inventory must not be null!");

        World world = dropLocation.getWorld();
        Validate.notNull(world, "dropLocation must have a valid world!");

        Set<CompressorRecipe> recipeSet = getRecipes();
        boolean success = false;

        for (ItemStack item : inventory) {
            if(item == null) {
                continue;
            }

            ItemMeta itemMeta = item.getItemMeta();
            if(itemMeta == null) {
                continue;
            }

            if (itemMeta instanceof BlockStateMeta) {
                BlockStateMeta stateMeta = (BlockStateMeta) itemMeta;
                BlockState blockState = stateMeta.getBlockState();
                if (isAllowed(blockState) && blockState instanceof InventoryHolder) {
                    InventoryHolder holder = (InventoryHolder) blockState;
                    Inventory itemInventory = holder.getInventory();

                    boolean compress = compressRecursive(dropLocation, itemInventory);
                    if (!success && compress) {
                        success = true;
                    }

                    stateMeta.setBlockState(blockState);
                    item.setItemMeta(stateMeta);
                }
            }
        }

        for (CompressorRecipe recipe : recipeSet) {
            XMaterial input = recipe.getInput();
            int removeCount = removeAndCount(input, inventory);
            if (removeCount <= 0) {
                continue;
            }

            ItemStack[] convert = recipe.convert(removeCount);
            if (!success) {
                success = (removeCount >= recipe.getAmount());
            }

            Map<Integer, ItemStack> leftover = inventory.addItem(convert);
            Collection<ItemStack> dropCollection = leftover.values();
            for (ItemStack drop : dropCollection) {
                if (ItemUtility.isAir(drop)) {
                    continue;
                }

                world.dropItem(dropLocation, drop);
            }
        }

        return success;
    }
}
