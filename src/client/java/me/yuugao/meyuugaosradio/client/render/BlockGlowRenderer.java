package me.yuugao.meyuugaosradio.client.render;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;

import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;

public class BlockGlowRenderer {
    private static final RenderLayer DEBUG_QUADS_WITH_NO_DEPTH_TEST = RenderLayer.of(
            "debug_quads_with_no_depth_test",
            RenderLayer.getDebugQuads().getExpectedBufferSize(),
            false,
            false,
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation("pipeline/debug_quads")
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).build(),
            RenderLayer.MultiPhaseParameters.builder().build(false)
    );
    private static final Map<BlockPos, GlowInfo> blocksToRender = new HashMap<>();
    private static boolean enabled = false;

    public static void onDisconnect() {
        clearAll();
        setEnabled(false);
    }

    public static void setEnabled(boolean state) {
        enabled = state;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static Map<BlockPos, GlowInfo> getBlocks() {
        return blocksToRender;
    }

    public static void addBlock(BlockPos pos, float r, float g, float b, float a) {
        blocksToRender.put(pos.toImmutable(), new GlowInfo(r, g, b, a));
    }

    public static void removeBlock(BlockPos pos) {
        blocksToRender.remove(pos.toImmutable());
    }

    public static void clearAll() {
        blocksToRender.clear();
    }

    public static void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider) {
        if (!enabled || blocksToRender.isEmpty()) return;

        VertexConsumer buffer = vertexConsumerProvider.getBuffer(DEBUG_QUADS_WITH_NO_DEPTH_TEST);
        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();

        blocksToRender.forEach((pos, glow) -> {
            float x = pos.getX();
            float y = pos.getY();
            float z = pos.getZ();
            float offset = 0.001f;

            buffer.vertex(matrix4f, x, y + 1 + offset, z).color(glow.r(), glow.g(), glow.b(), glow.a());
            buffer.vertex(matrix4f, x, y + 1 + offset, z + 1).color(glow.r(), glow.g(), glow.b(), glow.a());
            buffer.vertex(matrix4f, x + 1, y + 1 + offset, z + 1).color(glow.r(), glow.g(), glow.b(), glow.a());
            buffer.vertex(matrix4f, x + 1, y + 1 + offset, z).color(glow.r(), glow.g(), glow.b(), glow.a());

            buffer.vertex(matrix4f, x, y - offset, z).color(glow.r(), glow.g(), glow.b(), glow.a());
            buffer.vertex(matrix4f, x + 1, y - offset, z).color(glow.r(), glow.g(), glow.b(), glow.a());
            buffer.vertex(matrix4f, x + 1, y - offset, z + 1).color(glow.r(), glow.g(), glow.b(), glow.a());
            buffer.vertex(matrix4f, x, y - offset, z + 1).color(glow.r(), glow.g(), glow.b(), glow.a());

            buffer.vertex(matrix4f, x, y, z - offset).color(glow.r(), glow.g(), glow.b(), glow.a());
            buffer.vertex(matrix4f, x, y + 1, z - offset).color(glow.r(), glow.g(), glow.b(), glow.a());
            buffer.vertex(matrix4f, x + 1, y + 1, z - offset).color(glow.r(), glow.g(), glow.b(), glow.a());
            buffer.vertex(matrix4f, x + 1, y, z - offset).color(glow.r(), glow.g(), glow.b(), glow.a());

            buffer.vertex(matrix4f, x, y, z + 1 + offset).color(glow.r(), glow.g(), glow.b(), glow.a());
            buffer.vertex(matrix4f, x + 1, y, z + 1 + offset).color(glow.r(), glow.g(), glow.b(), glow.a());
            buffer.vertex(matrix4f, x + 1, y + 1, z + 1 + offset).color(glow.r(), glow.g(), glow.b(), glow.a());
            buffer.vertex(matrix4f, x, y + 1, z + 1 + offset).color(glow.r(), glow.g(), glow.b(), glow.a());

            buffer.vertex(matrix4f, x - offset, y, z).color(glow.r(), glow.g(), glow.b(), glow.a());
            buffer.vertex(matrix4f, x - offset, y, z + 1).color(glow.r(), glow.g(), glow.b(), glow.a());
            buffer.vertex(matrix4f, x - offset, y + 1, z + 1).color(glow.r(), glow.g(), glow.b(), glow.a());
            buffer.vertex(matrix4f, x - offset, y + 1, z).color(glow.r(), glow.g(), glow.b(), glow.a());

            buffer.vertex(matrix4f, x + 1 + offset, y, z).color(glow.r(), glow.g(), glow.b(), glow.a());
            buffer.vertex(matrix4f, x + 1 + offset, y + 1, z).color(glow.r(), glow.g(), glow.b(), glow.a());
            buffer.vertex(matrix4f, x + 1 + offset, y + 1, z + 1).color(glow.r(), glow.g(), glow.b(), glow.a());
            buffer.vertex(matrix4f, x + 1 + offset, y, z + 1).color(glow.r(), glow.g(), glow.b(), glow.a());
        });
    }

    public record GlowInfo(float r, float g, float b, float a) {
    }
}