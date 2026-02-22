package me.yuugao.meyuugaosradio.client.sound;

import static me.yuugao.meyuugaosradio.Constants.CLIENT_LOGGER;


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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import javax.sound.sampled.*;

public class ClientHlsAudioManager {
    private static final Map<String, ClientAudioInstance> audioInstances = new ConcurrentHashMap<>();
    private static final Map<String, AtomicBoolean> startingStreams = new ConcurrentHashMap<>();

    public static void handleVolumeUpdate(String streamUrl, float volume) {
        float finalVolume = volume * MinecraftClient.getInstance().options.getSoundVolume(SoundCategory.MASTER) * ClientModConfigManager.getConfig().volume / 100;

        ClientAudioInstance instance = audioInstances.get(streamUrl);
        if (instance != null) {
            instance.setVolume(finalVolume);

            if (finalVolume > 0.001f) {
                if (!instance.isPlaying()) {
                    instance.startStream();
                }
            } else if (finalVolume <= 0.001f) {
                if (instance.isPlaying()) {
                    instance.stopStream();
                }
            }
        } else if (finalVolume > 0.001f) {
            detectAndStartStream(streamUrl);
        }
    }

    public static void handleStreamStart(String streamUrl) {
        if (!audioInstances.containsKey(streamUrl)) {
            detectAndStartStream(streamUrl);
        }
    }

    public static void handleStreamStop(String streamUrl) {
        startingStreams.remove(streamUrl);

        ClientAudioInstance instance = audioInstances.remove(streamUrl);
        if (instance != null) {
            instance.stopStream();
        }
    }

    private static void detectAndStartStream(String streamUrl) {
        AtomicBoolean isStarting = startingStreams.computeIfAbsent(streamUrl, key -> new AtomicBoolean(false));

        if (!isStarting.compareAndSet(false, true)) {
            return;
        }

        new Thread(() -> {
            try {
                String[] probeCommand = {
                        "ffprobe",
                        "-v", "quiet",
                        "-print_format", "csv",
                        "-show_entries", "stream=sample_rate,channels",
                        "-of", "csv=p=0",
                        streamUrl
                };

                Process probeProcess = new ProcessBuilder(probeCommand).start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(probeProcess.getInputStream()));
                String line = reader.readLine();

                int sampleRate = 0;
                int channels = 0;

                if (line != null && !line.trim().isEmpty()) {
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        sampleRate = Integer.parseInt(parts[0].trim());
                        channels = Integer.parseInt(parts[1].trim());
                    }
                }

                probeProcess.waitFor();

                if (startingStreams.containsKey(streamUrl) && sampleRate != 0 && channels != 0) {
                    ClientAudioInstance instance = new ClientAudioInstance(streamUrl, sampleRate, channels);
                    audioInstances.put(streamUrl, instance);
                    instance.startStream();
                }
            } catch (Exception ignored) {
            } finally {
                startingStreams.remove(streamUrl);
            }
        }).start();
    }

    public static class ClientAudioInstance {
        private final String streamUrl;
        private final int sampleRate;
        private final int channels;
        private final AtomicBoolean isPlaying;
        private final AtomicBoolean isStarting;
        private final BlockingQueue<byte[]> audioQueue;
        private ScheduledExecutorService scheduler;

        private Process ffmpegProcess;
        private Thread playbackThread;
        private Thread readThread;

        private volatile boolean stopRequested;
        private SourceDataLine audioLine;
        private FloatControl volumeControl;
        private float currentVolume;

        public ClientAudioInstance(String streamUrl, int sampleRate, int channels) {
            this.streamUrl = streamUrl;
            this.sampleRate = sampleRate;
            this.channels = channels;
            this.isPlaying = new AtomicBoolean(false);
            this.isStarting = new AtomicBoolean(false);
            this.audioQueue = new LinkedBlockingQueue<>(30);
            this.stopRequested = false;
            this.currentVolume = 0.0f;
        }

        public void startStream() {
            if (!isStarting.compareAndSet(false, true) || isPlaying.get()) {
                return;
            }

            try {
                stopRequested = false;
                audioQueue.clear();

                scheduler = new ScheduledThreadPoolExecutor(1);

                final int frameSize = channels * 2;
                final long bytesPerSecond = (long) sampleRate * channels * 2;

                readThread = new Thread(() -> {
                    final AtomicInteger restartAttempts = new AtomicInteger(0);
                    final int maxRestartAttempts = 3;

                    Runnable connectTask = new Runnable() {
                        @Override
                        public void run() {
                            if (stopRequested || restartAttempts.get() >= maxRestartAttempts) {
                                return;
                            }

                            try {
                                ProcessBuilder pb = getProcessBuilder();
                                ffmpegProcess = pb.start();

                                InputStream audioStream = ffmpegProcess.getInputStream();
                                byte[] buffer = new byte[4096];
                                int bytesRead;

                                while (!stopRequested && (bytesRead = audioStream.read(buffer)) != -1) {
                                    if (bytesRead > 0) {
                                        int alignedBytes = (bytesRead / frameSize) * frameSize;
                                        if (alignedBytes > 0) {
                                            byte[] audioData = new byte[alignedBytes];
                                            System.arraycopy(buffer, 0, audioData, 0, alignedBytes);

                                            if (!audioQueue.offer(audioData, 100, TimeUnit.MILLISECONDS)) {
                                                CLIENT_LOGGER.info("Audio queue is full, skipping...");
                                            }
                                        }
                                    }
                                }

                                if (!stopRequested) {
                                    restartAttempts.incrementAndGet();
                                    scheduler.schedule(this, 2, TimeUnit.SECONDS);
                                }

                                restartAttempts.set(0);
                            } catch (Exception e) {
                                if (!stopRequested) {
                                    int attempts = restartAttempts.incrementAndGet();
                                    if (attempts < maxRestartAttempts) {
                                        CLIENT_LOGGER.info("FFMPEG connection failed, attempt {}/{}. Retrying in 2s...",
                                                attempts, maxRestartAttempts);
                                        CLIENT_LOGGER.debug("Connection error details: ", e);
                                        scheduler.schedule(this, 2, TimeUnit.SECONDS);
                                    } else {
                                        CLIENT_LOGGER.warn("FFMPEG connection failed after {} attempts. Stopping stream",
                                                attempts);
                                        CLIENT_LOGGER.debug("Final connection error details: ", e);
                                        stopStream();
                                    }
                                }
                            } finally {
                                if (ffmpegProcess != null) {
                                    ffmpegProcess.destroy();
                                }
                            }
                        }
                    };

                    scheduler.execute(connectTask);
                });

                playbackThread = new Thread(() -> {
                    try {
                        AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
                        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

                        if (!AudioSystem.isLineSupported(info)) {
                            return;
                        }

                        audioLine = (SourceDataLine) AudioSystem.getLine(info);
                        audioLine.open(format, 4096 * 8);

                        if (audioLine.isControlSupported(FloatControl.Type.VOLUME)) {
                            volumeControl = (FloatControl) audioLine.getControl(FloatControl.Type.VOLUME);
                        } else if (audioLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                            volumeControl = (FloatControl) audioLine.getControl(FloatControl.Type.MASTER_GAIN);
                        }

                        updateAudioVolume();
                        audioLine.start();

                        long nextWriteTime = System.nanoTime();

                        while (!stopRequested || !audioQueue.isEmpty()) {
                            byte[] audioData = audioQueue.poll(50, TimeUnit.MILLISECONDS);
                            if (audioData != null && audioData.length > 0) {
                                long currentTime = System.nanoTime();
                                if (currentTime < nextWriteTime) {
                                    long sleepNanos = nextWriteTime - currentTime;
                                    LockSupport.parkNanos(sleepNanos);
                                }

                                audioLine.write(audioData, 0, audioData.length);
                                long nanosToAdd = (audioData.length * 1000000000L) / bytesPerSecond;
                                nextWriteTime += nanosToAdd;
                            } else if (stopRequested) {
                                break;
                            }
                        }

                        audioLine.drain();
                    } catch (Exception e) {
                        if (!stopRequested) {
                            CLIENT_LOGGER.warn("Playback thread aborted due to exception: {}",
                                    e.getMessage());
                            CLIENT_LOGGER.debug("Playback thread abort details: ", e);
                            stopStream();
                        }
                    } finally {
                        if (audioLine != null) {
                            audioLine.close();
                        }
                        volumeControl = null;
                    }
                });

                readThread.setDaemon(true);
                playbackThread.setDaemon(true);
                readThread.start();
                playbackThread.start();
                isPlaying.set(true);
            } finally {
                isStarting.set(false);
            }
        }

        private @NotNull ProcessBuilder getProcessBuilder() {
            String[] ffmpegCommand = {
                    "ffmpeg",
                    "-v", "warning",
                    "-i", streamUrl,
                    "-vn",
                    "-af", "acompressor=threshold=-20dB:ratio=4:attack=20:release=200,volume=5.0",
                    "-c:a", "pcm_s16le",
                    "-f", "s16le",
                    "-ar", String.valueOf(sampleRate),
                    "-ac", String.valueOf(channels),
                    "-reconnect", "1",
                    "-reconnect_streamed", "1",
                    "-reconnect_delay_max", "2",
                    "-reconnect_on_network_error", "1",
                    "-fflags", "+discardcorrupt+genpts+igndts",
                    "-avoid_negative_ts", "make_zero",
                    "-max_delay", "500000",
                    "-bufsize", String.valueOf(sampleRate * 2),
                    "-threads", "1",
                    "-nostats",
                    "-use_wallclock_as_timestamps", "1",
                    "pipe:1"
            };

            ProcessBuilder pb = new ProcessBuilder(ffmpegCommand);
            pb.redirectErrorStream(true);
            return pb;
        }

        public void stopStream() {
            if (!isPlaying.getAndSet(false)) {
                return;
            }

            stopRequested = true;

            if (ffmpegProcess != null) {
                ffmpegProcess.destroy();
            }

            audioQueue.clear();

            if (audioLine != null && audioLine.isOpen()) {
                audioLine.close();
            }

            volumeControl = null;

            if (readThread != null) {
                readThread.interrupt();
            }

            if (playbackThread != null) {
                playbackThread.interrupt();
            }

            if (!scheduler.isShutdown()) {
                scheduler.shutdown();
            }
        }

        public void setVolume(float volume) {
            this.currentVolume = volume;
            updateAudioVolume();
        }

        private void updateAudioVolume() {
            if (volumeControl != null) {
                if (currentVolume > 0.001f) {
                    float effectiveVolume = (float) Math.pow(currentVolume, 0.3);
                    effectiveVolume = Math.min(effectiveVolume, 1.0f);

                    if (volumeControl.getType() == FloatControl.Type.VOLUME) {
                        float min = volumeControl.getMinimum();
                        float max = volumeControl.getMaximum();
                        float volume = min + (max - min) * effectiveVolume;
                        volumeControl.setValue(volume);
                    } else if (volumeControl.getType() == FloatControl.Type.MASTER_GAIN) {
                        float minDB = volumeControl.getMinimum();
                        float maxDB = volumeControl.getMaximum();

                        float gainDB;
                        if (effectiveVolume > 1.0f) {
                            gainDB = (effectiveVolume - 1.0f) * 6.0f;
                        } else {
                            gainDB = minDB * (1.0f - effectiveVolume);
                        }

                        volumeControl.setValue(Math.max(minDB, Math.min(maxDB, gainDB)));
                    }
                } else {
                    if (volumeControl.getType() == FloatControl.Type.VOLUME || volumeControl.getType() == FloatControl.Type.MASTER_GAIN) {
                        volumeControl.setValue(volumeControl.getMinimum());
                    }
                }
            }
        }

        public boolean isPlaying() {
            return isPlaying.get();
        }
    }

    public static void cleanup() {
        stopAudioInstances();
        startingStreams.clear();
    }

    public static void stopAudioInstances() {
        audioInstances.values().forEach(ClientAudioInstance::stopStream);
    }
}