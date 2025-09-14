package me.yuugao.meyuugaosradio.block;

import me.yuugao.meyuugaosradio.entity.AbstractEnergyBlockEntity;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

public abstract class AbstractEnergyBlock extends BlockWithEntity {
    public static final EnumProperty<Direction> FACING = HorizontalFacingBlock.FACING;
    public static final EnumProperty<DirectionEnum> DIRECTION = EnumProperty.of("direction", DirectionEnum.class);
    public static final EnumProperty<EnergyStateEnum> ENERGY_STATE = EnumProperty.of("energy_state", EnergyStateEnum.class);

    protected AbstractEnergyBlock(Settings settings) {
        super(settings.nonOpaque().strength(2.0f));

        this.setDefaultState(stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(DIRECTION, DirectionEnum.SIDE)
                .with(ENERGY_STATE, EnergyStateEnum.DISABLED));
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, DIRECTION, ENERGY_STATE);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction side = ctx.getSide();
        DirectionEnum direction = side == Direction.UP ? DirectionEnum.UP :
                side == Direction.DOWN ? DirectionEnum.DOWN : DirectionEnum.SIDE;

        return getDefaultState()
                .with(FACING, ctx.getHorizontalPlayerFacing().getOpposite())
                .with(DIRECTION, direction)
                .with(ENERGY_STATE, EnergyStateEnum.DISABLED);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, getBlockEntityType(), (w, pos, s, be) -> {
            if (!w.isClient() && s.get(ENERGY_STATE).equals(EnergyStateEnum.ENABLED)) {
                if (!((AbstractEnergyBlockEntity) be).consumeEnergy()) {
                    onDisabled(w, pos, s);
                }
            }
        });
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof AbstractEnergyBlockEntity abstractEnergyBlockEntity) {
            NbtCompound nbt = itemStack.get(DataComponentTypes.CUSTOM_DATA).getNbt();
            if (nbt != null) {
                abstractEnergyBlockEntity.readNbt(nbt, world.getRegistryManager());
            }
        }
    }

    public void rotateBlock(World world, BlockPos pos, BlockState state) {
        if (state.contains(Properties.HORIZONTAL_FACING)) {
            Direction newFacing = state.get(FACING).rotateYClockwise();
            world.setBlockState(pos, state.with(FACING, newFacing));
        } else {
            world.setBlockState(pos, state.rotate(BlockRotation.CLOCKWISE_90));
        }
    }

    public void dropBlock(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        BlockEntity be = world.getBlockEntity(pos);
        ItemStack drop = new ItemStack(state.getBlock());

        if (be instanceof AbstractEnergyBlockEntity abstractEnergyBlockEntity) {
            NbtCompound nbt = new NbtCompound();
            abstractEnergyBlockEntity.writeNbt(nbt, world.getRegistryManager());
            drop.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        }

        Block.dropStack(world, pos, drop);
        world.removeBlock(pos, false);
    }

    protected Vec3d getVecDirection(World world, BlockPos pos) {
        Direction facing = world.getBlockState(pos).get(HorizontalFacingBlock.FACING);
        DirectionEnum direction = world.getBlockState(pos).get(AbstractEnergyBlock.DIRECTION);

        return new Vec3d(direction == DirectionEnum.SIDE ? facing.getOffsetX() : 0, direction == DirectionEnum.SIDE ? 0 :
                direction == DirectionEnum.UP ? 1 : -1, direction == DirectionEnum.SIDE ? facing.getOffsetZ() : 0).normalize();
    }

    public void onEnabled(World world, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state.with(ENERGY_STATE, EnergyStateEnum.ENABLED), Block.NOTIFY_ALL);
    }

    public void onDisabled(World world, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state.with(ENERGY_STATE, EnergyStateEnum.DISABLED), Block.NOTIFY_ALL);
    }

    protected abstract BlockEntityType<?> getBlockEntityType();

    public abstract void globalUnbind(PlayerEntity player, World world, BlockPos pos);

    public abstract void use(World world, BlockPos pos, ServerPlayerEntity player);

    public abstract void glow(World world, BlockPos pos, ServerPlayerEntity player);
}