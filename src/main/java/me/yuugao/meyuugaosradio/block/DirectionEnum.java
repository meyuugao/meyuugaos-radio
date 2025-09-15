package me.yuugao.meyuugaosradio.block;

import net.minecraft.util.StringIdentifiable;

public enum DirectionEnum implements StringIdentifiable {
    SIDE("side"),
    UP("up"),
    DOWN("down");

    private final String name;

    DirectionEnum(String name) {
        this.name = name;
    }

    @Override
    public String asString() {
        return this.name;
    }
}