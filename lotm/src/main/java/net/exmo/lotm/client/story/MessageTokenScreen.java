package net.exmo.lotm.client.story;

import net.exmo.lotm.story.StoryPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MessageTokenScreen extends Screen {
    private static final int PAPER = 0xFFF7F4EC;
    private static final int INK = 0xFF1C1C1E;
    private final String saved;
    private EditBox box;

    public MessageTokenScreen(String saved) {
        super(Component.translatable("item.lotm.message_token"));
        this.saved = saved == null ? "" : saved;
    }

    public static void open(String text) {
        Minecraft.getInstance().setScreen(new MessageTokenScreen(text));
    }

    @Override
    protected void init() {
        int left = width / 2 - 94;
        int top = height / 2 - 70;
        box = new EditBox(font, left, top + 72, 188, 16, Component.translatable("item.lotm.message_token"));
        box.setMaxLength(10);
        addRenderableWidget(box);
        addRenderableWidget(Button.builder(Component.translatable("item.lotm.message_token.send"), button -> send())
                .bounds(left, top + 98, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(left + 98, top + 98, 90, 20).build());
        setInitialFocus(box);
    }

    private void send() {
        PacketDistributor.sendToServer(new StoryPayloads.SendLetterPayload(box.getValue()));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = width / 2 - 110;
        int top = height / 2 - 84;
        graphics.fill(left, top, left + 220, top + 168, PAPER);
        graphics.drawString(font, title, left + 16, top + 14, INK, false);
        String shown = saved.isBlank()
                ? Component.translatable("item.lotm.message_token.none").getString()
                : Component.translatable("item.lotm.message_token.past", saved).getString();
        graphics.drawString(font, font.plainSubstrByWidth(shown, 188), left + 16, top + 36, 0xFF8E8E93, false);
        graphics.drawString(font, Component.translatable("item.lotm.message_token.desc"), left + 16, top + 54, INK, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
