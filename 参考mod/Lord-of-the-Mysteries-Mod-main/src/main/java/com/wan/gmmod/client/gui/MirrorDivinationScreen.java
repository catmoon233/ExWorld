package com.wan.gmmod.client.gui;

import com.wan.gmmod.common.network.packet.MirrorDivinationPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 魔镜占卜界面（女巫 · 序列 7）。
 * <p>
 * 提供「占卜 / 反占卜 / 通灵」三种模式按钮，点击后发送
 * {@link MirrorDivinationPacket} 到服务端执行占卜并关闭界面。
 * 视觉复用灵摆占卜的结果逻辑，但作为魔镜更精美的入口。
 */
public class MirrorDivinationScreen extends Screen {
    private static final int BTN_W = 140;
    private static final int BTN_H = 20;
    private static final int GAP = 8;

    public MirrorDivinationScreen() {
        super(Component.translatable("gui.guimi_mod.mirror.title"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2 - BTN_W / 2;
        // 五个模式按钮垂直居中排布
        int rows = 5;
        int cy = this.height / 2 - (BTN_H * rows + GAP * (rows - 1)) / 2;

        Button divine = Button.builder(
                Component.translatable("gui.guimi_mod.mirror.mode.divine"),
                b -> select(0)).bounds(cx, cy, BTN_W, BTN_H).build();
        divine.setTooltip(Tooltip.create(Component.translatable("gui.guimi_mod.mirror.desc.divine")));
        addRenderableWidget(divine);

        Button counter = Button.builder(
                Component.translatable("gui.guimi_mod.mirror.mode.counter"),
                b -> select(1)).bounds(cx, cy + (BTN_H + GAP), BTN_W, BTN_H).build();
        counter.setTooltip(Tooltip.create(Component.translatable("gui.guimi_mod.mirror.desc.counter")));
        addRenderableWidget(counter);

        Button commune = Button.builder(
                Component.translatable("gui.guimi_mod.mirror.mode.commune"),
                b -> select(2)).bounds(cx, cy + (BTN_H + GAP) * 2, BTN_W, BTN_H).build();
        commune.setTooltip(Tooltip.create(Component.translatable("gui.guimi_mod.mirror.desc.commune")));
        addRenderableWidget(commune);

        Button barrier = Button.builder(
                Component.translatable("gui.guimi_mod.mirror.mode.barrier"),
                b -> select(3)).bounds(cx, cy + (BTN_H + GAP) * 3, BTN_W, BTN_H).build();
        barrier.setTooltip(Tooltip.create(Component.translatable("gui.guimi_mod.mirror.desc.barrier")));
        addRenderableWidget(barrier);

        Button trap = Button.builder(
                Component.translatable("gui.guimi_mod.mirror.mode.trap"),
                b -> select(4)).bounds(cx, cy + (BTN_H + GAP) * 4, BTN_W, BTN_H).build();
        trap.setTooltip(Tooltip.create(Component.translatable("gui.guimi_mod.mirror.desc.trap")));
        addRenderableWidget(trap);
    }

    private void select(int mode) {
        PacketDistributor.sendToServer(new MirrorDivinationPacket(mode));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2,
                this.height / 2 - (BTN_H * 5 + GAP * 4) / 2 - 24, 0xFFD0F0FF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
