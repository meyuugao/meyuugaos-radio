package me.yuugao.meyuugaosradio.item;

import net.minecraft.block.Block;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

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
        NbtCompound blockEntityTag = new NbtCompound();
        blockEntityTag.putLong("Energy", 0);
        blockEntityTag.putLong("Capacity", this.capacity);
        blockEntityTag.putInt("Usage", this.usage);

        stack.set(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.of(blockEntityTag));
    }

    private NbtCompound getOrCreateBlockEntityTag(ItemStack stack) {
        NbtComponent nbtComponent = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
        if (nbtComponent == null) {
            setupEnergyComponents(stack);
            nbtComponent = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
        }
        return nbtComponent.copyNbt();
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);

        NbtCompound blockEntityTag = getOrCreateBlockEntityTag(stack);
        long energy = blockEntityTag.getLong("Energy");
        long capacity = blockEntityTag.getLong("Capacity");
        int usage = blockEntityTag.getInt("Usage");

        tooltip.add(createEnergyText(energy, capacity));
        tooltip.add(createUsageText(usage));
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