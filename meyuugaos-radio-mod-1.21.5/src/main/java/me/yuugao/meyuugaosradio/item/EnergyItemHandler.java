package me.yuugao.meyuugaosradio.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import org.apache.commons.lang3.StringUtils;

import java.util.function.Consumer;

public class EnergyItemHandler {
    private final long defaultCapacity;
    private final long defaultUsage;

    public EnergyItemHandler(long defaultCapacity, long defaultUsage) {
        this.defaultCapacity = defaultCapacity;
        this.defaultUsage = defaultUsage;
    }

    public void appendTooltip(ItemStack stack, Consumer<Text> textConsumer) {
        long energy = getEnergy(stack);
        long capacity = getCapacity(stack);
        long usage = getUsage(stack);

        textConsumer.accept(createEnergyText(energy, capacity));
        textConsumer.accept(createUsageText(usage));
    }

    public void setupEnergyComponents(ItemStack stack) {
        NbtCompound energyData = new NbtCompound();
        energyData.putLong("Energy", 0L);
        energyData.putLong("Capacity", defaultCapacity);
        energyData.putLong("Usage", defaultUsage);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(energyData));
    }

    public long getCapacity(ItemStack stack) {
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);

        return nbtComponent != null ? nbtComponent.copyNbt().getLong("Capacity").orElse(defaultCapacity) : defaultCapacity;
    }

    public long getEnergy(ItemStack stack) {
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);

        return nbtComponent != null ? nbtComponent.copyNbt().getLong("Energy").orElse(0L) : 0L;
    }

    public long getUsage(ItemStack stack) {
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);

        return nbtComponent != null ? nbtComponent.copyNbt().getLong("Usage").orElse(defaultUsage) : defaultUsage;
    }

    private void setEnergy(ItemStack stack, long energy) {
        NbtCompound nbt = new NbtCompound();
        nbt.putLong("Energy", Math.min(energy, getCapacity(stack)));
        nbt.putLong("Capacity", getCapacity(stack));
        nbt.putLong("Usage", getUsage(stack));
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public void addEnergy(ItemStack stack, long amount) {
        setEnergy(stack, getEnergy(stack) + amount);
    }

    public void removeEnergy(ItemStack stack, long amount) {
        setEnergy(stack, Math.max(0, getEnergy(stack) - amount));
    }

    public MutableText createEnergyText(long energy, long capacity) {
        return Text.literal(StringUtils.EMPTY)
                .append(Text.translatable("tooltip.energy").formatted(Formatting.GRAY))
                .append(Text.literal(String.format(" %d/%d E", energy, capacity)).formatted(Formatting.GOLD));
    }

    public MutableText createUsageText(long usage) {
        return Text.literal(StringUtils.EMPTY)
                .append(Text.translatable("tooltip.usage").formatted(Formatting.GRAY))
                .append(Text.literal(String.format(" %d E/", usage)))
                .append(Text.translatable("tooltip.usage.ticksuffix")).formatted(Formatting.GOLD);
    }
}