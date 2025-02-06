package com.github.sirblobman.block.compressor.recipe;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import org.bukkit.inventory.ItemStack;

import com.github.sirblobman.api.utility.ItemUtility;
import com.github.sirblobman.api.utility.Validate;
import com.github.sirblobman.block.compressor.recipe.ingredient.Ingredient;

public record Recipe(String id, Ingredient input, Ingredient output) {
    public Recipe(@NotNull String id, @NotNull Ingredient input, @NotNull Ingredient output) {
        this.id = Validate.notEmpty(id, "id must not be empty!");
        this.input = input;
        this.output = output;
    }

    @Override
    public @NotNull String id() {
        return this.id;
    }

    @Override
    public @NotNull Ingredient input() {
        return this.input;
    }

    @Override
    public @NotNull Ingredient output() {
        return this.output;
    }

    public ItemStack @NotNull [] convert(int inputCount) {
        Ingredient input = input();
        Ingredient output = output();
        int amount = input.getQuantity();

        int outputCount = (inputCount / amount);
        int extraCount = (inputCount % amount);
        List<ItemStack> itemList = new ArrayList<>();

        while (outputCount > 0) {
            int currentAmount = Math.min(outputCount, 64);
            outputCount -= currentAmount;

            ItemStack item = output.getItem(currentAmount);
            if (ItemUtility.isAir(item)) {
                continue;
            }

            itemList.add(item);
        }

        while (extraCount > 0) {
            int currentAmount = Math.min(extraCount, 64);
            extraCount -= currentAmount;

            ItemStack item = input.getItem(currentAmount);
            if (ItemUtility.isAir(item)) {
                continue;
            }

            itemList.add(item);
        }

        return itemList.toArray(new ItemStack[0]);
    }
}
