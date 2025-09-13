package me.yuugao.meyuugaosradio.client.render;

import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;

import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;

public class BlockGlowRenderer {
    private static final Map<BlockPos, GlowInfo> blocksToRender = new HashMap<>();
    private static VertexBuffer vertexBuffer;
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
        rebuildVertexBuffer();
    }

    public static void removeBlock(BlockPos pos) {
        blocksToRender.remove(pos.toImmutable());
        rebuildVertexBuffer();
    }

    public static void clearAll() {
        blocksToRender.clear();
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }

    public static void render(MatrixStack matrices, Matrix4f projectionMatrix) {
        if (!enabled || vertexBuffer == null) return;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();

        vertexBuffer.bind();
        vertexBuffer.draw(matrices.peek().getPositionMatrix(), projectionMatrix, RenderSystem.getShader());
        VertexBuffer.unbind();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void rebuildVertexBuffer() {
        if (vertexBuffer != null) {
            vertexBuffer.close();
        }

        if (blocksToRender.isEmpty()) {
            vertexBuffer = null;
            return;
        }

        vertexBuffer = new VertexBuffer(GlUsage.STATIC_WRITE);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        blocksToRender.forEach((pos, glow) -> renderBlockFaces(buffer, pos, glow.r(), glow.g(), glow.b(), glow.a()));

        vertexBuffer.bind();
        vertexBuffer.upload(buffer.end());
        VertexBuffer.unbind();
    }

    private static void renderBlockFaces(BufferBuilder buffer, BlockPos pos, float r, float g, float b, float a) {
        float x = pos.getX();
        float y = pos.getY();
        float z = pos.getZ();
        float offset = 0.001f;

        buffer.vertex(x, y + 1 + offset, z).color(r, g, b, a);
        buffer.vertex(x, y + 1 + offset, z + 1).color(r, g, b, a);
        buffer.vertex(x + 1, y + 1 + offset, z + 1).color(r, g, b, a);
        buffer.vertex(x + 1, y + 1 + offset, z).color(r, g, b, a);

        buffer.vertex(x, y - offset, z).color(r, g, b, a);
        buffer.vertex(x + 1, y - offset, z).color(r, g, b, a);
        buffer.vertex(x + 1, y - offset, z + 1).color(r, g, b, a);
        buffer.vertex(x, y - offset, z + 1).color(r, g, b, a);

        buffer.vertex(x, y, z - offset).color(r, g, b, a);
        buffer.vertex(x, y + 1, z - offset).color(r, g, b, a);
        buffer.vertex(x + 1, y + 1, z - offset).color(r, g, b, a);
        buffer.vertex(x + 1, y, z - offset).color(r, g, b, a);

        buffer.vertex(x, y, z + 1 + offset).color(r, g, b, a);
        buffer.vertex(x + 1, y, z + 1 + offset).color(r, g, b, a);
        buffer.vertex(x + 1, y + 1, z + 1 + offset).color(r, g, b, a);
        buffer.vertex(x, y + 1, z + 1 + offset).color(r, g, b, a);

        buffer.vertex(x - offset, y, z).color(r, g, b, a);
        buffer.vertex(x - offset, y, z + 1).color(r, g, b, a);
        buffer.vertex(x - offset, y + 1, z + 1).color(r, g, b, a);
        buffer.vertex(x - offset, y + 1, z).color(r, g, b, a);

        buffer.vertex(x + 1 + offset, y, z).color(r, g, b, a);
        buffer.vertex(x + 1 + offset, y + 1, z).color(r, g, b, a);
        buffer.vertex(x + 1 + offset, y + 1, z + 1).color(r, g, b, a);
        buffer.vertex(x + 1 + offset, y, z + 1).color(r, g, b, a);
    }

    public record GlowInfo(float r, float g, float b, float a) {
    }
}