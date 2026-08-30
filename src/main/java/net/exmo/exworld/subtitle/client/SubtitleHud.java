package net.exmo.exworld.subtitle.client;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.subtitle.SubtitlePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.util.ArrayDeque;
import java.util.Deque;

/** Queueing region-title HUD adapted from StarRailExpress2's fade/hold/fade subtitle design. */
public final class SubtitleHud {
    private static final Deque<Entry> QUEUE = new ArrayDeque<>();
    private static Entry current;
    private static int tick;

    private SubtitleHud() {}

    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "subtitles"),
                (graphics, delta) -> render(graphics, delta.getGameTimeDeltaPartialTick(false)));
    }

    public static void enqueue(SubtitlePayload payload) {
        QUEUE.addLast(new Entry(payload.title(), payload.subtitle(), Math.max(20, payload.durationTicks()), payload.color()));
    }

    public static void tick() {
        if (current == null) advance();
        else if (++tick >= current.duration + 30) advance();
    }

    private static void advance() {
        current = QUEUE.pollFirst();
        tick = 0;
    }

    private static void render(GuiGraphics graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (current == null || minecraft.options.hideGui || minecraft.player == null) return;
        float age = tick + Mth.clamp(partialTick, 0.0F, 1.0F);
        float intro = Mth.clamp(age / 10.0F, 0.0F, 1.0F);
        float outro = 1.0F - Mth.clamp((age - current.duration - 10.0F) / 20.0F, 0.0F, 1.0F);
        float alpha = intro * outro;
        if (alpha * 255.0F < 4.0F) return;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int y = minecraft.getWindow().getGuiScaledHeight() * 27 / 100;
        int titleWidth = minecraft.font.width(current.title) * 2;
        int subtitleWidth = minecraft.font.width(current.subtitle);
        int panelWidth = Math.max(titleWidth, subtitleWidth) + 50;
        int a = Mth.clamp((int) (alpha * 170.0F), 0, 255);
        graphics.fill(width / 2 - panelWidth / 2, y - 12, width / 2 + panelWidth / 2, y + 38, a << 24 | 0x10151A);
        graphics.fill(width / 2 - panelWidth / 2 + 12, y + 35, width / 2 + panelWidth / 2 - 12, y + 36,
                withAlpha(current.color, alpha));
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(width / 2.0F, y, 0.0F);
        pose.scale(2.0F, 2.0F, 1.0F);
        graphics.drawString(minecraft.font, current.title, -minecraft.font.width(current.title) / 2, 0,
                withAlpha(current.color, alpha), true);
        pose.popPose();
        graphics.drawCenteredString(minecraft.font, current.subtitle, width / 2, y + 23,
                withAlpha(0xFFC9D2DD, alpha * 0.88F));
    }

    private static int withAlpha(int color, float alpha) {
        return Mth.clamp((int) (alpha * 255.0F), 4, 255) << 24 | color & 0xFFFFFF;
    }

    private record Entry(Component title, Component subtitle, int duration, int color) {}
}
