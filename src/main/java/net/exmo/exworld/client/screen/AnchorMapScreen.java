package net.exmo.exworld.client.screen;

import net.exmo.exworld.network.AnchorActionPayload;
import net.exmo.exworld.world.model.AnchorSnapshot;
import net.exmo.exworld.world.model.TravelAnchor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AnchorMapScreen extends Screen {
    private final AnchorSnapshot snapshot;
    private TravelAnchor selected;

    public AnchorMapScreen(AnchorSnapshot snapshot) {
        super(Component.translatable(snapshot.respawnMode() ? "screen.exworld.respawn_anchor" : "screen.exworld.anchor_map"));
        this.snapshot = snapshot;
        this.selected = snapshot.anchors().stream().filter(a -> a.id().equals(snapshot.currentAnchorId())).findFirst()
                .orElse(snapshot.anchors().isEmpty() ? null : snapshot.anchors().getFirst());
    }

    @Override protected void init() {
        int y = height - 42;
        Button action = addRenderableWidget(Button.builder(Component.translatable(snapshot.respawnMode()
                        ? selected == null ? "screen.exworld.respawn_world_spawn" : "screen.exworld.respawn_here"
                        : "screen.exworld.teleport_here"), button -> act())
                .bounds(width / 2 - 80, y, 160, 20).build());
        action.active = snapshot.respawnMode() || selected != null;
    }

    private void act() {
        if (selected == null && !snapshot.respawnMode()) return;
        PacketDistributor.sendToServer(new AnchorActionPayload(selected == null ? "" : selected.id(), snapshot.respawnMode()
                ? AnchorActionPayload.Action.SELECT_RESPAWN : AnchorActionPayload.Action.TELEPORT));
        if (snapshot.respawnMode() && minecraft != null && minecraft.player != null) minecraft.player.respawn();
        onClose();
    }

    @Override
    public void renderBackground(GuiGraphics p_283688_, int p_296369_, int p_296477_, float p_294317_) {
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = width / 2 - 170;
        int top = 34;
        graphics.fill(left - 12, top - 18, left + 352, height - 12, 0xEEEEE0BC);
        graphics.drawCenteredString(font, title, width / 2, top - 10, 0xFF332A23);
        for (int i = 0; i < snapshot.anchors().size(); i++) {
            TravelAnchor anchor = snapshot.anchors().get(i);
            int y = top + i * 24;
            boolean active = selected != null && selected.id().equals(anchor.id());
            graphics.fill(left, y, left + 220, y + 20, active ? 0xFFB85C38 : 0x556A6258);
            graphics.drawString(font, "◆ " + anchor.name(), left + 8, y + 6, active ? 0xFFFFFFFF : 0xFF332A23, false);
        }
        if (selected != null) {
            int x = left + 236;
            graphics.drawString(font, selected.name(), x, top + 4, 0xFF332A23, false);
            graphics.drawString(font, "世界格：" + selected.tileId(), x, top + 24, 0xFF665748, false);
            graphics.drawString(font, "位置：" + selected.pos().toShortString(), x, top + 40, 0xFF665748, false);
        }
        if (!snapshot.respawnMode() && snapshot.cooldownRemainingMs() > 0) {
            graphics.drawCenteredString(font, "传送冷却：" + (snapshot.cooldownRemainingMs() + 999) / 1000 + " 秒", width / 2, height - 64, 0xFF9A4C36);
        }
        if (snapshot.respawnMode() && snapshot.anchors().isEmpty()) {
            graphics.drawCenteredString(font, "没有已绑定的传送锚点，将在世界出生点复活", width / 2, height / 2, 0xFF665748);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = width / 2 - 170;
        int top = 34;
        for (int i = 0; i < snapshot.anchors().size(); i++) {
            int y = top + i * 24;
            if (mouseX >= left && mouseX <= left + 220 && mouseY >= y && mouseY <= y + 20) {
                selected = snapshot.anchors().get(i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean shouldCloseOnEsc() { return !snapshot.respawnMode(); }
    @Override public boolean isPauseScreen() { return false; }
}
