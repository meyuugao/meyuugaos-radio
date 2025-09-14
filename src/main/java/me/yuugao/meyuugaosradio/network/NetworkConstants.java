package me.yuugao.meyuugaosradio.network;

import static me.yuugao.meyuugaosradio.Constants.MOD_ID;


import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class NetworkConstants {
    public record ServerRadioPayload(BlockPos pos, List<BlockPos> speakers) implements CustomPayload {
        public static final Id<ServerRadioPayload> ID = new Id<>(Identifier.of(MOD_ID, "server_radio_packet"));
        public static final PacketCodec<RegistryByteBuf, ServerRadioPayload> CODEC = PacketCodec.of(
                (payload, buf) -> {
                    buf.writeBlockPos(payload.pos);
                    buf.writeInt(payload.speakers.size());
                    for (BlockPos speakerPos : payload.speakers) {
                        buf.writeBlockPos(speakerPos);
                    }
                },
                buf -> {
                    BlockPos pos = buf.readBlockPos();
                    int count = buf.readInt();
                    List<BlockPos> speakers = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        speakers.add(buf.readBlockPos());
                    }
                    return new ServerRadioPayload(pos, speakers);
                }
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ServerRequestBlocksPayload(BlockPos pos) implements CustomPayload {
        public static final Id<ServerRequestBlocksPayload> ID = new Id<>(Identifier.of(MOD_ID, "server_request_blocks_packet"));
        public static final PacketCodec<RegistryByteBuf, ServerRequestBlocksPayload> CODEC = PacketCodec.of(
                (payload, buf) -> buf.writeBlockPos(payload.pos),
                buf -> new ServerRequestBlocksPayload(buf.readBlockPos())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ServerRadioGlobalUnbindPayload(BlockPos pos) implements CustomPayload {
        public static final Id<ServerRadioGlobalUnbindPayload> ID = new Id<>(Identifier.of(MOD_ID, "server_radio_globalunbind_packet"));
        public static final PacketCodec<RegistryByteBuf, ServerRadioGlobalUnbindPayload> CODEC = PacketCodec.of(
                (payload, buf) -> buf.writeBlockPos(payload.pos),
                buf -> new ServerRadioGlobalUnbindPayload(buf.readBlockPos())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ServerSpeakerGlobalUnbindPayload(BlockPos pos) implements CustomPayload {
        public static final Id<ServerSpeakerGlobalUnbindPayload> ID = new Id<>(Identifier.of(MOD_ID, "server_speaker_globalunbind_packet"));
        public static final PacketCodec<RegistryByteBuf, ServerSpeakerGlobalUnbindPayload> CODEC = PacketCodec.of(
                (payload, buf) -> buf.writeBlockPos(payload.pos),
                buf -> new ServerSpeakerGlobalUnbindPayload(buf.readBlockPos())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ServerAddBlockPayload(BlockPos pos, float r, float g, float b, float a) implements CustomPayload {
        public static final Id<ServerAddBlockPayload> ID = new Id<>(Identifier.of(MOD_ID, "server_add_block_packet"));
        public static final PacketCodec<RegistryByteBuf, ServerAddBlockPayload> CODEC = PacketCodec.of(
                (payload, buf) -> {
                    buf.writeBlockPos(payload.pos);
                    buf.writeFloat(payload.r);
                    buf.writeFloat(payload.g);
                    buf.writeFloat(payload.b);
                    buf.writeFloat(payload.a);
                },
                buf -> new ServerAddBlockPayload(
                        buf.readBlockPos(),
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readFloat()
                )
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ServerRemoveBlockPayload(BlockPos pos) implements CustomPayload {
        public static final Id<ServerRemoveBlockPayload> ID = new Id<>(Identifier.of(MOD_ID, "server_remove_block_packet"));
        public static final PacketCodec<RegistryByteBuf, ServerRemoveBlockPayload> CODEC = PacketCodec.of(
                (payload, buf) -> buf.writeBlockPos(payload.pos),
                buf -> new ServerRemoveBlockPayload(buf.readBlockPos())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ServerPlayerSendMessagePayload(String textJson, boolean overlay) implements CustomPayload {
        public static final Id<ServerPlayerSendMessagePayload> ID = new Id<>(Identifier.of(MOD_ID, "server_player_sendmessage_packet"));
        public static final PacketCodec<RegistryByteBuf, ServerPlayerSendMessagePayload> CODEC = PacketCodec.of(
                (payload, buf) -> {
                    buf.writeString(payload.textJson);
                    buf.writeBoolean(payload.overlay);
                },
                buf -> new ServerPlayerSendMessagePayload(
                        buf.readString(),
                        buf.readBoolean()
                )
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ServerOpenRadioGuiPayload(BlockPos pos, String streamUrl, float volume) implements CustomPayload {
        public static final Id<ServerOpenRadioGuiPayload> ID = new Id<>(Identifier.of(MOD_ID, "server_open_radio_gui_packet"));
        public static final PacketCodec<RegistryByteBuf, ServerOpenRadioGuiPayload> CODEC = PacketCodec.of(
                (payload, buf) -> {
                    buf.writeBlockPos(payload.pos);
                    buf.writeString(payload.streamUrl);
                    buf.writeFloat(payload.volume);
                },
                buf -> new ServerOpenRadioGuiPayload(
                        buf.readBlockPos(),
                        buf.readString(),
                        buf.readFloat()
                )
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ServerOpenSpeakerGuiPayload(BlockPos pos, float volume) implements CustomPayload {
        public static final Id<ServerOpenSpeakerGuiPayload> ID = new Id<>(Identifier.of(MOD_ID, "server_open_speaker_gui_packet"));
        public static final PacketCodec<RegistryByteBuf, ServerOpenSpeakerGuiPayload> CODEC = PacketCodec.of(
                (payload, buf) -> {
                    buf.writeBlockPos(payload.pos);
                    buf.writeFloat(payload.volume);
                },
                buf -> new ServerOpenSpeakerGuiPayload(buf.readBlockPos(), buf.readFloat())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ServerStreamStartPayload(String streamUrl) implements CustomPayload {
        public static final Id<ServerStreamStartPayload> ID = new Id<>(Identifier.of(MOD_ID, "server_stream_start_packet"));
        public static final PacketCodec<RegistryByteBuf, ServerStreamStartPayload> CODEC = PacketCodec.of(
                (payload, buf) -> buf.writeString(payload.streamUrl),
                buf -> new ServerStreamStartPayload(buf.readString())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ServerStreamStopPayload(String streamUrl) implements CustomPayload {
        public static final Id<ServerStreamStopPayload> ID = new Id<>(Identifier.of(MOD_ID, "server_stream_stop_packet"));
        public static final PacketCodec<RegistryByteBuf, ServerStreamStopPayload> CODEC = PacketCodec.of(
                (payload, buf) -> buf.writeString(payload.streamUrl),
                buf -> new ServerStreamStopPayload(buf.readString())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ServerVolumeUpdatePayload(String streamUrl, float volume) implements CustomPayload {
        public static final Id<ServerVolumeUpdatePayload> ID = new Id<>(Identifier.of(MOD_ID, "server_volume_update_packet"));
        public static final PacketCodec<RegistryByteBuf, ServerVolumeUpdatePayload> CODEC = PacketCodec.of(
                (payload, buf) -> {
                    buf.writeString(payload.streamUrl);
                    buf.writeFloat(payload.volume);
                },
                buf -> new ServerVolumeUpdatePayload(buf.readString(), buf.readFloat())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ServerGlowClearPayload() implements CustomPayload {
        public static final Id<ServerGlowClearPayload> ID = new Id<>(Identifier.of(MOD_ID, "server_glow_clear_packet"));
        public static final PacketCodec<RegistryByteBuf, ServerGlowClearPayload> CODEC = PacketCodec.of(
                (buf, payload) -> {
                },
                buf -> new ServerGlowClearPayload()
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ClientBlocksUpdatePayload(boolean enabled, int count, List<BlockPos> blocks,
                                            BlockPos speakerPos) implements CustomPayload {
        public static final Id<ClientBlocksUpdatePayload> ID = new Id<>(Identifier.of(MOD_ID, "client_blocks_update_packet"));
        public static final PacketCodec<RegistryByteBuf, ClientBlocksUpdatePayload> CODEC = PacketCodec.of(
                (payload, buf) -> {
                    buf.writeBoolean(payload.enabled);
                    buf.writeInt(payload.count);
                    for (BlockPos blockPos : payload.blocks) {
                        buf.writeBlockPos(blockPos);
                    }
                    buf.writeBlockPos(payload.speakerPos);
                },
                buf -> {
                    boolean enabled = buf.readBoolean();
                    int count = buf.readInt();
                    List<BlockPos> blocks = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        blocks.add(buf.readBlockPos());
                    }
                    BlockPos speakerPos = buf.readBlockPos();
                    return new ClientBlocksUpdatePayload(enabled, count, blocks, speakerPos);
                }
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ClientRemotecontrollerOnClickPayload() implements CustomPayload {
        public static final Id<ClientRemotecontrollerOnClickPayload> ID = new Id<>(Identifier.of(MOD_ID, "client_remotecontroller_onclick_packet"));
        public static final PacketCodec<RegistryByteBuf, ClientRemotecontrollerOnClickPayload> CODEC = PacketCodec.of(
                (buf, payload) -> {
                },
                buf -> new ClientRemotecontrollerOnClickPayload()
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ClientRadioStateSwitchPayload(BlockPos pos, String streamUrl) implements CustomPayload {
        public static final Id<ClientRadioStateSwitchPayload> ID = new Id<>(Identifier.of(MOD_ID, "client_radio_state_switch_packet"));
        public static final PacketCodec<RegistryByteBuf, ClientRadioStateSwitchPayload> CODEC = PacketCodec.of(
                (payload, buf) -> {
                    buf.writeBlockPos(payload.pos);
                    buf.writeString(payload.streamUrl);
                },
                buf -> new ClientRadioStateSwitchPayload(buf.readBlockPos(), buf.readString())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ClientSpeakerStateSwitchPayload(BlockPos pos) implements CustomPayload {
        public static final Id<ClientSpeakerStateSwitchPayload> ID = new Id<>(Identifier.of(MOD_ID, "client_speaker_state_switch_packet"));
        public static final PacketCodec<RegistryByteBuf, ClientSpeakerStateSwitchPayload> CODEC = PacketCodec.of(
                (payload, buf) -> buf.writeBlockPos(payload.pos),
                buf -> new ClientSpeakerStateSwitchPayload(buf.readBlockPos())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ClientVolumeUpdatePayload(BlockPos pos, float volume,
                                            float volumeMultiplier) implements CustomPayload {
        public static final Id<ClientVolumeUpdatePayload> ID = new Id<>(Identifier.of(MOD_ID, "client_volume_update_packet"));
        public static final PacketCodec<RegistryByteBuf, ClientVolumeUpdatePayload> CODEC = PacketCodec.of(
                (payload, buf) -> {
                    buf.writeBlockPos(payload.pos);
                    buf.writeFloat(payload.volume);
                    buf.writeFloat(payload.volumeMultiplier);
                },
                buf -> new ClientVolumeUpdatePayload(buf.readBlockPos(), buf.readFloat(), buf.readFloat())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}