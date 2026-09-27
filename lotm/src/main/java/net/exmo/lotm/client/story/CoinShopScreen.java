package net.exmo.lotm.client.story;

import net.exmo.lotm.story.StoryPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CoinShopScreen extends Screen {
    private static final int PAPER = 0xFFF7F4EC;
    private static final int INK = 0xFF1C1C1E;

    public CoinShopScreen() {
        super(Component.translatable("item.lotm.coin"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new CoinShopScreen());
    }

    @Override
    protected void init() {
        int left = width / 2 - 94;
        int top = height / 2 - 48;
        addRenderableWidget(Button.builder(Component.translatable("item.lotm.coin.bread"), button -> buy("bread"))
                .bounds(left, top + 36, 188, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("item.lotm.coin.remedy"), button -> buy("remedy"))
                .bounds(left, top + 62, 188, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(left, top + 92, 188, 20).build());
    }

    private void buy(String offer) {
        PacketDistributor.sendToServer(new StoryPayloads.BuyPayload(offer));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = width / 2 - 110;
        int top = height / 2 - 70;
        graphics.fill(left, top, left + 220, top + 156, PAPER);
        graphics.drawString(font, title, left + 16, top + 14, INK, false);
        graphics.drawString(font, Component.translatable("item.lotm.coin.desc"), left + 16, top + 32, 0xFF8E8E93, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
