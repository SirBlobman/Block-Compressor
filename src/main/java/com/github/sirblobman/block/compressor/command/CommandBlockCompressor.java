package com.github.sirblobman.block.compressor.command;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.bukkit.command.CommandSender;

import com.github.sirblobman.api.command.Command;
import com.github.sirblobman.api.configuration.ConfigurationManager;
import com.github.sirblobman.api.language.LanguageManager;
import com.github.sirblobman.block.compressor.BlockCompressorPlugin;
import com.github.sirblobman.block.compressor.manager.CompressorRecipeManager;

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
        return (args.length == 1 ? Collections.singletonList("reload") : Collections.emptyList());
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if(args.length < 1) return false;

        String sub = args[0].toLowerCase(Locale.US);
        if(!sub.equals("reload")) return false;

        ConfigurationManager configurationManager = this.plugin.getConfigurationManager();
        configurationManager.reload("config.yml");
        configurationManager.reload("language.yml");

        LanguageManager languageManager = getLanguageManager();
        languageManager.reloadLanguages();

        CompressorRecipeManager compressorRecipeManager = this.plugin.getCompressorRecipeManager();
        compressorRecipeManager.reloadRecipes();

        languageManager.sendMessage(sender, "reload-success", null, true);
        return true;
    }
}
