package com.github.sirblobman.block.compressor.command;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import org.bukkit.command.CommandSender;

import com.github.sirblobman.api.command.Command;
import com.github.sirblobman.block.compressor.BlockCompressorPlugin;
import com.github.sirblobman.block.compressor.tool.ToolManager;

public final class CommandBlockCompressor extends Command {
    private final BlockCompressorPlugin plugin;

    public CommandBlockCompressor(@NotNull BlockCompressorPlugin plugin) {
        super(plugin, "block-compressor");
        this.plugin = plugin;
    }

    @Override
    protected @NotNull List<String> onTabComplete(@NotNull CommandSender sender, String @NotNull [] args) {
        if (args.length == 1) {
            return Collections.singletonList("reload");
        }

        return Collections.emptyList();
    }

    @Override
    protected boolean execute(@NotNull CommandSender sender, String @NotNull [] args) {
        BlockCompressorPlugin plugin = getBlockCompressorPlugin();
        plugin.reloadConfig();

        ToolManager toolManager = plugin.getToolManager();
        if (toolManager.getTools().isEmpty()) {
            sendMessage(sender, "invalid-configuration");
        } else {
            sendMessage(sender, "reload-success");
        }

        return true;
    }

    private @NotNull BlockCompressorPlugin getBlockCompressorPlugin() {
        return this.plugin;
    }
}
