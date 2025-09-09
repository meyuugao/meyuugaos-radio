package me.yuugao.meyuugaosradio.client.gui;

import static me.yuugao.meyuugaosradio.Constants.RADIO_VOLUME_MULTIPLIER;
import static me.yuugao.meyuugaosradio.client.gui.ModTextures.*;


import me.yuugao.meyuugaosradio.block.AbstractEnergyBlock;
import me.yuugao.meyuugaosradio.block.EnergyStateEnum;
import me.yuugao.meyuugaosradio.client.network.ClientNetworkManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;

import org.lwjgl.glfw.GLFW;

public class RadioGuiScreen extends VolumeControlGuiScreen {
    private String streamUrl;
    private boolean textFieldFocused = false;
    private int cursorPosition = 0;
    private boolean cursorVisible = true;
    private int textOffset = 0;
    private long lastCursorTime;

    public RadioGuiScreen(BlockPos pos, String streamUrl, float volume) {
        super(pos, volume);
        this.streamUrl = streamUrl;
    }

    @Override
    protected void init() {
        super.init();
        lastCursorTime = System.currentTimeMillis();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            textFieldFocused = false;
            int buttonX = x + RADIO_BUTTON_X;
            int buttonY = y + RADIO_BUTTON_Y;

            if (mouseX >= buttonX && mouseX <= buttonX + RADIO_BUTTON_WIDTH &&
                    mouseY >= buttonY && mouseY <= buttonY + RADIO_BUTTON_HEIGHT) {
                if (this.streamUrl.endsWith("live.m3u8")) {
                    this.streamUrl = this.streamUrl.substring(0, this.streamUrl.length() - 9) + "aac_hifi.m3u8";
                }
                ClientNetworkManager.sendClientRadioStateSwitchPacket(this.pos, this.streamUrl);
                return true;
            }

            int textFieldScreenX = x + TEXT_FIELD_X1;
            int textFieldScreenY = y + TEXT_FIELD_Y1;

            if (mouseX >= textFieldScreenX && mouseX <= textFieldScreenX + TEXT_FIELD_BORDER_WIDTH &&
                    mouseY >= textFieldScreenY && mouseY <= textFieldScreenY + TEXT_FIELD_BORDER_HEIGHT) {
                textFieldFocused = true;
                cursorVisible = true;
                lastCursorTime = System.currentTimeMillis();
                cursorPosition = streamUrl.length();
                updateTextOffset();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (textFieldFocused) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                textFieldFocused = false;
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!streamUrl.isEmpty() && cursorPosition > 0) {
                    if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                        int wordStart = findWordStart(cursorPosition);
                        streamUrl = streamUrl.substring(0, wordStart) + streamUrl.substring(cursorPosition);
                        cursorPosition = wordStart;
                    } else {
                        streamUrl = streamUrl.substring(0, cursorPosition - 1) + streamUrl.substring(cursorPosition);
                        cursorPosition--;
                    }
                    updateTextOffset();
                }
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_DELETE) {
                if (cursorPosition < streamUrl.length()) {
                    if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                        int wordEnd = findWordEndForDelete(cursorPosition);
                        streamUrl = streamUrl.substring(0, cursorPosition) + streamUrl.substring(wordEnd);
                    } else {
                        streamUrl = streamUrl.substring(0, cursorPosition) + streamUrl.substring(cursorPosition + 1);
                    }
                    updateTextOffset();
                }
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_LEFT) {
                if (cursorPosition > 0) {
                    if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                        cursorPosition = findWordStart(cursorPosition);
                    } else {
                        cursorPosition--;
                    }
                    updateTextOffset();
                }
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                if (cursorPosition < streamUrl.length()) {
                    if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                        cursorPosition = findNextWordStart(cursorPosition);
                    } else {
                        cursorPosition++;
                    }
                    updateTextOffset();
                }
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_HOME) {
                cursorPosition = 0;
                updateTextOffset();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_END) {
                cursorPosition = streamUrl.length();
                updateTextOffset();
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
                if (clipboard != null && !clipboard.isEmpty()) {
                    streamUrl = streamUrl.substring(0, cursorPosition) + clipboard + streamUrl.substring(cursorPosition);
                    cursorPosition += clipboard.length();
                    updateTextOffset();
                }
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (textFieldFocused) {
            if ((modifiers & GLFW.GLFW_MOD_CONTROL) == 0) {
                streamUrl = streamUrl.substring(0, cursorPosition) + chr + streamUrl.substring(cursorPosition);
                cursorPosition++;
                updateTextOffset();
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCursorTime > 500) {
            cursorVisible = !cursorVisible;
            lastCursorTime = currentTime;
        }
        if (currentTime - volumeLastCursorTime > 500) {
            volumeCursorVisible = !volumeCursorVisible;
            volumeLastCursorTime = currentTime;
        }

        this.renderBackground(context);
        context.drawTexture(RADIO_GUI_TEXTURE, x, y, 0, 0, RADIO_GUI_WIDTH, RADIO_GUI_HEIGHT, RADIO_GUI_WIDTH, RADIO_GUI_HEIGHT);

        context.drawTexture(
                MinecraftClient.getInstance().world.getBlockState(pos).get(AbstractEnergyBlock.ENERGY_STATE).equals(EnergyStateEnum.ENABLED) ?
                        RADIO_BUTTON_ENABLED_TEXTURE : RADIO_BUTTON_DISABLED_TEXTURE,
                x + RADIO_BUTTON_X, y + RADIO_BUTTON_Y, 0, 0, RADIO_BUTTON_WIDTH, RADIO_BUTTON_HEIGHT, RADIO_BUTTON_WIDTH, RADIO_BUTTON_HEIGHT
        );

        context.drawTexture(RADIO_MAIN_TEXT_FIELD_TEXTURE, x + TEXT_FIELD_X, y + TEXT_FIELD_Y, 0, 0, TEXT_FIELD_WIDTH, TEXT_FIELD_HEIGHT, TEXT_FIELD_WIDTH, TEXT_FIELD_HEIGHT);

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int textX = x + TEXT_FIELD_X1 + 2;
        int textY = y + TEXT_FIELD_Y1;

        context.enableScissor(
                x + TEXT_FIELD_X1,
                y + TEXT_FIELD_Y1,
                x + TEXT_FIELD_X2,
                y + TEXT_FIELD_Y2
        );

        context.drawText(textRenderer, streamUrl, textX - textOffset, textY, 0xFFFFFF, false);

        if (textFieldFocused) {
            String textBeforeCursor = streamUrl.substring(0, cursorPosition);
            int textWidth = textRenderer.getWidth(textBeforeCursor);
            int cursorX = textX + textWidth - textOffset - 1;

            if (cursorVisible) {
                context.drawTexture(TEXT_FIELD_CURSOR_TEXTURE, cursorX, textY, 0, 0, TEXT_FIELD_CURSOR_WIDTH, TEXT_FIELD_CURSOR_HEIGHT, 1, 8);
            }
        }

        context.disableScissor();

        super.renderVolumeControls(context, textRenderer);
        super.render(context, mouseX, mouseY, delta);
    }

    private int findWordStart(int position) {
        if (position == 0) return 0;
        String text = streamUrl;
        int i = position - 1;

        if (i >= 0 && Character.isLetterOrDigit(text.charAt(i))) {
            while (i >= 0 && Character.isLetterOrDigit(text.charAt(i))) {
                i--;
            }
        } else {
            while (i >= 0 && !Character.isLetterOrDigit(text.charAt(i))) {
                i--;
            }
        }
        return i + 1;
    }

    private int findNextWordStart(int position) {
        if (position >= streamUrl.length()) return streamUrl.length();
        String text = streamUrl;
        int i = position;

        if (i < text.length() && Character.isLetterOrDigit(text.charAt(i))) {
            while (i < text.length() && Character.isLetterOrDigit(text.charAt(i))) {
                i++;
            }
        } else {
            while (i < text.length() && !Character.isLetterOrDigit(text.charAt(i))) {
                i++;
            }
        }
        return i;
    }

    private int findWordEndForDelete(int position) {
        if (position >= streamUrl.length()) return streamUrl.length();
        String text = streamUrl;
        int i = position;

        if (i < text.length() && Character.isLetterOrDigit(text.charAt(i))) {
            while (i < text.length() && Character.isLetterOrDigit(text.charAt(i))) {
                i++;
            }
        } else {
            while (i < text.length() && !Character.isLetterOrDigit(text.charAt(i))) {
                i++;
            }
        }
        return i;
    }

    private void updateTextOffset() {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        String textBeforeCursor = streamUrl.substring(0, cursorPosition);
        int textWidth = textRenderer.getWidth(textBeforeCursor);
        if (textWidth - textOffset > TEXT_FIELD_BORDER_WIDTH - 4) {
            textOffset = textWidth - TEXT_FIELD_BORDER_WIDTH + 4;
        }
        if (textWidth - textOffset < 0) {
            textOffset = textWidth;
        }
        textOffset = Math.max(0, textOffset);
    }

    @Override
    protected int getGuiWidth() {
        return RADIO_GUI_WIDTH;
    }

    @Override
    protected int getGuiHeight() {
        return RADIO_GUI_HEIGHT;
    }

    @Override
    protected int getVolumeSliderTrackX() {
        return RADIO_VOLUME_SLIDER_TRACK_X;
    }

    @Override
    protected int getVolumeSliderTrackY() {
        return RADIO_VOLUME_SLIDER_TRACK_Y;
    }

    @Override
    protected int getVolumeTextFieldX() {
        return RADIO_VOLUME_TEXT_FIELD_X;
    }

    @Override
    protected int getVolumeTextFieldY() {
        return RADIO_VOLUME_TEXT_FIELD_Y;
    }

    @Override
    protected float getVolumeMultiplier() {
        return RADIO_VOLUME_MULTIPLIER;
    }

    @Override
    protected void sendVolumeUpdatePacket(float volume) {
        ClientNetworkManager.sendClientVolumeUpdatePacket(pos, volume, RADIO_VOLUME_MULTIPLIER);
    }
}