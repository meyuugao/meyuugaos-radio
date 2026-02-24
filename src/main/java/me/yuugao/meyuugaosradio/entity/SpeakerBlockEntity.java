package me.yuugao.meyuugaosradio.entity;

import static me.yuugao.meyuugaosradio.Constants.*;


import me.yuugao.meyuugaosradio.Radio;
import me.yuugao.meyuugaosradio.block.AbstractEnergyBlock;
import me.yuugao.meyuugaosradio.block.DirectionEnum;
import me.yuugao.meyuugaosradio.sound.ServerHlsAudioManager;

import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class SpeakerBlockEntity extends AbstractEnergyBlockEntity {
    private BlockPos radioPos;

    public SpeakerBlockEntity(BlockPos pos, BlockState state) {
        super(Radio.SPEAKER_BLOCK_ENTITY, pos, state, SPEAKER_ENERGY_CAPACITY, SPEAKER_ENERGY_USAGE);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        if (radioPos == null) {
            nbt.remove("RadioPos");
        } else {
            NbtCompound posCompound = new NbtCompound();
            posCompound.putIntArray("pos", new int[]{radioPos.getX(), radioPos.getY(), radioPos.getZ()});
            nbt.put("RadioPos", posCompound);
        }

        super.writeNbt(nbt, registryLookup);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        if (nbt.contains("RadioPos")) {
            NbtCompound compound = nbt.getCompound("RadioPos");
            int[] coords = compound.getIntArray("pos");
            if (coords.length == 3) {
                radioPos = new BlockPos(coords[0], coords[1], coords[2]);
            } else {
                radioPos = null;
            }
        } else {
            radioPos = null;
        }
    }

    public void connectRadio(BlockPos pos) {
        if (this.world == null) return;

        BlockEntity blockEntity = this.world.getBlockEntity(pos.toImmutable());
        if (blockEntity instanceof RadioBlockEntity radioBlockEntity) {
            this.radioPos = pos;

            if (!radioBlockEntity.getStreamUrl().isEmpty() && this.isEnabled()) {
                Direction facing = this.world.getBlockState(this.pos).get(HorizontalFacingBlock.FACING);
                DirectionEnum direction = this.world.getBlockState(this.pos).get(AbstractEnergyBlock.DIRECTION);
                Vec3d vecDirection = new Vec3d(direction == DirectionEnum.SIDE ? facing.getOffsetX() : 0,
                        direction == DirectionEnum.SIDE ? 0 : direction == DirectionEnum.UP ? 1 : -1,
                        direction == DirectionEnum.SIDE ? facing.getOffsetZ() : 0).normalize();

                ServerHlsAudioManager.addSoundSource(radioBlockEntity.getStreamUrl(), this.pos, vecDirection,
                        this.volume * SPEAKER_VOLUME_MULTIPLIER, SPEAKER_MAX_RANGE, world.getRegistryKey());
            }
        }

        markDirty();
    }

    public void disconnectRadio() {
        if (this.world == null || radioPos == null) return;

        BlockEntity blockEntity = this.world.getBlockEntity(radioPos);
        if (blockEntity instanceof RadioBlockEntity radioBlockEntity) {
            radioPos = null;

            if (!radioBlockEntity.getStreamUrl().isEmpty() && this.isEnabled()) {
                ServerHlsAudioManager.removeSoundSource(radioBlockEntity.getStreamUrl(), this.pos, world.getRegistryKey());
            }
        }

        markDirty();
    }

    public BlockPos getRadioPos() {
        return radioPos;
    }

    public void setRadioPos(BlockPos radioPos) {
        this.radioPos = radioPos;
        markDirty();
    }
}