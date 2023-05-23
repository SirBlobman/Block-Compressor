package com.github.sirblobman.block.compressor.recipe.ingredient;

import org.jetbrains.annotations.NotNull;

import org.bukkit.inventory.ItemStack;

public final class CustomItemIngredient extends Ingredient {
    private final ItemStack stack;
    private final int quantity;

    public CustomItemIngredient(@NotNull ItemStack stack, int quantity) {
        this.stack = stack;
        this.quantity = quantity;
    }

    @Override
    public int getQuantity() {
        return this.quantity;
    }

    @Override
    public @NotNull IngredientType getType() {
        return IngredientType.CUSTOM_ITEM;
    }

    @Override
    public boolean matches(@NotNull ItemStack stack) {
        return this.stack.isSimilar(stack);
    }

    @Override
    public @NotNull ItemStack getItem(int amount) {
        ItemStack item = this.stack.clone();
        item.setAmount(amount);
        return item;
    }
}
