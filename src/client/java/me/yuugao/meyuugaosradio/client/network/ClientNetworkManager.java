package me.yuugao.meyuugaosradio.client.network;

import me.yuugao.meyuugaosradio.client.gui.RadioGuiScreen;
import me.yuugao.meyuugaosradio.client.gui.SpeakerGuiScreen;
import me.yuugao.meyuugaosradio.client.render.BlockGlowRenderer;
import me.yuugao.meyuugaosradio.client.sound.ClientHlsAudioManager;
import me.yuugao.meyuugaosradio.network.NetworkConstants;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.List;

public class ClientNetworkManager {
    private static final float[] RADIO_COLOR = {1f, 0f, 0f, 0.7f};
    private static final float[] SPEAKER_COLOR = {0f, 1f, 0f, 0.7f};

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(NetworkConstants.ServerRadioPayload.ID, (payload, context) ->
                context.client().execute(() -> clientRadioUse(payload.pos(), payload.speakers())));

        ClientPlayNetworking.registerGlobalReceiver(NetworkConstants.ServerRequestBlocksPayload.ID, (payload, context) ->
                context.client().execute(() -> sendClientBlocksUpdatePacket(BlockGlowRenderer.isEnabled(), BlockGlowRenderer.getBlocksToRender().keySet(), payload.pos())));

        ClientPlayNetworking.registerGlobalReceiver(NetworkConstants.ServerRadioGlobalUnbindPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (BlockGlowRenderer.isEnabled() && BlockGlowRenderer.getBlocksToRender().containsKey(payload.pos())) {
                        BlockGlowRenderer.clearAll();
                        BlockGlowRenderer.setEnabled(false);
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(NetworkConstants.ServerSpeakerGlobalUnbindPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (BlockGlowRenderer.isEnabled() && BlockGlowRenderer.getBlocksToRender().containsKey(payload.pos())) {
                        BlockGlowRenderer.removeBlock(payload.pos());
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(NetworkConstants.ServerAddBlockPayload.ID, (payload, context) ->
                context.client().execute(() -> BlockGlowRenderer.addBlock(payload.pos(), payload.r(), payload.g(), payload.b(), payload.a())));

        ClientPlayNetworking.registerGlobalReceiver(NetworkConstants.ServerRemoveBlockPayload.ID, (payload, context) ->
                context.client().execute(() -> BlockGlowRenderer.removeBlock(payload.pos())));

        ClientPlayNetworking.registerGlobalReceiver(NetworkConstants.ServerPlayerSendMessagePayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().getNetworkHandler() != null) {
                        Text text = Text.Serialization.fromJson(payload.textJson(), MinecraftClient.getInstance().getNetworkHandler().getRegistryManager());
                        MinecraftClient.getInstance().player.sendMessage(text, payload.overlay());
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(NetworkConstants.ServerOpenRadioGuiPayload.ID, (payload, context) ->
                context.client().execute(() -> MinecraftClient.getInstance().setScreen(new RadioGuiScreen(payload.pos(), payload.streamUrl(), payload.volume()))));

        ClientPlayNetworking.registerGlobalReceiver(NetworkConstants.ServerOpenSpeakerGuiPayload.ID, (payload, context) ->
                context.client().execute(() -> MinecraftClient.getInstance().setScreen(new SpeakerGuiScreen(payload.pos(), payload.volume()))));

        ClientPlayNetworking.registerGlobalReceiver(NetworkConstants.ServerStreamStartPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientHlsAudioManager.handleStreamStart(payload.streamUrl())));

        ClientPlayNetworking.registerGlobalReceiver(NetworkConstants.ServerStreamStopPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientHlsAudioManager.handleStreamStop(payload.streamUrl())));

        ClientPlayNetworking.registerGlobalReceiver(NetworkConstants.ServerVolumeUpdatePayload.ID, (payload, context) ->
                context.client().execute(() -> ClientHlsAudioManager.handleVolumeUpdate(payload.streamUrl(), payload.volume())));

        ClientPlayNetworking.registerGlobalReceiver(NetworkConstants.ServerGlowClearPayload.ID, (payload, context) ->
                context.client().execute(() -> BlockGlowRenderer.setEnabled(false)));
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
        ClientPlayNetworking.send(new NetworkConstants.ClientBlocksUpdatePayload(enabled, blocks.size(), List.copyOf(blocks), speakerPos));
    }

    public static void sendClientRemotecontrollerOnClickPacket() {
        ClientPlayNetworking.send(new NetworkConstants.ClientRemotecontrollerOnClickPayload());
    }

    public static void sendClientRadioStateSwitchPacket(BlockPos pos, String streamUrl) {
        ClientPlayNetworking.send(new NetworkConstants.ClientRadioStateSwitchPayload(pos, streamUrl));
    }

    public static void sendClientSpeakerStateSwitchPacket(BlockPos pos) {
        ClientPlayNetworking.send(new NetworkConstants.ClientSpeakerStateSwitchPayload(pos));
    }

    public static void sendClientVolumeUpdatePacket(BlockPos pos, float volume, float volumeMultiplier) {
        ClientPlayNetworking.send(new NetworkConstants.ClientVolumeUpdatePayload(pos, volume, volumeMultiplier));
    }
}