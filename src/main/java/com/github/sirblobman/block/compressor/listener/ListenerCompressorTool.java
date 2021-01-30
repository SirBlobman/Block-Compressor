package com.github.sirblobman.block.compressor.listener;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.github.sirblobman.api.utility.ItemUtility;
import com.github.sirblobman.api.utility.Validate;
import com.github.sirblobman.api.xseries.XMaterial;
import com.github.sirblobman.block.compressor.BlockCompressorPlugin;
import com.github.sirblobman.block.compressor.manager.CompressorRecipeManager;
import com.github.sirblobman.block.compressor.object.CompressorRecipe;

public final class ListenerCompressorTool implements Listener {
    private final BlockCompressorPlugin plugin;
    public ListenerCompressorTool(BlockCompressorPlugin plugin) {
        this.plugin = Validate.notNull(plugin, "plugin must not be null!");
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onInteract(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        if(!this.plugin.isCompressorTool(item)) return;
        e.setCancelled(true);

        Action action = e.getAction();
        if(action != Action.RIGHT_CLICK_BLOCK) return;

        Block block = e.getClickedBlock();
        if(block == null) return;

        World world = block.getWorld();
        Location dropLocation = block.getLocation().add(0.0D, 1.0D, 0.0D);

        BlockState state = block.getState();
        if(!(state instanceof Chest) && !(state instanceof DoubleChest)) return;
        InventoryHolder inventoryHolder = (InventoryHolder) state;
        Inventory inventory = inventoryHolder.getInventory();

        CompressorRecipeManager compressorRecipeManager = this.plugin.getCompressorRecipeManager();
        Set<CompressorRecipe> recipeSet = compressorRecipeManager.getRecipes();

        boolean success = false;
        for(CompressorRecipe recipe : recipeSet) {
            XMaterial input = recipe.getInput();
            int removeCount = removeAndCount(input, inventory);
            if(removeCount <= 0) continue;

            ItemStack[] convert = recipe.convert(removeCount);
            if(!success) success = (removeCount >= recipe.getAmount());

            Map<Integer, ItemStack> leftover = inventory.addItem(convert);
            Collection<ItemStack> dropCollection = leftover.values();
            for(ItemStack drop : dropCollection) {
                if(ItemUtility.isAir(drop)) continue;
                world.dropItem(dropLocation, drop);
            }
        }

        Player player = e.getPlayer();
        String messagePath = ("compress-" + (success ? "successful" : "failure"));
        this.plugin.sendMessage(player, messagePath);
    }

    private int removeAndCount(XMaterial material, Inventory inventory) {
        int amount = 0;
        int inventorySize = inventory.getSize();
        ItemStack air = ItemUtility.getAir();

        for(int slot = 0; slot < inventorySize; slot++) {
            ItemStack item = inventory.getItem(slot);
            if(ItemUtility.isAir(item)) continue;
            if(!material.isSimilar(item)) continue;

            amount += item.getAmount();
            inventory.setItem(slot, air);
        }

        return amount;
    }
}