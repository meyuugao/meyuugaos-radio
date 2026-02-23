package me.yuugao.meyuugaosradio.client.gui;

import static me.yuugao.meyuugaosradio.client.gui.ModTextures.*;


import me.yuugao.meyuugaosradio.block.AbstractEnergyBlock;
import me.yuugao.meyuugaosradio.block.EnergyStateEnum;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import org.lwjgl.glfw.GLFW;

public abstract class BaseGuiScreen extends Screen {
    protected final BlockPos pos;
    protected long currentTime;
    protected int x;
    protected int y;
    protected float volume;
    protected boolean volumeTextFieldFocused = false;
    protected int volumeCursorPosition = 0;
    protected boolean volumeCursorVisible = true;
    protected long volumeLastCursorTime;
    protected boolean volumeSliderDragging = false;
    protected int dragOffsetY;

    protected BaseGuiScreen(BlockPos pos, float volume) {
        super(Text.empty());

        this.pos = pos;
        this.volume = volume;
    }

    @Override
    protected void init() {
        super.init();

        x = (width - getGuiWidth()) / 2;
        y = (height - getGuiHeight()) / 2;
        volumeLastCursorTime = System.currentTimeMillis();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            volumeTextFieldFocused = false;

            int trackTop = y + getVolumeSliderTrackY() + 1;
            int trackBottom = y + getVolumeSliderTrackY() + 1 + VOLUME_SLIDER_TRACK_HEIGHT - VOLUME_SLIDER_THUMB_HEIGHT - 2;
            int thumbY = trackTop + (int) ((1.0f - volume) * (trackBottom - trackTop));
            int thumbX = x + getVolumeSliderTrackX() - 2;

            if (click.x() >= thumbX && click.x() <= thumbX + VOLUME_SLIDER_THUMB_WIDTH &&
                    click.y() >= thumbY && click.y() <= thumbY + VOLUME_SLIDER_THUMB_HEIGHT) {
                volumeSliderDragging = true;
                dragOffsetY = (int) click.y() - thumbY;
                return true;
            }

            int volumeTextFieldX = x + getVolumeTextFieldX();
            int volumeTextFieldY = y + getVolumeTextFieldY();

            if (click.x() >= volumeTextFieldX && click.x() <= volumeTextFieldX + VOLUME_TEXT_FIELD_WIDTH &&
                    click.y() >= volumeTextFieldY && click.y() <= volumeTextFieldY + VOLUME_TEXT_FIELD_HEIGHT) {
                volumeTextFieldFocused = true;
                volumeCursorVisible = true;
                volumeLastCursorTime = System.currentTimeMillis();
                volumeCursorPosition = String.valueOf((int) (volume * 100)).length();
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) {
            volumeSliderDragging = false;
        }

        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (volumeSliderDragging && click.button() == 0) {
            int trackTop = y + getVolumeSliderTrackY() + 1;
            int trackBottom = y + getVolumeSliderTrackY() + 1 + VOLUME_SLIDER_TRACK_HEIGHT - VOLUME_SLIDER_THUMB_HEIGHT - 2;

            float adjustedMouseY = (float) click.y() - dragOffsetY;
            float newVolume = 1.0f - Math.max(0.0f, Math.min(1.0f, (adjustedMouseY - trackTop) / (trackBottom - trackTop)));

            if (newVolume != volume) {
                volume = newVolume;
                sendVolumeUpdatePacket(volume);
            }

            return true;
        }

        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (volumeTextFieldFocused) {
            String volumeText = String.valueOf((int) (volume * 100));

            if (input.getKeycode() == GLFW.GLFW_KEY_ENTER || input.getKeycode() == GLFW.GLFW_KEY_KP_ENTER) {
                volumeTextFieldFocused = false;
                return true;
            } else if (input.getKeycode() == GLFW.GLFW_KEY_BACKSPACE) {
                if (volumeCursorPosition > 0) {
                    String newText = volumeText.substring(0, volumeCursorPosition - 1) + volumeText.substring(volumeCursorPosition);
                    volumeCursorPosition--;
                    updateVolumeFromText(newText);
                }
                return true;
            } else if (input.getKeycode() == GLFW.GLFW_KEY_DELETE) {
                if (volumeCursorPosition < volumeText.length()) {
                    String newText = volumeText.substring(0, volumeCursorPosition) + volumeText.substring(volumeCursorPosition + 1);
                    updateVolumeFromText(newText);
                }
                return true;
            } else if (input.getKeycode() == GLFW.GLFW_KEY_LEFT) {
                if ((input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
                    volumeCursorPosition = 0;
                } else if (volumeCursorPosition > 0) {
                    volumeCursorPosition--;
                }
                return true;
            } else if (input.getKeycode() == GLFW.GLFW_KEY_RIGHT) {
                if ((input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
                    volumeCursorPosition = volumeText.length();
                } else if (volumeCursorPosition < volumeText.length()) {
                    volumeCursorPosition++;
                }
                return true;
            } else if (input.getKeycode() == GLFW.GLFW_KEY_HOME) {
                volumeCursorPosition = 0;
                return true;
            } else if (input.getKeycode() == GLFW.GLFW_KEY_END) {
                volumeCursorPosition = volumeText.length();
                return true;
            }
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (volumeTextFieldFocused) {
            if ((input.modifiers() & GLFW.GLFW_MOD_CONTROL) == 0 && input.asString().matches("-?\\d+")) {
                String volumeText = String.valueOf((int) (volume * 100));
                String newText;

                if (volumeText.equals("0") && volumeCursorPosition == 1) {
                    newText = input.asString();
                } else {
                    newText = volumeText.substring(0, volumeCursorPosition) + input.asString() + volumeText.substring(volumeCursorPosition);
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

        return super.charTyped(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (MinecraftClient.getInstance().world == null) return;

        currentTime = System.currentTimeMillis();
        if (currentTime - volumeLastCursorTime > 500) {
            volumeCursorVisible = !volumeCursorVisible;
            volumeLastCursorTime = currentTime;
        }

        context.drawTexture(RenderPipelines.GUI_TEXTURED, getGuiTexture(), x, y, 0, 0, getGuiWidth(), getGuiHeight(), getGuiWidth(), getGuiHeight());

        context.drawTexture(RenderPipelines.GUI_TEXTURED,
                MinecraftClient.getInstance().world.getBlockState(pos).get(AbstractEnergyBlock.ENERGY_STATE).equals(EnergyStateEnum.ENABLED) ?
                        getEnabledButtonTexture() : getDisabledButtonTexture(),
                x + getButtonX(), y + getButtonY(),
                0, 0,
                getButtonWidth(), getButtonHeight(), getButtonWidth(), getButtonHeight());

        String volumeText = String.valueOf((int) (volume * 100));
        int volumeTextY = y + getVolumeTextFieldY() + 4;

        context.drawTexture(RenderPipelines.GUI_TEXTURED,
                VOLUME_TEXT_FIELD_TEXTURE,
                x + getVolumeTextFieldX(), y + getVolumeTextFieldY(),
                0, 0,
                VOLUME_TEXT_FIELD_WIDTH, VOLUME_TEXT_FIELD_HEIGHT,
                VOLUME_TEXT_FIELD_WIDTH, VOLUME_TEXT_FIELD_HEIGHT);

        context.drawText(textRenderer, volumeText, x + getVolumeTextFieldX() + 4, volumeTextY, 0xFFFFFFFF, false);

        if (volumeTextFieldFocused) {
            int textWidth = textRenderer.getWidth(volumeText.substring(0, volumeCursorPosition));
            int cursorX = x + getVolumeTextFieldX() + 4 + textWidth;

            if (volumeCursorVisible) {
                context.drawTexture(RenderPipelines.GUI_TEXTURED,
                        TEXT_FIELD_CURSOR_TEXTURE,
                        cursorX, volumeTextY,
                        0, 0,
                        TEXT_FIELD_CURSOR_WIDTH, TEXT_FIELD_CURSOR_HEIGHT - 1,
                        1, 7);
            }
        }

        context.drawTexture(RenderPipelines.GUI_TEXTURED,
                VOLUME_SLIDER_TRACK_TEXTURE,
                x + getVolumeSliderTrackX(), y + getVolumeSliderTrackY(),
                0, 0,
                VOLUME_SLIDER_TRACK_WIDTH, VOLUME_SLIDER_TRACK_HEIGHT,
                VOLUME_SLIDER_TRACK_WIDTH, VOLUME_SLIDER_TRACK_HEIGHT);

        int trackTop = y + getVolumeSliderTrackY() + 1;
        int trackBottom = y + getVolumeSliderTrackY() + 1 + VOLUME_SLIDER_TRACK_HEIGHT - VOLUME_SLIDER_THUMB_HEIGHT - 2;
        int thumbY = trackTop + (int) ((1.0f - volume) * (trackBottom - trackTop));

        context.drawTexture(RenderPipelines.GUI_TEXTURED,
                VOLUME_SLIDER_THUMB_TEXTURE,
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
    public boolean shouldPause() {
        return false;
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