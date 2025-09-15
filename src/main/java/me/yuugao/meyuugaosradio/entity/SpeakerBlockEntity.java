package me.yuugao.meyuugaosradio.entity;

import static me.yuugao.meyuugaosradio.Constants.*;


import me.yuugao.meyuugaosradio.Radio;
import me.yuugao.meyuugaosradio.block.AbstractEnergyBlock;
import me.yuugao.meyuugaosradio.block.DirectionEnum;
import me.yuugao.meyuugaosradio.sound.ServerHlsAudioManager;

import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class SpeakerBlockEntity extends AbstractEnergyBlockEntity {
    private BlockPos radioPos;

    public SpeakerBlockEntity(BlockPos pos, BlockState state) {
        super(Radio.SPEAKER_BLOCK_ENTITY, pos, state, SPEAKER_ENERGY_CAPACITY, SPEAKER_ENERGY_USAGE);
    }

    @Override
    public void writeData(WriteView view) {
        super.writeData(view);

        if (radioPos != null) {
            view.putInt("RadioPosX", radioPos.getX());
            view.putInt("RadioPosY", radioPos.getY());
            view.putInt("RadioPosZ", radioPos.getZ());
            view.putBoolean("HasRadioPos", true);
        } else {
            view.putBoolean("HasRadioPos", false);
        }
    }

    @Override
    public void readData(ReadView view) {
        super.readData(view);

        if (view.getBoolean("HasRadioPos", false)) {
            int x = view.getInt("RadioPosX", 0);
            int y = view.getInt("RadioPosY", 0);
            int z = view.getInt("RadioPosZ", 0);
            radioPos = new BlockPos(x, y, z).toImmutable();
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
}