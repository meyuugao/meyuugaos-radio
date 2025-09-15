package me.yuugao.meyuugaosradio.client.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "meyuugaosradio_client")
public class ClientModConfig implements ConfigData {
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int volume = 100;
}