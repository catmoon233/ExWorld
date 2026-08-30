package net.exmo.exworld.client.quest;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;
import net.exmo.exworld.progress.PlayerResourceVault;
import net.exmo.exworld.progress.QuestJournalSnapshot;
import net.exmo.exworld.progress.QuestKind;
import net.exmo.exworld.progress.QuestSnapshot;
import net.exmo.exworld.progress.QuestStatus;
import net.exmo.exworld.progress.QuestTimelineEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Texture-backed, responsive double-page journal. Text is always carried by a parchment plaque. */
public final class QuestJournalScreen extends Screen {
    private static final int INK = 0xFF4B351C;
    private static final int GOLD = 0xFFF1C564;
    private static final int ALERT = 0xFF9B3527;

    private Page page = Page.QUESTS;
    private ResourceLocation selected;

    public QuestJournalScreen() { super(Component.translatable("screen.exworld.quest_journal")); }

    @Override protected void init() { rebuildWidgets(); }

    public void rebuildWidgets() {
        if (minecraft == null) return;
        clearWidgets();
        QuestJournalSnapshot data = QuestClient.snapshot();
        if (selected == null || data.quests().stream().noneMatch(quest -> quest.id().equals(selected))) {
            selected = data.quests().isEmpty() ? null : data.quests().getFirst().id();
        }
        Layout layout = layout();
        int tabGap = Math.max(2, layout.scale(3));
        int tabWidth = (layout.width() - layout.margin() * 2 - tabGap * (Page.values().length - 1)) / Page.values().length;
        int tabX = layout.x() + layout.margin();
        for (Page candidate : Page.values()) {
            Page chosen = candidate;
            JournalButton button = new JournalButton(tabX, layout.y() + layout.scale(10), tabWidth, layout.tabHeight(),
                    Component.translatable(candidate.key), ignored -> { page = chosen; rebuildWidgets(); }).selected(candidate == page);
            button.active = candidate != page;
            addRenderableWidget(button);
            tabX += tabWidth + tabGap;
        }

        if (page == Page.QUESTS) buildQuestWidgets(data, layout);
        else if (page == Page.MAIL) buildMailWidgets(data, layout);
    }

    private void buildQuestWidgets(QuestJournalSnapshot data, Layout layout) {
        int row = layout.contentY() + layout.headerHeight() + layout.scale(4);
        int bottom = layout.footerY() - layout.scale(4);
        for (QuestSnapshot quest : data.quests()) {
            if (row + layout.rowHeight() > bottom) break;
            ResourceLocation questId = quest.id();
            addRenderableWidget(new JournalButton(layout.leftX(), row, layout.pageWidth(), layout.rowHeight(),
                    Component.translatable(quest.titleKey()), ignored -> { selected = questId; rebuildWidgets(); }).selected(questId.equals(selected)));
            row += layout.rowHeight() + layout.scale(3);
        }

        QuestSnapshot quest = selected == null ? null : QuestClient.quest(selected).orElse(null);
        if (quest == null) return;
        int actionWidth = (layout.pageWidth() - layout.scale(5)) / 2;
        int actionY = layout.footerY();
        addRenderableWidget(new JournalButton(layout.rightX(), actionY, actionWidth, layout.rowHeight(),
                Component.translatable("screen.exworld.quest_pin"), ignored -> QuestClient.pin(quest.id())));
        addRenderableWidget(new JournalButton(layout.rightX() + actionWidth + layout.scale(5), actionY, actionWidth,
                layout.rowHeight(), Component.translatable("screen.exworld.quest_navigate"), ignored -> QuestClient.navigate(quest.id())));

        int branchY = actionY - layout.rowHeight() - layout.scale(4);
        for (String branch : quest.branches().stream().limit(2).toList()) {
            String choice = branch;
            addRenderableWidget(new JournalButton(layout.rightX(), branchY, layout.pageWidth(), layout.rowHeight(),
                    Component.literal("→ " + branch), ignored -> QuestClient.branch(quest.id(), choice)));
            branchY -= layout.rowHeight() + layout.scale(3);
        }
    }

    private void buildMailWidgets(QuestJournalSnapshot data, Layout layout) {
        int row = layout.contentY() + layout.headerHeight() + layout.scale(5);
        int bottom = layout.footerY() - layout.scale(4);
        for (var mail : data.mail()) {
            if (row + layout.rowHeight() > bottom) break;
            UUID id = mail.id();
            Component label = Component.literal("✉ " + mail.subject() + " ×" + mail.attachments().size());
            addRenderableWidget(new JournalButton(layout.leftX(), row, layout.pageWidth(), layout.rowHeight(), label,
                    ignored -> QuestClient.claim(id)));
            row += layout.rowHeight() + layout.scale(3);
        }
    }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        JournalGuiTextures.book(graphics, layout.x(), layout.y(), layout.width(), layout.height());
        QuestJournalSnapshot data = QuestClient.snapshot();
        if (page == Page.QUESTS) renderQuests(graphics, data, layout);
        else if (page == Page.TIMELINE) renderTimeline(graphics, data, layout);
        else if (page == Page.MAIL) renderMail(graphics, data, layout);
        else renderResources(graphics, data, layout);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderQuests(GuiGraphics graphics, QuestJournalSnapshot data, Layout layout) {
        plaqueTitle(graphics, layout.leftX(), layout.contentY(), layout.pageWidth(), Component.translatable("screen.exworld.quest_main_side"), layout);
        QuestSnapshot quest = selected == null ? null : QuestClient.quest(selected).orElse(null);
        if (quest == null) {
            plaqueTitle(graphics, layout.rightX(), layout.contentY(), layout.pageWidth(), Component.translatable("screen.exworld.quest_empty"), layout);
            return;
        }
        plaqueTitle(graphics, layout.rightX(), layout.contentY(), layout.pageWidth(), Component.translatable(quest.titleKey()), layout);
        int descriptionY = layout.contentY() + layout.headerHeight() + layout.scale(4);
        int descriptionHeight = layout.scale(46);
        JournalGuiTextures.panel(graphics, layout.rightX(), descriptionY, layout.pageWidth(), descriptionHeight);
        graphics.drawWordWrap(font, Component.translatable(quest.descriptionKey()), layout.rightX() + layout.scale(10), descriptionY + layout.scale(7),
                layout.pageWidth() - layout.scale(20), INK);

        int row = descriptionY + descriptionHeight + layout.scale(4);
        plaqueTitle(graphics, layout.rightX(), row, layout.pageWidth(), Component.translatable(quest.nodeTitleKey()), layout);
        row += layout.headerHeight() + layout.scale(3);
        for (QuestSnapshot.Objective objective : quest.objectives()) {
            if (row + layout.rowHeight() > layout.footerY() - layout.rowHeight() * 2) break;
            JournalGuiTextures.panel(graphics, layout.rightX(), row, layout.pageWidth(), layout.rowHeight());
            String value = "□ " + objective.current() + "/" + objective.required() + "  " + shortId(objective.target());
            graphics.drawString(font, ellipsize(value, layout.pageWidth() - layout.scale(18)), layout.rightX() + layout.scale(9),
                    row + Math.max(3, (layout.rowHeight() - 8) / 2), INK, false);
            row += layout.rowHeight() + layout.scale(3);
        }
        if (quest.status() == QuestStatus.CONTENT_MISSING) {
            JournalGuiTextures.panel(graphics, layout.rightX(), layout.footerY() - layout.rowHeight() - layout.scale(3), layout.pageWidth(), layout.rowHeight());
            graphics.drawCenteredString(font, Component.translatable("screen.exworld.quest_content_missing"),
                    layout.rightX() + layout.pageWidth() / 2, layout.footerY() - layout.rowHeight() + Math.max(3, (layout.rowHeight() - 8) / 2), ALERT);
        }
    }

    private void renderTimeline(GuiGraphics graphics, QuestJournalSnapshot data, Layout layout) {
        plaqueTitle(graphics, layout.leftX(), layout.contentY(), layout.pageWidth(), Component.translatable("screen.exworld.quest_timeline"), layout);
        int row = layout.contentY() + layout.headerHeight() + layout.scale(5);
        int entries = Math.max(1, (layout.footerY() - row) / (layout.rowHeight() + layout.scale(3)));
        for (QuestTimelineEntry entry : data.timeline().stream().skip(Math.max(0, data.timeline().size() - entries)).toList()) {
            JournalGuiTextures.panel(graphics, layout.leftX(), row, layout.pageWidth(), layout.rowHeight());
            String text = "• " + entry.type() + "  " + shortId(entry.questId().toString());
            graphics.drawString(font, ellipsize(text, layout.pageWidth() - layout.scale(18)), layout.leftX() + layout.scale(9),
                    row + Math.max(3, (layout.rowHeight() - 8) / 2), INK, false);
            row += layout.rowHeight() + layout.scale(3);
        }
        plaqueTitle(graphics, layout.rightX(), layout.contentY(), layout.pageWidth(), Component.translatable("screen.exworld.quest_journal"), layout);
        JournalGuiTextures.panel(graphics, layout.rightX(), layout.contentY() + layout.headerHeight() + layout.scale(5), layout.pageWidth(), layout.scale(48));
        graphics.drawWordWrap(font, Component.translatable("screen.exworld.quest_timeline"), layout.rightX() + layout.scale(10),
                layout.contentY() + layout.headerHeight() + layout.scale(14), layout.pageWidth() - layout.scale(20), INK);
    }

    private void renderMail(GuiGraphics graphics, QuestJournalSnapshot data, Layout layout) {
        plaqueTitle(graphics, layout.leftX(), layout.contentY(), layout.pageWidth(), Component.translatable("screen.exworld.quest_mail"), layout);
        plaqueTitle(graphics, layout.rightX(), layout.contentY(), layout.pageWidth(), Component.translatable("screen.exworld.quest_tab_mail"), layout);
        JournalGuiTextures.panel(graphics, layout.rightX(), layout.contentY() + layout.headerHeight() + layout.scale(5), layout.pageWidth(), layout.scale(48));
        graphics.drawWordWrap(font, Component.translatable("screen.exworld.mail_hint"), layout.rightX() + layout.scale(10),
                layout.contentY() + layout.headerHeight() + layout.scale(14), layout.pageWidth() - layout.scale(20), INK);
        if (data.mail().isEmpty()) {
            JournalGuiTextures.panel(graphics, layout.leftX(), layout.contentY() + layout.headerHeight() + layout.scale(5), layout.pageWidth(), layout.scale(34));
            graphics.drawCenteredString(font, Component.translatable("screen.exworld.quest_empty"), layout.leftX() + layout.pageWidth() / 2,
                    layout.contentY() + layout.headerHeight() + layout.scale(17), INK);
        }
    }

    private void renderResources(GuiGraphics graphics, QuestJournalSnapshot data, Layout layout) {
        plaqueTitle(graphics, layout.leftX(), layout.contentY(), layout.pageWidth(), Component.translatable("screen.exworld.quest_resources"), layout);
        plaqueTitle(graphics, layout.rightX(), layout.contentY(), layout.pageWidth(), Component.translatable("screen.exworld.quest_tab_resources"), layout);
        int row = layout.contentY() + layout.headerHeight() + layout.scale(6);
        for (var entry : data.resources().entrySet()) {
            if (row + layout.scale(30) > layout.footerY()) break;
            JournalGuiTextures.panel(graphics, layout.leftX(), row, layout.width() - layout.margin() * 2, layout.scale(30));
            Component label = resourceName(entry.getKey());
            graphics.drawString(font, label, layout.leftX() + layout.scale(13), row + layout.scale(10), INK, true);
            graphics.drawString(font, formatAmount(entry.getValue()), layout.rightX() + layout.pageWidth() - layout.scale(13) - font.width(formatAmount(entry.getValue())),
                    row + layout.scale(10), GOLD, true);
            row += layout.scale(34);
        }
        if (data.resources().isEmpty()) {
            JournalGuiTextures.panel(graphics, layout.leftX(), row, layout.width() - layout.margin() * 2, layout.scale(30));
            graphics.drawCenteredString(font, Component.translatable("screen.exworld.quest_empty"), layout.x() + layout.width() / 2, row + layout.scale(10), INK);
        }
    }

    private void plaqueTitle(GuiGraphics graphics, int x, int y, int width, Component text, Layout layout) {
        JournalGuiTextures.panel(graphics, x, y, width, layout.headerHeight());
        graphics.drawCenteredString(font, text, x + width / 2, y + Math.max(3, (layout.headerHeight() - 8) / 2), INK);
    }

    private Component resourceName(ResourceLocation id) {
        return id.equals(PlayerResourceVault.GOLD) ? Component.translatable("resource.exworld.gold") : Component.literal(shortId(id.toString()));
    }

    private String ellipsize(String value, int width) { return font.plainSubstrByWidth(value, Math.max(12, width), true); }
    private static String formatAmount(long amount) { return String.format(Locale.ROOT, "%,d", amount); }
    private static String shortId(String id) { int index = id.indexOf(':'); return index >= 0 ? id.substring(index + 1) : id; }

    private Layout layout() {
        float scale = Math.min(1.0F, Math.min((width - 16) / (float) JournalGuiTextures.BOOK_WIDTH,
                (height - 16) / (float) JournalGuiTextures.BOOK_HEIGHT));
        scale = Math.max(0.50F, scale);
        int bookWidth = Math.round(JournalGuiTextures.BOOK_WIDTH * scale);
        int bookHeight = Math.round(JournalGuiTextures.BOOK_HEIGHT * scale);
        int x = (width - bookWidth) / 2;
        int y = (height - bookHeight) / 2;
        int margin = Math.max(9, Math.round(22 * scale));
        int gutter = Math.max(6, Math.round(16 * scale));
        int pageWidth = bookWidth / 2 - margin - gutter / 2;
        int leftX = x + margin;
        int rightX = x + bookWidth / 2 + gutter / 2;
        int tabHeight = Math.max(18, Math.round(22 * scale));
        int contentY = y + Math.max(tabHeight + 10, Math.round(42 * scale));
        int rowHeight = Math.max(18, Math.round(24 * scale));
        int footerY = y + bookHeight - margin - rowHeight;
        return new Layout(x, y, bookWidth, bookHeight, leftX, rightX, pageWidth, margin, contentY, footerY, tabHeight, rowHeight, scale);
    }

    private enum Page {
        QUESTS("screen.exworld.quest_tab_quests"), TIMELINE("screen.exworld.quest_tab_timeline"),
        MAIL("screen.exworld.quest_tab_mail"), RESOURCES("screen.exworld.quest_tab_resources");
        private final String key;
        Page(String key) { this.key = key; }
    }

    private record Layout(int x, int y, int width, int height, int leftX, int rightX, int pageWidth, int margin,
                          int contentY, int footerY, int tabHeight, int rowHeight, float scaleFactor) {
        int scale(int value) { return Math.max(1, Math.round(value * scaleFactor)); }
        int headerHeight() { return Math.max(18, scale(24)); }
    }

    @Override public boolean isPauseScreen() { return false; }
}
