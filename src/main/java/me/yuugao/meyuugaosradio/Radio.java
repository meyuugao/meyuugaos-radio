package me.yuugao.meyuugaosradio;

import static me.yuugao.meyuugaosradio.Constants.*;


import me.yuugao.meyuugaosradio.block.RadioBlock;
import me.yuugao.meyuugaosradio.block.SpeakerBlock;
import me.yuugao.meyuugaosradio.entity.RadioBlockEntity;
import me.yuugao.meyuugaosradio.entity.SpeakerBlockEntity;
import me.yuugao.meyuugaosradio.events.ServerEventsManager;
import me.yuugao.meyuugaosradio.item.EnergyBlockItem;
import me.yuugao.meyuugaosradio.item.EnergyItemHandler;
import me.yuugao.meyuugaosradio.item.RemoteControllerItem;
import me.yuugao.meyuugaosradio.network.ServerNetworkManager;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameRules;

import java.util.function.Function;

import team.reborn.energy.api.EnergyStorage;

public class Radio implements ModInitializer {
    private Block RADIO_BLOCK;
    private Block SPEAKER_BLOCK;

    private RemoteControllerItem REMOTE_CONTROLLER_ITEM;

    public static BlockEntityType<RadioBlockEntity> RADIO_BLOCK_ENTITY;
    public static BlockEntityType<SpeakerBlockEntity> SPEAKER_BLOCK_ENTITY;

    public static GameRules.Key<GameRules.IntRule> RADIO_CONNECT_RADIUS;

    public static SoundEvent BLOCK_DISMANTLE;

    @Override
    public void onInitialize() {
        registerBlocks();
        registerItems();
        registerBlockEntities();
        registerEnergyStorages();
        registerGameRules();
        registerSounds();
        ServerNetworkManager.initialize();
        ServerEventsManager.initialize();

        SERVER_LOGGER.info("[SERVER] MeYuugaos Radio mod initialized!");
    }

    private void registerBlocks() {
        RADIO_BLOCK = registerBlock(RADIO_BLOCK_ID, RadioBlock::new, AbstractBlock.Settings.of(Material.METAL));
        SPEAKER_BLOCK = registerBlock(SPEAKER_BLOCK_ID, SpeakerBlock::new, AbstractBlock.Settings.of(Material.METAL));
    }

    private void registerItems() {
        registerItem(RADIO_BLOCK_ID, settings ->
                        new EnergyBlockItem(RADIO_BLOCK, settings, RADIO_ENERGY_CAPACITY, RADIO_ENERGY_USAGE),
                new Item.Settings().maxCount(DEFAULT_STACK_SIZE).group(ItemGroup.REDSTONE));
        registerItem(SPEAKER_BLOCK_ID, settings ->
                        new EnergyBlockItem(SPEAKER_BLOCK, settings, SPEAKER_ENERGY_CAPACITY, SPEAKER_ENERGY_USAGE),
                new Item.Settings().maxCount(DEFAULT_STACK_SIZE).group(ItemGroup.REDSTONE));

        REMOTE_CONTROLLER_ITEM = (RemoteControllerItem) registerItem(REMOTE_CONTROLLER_ID, RemoteControllerItem::new,
                new Item.Settings().maxCount(REMOTE_CONTROLLER_STACK_SIZE).group(ItemGroup.REDSTONE));
        registerItem(ELECTRONIC_CIRCUIT_ID, Item::new, new Item.Settings().maxCount(DEFAULT_STACK_SIZE).group(ItemGroup.REDSTONE));
        registerItem(BATTERY_ID, Item::new, new Item.Settings().maxCount(DEFAULT_STACK_SIZE).group(ItemGroup.REDSTONE));
        registerItem(SMALL_BATTERY_ID, Item::new, new Item.Settings().maxCount(DEFAULT_STACK_SIZE).group(ItemGroup.REDSTONE));
        registerItem(ANTENNA_ID, Item::new, new Item.Settings().maxCount(DEFAULT_STACK_SIZE).group(ItemGroup.REDSTONE));
        registerItem(SMALL_MEMBRANE_ID, Item::new, new Item.Settings().maxCount(DEFAULT_STACK_SIZE).group(ItemGroup.REDSTONE));
        registerItem(MEMBRANE_ID, Item::new, new Item.Settings().maxCount(DEFAULT_STACK_SIZE).group(ItemGroup.REDSTONE));
    }

    private void registerBlockEntities() {
        RADIO_BLOCK_ENTITY = registerBlockEntity(RADIO_BLOCK_ENTITY_ID, abstractBlock ->
                FabricBlockEntityTypeBuilder.create(RadioBlockEntity::new, RADIO_BLOCK).build(), RADIO_BLOCK);
        SPEAKER_BLOCK_ENTITY = registerBlockEntity(SPEAKER_BLOCK_ENTITY_ID, abstractBlock ->
                FabricBlockEntityTypeBuilder.create(SpeakerBlockEntity::new, SPEAKER_BLOCK).build(), SPEAKER_BLOCK);
    }

    private Block registerBlock(String path, Function<AbstractBlock.Settings, Block> factory, AbstractBlock.Settings settings) {
        RegistryKey<Block> registryKey = RegistryKey.of(Registry.BLOCK_KEY, new Identifier(MOD_ID, path));
        return Registry.register(Registry.BLOCK, registryKey, factory.apply(settings));
    }

    private Item registerItem(String path, Function<Item.Settings, Item> factory, Item.Settings settings) {
        RegistryKey<Item> registryKey = RegistryKey.of(Registry.ITEM_KEY, new Identifier(MOD_ID, path));
        return Registry.register(Registry.ITEM, registryKey, factory.apply(settings));
    }

    private <T extends BlockEntity> BlockEntityType<T> registerBlockEntity(String path, Function<AbstractBlock, BlockEntityType<T>> factory, AbstractBlock block) {
        RegistryKey<BlockEntityType<?>> registryKey = RegistryKey.of(Registry.BLOCK_ENTITY_TYPE_KEY, new Identifier(MOD_ID, path));
        return Registry.register(Registry.BLOCK_ENTITY_TYPE, registryKey, factory.apply(block));
    }

    private void registerEnergyStorages() {
        EnergyStorage.SIDED.registerForBlockEntity((be, dir) -> be, RADIO_BLOCK_ENTITY);
        EnergyStorage.SIDED.registerForBlockEntity((be, dir) -> be, SPEAKER_BLOCK_ENTITY);

        EnergyStorage.ITEM.registerForItems((stack, context) -> {
            if (!(stack.getItem() instanceof RemoteControllerItem remoteControllerItem)) return null;

            EnergyItemHandler energyHandler = remoteControllerItem.getEnergyItemHandler();

            return new EnergyStorage() {
                @Override
                public long insert(long maxAmount, TransactionContext transaction) {
                    long inserted = Math.min(maxAmount,
                            energyHandler.getCapacity(stack) - energyHandler.getEnergy(stack));

                    transaction.addCloseCallback((context, result) -> {
                        if (result.wasCommitted()) {
                            energyHandler.addEnergy(stack, inserted);
                        }
                    });

                    return inserted;
                }

                @Override
                public long extract(long maxAmount, TransactionContext transaction) {
                    return 0;
                }

                @Override
                public long getAmount() {
                    return energyHandler.getEnergy(stack);
                }

                @Override
                public long getCapacity() {
                    return energyHandler.getCapacity(stack);
                }
            };
        }, REMOTE_CONTROLLER_ITEM);
    }

    private void registerGameRules() {
        RADIO_CONNECT_RADIUS = GameRuleRegistry.register("meyuugaosradioConnectRadius", GameRules.Category.MISC, GameRuleFactory.createIntRule(50));
    }

    private void registerSounds() {
        BLOCK_DISMANTLE = registerSound(BLOCK_DISMANTLE_SOUND_ID);
    }

    private SoundEvent registerSound(String name) {
        Identifier id = new Identifier("meyuugaosradio", name);
        return Registry.register(Registry.SOUND_EVENT, id, new SoundEvent(id));
    }
}