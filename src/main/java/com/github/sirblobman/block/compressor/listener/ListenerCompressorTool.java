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
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.github.sirblobman.api.language.LanguageManager;
import com.github.sirblobman.api.plugin.listener.PluginListener;
import com.github.sirblobman.api.utility.ItemUtility;
import com.github.sirblobman.api.utility.VersionUtility;
import com.github.sirblobman.api.xseries.XMaterial;
import com.github.sirblobman.api.xseries.XSound;
import com.github.sirblobman.block.compressor.BlockCompressorPlugin;
import com.github.sirblobman.block.compressor.manager.CompressorRecipeManager;
import com.github.sirblobman.block.compressor.object.CompressorRecipe;

public final class ListenerCompressorTool extends PluginListener<BlockCompressorPlugin> {
    public ListenerCompressorTool(BlockCompressorPlugin plugin) {
        super(plugin);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onInteract(PlayerInteractEvent e) {
        int minorVersion = VersionUtility.getMinorVersion();
        if(minorVersion > 8) {
            EquipmentSlot hand = e.getHand();
            if(hand != EquipmentSlot.HAND) {
                return;
            }
        }

        Player player = e.getPlayer();
        ItemStack item = getItemInMainHand(player);

        BlockCompressorPlugin plugin = getPlugin();
        if(!plugin.isCompressorTool(item)) {
            return;
        }
        
        e.setCancelled(true);
        Action action = e.getAction();
        if(action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = e.getClickedBlock();
        if(block == null) {
            return;
        }

        World world = block.getWorld();
        Location dropLocation = block.getLocation().add(0.0D, 1.0D, 0.0D);

        BlockState state = block.getState();
        if(!(state instanceof Chest) && !(state instanceof DoubleChest)) {
            return;
        }
        
        InventoryHolder inventoryHolder = (InventoryHolder) state;
        Inventory inventory = inventoryHolder.getInventory();

        CompressorRecipeManager compressorRecipeManager = plugin.getCompressorRecipeManager();
        Set<CompressorRecipe> recipeSet = compressorRecipeManager.getRecipes();

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

        if(success && plugin.hasDurability(item)) {
            item = plugin.decreaseDurability(item);
            int durability = plugin.getDurability(item);
            if(durability <= 0) {
                XSound.ENTITY_ITEM_BREAK.play(player, 1.0F, 1.0F);
                setItemInMainHand(player, ItemUtility.getAir());
            } else {
                plugin.updateDurability(item);
                setItemInMainHand(player, item);
            }
        }

        LanguageManager languageManager = plugin.getLanguageManager();
        String messagePath = ("compress-" + (success ? "successful" : "failure"));
        languageManager.sendMessage(player, messagePath, null, true);
    }

    private int removeAndCount(XMaterial material, Inventory inventory) {
        int amount = 0;
        int inventorySize = inventory.getSize();
        ItemStack air = ItemUtility.getAir();

        for(int slot = 0; slot < inventorySize; slot++) {
            ItemStack item = inventory.getItem(slot);
            if(ItemUtility.isAir(item) || !material.isSimilar(item)) {
                continue;
            }

            amount += item.getAmount();
            inventory.setItem(slot, air);
        }

        return amount;
    }

    @SuppressWarnings("deprecation")
    private ItemStack getItemInMainHand(Player player) {
        int minorVersion = VersionUtility.getMinorVersion();
        PlayerInventory playerInventory = player.getInventory();
        return (minorVersion < 9 ? playerInventory.getItemInHand() : playerInventory.getItemInMainHand());
    }

    @SuppressWarnings("deprecation")
    private void setItemInMainHand(Player player, ItemStack item) {
        int minorVersion = VersionUtility.getMinorVersion();
        PlayerInventory playerInventory = player.getInventory();
        if(minorVersion < 9) {
            playerInventory.setItemInHand(item);
            return;
        }

        playerInventory.setItemInMainHand(item);
    }
}
