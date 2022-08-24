package com.github.sirblobman.block.compressor.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.github.sirblobman.api.adventure.adventure.text.Component;
import com.github.sirblobman.api.adventure.adventure.text.minimessage.MiniMessage;
import com.github.sirblobman.api.item.ItemBuilder;
import com.github.sirblobman.api.language.ComponentHelper;
import com.github.sirblobman.api.language.LanguageManager;
import com.github.sirblobman.api.language.Replacer;
import com.github.sirblobman.api.language.SimpleReplacer;
import com.github.sirblobman.api.nms.ItemHandler;
import com.github.sirblobman.api.nms.MultiVersionHandler;
import com.github.sirblobman.api.utility.ItemUtility;
import com.github.sirblobman.api.utility.Validate;
import com.github.sirblobman.api.xseries.XMaterial;
import com.github.sirblobman.block.compressor.BlockCompressorPlugin;

import org.jetbrains.annotations.Nullable;

public final class ToolManager {
    private final BlockCompressorPlugin plugin;

    public ToolManager(BlockCompressorPlugin plugin) {
        this.plugin = Validate.notNull(plugin, "plugin must not be null!");
    }

    private BlockCompressorPlugin getPlugin() {
        return this.plugin;
    }

    private YamlConfiguration getConfiguration() {
        BlockCompressorPlugin plugin = getPlugin();
        return plugin.getConfig();
    }

    private LanguageManager getLanguageManager() {
        BlockCompressorPlugin plugin = getPlugin();
        return plugin.getLanguageManager();
    }

    private MultiVersionHandler getMultiVersionHandler() {
        BlockCompressorPlugin plugin = getPlugin();
        return plugin.getMultiVersionHandler();
    }

    private ItemHandler getItemHandler() {
        MultiVersionHandler multiVersionHandler = getMultiVersionHandler();
        return multiVersionHandler.getItemHandler();
    }

    @Nullable
    public ItemStack createCompressorTool(Player player) {
        if(player == null) {
            return null;
        }

        YamlConfiguration configuration = getConfiguration();
        ConfigurationSection section = configuration.getConfigurationSection("compressor-tool");
        if (section == null) {
            return null;
        }

        String materialName = section.getString("material", "DIAMOND_HOE");
        Optional<XMaterial> optionalMaterial = XMaterial.matchXMaterial(materialName);
        XMaterial material = optionalMaterial.orElse(XMaterial.DIAMOND_HOE);

        ItemBuilder builder = new ItemBuilder(material);
        builder.withFlags(ItemFlag.values());

        if (section.isInt("model")) {
            int model = section.getInt("model");
            builder.withModel(model);
        }

        ItemStack item = builder.build();
        ItemHandler itemHandler = getItemHandler();
        item = itemHandler.setCustomNBT(item, "compressor-tool", "yes");

        int durability = section.getInt("durability", -1);
        if (durability < 1 || durability > 2_000_000_000) {
            item = itemHandler.setCustomNBT(item, "max-durability", "infinite");
        } else {
            item = itemHandler.setCustomNBT(item, "durability", Integer.toString(durability));
            item = itemHandler.setCustomNBT(item, "max-durability", Integer.toString(durability));
        }

        item = updateDurability(player, item);
        return item;
    }

    public boolean isCompressorTool(ItemStack item) {
        if (ItemUtility.isAir(item)) {
            return false;
        }

        ItemHandler itemHandler = getItemHandler();
        String value = itemHandler.getCustomNBT(item, "compressor-tool", "no");
        return (value != null && value.equals("yes"));
    }

    public boolean hasDurability(ItemStack item) {
        if (ItemUtility.isAir(item)) {
            return false;
        }

        ItemHandler itemHandler = getItemHandler();
        String customNBT = itemHandler.getCustomNBT(item, "durability", "N/A");
        return (customNBT != null && !customNBT.isEmpty() && !customNBT.equals("N/A"));
    }

    public int getDurability(ItemStack item) {
        if (!hasDurability(item)) {
            return -1;
        }

        ItemHandler itemHandler = getItemHandler();
        String customNBT = itemHandler.getCustomNBT(item, "durability", "N/A");
        return Integer.parseInt(customNBT);
    }

    public int getMaxDurability(ItemStack item) {
        if (!hasDurability(item)) {
            return -1;
        }

        ItemHandler itemHandler = getItemHandler();
        String customNBT = itemHandler.getCustomNBT(item, "max-durability", "N/A");
        if (customNBT.equals("infinity")) {
            return -1;
        }

        return Integer.parseInt(customNBT);
    }

    public ItemStack decreaseDurability(ItemStack item) {
        if (!hasDurability(item)) {
            return item;
        }

        int durability = getDurability(item);
        if (durability <= 0) {
            return item;
        }

        ItemHandler itemHandler = getItemHandler();
        return itemHandler.setCustomNBT(item, "durability", Integer.toString(durability - 1));
    }

    public ItemStack updateDurability(Player player, ItemStack item) {
        Validate.notNull(player, "player must not be null!");
        if (ItemUtility.isAir(item)) {
            return item;
        }

        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return item;
        }

        ItemHandler itemHandler = getItemHandler();
        Component displayName = getDisplayName(player, item);
        List<Component> lore = getLore(player, item);

        item = itemHandler.setDisplayName(item, displayName);
        item = itemHandler.setLore(item, lore);
        return item;
    }

    private Component getDisplayName(Player player, ItemStack item) {
        LanguageManager languageManager = getLanguageManager();
        MiniMessage miniMessage = languageManager.getMiniMessage();

        boolean hasDurability = hasDurability(item);
        Replacer currentMaxReplacer = null;
        if(hasDurability) {
            int durability = getDurability(item);
            int maxDurability = getMaxDurability(item);
            String durabilityString = Integer.toString(durability);
            String maxDurabilityString = Integer.toString(maxDurability);
            currentMaxReplacer = message -> message.replace("{current}", durabilityString)
                    .replace("{max}", maxDurabilityString);
        }

        String durabilityFormatPathPart = (hasDurability ? "durability-normal" : "durability-infinite");
        String durabilityFormatPath = ("compressor-tool.display-name." + durabilityFormatPathPart);
        String durabilityFormat = languageManager.getMessageString(player, durabilityFormatPath, currentMaxReplacer);

        String fullPath = "compressor-tool.display-name.format";
        Replacer durabilityReplacer = new SimpleReplacer("{durability}", durabilityFormat);
        String displayNameString = languageManager.getMessageString(player, fullPath, durabilityReplacer);

        Component displayName = miniMessage.deserialize(displayNameString);
        return ComponentHelper.wrapNoItalics(displayName);
    }

    private List<Component> getLore(Player player, ItemStack item) {
        LanguageManager languageManager = getLanguageManager();
        MiniMessage miniMessage = languageManager.getMiniMessage();

        boolean hasDurability = hasDurability(item);
        Replacer currentMaxReplacer = null;
        if(hasDurability) {
            int durability = getDurability(item);
            int maxDurability = getMaxDurability(item);
            String durabilityString = Integer.toString(durability);
            String maxDurabilityString = Integer.toString(maxDurability);
            currentMaxReplacer = message -> message.replace("{current}", durabilityString)
                    .replace("{max}", maxDurabilityString);
        }

        String durabilityFormatPathPart = (hasDurability ? "durability-normal" : "durability-infinite");
        String durabilityFormatPath = ("compressor-tool.lore." + durabilityFormatPathPart);
        String durabilityFormat = languageManager.getMessageString(player, durabilityFormatPath, currentMaxReplacer);

        String fullPath = "compressor-tool.lore.format";
        Replacer durabilityReplacer = new SimpleReplacer("{durability}", durabilityFormat);
        String loreString = languageManager.getMessageString(player, fullPath, durabilityReplacer);
        String[] loreSplit = loreString.split(Pattern.quote("\n"));

        List<Component> lore = new ArrayList<>();
        for (String lineString : loreSplit) {
            Component line = miniMessage.deserialize(lineString);
            Component noItalics = ComponentHelper.wrapNoItalics(line);
            lore.add(noItalics);
        }

        return lore;
    }
}
