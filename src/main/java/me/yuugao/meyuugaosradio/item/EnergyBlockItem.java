package me.yuugao.meyuugaosradio.item;

import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
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
        setupEnergyNbt(stack);
        return stack;
    }

    private void setupEnergyNbt(ItemStack stack) {
        NbtCompound blockEntityTag = new NbtCompound();
        blockEntityTag.putLong("Energy", 0);
        blockEntityTag.putLong("Capacity", this.capacity);
        blockEntityTag.putInt("Usage", this.usage);

        NbtCompound nbt = new NbtCompound();
        nbt.put("BlockEntityTag", blockEntityTag);
        stack.setNbt(nbt);
    }

    private NbtCompound getOrCreateBlockEntityTag(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        if (!nbt.contains("BlockEntityTag")) {
            setupEnergyNbt(stack);
        }
        return nbt.getCompound("BlockEntityTag");
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

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