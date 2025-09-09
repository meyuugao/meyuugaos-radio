package me.yuugao.meyuugaosradio.client.gui;

import static me.yuugao.meyuugaosradio.Constants.SPEAKER_VOLUME_MULTIPLIER;
import static me.yuugao.meyuugaosradio.client.gui.ModTextures.*;


import me.yuugao.meyuugaosradio.block.AbstractEnergyBlock;
import me.yuugao.meyuugaosradio.block.EnergyStateEnum;
import me.yuugao.meyuugaosradio.client.network.ClientNetworkManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;

public class SpeakerGuiScreen extends VolumeControlGuiScreen {
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
        long currentTime = System.currentTimeMillis();
        if (currentTime - volumeLastCursorTime > 500) {
            volumeCursorVisible = !volumeCursorVisible;
            volumeLastCursorTime = currentTime;
        }

        this.renderBackground(context);
        context.drawTexture(SPEAKER_GUI_TEXTURE, x, y, 0, 0, SPEAKER_GUI_WIDTH, SPEAKER_GUI_HEIGHT, SPEAKER_GUI_WIDTH, SPEAKER_GUI_HEIGHT);

        context.drawTexture(MinecraftClient.getInstance().world.getBlockState(pos).get(AbstractEnergyBlock.ENERGY_STATE).equals(EnergyStateEnum.ENABLED) ?
                        SPEAKER_BUTTON_ENABLED_TEXTURE : SPEAKER_BUTTON_DISABLED_TEXTURE,
                x + SPEAKER_BUTTON_X, y + SPEAKER_BUTTON_Y, 0, 0,
                SPEAKER_BUTTON_WIDTH, SPEAKER_BUTTON_HEIGHT, SPEAKER_BUTTON_WIDTH, SPEAKER_BUTTON_HEIGHT);

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        super.renderVolumeControls(context, textRenderer);
        super.render(context, mouseX, mouseY, delta);
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
    protected float getVolumeMultiplier() {
        return SPEAKER_VOLUME_MULTIPLIER;
    }

    @Override
    protected void sendVolumeUpdatePacket(float volume) {
        ClientNetworkManager.sendClientVolumeUpdatePacket(pos, volume, SPEAKER_VOLUME_MULTIPLIER);
    }
}