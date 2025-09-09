package me.yuugao.meyuugaosradio.block;

import net.minecraft.util.StringIdentifiable;

public enum EnergyStateEnum implements StringIdentifiable {
    ENABLED("enabled"),
    DISABLED("disabled");

    private final String name;

    EnergyStateEnum(String name) {
        this.name = name;
    }

    @Override
    public String asString() {
        return this.name;
    }
}