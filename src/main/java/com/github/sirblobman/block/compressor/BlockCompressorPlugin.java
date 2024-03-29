package com.github.sirblobman.block.compressor;

import org.jetbrains.annotations.NotNull;

import org.bukkit.plugin.java.JavaPlugin;

import com.github.sirblobman.api.configuration.ConfigurationManager;
import com.github.sirblobman.api.core.CorePlugin;
import com.github.sirblobman.api.language.Language;
import com.github.sirblobman.api.language.LanguageManager;
import com.github.sirblobman.api.plugin.ConfigurablePlugin;
import com.github.sirblobman.api.update.SpigotUpdateManager;
import com.github.sirblobman.block.compressor.command.CommandBlockCompressor;
import com.github.sirblobman.block.compressor.command.CommandCompress;
import com.github.sirblobman.block.compressor.command.CommandCompressTool;
import com.github.sirblobman.block.compressor.recipe.RecipeManager;
import com.github.sirblobman.block.compressor.tool.ListenerTool;
import com.github.sirblobman.block.compressor.tool.ToolManager;
import com.github.sirblobman.api.shaded.bstats.bukkit.Metrics;
import com.github.sirblobman.api.shaded.bstats.charts.SimplePie;

public final class BlockCompressorPlugin extends ConfigurablePlugin {
    private final RecipeManager recipeManager;
    private final ToolManager toolManager;

    public BlockCompressorPlugin() {
        this.recipeManager = new RecipeManager(this);
        this.toolManager = new ToolManager(this);
    }

    @Override
    public void onLoad() {
        ConfigurationManager configurationManager = getConfigurationManager();
        configurationManager.saveDefault("config.yml");
        configurationManager.saveDefault("recipes.yml");
        configurationManager.saveDefault("tools.yml");

        LanguageManager languageManager = getLanguageManager();
        languageManager.saveDefaultLanguageFiles();
    }

    @Override
    public void onEnable() {
        reloadConfiguration();

        LanguageManager languageManager = getLanguageManager();
        languageManager.onPluginEnable();

        registerCommands();
        registerListeners();
        registerUpdateChecker();
        register_bStats();
    }

    @Override
    public void onDisable() {
        // Do Nothing
    }

    @Override
    protected void reloadConfiguration() {
        ConfigurationManager configurationManager = getConfigurationManager();
        configurationManager.reload("config.yml");
        configurationManager.reload("recipes.yml");
        configurationManager.reload("tools.yml");

        LanguageManager languageManager = getLanguageManager();
        languageManager.reloadLanguages();

        RecipeManager recipeManager = getRecipeManager();
        recipeManager.loadRecipes();

        ToolManager toolManager = getToolManager();
        toolManager.loadTools();
    }

    public @NotNull RecipeManager getRecipeManager() {
        return this.recipeManager;
    }

    public @NotNull ToolManager getToolManager() {
        return this.toolManager;
    }

    private void registerCommands() {
        new CommandBlockCompressor(this).register();
        new CommandCompress(this).register();
        new CommandCompressTool(this).register();
    }

    private void registerListeners() {
        new ListenerTool(this).register();
    }

    private void registerUpdateChecker() {
        CorePlugin corePlugin = JavaPlugin.getPlugin(CorePlugin.class);
        SpigotUpdateManager updateManager = corePlugin.getSpigotUpdateManager();
        updateManager.addResource(this, 88448L);
    }

    private void register_bStats() {
        Metrics metrics = new Metrics(this, 16253);
        metrics.addCustomChart(new SimplePie("selected_language", this::getDefaultLanguageCode));
    }

    private @NotNull String getDefaultLanguageCode() {
        LanguageManager languageManager = getLanguageManager();
        Language defaultLanguage = languageManager.getDefaultLanguage();
        return (defaultLanguage == null ? "none" : defaultLanguage.getLanguageName());
    }
}
