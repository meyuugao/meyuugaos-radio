package me.yuugao.meyuugaosradio.item;

import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EnergyBlockItem extends BlockItem {
    private final EnergyItemHandler energyItemHandler;

    public EnergyBlockItem(Block block, Settings settings, long capacity, long usage) {
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
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        energyItemHandler.appendTooltip(stack, tooltip);
    }
}