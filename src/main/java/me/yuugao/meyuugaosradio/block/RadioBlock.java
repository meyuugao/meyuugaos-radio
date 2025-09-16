package me.yuugao.meyuugaosradio.block;

import static me.yuugao.meyuugaosradio.Constants.*;


import me.yuugao.meyuugaosradio.Radio;
import me.yuugao.meyuugaosradio.entity.RadioBlockEntity;
import me.yuugao.meyuugaosradio.entity.SpeakerBlockEntity;
import me.yuugao.meyuugaosradio.network.ServerNetworkManager;
import me.yuugao.meyuugaosradio.sound.ServerHlsAudioManager;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class RadioBlock extends AbstractEnergyBlock {
    public RadioBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new RadioBlockEntity(pos, state);
    }

    @Override
    protected BlockEntityType<?> getBlockEntityType() {
        return Radio.RADIO_BLOCK_ENTITY;
    }

    @Override
    public void use(World world, BlockPos pos, ServerPlayerEntity player) {
        if (world.getBlockEntity(pos) instanceof RadioBlockEntity radioBlockEntity) {
            ServerNetworkManager.sendServerOpenRadioGuiPacket(player, pos, radioBlockEntity.getStreamUrl(), radioBlockEntity.getVolume());
        }
    }

    @Override
    public void glow(World world, BlockPos pos, ServerPlayerEntity player) {
        if (world.isClient()) return;

        RadioBlockEntity radioBlockEntity = (RadioBlockEntity) world.getBlockEntity(pos);
        if (radioBlockEntity != null) {
            ServerNetworkManager.sendServerRadioPacket(player, radioBlockEntity);
        }
    }

    public void onEnabled(World world, BlockPos pos, BlockState state, String streamUrl) {
        super.onEnabled(world, pos, state);

        if (world.getBlockEntity(pos) instanceof RadioBlockEntity radioBlockEntity) {
            if (!streamUrl.isEmpty()) {
                radioBlockEntity.setStreamUrl(streamUrl);
            }

            String currentStreamUrl = radioBlockEntity.getStreamUrl();
            if (!currentStreamUrl.isEmpty()) {
                ServerHlsAudioManager.addSoundSource(currentStreamUrl, pos, this.getVecDirection(world, pos),
                        radioBlockEntity.getVolume() * RADIO_VOLUME_MULTIPLIER, RADIO_MAX_RANGE, world.getRegistryKey());

                List.copyOf(radioBlockEntity.getSpeakers()).forEach(speakerPos -> {
                    if (world.getBlockEntity(speakerPos) instanceof SpeakerBlockEntity speakerBlockEntity && world.getServer() != null) {
                        if (!radioBlockEntity.getPos().isWithinDistance(speakerPos, world.getServer().getGameRules().getInt(Radio.RADIO_CONNECT_RADIUS) + 1)) {
                            speakerBlockEntity.disconnectRadio();
                            radioBlockEntity.disconnectSpeaker(speakerPos);
                        }
                    }
                });

                activateConnectedSpeakers(world, radioBlockEntity);
            }
        }
    }

    private void activateConnectedSpeakers(World world, RadioBlockEntity radioBlockEntity) {
        radioBlockEntity.getSpeakers().forEach(speakerPos -> {
            BlockEntity blockEntity = world.getBlockEntity(speakerPos);
            if (blockEntity instanceof SpeakerBlockEntity speakerBlockEntity && speakerBlockEntity.isEnabled()) {
                ServerHlsAudioManager.addSoundSource(radioBlockEntity.getStreamUrl(),
                        speakerBlockEntity.getPos(), this.getVecDirection(world, speakerBlockEntity.getPos()),
                        speakerBlockEntity.getVolume() * SPEAKER_VOLUME_MULTIPLIER, SPEAKER_MAX_RANGE, world.getRegistryKey());
            }
        });
    }

    @Override
    public void onDisabled(World world, BlockPos pos, BlockState state) {
        super.onDisabled(world, pos, state);

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof RadioBlockEntity radioBlockEntity) {
            String currentStreamUrl = radioBlockEntity.getStreamUrl();
            if (!currentStreamUrl.isEmpty()) {
                ServerHlsAudioManager.removeSoundSource(currentStreamUrl, pos, world.getRegistryKey());

                radioBlockEntity.getSpeakers().forEach(speakerPos ->
                        ServerHlsAudioManager.removeSoundSource(currentStreamUrl, speakerPos, world.getRegistryKey()));
            }
        }
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        super.onBreak(world, pos, state, player);

        this.globalUnbind(player, world, pos);
    }

    @Override
    public void globalUnbind(PlayerEntity player, World world, BlockPos pos) {
        RadioBlockEntity radioBlockEntity = (RadioBlockEntity) world.getBlockEntity(pos);

        if (world.isClient() || radioBlockEntity == null) return;

        if (!radioBlockEntity.getStreamUrl().isEmpty()) {
            ServerHlsAudioManager.removeSoundSource(radioBlockEntity.getStreamUrl(), pos, world.getRegistryKey());
        }

        radioBlockEntity.getSpeakers().forEach(speakerPos -> {
            SpeakerBlockEntity speakerBlockEntity = (SpeakerBlockEntity) world.getBlockEntity(speakerPos);
            if (speakerBlockEntity != null) {
                speakerBlockEntity.disconnectRadio();
            }
        });

        radioBlockEntity.getSpeakers().clear();
        radioBlockEntity.setStreamUrl(StringUtils.EMPTY);
        radioBlockEntity.markDirty();

        ServerNetworkManager.sendRadioGlobalUnbind((ServerPlayerEntity) player, pos);
    }
}