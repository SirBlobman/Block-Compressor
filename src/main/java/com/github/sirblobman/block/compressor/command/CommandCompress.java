package com.github.sirblobman.block.compressor.command;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.github.sirblobman.api.command.PlayerCommand;
import com.github.sirblobman.api.language.LanguageManager;
import com.github.sirblobman.api.utility.ItemUtility;
import com.github.sirblobman.api.xseries.XMaterial;
import com.github.sirblobman.block.compressor.BlockCompressorPlugin;
import com.github.sirblobman.block.compressor.manager.CompressorRecipeManager;
import com.github.sirblobman.block.compressor.object.CompressorRecipe;

import org.jetbrains.annotations.NotNull;

public final class CommandCompress extends PlayerCommand {
    private final BlockCompressorPlugin plugin;

    public CommandCompress(BlockCompressorPlugin plugin) {
        super(plugin, "compress");
        this.plugin = plugin;
    }

    @NotNull
    @Override
    protected LanguageManager getLanguageManager() {
        return this.plugin.getLanguageManager();
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public boolean execute(Player player, String[] args) {
        PlayerInventory inventory = player.getInventory();
        CompressorRecipeManager compressorRecipeManager = this.plugin.getCompressorRecipeManager();
        Set<CompressorRecipe> recipeSet = compressorRecipeManager.getRecipes();

        World world = player.getWorld();
        Location dropLocation = player.getLocation();

        boolean success = false;
        for(CompressorRecipe recipe : recipeSet) {
            XMaterial input = recipe.getInput();
            int removeCount = removeAndCount(input, inventory);
            if(removeCount <= 0) {
                continue;
            }

            ItemStack[] convert = recipe.convert(removeCount);
            if(!success) success = (removeCount >= recipe.getAmount());

            Map<Integer, ItemStack> leftover = inventory.addItem(convert);
            Collection<ItemStack> dropCollection = leftover.values();
            for(ItemStack drop : dropCollection) {
                if(ItemUtility.isAir(drop)) {
                    continue;
                }
                
                world.dropItem(dropLocation, drop);
            }
        }

        LanguageManager languageManager = getLanguageManager();
        String messagePath = ("compress-" + (success ? "successful" : "failure"));
        languageManager.sendMessage(player, messagePath, null, true);

        player.updateInventory();
        return true;
    }

    private int removeAndCount(XMaterial material, Inventory inventory) {
        int amount = 0;
        int inventorySize = inventory.getSize();
        ItemStack air = ItemUtility.getAir();

        for(int slot = 0; slot < inventorySize; slot++) {
            ItemStack item = inventory.getItem(slot);
            if(ItemUtility.isAir(item)) {
                continue;
            }
            
            if(!material.isSimilar(item)) {
                continue;
            }

            amount += item.getAmount();
            inventory.setItem(slot, air);
        }

        return amount;
    }
}
