package me.yuugao.meyuugaosradio.item;

import net.minecraft.block.Block;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class EnergyBlockItem extends BlockItem {
    private final EnergyItemHandler energyHandler;

    public EnergyBlockItem(Block block, Settings settings, long capacity, int usage) {
        super(block, settings);

        this.energyHandler = new EnergyItemHandler(capacity, usage);
    }

    @Override
    public ItemStack getDefaultStack() {
        ItemStack stack = super.getDefaultStack();
        energyHandler.setupEnergyComponents(stack);

        return stack;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context,
                              TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        long energy = energyHandler.getEnergy(stack);
        long capacity = energyHandler.getCapacity(stack);
        int usage = energyHandler.getUsage(stack);

        textConsumer.accept(energyHandler.createEnergyText(energy, capacity));
        textConsumer.accept(energyHandler.createUsageText(usage));
    }
}