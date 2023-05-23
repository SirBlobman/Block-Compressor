package com.github.sirblobman.block.compressor.tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.bukkit.inventory.ItemStack;

import com.github.sirblobman.api.item.ItemBuilder;
import com.github.sirblobman.api.language.ComponentHelper;
import com.github.sirblobman.api.language.LanguageManager;
import com.github.sirblobman.api.nbt.CustomNbtContainer;
import com.github.sirblobman.api.nbt.CustomNbtTypes;
import com.github.sirblobman.api.nms.ItemHandler;
import com.github.sirblobman.api.utility.Validate;
import com.github.sirblobman.block.compressor.BlockCompressorPlugin;
import com.github.sirblobman.api.shaded.adventure.text.Component;
import com.github.sirblobman.api.shaded.adventure.text.TextReplacementConfig;
import com.github.sirblobman.api.shaded.adventure.text.minimessage.MiniMessage;
import com.github.sirblobman.api.shaded.xseries.XMaterial;

public final class Tool {
    private final String id;
    private XMaterial material;
    private boolean infiniteDurability;
    private int durability;
    private String displayNameString;
    private final List<String> loreStringList;
    private Integer model;
    private boolean glowing;

    private transient Component displayName;
    private transient List<Component> loreList;

    public Tool(@NotNull String id) {
        this.id = Validate.notEmpty(id, "id must not be empty!");
        this.material = XMaterial.GOLDEN_HOE;
        this.durability = 100;
        this.infiniteDurability = false;
        this.displayNameString = "Compressor Tool";
        this.loreStringList = new ArrayList<>();
        this.model = null;
        this.glowing = false;

        this.displayName = null;
        this.loreList = null;
    }

    public @NotNull String getId() {
        return this.id;
    }

    public @NotNull XMaterial getMaterial() {
        return this.material;
    }

    public void setMaterial(@NotNull XMaterial material) {
        this.material = material;
    }

    public boolean isInfiniteDurability() {
        return this.infiniteDurability;
    }

    public void setInfiniteDurability(boolean infiniteDurability) {
        this.infiniteDurability = infiniteDurability;
    }

    public int getDurability() {
        if (isInfiniteDurability()) {
            return Integer.MAX_VALUE;
        }

        return this.durability;
    }

    public void setDurability(int durability) {
        this.durability = durability;
    }

    public @Nullable String getDisplayNameString() {
        return this.displayNameString;
    }

    public void setDisplayNameString(@Nullable String displayNameString) {
        this.displayNameString = displayNameString;
        this.displayName = null;
    }

    public @Nullable Component getDisplayName(@NotNull BlockCompressorPlugin plugin) {
        if (this.displayName != null) {
            return this.displayName;
        }

        String displayNameString = getDisplayNameString();
        if (displayNameString == null) {
            return null;
        }

        LanguageManager languageManager = plugin.getLanguageManager();
        MiniMessage miniMessage = languageManager.getMiniMessage();
        this.displayName = miniMessage.deserialize(displayNameString);
        return this.displayName;
    }

    public @Nullable Component getDisplayName(@NotNull BlockCompressorPlugin plugin, int durability) {
        Component displayName = getDisplayName(plugin);
        if (displayName == null) {
            return null;
        }

        int maxDurability = getDurability();
        TextReplacementConfig current = TextReplacementConfig.builder().matchLiteral("{current_durability}")
                .replacement(Component.text(durability)).build();
        TextReplacementConfig max = TextReplacementConfig.builder().matchLiteral("{max_durability}")
                .replacement(Component.text(maxDurability)).build();
        return displayName.replaceText(current).replaceText(max);
    }

    public @NotNull List<String> getLoreStringList() {
        return Collections.unmodifiableList(this.loreStringList);
    }

    public void setLoreStringList(@NotNull List<String> loreStringList) {
        this.loreStringList.clear();
        this.loreStringList.addAll(loreStringList);
        this.loreList = null;
    }

    public @NotNull List<Component> getLore(@NotNull BlockCompressorPlugin plugin) {
        if (this.loreList != null) {
            return this.loreList;
        }

        List<String> loreStringList = getLoreStringList();
        if (loreStringList.isEmpty()) {
            return Collections.emptyList();
        }

        LanguageManager languageManager = plugin.getLanguageManager();
        MiniMessage miniMessage = languageManager.getMiniMessage();
        List<Component> loreList = new ArrayList<>();

        for (String lineString : loreStringList) {
            Component line = miniMessage.deserialize(lineString);
            loreList.add(line);
        }

        this.loreList = Collections.unmodifiableList(loreList);
        return this.loreList;
    }

    public @NotNull List<Component> getLore(@NotNull BlockCompressorPlugin plugin, int durability) {
        List<Component> loreList = getLore(plugin);
        if (loreList.isEmpty()) {
            return Collections.emptyList();
        }

        int maxDurability = getDurability();
        TextReplacementConfig current = TextReplacementConfig.builder().matchLiteral("{current_durability}")
                .replacement(Component.text(durability)).build();
        TextReplacementConfig max = TextReplacementConfig.builder().matchLiteral("{max_durability}")
                .replacement(Component.text(maxDurability)).build();

        List<Component> replacedLore = new ArrayList<>();
        for (Component component : loreList) {
            Component replaced = component.replaceText(current).replaceText(max);
            replacedLore.add(replaced);
        }

        return Collections.unmodifiableList(replacedLore);
    }

    public @Nullable Integer getModel() {
        return this.model;
    }

    public void setModel(@Nullable Integer model) {
        this.model = model;
    }

    public boolean isGlowing() {
        return this.glowing;
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }

    public @NotNull ItemStack getTool(@NotNull BlockCompressorPlugin plugin) {
        XMaterial material = getMaterial();
        ItemBuilder builder = new ItemBuilder(material).withAmount(1);

        ItemHandler itemHandler = plugin.getMultiVersionHandler().getItemHandler();

        Component displayName = getDisplayName(plugin);
        if (displayName != null) {
            builder.withName(itemHandler, ComponentHelper.wrapNoItalics(displayName));
        }

        builder.withLore(itemHandler, ComponentHelper.wrapNoItalics(getLore(plugin)));
        builder.withModel(getModel());

        if (isGlowing()) {
            builder.withGlowing();
        }

        ItemStack item = builder.build();
        CustomNbtContainer customNbt = itemHandler.getCustomNbt(item);

        if (!isInfiniteDurability()) {
            int durability = getDurability();
            customNbt.set("durability", CustomNbtTypes.INTEGER, durability);
        }

        customNbt.set("compressor-tool", CustomNbtTypes.STRING, getId());
        return itemHandler.setCustomNbt(item, customNbt);
    }

    public @NotNull ItemStack fixDescription(@NotNull BlockCompressorPlugin plugin, @NotNull ItemStack stack,
                                             int durability) {
        ItemBuilder builder = new ItemBuilder(stack);
        ItemHandler itemHandler = plugin.getMultiVersionHandler().getItemHandler();

        Component displayName = getDisplayName(plugin, durability);
        if (displayName != null) {
            builder.withName(itemHandler, ComponentHelper.wrapNoItalics(displayName));
        }

        builder.withLore(itemHandler, ComponentHelper.wrapNoItalics(getLore(plugin, durability)));
        return builder.build();
    }
}
