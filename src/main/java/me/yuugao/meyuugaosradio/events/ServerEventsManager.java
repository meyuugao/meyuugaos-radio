package me.yuugao.meyuugaosradio.events;

import me.yuugao.meyuugaosradio.block.AbstractEnergyBlock;
import me.yuugao.meyuugaosradio.block.EnergyStateEnum;
import me.yuugao.meyuugaosradio.entity.AbstractEnergyBlockEntity;
import me.yuugao.meyuugaosradio.sound.ServerHlsAudioManager;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

public class ServerEventsManager {
    private static boolean shouldUnload = false;

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(minecraftServer -> minecraftServer.getWorlds().forEach(serverWorld -> ServerHlsAudioManager.onEndServerTick(serverWorld.getPlayers())));

        ServerLifecycleEvents.SERVER_STOPPING.register(minecraftServer -> {
            shouldUnload = true;
            ServerHlsAudioManager.stopAllAudioInstances();
        });

        ServerWorldEvents.UNLOAD.register((minecraftServer, serverWorld) -> {
            ServerHlsAudioManager.cleanupWorld(serverWorld.getRegistryKey());
        });

        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, serverWorld) -> {
            if (shouldUnload) { //tip: при выключении сервера
                if (blockEntity instanceof AbstractEnergyBlockEntity abstractEnergyBlockEntity) { //tip: выключаем все радио и динамики
                    BlockState state = abstractEnergyBlockEntity.getCachedState();
                    if (state.get(AbstractEnergyBlock.ENERGY_STATE).equals(EnergyStateEnum.ENABLED)) {
                        long currentEnergy = abstractEnergyBlockEntity.getAmount();
                        BlockPos pos = abstractEnergyBlockEntity.getPos();
                        serverWorld.removeBlockEntity(pos);
                        ((AbstractEnergyBlock) state.getBlock()).onDisabled(serverWorld, pos, state);
                        if (serverWorld.getBlockEntity(pos) instanceof AbstractEnergyBlockEntity newEntity) {
                            newEntity.setEnergy(currentEnergy);
                        }
                    }
                }
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(minecraftServer -> shouldUnload = false);
    }
}