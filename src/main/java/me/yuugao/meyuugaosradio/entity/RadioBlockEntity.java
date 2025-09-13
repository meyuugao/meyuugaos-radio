package me.yuugao.meyuugaosradio.entity;

import me.yuugao.meyuugaosradio.Radio;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class RadioBlockEntity extends AbstractEnergyBlockEntity {
    private final List<BlockPos> speakers;
    private String streamUrl;

    public RadioBlockEntity(BlockPos pos, BlockState state) {
        super(Radio.RADIO_BLOCK_ENTITY, pos, state, 100_000L, 8L);

        this.speakers = new ArrayList<>();
        this.streamUrl = StringUtils.EMPTY;
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);

        NbtList list = new NbtList();
        for (BlockPos pos : speakers) {
            NbtCompound posCompound = new NbtCompound();
            posCompound.putIntArray("pos", new int[]{pos.getX(), pos.getY(), pos.getZ()});
            list.add(posCompound);
        }
        nbt.put("Speakers", list);
        nbt.putString("StreamUrl", streamUrl);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        if (nbt.contains("Speakers", NbtElement.LIST_TYPE)) {
            NbtList list = nbt.getList("Speakers", NbtElement.COMPOUND_TYPE);
            speakers.clear();
            for (int i = 0; i < list.size(); i++) {
                NbtCompound compound = list.getCompound(i);
                int[] coords = compound.getIntArray("pos");
                if (coords.length == 3) {
                    BlockPos pos = new BlockPos(coords[0], coords[1], coords[2]);
                    speakers.add(pos.toImmutable());
                }
            }
        }
        streamUrl = nbt.getString("StreamUrl");
    }

    public void connectSpeaker(BlockPos pos) {
        BlockPos immutablePos = pos.toImmutable();
        if (!speakers.contains(immutablePos)) {
            speakers.add(immutablePos);
            markDirty();
        }
    }

    public void disconnectSpeaker(BlockPos pos) {
        BlockPos immutablePos = pos.toImmutable();
        if (speakers.remove(immutablePos)) {
            markDirty();
        }
    }

    public List<BlockPos> getSpeakers() {
        return speakers;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
        markDirty();
    }

    public void setSpeakers(List<BlockPos> speakers) {
        this.speakers.clear();
        this.speakers.addAll(speakers);
        markDirty();
    }
}