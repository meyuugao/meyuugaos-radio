package me.yuugao.meyuugaosradio.client.events;

import static me.yuugao.meyuugaosradio.Constants.CLIENT_LOGGER;


import me.yuugao.meyuugaosradio.client.render.BlockGlowRenderer;
import me.yuugao.meyuugaosradio.client.sound.ClientHlsAudioManager;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;

public class ClientEventsManager {
    private static ClientWorld lastWorld;
    private static boolean isPaused;

    public static void initialize() {
        lastWorld = null;
        isPaused = false;

        WorldRenderEvents.END_MAIN.register(worldRenderContext -> {
            MatrixStack matrixStack = worldRenderContext.matrices();
            GameRenderer gameRenderer = worldRenderContext.gameRenderer();
            if (matrixStack == null) return;

            matrixStack.push();
            matrixStack.translate(-gameRenderer.getCamera().getPos().x,
                    -gameRenderer.getCamera().getPos().y,
                    -gameRenderer.getCamera().getPos().z);

            BlockGlowRenderer.render(matrixStack, worldRenderContext.consumers());

            matrixStack.pop();
        });
        ClientTickEvents.END_CLIENT_TICK.register(minecraftClient -> {
            ClientWorld currentWorld = minecraftClient.world;

            if (currentWorld != lastWorld) {
                if (lastWorld != null) {
                    onWorldChanged();
                }
                lastWorld = currentWorld;
            }

            if (minecraftClient.isPaused() && !isPaused) {
                isPaused = true;
                ClientHlsAudioManager.stopAudioInstances();
            } else if (!minecraftClient.isPaused() && isPaused) {
                isPaused = false;
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((clientPlayNetworkHandler, minecraftClient) ->
                minecraftClient.execute(BlockGlowRenderer::onDisconnect));
    }

    private static void onWorldChanged() {
        ClientHlsAudioManager.cleanup();
        BlockGlowRenderer.clearAll();
    }
}