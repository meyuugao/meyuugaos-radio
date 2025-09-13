package me.yuugao.meyuugaosradio.entity;

import static me.yuugao.meyuugaosradio.Constants.SPEAKER_MAX_RANGE;
import static me.yuugao.meyuugaosradio.Constants.SPEAKER_VOLUME_MULTIPLIER;


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
    private BlockPos radioPos = null;

    public SpeakerBlockEntity(BlockPos pos, BlockState state) {
        super(Radio.SPEAKER_BLOCK_ENTITY, pos, state, 200_000L, 16L);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (radioPos == null) {
            nbt.remove("RadioPos");
        } else {
            NbtCompound posCompound = new NbtCompound();
            posCompound.putIntArray("pos", new int[]{radioPos.getX(), radioPos.getY(), radioPos.getZ()});
            nbt.put("RadioPos", posCompound);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("RadioPos")) {
            NbtCompound compound = nbt.getCompound("RadioPos").orElse(new NbtCompound());
            int[] coords = compound.getIntArray("pos").orElse(new int[0]);
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

        radioPos = pos.toImmutable();
        BlockEntity blockEntity = this.world.getBlockEntity(radioPos);
        if (blockEntity instanceof RadioBlockEntity radioBlockEntity && !radioBlockEntity.getStreamUrl().isEmpty() && this.isEnabled()) {
            Direction facing = this.world.getBlockState(this.pos).get(HorizontalFacingBlock.FACING);
            DirectionEnum direction = this.world.getBlockState(this.pos).get(AbstractEnergyBlock.DIRECTION);
            Vec3d vecDirection = new Vec3d(direction == DirectionEnum.SIDE ? facing.getOffsetX() : 0, direction == DirectionEnum.SIDE ? 0 : direction == DirectionEnum.UP ? 1 : -1, direction == DirectionEnum.SIDE ? facing.getOffsetZ() : 0).normalize();
            ServerHlsAudioManager.addSoundSource(radioBlockEntity.getStreamUrl(), this.pos, vecDirection, this.volume * SPEAKER_VOLUME_MULTIPLIER, SPEAKER_MAX_RANGE, world.getRegistryKey());
        }
        markDirty();
    }

    public void disconnectRadio() {
        if (this.world == null || radioPos == null) return;

        BlockEntity blockEntity = this.world.getBlockEntity(radioPos);
        if (blockEntity instanceof RadioBlockEntity radioBlockEntity && !radioBlockEntity.getStreamUrl().isEmpty() && this.isEnabled()) {
            ServerHlsAudioManager.removeSoundSource(radioBlockEntity.getStreamUrl(), this.pos, world.getRegistryKey());
        }
        radioPos = null;
        markDirty();
    }

    public BlockPos getRadioPos() {
        return radioPos;
    }
}