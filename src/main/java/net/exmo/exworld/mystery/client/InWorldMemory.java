package net.exmo.exworld.mystery.client;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.client.memory.ReForgedMemoryClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Connected, first-person scene replay. ReForgedPlay separately stores the full packet history. */
public final class InWorldMemory {
    private static final long MIN_FREE_BYTES = 32L * 1024 * 1024;
    private static final long SAMPLE_NS = 100_000_000L;
    private static UUID run;
    private static UUID failedRun;
    private static long startedAt, lastSample, lastDiskCheck, lastVoiceDiskCheck, pausedAt, pausedNanos;
    private static boolean recording, paused, watchingWorld;
    private static Path base;
    private static WorldMemoryTrack.Writer scene;
    private static RandomAccessFile voiceFile;
    private static BufferedWriter sounds;
    private static long voiceSamples;
    private static MemoryScreen screen;
    private static volatile WorldMemoryTrack.Frame playbackPose;
    private static volatile String error = "";

    private record SoundCue(long millis, String id, float volume, float pitch) {}

    private InWorldMemory() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(InWorldMemory::capture);
        Runtime.getRuntime().addShutdownHook(new Thread(InWorldMemory::stop, "exworld-memory-shutdown"));
    }

    public static void sync(UUID nextRun, String phase, String nextState, String knowledge) {
        Minecraft mc = Minecraft.getInstance();
        if (nextRun == null || phase.equals("LOBBY") || phase.equals("FINISHED")) {
            stop(); closeScreen(); run = nextRun; return;
        }
        boolean changedRun = !nextRun.equals(run);
        if (changedRun) {
            stop(); closeScreen(); run = nextRun; failedRun = null; base = null; watchingWorld = false;
        }
        if (paused && !changedRun) {
            pausedNanos += System.nanoTime() - pausedAt;
            paused = false;
            ReForgedMemoryClient.markMysteryRun(nextRun, true);
        }
        boolean alive = nextState.equals("PAST_ACTIVE") || nextState.equals("FUTURE_ACTIVE") || nextState.equals("FUTURE_DOOMED");
        boolean dead = nextState.equals("PAST_DEAD") || nextState.equals("FUTURE_DEAD");
        if (alive && !recording && !nextRun.equals(failedRun)) start(nextRun);
        if (dead && screen == null && !watchingWorld) {
            stop();
            mc.execute(() -> {
                if (screen != null) return;
                screen = new MemoryScreen(base, knowledge, error);
                mc.setScreen(screen);
            });
        }
    }

    private static synchronized void start(UUID id) {
        if (recording) return;
        try {
            Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("exworld/mystery/memory");
            Files.createDirectories(dir);
            if (Files.getFileStore(dir).getUsableSpace() < MIN_FREE_BYTES) {
                error = "磁盘空间不足，无法记录本轮世界轨迹"; failedRun = id; return;
            }
            base = dir.resolve(id.toString());
            error = "";
            startedAt = System.nanoTime(); lastSample = 0; lastDiskCheck = 0; lastVoiceDiskCheck = 0;
            pausedNanos = 0; paused = false; voiceSamples = 0;
            scene = WorldMemoryTrack.open(Path.of(base + ".xwr"));
            voiceFile = new RandomAccessFile(base + ".wav", "rw");
            voiceFile.setLength(0); writeWavHeader(voiceFile, 0);
            sounds = Files.newBufferedWriter(Path.of(base + ".sounds"), StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            recording = true;
            ReForgedMemoryClient.markMysteryRun(id, true);
        } catch (Exception failure) {
            error = "世界轨迹初始化失败：" + failure.getMessage(); failedRun = id;
            Exworld.LOGGER.error("Mystery world memory could not start", failure);
            recording = false; closeFiles();
        }
    }

    private static void capture(ClientTickEvent.Post event) {
        if (!recording || paused) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        long now = System.nanoTime();
        if (now - lastSample < SAMPLE_NS) return;
        lastSample = now;
        if (now - lastDiskCheck > 10_000_000_000L) {
            lastDiskCheck = now;
            try {
                if (Files.getFileStore(base.getParent()).getUsableSpace() < MIN_FREE_BYTES) {
                    error = "磁盘空间不足，世界轨迹已停止"; failedRun = run; stop(); return;
                }
            } catch (IOException failure) { error = "磁盘检测失败"; failedRun = run; stop(); return; }
        }
        try {
            var camera = mc.gameRenderer.getMainCamera();
            var position = camera.getPosition();
            List<WorldMemoryTrack.Actor> actors = new ArrayList<>();
            for (Entity entity : mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(32), e -> true)) {
                if (actors.size() == 64) break;
                actors.add(new WorldMemoryTrack.Actor(entity.getId(),
                        BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(),
                        entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot()));
            }
            int millis = (int)Math.min(Integer.MAX_VALUE, (now - startedAt - pausedNanos) / 1_000_000L);
            scene.write(new WorldMemoryTrack.Frame(millis, mc.level.dimension().location().toString(),
                    position.x, position.y, position.z, camera.getYRot(), camera.getXRot(),
                    mc.level.getDayTime(), actors));
        } catch (Exception failure) {
            error = "世界轨迹写入失败：" + failure.getMessage(); failedRun = run;
            Exworld.LOGGER.error("Mystery world memory write failed", failure);
            stop();
        }
    }

    /** Raw 48 kHz voice received from the already authorized Simple Voice Chat session. */
    public static synchronized void voice(short[] samples) {
        if (!recording || paused || voiceFile == null || samples == null || samples.length == 0) return;
        try {
            long now = System.nanoTime();
            long target = Math.max(0, (now - startedAt - pausedNanos) * 48_000 / 1_000_000_000L);
            long end = target + samples.length;
            if (end > (Integer.MAX_VALUE - 44L) / 2L) throw new IOException("Voice track exceeds WAV limit");
            long desired = 44 + end * 2;
            if (now - lastVoiceDiskCheck >= 10_000_000_000L) {
                lastVoiceDiskCheck = now;
                if (Files.getFileStore(base.getParent()).getUsableSpace() <
                        Math.max(0, desired - voiceFile.length()) + MIN_FREE_BYTES)
                    throw new IOException("Insufficient disk space for voice track");
            }
            voiceFile.setLength(Math.max(voiceFile.length(), desired));
            byte[] mixed = new byte[samples.length * 2];
            voiceFile.seek(44 + target * 2); voiceFile.readFully(mixed);
            for (int i = 0; i < samples.length; i++) {
                int prior = (short)((mixed[i * 2] & 0xFF) | (mixed[i * 2 + 1] << 8));
                int combined = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, prior + samples[i]));
                mixed[i * 2] = (byte)combined; mixed[i * 2 + 1] = (byte)(combined >>> 8);
            }
            voiceFile.seek(44 + target * 2); voiceFile.write(mixed);
            voiceSamples = Math.max(voiceSamples, end);
        } catch (IOException failure) { error = "语音录制失败：" + failure.getMessage(); failedRun = run; stop(); }
    }

    public static synchronized void sound(SoundInstance instance) {
        if (!recording || paused || sounds == null || instance == null) return;
        try {
            // Some modded sounds and missing resources never resolve. Recording is optional
            // and must not turn a harmless absent sound into a client crash.
            if (instance.getSound() == null || instance.getLocation() == null) return;
            sounds.write((System.nanoTime() - startedAt - pausedNanos) / 1_000_000
                    + "\t" + instance.getLocation() + "\t" + instance.getVolume()
                    + "\t" + instance.getPitch() + "\n");
        } catch (IOException failure) { error = "音效记录失败：" + failure.getMessage(); failedRun = run; stop(); }
        catch (RuntimeException malformedSound) {
            Exworld.LOGGER.debug("Skipping unresolved mystery memory sound", malformedSound);
        }
    }

    public static synchronized void stop() {
        if (!recording) return;
        recording = false;
        closeFiles();
        ReForgedMemoryClient.markMysteryRun(run, false);
    }

    public static void disconnected() {
        if (recording && !paused) { paused = true; pausedAt = System.nanoTime(); }
        closeScreen(); watchingWorld = false;
    }

    public static boolean isPlaybackOpen() { return screen != null; }
    public static WorldMemoryTrack.Frame playbackPose() { return playbackPose; }

    private static void closeFiles() {
        if (scene != null) try { scene.close(); } catch (IOException failure) {
            Exworld.LOGGER.warn("Cannot finish mystery world track", failure);
        } finally { scene = null; }
        RandomAccessFile file = voiceFile; voiceFile = null;
        if (file != null) {
            try { writeWavHeader(file, voiceSamples * 2); }
            catch (IOException failure) { Exworld.LOGGER.warn("Cannot finish mystery voice track", failure); }
            finally { try { file.close(); } catch (IOException ignored) {} }
        }
        try { if (sounds != null) sounds.close(); } catch (IOException ignored) {}
        sounds = null;
    }

    private static void writeWavHeader(RandomAccessFile file, long dataBytes) throws IOException {
        file.seek(0); file.writeBytes("RIFF"); writeIntLE(file, (int)Math.min(Integer.MAX_VALUE, 36 + dataBytes));
        file.writeBytes("WAVEfmt "); writeIntLE(file, 16);
        writeShortLE(file, 1); writeShortLE(file, 1);
        writeIntLE(file, 48_000); writeIntLE(file, 96_000);
        writeShortLE(file, 2); writeShortLE(file, 16);
        file.writeBytes("data"); writeIntLE(file, (int)Math.min(Integer.MAX_VALUE, dataBytes));
        file.seek(44 + dataBytes);
    }

    private static void writeIntLE(RandomAccessFile file, int value) throws IOException {
        file.writeByte(value); file.writeByte(value >>> 8); file.writeByte(value >>> 16); file.writeByte(value >>> 24);
    }
    private static void writeShortLE(RandomAccessFile file, int value) throws IOException {
        file.writeByte(value); file.writeByte(value >>> 8);
    }

    private static void closeScreen() {
        MemoryScreen old = screen; screen = null; playbackPose = null;
        if (old != null) {
            old.closePlayback();
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == old) mc.setScreen(null);
        }
    }

    private static final class MemoryScreen extends Screen {
        private final Path path;
        private final String knowledge;
        private final String reason;
        private volatile List<WorldMemoryTrack.Frame> timeline = List.of();
        private volatile List<SoundCue> cues = List.of();
        private volatile boolean playing = true;
        private volatile long playbackMillis, generation;
        private volatile long origin;
        private int cueIndex;
        private Thread loader, voicePlayer;

        MemoryScreen(Path path, String knowledge, String reason) {
            super(Component.literal("生前记忆"));
            this.path = path; this.knowledge = knowledge; this.reason = reason;
            load();
        }

        @Override protected void init() {
            addRenderableWidget(Button.builder(Component.literal("旁观世界"), ignored -> leave(false))
                    .bounds(width / 2 - 105, height - 48, 100, 20).build());
            addRenderableWidget(Button.builder(Component.literal("查看手记"), ignored -> leave(true))
                    .bounds(width / 2 + 5, height - 48, 100, 20).build());
        }

        private void leave(boolean journal) {
            watchingWorld = true;
            closeScreen();
            if (journal) Minecraft.getInstance().setScreen(new MysteryJournalScreen());
        }

        private void load() {
            if (path == null) return;
            loader = new Thread(() -> {
                try {
                    Path file = Path.of(path + ".xwr");
                    List<WorldMemoryTrack.Frame> loaded = Files.isRegularFile(file) ? WorldMemoryTrack.read(file) : List.of();
                    cues = readCues(Path.of(path + ".sounds"));
                    origin = System.nanoTime();
                    timeline = loaded;
                    if (Files.isRegularFile(Path.of(path + ".wav"))) {
                        voicePlayer = new Thread(() -> playVoice(Path.of(path + ".wav")), "exworld-memory-voice");
                        voicePlayer.setDaemon(true); voicePlayer.start();
                    }
                } catch (IOException failure) {
                    error = "世界轨迹读取失败：" + failure.getMessage();
                    Exworld.LOGGER.warn("Cannot read mystery world track", failure);
                }
            }, "exworld-memory-loader");
            loader.setDaemon(true); loader.start();
        }

        @Override public void tick() {
            List<WorldMemoryTrack.Frame> frames = timeline;
            if (frames.isEmpty()) return;
            int duration = Math.max(1, frames.getLast().millis());
            long elapsed = (System.nanoTime() - origin) / 1_000_000L;
            long current = elapsed % duration;
            if (current < playbackMillis) { generation++; cueIndex = 0; }
            playbackMillis = current;
            WorldMemoryTrack.Frame pose = WorldMemoryTrack.at(frames, current);
            Minecraft mc = Minecraft.getInstance();
            playbackPose = pose != null && mc.level != null &&
                    pose.dimension().equals(mc.level.dimension().location().toString()) ? pose : null;
            List<SoundCue> events = cues;
            while (cueIndex < events.size() && events.get(cueIndex).millis <= current) {
                SoundCue cue = events.get(cueIndex++);
                ResourceLocation id = ResourceLocation.tryParse(cue.id);
                if (id == null) continue;
                var sound = BuiltInRegistries.SOUND_EVENT.get(id);
                if (sound != null) mc.getSoundManager().play(
                        net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(sound, cue.pitch, cue.volume));
            }
        }

        private void playVoice(Path file) {
            AudioFormat format = new AudioFormat(48_000, 16, 1, true, false);
            SourceDataLine line = null;
            try {
                if (Files.size(file) <= 44) return;
                line = (SourceDataLine)AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
                line.open(format); line.start();
                byte[] bytes = new byte[4_800];
                while (playing) {
                    long playingLoop = generation;
                    long voiceMillis = 0;
                    try (var input = Files.newInputStream(file)) {
                        input.skipNBytes(44);
                        int length;
                        while (playing && playingLoop == generation && (length = input.read(bytes)) >= 0) {
                            while (playing && playingLoop == generation && playbackMillis + 30 < voiceMillis)
                                Thread.sleep(5);
                            if (!playing || playingLoop != generation) break;
                            line.write(bytes, 0, length);
                            voiceMillis += length * 1000L / 96_000L;
                        }
                    }
                    line.flush();
                    while (playing && playingLoop == generation) Thread.sleep(10);
                }
            } catch (Exception failure) { Exworld.LOGGER.debug("Voice track unavailable for mystery memory", failure); }
            finally { if (line != null) { line.stop(); line.close(); } }
        }

        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, 27, 0xB810141B);
            graphics.fill(0, height - 76, width, height, 0xB810141B);
            String status = timeline.isEmpty()
                    ? (!error.isBlank() ? error : reason.isBlank() ? "正在读取世界轨迹…" : reason)
                    : "生前记忆 · 世界视角循环 · " + playbackMillis / 1000 + " 秒 · 等待下一轮";
            graphics.drawCenteredString(font, status, width / 2, 9, 0xFFE9DCA9);
            if (!knowledge.isBlank()) graphics.drawCenteredString(font,
                    "已知线索：" + font.plainSubstrByWidth(knowledge.replace('\n', '、'), width - 36),
                    width / 2, height - 16, 0xFFC9D2DD);
            super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override public boolean isPauseScreen() { return false; }
        @Override public void onClose() { /* Only the next run closes death waiting automatically. */ }

        private void closePlayback() {
            playing = false; playbackPose = null;
            if (loader != null) loader.interrupt();
            if (voicePlayer != null) voicePlayer.interrupt();
        }

        private static List<SoundCue> readCues(Path path) {
            if (!Files.isRegularFile(path)) return List.of();
            List<SoundCue> values = new ArrayList<>();
            try {
                for (String line : Files.readAllLines(path)) {
                    String[] parts = line.split("\\t");
                    if (parts.length == 4) values.add(new SoundCue(Long.parseLong(parts[0]), parts[1],
                            Float.parseFloat(parts[2]), Float.parseFloat(parts[3])));
                }
            } catch (Exception failure) { Exworld.LOGGER.debug("Cannot read mystery sound events", failure); }
            return values;
        }
    }
}
