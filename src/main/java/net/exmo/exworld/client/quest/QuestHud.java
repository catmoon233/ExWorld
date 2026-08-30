package net.exmo.exworld.client.quest;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.client.battle.BattleClient;
import net.exmo.exworld.client.perspective.FirstPersonToggle;
import net.exmo.exworld.progress.QuestJournalSnapshot;
import net.exmo.exworld.progress.QuestKind;
import net.exmo.exworld.progress.QuestSnapshot;
import net.exmo.exworld.progress.QuestStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Compact exploration-only objective plaque. */
public final class QuestHud {
    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "quest_hud");
    private static final int X = 12;
    private static final int Y = 12;
    private static final int WIDTH = 204;
    private static final int HEIGHT = 48;

    private QuestHud() {}

    public static void registerLayer(RegisterGuiLayersEvent event) { event.registerAboveAll(LAYER, (graphics, partial) -> render(graphics)); }

    private static void render(GuiGraphics graphics) {
        if (BattleClient.active() || Minecraft.getInstance().screen != null) return;
        QuestSnapshot value = shownQuest();
        if (value == null) return;
        var font = Minecraft.getInstance().font;
        JournalGuiTextures.panel(graphics, X, Y, WIDTH, HEIGHT);
        String title = "✒ " + net.minecraft.network.chat.Component.translatable(value.titleKey()).getString();
        graphics.drawString(font, font.plainSubstrByWidth(title, WIDTH - 18, true), X + 9, Y + 10, 0xFF4B351C, true);
        if (!value.objectives().isEmpty()) {
            var objective = value.objectives().getFirst();
            String progress = "□ " + objective.current() + "/" + objective.required() + "  " + shortId(objective.target());
            graphics.drawString(font, font.plainSubstrByWidth(progress, WIDTH - 18, true), X + 9, Y + 27, 0xFFFFE1A0, true);
        }
    }

    public static boolean click(double x, double y) {
        if (BattleClient.active() || FirstPersonToggle.firstPerson() || x < X || x > X + WIDTH || y < Y || y > Y + HEIGHT) return false;
        QuestSnapshot value = shownQuest();
        if (value == null) return false;
        QuestClient.navigate(value.id());
        return true;
    }

    private static QuestSnapshot shownQuest() {
        QuestJournalSnapshot data = QuestClient.snapshot();
        QuestSnapshot main = data.quests().stream().filter(quest -> quest.kind() == QuestKind.MAIN && quest.status() == QuestStatus.ACTIVE).findFirst().orElse(null);
        return main != null ? main : data.pinnedQuest() == null ? null : QuestClient.quest(data.pinnedQuest()).orElse(null);
    }

    private static String shortId(String value) { int index = value.indexOf(':'); return index >= 0 ? value.substring(index + 1) : value; }
}
