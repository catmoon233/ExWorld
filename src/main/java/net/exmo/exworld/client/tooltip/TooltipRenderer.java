package net.exmo.exworld.client.tooltip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2ic;

import java.util.ArrayList;
import java.util.List;

/** Orchestrates layout and drawing of the ExWorld item tooltip. */
public final class TooltipRenderer {
    private static final int NATIVE_GAP = 2;
    private static final int INACTIVE = 0xFF6A6A70;
    private static final int ACTIVE = 0xFF8FDF8A;
    private static final int TRANSITION_MS = 450;
    private static final int FADE_MS = 180;

    private static int lastBlendedAccent = RarityPalette.COMMON;
    private static int targetAccent = RarityPalette.COMMON;
    private static long accentChangeNanos = System.nanoTime();

    private TooltipRenderer() {}

    public static boolean tryRender(
            GuiGraphics graphics,
            Font font,
            List<ClientTooltipComponent> components,
            int mouseX,
            int mouseY,
            ClientTooltipPositioner positioner
    ) {
        ItemStack stack = TooltipCapture.stack();
        if (stack.isEmpty() || components == null || components.isEmpty()) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) return false;
        if (positioner == null) positioner = DefaultTooltipPositioner.INSTANCE;

        List<Component> lines = TooltipCapture.lines();
        if (lines.isEmpty()) {
            lines = Screen.getTooltipFromItem(minecraft, stack);
        }
        TooltipModel model = TooltipModelFactory.build(stack, lines, minecraft.player);
        List<ClientTooltipComponent> natives = natives(components);

        int target = model.qualityId().map(RarityPalette::qualityColor)
                .orElseGet(() -> RarityPalette.color(model.rarity()));
        if (target != targetAccent) {
            targetAccent = target;
            accentChangeNanos = System.nanoTime();
        }
        long changeElapsed = (System.nanoTime() - accentChangeNanos) / 1_000_000L;
        float transition = changeElapsed >= TRANSITION_MS
                ? 1f : easeOutCubic(changeElapsed / (float) TRANSITION_MS);
        int accent = RarityPalette.lerp(lastBlendedAccent, targetAccent, transition);
        lastBlendedAccent = accent;
        TooltipTheme theme = RarityPalette.themeOf(accent);

        long tooltipMs = TooltipCapture.tooltipTimeMs();
        float fade = tooltipMs < FADE_MS ? clamp(tooltipMs / (float) FADE_MS) : 1f;
        long now = System.currentTimeMillis();

        // ---- Width: title row keeps its own cap so name tags wrap instead of overflowing ----
        int titleW = font.width(model.title());
        int firstCap = Math.max(1, Math.min(TooltipLayout.MAX_TEXT, titleW) - TooltipLayout.TAG_GAP);
        int laterCap = TooltipLayout.MAX_TEXT - TooltipLayout.TAG_GAP;
        List<List<NameTag>> tagRows = NameTag.wrap(
                model.nameTags(),
                tag -> TooltipLayout.chipWidth(font.width(tag.label())),
                firstCap, laterCap, TooltipLayout.TAG_GAP);
        int row1Width = tagRows.isEmpty() ? 0 : NameTag.rowWidth(tagRows.getFirst(), font::width, TooltipLayout.CHIP_PAD_H, TooltipLayout.TAG_GAP);
        int headerW = TooltipLayout.headerContentWidth(titleW, row1Width, font.width(model.rarityLabel()));

        int width = Math.max(TooltipLayout.MIN_WIDTH, headerW);
        width = Math.max(width, suitWidth(model.suits(), font));
        width = Math.max(width, slotRowWidth(model.slots(), font));
        for (Component line : model.bodyLines()) width = Math.max(width, font.width(line));
        for (ClientTooltipComponent nativeComponent : natives) {
            width = Math.max(width, nativeComponent.getWidth(font));
        }
        width = Math.min(TooltipLayout.MAX_TEXT, width);
        List<List<Chip>> chipRows = TooltipLayout.wrapChips(model.chips(), font::width, width);
        width = Math.min(TooltipLayout.MAX_TEXT, Math.max(width, maxChipRowWidth(chipRows, font)));
        int panelW = width + TooltipLayout.PAD * 2;

        // ---- Height ----
        int headerH = TooltipLayout.headerHeight();
        int chipsH = TooltipLayout.chipBlockHeight(chipRows.size());
        int suitsH = suitHeight(model.suits());
        int bodyH = model.bodyLines().size() * (TooltipLayout.LINE + 1);
        int nativeH = 0;
        for (ClientTooltipComponent nativeComponent : natives) {
            nativeH += nativeComponent.getHeight() + NATIVE_GAP;
        }
        int contentH = 0;
        if (chipsH > 0) contentH += 4 + chipsH;
        if (model.slots().total() > 0) contentH += 4 + (TooltipLayout.LINE + 2)
                + (model.slots().names().isEmpty() ? 0 : TooltipLayout.LINE + 1);
        if (suitsH > 0) contentH += 6 + suitsH;
        if (bodyH > 0) contentH += 6 + bodyH;
        if (nativeH > 0) contentH += 4 + nativeH;

        int viewport = Math.min(contentH, TooltipLayout.MAX_BODY);
        int overflow = Math.max(0, contentH - viewport);
        TooltipCapture.setMaxScroll(overflow);
        int scroll = Math.min(TooltipCapture.scroll(), overflow);
        int panelH = TooltipLayout.PAD + headerH + (contentH == 0 ? 0 : 4 + viewport) + TooltipLayout.PAD;

        int screenW = minecraft.getWindow().getGuiScaledWidth();
        int screenH = minecraft.getWindow().getGuiScaledHeight();
        Vector2ic pos = positioner.positionTooltip(screenW, screenH, mouseX, mouseY, panelW, panelH);
        int x = pos.x();
        int y = Math.max(0, pos.y() - 14);
        if (x < 6) x = 6;
        if (y < 6) y = 6;
        if (x + panelW > screenW - 6) x = Math.max(6, screenW - 6 - panelW);
        if (y + panelH > screenH - 6) y = Math.max(6, screenH - 6 - panelH);

        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, 400.0f);

        TooltipPainter.drawGlow(graphics, x, y, panelW, panelH, accent, fade);
        TooltipPainter.drawPanel(graphics, x, y, panelW, panelH, theme, accent, fade, now);

        // ---- Header: slot + animated icon + title + tags + rarity ----
        int slotX = x + TooltipLayout.PAD;
        int slotY = y + TooltipLayout.PAD + (headerH - TooltipLayout.SLOT) / 2;
        int iconCenterX = slotX + TooltipLayout.SLOT / 2;
        int iconCenterY = slotY + TooltipLayout.SLOT / 2;
        TooltipPainter.drawAnimatedItem(graphics, stack, iconCenterX, iconCenterY,
                TooltipCapture.itemTimeMs(), isEquipment(stack));

        int contentLeft = x + TooltipLayout.PAD;
        int contentRight = x + panelW - TooltipLayout.PAD;
        int textX = contentLeft + TooltipLayout.TITLE_OFFSET;
        int titleY = slotY + 2;
        int rarityY = titleY + TooltipLayout.LINE + 2;
        int row2Y = model.rarityLabel().isEmpty() ? rarityY : rarityY + TooltipLayout.LINE + 2;

        if (titleW > width) {
            graphics.enableScissor(textX, titleY - 1, contentRight, titleY + TooltipLayout.LINE + 1);
            graphics.drawString(font, Component.literal(model.title().getString()), textX, titleY, theme.name(), true);
            graphics.disableScissor();
        } else {
            graphics.drawString(font, Component.literal(model.title().getString()), textX, titleY, theme.name(), true);
        }

        int tagX = textX + titleW + TooltipLayout.TAG_GAP;
        for (NameTag tag : tagRows.getFirst()) {
            int bg = tag.color() | 0xCC000000;
            int fg = RarityPalette.isCommon(tag.color()) ? RarityPalette.contrastText(tag.color()) : theme.badgeCutout();
            tagX = TooltipPainter.drawBadge(graphics, font, tag.label(), tagX, titleY, bg, fg, contentLeft, contentRight)
                    + TooltipLayout.TAG_GAP;
        }
        if (tagRows.size() > 1) {
            int rowX = textX;
            for (NameTag tag : tagRows.get(1)) {
                int bg = tag.color() | 0xCC000000;
                int fg = RarityPalette.isCommon(tag.color()) ? RarityPalette.contrastText(tag.color()) : theme.badgeCutout();
                rowX = TooltipPainter.drawBadge(graphics, font, tag.label(), rowX, row2Y, bg, fg, contentLeft, contentRight)
                        + TooltipLayout.TAG_GAP;
            }
        }
        if (!model.rarityLabel().isEmpty()) {
            graphics.drawString(font, model.rarityLabel(), textX, rarityY, accent, false);
        }

        // ---- Content: chips / suits / body / natives inside a scrolling viewport ----
        int contentTop = y + TooltipLayout.PAD + headerH + 4;
        int scissorBottom = contentTop + viewport;
        if (contentH > 0) {
            graphics.enableScissor(x + 2, contentTop, x + panelW - 2, scissorBottom);
            int cy = contentTop - scroll;
            if (!chipRows.isEmpty()) {
                for (List<Chip> row : chipRows) {
                    int cx = contentLeft;
                    for (Chip chip : row) {
                        int fg = RarityPalette.isCommon(chip.color()) ? RarityPalette.contrastText(chip.color()) : theme.badgeCutout();
                        cx = TooltipPainter.drawBadge(graphics, font, chip.label(), cx, cy,
                                chip.color() | 0xCC000000, fg, contentLeft, contentRight) + TooltipLayout.CHIP_GAP;
                    }
                    cy += TooltipLayout.LINE + TooltipLayout.ROW_GAP;
                }
                cy += 2;
            }
            if (model.slots().total() > 0) {
                TooltipPainter.drawSeparator(graphics, contentLeft, cy, width, accent, fade);
                cy += 4;
                ExModifierTooltip.SlotSection slots = model.slots();
                String slotHeader = translate("tooltip.exworld.section.slots") + " (" + slots.unlocked() + "/" + slots.total() + ")";
                TooltipPainter.drawSectionHeader(graphics, font, slotHeader, contentLeft, cy, theme.sectionHeader());
                cy += TooltipLayout.LINE + 2;
                int px = contentLeft + 4;
                long tooltipElapsed = TooltipCapture.tooltipTimeMs();
                for (int i = 0; i < slots.total(); i++) {
                    boolean filled = i < slots.unlocked();
                    TooltipPainter.drawAnimatedPip(graphics, px, cy + 1, 5,
                            filled ? accent : theme.badgeBg(),
                            filled ? RarityPalette.brighten(accent, 0.5f) : 0xFF3A3A46,
                            tooltipElapsed, i);
                    px += 7;
                }
                if (!slots.names().isEmpty()) {
                    TooltipPainter.drawBodyLine(graphics, font, String.join(" · ", slots.names()), px + 4, cy, 0xFFB9C0CC);
                    cy += TooltipLayout.LINE + 1;
                }
            }
            if (!model.suits().isEmpty()) {
                TooltipPainter.drawSeparator(graphics, contentLeft, cy, width, accent, fade);
                cy += 4;
                for (ExModifierTooltip.SuitSection section : model.suits()) {
                    String header = translate("tooltip.exworld.section.suit") + " " + section.name() + " (" + section.owned() + "/" + section.required() + ")";
                    TooltipPainter.drawSectionHeader(graphics, font, header, contentLeft, cy, theme.sectionHeader());
                    cy += TooltipLayout.LINE + 2;
                    for (ExModifierTooltip.SuitBonus bonus : section.bonuses()) {
                        String line = bonus.pieces() + "  " + bonus.text();
                        TooltipPainter.drawBodyLine(graphics, font, line, contentLeft + 4, cy,
                                bonus.active() ? ACTIVE : INACTIVE);
                        cy += TooltipLayout.LINE + 1;
                    }
                }
            }
            if (!model.bodyLines().isEmpty()) {
                TooltipPainter.drawSeparator(graphics, contentLeft, cy, width, accent, fade);
                cy += 4;
                for (Component line : model.bodyLines()) {
                    TooltipPainter.drawComponent(graphics, font, line, contentLeft, cy, 0xFFFFFFFF);
                    cy += TooltipLayout.LINE + 1;
                }
            }
            if (!natives.isEmpty()) {
                cy += 2;
                for (ClientTooltipComponent nativeComponent : natives) {
                    nativeComponent.renderText(font, contentLeft, cy, graphics.pose().last().pose(), graphics.bufferSource());
                    nativeComponent.renderImage(font, contentLeft, cy, graphics);
                    cy += nativeComponent.getHeight() + NATIVE_GAP;
                }
            }
            graphics.disableScissor();
        }

        TooltipPainter.drawFooterDots(graphics, x + panelW / 2, y + panelH - TooltipLayout.PAD / 2 - 1,
                theme, accent, fade, now);

        graphics.pose().popPose();
        TooltipCapture.markRendered();
        return true;
    }

    private static boolean isEquipment(ItemStack stack) {
        return stack != null && (stack.getItem() instanceof Equipable);
    }

    private static List<ClientTooltipComponent> natives(List<ClientTooltipComponent> components) {
        List<ClientTooltipComponent> natives = new ArrayList<>();
        for (ClientTooltipComponent component : components) {
            if (component instanceof ClientTextTooltip) continue;
            natives.add(component);
        }
        return natives;
    }

    private static int slotRowWidth(ExModifierTooltip.SlotSection slots, Font font) {
        if (slots == null || slots.total() <= 0) return 0;
        String header = translate("tooltip.exworld.section.slots") + " (" + slots.unlocked() + "/" + slots.total() + ")";
        int names = font.width(String.join(" · ", slots.names())) + slots.total() * 7 + 4;
        return Math.max(font.width("◆ " + header), names);
    }

    private static int maxChipRowWidth(List<List<Chip>> rows, Font font) {
        int max = 0;
        for (List<Chip> row : rows) {
            int w = 0;
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) w += TooltipLayout.CHIP_GAP;
                w += TooltipLayout.chipWidth(font.width(row.get(i).label()));
            }
            max = Math.max(max, w);
        }
        return max;
    }

    private static int suitWidth(List<ExModifierTooltip.SuitSection> suits, Font font) {
        int max = 0;
        for (ExModifierTooltip.SuitSection section : suits) {
            String header = translate("tooltip.exworld.section.suit") + " " + section.name() + " (" + section.owned() + "/" + section.required() + ")";
            max = Math.max(max, font.width("◆ " + header));
            for (ExModifierTooltip.SuitBonus bonus : section.bonuses()) {
                max = Math.max(max, font.width(bonus.pieces() + "  " + bonus.text()) + 4);
            }
        }
        return max;
    }

    private static int suitHeight(List<ExModifierTooltip.SuitSection> suits) {
        if (suits.isEmpty()) return 0;
        int h = 0;
        for (ExModifierTooltip.SuitSection section : suits) {
            h += TooltipLayout.LINE + 2;
            h += section.bonuses().size() * (TooltipLayout.LINE + 1);
        }
        return h;
    }

    private static float easeOutCubic(float t) {
        float x = clamp(t);
        return 1.0f - (1.0f - x) * (1.0f - x) * (1.0f - x);
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static String translate(String key) {
        try {
            if (I18n.exists(key)) return I18n.get(key);
        } catch (Throwable ignored) {
        }
        return key;
    }
}
