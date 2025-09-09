package me.yuugao.meyuugaosradio.network;

import net.minecraft.util.Identifier;

public class NetworkConstants {
    public static final String MOD_ID = "meyuugaosradio";

    public static final Identifier SERVER_RADIO_PACKET = id("server_radio_packet");
    public static final Identifier SERVER_REQUEST_BLOCKS_PACKET = id("server_request_blocks_packet");
    public static final Identifier SERVER_RADIO_GLOBALUNBIND_PACKET = id("server_radio_globalunbind_packet");
    public static final Identifier SERVER_SPEAKER_GLOBALUNBIND_PACKET = id("server_speaker_globalunbind_packet");
    public static final Identifier SERVER_ADD_BLOCK_PACKET = id("server_add_block_packet");
    public static final Identifier SERVER_REMOVE_BLOCK_PACKET = id("server_remove_block_packet");
    public static final Identifier SERVER_PLAYER_SENDMESSAGE_PACKET = id("server_player_sendmessage_packet");
    public static final Identifier SERVER_OPEN_RADIO_GUI_PACKET = id("server_open_radio_gui_packet");
    public static final Identifier SERVER_OPEN_SPEAKER_GUI_PACKET = id("server_open_speaker_gui_packet");
    public static final Identifier SERVER_STREAM_START_PACKET = id("server_stream_start_packet");
    public static final Identifier SERVER_VOLUME_UPDATE_PACKET = id("server_volume_update_packet");
    public static final Identifier SERVER_STREAM_STOP_PACKET = id("server_stream_stop_packet");
    public static final Identifier SERVER_GLOW_CLEAR_PACKET = id("server_glow_clear_packet");

    public static final Identifier CLIENT_BLOCKS_UPDATE_PACKET = id("client_blocks_update_packet");
    public static final Identifier CLIENT_REMOTECONTROLLER_ONCLICK_PACKET = id("client_remotecontroller_onclick_packet");
    public static final Identifier CLIENT_RADIO_STATE_SWITCH_PACKET = id("client_radio_state_switch_packet");
    public static final Identifier CLIENT_SPEAKER_STATE_SWITCH_PACKET = id("client_speaker_state_switch_packet");
    public static final Identifier CLIENT_VOLUME_UPDATE_PACKET = id("client_volume_update_packet");

    private static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}