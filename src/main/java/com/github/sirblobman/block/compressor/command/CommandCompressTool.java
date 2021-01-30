package com.github.sirblobman.block.compressor.command;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.github.sirblobman.api.command.Command;
import com.github.sirblobman.api.utility.ItemUtility;
import com.github.sirblobman.block.compressor.BlockCompressorPlugin;

public final class CommandCompressTool extends Command {
    private final BlockCompressorPlugin plugin;
    public CommandCompressTool(BlockCompressorPlugin plugin) {
        super(plugin, "compress-tool");
        this.plugin = plugin;
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
        String targetName = (args.length > 0 ? args[0] : sender.getName());
        Player target = Bukkit.getPlayer(targetName);
        if(target == null) {
            sender.sendMessage(ChatColor.RED + "Unknown Player: " + ChatColor.GRAY + targetName);
            return true;
        }

        ItemStack compressorTool = this.plugin.getCompressorTool();
        if(ItemUtility.isAir(compressorTool)) {
            sender.sendMessage(ChatColor.RED + "The compressor tool is misconfigured.");
            return true;
        }

        giveItems(target, compressorTool);
        this.plugin.sendMessage(sender, "tool-give", message -> message.replace("{target}", target.getName()));
        this.plugin.sendMessage(target, "tool-get");
        return true;
    }
}