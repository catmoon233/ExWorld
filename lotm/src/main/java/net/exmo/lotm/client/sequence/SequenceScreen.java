package net.exmo.lotm.client.sequence;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.exmo.exworld.inventory.InventoryLayout;
import net.exmo.exworld.network.InventoryPayloads;
import net.exmo.lotm.network.SequencePayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

 import java.util.ArrayList;
 import java.util.List;

/** Sequence sheet in the same window as the backpack. Empty ranks are omitted by the snapshot. */
public final class SequenceScreen extends Screen {
    private static final int TAB_W = 60;
    private static final int LIST_W = 148;
    private static final int ENTRY_H = 28;
    private static final int SKILL = 18;
     private static final int INTRO_H = 48;
    private static final int LINE = 11;

    private int panelX;
    private int panelY;
    private int scroll;
    private int introScroll;
    private int skillScroll;
    private int selected = -1;
    private String selectedSkill = "";
    private String boundCurrent = "";


    public SequenceScreen() {
        super(Component.translatable("screen.exworld.sequence"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        panelX = (width - InventoryLayout.IMAGE_WIDTH) / 2;
        panelY = (height - InventoryLayout.IMAGE_HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, InventoryLayout.PANEL);
        SequencePayloads.SequenceSnapshotPayload snapshot = ClientSequenceState.snapshot();
        syncSelection(snapshot);
        drawChrome(graphics, mouseX, mouseY);
        if (!snapshot.hasSequence() || snapshot.entries().isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.exworld.sequence_empty"),
                    panelX + InventoryLayout.IMAGE_WIDTH / 2, panelY + InventoryLayout.IMAGE_HEIGHT / 2, InventoryLayout.MUTED);
            return;
        }
        drawHeader(graphics, snapshot);
        drawList(graphics, snapshot, mouseX, mouseY);
        drawDetail(graphics, snapshot, mouseX, mouseY);
    }

    private void syncSelection(SequencePayloads.SequenceSnapshotPayload snapshot) {
        if (snapshot.currentId().equals(boundCurrent) && selected >= 0 && selected < snapshot.entries().size()) return;
        boundCurrent = snapshot.currentId();
        selected = 0;
        selectedSkill = "";
        introScroll = 0;
        skillScroll = 0;

        for (int i = 0; i < snapshot.entries().size(); i++) {
            if (snapshot.entries().get(i).id().equals(snapshot.currentId())) selected = i;
        }
    }

    private void drawChrome(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = panelX;
        int y = panelY;
        drawTab(graphics, x, y, Component.translatable("screen.exworld.backpack_tab"), false,
                hit(mouseX, mouseY, x, y, TAB_W, InventoryLayout.TAB_H));
        drawTab(graphics, x + 64, y, Component.translatable("screen.exworld.character_tab"), false, false);
        drawTab(graphics, x + 128, y, Component.translatable("screen.exworld.quests_tab"), false, false);
        drawTab(graphics, x + 192, y, Component.translatable("screen.exworld.sequence_tab"), true, false);
        int body = y + InventoryLayout.TAB_H;
        graphics.fill(x, body, x + InventoryLayout.IMAGE_WIDTH, y + InventoryLayout.IMAGE_HEIGHT, InventoryLayout.SURFACE);
        graphics.fill(x, body, x + InventoryLayout.IMAGE_WIDTH, body + 1, InventoryLayout.LINE);
        graphics.fill(x, y + InventoryLayout.IMAGE_HEIGHT - 1, x + InventoryLayout.IMAGE_WIDTH, y + InventoryLayout.IMAGE_HEIGHT, InventoryLayout.LINE);
        graphics.fill(x, body, x + 1, y + InventoryLayout.IMAGE_HEIGHT, InventoryLayout.LINE);
        graphics.fill(x + InventoryLayout.IMAGE_WIDTH - 1, body, x + InventoryLayout.IMAGE_WIDTH, y + InventoryLayout.IMAGE_HEIGHT, InventoryLayout.LINE);
        int split = x + LIST_W + 12;
        graphics.fill(split, body + 8, split + 1, y + InventoryLayout.IMAGE_HEIGHT - 8, InventoryLayout.LINE_INNER);
    }

    private void drawHeader(GuiGraphics graphics, SequencePayloads.SequenceSnapshotPayload snapshot) {
        int x = panelX + 10;
        int y = panelY + InventoryLayout.TAB_H + 6;
        Component name = Component.translatable(snapshot.currentNameKey());
        Component rank = Component.translatable(snapshot.currentRankKey());
        graphics.drawString(font, name, x, y, InventoryLayout.TEXT, false);
        graphics.drawString(font, rank, x + font.width(name) + 8, y, InventoryLayout.MUTED, false);
        if (!snapshot.pathwayKey().isBlank()) {
            graphics.drawString(font, Component.translatable(snapshot.pathwayKey()), x + LIST_W + 16, y, InventoryLayout.MUTED, false);
        }
    }

    private int listY() {
        return panelY + InventoryLayout.TAB_H + 22;
    }

    private int listH() {
        return panelY + InventoryLayout.IMAGE_HEIGHT - 8 - listY();
    }

    private void drawList(GuiGraphics graphics, SequencePayloads.SequenceSnapshotPayload snapshot, int mouseX, int mouseY) {
        int x = panelX + 8;
        int y = listY();
        int h = listH();
        graphics.fill(x, y, x + LIST_W, y + h, InventoryLayout.SURFACE_INNER);
        int maxScroll = Math.max(0, snapshot.entries().size() * ENTRY_H - h);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        graphics.enableScissor(x, y, x + LIST_W, y + h);
        for (int i = 0; i < snapshot.entries().size(); i++) {
            SequencePayloads.EntryView entry = snapshot.entries().get(i);
            int ey = y + i * ENTRY_H - scroll;
            boolean active = i == selected;
            boolean hover = mouseX >= x && mouseX < x + LIST_W && mouseY >= Math.max(y, ey) && mouseY < Math.min(y + h, ey + ENTRY_H);
            if (active || hover) {
                graphics.fill(x + 1, ey + 1, x + LIST_W - 1, ey + ENTRY_H - 1, active ? InventoryLayout.BUTTON_HOVER : InventoryLayout.BUTTON);
            }
            if (active) graphics.fill(x, ey + 4, x + 2, ey + ENTRY_H - 4, InventoryLayout.ACCENT);
            graphics.drawString(font, Component.translatable(entry.nameKey()), x + 8, ey + 4, InventoryLayout.TEXT, false);
            graphics.drawString(font, Component.translatable(entry.rankKey()), x + 8, ey + 15, InventoryLayout.MUTED, false);
        }
        graphics.disableScissor();
        if (maxScroll > 0) {
            int barH = Math.max(12, h * h / (h + maxScroll));
            int barY = y + (int) ((h - barH) * (scroll / (float) maxScroll));
            graphics.fill(x + LIST_W - 2, barY, x + LIST_W, barY + barH, InventoryLayout.LINE);
        }
    }

    private void drawDetail(GuiGraphics graphics, SequencePayloads.SequenceSnapshotPayload snapshot, int mouseX, int mouseY) {
        if (selected < 0 || selected >= snapshot.entries().size()) return;
        SequencePayloads.EntryView entry = snapshot.entries().get(selected);
        int x = detailX();
        int y = listY();
        int w = detailW();
        graphics.fill(x, y, x + w, y + INTRO_H, InventoryLayout.SURFACE_INNER);
         List<FormattedCharSequence> lines = wrap(Component.translatable(entry.introductionKey()), w - 16);
        introScroll = clampScroll(introScroll, lines.size(), INTRO_H);
        drawScrollingText(graphics, lines, x, y, w, INTRO_H, introScroll, InventoryLayout.TEXT);

        int skillsY = y + INTRO_H + 6;
        graphics.drawString(font, Component.translatable("screen.exworld.sequence_skills"), x, skillsY, InventoryLayout.MUTED, false);
        int iconY = skillsY + 12;
        int stride = SKILL + 4;
        int perRow = Math.max(1, w / stride);
        SequencePayloads.SkillView hovered = null;
        if (entry.skills().isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.exworld.sequence_no_skills"), x, iconY, InventoryLayout.MUTED, false);
        }
        for (int i = 0; i < entry.skills().size(); i++) {
            SequencePayloads.SkillView skill = entry.skills().get(i);
            int ix = x + (i % perRow) * stride;
            int iy = iconY + (i / perRow) * stride;
            if (iy + SKILL > skillDescY() - 4) break;
            boolean hover = hit(mouseX, mouseY, ix, iy, SKILL, SKILL);
            boolean chosen = skill.id().equals(selectedSkill);
            graphics.fill(ix - 1, iy - 1, ix + SKILL + 1, iy + SKILL + 1, chosen || hover ? InventoryLayout.ACCENT : InventoryLayout.LINE);
            graphics.fill(ix, iy, ix + SKILL, iy + SKILL, InventoryLayout.SLOT);
            drawSkillIcon(graphics, skill, ix + 1, iy + 1);
            if (hover) hovered = skill;
        }

        int descY = skillDescY();
        int descH = skillDescH();
        graphics.fill(x, descY, x + w, descY + descH, InventoryLayout.SURFACE_INNER);
        SequencePayloads.SkillView chosen = findSkill(entry, selectedSkill);
        if (chosen == null) {
            graphics.drawString(font, Component.translatable("screen.exworld.sequence_skill_empty"), x + 6, descY + 6, InventoryLayout.MUTED, false);
        } else {
            graphics.drawString(font, Component.translatable(chosen.nameKey()), x + 6, descY + 4, InventoryLayout.TEXT, false);
             List<FormattedCharSequence> body = wrap(Component.translatable(chosen.descriptionKey()), w - 16);
            int bodyY = descY + 16;
            int bodyH = Math.max(LINE, descH - 18);
            skillScroll = clampScroll(skillScroll, body.size(), bodyH);
            drawScrollingText(graphics, body, x, bodyY, w, bodyH, skillScroll, InventoryLayout.MUTED);
        }
        if (hovered != null) {
            Component label = Component.translatable(hovered.nameKey());
            int tw = font.width(label) + 8;
            int tx = Math.min(mouseX + 8, panelX + InventoryLayout.IMAGE_WIDTH - tw - 4);
            int ty = Math.max(panelY + InventoryLayout.TAB_H + 2, mouseY - 16);
            graphics.fill(tx, ty, tx + tw, ty + 14, InventoryLayout.TAB_IDLE);
            graphics.drawString(font, label, tx + 4, ty + 3, InventoryLayout.TEXT, false);
        }
    }

    private int detailX() {
        return panelX + 8 + LIST_W + 12;
    }

    private int detailW() {
        return panelX + InventoryLayout.IMAGE_WIDTH - 8 - detailX();
    }

    private int skillDescY() {
        return listY() + INTRO_H + 6 + 12 + SKILL + 6;
    }

    private int skillDescH() {
        return Math.max(LINE + 8, panelY + InventoryLayout.IMAGE_HEIGHT - 8 - skillDescY());
    }

    private int clampScroll(int value, int lines, int viewH) {
        int max = Math.max(0, lines * LINE - Math.max(0, viewH - 8));
        return Math.max(0, Math.min(value, max));
    }

    private void drawScrollingText(GuiGraphics graphics, List<FormattedCharSequence> lines, int x, int y, int w, int h, int textScroll, int color) {
        int max = Math.max(0, lines.size() * LINE - Math.max(0, h - 8));
        int used = Math.max(0, Math.min(textScroll, max));
        graphics.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        int lineY = y + 4 - used;
        for (FormattedCharSequence line : lines) {
             if (lineY >= y + 2 && lineY + LINE <= y + h - 1) {
                graphics.drawString(font, line, x + 6, lineY, color, false);
            }
            lineY += LINE;
        }
        graphics.disableScissor();
        if (max > 0) {
            int barH = Math.max(10, h * h / (h + max));
            int barY = y + (int) ((h - barH) * (used / (float) max));
            graphics.fill(x + w - 3, barY, x + w - 1, barY + barH, InventoryLayout.LINE);
        }
    }


    private void drawSkillIcon(GuiGraphics graphics, SequencePayloads.SkillView skill, int x, int y) {
        if (!skill.spellId().isBlank()) {
            AbstractSpell spell = SpellRegistry.getSpell(ResourceLocation.parse(skill.spellId()));
            if (spell != null && spell != SpellRegistry.none()) {
                graphics.blit(spell.getSpellIconResource(), x, y, 0, 0, 16, 16, 16, 16);
                return;
            }
        }
        ItemStack stack = ItemStack.EMPTY;
        if (!skill.iconItem().isBlank()) {
            try {
                var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(skill.iconItem()));
                if (item != null) stack = new ItemStack(item);
            } catch (RuntimeException ignored) {
                stack = ItemStack.EMPTY;
            }
        }
        if (!stack.isEmpty()) graphics.renderItem(stack, x, y);
    }

    private static SequencePayloads.SkillView findSkill(SequencePayloads.EntryView entry, String id) {
        if (id == null || id.isBlank()) return null;
        for (SequencePayloads.SkillView skill : entry.skills()) {
            if (id.equals(skill.id())) return skill;
        }
        return null;
    }

    private void drawTab(GuiGraphics graphics, int x, int y, Component label, boolean active, boolean hovered) {
        int bg = active ? InventoryLayout.SURFACE : hovered ? InventoryLayout.BUTTON_HOVER : InventoryLayout.TAB_IDLE;
        graphics.fill(x, y, x + TAB_W, y + InventoryLayout.TAB_H, bg);
        graphics.fill(x, y + InventoryLayout.TAB_H - 1, x + TAB_W, y + InventoryLayout.TAB_H,
                active ? InventoryLayout.ACCENT : InventoryLayout.LINE_INNER);
        graphics.drawCenteredString(font, label, x + TAB_W / 2, y + 7, active ? InventoryLayout.TEXT : InventoryLayout.MUTED);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        if (hit(mouseX, mouseY, panelX, panelY, TAB_W, InventoryLayout.TAB_H)) {
            playClick();
            PacketDistributor.sendToServer(new InventoryPayloads.OpenBackpackPayload());
            return true;
        }
        SequencePayloads.SequenceSnapshotPayload snapshot = ClientSequenceState.snapshot();
        int listX = panelX + 8;
        int y = listY();
        int h = listH();
        if (hit(mouseX, mouseY, listX, y, LIST_W, h)) {
            int index = (int) ((mouseY - y + scroll) / ENTRY_H);
            if (index >= 0 && index < snapshot.entries().size()) {
                if (index != selected) {
                    selectedSkill = "";
                    skillScroll = 0;
                }
                selected = index;
                introScroll = 0;
                playClick();
            }
            return true;
        }
        if (selected >= 0 && selected < snapshot.entries().size()) {
            SequencePayloads.EntryView entry = snapshot.entries().get(selected);
            int x = detailX();
            int w = detailW();
            int stride = SKILL + 4;
            int perRow = Math.max(1, w / stride);
            int iconY = listY() + INTRO_H + 6 + 12;
            for (int i = 0; i < entry.skills().size(); i++) {
                int ix = x + (i % perRow) * stride;
                int iy = iconY + (i / perRow) * stride;
                if (iy + SKILL > skillDescY() - 4) break;

                if (hit(mouseX, mouseY, ix, iy, SKILL, SKILL)) {
                    selectedSkill = entry.skills().get(i).id();
                    skillScroll = 0;
                    playClick();

                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int step = (int) Math.signum(scrollY) * LINE;
        if (hit(mouseX, mouseY, panelX + 8, listY(), LIST_W, listH())) {
            scroll = Math.max(0, scroll - (int) Math.signum(scrollY) * ENTRY_H);
            return true;
        }
        if (hit(mouseX, mouseY, detailX(), listY(), detailW(), INTRO_H)) {
            introScroll = Math.max(0, introScroll - step);
            return true;
        }
        if (hit(mouseX, mouseY, detailX(), skillDescY(), detailW(), skillDescH())) {
            skillScroll = Math.max(0, skillScroll - step);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }


    private void playClick() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
        }
    }

    private static boolean hit(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
 
     private List<FormattedCharSequence> wrap(Component component, int width) {
         int limit = Math.max(8, width);
         String raw = component.getString().replace("\r\n", "\n").replace('\r', '\n');
         List<FormattedCharSequence> lines = new ArrayList<>();
         for (String paragraph : breakBeforeLabels(raw).split("\n", -1)) {
             String text = paragraph.trim();
             if (!text.isEmpty()) pack(text, limit, lines);
         }
         if (lines.isEmpty()) lines.add(FormattedCharSequence.EMPTY);
         return lines;
     }
 
     private static String breakBeforeLabels(String raw) {
         String[] labels = {"属性：", "属性:", "主动：", "主动:", "被动：", "被动:", "Stats:", "Actives:", "Active:", "Passive:"};
         StringBuilder out = new StringBuilder(raw.length() + 8);
         int index = 0;
         while (index < raw.length()) {
             String label = labelAt(raw, index, labels);
             if (label != null) {
                 if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') out.append('\n');
                 out.append(label);
                 index += label.length();
             } else {
                 out.append(raw.charAt(index));
                 index++;
             }
         }
         return out.toString();
     }
 
     private static String labelAt(String raw, int index, String[] labels) {
         for (String label : labels) {
             if (raw.startsWith(label, index)) return label;
         }
         return null;
     }
 
     private void pack(String text, int width, List<FormattedCharSequence> lines) {
         List<String> tokens = tokens(text);
         int index = 0;
         while (index < tokens.size()) {
             StringBuilder line = new StringBuilder();
             int lastBreak = -1;
             int cursor = index;
             while (cursor < tokens.size()) {
                 String token = tokens.get(cursor);
                 if (line.length() == 0 && font.width(token) > width) {
                     hardSplit(token, width, lines);
                     cursor++;
                     break;
                 }
                 if (line.length() > 0 && font.width(line.toString() + token) > width) break;
                 line.append(token);
                 cursor++;
                 if (endsBreak(token)) lastBreak = cursor;
             }
             if (line.length() == 0) {
                 index = Math.max(cursor, index + 1);
                 continue;
             }
             if (cursor < tokens.size() && lastBreak > index && lastBreak < cursor) {
                 StringBuilder cut = new StringBuilder();
                 for (int token = index; token < lastBreak; token++) cut.append(tokens.get(token));
                 lines.add(visual(cut.toString().strip()));
                 index = lastBreak;
             } else {
                 lines.add(visual(line.toString().strip()));
                 index = cursor;
             }
         }
     }
 
     private void hardSplit(String token, int width, List<FormattedCharSequence> lines) {
         int start = 0;
         while (start < token.length()) {
             int end = start + 1;
             while (end < token.length() && font.width(token.substring(start, end + 1)) <= width) end++;
             lines.add(visual(token.substring(start, end)));
             start = end;
         }
     }
 
     private FormattedCharSequence visual(String text) {
         return Component.literal(text).getVisualOrderText();
     }
 
     private static boolean endsBreak(String token) {
         if (token.isEmpty() || " ".equals(token)) return false;
         char last = token.charAt(token.length() - 1);
         return "。！？；，、.?!;,".indexOf(last) >= 0;
     }
 
     private static List<String> tokens(String text) {
         List<String> out = new ArrayList<>();
         int index = 0;
         while (index < text.length()) {
             char current = text.charAt(index);
             if (current == ' ' || current == '\u3000') {
                 int next = index;
                 while (next < text.length() && (text.charAt(next) == ' ' || text.charAt(next) == '\u3000')) next++;
                 int unit = !out.isEmpty() && isNumber(out.get(out.size() - 1)) ? unitLength(text, next) : 0;
                 if (unit > 0) {
                     out.set(out.size() - 1, out.get(out.size() - 1) + text.substring(next, next + unit));
                     index = next + unit;
                 } else {
                     out.add(" ");
                     index++;
                 }
                 continue;
             }
             if (isNumberStart(text, index)) {
                 int end = numberEnd(text, index);
                 int spaced = end;
                 while (spaced < text.length() && (text.charAt(spaced) == ' ' || text.charAt(spaced) == '\u3000')) spaced++;
                 int unit = unitLength(text, spaced);
                 if (unit > 0) end = spaced + unit;
                 else {
                     int tight = unitLength(text, end);
                     if (tight > 0) end += tight;
                 }
                 out.add(text.substring(index, end));
                 index = end;
                 continue;
             }
             if (isCjk(current) || "。！？；，、：:".indexOf(current) >= 0) {
                 out.add(text.substring(index, index + 1));
                 index++;
                 continue;
             }
             int end = index + 1;
             while (end < text.length() && !isBoundary(text.charAt(end))) end++;
             out.add(text.substring(index, end));
             index = end;
         }
         return out;
     }
 
     private static boolean isBoundary(char current) {
         return current == ' ' || current == '\u3000' || isCjk(current) || "。！？；，、：:".indexOf(current) >= 0 || isDigit(current);
     }
 
     private static boolean isNumberStart(String text, int index) {
         char current = text.charAt(index);
         if (isDigit(current)) return true;
         return (current == '+' || current == '-' || current == '＋' || current == '－')
                 && index + 1 < text.length() && isDigit(text.charAt(index + 1));
     }
 
     private static int numberEnd(String text, int index) {
         int cursor = index;
         if (text.charAt(cursor) == '+' || text.charAt(cursor) == '-' || text.charAt(cursor) == '＋' || text.charAt(cursor) == '－') cursor++;
         while (cursor < text.length() && (isDigit(text.charAt(cursor)) || text.charAt(cursor) == '.' || text.charAt(cursor) == '．')) cursor++;
         if (cursor < text.length() && (text.charAt(cursor) == '%' || text.charAt(cursor) == '％')) cursor++;
         return cursor;
     }
 
     private static int unitLength(String text, int index) {
         if (index >= text.length()) return 0;
         if (text.startsWith("分钟", index)) return 2;
         char current = text.charAt(index);
         if ("秒点格次个层级".indexOf(current) >= 0) return 1;
         if ((current == 's' || current == 'S') && (index + 1 >= text.length() || !Character.isLetter(text.charAt(index + 1)))) return 1;
         return 0;
     }
 
     private static boolean isNumber(String token) {
         return !token.isEmpty() && isNumberStart(token, 0);
     }
 
     private static boolean isDigit(char current) {
         return current >= '0' && current <= '9';
     }
 
     private static boolean isCjk(char current) {
         Character.UnicodeScript script = Character.UnicodeScript.of(current);
         return script == Character.UnicodeScript.HAN || script == Character.UnicodeScript.HIRAGANA || script == Character.UnicodeScript.KATAKANA;
     }
 }
