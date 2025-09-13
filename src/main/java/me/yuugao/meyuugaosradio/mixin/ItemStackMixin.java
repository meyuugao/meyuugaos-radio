package me.yuugao.meyuugaosradio.mixin;

import static me.yuugao.meyuugaosradio.Constants.MEYUUGAOSRADIO_REMOTE_CONTROLLER_ID;
import static me.yuugao.meyuugaosradio.Constants.TECHREBORN_WRENCH_ID;


import me.yuugao.meyuugaosradio.Radio;
import me.yuugao.meyuugaosradio.block.AbstractEnergyBlock;
import me.yuugao.meyuugaosradio.block.DirectionEnum;
import me.yuugao.meyuugaosradio.block.RadioBlock;
import me.yuugao.meyuugaosradio.entity.RadioBlockEntity;
import me.yuugao.meyuugaosradio.entity.SpeakerBlockEntity;
import me.yuugao.meyuugaosradio.item.EnergyItemHandler;
import me.yuugao.meyuugaosradio.item.RemoteControllerItem;
import me.yuugao.meyuugaosradio.sound.ServerHlsAudioManager;

import net.minecraft.block.Block;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void useOnBlock(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (!context.getWorld().isClient()) {
            Block block = context.getWorld().getBlockState(context.getBlockPos()).getBlock();
            if (context.getPlayer() != null && block instanceof AbstractEnergyBlock abstractEnergyBlock) {
                ItemStack itemStack = context.getPlayer().getMainHandStack();
                if (itemStack.isOf(Registries.ITEM.get(TECHREBORN_WRENCH_ID))) {
                    if (context.getPlayer().isSneaking()) {
                        abstractEnergyBlock.dropBlock(context.getWorld(), context.getBlockPos());
                        abstractEnergyBlock.globalUnbind(context.getPlayer(), context.getWorld(), context.getBlockPos());
                        context.getWorld().playSound(null, context.getBlockPos(), Radio.BLOCK_DISMANTLE, SoundCategory.BLOCKS, 0.5f, 1f);
                    } else {
                        abstractEnergyBlock.rotateBlock(context.getWorld(), context.getBlockPos(), context.getWorld().getBlockState(context.getBlockPos()));
                        updateSoundSourceDirection(context);
                    }
                    cir.setReturnValue(ActionResult.SUCCESS);
                } else if (itemStack.isOf(Registries.ITEM.get(MEYUUGAOSRADIO_REMOTE_CONTROLLER_ID))) {
                    RemoteControllerItem remoteControllerItem = (RemoteControllerItem) itemStack.getItem();
                    if (block instanceof RadioBlock && context.getPlayer().isSneaking()) {
                        BlockEntity blockEntity = context.getWorld().getBlockEntity(context.getBlockPos());
                        if (blockEntity instanceof RadioBlockEntity radioBlockEntity) {
                            EnergyItemHandler energyItemHandler = remoteControllerItem.getEnergyHandler();
                            long toTransfer = Math.min(energyItemHandler.getCapacity(itemStack) - energyItemHandler.getEnergy(itemStack), radioBlockEntity.getAmount());
                            radioBlockEntity.setEnergy(radioBlockEntity.getAmount() - toTransfer);
                            energyItemHandler.addEnergy(itemStack, toTransfer);
                        }
                    }
                    cir.setReturnValue(ActionResult.SUCCESS);
                }
            }
        }
    }

    @Unique
    private void updateSoundSourceDirection(ItemUsageContext context) {
        String streamUrl = null;
        BlockEntity blockEntity = context.getWorld().getBlockEntity(context.getBlockPos());
        if (blockEntity instanceof RadioBlockEntity radioBlockEntity) {
            streamUrl = radioBlockEntity.getStreamUrl();
        } else if (blockEntity instanceof SpeakerBlockEntity speakerBlockEntity) {
            if (speakerBlockEntity.getRadioPos() != null) {
                BlockEntity blockEntity1 = context.getWorld().getBlockEntity(speakerBlockEntity.getRadioPos());
                if (blockEntity1 instanceof RadioBlockEntity radioBlockEntity1) {
                    streamUrl = radioBlockEntity1.getStreamUrl();
                }
            }
        }
        if (streamUrl != null) {
            Direction facing = context.getWorld().getBlockState(context.getBlockPos()).get(HorizontalFacingBlock.FACING);
            DirectionEnum direction = context.getWorld().getBlockState(context.getBlockPos()).get(AbstractEnergyBlock.DIRECTION);
            ServerHlsAudioManager.updateSoundSourceDirection(streamUrl, context.getBlockPos(), new Vec3d(direction == DirectionEnum.SIDE ? facing.getOffsetX() : 0, direction == DirectionEnum.SIDE ? 0 : direction == DirectionEnum.UP ? 1 : -1, direction == DirectionEnum.SIDE ? facing.getOffsetZ() : 0).normalize(), context.getWorld().getRegistryKey());
        }
    }
}