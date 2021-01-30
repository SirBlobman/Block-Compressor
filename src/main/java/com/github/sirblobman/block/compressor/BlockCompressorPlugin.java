package com.github.sirblobman.block.compressor;

import java.util.List;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.sirblobman.api.configuration.ConfigurationManager;
import com.github.sirblobman.api.item.ItemBuilder;
import com.github.sirblobman.api.language.Replacer;
import com.github.sirblobman.api.nms.ItemHandler;
import com.github.sirblobman.api.nms.MultiVersionHandler;
import com.github.sirblobman.api.utility.ItemUtility;
import com.github.sirblobman.api.utility.MessageUtility;
import com.github.sirblobman.api.xseries.XMaterial;
import com.github.sirblobman.block.compressor.command.CommandCompress;
import com.github.sirblobman.block.compressor.command.CommandCompressTool;
import com.github.sirblobman.block.compressor.listener.ListenerCompressorTool;
import com.github.sirblobman.block.compressor.manager.CompressorRecipeManager;

public final class BlockCompressorPlugin extends JavaPlugin {
    private final MultiVersionHandler multiVersionHandler;
    private final ConfigurationManager configurationManager;
    private final CompressorRecipeManager compressorRecipeManager;
    public BlockCompressorPlugin() {
        this.multiVersionHandler = new MultiVersionHandler(this);
        this.configurationManager = new ConfigurationManager(this);
        this.compressorRecipeManager = new CompressorRecipeManager(this);
    }

    @Override
    public void onLoad() {
        saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        CompressorRecipeManager compressorRecipeManager = getCompressorRecipeManager();
        compressorRecipeManager.reloadRecipes();

        new CommandCompress(this).register();
        new CommandCompressTool(this).register();

        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new ListenerCompressorTool(this), this);
    }

    @Override
    public void onDisable() {
        // Do Nothing
    }

    @Override
    public void saveDefaultConfig() {
        ConfigurationManager configurationManager = getConfigurationManager();
        configurationManager.saveDefault("config.yml");
    }

    @Override
    public void saveConfig() {
        ConfigurationManager configurationManager = getConfigurationManager();
        configurationManager.save("config.yml");
    }

    @Override
    public void reloadConfig() {
        ConfigurationManager configurationManager = getConfigurationManager();
        configurationManager.reload("config.yml");

        CompressorRecipeManager compressorRecipeManager = getCompressorRecipeManager();
        compressorRecipeManager.reloadRecipes();
    }

    @Override
    public YamlConfiguration getConfig() {
        ConfigurationManager configurationManager = getConfigurationManager();
        return configurationManager.get("config.yml");
    }

    public ConfigurationManager getConfigurationManager() {
        return this.configurationManager;
    }

    public MultiVersionHandler getMultiVersionHandler() {
        return this.multiVersionHandler;
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
        return itemHandler.setCustomNBT(item, "compressor-tool", "yes");
    }

    public boolean isCompressorTool(ItemStack item) {
        if(ItemUtility.isAir(item)) return false;

        MultiVersionHandler multiVersionHandler = getMultiVersionHandler();
        ItemHandler itemHandler = multiVersionHandler.getItemHandler();

        String value = itemHandler.getCustomNBT(item, "compressor-tool", "no");
        return (value != null && value.equals("yes"));
    }

    public void sendMessage(CommandSender sender, String path, Replacer... replacers) {
        String message = getMessage(path);
        if(message == null || message.isEmpty()) return;
        for(Replacer replacer : replacers) message = replacer.replace(message);
        sender.sendMessage(message);
    }

    private String getMessage(String path) {
        String messagePath = ("language." + path);
        YamlConfiguration configuration = getConfig();
        if(configuration.isList(messagePath)) {
            List<String> messageList = configuration.getStringList(messagePath);
            List<String> coloredList = MessageUtility.colorList(messageList);
            return String.join("\n", coloredList);
        }

        if(configuration.isString(messagePath)) {
            String message = configuration.getString(messagePath);
            if(message != null) return MessageUtility.color(message);
        }

        return String.format("{%s}", messagePath);
    }
}