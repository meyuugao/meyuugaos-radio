package me.yuugao.meyuugaosradio.client.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import me.yuugao.meyuugaosradio.client.sound.ClientHlsAudioManager;

public class ClientModConfigManager {
    private static ClientModConfig config;

    public static void initialize() {
        AutoConfig.register(ClientModConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(ClientModConfig.class).getConfig();

        AutoConfig.getConfigHolder(ClientModConfig.class).registerSaveListener((manager, data) -> {
            data.validatePostLoad();
            config = data;

            ClientHlsAudioManager.onConfigChanged();

            return net.minecraft.util.ActionResult.PASS;
        });
    }

    public static ClientModConfig getConfig() {
        return config;
    }
}