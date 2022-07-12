package com.github.sirblobman.block.compressor.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.github.sirblobman.api.item.ItemBuilder;
import com.github.sirblobman.api.nms.ItemHandler;
import com.github.sirblobman.api.nms.MultiVersionHandler;
import com.github.sirblobman.api.utility.ItemUtility;
import com.github.sirblobman.api.utility.MessageUtility;
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

    private YamlConfiguration getConfig() {
        BlockCompressorPlugin plugin = getPlugin();
        return plugin.getConfig();
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
    public ItemStack createCompressorTool() {
        YamlConfiguration configuration = getConfig();
        ConfigurationSection section = configuration.getConfigurationSection("compressor-tool");
        if (section == null) {
            return null;
        }

        String materialName = section.getString("material", "DIAMOND_HOE");
        Optional<XMaterial> optionalMaterial = XMaterial.matchXMaterial(materialName);
        XMaterial material = optionalMaterial.orElse(XMaterial.DIAMOND_HOE);
        ItemBuilder builder = new ItemBuilder(material).withAmount(1).withFlags(ItemFlag.values());

        String displayName = section.getString("display-name");
        if (displayName != null && !displayName.isEmpty()) {
            String displayNameColored = MessageUtility.color(displayName);
            builder.withName(displayNameColored);
        }

        List<String> loreList = section.getStringList("lore");
        if (!loreList.isEmpty()) {
            List<String> loreListColored = MessageUtility.colorList(loreList);
            builder.withLore(loreListColored);
        }

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

        updateDurability(item);
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

    public void updateDurability(ItemStack item) {
        if (ItemUtility.isAir(item)) {
            return;
        }

        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            return;
        }

        String displayName = getDisplayNameFormatted(item);
        List<String> lore = getLoreFormatted(item);

        itemMeta.setDisplayName(displayName);
        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);
    }

    private String getDisplayNameFormatted(ItemStack item) {
        YamlConfiguration configuration = getConfig();
        ConfigurationSection section = configuration.getConfigurationSection("compressor-tool");
        String configDisplayName = section.getString("display-name");

        if (!configDisplayName.contains("{durability}")) {
            return MessageUtility.color(configDisplayName);
        }


        boolean hasDurability = hasDurability(item);
        String formatPath = (hasDurability ? "durability-normal" : "durability-infinite");
        String fullFormatPath = ("display-name-format." + formatPath);
        String displayNameFormat = configuration.getString(fullFormatPath);

        if (hasDurability) {
            int maxDurability = getMaxDurability(item);
            int durability = getDurability(item);
            String durabilityString = Integer.toString(durability);
            String maxDurabilityString = Integer.toString(maxDurability);
            displayNameFormat = displayNameFormat.replace("{current}", durabilityString)
                    .replace("{max}", maxDurabilityString);
        }

        String displayName = configDisplayName.replace("{durability}", displayNameFormat);
        return MessageUtility.color(displayName);
    }

    private List<String> getLoreFormatted(ItemStack item) {
        YamlConfiguration configuration = getConfig();
        ConfigurationSection section = configuration.getConfigurationSection("compressor-tool");
        List<String> configLore = section.getStringList("lore");
        List<String> lore = new ArrayList<>();


        boolean hasDurability = hasDurability(item);
        String formatPath = (hasDurability ? "durability-normal" : "durability-infinite");
        String fullFormatPath = ("display-name-format." + formatPath);
        String durabilityFormat = configuration.getString(fullFormatPath);

        if (hasDurability) {
            int maxDurability = getMaxDurability(item);
            int durability = getDurability(item);
            String durabilityString = Integer.toString(durability);
            String maxDurabilityString = Integer.toString(maxDurability);
            durabilityFormat = durabilityFormat.replace("{current}", durabilityString)
                    .replace("{max}", maxDurabilityString);
        }

        for (String line : configLore) {
            if (line.contains("{durability}")) {
                line = line.replace("{durability}", durabilityFormat);
            }

            lore.add(line);
        }

        return MessageUtility.colorList(lore);
    }
}
