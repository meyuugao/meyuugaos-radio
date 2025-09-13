package me.yuugao.meyuugaosradio;

import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {
    public static final String MOD_ID = "meyuugaosradio";

    public static final Logger LOGGER = LoggerFactory.getLogger("meyuugaosradio");

    public static final String RADIO_BLOCK_ID = "radio";
    public static final String SPEAKER_BLOCK_ID = "speaker";

    public static final String REMOTE_CONTROLLER_ID = "remote_controller";
    public static final String ELECTRONIC_CIRCUIT_ID = "electronic_circuit";
    public static final String BATTERY_ID = "battery";
    public static final String SMALL_BATTERY_ID = "small_battery";
    public static final String ANTENNA_ID = "antenna";
    public static final String SMALL_MEMBRANE_ID = "small_membrane";
    public static final String MEMBRANE_ID = "membrane";

    public static final String RADIO_BLOCK_ENTITY_ID = "radio_block_entity";
    public static final String SPEAKER_BLOCK_ENTITY_ID = "speaker_block_entity";

    public static final String BLOCK_DISMANTLE_SOUND_ID = "block_dismantle";

    public static final long RADIO_ENERGY_CAPACITY = 100_000L;
    public static final int RADIO_ENERGY_USAGE = 8;

    public static final long SPEAKER_ENERGY_CAPACITY = 200_000L;
    public static final int SPEAKER_ENERGY_USAGE = 16;

    public static final long REMOTE_CONTROLLER_ENERGY_CAPACITY = 10_000L;
    public static final int REMOTE_CONTROLLER_ENERGY_USAGE = 8;

    public static final float RADIO_VOLUME_MULTIPLIER = 2f;
    public static final float SPEAKER_VOLUME_MULTIPLIER = 4f;
    public static final float RADIO_MAX_RANGE = 16f;
    public static final float SPEAKER_MAX_RANGE = 24f;

    public static final int DEFAULT_STACK_SIZE = 64;
    public static final int REMOTE_CONTROLLER_STACK_SIZE = 1;

    public static final Identifier TECHREBORN_WRENCH_ID = Identifier.of("techreborn", "wrench");
    public static final Identifier MEYUUGAOSRADIO_REMOTE_CONTROLLER_ID = Identifier.of("meyuugaosradio", "remote_controller");


    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}