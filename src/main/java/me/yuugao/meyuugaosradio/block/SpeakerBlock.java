package me.yuugao.meyuugaosradio.block;

import static me.yuugao.meyuugaosradio.Constants.SPEAKER_MAX_RANGE;
import static me.yuugao.meyuugaosradio.Constants.SPEAKER_VOLUME_MULTIPLIER;


import me.yuugao.meyuugaosradio.Radio;
import me.yuugao.meyuugaosradio.entity.RadioBlockEntity;
import me.yuugao.meyuugaosradio.entity.SpeakerBlockEntity;
import me.yuugao.meyuugaosradio.network.ServerNetworkManager;
import me.yuugao.meyuugaosradio.sound.ServerHlsAudioManager;

import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.mojang.serialization.MapCodec;

public class SpeakerBlock extends AbstractEnergyBlock {
    public SpeakerBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SpeakerBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<?> getBlockEntityType() {
        return Radio.SPEAKER_BLOCK_ENTITY;
    }

    @Override
    public void use(World world, BlockPos pos, ServerPlayerEntity player) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof SpeakerBlockEntity speakerBlockEntity) {
            ServerNetworkManager.sendServerOpenSpeakerGuiPacket(player, pos, speakerBlockEntity.getVolume());
        }
    }

    @Override
    public void glow(World world, BlockPos pos, ServerPlayerEntity player) {
        if (world.isClient()) return;

        ServerNetworkManager.sendServerRequestBlocks(player, pos);
    }

    public void onEnabled(World world, BlockPos pos, BlockState state) {
        super.onEnabled(world, pos, state);

        if (world.getBlockEntity(pos) instanceof SpeakerBlockEntity speakerBlockEntity) {
            if (speakerBlockEntity.getRadioPos() != null) {
                BlockEntity blockEntity = world.getBlockEntity(speakerBlockEntity.getRadioPos());
                if (blockEntity instanceof RadioBlockEntity radioBlockEntity && !radioBlockEntity.getStreamUrl().isEmpty()) {
                    ServerHlsAudioManager.addSoundSource(radioBlockEntity.getStreamUrl(), pos, this.getVecDirection(world, pos),
                            speakerBlockEntity.getVolume() * SPEAKER_VOLUME_MULTIPLIER, SPEAKER_MAX_RANGE, world.getRegistryKey());
                }
            }
        }
    }

    @Override
    public void onDisabled(World world, BlockPos pos, BlockState state) {
        super.onDisabled(world, pos, state);

        if (world.getBlockEntity(pos) instanceof SpeakerBlockEntity speakerBlockEntity) {
            if (speakerBlockEntity.getRadioPos() != null) {
                BlockEntity blockEntity1 = world.getBlockEntity(speakerBlockEntity.getRadioPos());
                if (blockEntity1 instanceof RadioBlockEntity radioBlockEntity) {
                    if (!radioBlockEntity.getStreamUrl().isEmpty()) {
                        ServerHlsAudioManager.removeSoundSource(radioBlockEntity.getStreamUrl(), pos, world.getRegistryKey());
                    }
                }
            }
        }
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        super.onBreak(world, pos, state, player);

        this.globalUnbind(player, world, pos);

        return state;
    }

    @Override
    public void globalUnbind(PlayerEntity player, World world, BlockPos pos) {
        SpeakerBlockEntity speakerBlockEntity = (SpeakerBlockEntity) world.getBlockEntity(pos);
        if (world.isClient() || speakerBlockEntity == null) return;

        BlockPos radioPos = speakerBlockEntity.getRadioPos();
        if (radioPos != null) {
            speakerBlockEntity.disconnectRadio();
            BlockEntity blockEntity = world.getBlockEntity(radioPos);
            if (blockEntity instanceof RadioBlockEntity bindedRadioBlockEntity) {
                bindedRadioBlockEntity.disconnectSpeaker(pos);
            }
        }

        ServerNetworkManager.sendSpeakerGlobalUnbind((ServerPlayerEntity) player, speakerBlockEntity.getPos());
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return null;
    }
}