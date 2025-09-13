package me.yuugao.meyuugaosradio.item;

import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

public class EnergyBlockItem extends BlockItem {
    private final long capacity;
    private final int usage;

    public EnergyBlockItem(Block block, Settings settings, long capacity, int usage) {
        super(block, settings);

        this.capacity = capacity;
        this.usage = usage;
    }

    @Override
    public ItemStack getDefaultStack() {
        ItemStack stack = super.getDefaultStack();
        setupEnergyComponents(stack);

        return stack;
    }

    private void setupEnergyComponents(ItemStack stack) {
        NbtCompound energyData = new NbtCompound();
        energyData.putLong("Energy", 0);
        energyData.putLong("Capacity", this.capacity);
        energyData.putInt("Usage", this.usage);

        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(energyData));
    }

    private NbtCompound getOrCreateEnergyData(ItemStack stack) {
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent == null) {
            setupEnergyComponents(stack);
            nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        }

        return nbtComponent != null ? nbtComponent.copyNbt() : new NbtCompound();
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        NbtCompound energyData = getOrCreateEnergyData(stack);
        long energy = energyData.getLong("Energy").orElse(0L);
        long capacity = energyData.getLong("Capacity").orElse(this.capacity);
        int usage = energyData.getInt("Usage").orElse(this.usage);

        textConsumer.accept(createEnergyText(energy, capacity));
        textConsumer.accept(createUsageText(usage));
    }

    private MutableText createEnergyText(long energy, long capacity) {
        return Text.literal("")
                .append(Text.translatable("tooltip.energy").formatted(Formatting.GRAY))
                .append(Text.literal(String.format(" %d/%d E", energy, capacity)).formatted(Formatting.GOLD));
    }

    private MutableText createUsageText(int usage) {
        return Text.literal("")
                .append(Text.translatable("tooltip.usage").formatted(Formatting.GRAY))
                .append(Text.literal(String.format(" %d E/", usage)))
                .append(Text.translatable("tooltip.usage.ticksuffix")).formatted(Formatting.GOLD);
    }
}