package net.exmo.exworld.mystery.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.exmo.exworld.Exworld;
import net.exmo.exworld.mystery.GamePhase;
import net.exmo.exworld.mystery.MysteryPayloads;
import net.exmo.exworld.mystery.MysterySounds;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

/** Client-owned interpolation only. All game progress is delivered by the server. */
public final class MysteryClient {
    private static final KeyMapping SKIP = new KeyMapping("key.exworld.mystery_skip", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K, "key.categories.exworld");
    private static final KeyMapping JOURNAL = new KeyMapping("key.exworld.mystery_journal", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_U, "key.categories.exworld");
    private static UUID runId;
    private static String phase = GamePhase.LOBBY.name();
    private static String state = "UNASSIGNED";
    private static String era = "NONE";
    private static String display = "";
    private static String knowledge = "";
    private static long serverTick;
    private static int cycle;
    private static int seals;
    private static int suspicion;
    private static int hintStage;
    private static long endingAt = -1;
    private static Cue cue;
    private static SimpleSoundInstance announcement;
    private static Screen previewReturn;
    private static int localTick;
    private static float cycleFlash;
    private static float clueFlash;
    private static float suspicionFlash;
    private static float dangerVisibility;

    private MysteryClient() {}

    public static void register(IEventBus bus) {
        bus.addListener((RegisterKeyMappingsEvent event) -> event.register(SKIP));
        bus.addListener((RegisterKeyMappingsEvent event) -> event.register(JOURNAL));
        bus.addListener(MysteryClient::layers);
        NeoForge.EVENT_BUS.addListener(MysteryClient::tick);
        InWorldMemory.register();
    }

    private static void layers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "mystery_hud"),
                (graphics, delta) -> render(graphics, delta.getGameTimeDeltaPartialTick(false)));
    }

    public static boolean inGame() { return !GamePhase.LOBBY.name().equals(phase) && !display.isBlank(); }
    public static String knowledge() { return knowledge; }
    public static String displayName() { return display; }
    public static int ordinaryTaskY() { return 52 + Math.round(31 * dangerVisibility); }

    public static void receive(MysteryPayloads.Client payload) {
        if (payload == null || payload.tag() == null) return;
        CompoundTag tag = payload.tag();
        switch (payload.kind()) {
            case "state" -> install(tag);
            case "cue" -> beginCue(tag);
            case "blueprint" -> Minecraft.getInstance().setScreen(new net.exmo.exworld.mystery.blueprint.client.BlueprintScreen(tag));
            case "blueprint_result" -> {
                if (Minecraft.getInstance().screen instanceof net.exmo.exworld.mystery.blueprint.client.BlueprintScreen screen)
                    screen.result(tag.getString("message"));
            }
            default -> {}
        }
    }

    private static void install(CompoundTag tag) {
        UUID nextRun = tag.hasUUID("run") ? tag.getUUID("run") : null;
        if (runId != null && nextRun != null && !runId.equals(nextRun)) { cue = null; stopAnnouncement(); MysteryCamera.clear(); localTick = 0; }
        runId = nextRun;
        phase = tag.getString("phase"); state = tag.getString("state"); era = tag.getString("era");
        display = tag.getString("display");
        if (tag.getInt("cycle") > cycle) cycleFlash = 1;
        cycle = tag.getInt("cycle");
        serverTick = tag.getLong("tick");
        localTick = 0;
        seals = tag.getInt("seals");
        if (suspicion != tag.getInt("suspicion")) suspicionFlash = 1;
        suspicion = tag.getInt("suspicion"); hintStage = tag.getInt("hint_stage");
        endingAt = tag.contains("ending_at") ? tag.getLong("ending_at") : -1;
        String nextKnowledge = tag.getString("knowledge");
        if (!nextKnowledge.equals(knowledge) && !nextKnowledge.isBlank()) clueFlash = 1;
        knowledge = nextKnowledge;
        InWorldMemory.sync(nextRun, phase, state, knowledge);
    }

    private static void beginCue(CompoundTag tag) {
        UUID incoming = tag.hasUUID("run") ? tag.getUUID("run") : null;
        if (runId != null && !runId.equals(incoming)) return;
        int age = (int)Math.max(0, serverTick + localTick - tag.getLong("start"));
        int duration = Math.max(1, tag.getInt("duration"));
        if (age >= duration) return;
        String id = tag.getString("id");
        stopAnnouncement();
        cue = new Cue(id, duration, age);
        if (age < 20) playAnnouncement(id);
        MysteryCamera.start(id, duration, age);
    }

    public static void previewCue(String id, int duration) { previewCue(id, duration, null); }

    public static void previewCue(String id, int duration, Screen returnTo) {
        previewReturn = returnTo;
        if (returnTo != null) Minecraft.getInstance().setScreen(null);
        cue = new Cue(id, Math.max(1, duration), 0);
        MysteryCamera.start(id, duration);
        playAnnouncement(id);
    }

    private static void playAnnouncement(String id) {
        SoundEvent sound = MysterySounds.cue(id);
        if (sound == null) return;
        ResourceLocation asset = ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "sounds/mystery/" + id + ".ogg");
        Minecraft mc = Minecraft.getInstance();
        if (mc.getResourceManager().getResource(asset).isPresent()) {
            announcement = SimpleSoundInstance.forUI(sound, 1.0F);
            mc.getSoundManager().play(announcement);
        }
    }

    private static void tick(ClientTickEvent.Post event) {
        while (SKIP.consumeClick()) { cue = null; MysteryCamera.clear(); stopAnnouncement(); restorePreview(); }
        while (JOURNAL.consumeClick()) if (inGame() && Minecraft.getInstance().screen == null)
            Minecraft.getInstance().setScreen(new MysteryJournalScreen());
        if (Minecraft.getInstance().player == null) {
            cue = null; stopAnnouncement(); previewReturn = null; runId = null; phase = GamePhase.LOBBY.name(); InWorldMemory.disconnected(); return;
        }
        localTick++;
        if (cue != null && ++cue.age >= cue.duration) { cue = null; MysteryCamera.clear(); stopAnnouncement(); restorePreview(); }
        cycleFlash = Math.max(0, cycleFlash - .05F);
        clueFlash = Math.max(0, clueFlash - .05F);
        suspicionFlash = Math.max(0, suspicionFlash - .05F);
        dangerVisibility = Mth.clamp(dangerVisibility + (danger().isBlank() ? -.1F : .1F), 0, 1);
        MysteryCamera.tick();
    }

    private static void render(GuiGraphics graphics, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || InWorldMemory.isPlaybackOpen()) return;
        int w = graphics.guiWidth(), h = graphics.guiHeight();
        if (!inGame()) {
            if (cue != null) renderCue(graphics, mc, w, h, partial);
            return;
        }
        float intro = Mth.clamp((serverTick + localTick + partial) / 10F, 0, 1);
        int panelAlpha = (int)(intro * 160) << 24;
        String objective = objective();
        String danger = danger();
        int clueY = 8 + Math.round(31 * dangerVisibility);
        if (dangerVisibility > 0.01F) {
            int pulse = (int)(35 * Math.sin((localTick + partial) * .2));
            int dangerAlpha = (int)(dangerVisibility * 160);
            graphics.fill(8, 8, Math.min(w - 8, 290), 33, (dangerAlpha << 24) | 0x003B1720);
            if (!danger.isBlank()) graphics.drawString(mc.font, fontLine(mc, danger, 265), 16, 17,
                    (Mth.clamp((int)(dangerVisibility * (215 + pulse)), 0, 255) << 24) | 0x00FFA0A0, false);
        }
        graphics.fill(8, clueY, Math.min(w - 8, 290), clueY + 34, panelAlpha | 0x10151A);
        graphics.drawString(mc.font, Component.translatable("hud.exworld.mystery.current_clue"), 16, clueY + 6, 0xFFFFE4AD, false);
        graphics.drawString(mc.font, fontLine(mc, objective, 263), 16, clueY + 19, 0xFFC9D2DD, false);
        graphics.fill(8, h - 62, 215, h - 8, panelAlpha | 0x10151A);
        graphics.drawString(mc.font, Component.literal(display), 17, h - 54, 0xFFE9DCA9, true);
        graphics.drawString(mc.font, Component.translatable("hud.exworld.mystery." + era.toLowerCase(java.util.Locale.ROOT)), 17, h - 40, 0xFFC9D2DD, false);
        if (suspicion > 0) {
            graphics.drawString(mc.font, Component.translatable("hud.exworld.mystery.suspicion", suspicion), 17, h - 24, 0xFFFFBF7A, true);
        } else {
            graphics.drawString(mc.font, Component.translatable("hud.exworld.mystery.cycle", cycle), 17, h - 24, 0xFFB9A8E0, false);
        }
        graphics.drawString(mc.font, Component.translatable("hud.exworld.mystery.seals", seals),
                Math.max(220, w - 110), h - 18, 0xFFE9DCA9, false);
        if (cycleFlash > 0) graphics.drawCenteredString(mc.font,
                Component.translatable("hud.exworld.mystery.cycle", cycle), w / 2, h / 3,
                ((int)(cycleFlash * 255) << 24) | 0x00E9DCA9);
        if (clueFlash > 0) graphics.drawCenteredString(mc.font,
                Component.translatable("hud.exworld.mystery.clue_added"), w / 2, h / 3 + 16,
                ((int)(clueFlash * 255) << 24) | 0x00E9DCA9);
        if (suspicionFlash > 0 && suspicion > 0) graphics.drawCenteredString(mc.font,
                Component.translatable("hud.exworld.mystery.suspicion", suspicion), w / 2, h / 3 + 32,
                ((int)(suspicionFlash * 255) << 24) | 0x00FFBF7A);
        if (cue != null) renderCue(graphics, mc, w, h, partial);
    }

    private static String fontLine(Minecraft mc, String value, int width) {
        return mc.font.plainSubstrByWidth(value, width);
    }

    private static String objective() {
        if (phase.equals("ENDING")) return Component.translatable("hud.exworld.mystery.objective.chase").getString();
        if (phase.equals("FINISHED")) return Component.translatable("hud.exworld.mystery.objective.finished").getString();
        if (hintStage > 0) return Component.translatable("hud.exworld.mystery.hint."
                + era.toLowerCase(java.util.Locale.ROOT) + "." + hintStage).getString();
        if (era.equals("PAST")) {
            if (!knowledge.contains("past:ritual_seen")) return Component.translatable("hud.exworld.mystery.objective.ritual").getString();
            if (!knowledge.contains("priest:reported")) return Component.translatable("hud.exworld.mystery.objective.priest").getString();
            if (cycle == 0) return Component.translatable("hud.exworld.mystery.objective.wait_cycle").getString();
            if (!knowledge.contains("priest:seal_crafted")) return Component.translatable("hud.exworld.mystery.objective.death_place").getString();
            return Component.translatable("hud.exworld.mystery.objective.past_finish").getString();
        }
        if (!knowledge.contains("future:recipe_known")) return Component.translatable("hud.exworld.mystery.objective.recipe").getString();
        if (!knowledge.contains("future:seal_route")) return Component.translatable("hud.exworld.mystery.objective.church").getString();
        return Component.translatable("hud.exworld.mystery.objective.train").getString();
    }

    private static String danger() {
        if (state.equals("FUTURE_DOOMED")) return Component.translatable("hud.exworld.mystery.doomed").getString();
        if (phase.equals("INTRO")) return Component.translatable("hud.exworld.mystery.hunt_countdown",
                Math.max(0, (int)((3600 - serverTick - localTick) / 20))).getString();
        if (phase.equals("ENDING") && endingAt > 0) return Component.translatable("hud.exworld.mystery.chase_countdown",
                Math.max(0, (int)((endingAt - serverTick - localTick) / 20))).getString();
        return "";
    }

    private static void stopAnnouncement() {
        if (announcement != null) Minecraft.getInstance().getSoundManager().stop(announcement);
        announcement = null;
    }

    private static void restorePreview() {
        Screen target = previewReturn;
        previewReturn = null;
        if (target != null && Minecraft.getInstance().screen == null) Minecraft.getInstance().setScreen(target);
    }

    private static void renderCue(GuiGraphics graphics, Minecraft mc, int w, int h, float partial) {
        float age = cue.age + partial;
        float alpha = Math.min(1F, age / 10F) * Math.min(1F, (cue.duration - age) / 12F);
        if (alpha <= 0) return;
        int a = Mth.clamp((int)(alpha * 255), 0, 255);
        if (cue.id.equals("identity_reveal") || cue.id.equals("rewind") || cue.id.startsWith("ending_")) {
            float opening = Mth.clamp((8 - age) / 8F, 0, 1);
            float closing = Mth.clamp((age - cue.duration + 8) / 8F, 0, 1);
            int veil = Mth.clamp((int)(Math.max(opening, closing) * 180), 0, 180);
            if (veil > 0) graphics.fill(0, 0, w, h, veil << 24);
        }
        int bar = Math.max(10, h / 16);
        graphics.fill(0, 0, w, bar, a << 24);
        graphics.fill(0, h - bar, w, h, a << 24);
        Component title = Component.translatable("cue.exworld.mystery." + cue.id + ".title");
        Component subtitle = Component.translatable("cue.exworld.mystery." + cue.id + ".subtitle");
        graphics.drawCenteredString(mc.font, title, w / 2, h / 3, (a << 24) | 0x00FFE4AD);
        graphics.drawCenteredString(mc.font, subtitle, w / 2, h / 3 + 18, (a << 24) | 0x00D7DFE8);
        if (cue.id.equals("cycle_recap") && !knowledge.isBlank()) {
            String[] facts = knowledge.split("\n");
            int shown = Math.min(2, facts.length);
            for (int i = 0; i < shown; i++) {
                String fact = facts[facts.length - shown + i];
                String translated = Component.translatable("clue.exworld." + fact.replace(':', '.')).getString();
                graphics.drawCenteredString(mc.font, fontLine(mc, translated, w - 32), w / 2,
                        h / 3 + 38 + i * 15, (a << 24) | 0x00B9C8D5);
            }
        }
        graphics.drawCenteredString(mc.font, Component.translatable("hud.exworld.mystery.skip"), w / 2, h - bar - 16,
                ((a * 2 / 3) << 24) | 0x00C9D2DD);
    }

    private static final class Cue {
        final String id;
        final int duration;
        int age;
        Cue(String id, int duration, int age) { this.id = id; this.duration = duration; this.age = age; }
    }
}
