package com.github.sirblobman.block.compressor.listener;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
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
import com.github.sirblobman.api.xseries.XSound;
import com.github.sirblobman.block.compressor.BlockCompressorPlugin;
import com.github.sirblobman.block.compressor.manager.CompressorRecipeManager;
import com.github.sirblobman.block.compressor.manager.ToolManager;

public final class ListenerCompressorTool extends PluginListener<BlockCompressorPlugin> {
    public ListenerCompressorTool(BlockCompressorPlugin plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if(isNotMainHand(e)) {
            return;
        }

        Player player = e.getPlayer();
        ItemStack item = getItemInMainHand(player);
        ToolManager toolManager = getToolManager();

        if (!toolManager.isCompressorTool(item)) {
            return;
        }

        e.setCancelled(true);
        Action action = e.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = e.getClickedBlock();
        if (block == null) {
            return;
        }

        BlockState state = block.getState();
        CompressorRecipeManager recipeManager = getRecipeManager();
        if (!recipeManager.canCompressContents(state)) {
            return;
        }

        Location blockLocation = block.getLocation();
        Location dropLocation = blockLocation.add(0.5D, 1.0D, 0.5D);
        InventoryHolder inventoryHolder = (InventoryHolder) state;
        Inventory inventory = inventoryHolder.getInventory();

        boolean success = recipeManager.compressRecursive(dropLocation, inventory);
        if (success && toolManager.hasDurability(item)) {
            item = toolManager.decreaseDurability(item);
            int durability = toolManager.getDurability(item);
            if (durability <= 0) {
                XSound.ENTITY_ITEM_BREAK.play(player, 1.0F, 1.0F);
                setItemInMainHand(player, ItemUtility.getAir());
            } else {
                item = toolManager.updateDurability(player, item);
                setItemInMainHand(player, item);
            }
        }

        LanguageManager languageManager = getLanguageManager();
        String messagePath = ("compress-" + (success ? "successful" : "failure"));
        languageManager.sendMessage(player, messagePath);
    }

    private ToolManager getToolManager() {
        BlockCompressorPlugin plugin = getPlugin();
        return plugin.getToolManager();
    }

    private CompressorRecipeManager getRecipeManager() {
        BlockCompressorPlugin plugin = getPlugin();
        return plugin.getCompressorRecipeManager();
    }

    private LanguageManager getLanguageManager() {
        BlockCompressorPlugin plugin = getPlugin();
        return plugin.getLanguageManager();
    }

    private boolean isNotMainHand(PlayerInteractEvent e) {
        int minorVersion = VersionUtility.getMinorVersion();
        if(minorVersion < 9) {
            return false;
        }

        EquipmentSlot hand = e.getHand();
        return (hand != EquipmentSlot.HAND);
    }

    @SuppressWarnings("deprecation")
    private ItemStack getItemInMainHand(Player player) {
        int minorVersion = VersionUtility.getMinorVersion();
        PlayerInventory playerInventory = player.getInventory();
        if (minorVersion < 9) {
            return playerInventory.getItemInHand();
        }

        return playerInventory.getItemInMainHand();
    }

    @SuppressWarnings("deprecation")
    private void setItemInMainHand(Player player, ItemStack item) {
        int minorVersion = VersionUtility.getMinorVersion();
        PlayerInventory playerInventory = player.getInventory();
        if (minorVersion < 9) {
            playerInventory.setItemInHand(item);
            return;
        }

        playerInventory.setItemInMainHand(item);
    }
}
