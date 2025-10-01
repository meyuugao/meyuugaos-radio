package me.yuugao.meyuugaosradio.client.events;

import me.yuugao.meyuugaosradio.client.render.BlockGlowRenderer;
import me.yuugao.meyuugaosradio.client.sound.ClientHlsAudioManager;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;

public class ClientEventsManager {
    private static ClientWorld lastWorld;
    private static boolean isPaused;

    public static void initialize() {
        lastWorld = null;
        isPaused = false;

        WorldRenderEvents.AFTER_TRANSLUCENT.register(worldRenderContext -> {
            MatrixStack matrixStack = worldRenderContext.matrixStack();
            if (matrixStack == null) return;

            matrixStack.push();
            matrixStack.translate(-worldRenderContext.camera().getPos().x,
                    -worldRenderContext.camera().getPos().y,
                    -worldRenderContext.camera().getPos().z);

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