package me.yuugao.meyuugaosradio.network;

import me.yuugao.meyuugaosradio.Radio;
import me.yuugao.meyuugaosradio.block.AbstractEnergyBlock;
import me.yuugao.meyuugaosradio.block.EnergyStateEnum;
import me.yuugao.meyuugaosradio.block.RadioBlock;
import me.yuugao.meyuugaosradio.block.SpeakerBlock;
import me.yuugao.meyuugaosradio.entity.AbstractEnergyBlockEntity;
import me.yuugao.meyuugaosradio.entity.RadioBlockEntity;
import me.yuugao.meyuugaosradio.entity.SpeakerBlockEntity;
import me.yuugao.meyuugaosradio.item.RemoteControllerItem;
import me.yuugao.meyuugaosradio.sound.ServerHlsAudioManager;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class ServerNetworkManager {
    public static void initialize() {
        PayloadTypeRegistry.playS2C().register(NetworkConstants.ServerRadioPayload.ID, NetworkConstants.ServerRadioPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NetworkConstants.ServerRequestBlocksPayload.ID, NetworkConstants.ServerRequestBlocksPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NetworkConstants.ServerRadioGlobalUnbindPayload.ID, NetworkConstants.ServerRadioGlobalUnbindPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NetworkConstants.ServerSpeakerGlobalUnbindPayload.ID, NetworkConstants.ServerSpeakerGlobalUnbindPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NetworkConstants.ServerAddBlockPayload.ID, NetworkConstants.ServerAddBlockPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NetworkConstants.ServerRemoveBlockPayload.ID, NetworkConstants.ServerRemoveBlockPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NetworkConstants.ServerPlayerSendMessagePayload.ID, NetworkConstants.ServerPlayerSendMessagePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NetworkConstants.ServerOpenRadioGuiPayload.ID, NetworkConstants.ServerOpenRadioGuiPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NetworkConstants.ServerOpenSpeakerGuiPayload.ID, NetworkConstants.ServerOpenSpeakerGuiPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NetworkConstants.ServerStreamStopPayload.ID, NetworkConstants.ServerStreamStopPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NetworkConstants.ServerStreamStartPayload.ID, NetworkConstants.ServerStreamStartPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NetworkConstants.ServerVolumeUpdatePayload.ID, NetworkConstants.ServerVolumeUpdatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NetworkConstants.ServerGlowClearPayload.ID, NetworkConstants.ServerGlowClearPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(NetworkConstants.ClientBlocksUpdatePayload.ID, NetworkConstants.ClientBlocksUpdatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NetworkConstants.ClientRemotecontrollerOnClickPayload.ID, NetworkConstants.ClientRemotecontrollerOnClickPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NetworkConstants.ClientRadioStateSwitchPayload.ID, NetworkConstants.ClientRadioStateSwitchPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NetworkConstants.ClientSpeakerStateSwitchPayload.ID, NetworkConstants.ClientSpeakerStateSwitchPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NetworkConstants.ClientVolumeUpdatePayload.ID, NetworkConstants.ClientVolumeUpdatePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(NetworkConstants.ClientBlocksUpdatePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> serverSpeakerUse(player, payload.enabled(), payload.blocks(), player.getWorld(), payload.speakerPos()));
        });

        ServerPlayNetworking.registerGlobalReceiver(NetworkConstants.ClientRemotecontrollerOnClickPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                ItemStack stack = player.getMainHandStack();
                if (stack.getItem() instanceof RemoteControllerItem remoteControllerItem) {
                    remoteControllerItem.onLeftClick(player.getWorld(), player);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NetworkConstants.ClientRadioStateSwitchPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> serverRadioStateSwitch(player.getWorld(), payload.pos(), payload.streamUrl()));
        });

        ServerPlayNetworking.registerGlobalReceiver(NetworkConstants.ClientSpeakerStateSwitchPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> serverSpeakerStateSwitch(player.getWorld(), payload.pos()));
        });

        ServerPlayNetworking.registerGlobalReceiver(NetworkConstants.ClientVolumeUpdatePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> serverVolumeUpdate(player.getWorld(), payload.pos(), payload.volume(), payload.volumeMultiplier()));
        });
    }

    public static void sendServerRadioPacket(ServerPlayerEntity player, RadioBlockEntity radioBlockEntity) {
        ServerPlayNetworking.send(player, new NetworkConstants.ServerRadioPayload(radioBlockEntity.getPos(), radioBlockEntity.getSpeakers()));
    }

    public static void sendServerRequestBlocks(ServerPlayerEntity player, BlockPos speakerPos) {
        ServerPlayNetworking.send(player, new NetworkConstants.ServerRequestBlocksPayload(speakerPos));
    }

    public static void sendRadioGlobalUnbind(ServerPlayerEntity player, BlockPos radioPos) {
        ServerPlayNetworking.send(player, new NetworkConstants.ServerRadioGlobalUnbindPayload(radioPos));
    }

    public static void sendSpeakerGlobalUnbind(ServerPlayerEntity player, BlockPos speakerPos) {
        ServerPlayNetworking.send(player, new NetworkConstants.ServerSpeakerGlobalUnbindPayload(speakerPos));
    }

    public static void sendServerAddBlockPacket(ServerPlayerEntity player, BlockPos pos, float r, float g, float b, float a) {
        ServerPlayNetworking.send(player, new NetworkConstants.ServerAddBlockPayload(pos, r, g, b, a));
    }

    public static void sendServerRemoveBlockPacket(ServerPlayerEntity player, BlockPos pos) {
        ServerPlayNetworking.send(player, new NetworkConstants.ServerRemoveBlockPayload(pos));
    }

    public static void sendServerPlayerSendMessagePacket(ServerPlayerEntity player, Text text, boolean overlay) {
        String textJson = Text.Serialization.toJsonString(text, player.getRegistryManager());
        ServerPlayNetworking.send(player, new NetworkConstants.ServerPlayerSendMessagePayload(textJson, overlay));
    }

    public static void sendServerOpenRadioGuiPacket(ServerPlayerEntity player, BlockPos pos, String streamUrl, float volume) {
        ServerPlayNetworking.send(player, new NetworkConstants.ServerOpenRadioGuiPayload(pos, streamUrl, volume));
    }

    public static void sendServerOpenSpeakerGuiPacket(ServerPlayerEntity player, BlockPos pos, float volume) {
        ServerPlayNetworking.send(player, new NetworkConstants.ServerOpenSpeakerGuiPayload(pos, volume));
    }

    public static void sendServerStreamStopPacket(ServerPlayerEntity player, String streamUrl) {
        ServerPlayNetworking.send(player, new NetworkConstants.ServerStreamStopPayload(streamUrl));
    }

    public static void sendServerStreamStartPacket(ServerPlayerEntity player, String streamUrl) {
        ServerPlayNetworking.send(player, new NetworkConstants.ServerStreamStartPayload(streamUrl));
    }

    public static void sendServerVolumeUpdatePacket(ServerPlayerEntity player, String streamUrl, float volume) {
        ServerPlayNetworking.send(player, new NetworkConstants.ServerVolumeUpdatePayload(streamUrl, volume));
    }

    public static void sendServerGlowClearPacket(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new NetworkConstants.ServerGlowClearPayload());
    }

    private static void serverSpeakerUse(ServerPlayerEntity player, boolean renderEnabled, List<BlockPos> blocks, World world, BlockPos speakerPos) {
        if (!renderEnabled) return;

        SpeakerBlockEntity speakerBlockEntity = (SpeakerBlockEntity) world.getBlockEntity(speakerPos);
        RadioBlockEntity activeRadioBlockEntity = null;
        for (BlockPos blockPos : blocks) {
            if (world.getBlockEntity(blockPos) instanceof RadioBlockEntity) {
                activeRadioBlockEntity = (RadioBlockEntity) world.getBlockEntity(blockPos);
            }
        }
        if (speakerBlockEntity == null || activeRadioBlockEntity == null) return;

        if (speakerBlockEntity.getRadioPos() == null) {
            if (!activeRadioBlockEntity.getPos().isWithinDistance(speakerPos, world.getGameRules().getInt(Radio.RADIO_CONNECT_RADIUS) + 1)) {
                sendServerPlayerSendMessagePacket(player, Text.translatable("error.toofar").formatted(Formatting.BOLD).formatted(Formatting.RED), true);
                return;
            }

            sendServerAddBlockPacket(player, speakerBlockEntity.getPos(), 0f, 1f, 0f, 0.7f);
            speakerBlockEntity.connectRadio(activeRadioBlockEntity.getPos());
            activeRadioBlockEntity.connectSpeaker(speakerBlockEntity.getPos());
        } else {
            if (speakerBlockEntity.getRadioPos().equals(activeRadioBlockEntity.getPos())) {
                sendServerRemoveBlockPacket(player, speakerBlockEntity.getPos());
                speakerBlockEntity.disconnectRadio();
                activeRadioBlockEntity.disconnectSpeaker(speakerBlockEntity.getPos());
            } else {
                sendServerPlayerSendMessagePacket(player, Text.translatable("error.alreadyconnected").formatted(Formatting.BOLD).formatted(Formatting.RED), true);
            }
        }
    }

    private static void serverRadioStateSwitch(World world, BlockPos pos, String streamUrl) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof RadioBlockEntity radioBlockEntity && radioBlockEntity.getAmount() >= radioBlockEntity.getUsage()) {
            boolean state = radioBlockEntity.getCachedState().get(AbstractEnergyBlock.ENERGY_STATE).equals(EnergyStateEnum.ENABLED);
            Block block = world.getBlockState(pos).getBlock();

            if (!streamUrl.isEmpty()) {
                radioBlockEntity.setStreamUrl(streamUrl);
            }

            if (state) {
                if (block instanceof RadioBlock radioBlock) {
                    radioBlock.onDisabled(world, pos, world.getBlockState(pos));
                }
            } else {
                if (block instanceof RadioBlock radioBlock) {
                    radioBlock.onEnabled(world, pos, world.getBlockState(pos), radioBlockEntity.getStreamUrl());
                }
            }
        }
    }

    private static void serverSpeakerStateSwitch(World world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof SpeakerBlockEntity speakerBlockEntity && speakerBlockEntity.getAmount() >= speakerBlockEntity.getUsage()) {
            boolean state = speakerBlockEntity.getCachedState().get(AbstractEnergyBlock.ENERGY_STATE).equals(EnergyStateEnum.ENABLED);
            Block block = world.getBlockState(pos).getBlock();
            if (state) {
                if (block instanceof SpeakerBlock speakerBlock) {
                    speakerBlock.onDisabled(world, pos, world.getBlockState(pos));
                }
            } else {
                if (block instanceof SpeakerBlock speakerBlock) {
                    speakerBlock.onEnabled(world, pos, world.getBlockState(pos));
                }
            }
        }
    }

    private static void serverVolumeUpdate(World world, BlockPos pos, float volume, float volumeMultiplier) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof AbstractEnergyBlockEntity abstractEnergyBlockEntity) {
            abstractEnergyBlockEntity.setVolume(volume);
            String streamUrl = "";
            if (abstractEnergyBlockEntity instanceof RadioBlockEntity radioBlockEntity) {
                streamUrl = radioBlockEntity.getStreamUrl();
            } else if (abstractEnergyBlockEntity instanceof SpeakerBlockEntity speakerBlockEntity) {
                if (speakerBlockEntity.getRadioPos() != null) {
                    if (world.getBlockEntity(speakerBlockEntity.getRadioPos()) instanceof RadioBlockEntity radioBlockEntity) {
                        streamUrl = radioBlockEntity.getStreamUrl();
                    }
                }
            }
            if (!streamUrl.isEmpty()) {
                ServerHlsAudioManager.updateSoundSourceVolume(streamUrl, pos, volume * volumeMultiplier, world.getRegistryKey());
            }
        }
    }
}