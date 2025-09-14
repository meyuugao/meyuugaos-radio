package me.yuugao.meyuugaosradio.events;

import static me.yuugao.meyuugaosradio.Constants.SERVER_LOGGER;


import me.yuugao.meyuugaosradio.block.AbstractEnergyBlock;
import me.yuugao.meyuugaosradio.block.EnergyStateEnum;
import me.yuugao.meyuugaosradio.entity.AbstractEnergyBlockEntity;
import me.yuugao.meyuugaosradio.entity.RadioBlockEntity;
import me.yuugao.meyuugaosradio.sound.ServerHlsAudioManager;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class ServerEventsManager {
    private static boolean shouldUnload = false;

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(minecraftServer ->
                minecraftServer.getWorlds().forEach(serverWorld ->
                        ServerHlsAudioManager.onEndServerTick(serverWorld.getPlayers())));

        ServerLifecycleEvents.SERVER_STOPPING.register(minecraftServer -> {
            SERVER_LOGGER.info("Server stopping, unloading block entities and stopping all audio instances...");
            shouldUnload = true;
            ServerHlsAudioManager.stopAllAudioInstances();
        });

        ServerWorldEvents.UNLOAD.register((minecraftServer, serverWorld) ->
                ServerHlsAudioManager.cleanupWorld(serverWorld.getRegistryKey()));

        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, serverWorld) -> {
            if (shouldUnload) {
                if (blockEntity instanceof AbstractEnergyBlockEntity abstractEnergyBlockEntity) {
                    BlockState state = abstractEnergyBlockEntity.getCachedState();
                    if (state.get(AbstractEnergyBlock.ENERGY_STATE).equals(EnergyStateEnum.ENABLED)) {
                        long currentEnergy = abstractEnergyBlockEntity.getAmount();
                        float currentVolume = abstractEnergyBlockEntity.getVolume();
                        String currentStreamUrl = null;
                        List<BlockPos> currentSpeakers = null;

                        if (abstractEnergyBlockEntity instanceof RadioBlockEntity radioBlockEntity) {
                            currentStreamUrl = radioBlockEntity.getStreamUrl();
                            currentSpeakers = radioBlockEntity.getSpeakers();
                        }

                        BlockPos pos = abstractEnergyBlockEntity.getPos();
                        serverWorld.removeBlockEntity(pos);
                        ((AbstractEnergyBlock) state.getBlock()).onDisabled(serverWorld, pos, state);

                        if (serverWorld.getBlockEntity(pos) instanceof AbstractEnergyBlockEntity newAbstractEnergyBlockEntity) {
                            newAbstractEnergyBlockEntity.setEnergy(currentEnergy);
                            newAbstractEnergyBlockEntity.setVolume(currentVolume);

                            if (newAbstractEnergyBlockEntity instanceof RadioBlockEntity radioBlockEntity) {
                                if (currentStreamUrl != null) {
                                    radioBlockEntity.setStreamUrl(currentStreamUrl);
                                }

                                if (currentSpeakers != null) {
                                    radioBlockEntity.setSpeakers(currentSpeakers);
                                }
                            }
                        }
                    }
                }
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(minecraftServer -> shouldUnload = false);
    }
}