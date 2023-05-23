package com.github.sirblobman.block.compressor.command;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.github.sirblobman.api.command.Command;
import com.github.sirblobman.api.language.replacer.Replacer;
import com.github.sirblobman.api.language.replacer.StringReplacer;
import com.github.sirblobman.block.compressor.BlockCompressorPlugin;
import com.github.sirblobman.block.compressor.tool.Tool;
import com.github.sirblobman.block.compressor.tool.ToolManager;

public final class CommandCompressTool extends Command {
    private final BlockCompressorPlugin plugin;

    public CommandCompressTool(@NotNull BlockCompressorPlugin plugin) {
        super(plugin, "compress-tool");
        this.plugin = plugin;
    }

    @Override
    protected @NotNull List<String> onTabComplete(@NotNull CommandSender sender, String @NotNull [] args) {
        if (args.length == 1) {
            ToolManager toolManager = getToolManager();
            Map<String, Tool> toolMap = toolManager.getTools();
            Set<String> valueSet = toolMap.keySet();
            return getMatching(args[0], valueSet);
        }

        if (args.length == 2) {
            Set<String> valueSet = getOnlinePlayerNames();
            return getMatching(args[1], valueSet);
        }

        return Collections.emptyList();
    }

    @Override
    protected boolean execute(@NotNull CommandSender sender, String @NotNull [] args) {
        if (args.length < 1) {
            return false;
        }

        String toolId = args[0];
        ToolManager toolManager = getToolManager();
        Tool tool = toolManager.getTool(toolId);
        if (tool == null) {
            Replacer replacer = new StringReplacer("{id}", toolId);
            sendMessage(sender, "error.invalid-tool", replacer);
            return true;
        }

        Player target;
        if (args.length > 1) {
            target = findTarget(sender, args[1]);
            if (target == null) {
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sendMessage(sender, "error.missing-target");
            return true;
        }

        BlockCompressorPlugin plugin = getBlockCompressorPlugin();
        ItemStack item = tool.getTool(plugin);

        if (!tool.isInfiniteDurability()) {
            int durability = tool.getDurability();
            item = tool.fixDescription(plugin, item, durability);
        }

        giveItems(target, item);
        Replacer replacer = new StringReplacer("{target}", target.getName());
        sendMessage(sender, "tool-give", replacer);
        sendMessage(target, "tool-get");
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
