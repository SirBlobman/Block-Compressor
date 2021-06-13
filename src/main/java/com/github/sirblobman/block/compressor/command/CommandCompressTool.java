package com.github.sirblobman.block.compressor.command;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.github.sirblobman.api.command.Command;
import com.github.sirblobman.api.language.LanguageManager;
import com.github.sirblobman.api.language.Replacer;
import com.github.sirblobman.api.utility.ItemUtility;
import com.github.sirblobman.block.compressor.BlockCompressorPlugin;

import org.jetbrains.annotations.NotNull;

public final class CommandCompressTool extends Command {
    private final BlockCompressorPlugin plugin;

    public CommandCompressTool(BlockCompressorPlugin plugin) {
        super(plugin, "compress-tool");
        this.plugin = plugin;
    }

    @NotNull
    @Override
    protected LanguageManager getLanguageManager() {
        return this.plugin.getLanguageManager();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if(args.length == 1) {
            Set<String> valueSet = getOnlinePlayerNames();
            return getMatching(valueSet, args[0]);
        }

        return Collections.emptyList();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if(args.length == 0 && !(sender instanceof Player)) return false;
        LanguageManager languageManager = getLanguageManager();

        String targetName = (args.length > 0 ? args[0] : sender.getName());
        Player target = findTarget(sender, targetName);
        if(target == null) return true;

        ItemStack compressorTool = this.plugin.getCompressorTool();
        if(ItemUtility.isAir(compressorTool)) {
            languageManager.sendMessage(sender, "invalid-configuration", null, true);
            return true;
        }

        giveItems(target, compressorTool);
        String realTargetName = target.getName();
        Replacer replacer = message -> message.replace("{target}", realTargetName);

        languageManager.sendMessage(sender, "tool-give", replacer, true);
        languageManager.sendMessage(target, "tool-get", null, true);
        return true;
    }
}
