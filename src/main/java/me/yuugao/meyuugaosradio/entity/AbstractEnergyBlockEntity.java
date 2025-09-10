package me.yuugao.meyuugaosradio.entity;

import me.yuugao.meyuugaosradio.block.AbstractEnergyBlock;
import me.yuugao.meyuugaosradio.block.EnergyStateEnum;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

import team.reborn.energy.api.EnergyStorage;

public abstract class AbstractEnergyBlockEntity extends BlockEntity implements EnergyStorage {
    protected long energy = 0;
    protected final long capacity;
    protected final long usage;
    protected float volume = 0.5f;

    public AbstractEnergyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, long capacity, long usage) {
        super(type, pos, state);
        this.capacity = capacity;
        this.usage = usage;
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putLong("Energy", energy);
        nbt.putLong("Capacity", capacity);
        nbt.putLong("Usage", usage);
        nbt.putFloat("Volume", volume);
        super.writeNbt(nbt, registryLookup);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.energy = nbt.getLong("Energy");
        this.volume = nbt.getFloat("Volume");
    }

    public boolean isEnabled() {
        return this.getCachedState().get(AbstractEnergyBlock.ENERGY_STATE).equals(EnergyStateEnum.ENABLED);
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        long inserted = Math.min(maxAmount, capacity - energy);

        transaction.addCloseCallback((t, result) -> {
            if (result.wasCommitted()) {
                energy += inserted;
                markDirty();
            }
        });

        return inserted;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        return 0;
    }

    @Override
    public long getAmount() {
        return energy;
    }

    @Override
    public long getCapacity() {
        return capacity;
    }

    public long getUsage() {
        return usage;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
        markDirty();
    }

    public boolean consumeEnergy() {
        if (energy >= usage) {
            energy -= usage;
            markDirty();
            return true;
        }
        return false;
    }

    public void setEnergy(long energy) {
        this.energy = energy;
        markDirty();
    }
}