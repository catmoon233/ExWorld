package net.exmo.exworld.client.memory;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.exmo.exworld.command.TokenArgument;
import com.replaymod.core.ReplayMod;
import com.replaymod.recording.ReplayModRecording;
import com.replaymod.recording.Setting;
import com.replaymod.recording.packet.PacketListener;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replaystudio.data.Marker;
import com.replaymod.replaystudio.replay.ReplayFile;
import net.exmo.exworld.Exworld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/** A named window in a ReForgedPlay recording, stored as two ordinary replay markers. */
public final class ReForgedMemoryClient {
    private static final String START = "exworld.memory.start:";
    private static final String END = "exworld.memory.end:";
    private static String recordingId;
    private static String lastStoppedId;
    private static long lastStoppedAt;
    private static Requested pending;
    private static Playing playing;
    private static Playing awaitingCompatibility;
    private static long compatibilityDeadline;
    private static int scanDelay;
    private static Boolean renameDialogBeforePlayback;

    private ReForgedMemoryClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ReForgedMemoryClient::registerLayer);
        NeoForge.EVENT_BUS.addListener(ReForgedMemoryClient::commands);
        NeoForge.EVENT_BUS.addListener(ReForgedMemoryClient::tick);
    }

    private static void commands(RegisterClientCommandsEvent event) {
        var memory = Commands.literal("memory");
        memory.then(Commands.literal("record").then(Commands.argument("id", TokenArgument.token())
                .executes(c -> record(StringArgumentType.getString(c, "id")))));
        memory.then(Commands.literal("stop").executes(c -> stop()));
        memory.then(Commands.literal("play").then(Commands.argument("id", TokenArgument.token())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(ids(), b))
                .executes(c -> play(StringArgumentType.getString(c, "id"), false))));
        memory.then(Commands.literal("loop").then(Commands.argument("id", TokenArgument.token())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(ids(), b))
                .executes(c -> play(StringArgumentType.getString(c, "id"), true))));
        memory.then(Commands.literal("halt").executes(c -> halt()));
        memory.then(Commands.literal("list").executes(c -> list()));
        memory.then(Commands.literal("status").executes(c -> status()));
        memory.then(Commands.literal("delete").then(Commands.argument("id", TokenArgument.token())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(ids(), b))
                .executes(c -> delete(StringArgumentType.getString(c, "id")))));
        memory.then(Commands.literal("start").then(Commands.argument("id", TokenArgument.token())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(ids(), b))
                .then(Commands.argument("seconds", FloatArgumentType.floatArg(0))
                        .executes(c -> node(StringArgumentType.getString(c, "id"), true,
                                FloatArgumentType.getFloat(c, "seconds"))))));
        memory.then(Commands.literal("end").then(Commands.argument("id", TokenArgument.token())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(ids(), b))
                .then(Commands.argument("seconds", FloatArgumentType.floatArg(0))
                        .executes(c -> node(StringArgumentType.getString(c, "id"), false,
                                FloatArgumentType.getFloat(c, "seconds"))))));
        event.getDispatcher().register(Commands.literal("exworld").then(memory));
    }

    private static int record(String id) {
        if (!validId(id)) return fail("memory.exworld.bad_id");
        if (recordingId != null || pending != null || playing != null || awaitingCompatibility != null) {
            return fail("memory.exworld.busy");
        }
        if (id.equals(lastStoppedId) || ids().contains(id)) return fail("memory.exworld.duplicate", id);
        if (ReplayMod.instance == null || !ReplayMod.instance.getSettingsRegistry().get(Setting.AUTO_START_RECORDING)) {
            return fail("memory.exworld.auto_record_required");
        }
        PacketListener listener = listener();
        if (listener == null) return fail("memory.exworld.replay_unavailable");
        listener.addMarker(START + id);
        recordingId = id;
        return ok("memory.exworld.recording", id);
    }

    private static int stop() {
        if (recordingId == null) return fail("memory.exworld.not_recording");
        PacketListener listener = listener();
        if (listener == null) {
            recordingId = null;
            return fail("memory.exworld.replay_unavailable");
        }
        String id = recordingId;
        listener.addMarker(END + id);
        recordingId = null;
        lastStoppedId = id;
        lastStoppedAt = System.currentTimeMillis();
        return ok("memory.exworld.saved_after_exit", id);
    }

    private static int play(String id, boolean loop) {
        if (!validId(id)) return fail("memory.exworld.bad_id");
        if (recordingId != null || pending != null || awaitingCompatibility != null) return fail("memory.exworld.busy");
        if (ReplayMod.instance == null || ReplayModReplay.instance == null) return fail("memory.exworld.replay_unavailable");
        long now = System.currentTimeMillis();
        pending = new Requested(id, loop, now + 60_000, id.equals(lastStoppedId) ? lastStoppedAt : 0);
        scanDelay = 0;
        renameDialogBeforePlayback = ReplayMod.instance.getSettingsRegistry().get(Setting.RENAME_DIALOG);
        ReplayMod.instance.getSettingsRegistry().set(Setting.RENAME_DIALOG, false);
        // ReForgedPlay replaces the client connection with a virtual replay connection.
        // The current recording is finalized asynchronously when this connection closes.
        try {
            ReplayHandler handler = ReplayModReplay.instance.getReplayHandler();
            if (handler != null) handler.endReplay();
            else if (Minecraft.getInstance().level != null) Minecraft.getInstance().disconnect();
        } catch (IOException e) {
            pending = null;
            restoreRenameDialog();
            Exworld.LOGGER.error("Unable to leave the current replay", e);
            return fail("memory.exworld.replay_failed");
        }
        return ok("memory.exworld.opening", id);
    }

    private static int halt() {
        boolean hadPending = pending != null || awaitingCompatibility != null;
        pending = null;
        awaitingCompatibility = null;
        restoreRenameDialog();
        ReplayHandler handler = ReplayModReplay.instance == null ? null : ReplayModReplay.instance.getReplayHandler();
        if (playing == null || handler == null) return hadPending ? ok("memory.exworld.halted") : fail("memory.exworld.not_playing");
        try {
            handler.endReplay();
            clearPlayback();
            return ok("memory.exworld.halted");
        } catch (IOException e) {
            Exworld.LOGGER.error("Unable to close memory replay", e);
            return fail("memory.exworld.replay_failed");
        }
    }

    private static int list() {
        List<String> names = ids();
        return ok("memory.exworld.list", names.isEmpty() ? "-" : String.join(", ", names));
    }

    private static int status() {
        return ok("memory.exworld.status", recordingId == null ? "-" : recordingId,
                playing != null ? playing.clip.id : pending != null ? pending.id : awaitingCompatibility != null ? awaitingCompatibility.clip.id : "-",
                ids().size());
    }

    private static int delete(String id) {
        if (!validId(id)) return fail("memory.exworld.bad_id");
        if (id.equals(recordingId) || (playing != null && id.equals(playing.clip.id))
                || (pending != null && id.equals(pending.id))) return fail("memory.exworld.busy");
        int removed = 0;
        for (Path path : replayFiles()) {
            try (ReplayFile file = ReplayMod.instance.files.open(path)) {
                Set<Marker> markers = new HashSet<>(file.getMarkers().or(Set.of()));
                if (markers.removeIf(marker -> (START + id).equals(marker.getName()) || (END + id).equals(marker.getName()))) {
                    file.writeMarkers(markers);
                    file.save();
                    removed++;
                }
            } catch (IOException e) {
                Exworld.LOGGER.warn("Unable to update replay {}", path, e);
            }
        }
        if (removed == 0) return fail("memory.exworld.missing", id);
        if (id.equals(lastStoppedId)) lastStoppedId = null;
        return ok("memory.exworld.deleted", id);
    }

    private static int node(String id, boolean start, float seconds) {
        if (!validId(id) || !Float.isFinite(seconds) || seconds > Integer.MAX_VALUE / 1000.0F) {
            return fail("memory.exworld.bad_id");
        }
        if (playing != null && id.equals(playing.clip.id)) return fail("memory.exworld.busy");
        Clip clip = find(id);
        if (clip == null) return fail("memory.exworld.missing", id);
        int millis = Math.round(seconds * 1000.0F);
        if (start ? millis >= clip.end : millis <= clip.start) return fail("memory.exworld.bad_range");
        try (ReplayFile file = ReplayMod.instance.files.open(clip.path)) {
            int duration = file.getMetaData().getDuration();
            if (millis > duration || (start && millis == duration)) return fail("memory.exworld.bad_range");
            Set<Marker> markers = new HashSet<>(file.getMarkers().or(Set.of()));
            String markerName = (start ? START : END) + id;
            markers.removeIf(marker -> markerName.equals(marker.getName()));
            Marker marker = new Marker();
            marker.setName(markerName);
            marker.setTime(millis);
            markers.add(marker);
            file.writeMarkers(markers);
            file.save();
            return ok("memory.exworld.nodes", id, start ? millis : clip.start, start ? clip.end : millis);
        } catch (IOException e) {
            Exworld.LOGGER.error("Unable to edit memory marker in {}", clip.path, e);
            return fail("memory.exworld.replay_failed");
        }
    }

    private static void tick(ClientTickEvent.Post event) {
        if (recordingId != null && listener() == null) recordingId = null;
        if (awaitingCompatibility != null) {
            ReplayHandler handler = ReplayModReplay.instance == null ? null : ReplayModReplay.instance.getReplayHandler();
            if (handler != null) {
                playing = awaitingCompatibility;
                awaitingCompatibility = null;
                handler.doJump(playing.clip.start, false);
            } else if (System.currentTimeMillis() >= compatibilityDeadline) {
                awaitingCompatibility = null;
            }
        }
        if (pending != null && Minecraft.getInstance().level == null && ++scanDelay >= 20) {
            scanDelay = 0;
            Requested request = pending;
            Clip clip = find(request.id);
            if (clip != null && clip.modified < request.notBefore) clip = null;
            if (clip != null) {
                pending = null;
                restoreRenameDialog();
                open(clip, request.loop);
            } else if (System.currentTimeMillis() >= request.deadline) {
                pending = null;
                restoreRenameDialog();
                fail("memory.exworld.missing", request.id);
            }
        }
        if (playing == null) return;
        ReplayHandler handler = ReplayModReplay.instance == null ? null : ReplayModReplay.instance.getReplayHandler();
        if (handler == null) {
            clearPlayback();
            return;
        }
        if (Minecraft.getInstance().level != null && !OldTvFilter.wanted()) OldTvFilter.enable();
        int time = handler.getReplaySender().currentTimeStamp();
        OldTvFilter.tick(Math.max(0, time - playing.clip.start) / 1000.0F);
        if (time < playing.clip.end) return;
        if (playing.loop) handler.doJump(playing.clip.start, false);
        else {
            try {
                handler.endReplay();
            } catch (IOException e) {
                Exworld.LOGGER.error("Unable to finish memory replay", e);
            }
            clearPlayback();
        }
    }

    private static void open(Clip clip, boolean loop) {
        try {
            ReplayModReplay.instance.startReplay(clip.path.toFile());
            ReplayHandler handler = ReplayModReplay.instance.getReplayHandler();
            if (handler == null) {
                // ReForgedPlay is showing its mod compatibility dialog. Finish after the user loads it.
                awaitingCompatibility = new Playing(clip, loop);
                compatibilityDeadline = System.currentTimeMillis() + 60_000;
                return;
            }
            playing = new Playing(clip, loop);
            handler.doJump(clip.start, false);
        } catch (IOException | RuntimeException e) {
            Exworld.LOGGER.error("Unable to open memory replay {}", clip.path, e);
            fail("memory.exworld.replay_failed");
            clearPlayback();
        }
    }

    private static void clearPlayback() {
        playing = null;
        OldTvFilter.disable();
    }

    private static void restoreRenameDialog() {
        if (renameDialogBeforePlayback == null || ReplayMod.instance == null) return;
        ReplayMod.instance.getSettingsRegistry().set(Setting.RENAME_DIALOG, renameDialogBeforePlayback);
        renameDialogBeforePlayback = null;
    }

    private static PacketListener listener() {
        ReplayModRecording module = ReplayModRecording.instance;
        if (module == null || module.getConnectionEventHandler() == null) return null;
        return module.getConnectionEventHandler().getPacketListener();
    }

    /** Mark a mystery life in the already active full-world packet recording. */
    public static void markMysteryRun(UUID run, boolean start) {
        if (run == null) return;
        PacketListener active = listener();
        if (active != null) try {
            active.addMarker("exworld.mystery." + (start ? "start:" : "end:") + run);
        } catch (RuntimeException failure) {
            Exworld.LOGGER.warn("Could not mark mystery run in packet replay", failure);
        }
    }

    private static Clip find(String id) {
        return clips().stream().filter(clip -> id.equals(clip.id))
                .max(Comparator.comparingLong(clip -> clip.modified)).orElse(null);
    }

    private static List<String> ids() {
        return clips().stream().map(clip -> clip.id).distinct().sorted().toList();
    }

    private static List<Clip> clips() {
        List<Clip> result = new ArrayList<>();
        if (ReplayMod.instance == null) return result;
        for (Path path : replayFiles()) {
            try (ReplayFile file = ReplayMod.instance.files.open(path)) {
                Set<Marker> markers = file.getMarkers().or(Set.of());
                long modified = Files.getLastModifiedTime(path).toMillis();
                for (Marker first : markers) {
                    if (first.getName() == null || !first.getName().startsWith(START)) continue;
                    String id = first.getName().substring(START.length());
                    if (!validId(id)) continue;
                    int end = markers.stream().filter(marker -> (END + id).equals(marker.getName()))
                            .mapToInt(Marker::getTime).filter(time -> time > first.getTime()).min().orElse(-1);
                    if (end > first.getTime()) result.add(new Clip(id, path, first.getTime(), end, modified));
                }
            } catch (IOException e) {
                // A recording may still be saving, or an unrelated replay may be damaged.
                Exworld.LOGGER.debug("Skipping unreadable replay {}", path, e);
            }
        }
        return result;
    }

    private static List<Path> replayFiles() {
        if (ReplayMod.instance == null) return List.of();
        try (Stream<Path> files = Files.list(ReplayMod.instance.folders.getReplayFolder())) {
            return files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".mcpr")).toList();
        } catch (IOException e) {
            Exworld.LOGGER.warn("Unable to list ReForgedPlay recordings", e);
            return List.of();
        }
    }

    private static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "memory_tv"), ReForgedMemoryClient::overlay);
    }

    private static void overlay(GuiGraphics graphics, net.minecraft.client.DeltaTracker delta) {
        if (playing == null || Minecraft.getInstance().level == null) return;
        if (!OldTvFilter.loaded()) {
            int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            for (int y = 0; y < height; y += 3) graphics.fill(0, y, width, y + 1, 0x30000000);
        }
        graphics.drawString(Minecraft.getInstance().font,
                Component.translatable("memory.exworld.overlay", playing.clip.id), 8, 8, 0xFFE7D7A1, true);
    }

    private static boolean validId(String id) {
        return id != null && id.matches("[A-Za-z0-9_.+-]{1,32}");
    }

    private static int ok(String key, Object... args) {
        message(Component.translatable(key, args));
        return 1;
    }

    private static int fail(String key, Object... args) {
        message(Component.translatable(key, args));
        return 0;
    }

    private static void message(Component text) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) minecraft.player.displayClientMessage(text, false);
        else Exworld.LOGGER.info(text.getString());
    }

    private record Clip(String id, Path path, int start, int end, long modified) {}
    private record Requested(String id, boolean loop, long deadline, long notBefore) {}
    private record Playing(Clip clip, boolean loop) {}
}
