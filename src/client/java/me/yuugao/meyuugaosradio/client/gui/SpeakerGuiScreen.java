
package me.yuugao.meyuugaosradio.client.gui;

import static me.yuugao.meyuugaosradio.Constants.SPEAKER_VOLUME_MULTIPLIER;
import static me.yuugao.meyuugaosradio.client.gui.ModTextures.*;


import me.yuugao.meyuugaosradio.client.network.ClientNetworkManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class SpeakerGuiScreen extends BaseGuiScreen {
    public SpeakerGuiScreen(BlockPos pos, float volume) {
        super(pos, volume);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int buttonX = x + SPEAKER_BUTTON_X;
            int buttonY = y + SPEAKER_BUTTON_Y;

            if (mouseX >= buttonX && mouseX <= buttonX + SPEAKER_BUTTON_WIDTH &&
                    mouseY >= buttonY && mouseY <= buttonY + SPEAKER_BUTTON_HEIGHT) {
                ClientNetworkManager.sendClientSpeakerStateSwitchPacket(this.pos);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context);
    }

    @Override
    protected Identifier getGuiTexture() {
        return SPEAKER_GUI_TEXTURE;
    }

    @Override
    protected int getGuiWidth() {
        return SPEAKER_GUI_WIDTH;
    }

    @Override
    protected int getGuiHeight() {
        return SPEAKER_GUI_HEIGHT;
    }

    @Override
    protected Identifier getEnabledButtonTexture() {
        return ENABLED_SPEAKER_BUTTON_TEXTURE;
    }

    @Override
    protected Identifier getDisabledButtonTexture() {
        return DISABLED_SPEAKER_BUTTON_TEXTURE;
    }

    @Override
    protected int getButtonX() {
        return SPEAKER_BUTTON_X;
    }

    @Override
    protected int getButtonY() {
        return SPEAKER_BUTTON_Y;
    }

    @Override
    protected int getButtonWidth() {
        return SPEAKER_BUTTON_WIDTH;
    }

    @Override
    protected int getButtonHeight() {
        return SPEAKER_BUTTON_HEIGHT;
    }

    @Override
    protected int getVolumeSliderTrackX() {
        return SPEAKER_VOLUME_SLIDER_TRACK_X;
    }

    @Override
    protected int getVolumeSliderTrackY() {
        return SPEAKER_VOLUME_SLIDER_TRACK_Y;
    }

    @Override
    protected int getVolumeTextFieldX() {
        return SPEAKER_VOLUME_TEXT_FIELD_X;
    }

    @Override
    protected int getVolumeTextFieldY() {
        return SPEAKER_VOLUME_TEXT_FIELD_Y;
    }

    @Override
    protected void sendVolumeUpdatePacket(float volume) {
        ClientNetworkManager.sendClientVolumeUpdatePacket(pos, volume, SPEAKER_VOLUME_MULTIPLIER);
    }
}