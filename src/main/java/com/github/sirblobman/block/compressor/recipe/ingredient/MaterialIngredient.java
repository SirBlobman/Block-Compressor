package com.github.sirblobman.block.compressor.recipe.ingredient;

import org.jetbrains.annotations.NotNull;

import org.bukkit.inventory.ItemStack;

import com.github.sirblobman.api.item.ItemBuilder;
import com.github.sirblobman.api.shaded.xseries.XMaterial;

public final class MaterialIngredient extends Ingredient {
    private final XMaterial material;
    private final int quantity;

    public MaterialIngredient(@NotNull XMaterial material, int quantity) {
        this.material = material;
        this.quantity = quantity;
    }

    @Override
    public int getQuantity() {
        return this.quantity;
    }

    @Override
    public @NotNull IngredientType getType() {
        return IngredientType.MATERIAL;
    }

    @Override
    public boolean matches(@NotNull ItemStack stack) {
        XMaterial material = XMaterial.matchXMaterial(stack);
        return (material == this.material);
    }

    @Override
    public @NotNull ItemStack getItem(int amount) {
        return new ItemBuilder(this.material).withAmount(amount).build();
    }
}
