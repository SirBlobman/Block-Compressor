package com.github.sirblobman.block.compressor.command;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.github.sirblobman.api.command.Command;
import com.github.sirblobman.api.language.Replacer;
import com.github.sirblobman.api.language.SimpleReplacer;
import com.github.sirblobman.api.utility.ItemUtility;
import com.github.sirblobman.block.compressor.BlockCompressorPlugin;
import com.github.sirblobman.block.compressor.manager.ToolManager;

public final class CommandCompressTool extends Command {
    private final BlockCompressorPlugin plugin;

    public CommandCompressTool(BlockCompressorPlugin plugin) {
        super(plugin, "compress-tool");
        setPermissionName("block.compressor.command.compress-tool");
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            Set<String> valueSet = getOnlinePlayerNames();
            return getMatching(args[0], valueSet);
        }

        return Collections.emptyList();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0 && !(sender instanceof Player)) {
            return false;
        }

        String targetName = (args.length > 0 ? args[0] : sender.getName());
        Player target = findTarget(sender, targetName);
        if (target == null) {
            return true;
        }

        ToolManager toolManager = this.plugin.getToolManager();
        ItemStack compressorTool = toolManager.createCompressorTool(target);
        if (ItemUtility.isAir(compressorTool)) {
            sendMessage(sender, "invalid-configuration", null);
            return true;
        }

        giveItems(target, compressorTool);
        String realTargetName = target.getName();

        Replacer replacer = new SimpleReplacer("{target}", realTargetName);
        sendMessage(sender, "tool-give", replacer);
        sendMessage(target, "tool-get", null);
        return true;
    }
}
