package me.yuugao.meyuugaosradio.client.network;

import static me.yuugao.meyuugaosradio.network.NetworkConstants.*;


import me.yuugao.meyuugaosradio.client.gui.RadioGuiScreen;
import me.yuugao.meyuugaosradio.client.gui.SpeakerGuiScreen;
import me.yuugao.meyuugaosradio.client.render.BlockGlowRenderer;
import me.yuugao.meyuugaosradio.client.sound.ClientHlsAudioManager;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClientNetworkManager {
    private static final float[] RADIO_COLOR = {1f, 0f, 0f, 0.7f};
    private static final float[] SPEAKER_COLOR = {0f, 1f, 0f, 0.7f};

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(SERVER_RADIO_PACKET, (client, handler, buf, responseSender) -> {
            BlockPos radioPos = buf.readBlockPos();
            int count = buf.readInt();
            List<BlockPos> speakers = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                speakers.add(buf.readBlockPos());
            }
            client.execute(() -> clientRadioUse(radioPos, speakers));
        });

        ClientPlayNetworking.registerGlobalReceiver(SERVER_REQUEST_BLOCKS_PACKET, (client, handler, buf, responseSender) -> {
            BlockPos speakerPos = buf.readBlockPos();
            client.execute(() -> sendClientBlocksUpdatePacket(BlockGlowRenderer.isEnabled(), BlockGlowRenderer.getBlocksToRender().keySet(), speakerPos));
        });

        ClientPlayNetworking.registerGlobalReceiver(SERVER_RADIO_GLOBALUNBIND_PACKET, (client, handler, buf, responseSender) -> {
            BlockPos radioPos = buf.readBlockPos();
            client.execute(() -> {
                if (BlockGlowRenderer.isEnabled() && BlockGlowRenderer.getBlocksToRender().containsKey(radioPos)) {
                    BlockGlowRenderer.clearAll();
                    BlockGlowRenderer.setEnabled(false);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SERVER_SPEAKER_GLOBALUNBIND_PACKET, (client, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            client.execute(() -> {
                if (BlockGlowRenderer.isEnabled() && BlockGlowRenderer.getBlocksToRender().containsKey(pos)) {
                    BlockGlowRenderer.removeBlock(pos);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SERVER_ADD_BLOCK_PACKET, (client, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            float r = buf.readFloat();
            float g = buf.readFloat();
            float b = buf.readFloat();
            float a = buf.readFloat();
            client.execute(() -> BlockGlowRenderer.addBlock(pos, r, g, b, a));
        });

        ClientPlayNetworking.registerGlobalReceiver(SERVER_REMOVE_BLOCK_PACKET, (client, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            client.execute(() -> BlockGlowRenderer.removeBlock(pos));
        });

        ClientPlayNetworking.registerGlobalReceiver(SERVER_PLAYER_SENDMESSAGE_PACKET, (client, handler, buf, responseSender) -> {
            Text text = buf.readText();
            boolean overlay = buf.readBoolean();
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(text, overlay);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SERVER_OPEN_RADIO_GUI_PACKET, (client, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            String streamUrl = buf.readString();
            float volume = buf.readFloat();
            client.execute(() -> MinecraftClient.getInstance().setScreen(new RadioGuiScreen(pos, streamUrl, volume)));
        });

        ClientPlayNetworking.registerGlobalReceiver(SERVER_OPEN_SPEAKER_GUI_PACKET, (client, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            float volume = buf.readFloat();
            client.execute(() -> MinecraftClient.getInstance().setScreen(new SpeakerGuiScreen(pos, volume)));
        });

        ClientPlayNetworking.registerGlobalReceiver(SERVER_STREAM_START_PACKET, (client, handler, buf, responseSender) -> {
            String streamUrl = buf.readString();
            client.execute(() -> ClientHlsAudioManager.handleStreamStart(streamUrl));
        });

        ClientPlayNetworking.registerGlobalReceiver(SERVER_STREAM_STOP_PACKET, (client, handler, buf, responseSender) -> {
            String streamUrl = buf.readString();
            client.execute(() -> ClientHlsAudioManager.handleStreamStop(streamUrl));
        });

        ClientPlayNetworking.registerGlobalReceiver(SERVER_VOLUME_UPDATE_PACKET, (client, handler, buf, responseSender) -> {
            String streamUrl = buf.readString();
            float volume = buf.readFloat();
            client.execute(() -> ClientHlsAudioManager.handleVolumeUpdate(streamUrl, volume));
        });

        ClientPlayNetworking.registerGlobalReceiver(SERVER_GLOW_CLEAR_PACKET, (client, handler, buf, responseSender) -> {
            client.execute(() -> BlockGlowRenderer.setEnabled(false));
        });
    }

    private static void clientRadioUse(BlockPos radioPos, List<BlockPos> speakers) {
        if (BlockGlowRenderer.isEnabled()) {
            if (BlockGlowRenderer.getBlocksToRender().containsKey(radioPos)) {
                BlockGlowRenderer.clearAll();
                BlockGlowRenderer.setEnabled(false);
            } else {
                BlockGlowRenderer.clearAll();
                BlockGlowRenderer.addBlock(radioPos, RADIO_COLOR[0], RADIO_COLOR[1], RADIO_COLOR[2], RADIO_COLOR[3]);
                speakers.forEach(speakerPos ->
                        BlockGlowRenderer.addBlock(speakerPos, SPEAKER_COLOR[0], SPEAKER_COLOR[1], SPEAKER_COLOR[2], SPEAKER_COLOR[3]));
            }
        } else {
            BlockGlowRenderer.addBlock(radioPos, RADIO_COLOR[0], RADIO_COLOR[1], RADIO_COLOR[2], RADIO_COLOR[3]);
            speakers.forEach(speakerPos ->
                    BlockGlowRenderer.addBlock(speakerPos, SPEAKER_COLOR[0], SPEAKER_COLOR[1], SPEAKER_COLOR[2], SPEAKER_COLOR[3]));
            BlockGlowRenderer.setEnabled(true);
        }
    }

    public static void sendClientBlocksUpdatePacket(boolean enabled, Collection<BlockPos> blocks, BlockPos speakerPos) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(enabled);
        buf.writeInt(blocks.size());
        blocks.forEach(buf::writeBlockPos);
        buf.writeBlockPos(speakerPos);
        ClientPlayNetworking.send(CLIENT_BLOCKS_UPDATE_PACKET, buf);
    }

    public static void sendClientRemotecontrollerOnClickPacket() {
        ClientPlayNetworking.send(CLIENT_REMOTECONTROLLER_ONCLICK_PACKET, PacketByteBufs.create());
    }

    public static void sendClientRadioStateSwitchPacket(BlockPos pos, String streamUrl) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeString(streamUrl);
        ClientPlayNetworking.send(CLIENT_RADIO_STATE_SWITCH_PACKET, buf);
    }

    public static void sendClientSpeakerStateSwitchPacket(BlockPos pos) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        ClientPlayNetworking.send(CLIENT_SPEAKER_STATE_SWITCH_PACKET, buf);
    }

    public static void sendClientVolumeUpdatePacket(BlockPos pos, float volume, float volumeMultiplier) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeFloat(volume);
        buf.writeFloat(volumeMultiplier);
        ClientPlayNetworking.send(CLIENT_VOLUME_UPDATE_PACKET, buf);
    }
}