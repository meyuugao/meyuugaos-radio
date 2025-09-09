package me.yuugao.meyuugaosradio.client.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

public class ClientModConfigManager {
    private static ClientModConfig config;

    public static void initialize() {
        AutoConfig.register(ClientModConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(ClientModConfig.class).getConfig();
    }

    public static ClientModConfig getConfig() {
        return config;
    }
}