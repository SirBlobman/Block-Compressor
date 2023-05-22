package com.github.sirblobman.block.compressor.command;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import com.github.sirblobman.api.command.PlayerCommand;
import com.github.sirblobman.block.compressor.BlockCompressorPlugin;
import com.github.sirblobman.block.compressor.manager.CompressorRecipeManager;

public final class CommandCompress extends PlayerCommand {
    private final BlockCompressorPlugin plugin;

    public CommandCompress(@NotNull BlockCompressorPlugin plugin) {
        super(plugin, "compress");
        setPermissionName("block.compressor.command.compress");
        this.plugin = plugin;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull Player player, String @NotNull [] args) {
        return Collections.emptyList();
    }

    @Override
    public boolean execute(@NotNull Player player, String @NotNull [] args) {
        Location dropLocation = player.getLocation();
        PlayerInventory playerInventory = player.getInventory();
        CompressorRecipeManager compressorRecipeManager = getCompressorRecipeManager();

        boolean success = compressorRecipeManager.compressRecursive(dropLocation, playerInventory);
        String messagePath = ("compress-" + (success ? "successful" : "failure"));
        sendMessage(player, messagePath);

        player.updateInventory();
        return true;
    }

    private @NotNull BlockCompressorPlugin getBlockCompressorPlugin() {
        return this.plugin;
    }

    private @NotNull CompressorRecipeManager getCompressorRecipeManager() {
        BlockCompressorPlugin plugin = getBlockCompressorPlugin();
        return plugin.getCompressorRecipeManager();
    }
}
