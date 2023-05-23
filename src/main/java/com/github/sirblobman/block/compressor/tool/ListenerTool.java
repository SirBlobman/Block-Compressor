package com.github.sirblobman.block.compressor.tool;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
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
import com.github.sirblobman.api.nbt.CustomNbtContainer;
import com.github.sirblobman.api.nbt.CustomNbtTypes;
import com.github.sirblobman.api.nms.ItemHandler;
import com.github.sirblobman.api.nms.MultiVersionHandler;
import com.github.sirblobman.api.plugin.listener.PluginListener;
import com.github.sirblobman.api.utility.VersionUtility;
import com.github.sirblobman.block.compressor.BlockCompressorPlugin;
import com.github.sirblobman.api.shaded.xseries.XSound;

public class ListenerTool extends PluginListener<BlockCompressorPlugin> {
    public ListenerTool(@NotNull BlockCompressorPlugin plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e) {
        ItemStack item = getItemInHand(e);
        Tool tool = getTool(item);
        if (tool == null) {
            return;
        }

        e.setUseItemInHand(Result.DENY);

        Action action = e.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = e.getClickedBlock();
        if (block == null) {
            return;
        }

        BlockState blockState = block.getState();
        BlockCompressorPlugin plugin = getPlugin();
        ToolManager toolManager = plugin.getToolManager();
        if (!toolManager.canCompress(blockState)) {
            return;
        }

        Location blockLocation = block.getLocation();
        Location dropLocation = blockLocation.clone().add(0.5D, 1.5D, 0.5D);
        InventoryHolder holder = (InventoryHolder) blockState;
        Inventory inventory = holder.getInventory();

        Player player = e.getPlayer();
        boolean success = toolManager.compress(dropLocation, inventory);
        if (success && !tool.isInfiniteDurability()) {
            item = decreaseDurability(tool, item);
            int durability = getDurability(item);
            if (durability <= 0) {
                XSound.ENTITY_ITEM_BREAK.play(player, 1.0F, 1.0F);
                setItemInHand(e, null);
            } else {
                setItemInHand(e, item);
            }
        }

        LanguageManager languageManager = plugin.getLanguageManager();
        String messagePath = ("compress-" + (success ? "successful" : "failure"));
        languageManager.sendMessage(player, messagePath);
    }

    private @NotNull ItemStack getItemInHand(@NotNull PlayerInteractEvent e) {
        Player player = e.getPlayer();
        int minorVersion = VersionUtility.getMinorVersion();

        if (minorVersion >= 9) {
            EquipmentSlot hand = e.getHand();
            return getItemInHandModern(player, hand != null ? hand : EquipmentSlot.HAND);
        }

        return getItemInHandLegacy(player);
    }

    private void setItemInHand(@NotNull PlayerInteractEvent e, @Nullable ItemStack item) {
        Player player = e.getPlayer();
        int minorVersion = VersionUtility.getMinorVersion();

        if (minorVersion >= 9) {
            EquipmentSlot hand = e.getHand();
            setItemInHandModern(player, hand != null ? hand : EquipmentSlot.HAND, item);
            return;
        }

        setItemInHandLegacy(player, item);
    }

    private @NotNull ItemStack getItemInHandModern(@NotNull Player player, @NotNull EquipmentSlot hand) {
        PlayerInventory playerInventory = player.getInventory();
        if (hand == EquipmentSlot.HAND) {
            return playerInventory.getItemInMainHand();
        }

        return playerInventory.getItemInOffHand();
    }

    private void setItemInHandModern(@NotNull Player player, @NotNull EquipmentSlot hand, @Nullable ItemStack item) {
        PlayerInventory playerInventory = player.getInventory();
        if (hand == EquipmentSlot.HAND) {
            playerInventory.setItemInMainHand(item);
            return;
        }

        playerInventory.setItemInOffHand(item);
    }

    @SuppressWarnings("deprecation")
    private @NotNull ItemStack getItemInHandLegacy(@NotNull Player player) {
        PlayerInventory playerInventory = player.getInventory();
        return playerInventory.getItemInHand();
    }

    @SuppressWarnings("deprecation")
    private void setItemInHandLegacy(@NotNull Player player, @Nullable ItemStack item) {
        PlayerInventory playerInventory = player.getInventory();
        playerInventory.setItemInHand(item);
    }

    private @Nullable Tool getTool(@NotNull ItemStack item) {
        BlockCompressorPlugin plugin = getPlugin();
        MultiVersionHandler multiVersionHandler = plugin.getMultiVersionHandler();
        ItemHandler itemHandler = multiVersionHandler.getItemHandler();

        CustomNbtContainer customNbt = itemHandler.getCustomNbt(item);
        String toolId = customNbt.getOrDefault("compressor-tool", CustomNbtTypes.STRING, null);
        if (toolId == null) {
            return null;
        }

        ToolManager toolManager = plugin.getToolManager();
        return toolManager.getTool(toolId);
    }

    private int getDurability(@NotNull ItemStack item) {
        BlockCompressorPlugin plugin = getPlugin();
        MultiVersionHandler multiVersionHandler = plugin.getMultiVersionHandler();
        ItemHandler itemHandler = multiVersionHandler.getItemHandler();

        CustomNbtContainer customNbt = itemHandler.getCustomNbt(item);
        return customNbt.getOrDefault("durability", CustomNbtTypes.INTEGER, -1);
    }

    private @NotNull ItemStack decreaseDurability(@NotNull Tool tool, @NotNull ItemStack item) {
        if (tool.isInfiniteDurability()) {
            return item;
        }

        int durability = getDurability(item);
        if (durability <= 0) {
            return item;
        }

        BlockCompressorPlugin plugin = getPlugin();
        MultiVersionHandler multiVersionHandler = plugin.getMultiVersionHandler();
        ItemHandler itemHandler = multiVersionHandler.getItemHandler();

        int newDurability = durability - 1;
        CustomNbtContainer customNbt = itemHandler.getCustomNbt(item);
        customNbt.set("durability", CustomNbtTypes.INTEGER, newDurability);
        ItemStack stack = itemHandler.setCustomNbt(item, customNbt);
        return tool.fixDescription(plugin, stack, newDurability);
    }
}
