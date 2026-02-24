package me.yuugao.meyuugaosradio.entity;

import static me.yuugao.meyuugaosradio.Constants.RADIO_ENERGY_CAPACITY;
import static me.yuugao.meyuugaosradio.Constants.RADIO_ENERGY_USAGE;


import me.yuugao.meyuugaosradio.Radio;

import net.minecraft.block.BlockState;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
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
    public void writeData(WriteView view) {
        super.writeData(view);

        view.putInt("SpeakersCount", speakers.size());
        for (int i = 0; i < speakers.size(); i++) {
            BlockPos pos = speakers.get(i);
            view.putInt("SpeakerX" + i, pos.getX());
            view.putInt("SpeakerY" + i, pos.getY());
            view.putInt("SpeakerZ" + i, pos.getZ());
        }

        view.putString("StreamUrl", streamUrl);
    }

    @Override
    public void readData(ReadView view) {
        super.readData(view);

        speakers.clear();
        int speakersCount = view.getInt("SpeakersCount", 0);
        for (int i = 0; i < speakersCount; i++) {
            int x = view.getInt("SpeakerX" + i, 0);
            int y = view.getInt("SpeakerY" + i, 0);
            int z = view.getInt("SpeakerZ" + i, 0);
            speakers.add(new BlockPos(x, y, z).toImmutable());
        }

        streamUrl = view.getString("StreamUrl", StringUtils.EMPTY);
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

    public void setSpeakers(List<BlockPos> speakers) {
        this.speakers.clear();
        this.speakers.addAll(speakers);
        markDirty();
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
        markDirty();
    }
}