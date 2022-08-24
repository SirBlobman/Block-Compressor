package com.github.sirblobman.block.compressor;

import org.bukkit.plugin.java.JavaPlugin;

import com.github.sirblobman.api.bstats.bukkit.Metrics;
import com.github.sirblobman.api.bstats.charts.SimplePie;
import com.github.sirblobman.api.configuration.ConfigurationManager;
import com.github.sirblobman.api.core.CorePlugin;
import com.github.sirblobman.api.language.Language;
import com.github.sirblobman.api.language.LanguageManager;
import com.github.sirblobman.api.plugin.ConfigurablePlugin;
import com.github.sirblobman.api.update.UpdateManager;
import com.github.sirblobman.block.compressor.command.CommandBlockCompressor;
import com.github.sirblobman.block.compressor.command.CommandCompress;
import com.github.sirblobman.block.compressor.command.CommandCompressTool;
import com.github.sirblobman.block.compressor.listener.ListenerCompressorTool;
import com.github.sirblobman.block.compressor.manager.CompressorRecipeManager;
import com.github.sirblobman.block.compressor.manager.ToolManager;

public final class BlockCompressorPlugin extends ConfigurablePlugin {
    private final CompressorRecipeManager compressorRecipeManager;
    private final ToolManager toolManager;

    public BlockCompressorPlugin() {
        this.compressorRecipeManager = new CompressorRecipeManager(this);
        this.toolManager = new ToolManager(this);
    }

    @Override
    public void onLoad() {
        ConfigurationManager configurationManager = getConfigurationManager();
        configurationManager.saveDefault("config.yml");

        LanguageManager languageManager = getLanguageManager();
        languageManager.saveDefaultLanguageFiles();
    }

    @Override
    public void onEnable() {
        reloadConfiguration();

        registerCommands();
        registerListeners();
        registerUpdateChecker();
        registerbStats();
    }

    @Override
    public void onDisable() {
        // Do Nothing
    }

    @Override
    protected void reloadConfiguration() {
        ConfigurationManager configurationManager = getConfigurationManager();
        configurationManager.reload("config.yml");

        LanguageManager languageManager = getLanguageManager();
        languageManager.reloadLanguageFiles();

        CompressorRecipeManager compressorRecipeManager = getCompressorRecipeManager();
        compressorRecipeManager.reloadRecipes();
    }

    public CompressorRecipeManager getCompressorRecipeManager() {
        return this.compressorRecipeManager;
    }

    public ToolManager getToolManager() {
        return this.toolManager;
    }

    private void registerCommands() {
        new CommandBlockCompressor(this).register();
        new CommandCompress(this).register();
        new CommandCompressTool(this).register();
    }

    private void registerListeners() {
        new ListenerCompressorTool(this).register();
    }

    private void registerUpdateChecker() {
        CorePlugin corePlugin = JavaPlugin.getPlugin(CorePlugin.class);
        UpdateManager updateManager = corePlugin.getUpdateManager();
        updateManager.addResource(this, 88448L);
    }

    private void registerbStats() {
        Metrics metrics = new Metrics(this, 16253);
        metrics.addCustomChart(new SimplePie("selected_language", this::getDefaultLanguageCode));
    }

    private String getDefaultLanguageCode() {
        LanguageManager languageManager = getLanguageManager();
        Language defaultLanguage = languageManager.getDefaultLanguage();
        return (defaultLanguage == null ? "none" : defaultLanguage.getLanguageCode());
    }
}
