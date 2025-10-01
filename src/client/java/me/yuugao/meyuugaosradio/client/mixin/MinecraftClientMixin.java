package me.yuugao.meyuugaosradio.client.mixin;

import me.yuugao.meyuugaosradio.client.network.ClientNetworkManager;
import me.yuugao.meyuugaosradio.item.RemoteControllerItem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "doAttack", at = @At("HEAD"))
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        ClientPlayerEntity player = client.player;

        if (player != null) {
            ItemStack stack = player.getMainHandStack();
            if (stack.getItem() instanceof RemoteControllerItem) {
                ClientNetworkManager.sendClientRemotecontrollerOnClickPacket();
            }
        }
    }
}