package me.yuugao.meyuugaosradio.client.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "meyuugaosradio_client")
public class ClientModConfig implements ConfigData {
    public int volume = 100;

    @ConfigEntry.Gui.Tooltip
    public int audioBufferSize = 1000;

    @Override
    public void validatePostLoad() {
        if (volume < 0) volume = 0;
        if (volume > 100) volume = 100;

        if (audioBufferSize < 100) audioBufferSize = 100;
        if (audioBufferSize > 2000) audioBufferSize = 2000;
    }
}