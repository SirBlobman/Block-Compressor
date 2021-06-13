package com.github.sirblobman.block.compressor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.sirblobman.api.configuration.ConfigurationManager;
import com.github.sirblobman.api.core.CorePlugin;
import com.github.sirblobman.api.item.ItemBuilder;
import com.github.sirblobman.api.language.LanguageManager;
import com.github.sirblobman.api.nms.ItemHandler;
import com.github.sirblobman.api.nms.MultiVersionHandler;
import com.github.sirblobman.api.plugin.ConfigurablePlugin;
import com.github.sirblobman.api.update.UpdateManager;
import com.github.sirblobman.api.utility.ItemUtility;
import com.github.sirblobman.api.utility.MessageUtility;
import com.github.sirblobman.api.xseries.XMaterial;
import com.github.sirblobman.block.compressor.command.CommandBlockCompressor;
import com.github.sirblobman.block.compressor.command.CommandCompress;
import com.github.sirblobman.block.compressor.command.CommandCompressTool;
import com.github.sirblobman.block.compressor.listener.ListenerCompressorTool;
import com.github.sirblobman.block.compressor.manager.CompressorRecipeManager;

public final class BlockCompressorPlugin extends ConfigurablePlugin {
    private final CompressorRecipeManager compressorRecipeManager;

    public BlockCompressorPlugin() {
        this.compressorRecipeManager = new CompressorRecipeManager(this);
    }

    @Override
    public void onLoad() {
        ConfigurationManager configurationManager = getConfigurationManager();
        configurationManager.saveDefault("config.yml");

        LanguageManager languageManager = getLanguageManager();
        languageManager.saveDefaultLanguages();
        languageManager.reloadLanguages();
    }

    @Override
    public void onEnable() {
        CompressorRecipeManager compressorRecipeManager = getCompressorRecipeManager();
        compressorRecipeManager.reloadRecipes();

        new CommandBlockCompressor(this).register();
        new CommandCompress(this).register();
        new CommandCompressTool(this).register();
        new ListenerCompressorTool(this).register();

        CorePlugin corePlugin = JavaPlugin.getPlugin(CorePlugin.class);
        UpdateManager updateManager = corePlugin.getUpdateManager();
        updateManager.addResource(this, 88448L);
    }

    @Override
    public void onDisable() {
        // Do Nothing
    }

    public CompressorRecipeManager getCompressorRecipeManager() {
        return this.compressorRecipeManager;
    }

    public ItemStack getCompressorTool() {
        YamlConfiguration configuration = getConfig();
        ConfigurationSection section = configuration.getConfigurationSection("compressor-tool");
        if(section == null) return null;

        String materialName = section.getString("material");
        if(materialName == null) materialName = "DIAMOND_HOE";

        Optional<XMaterial> optionalMaterial = XMaterial.matchXMaterial(materialName);
        XMaterial material = optionalMaterial.orElse(XMaterial.DIAMOND_HOE);
        ItemBuilder builder = new ItemBuilder(material).withAmount(1).withFlags(ItemFlag.values());

        String displayName = section.getString("display-name");
        if(displayName != null && !displayName.isEmpty()) {
            String displayNameColored = MessageUtility.color(displayName);
            builder.withName(displayNameColored);
        }

        List<String> loreList = section.getStringList("lore");
        if(!loreList.isEmpty()) {
            List<String> loreListColored = MessageUtility.colorList(loreList);
            builder.withLore(loreListColored);
        }

        if(section.isInt("model")) {
            int model = section.getInt("model");
            builder.withModel(model);
        }

        ItemStack item = builder.build();
        MultiVersionHandler multiVersionHandler = getMultiVersionHandler();
        ItemHandler itemHandler = multiVersionHandler.getItemHandler();
        item = itemHandler.setCustomNBT(item, "compressor-tool", "yes");

        int durability = section.getInt("durability", -1);
        if(durability < 1 || durability > 2_000_000_000) {
            item = itemHandler.setCustomNBT(item, "max-durability", "infinite");
        } else {
            item = itemHandler.setCustomNBT(item, "durability", Integer.toString(durability));
            item = itemHandler.setCustomNBT(item, "max-durability", Integer.toString(durability));
        }

        updateDurability(item);
        return item;
    }

    public boolean isCompressorTool(ItemStack item) {
        if(ItemUtility.isAir(item)) return false;

        MultiVersionHandler multiVersionHandler = getMultiVersionHandler();
        ItemHandler itemHandler = multiVersionHandler.getItemHandler();

        String value = itemHandler.getCustomNBT(item, "compressor-tool", "no");
        return (value != null && value.equals("yes"));
    }

    public boolean hasDurability(ItemStack item) {
        if(ItemUtility.isAir(item)) return false;
        MultiVersionHandler multiVersionHandler = getMultiVersionHandler();
        ItemHandler itemHandler = multiVersionHandler.getItemHandler();

        String customNBT = itemHandler.getCustomNBT(item, "durability", "N/A");
        return (customNBT != null && !customNBT.isEmpty() && !customNBT.equals("N/A"));
    }

    public int getDurability(ItemStack item) {
        if(!hasDurability(item)) return -1;
        MultiVersionHandler multiVersionHandler = getMultiVersionHandler();
        ItemHandler itemHandler = multiVersionHandler.getItemHandler();

        String customNBT = itemHandler.getCustomNBT(item, "durability", "N/A");
        return Integer.parseInt(customNBT);
    }

    public int getMaxDurability(ItemStack item) {
        if(!hasDurability(item)) return -1;
        MultiVersionHandler multiVersionHandler = getMultiVersionHandler();
        ItemHandler itemHandler = multiVersionHandler.getItemHandler();

        String customNBT = itemHandler.getCustomNBT(item, "max-durability", "N/A");
        if(customNBT.equals("infinity")) return -1;
        return Integer.parseInt(customNBT);
    }

    public ItemStack decreaseDurability(ItemStack item) {
        if(!hasDurability(item)) return item;

        int durability = getDurability(item);
        if(durability <= 0) return item;

        MultiVersionHandler multiVersionHandler = getMultiVersionHandler();
        ItemHandler itemHandler = multiVersionHandler.getItemHandler();
        return itemHandler.setCustomNBT(item, "durability", Integer.toString(durability - 1));
    }

    public void updateDurability(ItemStack item) {
        if(ItemUtility.isAir(item)) return;
        YamlConfiguration configuration = getConfig();
        ConfigurationSection section = configuration.getConfigurationSection("compressor-tool");

        List<String> loreList = section.getStringList("lore");
        List<String> realLore = new ArrayList<>();
        boolean hasDurability = hasDurability(item);

        for(String line : loreList) {
            if(line.contains("{durability}")) {
                String formatPath = (hasDurability ? "durability-normal" : "durability-infinite");
                String format = configuration.getString("lore-format." + formatPath);
                if(hasDurability) {
                    int durability = getDurability(item);
                    int maxDurability = getMaxDurability(item);
                    format = format.replace("{current}", Integer.toString(durability))
                            .replace("{max}", Integer.toString(maxDurability));
                }

                line = line.replace("{durability}", format);
            }

            line = MessageUtility.color(line);
            realLore.add(line);
        }

        ItemMeta meta = item.getItemMeta();
        meta.setLore(realLore);
        item.setItemMeta(meta);
    }
}
