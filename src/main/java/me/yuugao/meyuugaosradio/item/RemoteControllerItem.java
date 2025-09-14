package me.yuugao.meyuugaosradio.item;

import static me.yuugao.meyuugaosradio.Constants.REMOTE_CONTROLLER_ENERGY_CAPACITY;
import static me.yuugao.meyuugaosradio.Constants.REMOTE_CONTROLLER_ENERGY_USAGE;


import me.yuugao.meyuugaosradio.block.AbstractEnergyBlock;
import me.yuugao.meyuugaosradio.network.ServerNetworkManager;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;

public class RemoteControllerItem extends Item {
    private final EnergyItemHandler energyItemHandler;

    @FunctionalInterface
    private interface BlockInteractionHandler {
        void handle(AbstractEnergyBlock block, World world, BlockPos pos, ServerPlayerEntity player);
    }

    public RemoteControllerItem(Settings settings) {
        super(settings);

        this.energyItemHandler = new EnergyItemHandler(REMOTE_CONTROLLER_ENERGY_CAPACITY, REMOTE_CONTROLLER_ENERGY_USAGE);
    }

    @Override
    public ItemStack getDefaultStack() {
        ItemStack stack = super.getDefaultStack();
        energyItemHandler.setupEnergyComponents(stack);

        return stack;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);

        energyItemHandler.appendTooltip(stack, tooltip);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        onRightClick(world, user);

        return TypedActionResult.success(user.getStackInHand(hand));
    }

    public void onLeftClick(World world, PlayerEntity user) {
        handleInteraction(world, user, AbstractEnergyBlock::use, false);
    }

    private void onRightClick(World world, PlayerEntity user) {
        handleInteraction(world, user, AbstractEnergyBlock::glow, true);
    }

    private void handleInteraction(World world, PlayerEntity user, BlockInteractionHandler interactionHandler, boolean shouldResetRender) {
        if (!world.isClient() && !user.isSneaking() && user instanceof ServerPlayerEntity serverPlayerEntity) {
            ItemStack stack = user.getMainHandStack();

            if (energyItemHandler.getEnergy(stack) < energyItemHandler.getUsage(stack)) {
                sendNotEnoughEnergyMessage(user);

                return;
            }

            energyItemHandler.removeEnergy(stack, energyItemHandler.getUsage(stack));
            BlockHitResult hit = raycastFromPlayer(user);

            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = hit.getBlockPos();
                BlockState state = world.getBlockState(pos);

                if (state.getBlock() instanceof AbstractEnergyBlock abstractEnergyBlock) {
                    interactionHandler.handle(abstractEnergyBlock, world, pos, serverPlayerEntity);

                    return;
                }
            }

            if (shouldResetRender) {
                ServerNetworkManager.sendServerGlowClearPacket(serverPlayerEntity);
            }
        }
    }

    private void sendNotEnoughEnergyMessage(PlayerEntity user) {
        if (user instanceof ServerPlayerEntity serverPlayerEntity) {
            ServerNetworkManager.sendServerPlayerSendMessagePacket(serverPlayerEntity,
                    Text.translatable("error.notenoughenergy").formatted(Formatting.BOLD, Formatting.RED), true);
        }
    }

    private BlockHitResult raycastFromPlayer(PlayerEntity player) {
        Vec3d start = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0F);
        Vec3d end = start.add(look.multiply(64));

        RaycastContext context = new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player);

        return player.getWorld().raycast(context);
    }

    @Override
    public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
        return false;
    }

    @Override
    public float getMiningSpeed(ItemStack stack, BlockState state) {
        return 0f;
    }

    public EnergyItemHandler getEnergyItemHandler() {
        return energyItemHandler;
    }
}