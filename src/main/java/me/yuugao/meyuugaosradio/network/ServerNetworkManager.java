package me.yuugao.meyuugaosradio.network;

import static me.yuugao.meyuugaosradio.network.NetworkConstants.*;


import me.yuugao.meyuugaosradio.Radio;
import me.yuugao.meyuugaosradio.block.AbstractEnergyBlock;
import me.yuugao.meyuugaosradio.block.EnergyStateEnum;
import me.yuugao.meyuugaosradio.block.RadioBlock;
import me.yuugao.meyuugaosradio.block.SpeakerBlock;
import me.yuugao.meyuugaosradio.entity.AbstractEnergyBlockEntity;
import me.yuugao.meyuugaosradio.entity.RadioBlockEntity;
import me.yuugao.meyuugaosradio.entity.SpeakerBlockEntity;
import me.yuugao.meyuugaosradio.item.RemoteControllerItem;
import me.yuugao.meyuugaosradio.sound.ServerHlsAudioManager;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ServerNetworkManager {
    public static void initialize() {
        ServerPlayNetworking.registerGlobalReceiver(CLIENT_BLOCKS_UPDATE_PACKET, (server, player, handler, buf, responseSender) -> {
            boolean enabled = buf.readBoolean();
            int count = buf.readInt();
            List<BlockPos> blocks = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                blocks.add(buf.readBlockPos());
            }
            BlockPos speakerPos = buf.readBlockPos();
            server.execute(() -> serverSpeakerUse(player, enabled, blocks, player.getWorld(), speakerPos));
        });

        ServerPlayNetworking.registerGlobalReceiver(CLIENT_REMOTECONTROLLER_ONCLICK_PACKET, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                ItemStack stack = player.getMainHandStack();
                if (stack.getItem() instanceof RemoteControllerItem remoteControllerItem) {
                    remoteControllerItem.onLeftClick(player.getWorld(), player);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(CLIENT_RADIO_STATE_SWITCH_PACKET, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            String streamUrl = buf.readString();
            server.execute(() -> serverRadioStateSwitch(player.getWorld(), pos, streamUrl));
        });

        ServerPlayNetworking.registerGlobalReceiver(CLIENT_SPEAKER_STATE_SWITCH_PACKET, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            server.execute(() -> serverSpeakerStateSwitch(player.getWorld(), pos));
        });

        ServerPlayNetworking.registerGlobalReceiver(CLIENT_VOLUME_UPDATE_PACKET, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            float volume = buf.readFloat();
            float volumeMultiplier = buf.readFloat();
            server.execute(() -> serverVolumeUpdate(player.getWorld(), pos, volume, volumeMultiplier));
        });
    }

    public static void sendServerRadioPacket(ServerPlayerEntity player, RadioBlockEntity radioBlockEntity) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(radioBlockEntity.getPos());
        List<BlockPos> speakers = radioBlockEntity.getSpeakers();
        buf.writeInt(speakers.size());
        speakers.forEach(buf::writeBlockPos);

        ServerPlayNetworking.send(player, SERVER_RADIO_PACKET, buf);
    }

    public static void sendServerRequestBlocks(ServerPlayerEntity player, BlockPos speakerPos) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(speakerPos);
        ServerPlayNetworking.send(player, SERVER_REQUEST_BLOCKS_PACKET, buf);
    }

    public static void sendRadioGlobalUnbind(ServerPlayerEntity player, BlockPos radioPos) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(radioPos);
        ServerPlayNetworking.send(player, SERVER_RADIO_GLOBALUNBIND_PACKET, buf);
    }

    public static void sendSpeakerGlobalUnbind(ServerPlayerEntity player, BlockPos speakerPos) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(speakerPos);
        ServerPlayNetworking.send(player, SERVER_SPEAKER_GLOBALUNBIND_PACKET, buf);
    }

    private static void serverSpeakerUse(ServerPlayerEntity player, boolean renderEnabled, List<BlockPos> blocks, World world, BlockPos speakerPos) {
        if (!renderEnabled) return;

        SpeakerBlockEntity speakerBlockEntity = (SpeakerBlockEntity) world.getBlockEntity(speakerPos);
        AtomicReference<RadioBlockEntity> activeRadioBlockEntity = new AtomicReference<>();

        blocks.forEach(blockPos -> {
            if (world.getBlockEntity(blockPos) instanceof RadioBlockEntity) {
                activeRadioBlockEntity.set((RadioBlockEntity) world.getBlockEntity(blockPos));
            }
        });

        if (speakerBlockEntity == null || activeRadioBlockEntity.get() == null) return;

        if (speakerBlockEntity.getRadioPos() == null) {
            if (!activeRadioBlockEntity.get().getPos().isWithinDistance(speakerPos, world.getGameRules().getInt(Radio.RADIO_CONNECT_RADIUS) + 1)) {
                sendServerPlayerSendMessagePacket(player, Text.translatable("error.toofar").formatted(Formatting.BOLD).formatted(Formatting.RED), true);
                return;
            }

            sendServerAddBlockPacket(player, speakerBlockEntity.getPos(), 0f, 1f, 0f, 0.7f);
            speakerBlockEntity.connectRadio(activeRadioBlockEntity.get().getPos());
            activeRadioBlockEntity.get().connectSpeaker(speakerBlockEntity.getPos());
        } else {
            if (speakerBlockEntity.getRadioPos().equals(activeRadioBlockEntity.get().getPos())) {
                sendServerRemoveBlockPacket(player, speakerBlockEntity.getPos());
                speakerBlockEntity.disconnectRadio();
                activeRadioBlockEntity.get().disconnectSpeaker(speakerBlockEntity.getPos());
            } else {
                sendServerPlayerSendMessagePacket(player, Text.translatable("error.alreadyconnected").formatted(Formatting.BOLD).formatted(Formatting.RED), true);
            }
        }
    }

    private static void serverRadioStateSwitch(World world, BlockPos pos, String streamUrl) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof RadioBlockEntity radioBlockEntity && radioBlockEntity.getAmount() >= radioBlockEntity.getUsage()) {
            boolean state = radioBlockEntity.getCachedState().get(AbstractEnergyBlock.ENERGY_STATE).equals(EnergyStateEnum.ENABLED);
            Block block = world.getBlockState(pos).getBlock();

            if (!streamUrl.isEmpty()) {
                radioBlockEntity.setStreamUrl(streamUrl);
            }

            if (state) {
                if (block instanceof RadioBlock radioBlock) {
                    radioBlock.onDisabled(world, pos, world.getBlockState(pos));
                }
            } else {
                if (block instanceof RadioBlock radioBlock) {
                    radioBlock.onEnabled(world, pos, world.getBlockState(pos), radioBlockEntity.getStreamUrl());
                }
            }
        }
    }

    private static void serverSpeakerStateSwitch(World world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof SpeakerBlockEntity speakerBlockEntity && speakerBlockEntity.getAmount() >= speakerBlockEntity.getUsage()) {
            boolean state = speakerBlockEntity.getCachedState().get(AbstractEnergyBlock.ENERGY_STATE).equals(EnergyStateEnum.ENABLED);
            Block block = world.getBlockState(pos).getBlock();

            if (state) {
                if (block instanceof SpeakerBlock speakerBlock) {
                    speakerBlock.onDisabled(world, pos, world.getBlockState(pos));
                }
            } else {
                if (block instanceof SpeakerBlock speakerBlock) {
                    speakerBlock.onEnabled(world, pos, world.getBlockState(pos));
                }
            }
        }
    }

    private static void serverVolumeUpdate(World world, BlockPos pos, float volume, float volumeMultiplier) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof AbstractEnergyBlockEntity abstractEnergyBlockEntity) {
            abstractEnergyBlockEntity.setVolume(volume);
            String streamUrl = StringUtils.EMPTY;

            if (abstractEnergyBlockEntity instanceof RadioBlockEntity radioBlockEntity) {
                streamUrl = radioBlockEntity.getStreamUrl();
            } else if (abstractEnergyBlockEntity instanceof SpeakerBlockEntity speakerBlockEntity) {
                if (speakerBlockEntity.getRadioPos() != null) {
                    if (world.getBlockEntity(speakerBlockEntity.getRadioPos()) instanceof RadioBlockEntity radioBlockEntity) {
                        streamUrl = radioBlockEntity.getStreamUrl();
                    }
                }
            }

            if (!streamUrl.isEmpty()) {
                ServerHlsAudioManager.updateSoundSourceVolume(streamUrl, pos, volume * volumeMultiplier, world.getRegistryKey());
            }
        }
    }

    private static void sendServerAddBlockPacket(ServerPlayerEntity player, BlockPos pos, float r, float g, float b, float a) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeFloat(r);
        buf.writeFloat(g);
        buf.writeFloat(b);
        buf.writeFloat(a);
        ServerPlayNetworking.send(player, SERVER_ADD_BLOCK_PACKET, buf);
    }

    private static void sendServerRemoveBlockPacket(ServerPlayerEntity player, BlockPos pos) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        ServerPlayNetworking.send(player, SERVER_REMOVE_BLOCK_PACKET, buf);
    }

    public static void sendServerPlayerSendMessagePacket(ServerPlayerEntity player, Text text, boolean overlay) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeText(text);
        buf.writeBoolean(overlay);
        ServerPlayNetworking.send(player, SERVER_PLAYER_SENDMESSAGE_PACKET, buf);
    }

    public static void sendServerOpenRadioGuiPacket(ServerPlayerEntity player, BlockPos pos, String streamUrl, float volume) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeString(streamUrl);
        buf.writeFloat(volume);
        ServerPlayNetworking.send(player, SERVER_OPEN_RADIO_GUI_PACKET, buf);
    }

    public static void sendServerOpenSpeakerGuiPacket(ServerPlayerEntity player, BlockPos pos, float volume) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeFloat(volume);
        ServerPlayNetworking.send(player, SERVER_OPEN_SPEAKER_GUI_PACKET, buf);
    }

    public static void sendServerStreamStartPacket(ServerPlayerEntity player, String streamUrl) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(streamUrl);
        ServerPlayNetworking.send(player, SERVER_STREAM_START_PACKET, buf);
    }

    public static void sendServerStreamStopPacket(ServerPlayerEntity player, String streamUrl) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(streamUrl);
        ServerPlayNetworking.send(player, SERVER_STREAM_STOP_PACKET, buf);
    }

    public static void sendServerVolumeUpdatePacket(ServerPlayerEntity player, String streamUrl, float volume) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(streamUrl);
        buf.writeFloat(volume);
        ServerPlayNetworking.send(player, SERVER_VOLUME_UPDATE_PACKET, buf);
    }

    public static void sendServerGlowClearPacket(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, SERVER_GLOW_CLEAR_PACKET, PacketByteBufs.create());
    }
}