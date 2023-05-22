package com.github.sirblobman.block.compressor.command;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.NotNull;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.sirblobman.api.command.Command;
import com.github.sirblobman.block.compressor.BlockCompressorPlugin;

public final class CommandBlockCompressor extends Command {
    public CommandBlockCompressor(@NotNull BlockCompressorPlugin plugin) {
        super(plugin, "block-compressor");
        setPermissionName("block.compressor.command.block-compressor");
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
        if (args.length < 1) {
            return false;
        }

        String sub = args[0].toLowerCase(Locale.US);
        if (!sub.equals("reload")) {
            return false;
        }

        JavaPlugin plugin = getPlugin();
        plugin.reloadConfig();

        sendMessage(sender, "reload-success");
        return true;
    }
}
