package net.exmo.exworld.client.quest;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Keeps the journal's generated PNGs at one known logical size. Scaling happens through the
 * pose stack, so a destination size never accidentally becomes a source-rectangle size.
 */
final class JournalGuiTextures {
    static final ResourceLocation BOOK = ResourceLocation.fromNamespaceAndPath("exworld", "textures/gui/quest_journal_book.png");
    static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath("exworld", "textures/gui/quest_journal_panel.png");
    static final int BOOK_WIDTH = 512;
    static final int BOOK_HEIGHT = 320;
    static final int PANEL_WIDTH = 256;
    static final int PANEL_HEIGHT = 64;

    private JournalGuiTextures() {}

    static void book(GuiGraphics graphics, int x, int y, int width, int height) {
        blitScaled(graphics, BOOK, x, y, width, height, BOOK_WIDTH, BOOK_HEIGHT);
    }

    static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        blitScaled(graphics, PANEL, x, y, width, height, PANEL_WIDTH, PANEL_HEIGHT);
    }

    private static void blitScaled(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height,
                                   int sourceWidth, int sourceHeight) {
        if (width <= 0 || height <= 0) return;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        pose.scale(width / (float) sourceWidth, height / (float) sourceHeight, 1.0F);
        graphics.blit(texture, 0, 0, 0, 0, sourceWidth, sourceHeight, sourceWidth, sourceHeight);
        pose.popPose();
    }
}
