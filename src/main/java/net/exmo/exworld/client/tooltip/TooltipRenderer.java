package net.exmo.exworld.client.tooltip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2ic;

import java.util.ArrayList;
import java.util.List;

/** Orchestrates layout and drawing of the ExWorld item tooltip. */
public final class TooltipRenderer {
    private static final int NATIVE_GAP = 2;
    private static final int INACTIVE = 0xFF6A6A70;

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

        List<Component> lines = TooltipCapture.lines();
        if (lines.isEmpty()) {
            lines = Screen.getTooltipFromItem(minecraft, stack);
        }
        TooltipModel model = TooltipModelFactory.build(stack, lines, minecraft.player);
        List<ClientTooltipComponent> natives = natives(components);
        TooltipTheme theme = RarityPalette.theme(model.rarity(), model.qualityId());

        int tagWidth = NameTag.rowWidth(model.nameTags(), font::width, TooltipLayout.CHIP_PAD_H, TooltipLayout.TAG_GAP);
        int headerW = TooltipLayout.headerContentWidth(font.width(model.title()), tagWidth, font.width(model.rarityLabel()));
        int width = Math.max(TooltipLayout.MIN_WIDTH, headerW);
        width = Math.max(width, suitWidth(model.suits(), font));
        for (String line : model.bodyLines()) width = Math.max(width, font.width(line));
        for (ClientTooltipComponent nativeComponent : natives) {
            width = Math.max(width, nativeComponent.getWidth(font));
        }
        width = Math.min(TooltipLayout.MAX_TEXT, width);
        List<List<Chip>> chipRows = TooltipLayout.wrapChips(model.chips(), font::width, width);
        width = Math.min(TooltipLayout.MAX_TEXT, Math.max(width, maxChipRowWidth(chipRows, font)));
        int panelW = width + TooltipLayout.PAD * 2;

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
        int y = pos.y();
        if (x < 4) x = 4;
        if (y < 4) y = 4;
        if (x + panelW > screenW - 4) x = Math.max(4, screenW - 4 - panelW);
        if (y + panelH > screenH - 4) y = Math.max(4, screenH - 4 - panelH);

        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, 400.0f);
        TooltipPainter.drawPanel(graphics, x, y, panelW, panelH, theme, System.currentTimeMillis());

        int slotX = x + TooltipLayout.PAD;
        int slotY = y + TooltipLayout.PAD;
        TooltipPainter.drawSlot(graphics, slotX, slotY, theme);
        TooltipPainter.drawItemIcon(graphics, stack, slotX, slotY);

        int textX = x + TooltipLayout.PAD + TooltipLayout.TITLE_OFFSET;
        int titleY = slotY + 1;
        graphics.drawString(font, model.title(), textX, titleY, theme.name(), true);
        int tagX = textX + font.width(model.title()) + TooltipLayout.TAG_GAP;
        for (NameTag tag : model.nameTags()) {
            tagX = TooltipPainter.drawBadge(graphics, font, tag.label(), tagX, titleY, tag.color() & 0x00FFFFFF | 0xCC000000, theme.badgeCutout()) + TooltipLayout.TAG_GAP;
        }
        graphics.drawString(font, model.rarityLabel(), textX, titleY + TooltipLayout.LINE + 2, RarityPalette.color(model.rarity()), false);

        int contentTop = y + TooltipLayout.PAD + headerH + 4;
        int scissorBottom = contentTop + viewport;
        if (contentH > 0) {
            graphics.enableScissor(x + 2, contentTop, x + panelW - 2, scissorBottom);
            int cy = contentTop - scroll;
            if (!chipRows.isEmpty()) {
                for (List<Chip> row : chipRows) {
                    int cx = x + TooltipLayout.PAD;
                    for (Chip chip : row) {
                        cx = TooltipPainter.drawBadge(graphics, font, chip.label(), cx, cy, chip.color() & 0x00FFFFFF | 0xCC000000, theme.badgeCutout()) + TooltipLayout.CHIP_GAP;
                    }
                    cy += TooltipLayout.LINE + TooltipLayout.ROW_GAP;
                }
                cy += 2;
            }
            if (!model.suits().isEmpty()) {
                TooltipPainter.drawSeparator(graphics, x + TooltipLayout.PAD, cy, width, theme.separator());
                cy += 4;
                for (ExModifierTooltip.SuitSection section : model.suits()) {
                    String header = translate("tooltip.exworld.section.suit") + " " + section.name() + " (" + section.owned() + "/" + section.required() + ")";
                    TooltipPainter.drawSectionHeader(graphics, font, header, x + TooltipLayout.PAD, cy, theme.sectionHeader());
                    cy += TooltipLayout.LINE + 2;
                    for (ExModifierTooltip.SuitBonus bonus : section.bonuses()) {
                        String line = bonus.pieces() + "  " + bonus.text();
                        int color = bonus.active() ? theme.body() : INACTIVE;
                        TooltipPainter.drawBodyLine(graphics, font, line, x + TooltipLayout.PAD + 4, cy, color);
                        cy += TooltipLayout.LINE + 1;
                    }
                }
            }
            if (!model.bodyLines().isEmpty()) {
                TooltipPainter.drawSeparator(graphics, x + TooltipLayout.PAD, cy, width, theme.separator());
                cy += 4;
                for (String line : model.bodyLines()) {
                    TooltipPainter.drawBodyLine(graphics, font, line, x + TooltipLayout.PAD, cy, theme.body());
                    cy += TooltipLayout.LINE + 1;
                }
            }
            if (!natives.isEmpty()) {
                cy += 2;
                for (ClientTooltipComponent nativeComponent : natives) {
                    nativeComponent.renderText(font, x + TooltipLayout.PAD, cy, graphics.pose().last().pose(), graphics.bufferSource());
                    nativeComponent.renderImage(font, x + TooltipLayout.PAD, cy, graphics);
                    cy += nativeComponent.getHeight() + NATIVE_GAP;
                }
            }
            graphics.disableScissor();
        }
        graphics.pose().popPose();
        TooltipCapture.markRendered();
        return true;
    }

    private static List<ClientTooltipComponent> natives(List<ClientTooltipComponent> components) {
        List<ClientTooltipComponent> natives = new ArrayList<>();
        for (ClientTooltipComponent component : components) {
            if (component instanceof ClientTextTooltip) continue;
            natives.add(component);
        }
        return natives;
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

    private static String translate(String key) {
        try {
            if (I18n.exists(key)) return I18n.get(key);
        } catch (Throwable ignored) {
        }
        return key;
    }
}
