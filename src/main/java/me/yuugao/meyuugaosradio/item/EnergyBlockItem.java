package me.yuugao.meyuugaosradio.item;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.List;

public class EnergyBlockItem extends BlockItem {
    private final EnergyItemHandler energyItemHandler;

    public EnergyBlockItem(Block block, Settings settings, long capacity, int usage) {
        super(block, settings);

        this.energyItemHandler = new EnergyItemHandler(capacity, usage);
    }

    @Override
    public ItemStack getDefaultStack() {
        ItemStack stack = super.getDefaultStack();
        energyItemHandler.setupEnergyComponents(stack);

        return stack;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        energyItemHandler.appendTooltip(stack, tooltip);
    }
}