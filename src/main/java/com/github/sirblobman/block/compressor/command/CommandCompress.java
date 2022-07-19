package com.github.sirblobman.block.compressor.command;

import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import com.github.sirblobman.api.command.PlayerCommand;
import com.github.sirblobman.block.compressor.BlockCompressorPlugin;
import com.github.sirblobman.block.compressor.manager.CompressorRecipeManager;

public final class CommandCompress extends PlayerCommand {
    private final BlockCompressorPlugin plugin;

    public CommandCompress(BlockCompressorPlugin plugin) {
        super(plugin, "compress");
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(Player player, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public boolean execute(Player player, String[] args) {
        Location dropLocation = player.getLocation();
        PlayerInventory playerInventory = player.getInventory();
        CompressorRecipeManager compressorRecipeManager = this.plugin.getCompressorRecipeManager();
        boolean success = compressorRecipeManager.compressRecursive(dropLocation, playerInventory);

        String messagePath = ("compress-" + (success ? "successful" : "failure"));
        sendMessage(player, messagePath, null, true);
        player.updateInventory();
        return true;
    }
}
