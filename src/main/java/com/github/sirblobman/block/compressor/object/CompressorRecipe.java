package com.github.sirblobman.block.compressor.object;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bukkit.inventory.ItemStack;

import com.github.sirblobman.api.utility.ItemUtility;
import com.github.sirblobman.api.utility.Validate;
import com.github.sirblobman.api.xseries.XMaterial;

public final class CompressorRecipe {
    private final XMaterial input, output;
    private final int amount;
    public CompressorRecipe(XMaterial input, XMaterial output, int amount) {
        this.input = Validate.notNull(input, "input must not be null!");
        this.output = Validate.notNull(output, "output must not be null!");
        this.amount = amount;
    }

    @Override
    public boolean equals(Object object) {
        if(!(object instanceof CompressorRecipe)) return false;
        CompressorRecipe other = (CompressorRecipe) object;
        return (this.input == other.input && this.output == other.output && this.amount == other.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.input, this.output, this.amount);
    }

    @Override
    public String toString() {
        return String.format("%s x %s = %s", this.amount, this.input.name(), this.output.name());
    }

    public XMaterial getInput() {
        return this.input;
    }

    public XMaterial getOutput() {
        return this.output;
    }

    public int getAmount() {
        return this.amount;
    }

    public ItemStack[] convert(int inputAmount) {
        int amount = getAmount();
        int blockAmount = (inputAmount / amount);
        int oreLeftAmount = (inputAmount % amount);

        XMaterial input = getInput();
        XMaterial output = getOutput();
        List<ItemStack> itemList = new ArrayList<>();

        while(blockAmount > 0) {
            int currentAmount = Math.min(blockAmount, 64);
            blockAmount -= currentAmount;

            ItemStack item = output.parseItem();
            if(ItemUtility.isAir(item)) continue;

            item.setAmount(currentAmount);
            itemList.add(item);
        }

        while(oreLeftAmount > 0) {
            int currentAmount = Math.min(oreLeftAmount, 64);
            oreLeftAmount -= currentAmount;

            ItemStack item = input.parseItem();
            if(item == null) continue;

            item.setAmount(currentAmount);
            itemList.add(item);
        }

        return itemList.toArray(new ItemStack[0]);
    }
}