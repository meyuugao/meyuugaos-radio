package me.yuugao.meyuugaosradio;

import static me.yuugao.meyuugaosradio.Constants.*;

import me.yuugao.meyuugaosradio.block.RadioBlock;
import me.yuugao.meyuugaosradio.block.SpeakerBlock;
import me.yuugao.meyuugaosradio.entity.RadioBlockEntity;
import me.yuugao.meyuugaosradio.entity.SpeakerBlockEntity;
import me.yuugao.meyuugaosradio.events.ServerEventsManager;
import me.yuugao.meyuugaosradio.item.EnergyBlockItem;
import me.yuugao.meyuugaosradio.item.RemoteControllerItem;
import me.yuugao.meyuugaosradio.network.ServerNetworkManager;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;

import java.util.AbstractMap;
import java.util.function.Function;

import team.reborn.energy.api.EnergyStorage;

public class Radio implements ModInitializer {
    private static Block RADIO_BLOCK;
    private static Block SPEAKER_BLOCK;

    private static Item RADIO_BLOCK_ITEM;
    private static Item SPEAKER_BLOCK_ITEM;
    private static Item REMOTE_CONTROLLER_ITEM;
    private static Item ELECTRONIC_CIRCUIT_ITEM;
    private static Item BATTERY_ITEM;
    private static Item SMALL_BATTERY_ITEM;
    private static Item ANTENNA_ITEM;
    private static Item SMALL_MEMBRANE_ITEM;
    private static Item MEMBRANE_ITEM;

    public static BlockEntityType<RadioBlockEntity> RADIO_BLOCK_ENTITY;
    public static BlockEntityType<SpeakerBlockEntity> SPEAKER_BLOCK_ENTITY;

    public static final SoundEvent BLOCK_DISMANTLE = registerSound(BLOCK_DISMANTLE_SOUND_ID);

    public static final GameRules.Key<GameRules.IntRule> RADIO_CONNECT_RADIUS =
            GameRuleRegistry.register("meyuugaosradioConnectRadius", GameRules.Category.MISC, GameRuleFactory.createIntRule(50));

    @Override
    public void onInitialize() {
        registerBlocks();
        registerItems();
        registerBlockEntities();
        registerItemGroups();
        registerEnergyStorages();
        ServerNetworkManager.initialize();
        ServerEventsManager.initialize();
    }

    private void registerBlocks() {
        AbstractMap.SimpleEntry<Block, Item> registeredRadio = registerBlock(RADIO_BLOCK_ID, RadioBlock::new, AbstractBlock.Settings.create().strength(2.0f));
        RADIO_BLOCK = registeredRadio.getKey();
        RADIO_BLOCK_ITEM = registeredRadio.getValue();

        AbstractMap.SimpleEntry<Block, Item> registeredSpeaker = registerBlock(SPEAKER_BLOCK_ID, SpeakerBlock::new, AbstractBlock.Settings.create().strength(2.0f));
        SPEAKER_BLOCK = registeredRadio.getKey();
        SPEAKER_BLOCK_ITEM = registeredRadio.getValue();
    }

    private static AbstractMap.SimpleEntry<Block, Item> registerBlock(String path, Function<AbstractBlock.Settings, Block> factory, AbstractBlock.Settings settings) {
        final Identifier identifier = Identifier.of("meyuugaosradio", path);
        final RegistryKey<Block> registryKey = RegistryKey.of(RegistryKeys.BLOCK, identifier);

        final Block block = Blocks.register(registryKey, factory, settings);
        final Item item = Items.register(block);
        return new AbstractMap.SimpleEntry<>(block, item);
    }

    private void registerItems() {
        REMOTE_CONTROLLER_ITEM = Registry.register(Registries.ITEM, id(REMOTE_CONTROLLER_ID),
                new RemoteControllerItem(new Item.Settings().maxCount(REMOTE_CONTROLLER_STACK_SIZE)));

        ELECTRONIC_CIRCUIT_ITEM = Registry.register(Registries.ITEM, id(ELECTRONIC_CIRCUIT_ID),
                new Item(new Item.Settings().maxCount(DEFAULT_STACK_SIZE)));

        BATTERY_ITEM = Registry.register(Registries.ITEM, id(BATTERY_ID),
                new Item(new Item.Settings().maxCount(DEFAULT_STACK_SIZE)));

        SMALL_BATTERY_ITEM = Registry.register(Registries.ITEM, id(SMALL_BATTERY_ID),
                new Item(new Item.Settings().maxCount(DEFAULT_STACK_SIZE)));

        ANTENNA_ITEM = Registry.register(Registries.ITEM, id(ANTENNA_ID),
                new Item(new Item.Settings().maxCount(DEFAULT_STACK_SIZE)));

        SMALL_MEMBRANE_ITEM = Registry.register(Registries.ITEM, id(SMALL_MEMBRANE_ID),
                new Item(new Item.Settings().maxCount(DEFAULT_STACK_SIZE)));

        MEMBRANE_ITEM = Registry.register(Registries.ITEM, id(MEMBRANE_ID),
                new Item(new Item.Settings().maxCount(DEFAULT_STACK_SIZE)));
    }

    private void registerBlockEntities() {
        RADIO_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, id(RADIO_BLOCK_ENTITY_ID),
                FabricBlockEntityTypeBuilder.create(RadioBlockEntity::new, RADIO_BLOCK).build());

        SPEAKER_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, id(SPEAKER_BLOCK_ENTITY_ID),
                FabricBlockEntityTypeBuilder.create(SpeakerBlockEntity::new, SPEAKER_BLOCK).build());
    }

    private void registerItemGroups() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(RADIO_BLOCK_ITEM.getDefaultStack());
            entries.add(SPEAKER_BLOCK_ITEM.getDefaultStack());
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(entries -> {
            entries.add(REMOTE_CONTROLLER_ITEM.getDefaultStack());
            entries.add(ELECTRONIC_CIRCUIT_ITEM.getDefaultStack());
            entries.add(BATTERY_ITEM.getDefaultStack());
            entries.add(SMALL_BATTERY_ITEM.getDefaultStack());
            entries.add(ANTENNA_ITEM.getDefaultStack());
            entries.add(SMALL_MEMBRANE_ITEM.getDefaultStack());
            entries.add(MEMBRANE_ITEM.getDefaultStack());
        });
    }

    private void registerEnergyStorages() {
        EnergyStorage.SIDED.registerForBlockEntity((be, dir) -> be, RADIO_BLOCK_ENTITY);
        EnergyStorage.SIDED.registerForBlockEntity((be, dir) -> be, SPEAKER_BLOCK_ENTITY);

        EnergyStorage.ITEM.registerForItems((stack, context) -> new EnergyStorage() {
            @Override
            public long insert(long maxAmount, TransactionContext transaction) {
                RemoteControllerItem item = (RemoteControllerItem) stack.getItem();
                long inserted = Math.min(maxAmount, item.getCapacity(stack) - item.getEnergy(stack));

                transaction.addCloseCallback((t, result) -> {
                    if (result.wasCommitted()) {
                        item.addEnergy(stack, inserted);
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
                RemoteControllerItem item = (RemoteControllerItem) stack.getItem();
                return item.getEnergy(stack);
            }

            @Override
            public long getCapacity() {
                RemoteControllerItem item = (RemoteControllerItem) stack.getItem();
                return item.getCapacity(stack);
            }
        }, REMOTE_CONTROLLER_ITEM);
    }

    private static SoundEvent registerSound(String name) {
        Identifier id = Identifier.of("meyuugaosradio", name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
}