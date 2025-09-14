package me.yuugao.meyuugaosradio.entity;

import static me.yuugao.meyuugaosradio.Constants.RADIO_ENERGY_CAPACITY;
import static me.yuugao.meyuugaosradio.Constants.RADIO_ENERGY_USAGE;


import me.yuugao.meyuugaosradio.Radio;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class RadioBlockEntity extends AbstractEnergyBlockEntity {
    private final List<BlockPos> speakers;
    private String streamUrl;

    public RadioBlockEntity(BlockPos pos, BlockState state) {
        super(Radio.RADIO_BLOCK_ENTITY, pos, state, RADIO_ENERGY_CAPACITY, RADIO_ENERGY_USAGE);

        this.speakers = new ArrayList<>();
        this.streamUrl = StringUtils.EMPTY;
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        speakers.forEach(speakerPos -> {
            NbtCompound posCompound = new NbtCompound();
            posCompound.putIntArray("pos", new int[]{speakerPos.getX(), speakerPos.getY(), speakerPos.getZ()});
            list.add(posCompound);
        });
        nbt.put("Speakers", list);
        nbt.putString("StreamUrl", streamUrl);

        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        if (nbt.contains("Speakers", NbtElement.LIST_TYPE)) {
            NbtList list = nbt.getList("Speakers", NbtElement.LIST_TYPE);
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
        streamUrl = nbt.contains("StreamUrl") ? nbt.getString("StreamUrl") : StringUtils.EMPTY;
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