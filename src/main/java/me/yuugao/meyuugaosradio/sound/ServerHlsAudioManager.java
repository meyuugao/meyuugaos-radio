package me.yuugao.meyuugaosradio.sound;

import me.yuugao.meyuugaosradio.network.ServerNetworkManager;

import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerHlsAudioManager {
    private static final Map<String, ServerAudioInstance> audioInstances = new ConcurrentHashMap<>();
    private static final Map<UUID, WorldPlayerInfo> allPlayers = new ConcurrentHashMap<>();

    public static void onEndServerTick(List<ServerPlayerEntity> worldPlayers) {
        updatePlayersList(worldPlayers);

        allPlayers.entrySet().removeIf(entry ->
                entry.getValue().player == null || entry.getValue().player.isDisconnected());

        for (WorldPlayerInfo playerInfo : allPlayers.values()) {
            ServerPlayerEntity player = playerInfo.player;
            if (player != null && !player.isDisconnected()) {
                for (ServerAudioInstance audioInstance : audioInstances.values()) {
                    if (audioInstance.worldRegistryKey.equals(playerInfo.worldRegistryKey)) {
                        float volume = audioInstance.calculateVolumeForPlayer(player.getWorld(), player);
                        ServerNetworkManager.sendServerVolumeUpdatePacket(player, audioInstance.streamUrl, volume);
                    }
                }
            }
        }
    }

    private static void updatePlayersList(List<ServerPlayerEntity> currentWorldPlayers) {
        for (ServerPlayerEntity player : currentWorldPlayers) {
            allPlayers.put(player.getUuid(), new WorldPlayerInfo(player, player.getWorld().getRegistryKey()));
        }
    }

    private static class WorldPlayerInfo {
        public final ServerPlayerEntity player;
        public final Object worldRegistryKey;

        public WorldPlayerInfo(ServerPlayerEntity player, Object worldRegistryKey) {
            this.player = player;
            this.worldRegistryKey = worldRegistryKey;
        }
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
        public final String streamUrl;
        public final Object worldRegistryKey;
        private final Map<BlockPos, ServerSoundSource> soundSources = new ConcurrentHashMap<>();

        public ServerAudioInstance(String streamUrl, Object worldRegistryKey) {
            this.streamUrl = streamUrl;
            this.worldRegistryKey = worldRegistryKey;
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
                sendToWorldPlayers(player -> ServerNetworkManager.sendServerStreamStartPacket(player, streamUrl), worldRegistryKey);
            }
        }

        public void removeSoundSource(BlockPos pos) {
            ServerSoundSource removed = soundSources.remove(pos);
            if (soundSources.isEmpty() && removed != null) {
                sendToWorldPlayers(player -> ServerNetworkManager.sendServerStreamStopPacket(player, streamUrl), removed.worldRegistryKey);
            }
        }

        public void updateSoundSourceDirection(BlockPos pos, Vec3d newDirection) {
            ServerSoundSource soundSource = soundSources.get(pos);
            if (soundSource != null) {
                soundSource.direction = newDirection.normalize();
                sendToWorldPlayers(player ->
                        ServerNetworkManager.sendServerVolumeUpdatePacket(player, streamUrl, calculateVolumeForPlayer(player.getWorld(), player)), soundSource.worldRegistryKey);
            }
        }

        public void updateSoundSourceVolume(BlockPos pos, float volume) {
            ServerSoundSource soundSource = soundSources.get(pos);
            if (soundSource != null) {
                soundSource.volume = volume;
                sendToWorldPlayers(player ->
                        ServerNetworkManager.sendServerVolumeUpdatePacket(player, streamUrl, calculateVolumeForPlayer(player.getWorld(), player)), soundSource.worldRegistryKey);
            }
        }

        private void sendToWorldPlayers(java.util.function.Consumer<ServerPlayerEntity> action, Object worldKey) {
            for (WorldPlayerInfo playerInfo : allPlayers.values()) {
                if (playerInfo.player != null && !playerInfo.player.isDisconnected() &&
                        playerInfo.worldRegistryKey.equals(worldKey)) {
                    action.accept(playerInfo.player);
                }
            }
        }

        public float calculateVolumeForPlayer(World world, ServerPlayerEntity player) {
            if (player == null || world == null || soundSources.isEmpty()) {
                return 0.0f;
            }

            Vec3d playerPos = player.getPos().add(0, player.getEyeHeight(player.getPose()), 0);
            float totalVolume = 0.0f;

            for (ServerSoundSource soundSource : soundSources.values()) {
                if (!soundSource.worldRegistryKey.equals(world.getRegistryKey())) {
                    continue;
                }

                Vec3d toPlayer = playerPos.subtract(soundSource.position);
                double distance = toPlayer.length();

                if (distance > soundSource.maxRange) {
                    continue;
                }

                float distanceFactor = (float) (1.0 / (1.0 + distance * 0.5));
                distanceFactor *= (1.0f - (float) (distance / soundSource.maxRange));

                float obstructionFactor = calculateObstructionFactor(soundSource.position, playerPos, world, soundSource.direction);

                Vec3d toPlayerNormalized = toPlayer.normalize();
                float dotProduct = (float) soundSource.direction.dotProduct(toPlayerNormalized);
                float directionFactor = (dotProduct + 1.0f) / 2.0f;
                float coneWidth = 0.4f;
                directionFactor = (float) Math.pow(directionFactor, coneWidth);

                float sourceVolume = soundSource.volume * distanceFactor * obstructionFactor * directionFactor;
                totalVolume += sourceVolume;
            }

            return Math.min(totalVolume, 1.0f);
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
            if (state.isAir()) return false;
            return !state.getCollisionShape(world, BlockPos.ORIGIN).isEmpty();
        }

        public boolean hasSources() {
            return !soundSources.isEmpty();
        }
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
            sendToWorldPlayers(player -> ServerNetworkManager.sendServerStreamStopPacket(player, streamUrl), worldRegistryKey);
        }
    }

    private static void sendToWorldPlayers(java.util.function.Consumer<ServerPlayerEntity> action, Object worldKey) {
        for (WorldPlayerInfo playerInfo : allPlayers.values()) {
            if (playerInfo.player != null && !playerInfo.player.isDisconnected() &&
                    playerInfo.worldRegistryKey.equals(worldKey)) {
                action.accept(playerInfo.player);
            }
        }
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
        for (ServerAudioInstance audioInstance : audioInstances.values()) {
            sendToWorldPlayers(player -> ServerNetworkManager.sendServerStreamStopPacket(player, audioInstance.streamUrl), audioInstance.worldRegistryKey);
        }
        audioInstances.clear();
    }

    public static void cleanupWorld(Object worldRegistryKey) {
        Iterator<Map.Entry<String, ServerAudioInstance>> iterator = audioInstances.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ServerAudioInstance> entry = iterator.next();
            if (entry.getValue().worldRegistryKey.equals(worldRegistryKey)) {
                sendToWorldPlayers(player -> ServerNetworkManager.sendServerStreamStopPacket(player, entry.getValue().streamUrl), worldRegistryKey);
                iterator.remove();
            }
        }

        allPlayers.entrySet().removeIf(entry -> entry.getValue().worldRegistryKey.equals(worldRegistryKey));
    }
}