package me.yuugao.meyuugaosradio.client.gui;

import static me.yuugao.meyuugaosradio.client.gui.ModTextures.*;


import me.yuugao.meyuugaosradio.block.AbstractEnergyBlock;
import me.yuugao.meyuugaosradio.block.EnergyStateEnum;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;

public abstract class BaseGuiScreen extends Screen {
    protected final BlockPos pos;
    protected long currentTime;
    protected int x;
    protected int y;
    protected float volume;
    protected boolean volumeTextFieldFocused;
    protected int volumeCursorPosition;
    protected boolean volumeCursorVisible;
    protected long volumeLastCursorTime;
    protected boolean volumeSliderDragging;
    protected int dragOffsetY;

    protected BaseGuiScreen(BlockPos pos, float volume) {
        super(new LiteralText(StringUtils.EMPTY));

        this.pos = pos;
        this.volume = volume;
    }

    @Override
    protected void init() {
        super.init();

        x = (width - getGuiWidth()) / 2;
        y = (height - getGuiHeight()) / 2;
        volumeLastCursorTime = System.currentTimeMillis();
        volumeTextFieldFocused = false;
        volumeCursorPosition = 0;
        volumeCursorVisible = true;
        volumeSliderDragging = false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            volumeTextFieldFocused = false;

            int trackTop = y + getVolumeSliderTrackY() + 1;
            int trackBottom = y + getVolumeSliderTrackY() + 1 + VOLUME_SLIDER_TRACK_HEIGHT - VOLUME_SLIDER_THUMB_HEIGHT - 2;
            int thumbY = trackTop + (int) ((1.0f - volume) * (trackBottom - trackTop));
            int thumbX = x + getVolumeSliderTrackX() - 2;

            if (mouseX >= thumbX && mouseX <= thumbX + VOLUME_SLIDER_THUMB_WIDTH &&
                    mouseY >= thumbY && mouseY <= thumbY + VOLUME_SLIDER_THUMB_HEIGHT) {
                volumeSliderDragging = true;
                dragOffsetY = (int) mouseY - thumbY;
                return true;
            }

            int volumeTextFieldX = x + getVolumeTextFieldX();
            int volumeTextFieldY = y + getVolumeTextFieldY();

            if (mouseX >= volumeTextFieldX && mouseX <= volumeTextFieldX + VOLUME_TEXT_FIELD_WIDTH &&
                    mouseY >= volumeTextFieldY && mouseY <= volumeTextFieldY + VOLUME_TEXT_FIELD_HEIGHT) {
                volumeTextFieldFocused = true;
                volumeCursorVisible = true;
                volumeLastCursorTime = System.currentTimeMillis();
                volumeCursorPosition = String.valueOf((int) (volume * 100)).length();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            volumeSliderDragging = false;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (volumeSliderDragging && button == 0) {
            int trackTop = y + getVolumeSliderTrackY() + 1;
            int trackBottom = y + getVolumeSliderTrackY() + 1 + VOLUME_SLIDER_TRACK_HEIGHT - VOLUME_SLIDER_THUMB_HEIGHT - 2;

            float adjustedMouseY = (float) mouseY - dragOffsetY;
            float newVolume = 1.0f - Math.max(0.0f, Math.min(1.0f, (adjustedMouseY - trackTop) / (trackBottom - trackTop)));

            if (newVolume != volume) {
                volume = newVolume;
                sendVolumeUpdatePacket(volume);
            }

            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (volumeTextFieldFocused) {
            String volumeText = String.valueOf((int) (volume * 100));

            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                volumeTextFieldFocused = false;
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (volumeCursorPosition > 0) {
                    String newText = volumeText.substring(0, volumeCursorPosition - 1) + volumeText.substring(volumeCursorPosition);
                    volumeCursorPosition--;
                    updateVolumeFromText(newText);
                }
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_DELETE) {
                if (volumeCursorPosition < volumeText.length()) {
                    String newText = volumeText.substring(0, volumeCursorPosition) + volumeText.substring(volumeCursorPosition + 1);
                    updateVolumeFromText(newText);
                }
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_LEFT) {
                if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                    volumeCursorPosition = 0;
                } else if (volumeCursorPosition > 0) {
                    volumeCursorPosition--;
                }
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                    volumeCursorPosition = volumeText.length();
                } else if (volumeCursorPosition < volumeText.length()) {
                    volumeCursorPosition++;
                }
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_HOME) {
                volumeCursorPosition = 0;
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_END) {
                volumeCursorPosition = volumeText.length();
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (volumeTextFieldFocused) {
            if ((modifiers & GLFW.GLFW_MOD_CONTROL) == 0 && String.valueOf(chr).matches("-?\\d+")) {
                String volumeText = String.valueOf((int) (volume * 100));
                String newText;

                if (volumeText.equals("0") && volumeCursorPosition == 1) {
                    newText = String.valueOf(chr);
                } else {
                    newText = volumeText.substring(0, volumeCursorPosition) + chr + volumeText.substring(volumeCursorPosition);
                    if (newText.length() <= 3) {
                        volumeCursorPosition++;
                    }
                }

                if (newText.length() <= 3) {
                    updateVolumeFromText(newText);
                }

                return true;
            }
        }

        return super.charTyped(chr, modifiers);
    }

    public void render(MatrixStack matrixStack) {
        if (MinecraftClient.getInstance().world == null) return;

        this.renderBackground(matrixStack);

        currentTime = System.currentTimeMillis();
        if (currentTime - volumeLastCursorTime > 500) {
            volumeCursorVisible = !volumeCursorVisible;
            volumeLastCursorTime = currentTime;
        }

        RenderSystem.setShaderTexture(0, getGuiTexture());
        drawTexture(matrixStack, x, y, 0, 0, getGuiWidth(), getGuiHeight(), getGuiWidth(), getGuiHeight());

        RenderSystem.setShaderTexture(0,
                MinecraftClient.getInstance().world.getBlockState(pos).get(AbstractEnergyBlock.ENERGY_STATE).equals(EnergyStateEnum.ENABLED) ?
                        getEnabledButtonTexture() : getDisabledButtonTexture());
        drawTexture(matrixStack, x + getButtonX(), y + getButtonY(), 0, 0,
                getButtonWidth(), getButtonHeight(), getButtonWidth(), getButtonHeight());

        String volumeText = String.valueOf((int) (volume * 100));
        int volumeTextY = y + getVolumeTextFieldY() + 4;

        RenderSystem.setShaderTexture(0, VOLUME_TEXT_FIELD_TEXTURE);
        drawTexture(matrixStack, x + getVolumeTextFieldX(), y + getVolumeTextFieldY(), 0, 0,
                VOLUME_TEXT_FIELD_WIDTH, VOLUME_TEXT_FIELD_HEIGHT, VOLUME_TEXT_FIELD_WIDTH, VOLUME_TEXT_FIELD_HEIGHT);

        drawTextWithShadow(matrixStack, textRenderer, new LiteralText(volumeText), x + getVolumeTextFieldX() + 4, volumeTextY, 0xFFFFFF);

        if (volumeTextFieldFocused) {
            int textWidth = textRenderer.getWidth(volumeText.substring(0, volumeCursorPosition));
            int cursorX = x + getVolumeTextFieldX() + 4 + textWidth;

            if (volumeCursorVisible) {
                RenderSystem.setShaderTexture(0, TEXT_FIELD_CURSOR_TEXTURE);
                drawTexture(matrixStack, cursorX, volumeTextY, 0, 0,
                        TEXT_FIELD_CURSOR_WIDTH, TEXT_FIELD_CURSOR_HEIGHT - 1, 1, 7);
            }
        }

        RenderSystem.setShaderTexture(0, VOLUME_SLIDER_TRACK_TEXTURE);
        drawTexture(matrixStack,
                x + getVolumeSliderTrackX(), y + getVolumeSliderTrackY(),
                0, 0,
                VOLUME_SLIDER_TRACK_WIDTH, VOLUME_SLIDER_TRACK_HEIGHT,
                VOLUME_SLIDER_TRACK_WIDTH, VOLUME_SLIDER_TRACK_HEIGHT);

        int trackTop = y + getVolumeSliderTrackY() + 1;
        int trackBottom = y + getVolumeSliderTrackY() + 1 + VOLUME_SLIDER_TRACK_HEIGHT - VOLUME_SLIDER_THUMB_HEIGHT - 2;
        int thumbY = trackTop + (int) ((1.0f - volume) * (trackBottom - trackTop));

        RenderSystem.setShaderTexture(0, VOLUME_SLIDER_THUMB_TEXTURE);
        drawTexture(matrixStack,
                x + getVolumeSliderTrackX() - 2, thumbY,
                0, 0,
                VOLUME_SLIDER_THUMB_WIDTH, VOLUME_SLIDER_THUMB_HEIGHT,
                VOLUME_SLIDER_THUMB_WIDTH, VOLUME_SLIDER_THUMB_HEIGHT);
    }

    protected void updateVolumeFromText(String text) {
        if (text.isEmpty()) {
            volume = 0.0f;
        } else {
            int value = Integer.parseInt(text);
            volume = Math.max(0, Math.min(100, value)) / 100.0f;
        }

        if (volume == 0.0f) {
            volumeCursorPosition = 1;
        }

        sendVolumeUpdatePacket(volume);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected void enableScissor(int x1, int y1, int x2, int y2) {
        Window window = MinecraftClient.getInstance().getWindow();
        int i = window.getFramebufferHeight();
        double d = window.getScaleFactor();
        double e = (double) x1 * d;
        double f = (double) i - (double) y2 * d;
        double g = (double) (x2 - x1) * d;
        double h = (double) (y2 - y1) * d;
        RenderSystem.enableScissor((int) e, (int) f, Math.max(0, (int) g), Math.max(0, (int) h));
    }

    protected void disableScissor() {
        RenderSystem.disableScissor();
    }

    protected abstract Identifier getGuiTexture();

    protected abstract int getGuiWidth();

    protected abstract int getGuiHeight();

    protected abstract Identifier getEnabledButtonTexture();

    protected abstract Identifier getDisabledButtonTexture();

    protected abstract int getButtonX();

    protected abstract int getButtonY();

    protected abstract int getButtonWidth();

    protected abstract int getButtonHeight();

    protected abstract int getVolumeSliderTrackX();

    protected abstract int getVolumeSliderTrackY();

    protected abstract int getVolumeTextFieldX();

    protected abstract int getVolumeTextFieldY();

    protected abstract void sendVolumeUpdatePacket(float volume);
}