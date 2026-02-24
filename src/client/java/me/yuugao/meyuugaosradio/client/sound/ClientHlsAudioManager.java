package me.yuugao.meyuugaosradio.client.sound;

import me.yuugao.meyuugaosradio.client.config.ClientModConfigManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import javax.sound.sampled.*;

public class ClientHlsAudioManager {
    private static final Map<String, ClientAudioInstance> audioInstances = new ConcurrentHashMap<>();
    private static final Map<String, AtomicBoolean> startingStreams = new ConcurrentHashMap<>();

    private static final float VOLUME_THRESHOLD = 0.001f;

    public static void handleVolumeUpdate(String streamUrl, float volume) {
        float finalVolume = volume * MinecraftClient.getInstance().options.getSoundVolume(SoundCategory.MASTER)
                * ClientModConfigManager.getConfig().volume / 100;

        ClientAudioInstance instance = audioInstances.get(streamUrl);
        if (instance != null) {
            instance.setVolume(finalVolume);

            if (finalVolume > VOLUME_THRESHOLD) {
                if (!instance.isPlaying()) instance.startStream();
            } else if (finalVolume <= VOLUME_THRESHOLD) {
                if (instance.isPlaying()) instance.stopStream();
            }
        } else if (finalVolume > VOLUME_THRESHOLD) {
            detectAndStartStream(streamUrl);
        }
    }

    public static void handleStreamStart(String streamUrl) {
        if (!audioInstances.containsKey(streamUrl)) detectAndStartStream(streamUrl);
    }

    public static void handleStreamStop(String streamUrl) {
        startingStreams.remove(streamUrl);
        ClientAudioInstance instance = audioInstances.remove(streamUrl);
        if (instance != null) instance.stopStream();
    }

    private static void detectAndStartStream(String streamUrl) {
        AtomicBoolean isStarting = startingStreams.computeIfAbsent(streamUrl, key -> new AtomicBoolean(false));
        if (!isStarting.compareAndSet(false, true)) return;

        new Thread(() -> {
            try {
                String[] probeCommand = {
                        "ffprobe", "-v", "quiet", "-print_format", "csv",
                        "-show_entries", "stream=sample_rate,channels", "-of", "csv=p=0", streamUrl
                };
                Process probeProcess = new ProcessBuilder(probeCommand).start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(probeProcess.getInputStream()));
                String line = reader.readLine();
                int sampleRate = 0, channels = 0;

                if (line != null && !line.trim().isEmpty()) {
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        sampleRate = Integer.parseInt(parts[0].trim());
                        channels = Integer.parseInt(parts[1].trim());
                    }
                }
                probeProcess.waitFor();

                if (startingStreams.containsKey(streamUrl) && sampleRate != 0 && channels != 0) {
                    int bufferSize = ClientModConfigManager.getConfig().audioBufferSize;
                    ClientAudioInstance instance = new ClientAudioInstance(streamUrl, sampleRate, channels, bufferSize);
                    audioInstances.put(streamUrl, instance);
                    instance.startStream();
                }
            } catch (Exception ignored) {
            } finally {
                startingStreams.remove(streamUrl);
            }
        }).start();
    }

    public static void cleanup() {
        stopAudioInstances();
        startingStreams.clear();
    }

    public static void stopAudioInstances() {
        audioInstances.values().forEach(ClientAudioInstance::stopStream);
    }

    public static void onConfigChanged() {
        int newBufferSize = ClientModConfigManager.getConfig().audioBufferSize;
        audioInstances.forEach((streamUrl, instance) -> {
            if (instance.isPlaying()) {
                instance.stopStream();
                ClientAudioInstance newInstance = new ClientAudioInstance(
                        streamUrl, instance.sampleRate, instance.channels, newBufferSize);
                audioInstances.put(streamUrl, newInstance);
                newInstance.startStream();
            }
        });
    }

    public static class ClientAudioInstance {
        private static final int AUDIO_LINE_BUFFER = 4096;
        private static final int AUDIO_LINE_BUFFER_MULTIPLIER = 8;
        private static final int READ_BUFFER_SIZE = 16384;
        private static final int BYTES_PER_SAMPLE = 2;
        private static final int RECONNECT_DELAY_SECONDS = 1;
        private final String streamUrl;
        private final int sampleRate;
        private final int channels;
        private final int frameSize;
        private final int maxBufferSize;
        private final AtomicBoolean isPlaying;
        private final AtomicBoolean isStarting;
        private final BlockingQueue<byte[]> audioQueue;
        private ScheduledExecutorService scheduler;
        private Process ffmpegProcess;
        private SourceDataLine audioLine;
        private FloatControl volumeControl;
        private float currentVolume;
        private volatile boolean stopRequested;

        public ClientAudioInstance(String streamUrl, int sampleRate, int channels, int maxBufferSize) {
            this.streamUrl = streamUrl;
            this.sampleRate = sampleRate;
            this.channels = channels;
            this.frameSize = channels * BYTES_PER_SAMPLE;
            this.maxBufferSize = maxBufferSize;
            this.isPlaying = new AtomicBoolean(false);
            this.isStarting = new AtomicBoolean(false);
            this.audioQueue = new LinkedBlockingQueue<>(maxBufferSize);
            this.stopRequested = false;
            this.currentVolume = 0.0f;
        }

        public int getMaxBufferSize() {
            return maxBufferSize;
        }

        private void initializeAudioLine() throws LineUnavailableException {
            AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            audioLine = (SourceDataLine) AudioSystem.getLine(info);
            audioLine.open(format, AUDIO_LINE_BUFFER * AUDIO_LINE_BUFFER_MULTIPLIER);

            if (audioLine.isControlSupported(FloatControl.Type.VOLUME))
                volumeControl = (FloatControl) audioLine.getControl(FloatControl.Type.VOLUME);
            else if (audioLine.isControlSupported(FloatControl.Type.MASTER_GAIN))
                volumeControl = (FloatControl) audioLine.getControl(FloatControl.Type.MASTER_GAIN);
        }

        private Runnable createConnectTask() {
            return new Runnable() {
                @Override
                public void run() {
                    if (stopRequested) return;
                    Process currentProcess = null;
                    try {
                        ProcessBuilder pb = getProcessBuilder();
                        currentProcess = pb.start();
                        ffmpegProcess = currentProcess;
                        InputStream audioStream = ffmpegProcess.getInputStream();
                        byte[] buffer = new byte[READ_BUFFER_SIZE];
                        int bytesRead;
                        while (!stopRequested && (bytesRead = audioStream.read(buffer)) != -1) {
                            if (bytesRead > 0) {
                                int alignedBytes = (bytesRead / frameSize) * frameSize;
                                if (alignedBytes > 0) {
                                    byte[] audioData = new byte[alignedBytes];
                                    System.arraycopy(buffer, 0, audioData, 0, alignedBytes);
                                    while (!stopRequested && !audioQueue.offer(audioData))
                                        audioQueue.poll();
                                }
                            }
                        }
                        if (!stopRequested)
                            scheduler.schedule(this, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        if (!stopRequested)
                            scheduler.schedule(this, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
                    } finally {
                        if (currentProcess != null && currentProcess.isAlive()) currentProcess.destroy();
                    }
                }
            };
        }

        private Thread createPlaybackThread() {
            final long bytesPerSecond = (long) sampleRate * channels * 2;

            return new Thread(() -> {
                try {
                    initializeAudioLine();
                    updateAudioVolume();
                    audioLine.start();

                    long nextWriteTime = System.nanoTime();

                    while (!stopRequested || !audioQueue.isEmpty()) {
                        byte[] audioData = audioQueue.poll(50, TimeUnit.MILLISECONDS);
                        if (audioData != null && audioData.length > 0) {
                            long currentTime = System.nanoTime();
                            if (currentTime < nextWriteTime) {
                                LockSupport.parkNanos(nextWriteTime - currentTime);
                            }
                            audioLine.write(audioData, 0, audioData.length);
                            long nanosToAdd = (audioData.length * 1000000000L) / bytesPerSecond;
                            nextWriteTime += nanosToAdd;
                        } else if (stopRequested) break;
                    }
                    audioLine.drain();
                } catch (Exception e) {
                    if (!stopRequested) stopStream();
                } finally {
                    if (audioLine != null) audioLine.close();
                    volumeControl = null;
                }
            });
        }

        public void startStream() {
            if (!isStarting.compareAndSet(false, true) || isPlaying.get()) return;
            try {
                stopRequested = false;
                audioQueue.clear();
                scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "FFmpeg-Reader-" + streamUrl.hashCode());
                    t.setPriority(Thread.MAX_PRIORITY);
                    t.setDaemon(true);
                    return t;
                });
                Thread playbackThread = createPlaybackThread();
                playbackThread.setPriority(Thread.NORM_PRIORITY);
                playbackThread.setDaemon(true);
                playbackThread.start();
                scheduler.execute(createConnectTask());
                isPlaying.set(true);
            } finally {
                isStarting.set(false);
            }
        }

        private @NotNull ProcessBuilder getProcessBuilder() {
            String[] ffmpegCommand = {
                    "ffmpeg", "-v", "warning", "-i", streamUrl, "-vn",
                    "-af", "acompressor=threshold=-20dB:ratio=4:attack=20:release=200,volume=5.0",
                    "-c:a", "pcm_s16le", "-f", "s16le",
                    "-ar", String.valueOf(sampleRate), "-ac", String.valueOf(channels),
                    "-reconnect", "1", "-reconnect_streamed", "1", "-reconnect_delay_max", "2",
                    "-reconnect_on_network_error", "1", "-reconnect_on_http_error", "1",
                    "-timeout", "5000000", "-buffer_size", "8192000", "-max_delay", "3000000",
                    "-probesize", "20000000", "-analyzeduration", "20000000",
                    "-threads", "1", "-nostats", "pipe:1"
            };
            ProcessBuilder pb = new ProcessBuilder(ffmpegCommand);
            pb.redirectErrorStream(false);
            return pb;
        }

        public void stopStream() {
            stopRequested = true;
            if (!isPlaying.getAndSet(false)) return;
            if (ffmpegProcess != null) ffmpegProcess.destroy();
            if (scheduler != null) scheduler.shutdown();
            audioQueue.clear();
            if (audioLine != null && audioLine.isOpen()) {
                audioLine.stop();
                audioLine.flush();
                audioLine.close();
            }
            volumeControl = null;
        }

        public void setVolume(float volume) {
            this.currentVolume = volume;
            updateAudioVolume();
        }

        private void updateAudioVolume() {
            if (volumeControl == null) return;
            if (currentVolume > VOLUME_THRESHOLD) {
                float effectiveVolume = (float) Math.pow(currentVolume, 1.0f / 3);
                effectiveVolume = Math.min(effectiveVolume, 1.0f);
                if (volumeControl.getType() == FloatControl.Type.VOLUME) {
                    float min = volumeControl.getMinimum(), max = volumeControl.getMaximum();
                    volumeControl.setValue(min + (max - min) * effectiveVolume);
                } else if (volumeControl.getType() == FloatControl.Type.MASTER_GAIN) {
                    float minDB = volumeControl.getMinimum(), maxDB = volumeControl.getMaximum();
                    volumeControl.setValue(minDB + (maxDB - minDB) * effectiveVolume);
                }
            } else {
                volumeControl.setValue(volumeControl.getMinimum());
            }
        }

        public boolean isPlaying() {
            return isPlaying.get();
        }
    }
}