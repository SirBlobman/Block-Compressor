package com.github.sirblobman.block.compressor.command;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.bukkit.command.CommandSender;

import com.github.sirblobman.api.command.Command;
import com.github.sirblobman.api.language.LanguageManager;
import com.github.sirblobman.block.compressor.BlockCompressorPlugin;

import org.jetbrains.annotations.NotNull;

public final class CommandBlockCompressor extends Command {
    private final BlockCompressorPlugin plugin;

    public CommandBlockCompressor(BlockCompressorPlugin plugin) {
        super(plugin, "block-compressor");
        this.plugin = plugin;
    }

    @NotNull
    @Override
    protected LanguageManager getLanguageManager() {
        return this.plugin.getLanguageManager();
    }

    @Override
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        if(args.length == 1) {
            return Collections.singletonList("reload");
        }
        
        return Collections.emptyList();
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if(args.length < 1) {
            return false;
        }

        String sub = args[0].toLowerCase(Locale.US);
        if(!sub.equals("reload")) {
            return false;
        }
        
        this.plugin.reloadConfig();
        LanguageManager languageManager = getLanguageManager();
        languageManager.sendMessage(sender, "reload-success", null, true);
        return true;
    }
}
