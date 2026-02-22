package me.yuugao.meyuugaosradio.client;

import static me.yuugao.meyuugaosradio.Constants.CLIENT_LOGGER;


import me.yuugao.meyuugaosradio.client.config.ClientModConfigManager;
import me.yuugao.meyuugaosradio.client.events.ClientEventsManager;
import me.yuugao.meyuugaosradio.client.network.ClientNetworkManager;

import net.fabricmc.api.ClientModInitializer;

public class RadioClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientModConfigManager.initialize();
        ClientNetworkManager.initialize();
        ClientEventsManager.initialize();

        CLIENT_LOGGER.info("[CLIENT] MeYuugaos Radio mod initialized!");
    }
}