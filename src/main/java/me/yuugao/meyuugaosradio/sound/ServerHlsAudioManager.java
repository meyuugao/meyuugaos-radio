package me.yuugao.meyuugaosradio.sound;

import me.yuugao.meyuugaosradio.network.ServerNetworkManager;

import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class ServerHlsAudioManager {
    private static final Map<String, ServerAudioInstance> audioInstances = new ConcurrentHashMap<>();
    private static final Map<UUID, WorldPlayerInfo> allPlayers = new ConcurrentHashMap<>();

    public static void onEndServerTick(List<ServerPlayerEntity> worldPlayers) {
        updatePlayersList(worldPlayers);

        allPlayers.entrySet().removeIf(entry ->
                entry.getValue().player == null || entry.getValue().player.isDisconnected());

        allPlayers.values().forEach(worldPlayerInfo -> {
            ServerPlayerEntity player = worldPlayerInfo.player;
            if (player != null && !player.isDisconnected()) {
                audioInstances.values().forEach(serverAudioInstance -> {
                    if (serverAudioInstance.worldRegistryKey.equals(worldPlayerInfo.worldRegistryKey)) {
                        float volume = serverAudioInstance.calculateVolumeForPlayer(player.getEntityWorld(), player);
                        ServerNetworkManager.sendServerVolumeUpdatePacket(player, serverAudioInstance.streamUrl, volume);
                    }
                });
            }
        });
    }

    private static void updatePlayersList(List<ServerPlayerEntity> currentWorldPlayers) {
        currentWorldPlayers.forEach(player ->
                allPlayers.put(player.getUuid(), new WorldPlayerInfo(player, player.getEntityWorld().getRegistryKey())));
    }

    public static void createAudioInstance(String streamUrl, Object worldRegistryKey) {
        String instanceKey = streamUrl + "_" + worldRegistryKey;
        if (!audioInstances.containsKey(instanceKey)) {
            audioInstances.put(instanceKey, new ServerAudioInstance(streamUrl, worldRegistryKey));
        }
    }

    public static void removeAudioInstance(String streamUrl, Object worldRegistryKey) {
        String instanceKey = streamUrl + "_" + worldRegistryKey;
        ServerAudioInstance audioInstance = audioInstances.remove(instanceKey);
        if (audioInstance != null && audioInstance.hasSources()) {
            sendToWorldPlayers(player ->
                    ServerNetworkManager.sendServerStreamStopPacket(player, streamUrl), worldRegistryKey);
        }
    }

    private static void sendToWorldPlayers(java.util.function.Consumer<ServerPlayerEntity> action, Object worldKey) {
        allPlayers.values().forEach(worldPlayerInfo -> {
            if (worldPlayerInfo.player != null && !worldPlayerInfo.player.isDisconnected() && worldPlayerInfo.worldRegistryKey.equals(worldKey)) {
                action.accept(worldPlayerInfo.player);
            }
        });
    }

    public static ServerAudioInstance getAudioInstance(String streamUrl, Object worldRegistryKey) {
        String instanceKey = streamUrl + "_" + worldRegistryKey;
        return audioInstances.get(instanceKey);
    }

    public static void addSoundSource(String streamUrl, BlockPos pos, Vec3d direction, float volume, float maxRange, Object worldRegistryKey) {
        createAudioInstance(streamUrl, worldRegistryKey);

        ServerAudioInstance instance = getAudioInstance(streamUrl, worldRegistryKey);
        if (instance != null) {
            instance.addSoundSource(pos, direction, volume, maxRange, worldRegistryKey);
        }
    }

    public static void removeSoundSource(String streamUrl, BlockPos pos, Object worldRegistryKey) {
        ServerAudioInstance audioInstance = getAudioInstance(streamUrl, worldRegistryKey);
        if (audioInstance != null) {
            audioInstance.removeSoundSource(pos);
            if (!audioInstance.hasSources()) {
                removeAudioInstance(streamUrl, worldRegistryKey);
            }
        }
    }

    public static void updateSoundSourceDirection(String streamUrl, BlockPos pos, Vec3d newDirection, Object worldRegistryKey) {
        ServerAudioInstance audioInstance = getAudioInstance(streamUrl, worldRegistryKey);
        if (audioInstance != null) {
            audioInstance.updateSoundSourceDirection(pos, newDirection);
        }
    }

    public static void updateSoundSourceVolume(String streamUrl, BlockPos pos, float volume, Object worldRegistryKey) {
        ServerAudioInstance audioInstance = getAudioInstance(streamUrl, worldRegistryKey);
        if (audioInstance != null) {
            audioInstance.updateSoundSourceVolume(pos, volume);
        }
    }

    public static void stopAllAudioInstances() {
        audioInstances.values().forEach(serverAudioInstance ->
                sendToWorldPlayers(player ->
                        ServerNetworkManager.sendServerStreamStopPacket(player, serverAudioInstance.streamUrl), serverAudioInstance.worldRegistryKey));

        audioInstances.clear();
    }

    public static void cleanupWorld(Object worldRegistryKey) {
        Iterator<Map.Entry<String, ServerAudioInstance>> iterator = audioInstances.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ServerAudioInstance> entry = iterator.next();
            if (entry.getValue().worldRegistryKey.equals(worldRegistryKey)) {
                sendToWorldPlayers(player ->
                        ServerNetworkManager.sendServerStreamStopPacket(player, entry.getValue().streamUrl), worldRegistryKey);
                iterator.remove();
            }
        }

        allPlayers.entrySet().removeIf(entry ->
                entry.getValue().worldRegistryKey.equals(worldRegistryKey));
    }

    private record WorldPlayerInfo(ServerPlayerEntity player, Object worldRegistryKey) {
    }

    public static class ServerSoundSource {
        public Vec3d position;
        public Vec3d direction;
        public float volume;
        public float maxRange;
        public Object worldRegistryKey;

        public ServerSoundSource(Vec3d position, Vec3d direction, float volume, float maxRange, Object worldRegistryKey) {
            this.position = position;
            this.direction = direction.normalize();
            this.volume = volume;
            this.maxRange = maxRange;
            this.worldRegistryKey = worldRegistryKey;
        }
    }

    public static class ServerAudioInstance {
        private final String streamUrl;
        private final Object worldRegistryKey;
        private final Map<BlockPos, ServerSoundSource> soundSources;

        public ServerAudioInstance(String streamUrl, Object worldRegistryKey) {
            this.streamUrl = streamUrl;
            this.worldRegistryKey = worldRegistryKey;
            this.soundSources = new ConcurrentHashMap<>();
        }

        public void addSoundSource(BlockPos pos, Vec3d direction, float volumeMultiplier, float maxRange, Object worldRegistryKey) {
            boolean wasEmpty = soundSources.isEmpty();
            soundSources.put(pos, new ServerSoundSource(
                    new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                    direction,
                    volumeMultiplier,
                    maxRange,
                    worldRegistryKey
            ));

            if (wasEmpty) {
                sendToWorldPlayers(player ->
                        ServerNetworkManager.sendServerStreamStartPacket(player, streamUrl), worldRegistryKey);
            }
        }

        public void removeSoundSource(BlockPos pos) {
            ServerSoundSource removed = soundSources.remove(pos);
            if (soundSources.isEmpty() && removed != null) {
                sendToWorldPlayers(player ->
                        ServerNetworkManager.sendServerStreamStopPacket(player, streamUrl), removed.worldRegistryKey);
            }
        }

        public void updateSoundSourceDirection(BlockPos pos, Vec3d newDirection) {
            ServerSoundSource soundSource = soundSources.get(pos);
            if (soundSource != null) {
                soundSource.direction = newDirection.normalize();
                sendToWorldPlayers(player ->
                        ServerNetworkManager.sendServerVolumeUpdatePacket(player, streamUrl, calculateVolumeForPlayer(player.getEntityWorld(), player)), soundSource.worldRegistryKey);
            }
        }

        public void updateSoundSourceVolume(BlockPos pos, float volume) {
            ServerSoundSource soundSource = soundSources.get(pos);
            if (soundSource != null) {
                soundSource.volume = volume;
                sendToWorldPlayers(player ->
                        ServerNetworkManager.sendServerVolumeUpdatePacket(player, streamUrl, calculateVolumeForPlayer(player.getEntityWorld(), player)), soundSource.worldRegistryKey);
            }
        }

        private void sendToWorldPlayers(java.util.function.Consumer<ServerPlayerEntity> action, Object worldKey) {
            allPlayers.values().forEach(worldPlayerInfo -> {
                if (worldPlayerInfo.player != null && !worldPlayerInfo.player.isDisconnected() && worldPlayerInfo.worldRegistryKey.equals(worldKey)) {
                    action.accept(worldPlayerInfo.player);
                }
            });
        }

        public float calculateVolumeForPlayer(World world, ServerPlayerEntity player) {
            if (player == null || world == null || soundSources.isEmpty()) {
                return 0.0f;
            }

            Vec3d playerPos = player.getEntityPos().add(0, player.getEyeHeight(player.getPose()), 0);
            AtomicReference<Float> totalVolume = new AtomicReference<>(0.0f);

            soundSources.values().forEach(serverSoundSource -> {
                if (!serverSoundSource.worldRegistryKey.equals(world.getRegistryKey())) {
                    return;
                }

                Vec3d toPlayer = playerPos.subtract(serverSoundSource.position);
                double distance = toPlayer.length();

                if (distance > serverSoundSource.maxRange) {
                    return;
                }

                float distanceFactor = (float) (serverSoundSource.volume / (1.0 + distance * 0.5));
                distanceFactor *= (1.0f - (float) (distance / serverSoundSource.maxRange));

                float obstructionFactor = calculateObstructionFactor(serverSoundSource.position, playerPos, world, serverSoundSource.direction);

                Vec3d toPlayerNormalized = toPlayer.normalize();
                float dotProduct = (float) serverSoundSource.direction.dotProduct(toPlayerNormalized);
                float directionFactor = (dotProduct + 1.0f) / 2.0f;
                float coneWidth = 0.4f;
                directionFactor = (float) Math.pow(directionFactor, coneWidth);

                float sourceVolume = distanceFactor * obstructionFactor * directionFactor;
                totalVolume.updateAndGet(v -> v + sourceVolume);
            });

            return Math.min(totalVolume.get(), 2.0f);
        }

        private float calculateObstructionFactor(Vec3d source, Vec3d target, World world, Vec3d direction) {
            Set<BlockPos> obstructions = new HashSet<>();

            Vec3d rayStart = new Vec3d(
                    source.x + direction.x * 0.5,
                    source.y + direction.y * 0.5,
                    source.z + direction.z * 0.5
            );

            BlockPos soundSourceBlockPos = new BlockPos(
                    (int) Math.floor(source.x),
                    (int) Math.floor(source.y),
                    (int) Math.floor(source.z)
            );

            Vec3d rayDirection = target.subtract(rayStart).normalize();
            double totalDistance = rayStart.distanceTo(target);

            double step = 0.5;
            int steps = (int) (totalDistance / step);

            for (int i = 0; i < steps; i++) {
                Vec3d currentPos = rayStart.add(rayDirection.multiply(i * step));
                BlockPos blockPos = new BlockPos(
                        (int) Math.floor(currentPos.x),
                        (int) Math.floor(currentPos.y),
                        (int) Math.floor(currentPos.z)
                );

                if (!blockPos.equals(soundSourceBlockPos)) {
                    BlockState state = world.getBlockState(blockPos);
                    if (shouldSuppress(world, state)) {
                        obstructions.add(blockPos);
                    }
                }

                if (currentPos.distanceTo(rayStart) >= totalDistance) {
                    break;
                }
            }

            return (float) Math.pow(0.8, obstructions.size());
        }

        private boolean shouldSuppress(World world, BlockState state) {
            return !state.isAir() && !state.getCollisionShape(world, BlockPos.ORIGIN).isEmpty();
        }

        public boolean hasSources() {
            return !soundSources.isEmpty();
        }
    }
}