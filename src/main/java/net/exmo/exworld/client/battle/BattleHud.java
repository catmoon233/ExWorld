package net.exmo.exworld.client.battle;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.battle.BattleSession;
import net.exmo.exworld.battle.api.BattleEvent;
import net.exmo.exworld.battle.api.BattleSnapshot;
import net.exmo.exworld.battle.model.BattleState;
import net.exmo.exworld.battle.skill.SkillDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.util.*;

/** Minimal tactical HUD with physical-card hand presentation and one shared drawing/hit-test layout. */
public final class BattleHud {
    private static final int VOID = 0xE80A0E14;
    private static final int GLASS = 0xD9161D27;
    private static final int GLASS_SOFT = 0xB9121821;
    private static final int EDGE = 0x996E7F90;
    private static final int GOLD = 0xFFF2CB72;
    private static final int TEXT = 0xFFF4F6F8;
    private static final int MUTED = 0xFF9AA7B5;
    private static final int HP = 0xFFF06068;
    private static final int MANA = 0xFF62A1FF;
    private static final int MOVE = 0xFF6EDAA2;
    private static final int ITEM_COLUMNS = 2;
    private static final int ITEM_VISIBLE_SLOTS = 6;
    private static final Map<String, Optional<ResourceLocation>> ICON_CACHE = new HashMap<>();
    private static volatile Layout lastLayout = Layout.EMPTY;
    private static LayoutKey lastLayoutKey;
    private static boolean logExpanded;
    private static int logScroll;
    private static int itemScroll;
    private static int itemPanelX;
    private static int itemPanelY;
    private static boolean draggingItemPanel;
    private static int itemPanelGrabX;
    private static int itemPanelGrabY;

    private BattleHud() {}

    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "battle_hud"),
                (graphics, delta) -> render(graphics));
    }

    private static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        BattleSnapshot snapshot = BattleClient.snapshot();
        BattleSnapshot.CombatantView player = BattleClient.localCombatant();
        if (snapshot == null || player == null || minecraft.options.hideGui) {
            lastLayout = Layout.EMPTY;
            lastLayoutKey = null;
            draggingItemPanel = false;
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth(), height = minecraft.getWindow().getGuiScaledHeight();
        double guiScale = minecraft.getWindow().getGuiScale();
        int mouseX = (int) (minecraft.mouseHandler.xpos() / guiScale), mouseY = (int) (minecraft.mouseHandler.ypos() / guiScale);
        List<BattleSnapshot.ItemView> items = usableItems(player.items());
        itemScroll = Mth.clamp(itemScroll, 0, maxItemScroll(items.size(), ITEM_VISIBLE_SLOTS, ITEM_COLUMNS));
        updateItemPanelDrag(mouseX, mouseY, width, height);
        LayoutKey layoutKey = new LayoutKey(width, height, player.hand().size(), items.size(), logExpanded,
                BattleClient.itemsOpen(), Screen.hasShiftDown(), itemPanelX, itemPanelY);
        Layout layout = layoutKey.equals(lastLayoutKey) ? lastLayout
                : Layout.create(width, height, player.hand().size(), items.size(), logExpanded, BattleClient.itemsOpen(),
                Screen.hasShiftDown(), itemPanelX, itemPanelY);
        lastLayoutKey = layoutKey;
        lastLayout = layout;

        renderTopBar(graphics, minecraft.font, snapshot, player, layout);
        renderPlayerPlate(graphics, minecraft, snapshot, player, layout);
        renderTargetPlate(graphics, minecraft.font, BattleClient.hoveredCombatant(), snapshot, layout);
        renderLog(graphics, minecraft.font, snapshot, layout);
        renderActions(graphics, minecraft.font, snapshot, player, layout, mouseX, mouseY);
        renderItems(graphics, minecraft, items, player, layout, mouseX, mouseY);
        renderHand(graphics, minecraft, snapshot, player, layout, mouseX, mouseY);
        renderPileOverlay(graphics,minecraft,player,layout,mouseX,mouseY);
        renderCardCastAnimations(graphics, minecraft, player, width, height);
        renderCursorCost(graphics, minecraft.font, player, width, height, mouseX, mouseY);
    }

    private static void renderCursorCost(GuiGraphics graphics, Font font, BattleSnapshot.CombatantView player,
                                         int width, int height, int mouseX, int mouseY) {
        Component label = null;
        int color = TEXT;
        BattleSnapshot.CardView selected = BattleClient.selectedCardView();
        if (selected != null && BattleClient.selectedCardCanTarget(BattleClient.hoveredCell())) {
            label = Component.translatable("hud.exworld.cursor_mana", selected.manaCost());
            color = MANA;
        } else if (!BattleClient.itemTargeting() && BattleClient.hoveredCell() != null) {
            Optional<List<net.exmo.exworld.battle.model.BattleCell>> path = BattleClient.movementPreviewPath();
            if (path.isPresent()) {
                label = Component.translatable("hud.exworld.path_cost", path.get().size(), player.movementRemaining());
                color = path.get().size() <= player.movementRemaining() ? MOVE : HP;
            }
        }
        if (label == null) return;
        int labelWidth = font.width(label), x = Math.min(width - labelWidth - 10, mouseX + 14), y = Math.min(height - 18, mouseY + 12);
        graphics.fill(x - 4, y - 3, x + labelWidth + 4, y + 11, 0xE8111822);
        graphics.fill(x - 4, y - 3, x + labelWidth + 4, y - 2, color);
        graphics.drawString(font, label, x, y, color, true);
    }

    private static void renderTopBar(GuiGraphics graphics, Font font, BattleSnapshot snapshot,
                                     BattleSnapshot.CombatantView player, Layout layout) {
        Rect bar = layout.header();
        shadow(graphics, bar, 2); cutPanel(graphics, bar, VOID, EDGE, 4);
        boolean localTurn = isLocalTurn(snapshot, player);
        graphics.fill(bar.x() + 5, bar.bottom() - 3, bar.right() - 5, bar.bottom() - 2, localTurn ? MOVE : targetColorForFaction(snapshot));

        Component round = Component.translatable("hud.exworld.battle_round", snapshot.round());
        graphics.drawString(font, round, bar.x() + 12, bar.y() + 10, MUTED, false);
        Component phase = Component.translatable(localTurn ? "hud.exworld.your_turn" : "hud.exworld.faction_turn", snapshot.activeFaction());
        graphics.drawCenteredString(font, phase, bar.centerX(), bar.y() + 7, localTurn ? 0xFF9AF2BA : TEXT);

        int seconds = Math.max(0, snapshot.phaseTicksRemaining() / 20);
        Component time = Component.literal(seconds + "s");
        graphics.drawString(font, time, bar.right() - font.width(time) - 12, bar.y() + 10, seconds <= 10 ? HP : TEXT, true);
        float progress = phaseProgress(snapshot);
        drawBar(graphics, bar.centerX() - 58, bar.y() + 20, 116, 3, progress, seconds <= 10 ? HP : GOLD);

        if (bar.w() >= 330) {
            int chipX = bar.x() + 12;
            for (String faction : snapshot.factionOrder()) {
                boolean active = faction.equals(snapshot.activeFaction());
                int color = active ? GOLD : 0xFF4C5866;
                graphics.fill(chipX, bar.bottom() - 8, chipX + (active ? 12 : 6), bar.bottom() - 6, color);
                chipX += active ? 16 : 10;
            }
        }
    }

    private static void renderPlayerPlate(GuiGraphics graphics, Minecraft minecraft, BattleSnapshot snapshot,
                                          BattleSnapshot.CombatantView player, Layout layout) {
        Font font = minecraft.font;
        Rect plate = layout.status(); shadow(graphics, plate, 2); cutPanel(graphics, plate, GLASS, EDGE, 4);
        graphics.fill(plate.x() + 5, plate.y() + 5, plate.x() + 8, plate.bottom() - 5, MOVE);
        graphics.drawString(font, trim(font, player.name(), plate.w() - 82), plate.x() + 14, plate.y() + 8, TEXT, true);
        Component state = Component.translatable(player.autoBattle() ? "hud.exworld.auto_active" :
                isLocalTurn(snapshot, player) ? "hud.exworld.awaiting_orders" : "hud.exworld.waiting_turn");
        graphics.drawString(font, trim(font, state.getString(), plate.w() - 82), plate.x() + 14, plate.y() + 20,
                player.autoBattle() ? GOLD : MUTED, false);

        int barX = plate.x() + 14, barW = plate.w() - 72;
        float vanillaAbsorption = minecraft.player == null ? 0.0F : minecraft.player.getAbsorptionAmount();
        drawCompactResource(graphics, font, barX, plate.y() + 34, barW, HP, player.health(), player.maxHealth(),
                hudAbsorption(player.block(), vanillaAbsorption), "HP");
        drawCompactResource(graphics, font, barX, plate.y() + 48, barW, MANA, player.mana(), player.maxMana(), 0.0F, "MP");

        int statX = plate.right() - 50;
        graphics.drawCenteredString(font, Integer.toString(player.movementRemaining()), statX + 10, plate.y() + 14, MOVE);
        graphics.drawCenteredString(font, Component.translatable("hud.exworld.move_short"), statX + 10, plate.y() + 25, MUTED);
        graphics.drawCenteredString(font, player.drawCount() + "/" + player.discardCount(), statX + 10, plate.y() + 41, TEXT);
        graphics.drawCenteredString(font, Component.translatable("hud.exworld.deck_short"), statX + 10, plate.y() + 52, MUTED);
    }

    private static void renderPileOverlay(GuiGraphics graphics,Minecraft minecraft,BattleSnapshot.CombatantView player,Layout layout,int mouseX,int mouseY){
        Rect draw=layout.drawPile(),discard=layout.discardPile();
        drawPileButton(graphics,minecraft.font,draw,Component.translatable("hud.exworld.draw_pile"),player.drawCount(),mouseX,mouseY,0xFF5685C5);
        drawPileButton(graphics,minecraft.font,discard,Component.translatable("hud.exworld.discard_pile"),player.discardCount(),mouseX,mouseY,0xFF8C6875);
        BattleClient.PileOverlay open=BattleClient.pileOverlay();if(open==BattleClient.PileOverlay.NONE)return;
        List<BattleSnapshot.CardView> cards=open==BattleClient.PileOverlay.DRAW?player.drawPile():player.discardPile();
        Rect panel=layout.pilePanel();cutPanel(graphics,panel,0xF20D131C,GOLD,5);
        graphics.drawString(minecraft.font,Component.translatable(open==BattleClient.PileOverlay.DRAW?"hud.exworld.draw_pile_order":"hud.exworld.discard_pile_contents"),panel.x()+12,panel.y()+10,GOLD,true);
        graphics.drawString(minecraft.font,Component.translatable("hud.exworld.close_pile"),panel.right()-82,panel.y()+10,MUTED,false);
        int hovered = -1;
        int cols=Math.max(1,(panel.w()-20)/96);for(int i=0;i<cards.size();i++){var card=cards.get(i);int x=panel.x()+10+(i%cols)*96,y=panel.y()+28+(i/cols)*22;if(y>panel.bottom()-18)break;
            Rect row = new Rect(x, y - 2, 92, 18); if (row.contains(mouseX, mouseY)) hovered = i;
            String prefix=open==BattleClient.PileOverlay.DRAW?(i+1)+". ":"";graphics.drawString(minecraft.font,trim(minecraft.font,prefix+Component.translatable(card.nameKey()).getString(),88),x,y,card.retained()?MOVE:TEXT,false);}
        if (hovered >= 0) graphics.renderTooltip(minecraft.font, cardTooltip(cards.get(hovered), player), Optional.empty(), mouseX, mouseY);
    }
    private static void drawPileButton(GuiGraphics graphics,Font font,Rect rect,Component title,int count,int mouseX,int mouseY,int accent){cutPanel(graphics,rect,rect.contains(mouseX,mouseY)?0xEE263441:0xDC171F29,accent,3);graphics.drawString(font,title,rect.x()+7,rect.y()+6,MUTED,false);graphics.drawString(font,Integer.toString(count),rect.right()-font.width(Integer.toString(count))-7,rect.y()+6,TEXT,true);}

    private static void renderTargetPlate(GuiGraphics graphics, Font font, BattleSnapshot.CombatantView target,
                                          BattleSnapshot snapshot, Layout layout) {
        if (target == null || !layout.target().visible()) return;
        Rect plate = layout.target(); shadow(graphics, plate, 2); cutPanel(graphics, plate, GLASS, EDGE, 4);
        int accent = target.factionId().equals(BattleClient.localCombatant().factionId()) ? MOVE : HP;
        graphics.fill(plate.right() - 4, plate.y() + 5, plate.right() - 2, plate.bottom() - 5, accent);
        graphics.drawString(font, trim(font, target.name(), plate.w() - 66), plate.x() + 10, plate.y() + 8, TEXT, true);
        graphics.drawString(font, trim(font, target.factionId(), 58), plate.right() - font.width(trim(font, target.factionId(), 58)) - 10,
                plate.y() + 8, accent, false);
        drawCompactResource(graphics, font, plate.x() + 10, plate.y() + 25, plate.w() - 20, HP, target.health(), target.maxHealth(), target.block(), "HP");
        drawCompactResource(graphics, font, plate.x() + 10, plate.y() + 39, plate.w() - 20, MANA, target.mana(), target.maxMana(), 0.0F, "MP");

        if (Screen.hasShiftDown()) renderExpandedTargetDetails(graphics, font, target, plate);
        else renderTargetStatusBadges(graphics, font, target, plate);
        if (snapshot.phaseDamage() > 0) {
            Component damage = Component.literal("Σ " + formatAmount(snapshot.phaseDamage()));
            graphics.drawString(font, damage, plate.right() - font.width(damage) - 10, plate.bottom() - 13, HP, true);
        }
    }

    private static void renderTargetStatusBadges(GuiGraphics graphics, Font font, BattleSnapshot.CombatantView target, Rect plate) {
        int x = plate.x() + 10;
        if (target.statuses().isEmpty()) graphics.drawString(font, Component.translatable("hud.exworld.no_status"), x, plate.y() + 55, MUTED, false);
        else for (BattleSnapshot.StatusView status : target.statuses().stream().limit(3).toList()) {
            Component badge = statusBadge(status, 8);
            int badgeW = font.width(badge) + 9;
            graphics.fill(x, plate.y() + 53, x + badgeW, plate.y() + 66, status.beneficial() ? 0x88347B58 : 0x887B3940);
            graphics.drawString(font, badge, x + 4, plate.y() + 56, status.beneficial() ? MOVE : 0xFFFF949A, false); x += badgeW + 3;
        }
    }

    private static void renderExpandedTargetDetails(GuiGraphics graphics, Font font, BattleSnapshot.CombatantView target, Rect plate) {
        int x = plate.x() + 10, y = plate.y() + 55;
        graphics.drawString(font, Component.translatable("hud.exworld.target_details"), x, y, GOLD, true); y += 12;
        graphics.drawString(font, Component.translatable("hud.exworld.target_health", formatAmount(target.health()), formatAmount(target.maxHealth())), x, y, MUTED, false);
        graphics.drawString(font, Component.translatable("hud.exworld.target_mana", formatAmount(target.mana()), formatAmount(target.maxMana())), plate.x() + plate.w() / 2, y, MUTED, false); y += 12;
        graphics.drawString(font, Component.translatable("hud.exworld.target_block", formatAmount(target.block())), x, y, MUTED, false);
        graphics.drawString(font, Component.translatable("hud.exworld.target_movement", target.movementRemaining()), plate.x() + plate.w() / 2, y, MUTED, false); y += 12;
        graphics.drawString(font, Component.translatable("hud.exworld.target_action_points", target.actionPointsRemaining(), target.actionPoints()), x, y, MUTED, false); y += 14;
        if (target.statuses().isEmpty()) { graphics.drawString(font, Component.translatable("hud.exworld.no_status"), x, y, MUTED, false); return; }
        graphics.drawString(font, Component.translatable("hud.exworld.target_effects"), x, y, GOLD, false); y += 12;
        for (BattleSnapshot.StatusView status : target.statuses().stream().limit(5).toList()) {
            String effect = statusBadge(status, 12).getString() + " · " + potionEffectDescription(status).getString();
            graphics.drawString(font, trim(font, effect, plate.w() - 20), x, y, status.beneficial() ? MOVE : 0xFFFF949A, false); y += 12;
        }
    }

    private static void renderLog(GuiGraphics graphics, Font font, BattleSnapshot snapshot, Layout layout) {
        if (!layout.log().visible()) return;
        List<BattleEvent> events = snapshot.events().stream().filter(event -> event.type() == BattleEvent.Type.SKILL
                || event.type() == BattleEvent.Type.DAMAGE || event.type() == BattleEvent.Type.HEAL
                || event.type() == BattleEvent.Type.DOWNED).toList();
        List<LogLine> lines = mergeLogEvents(events);
        if (lines.isEmpty()) return;
        Rect log = layout.log(); cutPanel(graphics, log, GLASS_SOFT, 0x554E5E6D, 3);
        Component title = Component.translatable("hud.exworld.combat_log");
        graphics.drawString(font, title, log.x() + 9, log.y() + 7, GOLD, true);
        Component toggle = Component.translatable(logExpanded ? "hud.exworld.log_collapse" : "hud.exworld.log_expand");
        graphics.drawString(font, toggle, log.right() - font.width(toggle) - 9, log.y() + 7, MUTED, false);
        int rows = Math.max(1, (log.h() - 24) / 12);
        int maxScroll = Math.max(0, lines.size() - rows);
        logScroll = Mth.clamp(logScroll, 0, maxScroll);
        LogWindow window = visibleLogWindow(lines.size(), rows, logScroll);
        for (int i = 0; i < window.count(); i++) {
            LogLine logLine = lines.get(window.start() + i); BattleEvent event = logLine.event();
            Component line = compactLog(event, logLine.count());
            int color = event.type() == BattleEvent.Type.DAMAGE ? 0xFFFFA0A5
                    : event.type() == BattleEvent.Type.HEAL ? 0xFF8DE9AE
                    : event.type() == BattleEvent.Type.DOWNED ? 0xFFFFD078 : i == window.count() - 1 && logScroll == 0 ? TEXT : MUTED;
            graphics.drawString(font, trim(font, line.getString(), log.w() - 25), log.x() + 9,
                    log.y() + 22 + i * 12, color, i == window.count() - 1 && logScroll == 0);
        }
        if (maxScroll > 0) {
            int trackY = log.y() + 22, trackH = log.h() - 29;
            graphics.fill(log.right() - 6, trackY, log.right() - 4, trackY + trackH, 0x663E4B58);
            int thumbH = Math.max(8, Math.round(trackH * rows / (float) lines.size()));
            int thumbY = trackY + Math.round((trackH - thumbH) * (maxScroll - logScroll) / (float) maxScroll);
            graphics.fill(log.right() - 7, thumbY, log.right() - 3, thumbY + thumbH, GOLD);
        }
    }

    static List<LogLine> mergeLogEvents(List<BattleEvent> events) {
        List<LogLine> lines = new ArrayList<>();
        for (BattleEvent event : events) {
            String text = compactLog(event).getString();
            if (!lines.isEmpty() && compactLog(lines.getLast().event()).getString().equals(text)) {
                LogLine previous = lines.removeLast(); lines.add(new LogLine(previous.event(), previous.count() + 1));
            } else lines.add(new LogLine(event, 1));
        }
        return List.copyOf(lines);
    }

    static List<Integer> visibleLogIndexes(int lineCount, int rows, int scroll) {
        LogWindow window = visibleLogWindow(lineCount, rows, scroll);
        List<Integer> indexes = new ArrayList<>(window.count());
        for (int index = window.start(); index < window.start() + window.count(); index++) indexes.add(index);
        return List.copyOf(indexes);
    }

    private static LogWindow visibleLogWindow(int lineCount, int rows, int scroll) {
        int visibleRows = Math.max(1, rows);
        int maxScroll = Math.max(0, lineCount - visibleRows);
        int end = Math.max(0, lineCount - Mth.clamp(scroll, 0, maxScroll));
        int start = Math.max(0, end - visibleRows);
        return new LogWindow(start, end - start);
    }

    static Component compactLog(BattleEvent event) { return compactLog(event, 1); }
    static Component compactLog(BattleEvent event, int count) {
        String target = event.targetName().isBlank() ? "·" : event.targetName();
        String skill = Component.translatable(event.skillNameKey()).getString();
        Component line = switch (event.type()) {
            case DAMAGE -> Component.literal(event.actorName() + " → " + target + "  -" + formatAmount(event.amount()));
            case HEAL -> Component.literal(event.actorName() + " → " + target + "  +" + formatAmount(event.amount()));
            case DOWNED -> Component.translatable("hud.exworld.unit_downed", target);
            default -> Component.literal(event.actorName() + " · " + skill);
        };
        return count > 1 ? Component.literal(line.getString() + " x" + count) : line;
    }

    static String formatAmount(double amount) { return String.format(Locale.ROOT, "%.1f", amount); }
    record LogLine(BattleEvent event, int count) { }
    private record LogWindow(int start, int count) { }
    static List<BattleSnapshot.ItemView> usableItems(List<BattleSnapshot.ItemView> items) {
        return items.stream().filter(BattleSnapshot.ItemView::usable).toList();
    }
    static int maxItemScroll(int itemCount, int visibleSlots, int columns) {
        int totalRows = Math.ceilDiv(Math.max(0, itemCount), Math.max(1, columns));
        int visibleRows = Math.max(1, Math.floorDiv(Math.max(1, visibleSlots), Math.max(1, columns)));
        return Math.max(0, totalRows - visibleRows);
    }
    static int scrollItemRows(int current, int wheelDelta, int itemCount, int visibleSlots, int columns) {
        return Mth.clamp(current - Integer.signum(wheelDelta), 0, maxItemScroll(itemCount, visibleSlots, columns));
    }
    static int powerCardFrameDrawCalls(int width, int height) { return Math.max(0, Math.min(width, height) + 5); }

    private static void renderActions(GuiGraphics graphics, Font font, BattleSnapshot snapshot,
                                      BattleSnapshot.CombatantView player, Layout layout, int mouseX, int mouseY) {
        Rect panel = layout.actions(); shadow(graphics, panel, 2); cutPanel(graphics, panel, GLASS, EDGE, 4);
        boolean localTurn = isLocalTurn(snapshot, player);
        boolean ready = player.playerId() != null && snapshot.readyPlayers().contains(player.playerId());
        boolean intro = snapshot.state() == BattleState.INTRO && snapshot.intro() != null;
        boolean skipped = intro && player.playerId() != null && snapshot.intro().skippedPlayers().contains(player.playerId());
        Component primary = intro ? Component.translatable("hud.exworld.skip_intro", snapshot.intro().skippedPlayers().size(), snapshot.intro().requiredPlayers())
                : Component.translatable(ready ? "hud.exworld.resume_turn" : "hud.exworld.end_turn");
        drawAction(graphics, font, layout.ready(), primary, "R", intro ? !skipped : localTurn, intro ? skipped : ready, mouseX, mouseY);
        if (!intro) renderReadyPlayers(graphics, snapshot, layout.ready());
        drawAction(graphics, font, layout.auto(), Component.translatable("hud.exworld.auto_short"), "B", true, player.autoBattle(), mouseX, mouseY);
        drawAction(graphics, font, layout.escape(), Component.translatable("hud.exworld.escape_short"), "X", true, false, mouseX, mouseY);
        drawAction(graphics, font, layout.backpack(), Component.translatable("hud.exworld.backpack"), "I", true, BattleClient.itemsOpen(), mouseX, mouseY);
        drawAction(graphics, font, layout.weapon1(), Component.translatable("hud.exworld.weapon_1"), "1", true, player.activeWeaponSlot() == 1, mouseX, mouseY);
        drawAction(graphics, font, layout.weapon2(), Component.translatable("hud.exworld.weapon_2"), "2", true, player.activeWeaponSlot() == 2, mouseX, mouseY);
    }

    private static void renderItems(GuiGraphics graphics, Minecraft minecraft, List<BattleSnapshot.ItemView> items,
                                    BattleSnapshot.CombatantView player, Layout layout, int mouseX, int mouseY) {
        if (!layout.itemPanel().visible()) return;
        Font font = minecraft.font;
        Rect panel = layout.itemPanel(); cutPanel(graphics, panel, 0xF20D131C, GOLD, 5);
        graphics.drawString(font, Component.translatable("hud.exworld.backpack"), panel.x() + 10, panel.y() + 8, GOLD, true);
        graphics.drawString(font, Component.translatable("hud.exworld.item_uses", player.itemUsesRemaining(), player.itemUseLimit()), panel.right() - 78, panel.y() + 8, MUTED, false);
        int first = itemScroll * ITEM_COLUMNS;
        BattleSnapshot.ItemView hovered = null;
        for (int i = 0; i < layout.itemRows().size() && first + i < items.size(); i++) {
            BattleSnapshot.ItemView item = items.get(first + i); Rect row = layout.itemRows().get(i);
            boolean hover = row.contains(mouseX, mouseY);
            if (hover) hovered = item;
            cutPanel(graphics, row, hover ? 0xEE314252 : 0xCC202833, 0x886E9AA9, 3);
            graphics.drawString(font, trim(font, item.displayName(), row.w() - 34), row.x() + 6, row.y() + 5, TEXT, false);
            graphics.drawString(font, "×" + item.count(), row.right() - 27, row.y() + 5, GOLD, false);
        }
        renderItemScrollbar(graphics, panel, items.size());
        if (hovered != null) graphics.renderTooltip(font, itemTooltip(minecraft, hovered), Optional.empty(), mouseX, mouseY);
    }

    private static void renderItemScrollbar(GuiGraphics graphics, Rect panel, int itemCount) {
        int maxScroll = maxItemScroll(itemCount, ITEM_VISIBLE_SLOTS, ITEM_COLUMNS);
        if (maxScroll == 0) return;
        int trackX = panel.right() - 5, trackY = panel.y() + 28, trackH = panel.h() - 34;
        graphics.fill(trackX, trackY, trackX + 2, trackY + trackH, 0x663E4B58);
        int visibleRows = ITEM_VISIBLE_SLOTS / ITEM_COLUMNS;
        int rows = Math.ceilDiv(itemCount, ITEM_COLUMNS);
        int thumbH = Math.max(8, Math.round(trackH * visibleRows / (float) rows));
        int thumbY = trackY + Math.round((trackH - thumbH) * itemScroll / (float) maxScroll);
        graphics.fill(trackX - 1, thumbY, trackX + 3, thumbY + thumbH, GOLD);
    }

    private static List<Component> itemTooltip(Minecraft minecraft, BattleSnapshot.ItemView item) {
        if (minecraft.player == null || item.inventorySlot() < 0 || item.inventorySlot() >= minecraft.player.getInventory().getContainerSize())
            return List.of(Component.literal(item.displayName()));
        ItemStack stack = minecraft.player.getInventory().getItem(item.inventorySlot());
        if (stack.isEmpty()) return List.of(Component.literal(item.displayName()));
        return stack.getTooltipLines(Item.TooltipContext.EMPTY, minecraft.player, TooltipFlag.Default.NORMAL);
    }

    private static void renderReadyPlayers(GuiGraphics graphics, BattleSnapshot snapshot, Rect action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null || snapshot.readyPlayers().isEmpty()) return;
        List<UUID> players = snapshot.combatants().values().stream()
                .filter(view -> view.playerId() != null && snapshot.readyPlayers().contains(view.playerId()))
                .map(BattleSnapshot.CombatantView::playerId).distinct().toList();
        int size = 16, gap = 3, total = players.size() * size + Math.max(0, players.size() - 1) * gap;
        int x = action.centerX() - total / 2, y = action.y() - size - 4;
        for (UUID playerId : players) {
            var info = minecraft.getConnection().getPlayerInfo(playerId);
            if (info != null) {
                graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xCC0A0E14);
                PlayerFaceRenderer.draw(graphics, info.getSkin().texture(), x, y, size);
            }
            x += size + gap;
        }
    }

    private static void renderHand(GuiGraphics graphics, Minecraft minecraft, BattleSnapshot snapshot,
                                   BattleSnapshot.CombatantView player, Layout layout, int mouseX, int mouseY) {
        if (player.hand().isEmpty()) return;
        Font font = minecraft.font;
        Rect first = layout.cards().getFirst(), last = layout.cards().getLast();
        for (int strip = 0; strip < 5; strip++) graphics.fill(Math.max(0, first.x() - 24 - strip * 10),
                first.y() + 42 + strip * 10, Math.min(graphics.guiWidth(), last.right() + 24 + strip * 10), graphics.guiHeight(), (0x12 + strip * 5) << 24);

        int hovered = -1;
        for (int i = 0; i < player.hand().size(); i++) {
            BattleSnapshot.CardView card = player.hand().get(i); Rect base = layout.cards().get(i);
            boolean selected = card.instanceId().equals(BattleClient.selectedCard()), hover = base.grow(0, 7).contains(mouseX, mouseY);
            if (hover) hovered = i;
            Rect rect = base.move(0, selected ? -12 : hover ? -7 : 0);
            boolean voiceHighlighted = BattleClient.isVoiceHighlighted(card.instanceId());
            if (voiceHighlighted) {
                float pulse = BattleClient.voiceHighlightProgress();
                rect = rect.move(Math.round(Mth.sin(pulse * 42.0F) * (1.0F - pulse) * 3.0F),
                        Math.round(Mth.cos(pulse * 35.0F) * (1.0F - pulse) * 2.0F));
            }
            renderCard(graphics, minecraft, player, card, rect, i, selected, hover, voiceHighlighted);
        }

        BattleSnapshot.CardView selected = selectedCard(player);
        Component hint = BattleClient.itemTargeting() ? Component.translatable("hud.exworld.item_target_hint") : selected == null ? Component.translatable("hud.exworld.move_hint")
                : Component.translatable("hud.exworld.target_hint", Component.translatable(targetKey(selected.targetType())));
        graphics.drawCenteredString(font, hint, layout.handCenterX(), first.y() - 17, selected == null ? MUTED : GOLD);

        if (hovered >= 0) graphics.renderTooltip(font, cardTooltip(player.hand().get(hovered), player), Optional.empty(), mouseX, mouseY);
    }

    private static List<Component> cardTooltip(BattleSnapshot.CardView card, BattleSnapshot.CombatantView player) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable(card.nameKey()).withColor(targetColor(card.targetType()) & 0xFFFFFF));
        tooltip.add(Component.translatable(card.descriptionKey()).withColor(0xD4DAE1));
        tooltip.add(Component.translatable("hud.exworld.card_details", card.manaCost(), card.range(), Component.translatable(targetKey(card.targetType()))));
        tooltip.add(Component.literal("★".repeat(card.star())).withColor(0xF2CB72));
        tooltip.add(Component.translatable("hud.exworld.card_type_" + card.type().name().toLowerCase(Locale.ROOT)).withColor(targetColor(card.targetType()) & 0xFFFFFF));
        if (card.keywords().contains(net.exmo.exworld.battle.card.CardDefinition.CardKeyword.ETHEREAL)) tooltip.add(Component.translatable("hud.exworld.ethereal"));
        if (card.keywords().contains(net.exmo.exworld.battle.card.CardDefinition.CardKeyword.EXHAUST)) tooltip.add(Component.translatable("hud.exworld.exhaust"));
        if (card.keywords().contains(net.exmo.exworld.battle.card.CardDefinition.CardKeyword.RETAIN)) tooltip.add(Component.translatable("hud.exworld.retained"));
        if (!card.playable()) tooltip.add(Component.translatable(card.manaCost() > player.mana() ? "hud.exworld.no_mana" : "hud.exworld.not_your_turn"));
        return tooltip;
    }

    private static void renderCard(GuiGraphics graphics, Minecraft minecraft, BattleSnapshot.CombatantView player,
                                   BattleSnapshot.CardView card, Rect rect, int index, boolean selected, boolean hover,
                                   boolean voiceHighlighted) {
        Font font = minecraft.font; int accent = targetColor(card.targetType());
        shadow(graphics, rect, selected ? 4 : 3);
        if (voiceHighlighted) {
            int alpha = 0x70 + Math.round((1.0F - BattleClient.voiceHighlightProgress()) * 0x65);
            cutPanel(graphics, rect.grow(5), alpha << 24 | (accent & 0xFFFFFF), accent, 5);
        }
        if (selected) { cutPanel(graphics, rect.grow(3), 0x883B3020, GOLD, 5); }
        drawCardFrame(graphics, rect, card, card.playable() ? 0xF20F151E : 0xED111419,
                selected ? GOLD : hover ? 0xFFDDE7EF : 0xFF687583);
        graphics.fill(rect.x() + 3, rect.y() + 3, rect.right() - 3, rect.y() + 6, accent);

        Rect art = new Rect(rect.x() + 6, rect.y() + 27, rect.w() - 12, Math.max(34, rect.h() / 3));
        drawArt(graphics, minecraft, card, art, accent);
        outline(graphics, art, 0xAA8793A0);

        gem(graphics, rect.x() + 5, rect.y() + 7, 20, MANA);
        graphics.drawCenteredString(font, Integer.toString(card.manaCost()), rect.x() + 15, rect.y() + 13, TEXT);
        if (index >= 0) graphics.drawString(font, Integer.toString(index + 1), rect.right() - 12, rect.y() + 9, MUTED, false);
        Component name = Component.translatable(card.nameKey());
        graphics.drawCenteredString(font, trim(font, name.getString(), rect.w() - 34), rect.centerX() + 5, rect.y() + 10, TEXT);

        int starY = art.bottom() + 4;
        String stars = "★".repeat(card.star()) + "☆".repeat(Math.max(0, 5 - card.star()));
        graphics.drawCenteredString(font, stars, rect.centerX(), starY, GOLD);
        int descriptionY = starY + 12;
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.translatable(card.descriptionKey()), rect.w() - 12);
        int visible = rect.h() >= 120 ? 2 : 1;
        for (int line = 0; line < Math.min(visible, lines.size()); line++)
            graphics.drawString(font, lines.get(line), rect.x() + 6, descriptionY + line * 10, MUTED, false);

        int footerY = rect.bottom() - 19;
        graphics.fill(rect.x() + 5, footerY - 3, rect.right() - 5, footerY - 2, 0x665E6C7A);
        graphics.drawString(font, Component.literal("◇ " + card.range()), rect.x() + 7, footerY + 2, TEXT, false);
        Component target = Component.translatable(targetKey(card.targetType()));
        graphics.drawString(font, trim(font, target.getString(), rect.w() / 2), rect.right() - font.width(trim(font, target.getString(), rect.w() / 2)) - 7, footerY + 2, accent, false);
        if (card.innate()) {
            graphics.fill(rect.x() + 5, art.y() + 4, rect.x() + 33, art.y() + 16, 0xD286642D);
            graphics.drawCenteredString(font, Component.translatable("hud.exworld.innate"), rect.x() + 19, art.y() + 6, 0xFFFFE5A5);
        }
        if(card.retained()){graphics.fill(rect.right()-36,art.y()+4,rect.right()-5,art.y()+16,0xD23A7055);graphics.drawCenteredString(font,Component.translatable("hud.exworld.retained"),rect.right()-20,art.y()+6,0xFFB9F3D0);}
        if (!card.playable()) {
            graphics.fill(rect.x() + 2, rect.y() + 2, rect.right() - 2, rect.bottom() - 2, 0x77000000);
            Component reason = Component.translatable(card.manaCost() > player.mana() ? "hud.exworld.no_mana" : "hud.exworld.not_your_turn");
            graphics.fill(rect.x() + 6, rect.centerY() - 8, rect.right() - 6, rect.centerY() + 8, 0xD0111419);
            graphics.drawCenteredString(font, trim(font, reason.getString(), rect.w() - 16), rect.centerX(), rect.centerY() - 4, 0xFFFFA3A8);
        }
    }

    private static void drawCardFrame(GuiGraphics graphics, Rect rect, BattleSnapshot.CardView card, int fill, int edge) {
        switch (card.type()) {
            case ATTACK -> {
                polygon(graphics, rect, fill, edge);
            }
            case POWER -> {
                int cx = rect.centerX(), cy = rect.centerY(), radius = Math.min(rect.w(), rect.h()) / 2;
                for (int y = -radius; y <= radius; y++) {
                    int span = (int) Math.sqrt(radius * radius - y * y);
                    graphics.fill(cx - span, cy + y, cx + span + 1, cy + y + 1, fill);
                }
                outline(graphics, rect, edge);
            }
            default -> cutPanel(graphics, rect, fill, edge, 5);
        }
    }

    private static void polygon(GuiGraphics graphics, Rect rect, int fill, int edge) {
        int top = rect.centerX(), left = rect.x(), right = rect.right() - 1, bottom = rect.bottom() - 1, shoulder = rect.y() + 10;
        for (int y = rect.y(); y <= bottom; y++) {
            float progress = y < shoulder ? (y - rect.y()) / (float) Math.max(1, shoulder - rect.y()) : (y - shoulder) / (float) Math.max(1, bottom - shoulder);
            int x1 = y < shoulder ? Math.round(top + (left - top) * progress) : Math.round(left + 5 * progress);
            int x2 = y < shoulder ? Math.round(top + (right - top) * progress) : Math.round(right - 5 * progress);
            graphics.fill(x1, y, x2 + 1, y + 1, fill);
        }
        graphics.fill(top, rect.y(), top + 1, rect.y() + 1, edge);
        graphics.fill(left + 5, bottom, right - 4, bottom + 1, edge);
    }

    private static void renderCardCastAnimations(GuiGraphics graphics, Minecraft minecraft,
                                                 BattleSnapshot.CombatantView player, int width, int height) {
        for (BattleClient.CardCastAnimation animation : BattleClient.cardCastAnimations()) {
            float progress = Mth.clamp(animation.age() / 18.0F, 0, 1);
            float lift = Math.min(120.0F, height * .28F) * (1.0F - (1.0F - progress) * (1.0F - progress));
            float scale = 1.0F + Mth.sin(progress * (float) Math.PI) * .28F;
            Rect card = new Rect(-42, -64, 84, 128);
            graphics.pose().pushPose();
            graphics.pose().translate(width / 2.0F, height - 105.0F - lift, 400);
            graphics.pose().scale(scale, scale, 1);
            renderCard(graphics, minecraft, player, animation.card(), card, -1, true, false, false);
            if (progress > .68F) {
                int alpha = Mth.clamp((int) ((progress - .68F) / .32F * 235), 0, 235);
                graphics.fill(card.x() - 4, card.y() - 4, card.right() + 4, card.bottom() + 4, alpha << 24);
            }
            graphics.pose().popPose();
        }
    }

    private static void drawArt(GuiGraphics graphics, Minecraft minecraft, BattleSnapshot.CardView card, Rect art, int accent) {
        int dark = 0xFF000000 | ((accent >> 1) & 0x007F7F7F);
        graphics.fill(art.x(), art.y(), art.right(), art.bottom(), 0xFF111822);
        int bands = Math.max(1, art.h() / 6);
        for (int i = 0; i < 6; i++) graphics.fill(art.x(), art.y() + i * bands, art.right(), Math.min(art.bottom(), art.y() + (i + 1) * bands),
                withAlpha(blend(dark, accent, i / 8F), 210));
        Optional<ResourceLocation> icon = icon(card.icon(), minecraft);
        if (icon.isPresent()) {
            int size = Math.min(art.w() - 8, art.h() - 6), x = art.centerX() - size / 2, y = art.centerY() - size / 2;
            graphics.blit(icon.get(), x, y, 0, 0, size, size, size, size);
        } else {
            String glyph = switch (card.targetType()) { case ENEMY -> "✦"; case ALLY -> "+"; case SELF -> "◆"; case CELL -> "◇"; };
            graphics.pose().pushPose(); graphics.pose().translate(art.centerX(), art.centerY(), 0); graphics.pose().scale(1.7F, 1.7F, 1);
            graphics.drawCenteredString(minecraft.font, glyph, 0, -4, 0xDDFFFFFF); graphics.pose().popPose();
        }
        graphics.fill(art.x(), art.bottom() - 8, art.right(), art.bottom(), 0x66000000);
    }

    public static Hit hitTest(double x, double y) {
        Layout layout = lastLayout;
        if(BattleClient.pileOverlay()!=BattleClient.PileOverlay.NONE&&layout.pilePanel().contains(x,y))return new Hit(Action.BLOCKED,-1);
        BattleSnapshot.CombatantView actor = BattleClient.localCombatant();
        int visibleItemCount = actor == null ? 0 : usableItems(actor.items()).size();
        for (int i = 0; i < layout.itemRows().size(); i++) {
            int itemIndex = itemScroll * ITEM_COLUMNS + i;
            if (itemIndex < visibleItemCount && layout.itemRows().get(i).contains(x, y)) return new Hit(Action.ITEM, itemIndex);
        }
        if (BattleClient.itemTargeting() && (layout.itemPanel().contains(x, y) || layout.cards().stream().anyMatch(card -> card.grow(0, 14).contains(x, y))))
            return new Hit(Action.BLOCKED, -1);
        for (int i = layout.cards().size() - 1; i >= 0; i--) if (layout.cards().get(i).grow(0, 14).contains(x, y))
            return new Hit(Action.CARD, i, actor != null && i < actor.hand().size() ? actor.hand().get(i).instanceId() : null);
        if (layout.ready().contains(x, y)) return new Hit(Action.READY, -1);
        if (layout.auto().contains(x, y)) return new Hit(Action.AUTO, -1);
        if (layout.escape().contains(x, y)) return new Hit(Action.ESCAPE, -1);
        if (layout.backpack().contains(x, y)) return new Hit(Action.BACKPACK, -1);
        if (layout.weapon1().contains(x, y)) return new Hit(Action.WEAPON_1, -1);
        if (layout.weapon2().contains(x, y)) return new Hit(Action.WEAPON_2, -1);
        if(layout.drawPile().contains(x,y))return new Hit(Action.DRAW_PILE,-1);
        if(layout.discardPile().contains(x,y))return new Hit(Action.DISCARD_PILE,-1);
        if (layout.log().contains(x, y)) return new Hit(Action.LOG, -1);
        if (layout.header().contains(x, y) || layout.status().contains(x, y) || layout.actions().contains(x, y) || layout.itemPanel().contains(x, y)
                || layout.target().contains(x, y)) return new Hit(Action.BLOCKED, -1);
        return new Hit(Action.WORLD, -1);
    }

    public static void toggleLog() { logExpanded = !logExpanded; logScroll = 0; }
    public static boolean scrollLog(double mouseX, double mouseY, double delta) {
        if (!logExpanded || !lastLayout.log().contains(mouseX, mouseY) || delta == 0) return false;
        logScroll = Math.max(0, logScroll + (delta > 0 ? 1 : -1));
        return true;
    }
    public static boolean scrollItems(double mouseX, double mouseY, double delta) {
        if (!lastLayout.itemPanel().contains(mouseX, mouseY) || delta == 0) return false;
        BattleSnapshot.CombatantView actor = BattleClient.localCombatant();
        if (actor == null) return false;
        int itemCount = usableItems(actor.items()).size();
        if (maxItemScroll(itemCount, ITEM_VISIBLE_SLOTS, ITEM_COLUMNS) == 0) return false;
        itemScroll = scrollItemRows(itemScroll, (int) Math.signum(delta), itemCount, ITEM_VISIBLE_SLOTS, ITEM_COLUMNS);
        return true;
    }
    public static boolean beginItemPanelDrag(double mouseX, double mouseY) {
        Rect panel = lastLayout.itemPanel();
        if (!panel.visible() || mouseY >= panel.y() + 24 || !panel.contains(mouseX, mouseY)) return false;
        draggingItemPanel = true;
        itemPanelGrabX = (int) mouseX - panel.x();
        itemPanelGrabY = (int) mouseY - panel.y();
        return true;
    }
    public static boolean endItemPanelDrag() {
        boolean wasDragging = draggingItemPanel;
        draggingItemPanel = false;
        return wasDragging;
    }
    public static void resetLog() { logExpanded = false; logScroll = 0; }

    private static void drawAction(GuiGraphics graphics, Font font, Rect rect, Component label, String key,
                                   boolean enabled, boolean active, int mouseX, int mouseY) {
        boolean hover = enabled && rect.contains(mouseX, mouseY);
        int background = !enabled ? 0xAA181D23 : active ? 0xDD5E4926 : hover ? 0xDD314252 : 0xCC222C37;
        cutPanel(graphics, rect, background, active ? GOLD : enabled ? 0x887F91A2 : 0x554B5660, 3);
        if (rect.w() < 55) {
            // The three bottom-right buttons are intentionally narrow. Center the full
            // localized label so Chinese labels such as “背包” and “武器1” are not clipped.
            String value = trim(font, label.getString(), rect.w() - 6);
            graphics.drawCenteredString(font, value, rect.centerX(), rect.y() + (rect.h() - 8) / 2,
                    enabled ? TEXT : MUTED);
        } else {
            graphics.drawString(font, key, rect.x() + 7, rect.y() + (rect.h() - 8) / 2, enabled ? GOLD : 0xFF66707A, true);
            String value = trim(font, label.getString(), rect.w() - 30);
            graphics.drawString(font, value, rect.right() - font.width(value) - 7, rect.y() + (rect.h() - 8) / 2, enabled ? TEXT : MUTED, false);
        }
    }

    private static void drawCompactResource(GuiGraphics graphics, Font font, int x, int y, int width, int color,
                                            float value, float maximum, float absorption, String label) {
        graphics.drawString(font, label, x, y, color, true);
        String numbers = "HP".equals(label) ? healthNumbers(value, maximum, absorption) : Mth.ceil(value) + "/" + Mth.ceil(maximum);
        graphics.drawString(font, numbers, x + width - font.width(numbers), y, TEXT, false);
        drawBar(graphics, x + 20, y + 3, Math.max(8, width - 20 - font.width(numbers) - 5), 4, maximum <= 0 ? 0 : value / maximum, color);
    }

    static String healthNumbers(float health, float maximum, float absorption) {
        String value = Mth.ceil(health) + "/" + Mth.ceil(maximum);
        return absorption > 0.0F ? value + " +" + Mth.ceil(absorption) : value;
    }

    static float hudAbsorption(float battleBlock, float vanillaAbsorption) {
        return Math.max(0.0F, battleBlock) + Math.max(0.0F, vanillaAbsorption);
    }

    private static void drawBar(GuiGraphics graphics, int x, int y, int width, int height, float progress, int color) {
        graphics.fill(x, y, x + width, y + height, 0xDD080C11);
        int fill = Math.round((width - 2) * Mth.clamp(progress, 0, 1));
        if (fill > 0) graphics.fill(x + 1, y + 1, x + 1 + fill, y + height - 1, color);
    }

    private static void cutPanel(GuiGraphics graphics, Rect rect, int fill, int edge, int cut) {
        graphics.fill(rect.x() + cut, rect.y(), rect.right() - cut, rect.bottom(), fill);
        graphics.fill(rect.x(), rect.y() + cut, rect.right(), rect.bottom() - cut, fill);
        graphics.fill(rect.x() + cut, rect.y(), rect.right() - cut, rect.y() + 1, edge);
        graphics.fill(rect.x() + cut, rect.bottom() - 1, rect.right() - cut, rect.bottom(), edge);
        graphics.fill(rect.x(), rect.y() + cut, rect.x() + 1, rect.bottom() - cut, edge);
        graphics.fill(rect.right() - 1, rect.y() + cut, rect.right(), rect.bottom() - cut, edge);
    }

    private static void outline(GuiGraphics graphics, Rect rect, int color) {
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.y() + 1, color); graphics.fill(rect.x(), rect.bottom() - 1, rect.right(), rect.bottom(), color);
        graphics.fill(rect.x(), rect.y(), rect.x() + 1, rect.bottom(), color); graphics.fill(rect.right() - 1, rect.y(), rect.right(), rect.bottom(), color);
    }

    private static void shadow(GuiGraphics graphics, Rect rect, int distance) {
        graphics.fill(rect.x() + distance, rect.y() + distance, rect.right() + distance, rect.bottom() + distance, 0x66000000);
    }

    private static void gem(GuiGraphics graphics, int x, int y, int size, int color) {
        graphics.fill(x + 3, y, x + size - 3, y + size, 0xFF0B1119); graphics.fill(x, y + 3, x + size, y + size - 3, 0xFF0B1119);
        graphics.fill(x + 4, y + 2, x + size - 4, y + size - 2, color); graphics.fill(x + 2, y + 4, x + size - 2, y + size - 4, color);
    }

    private static Optional<ResourceLocation> icon(String path, Minecraft minecraft) {
        if (path == null || path.isBlank()) return Optional.empty();
        return ICON_CACHE.computeIfAbsent(path, value -> {
            try { ResourceLocation location = ResourceLocation.parse(value); return minecraft.getResourceManager().getResource(location).isPresent() ? Optional.of(location) : Optional.empty(); }
            catch (Exception ignored) { return Optional.empty(); }
        });
    }

    private static float phaseProgress(BattleSnapshot snapshot) {
        int total = snapshot.state() == BattleState.INTRO && snapshot.intro() != null ? snapshot.intro().totalTicks()
                : snapshot.state() == BattleState.DEPLOYMENT ? BattleSession.DEFAULT_DEPLOYMENT_TICKS
                : snapshot.state() == BattleState.REWARD ? BattleSession.RESULT_TIMEOUT_TICKS : BattleSession.DEFAULT_PHASE_TICKS;
        return Mth.clamp(snapshot.phaseTicksRemaining() / (float) Math.max(1, total), 0, 1);
    }

    private static int targetColor(SkillDefinition.TargetType type) {
        return switch (type) { case ENEMY -> 0xFFE46F6B; case ALLY -> 0xFF62D691; case SELF -> 0xFF69A7F7; case CELL -> 0xFFE3B65C; };
    }
    private static Component statusBadge(BattleSnapshot.StatusView status, int nameLength) {
        String suffix = isPotionStatus(status) ? "Lv." + status.stacks() : Integer.toString(status.remainingRounds());
        return Component.literal(shorten(Component.translatable(status.nameKey()).getString(), nameLength) + " " + suffix);
    }
    private static boolean isPotionStatus(BattleSnapshot.StatusView status) { return status.id().startsWith("minecraft:"); }
    private static Component potionEffectDescription(BattleSnapshot.StatusView status) {
        if (!isPotionStatus(status)) return Component.translatable("hud.exworld.status_effect_generic");
        String effect = status.id().substring("minecraft:".length());
        return switch (effect) {
            case "speed", "slowness", "haste", "mining_fatigue", "strength", "weakness", "regeneration", "poison", "wither",
                    "resistance", "fire_resistance", "water_breathing", "invisibility", "night_vision", "blindness", "jump_boost",
                    "absorption", "health_boost", "levitation", "slow_falling", "glowing", "darkness", "hunger", "nausea",
                    "instant_health", "instant_damage", "saturation", "luck", "unluck", "wind_charged", "weaving", "oozing", "infested"
                    -> Component.translatable("hud.exworld.potion_effect." + effect);
            default -> Component.translatable("hud.exworld.potion_effect.generic");
        };
    }
    private static int targetColorForFaction(BattleSnapshot snapshot) { return snapshot.activeFaction().equals(BattleClient.localCombatant().factionId()) ? MOVE : HP; }
    private static String targetKey(SkillDefinition.TargetType type) { return "hud.exworld.target_" + type.name().toLowerCase(Locale.ROOT); }
    private static BattleSnapshot.CardView selectedCard(BattleSnapshot.CombatantView player) { return player.hand().stream().filter(card -> card.instanceId().equals(BattleClient.selectedCard())).findFirst().orElse(null); }
    private static boolean isLocalTurn(BattleSnapshot snapshot, BattleSnapshot.CombatantView player) { return snapshot.state() == BattleState.FACTION_PHASE && !player.downed() && player.factionId().equals(snapshot.activeFaction()); }
    private static String trim(Font font, String text, int width) { if (font.width(text) <= width) return text; String value = font.plainSubstrByWidth(text, Math.max(0, width - font.width("…"))); return value + "…"; }
    private static String shorten(String value, int length) { return value.length() <= length ? value : value.substring(0, Math.max(1, length - 1)) + "…"; }
    private static int withAlpha(int rgb, int alpha) { return (alpha << 24) | (rgb & 0xFFFFFF); }
    private static int blend(int a, int b, float progress) { int ar=(a>>16)&255,ag=(a>>8)&255,ab=a&255,br=(b>>16)&255,bg=(b>>8)&255,bb=b&255;return 0xFF000000|((int)(ar+(br-ar)*progress)<<16)|((int)(ag+(bg-ag)*progress)<<8)|(int)(ab+(bb-ab)*progress); }

    public enum Action { WORLD, BLOCKED, LOG, CARD, READY, AUTO, ESCAPE, BACKPACK, WEAPON_1, WEAPON_2, ITEM, DRAW_PILE, DISCARD_PILE }
    public record Hit(Action action, int index, UUID cardId) {
        public Hit(Action action, int index) { this(action, index, null); }
    }

    private record Rect(int x, int y, int w, int h) {
        int right() { return x + w; } int bottom() { return y + h; } int centerX() { return x + w / 2; } int centerY() { return y + h / 2; }
        boolean visible() { return w > 0 && h > 0; }
        boolean contains(double px, double py) { return px >= x && px < right() && py >= y && py < bottom(); }
        Rect move(int dx, int dy) { return new Rect(x + dx, y + dy, w, h); } Rect grow(int value) { return new Rect(x-value,y-value,w+value*2,h+value*2); }
        Rect grow(int horizontal, int vertical) { return new Rect(x-horizontal,y-vertical,w+horizontal*2,h+vertical*2); }
    }

    private static void updateItemPanelDrag(int mouseX, int mouseY, int width, int height) {
        if (!draggingItemPanel) return;
        itemPanelX = Mth.clamp(mouseX - itemPanelGrabX, 8, Math.max(8, width - 224));
        itemPanelY = Mth.clamp(mouseY - itemPanelGrabY, 42, Math.max(42, height - 124));
    }
    private record LayoutKey(int width, int height, int cardCount, int itemCount, boolean expandedLog, boolean itemsOpen,
                             boolean targetDetails, int itemPanelX, int itemPanelY) { }

    private record Layout(Rect header, Rect status, Rect actions, Rect target, Rect log, Rect ready, Rect auto, Rect escape,
                          Rect backpack, Rect weapon1, Rect weapon2, Rect drawPile, Rect discardPile, Rect pilePanel,
                          Rect itemPanel, List<Rect> itemRows, List<Rect> cards, int handCenterX) {
        private static final Rect ZERO = new Rect(0,0,0,0);
        private static final Layout EMPTY = new Layout(
                ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO,
                ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO,
                List.of(), List.of(), 0);
        private static Layout create(int width, int height, int cardCount, int itemCount, boolean expandedLog, boolean itemsOpen,
                                     boolean targetDetails, int itemPanelX, int itemPanelY) {
            boolean compact = width < 760;
            int headerW = Math.min(compact ? width - 20 : 470, Math.max(260, width - 20));
            Rect header = new Rect((width-headerW)/2, 8, headerW, 34);
            int sideY = compact ? 74 : height - 102;
            Rect status = new Rect(10, sideY, compact ? Math.min(205,width/2-15) : 214, 64);
            Rect actions = new Rect(width-(compact?142:154)-10, sideY, compact?142:154, 92);
            Rect ready = new Rect(actions.x()+6,actions.y()+6,actions.w()-12,24);
            Rect auto = new Rect(actions.x()+6,actions.y()+34,(actions.w()-15)/2,23);
            Rect escape = new Rect(auto.right()+3,actions.y()+34,actions.right()-auto.right()-9,23);
            int actionWidth = (actions.w() - 15) / 3;
            Rect backpack = new Rect(actions.x() + 6, actions.y() + 61, actionWidth, 23);
            Rect weapon1 = new Rect(backpack.right() + 3, actions.y() + 61, actionWidth, 23);
            Rect weapon2 = new Rect(weapon1.right() + 3, actions.y() + 61, actions.right() - weapon1.right() - 9, 23);
            boolean compactDetails = !compact || (width >= 460 && height >= 315);
            int detailY = compact ? 144 : 48;
            int targetHeight = targetDetails ? Math.min(194, Math.max(72, height - detailY - 8)) : 72;
            Rect target = compactDetails ? new Rect(width-(compact?194:216)-10,detailY,compact?194:216,targetHeight) : ZERO;
            int logWidth = compact ? Math.min(300,width-20) : Math.min(360,width-20);
            int logHeight = expandedLog ? Math.min(190,Math.max(92,height-detailY-145)) : 84;
            Rect log = compactDetails ? new Rect(10,detailY,logWidth,logHeight) : ZERO;

            int left = compact ? 8 : status.right()+8, right = compact ? width-8 : actions.x()-8;
            int available = Math.max(100,right-left), gap=5, desired=84;
            int cardW = cardCount==0?desired:Mth.clamp((available-gap*Math.max(0,cardCount-1))/Math.max(1,cardCount),58,desired);
            if(cardCount>1&&cardW*cardCount+gap*(cardCount-1)>available)gap=Math.max(-cardW+22,(available-cardW*cardCount)/(cardCount-1));
            int cardH = compact?112:128, total=cardCount*cardW+Math.max(0,cardCount-1)*gap, start=(left+right-total)/2, cardY=height-cardH-7;
            List<Rect> cards=new ArrayList<>();for(int i=0;i<cardCount;i++)cards.add(new Rect(start+i*(cardW+gap),cardY,cardW,cardH));
            int pileWidth=(status.w()-6)/2, pileY=Math.max(2,status.y()-26);
            Rect drawPile=new Rect(status.x(),pileY,pileWidth,22),discardPile=new Rect(status.right()-pileWidth,pileY,pileWidth,22);
            Rect pilePanel=new Rect(Math.max(12,width/2-260),Math.max(52,height/2-120),Math.min(520,width-24),Math.min(240,height-104));
            int defaultItemPanelX = Math.max(8, actions.x() - 232), defaultItemPanelY = Math.max(42, actions.y() - 132);
            int resolvedItemPanelX = itemPanelX == 0 ? defaultItemPanelX : Mth.clamp(itemPanelX, 8, Math.max(8, width - 224));
            int resolvedItemPanelY = itemPanelY == 0 ? defaultItemPanelY : Mth.clamp(itemPanelY, 42, Math.max(42, height - 124));
            Rect itemPanel = itemsOpen ? new Rect(resolvedItemPanelX, resolvedItemPanelY, 224, 124) : ZERO;
            List<Rect> itemRows = new ArrayList<>();
            for (int i = 0; i < itemCount && i < 6; i++) itemRows.add(new Rect(itemPanel.x() + 8 + (i % 2) * 105, itemPanel.y() + 26 + (i / 2) * 30, 100, 26));
            return new Layout(header,status,actions,target,log,ready,auto,escape,backpack,weapon1,weapon2,drawPile,discardPile,pilePanel,itemPanel,List.copyOf(itemRows),List.copyOf(cards),(left+right)/2);
        }
    }
}
