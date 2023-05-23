package com.github.sirblobman.block.compressor.recipe.ingredient;

import org.jetbrains.annotations.NotNull;

import org.bukkit.inventory.ItemStack;

public abstract class Ingredient {
    public abstract int getQuantity();
    public abstract @NotNull IngredientType getType();
    public abstract boolean matches(@NotNull ItemStack stack);
    public abstract @NotNull ItemStack getItem(int amount);
}
