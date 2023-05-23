package com.github.sirblobman.block.compressor.command;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import com.github.sirblobman.api.command.PlayerCommand;
import com.github.sirblobman.block.compressor.BlockCompressorPlugin;
import com.github.sirblobman.block.compressor.tool.ToolManager;

public final class CommandCompress extends PlayerCommand {
    private final BlockCompressorPlugin plugin;

    public CommandCompress(@NotNull BlockCompressorPlugin plugin) {
        super(plugin, "compress");
        this.plugin = plugin;
    }

    @Override
    protected @NotNull List<String> onTabComplete(@NotNull Player player, String @NotNull [] args) {
        return Collections.emptyList();
    }

    @Override
    protected boolean execute(@NotNull Player player, String @NotNull [] args) {
        ToolManager toolManager = getToolManager();
        PlayerInventory playerInventory = player.getInventory();

        boolean success = toolManager.compress(player.getLocation(), playerInventory);
        String messagePath = ("compress-" + (success ? "successful" : "failure"));
        sendMessage(player, messagePath);

        player.updateInventory();
        return true;
    }

    private @NotNull BlockCompressorPlugin getBlockCompressorPlugin() {
        return this.plugin;
    }

    private @NotNull ToolManager getToolManager() {
        BlockCompressorPlugin plugin = getBlockCompressorPlugin();
        return plugin.getToolManager();
    }
}
