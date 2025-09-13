package me.yuugao.meyuugaosradio.item;

import me.yuugao.meyuugaosradio.block.AbstractEnergyBlock;
import me.yuugao.meyuugaosradio.network.ServerNetworkManager;

import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Consumer;

public class RemoteControllerItem extends Item {
    private final long capacity;
    private final int usage;

    @FunctionalInterface
    private interface BlockInteractionHandler {
        void handle(AbstractEnergyBlock block, World world, BlockPos pos, ServerPlayerEntity player);
    }

    public RemoteControllerItem(Settings settings, long capacity, int usage) {
        super(settings);
        this.capacity = capacity;
        this.usage = usage;
    }

    @Override
    public ItemStack getDefaultStack() {
        ItemStack stack = super.getDefaultStack();
        setupEnergyComponents(stack);
        return stack;
    }

    private void setupEnergyComponents(ItemStack stack) {
        NbtCompound nbt = new NbtCompound();
        nbt.putLong("Energy", 0);
        nbt.putLong("Capacity", capacity);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);

        long energy = getEnergy(stack);
        long capacity = getCapacity(stack);

        MutableText energyText = Text.literal("").append(Text.translatable("tooltip.energy").formatted(Formatting.GRAY)).append(Text.literal(String.format(" %d/%d E", energy, capacity)).formatted(Formatting.GOLD));

        MutableText usageText = Text.literal("").append(Text.translatable("tooltip.usage").formatted(Formatting.GRAY)).append(Text.literal(String.format(" %d E/", usage)).append(Text.translatable("tooltip.usage.usesuffix")).formatted(Formatting.GOLD));

        textConsumer.accept(energyText);
        textConsumer.accept(usageText);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        onRightClick(world, user);
        return ActionResult.SUCCESS;
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

            if (getEnergy(stack) < usage) {
                sendNotEnoughEnergyMessage(user);
                return;
            }

            removeEnergy(stack, usage);
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
    public boolean canMine(ItemStack stack, BlockState state, World world, BlockPos pos, LivingEntity user) {
        return false;
    }

    @Override
    public float getMiningSpeed(ItemStack stack, BlockState state) {
        return 0f;
    }

    public long getCapacity(ItemStack stack) {
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        return nbtComponent != null ? nbtComponent.copyNbt().getLong("Capacity").orElse(capacity) : capacity;
    }

    public long getEnergy(ItemStack stack) {
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        return nbtComponent != null ? nbtComponent.copyNbt().getLong("Energy").orElse(0L) : 0;
    }

    public void setEnergy(ItemStack stack, long energy) {
        NbtCompound nbt = new NbtCompound();
        nbt.putLong("Energy", Math.min(energy, getCapacity(stack)));
        nbt.putLong("Capacity", getCapacity(stack));
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public void addEnergy(ItemStack stack, long amount) {
        setEnergy(stack, getEnergy(stack) + amount);
    }

    public void removeEnergy(ItemStack stack, long amount) {
        setEnergy(stack, Math.max(0, getEnergy(stack) - amount));
    }
}