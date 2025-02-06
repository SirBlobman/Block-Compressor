package com.github.sirblobman.block.compressor.tool;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

import com.github.sirblobman.api.configuration.ConfigurationManager;
import com.github.sirblobman.api.utility.ItemUtility;
import com.github.sirblobman.api.utility.Validate;
import com.github.sirblobman.api.utility.VersionUtility;
import com.github.sirblobman.block.compressor.BlockCompressorPlugin;
import com.github.sirblobman.block.compressor.recipe.Recipe;
import com.github.sirblobman.block.compressor.recipe.RecipeManager;
import com.github.sirblobman.block.compressor.recipe.ingredient.Ingredient;
import com.github.sirblobman.api.shaded.xseries.XMaterial;

public final class ToolManager {
    private final BlockCompressorPlugin plugin;
    private final Map<String, Tool> toolMap;

    public ToolManager(@NotNull BlockCompressorPlugin plugin) {
        this.plugin = plugin;
        this.toolMap = new LinkedHashMap<>();
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
            logger.info("[Debug] [Tools] " + message);
        }
    }

    public @NotNull Map<String, Tool> getTools() {
        return Collections.unmodifiableMap(this.toolMap);
    }

    public @Nullable Tool getTool(@NotNull String id) {
        Map<String, Tool> toolMap = getTools();
        return toolMap.get(id);
    }

    public void loadTools() {
        BlockCompressorPlugin plugin = getPlugin();
        this.toolMap.clear();

        ConfigurationManager configurationManager = plugin.getConfigurationManager();
        YamlConfiguration configuration = configurationManager.get("tools.yml");
        Set<String> toolIdSet = configuration.getKeys(false);

        for (String toolId : toolIdSet) {
            ConfigurationSection section = configuration.getConfigurationSection(toolId);
            if (section == null) {
                printDebug("Invalid section. Ignoring tool '" + toolId + "'.");
                continue;
            }

            printDebug("Loading tool '" + toolId + "'...");
            loadTool(toolId, section);
        }

        int toolCount = this.toolMap.size();
        printDebug("Successfully loaded " + toolCount + " tool(s)");
    }

    private void loadTool(@NotNull String toolId, @NotNull ConfigurationSection section) {
        String materialName = section.getString("material");
        if (materialName == null) {
            printDebug("Missing 'material' setting.");
            return;
        }

        Optional<XMaterial> optionalMaterial = XMaterial.matchXMaterial(materialName);
        if (!optionalMaterial.isPresent()) {
            printDebug("XMaterial value '" + materialName + "' does not exist.");
            return;
        }

        Tool tool = new Tool(toolId);
        tool.setMaterial(optionalMaterial.get());

        if (section.isInt("durability")) {
            tool.setDurability(section.getInt("durability", 100));
            tool.setInfiniteDurability(false);
        } else {
            tool.setDurability(Integer.MAX_VALUE);
            tool.setInfiniteDurability(true);
        }

        tool.setDisplayNameString(section.getString("display-name"));
        tool.setLoreStringList(section.getStringList("lore"));
        tool.setGlowing(section.getBoolean("glowing", false));

        if (section.isInt("model")) {
            tool.setModel(section.getInt("model", 0));
        } else {
            tool.setModel(null);
        }

        this.toolMap.put(toolId, tool);
    }

    public boolean canCompress(@NotNull BlockState state) {
        int minorVersion = VersionUtility.getMinorVersion();
        if (minorVersion >= 11) {
            if (state instanceof ShulkerBox) {
                return true;
            }
        }

        return (state instanceof Chest || state instanceof DoubleChest);
    }

    public boolean compress(@NotNull Location location, @NotNull Inventory inventory) {
        World world = location.getWorld();
        Validate.notNull(world, "location must have a valid world!");

        boolean success = false;
        for (ItemStack item : inventory) {
            if (ItemUtility.isAir(item)) {
                continue;
            }

            ItemMeta itemMeta = item.getItemMeta();
            if (itemMeta instanceof BlockStateMeta) {
                BlockStateMeta blockStateMeta = (BlockStateMeta) itemMeta;
                BlockState blockState = blockStateMeta.getBlockState();
                if (canCompress(blockState) && blockState instanceof InventoryHolder) {
                    InventoryHolder holder = (InventoryHolder) blockState;
                    Inventory itemInventory = holder.getInventory();

                    boolean compress = compress(location, itemInventory);
                    if (!success && compress) {
                        success = true;
                    }

                    blockStateMeta.setBlockState(blockState);
                    item.setItemMeta(blockStateMeta);
                }
            }
        }

        BlockCompressorPlugin plugin = getPlugin();
        RecipeManager recipeManager = plugin.getRecipeManager();
        Map<String, Recipe> recipeMap = recipeManager.getRecipes();
        Collection<Recipe> recipes = recipeMap.values();

        for (Recipe recipe : recipes) {
            Ingredient input = recipe.input();
            int removeCount = removeAndCount(input, inventory);
            if (removeCount <= 0) {
                continue;
            }

            ItemStack[] output = recipe.convert(removeCount);
            if (!success) {
                success = (removeCount >= input.getQuantity());
            }

            HashMap<Integer, ItemStack> leftover = inventory.addItem(output);
            Collection<ItemStack> drops = leftover.values();
            for (ItemStack drop : drops) {
                if (ItemUtility.isAir(drop)) {
                    continue;
                }

                world.dropItem(location, drop);
            }
        }

        return success;
    }

    private int removeAndCount(@NotNull Ingredient ingredient, @NotNull Inventory inventory) {
        int amount = 0;
        int inventorySize = inventory.getSize();
        ItemStack air = ItemUtility.getAir();

        for (int slot = 0; slot < inventorySize; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (ItemUtility.isAir(item) || !ingredient.matches(item)) {
                continue;
            }

            amount += item.getAmount();
            inventory.setItem(slot, air);
        }

        return amount;
    }
}
