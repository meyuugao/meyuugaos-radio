package me.yuugao.meyuugaosradio.item;

import me.yuugao.meyuugaosradio.block.AbstractEnergyBlock;
import me.yuugao.meyuugaosradio.network.ServerNetworkManager;

import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
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

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RemoteControllerItem extends Item {
    @FunctionalInterface
    private interface BlockInteractionHandler {
        void handle(AbstractEnergyBlock block, World world, BlockPos pos, ServerPlayerEntity player);
    }

    private static final long CAPACITY = 10_000L;
    private static final long USAGE = 10L;

    public RemoteControllerItem(Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack getDefaultStack() {
        ItemStack stack = super.getDefaultStack();
        setupEnergyNbt(stack);
        return stack;
    }

    private void setupEnergyNbt(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putLong("Energy", 0);
        nbt.putLong("Capacity", CAPACITY);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        long energy = getEnergy(stack);
        long capacity = getCapacity(stack);

        MutableText energyText = Text.literal("").append(Text.translatable("tooltip.energy").formatted(Formatting.GRAY)).append(Text.literal(String.format(" %d/%d E", energy, capacity)).formatted(Formatting.GOLD));

        MutableText usageText = Text.literal("").append(Text.translatable("tooltip.usage").formatted(Formatting.GRAY)).append(Text.literal(String.format(" %d E/", USAGE)).append(Text.translatable("tooltip.usage.usesuffix")).formatted(Formatting.GOLD));

        tooltip.add(energyText);
        tooltip.add(usageText);
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

            if (getEnergy(stack) < USAGE) {
                sendNotEnoughEnergyMessage(user);
                return;
            }

            removeEnergy(stack, USAGE);
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
            ServerNetworkManager.sendServerPlayerSendMessagePacket(serverPlayerEntity, Text.translatable("error.notenoughenergy").formatted(Formatting.BOLD, Formatting.RED), true);
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
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        return 0f;
    }

    public long getCapacity(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains("Capacity") ? nbt.getLong("Capacity") : CAPACITY;
    }

    public long getEnergy(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains("Energy") ? nbt.getLong("Energy") : 0;
    }

    public void setEnergy(ItemStack stack, long energy) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putLong("Energy", Math.min(energy, getCapacity(stack)));
    }

    public void addEnergy(ItemStack stack, long amount) {
        setEnergy(stack, getEnergy(stack) + amount);
    }

    public void removeEnergy(ItemStack stack, long amount) {
        setEnergy(stack, Math.max(0, getEnergy(stack) - amount));
    }
}