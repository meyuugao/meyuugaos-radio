package me.yuugao.meyuugaosradio.entity;

import me.yuugao.meyuugaosradio.Radio;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class RadioBlockEntity extends AbstractEnergyBlockEntity {
    private final List<BlockPos> speakers = new ArrayList<>();
    private String streamUrl = "";

    public RadioBlockEntity(BlockPos pos, BlockState state) {
        super(Radio.RADIO_BLOCK_ENTITY, pos, state, 100_000L, 8L);
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        NbtList list = new NbtList();
        for (BlockPos pos : speakers) {
            list.add(NbtHelper.fromBlockPos(pos));
        }
        nbt.put("Speakers", list);
        nbt.putString("StreamUrl", streamUrl);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        if (nbt.contains("Speakers", NbtElement.LIST_TYPE)) {
            NbtList list = nbt.getList("Speakers", NbtElement.COMPOUND_TYPE);
            speakers.clear();
            for (int i = 0; i < list.size(); i++) {
                BlockPos pos = NbtHelper.toBlockPos(list.getCompound(i));
                speakers.add(pos.toImmutable());
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
}