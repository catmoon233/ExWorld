package net.exmo.exworld.client.npc;

import net.exmo.exworld.npc.network.NpcPayloads;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Modern dialog panel: slide-in, typewriter, fading choices and a configurable background. */
public final class UrbanDialogScreen extends Screen {
    private int entityId;
    private String dialogId = "";
    private String npcName = "";
    private String fullText = "";
    private boolean thinking;
    private int argb = 0xFF101418;
    private float alpha = 0.86f;
    private String texture = "";
    private int speed = 1;
    private final List<String[]> buttons = new ArrayList<>();
    private int openTicks;
    private int shown;

    public UrbanDialogScreen(CompoundTag tag) {
        super(Component.translatable("npc.exworld.dialog"));
        update(tag);
    }

    public boolean same(int entityId) { return this.entityId == entityId; }

    public void update(CompoundTag tag) {
        int previous = entityId;
        String previousDialog = dialogId;
        entityId = tag.getInt("entity");
        dialogId = tag.getString("dialog");
        npcName = tag.getString("name");
        fullText = tag.getString("text");
        thinking = tag.getBoolean("thinking");
        argb = tag.getInt("argb");
        alpha = tag.contains("alpha") ? tag.getFloat("alpha") : 0.86f;
        texture = tag.getString("texture");
        speed = Math.max(1, tag.getInt("speed"));
        buttons.clear();
        for (String line : tag.getString("buttons").split("\n")) {
            if (line.isBlank()) continue;
            String[] parts = line.split("\t", -1);
            buttons.add(new String[] { parts.length > 0 ? parts[0] : "", parts.length > 1 ? parts[1] : "", parts.length > 2 ? parts[2] : "" });
        }
        if (previous != entityId || !previousDialog.equals(dialogId)) shown = 0;
        if (thinking) shown = 0;
        rebuildChoices();
    }

    @Override
    protected void init() { rebuildChoices(); }

    private void rebuildChoices() {
        clearWidgets();
        if (thinking || shown < fullText.length()) return;
        int[] box = sheet();
        int x = box[0];
        int y = box[1] + box[3] - 36;
        int i = 0;
        for (String[] button : buttons) {
            int index = i++;
            int bw = Math.min(160, box[2] - 72);
            int bx = x + 48 + (index % 2) * (bw + 8);
            int by = y - 8 - ((buttons.size() - 1) / 2 - index / 2) * 28;
            OreButton.Kind kind = index == 0 ? OreButton.Kind.PRIMARY : OreButton.Kind.SECONDARY;
            addRenderableWidget(OreButton.of(bx, by, bw, 24, Component.literal(button[0].isBlank() ? "…" : button[0]), pressed -> choose(index), kind));
        }
        addRenderableWidget(OreButton.of(x + box[2] - 72, box[1] + 10, 56, 24, Component.translatable("npc.exworld.close"), pressed -> onClose(), OreButton.Kind.DANGER));
    }

    private void choose(int index) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("entity", entityId);
        tag.putString("dialog", dialogId);
        tag.putInt("button", index);
        PacketDistributor.sendToServer(new NpcPayloads.Server("dialog_button", tag));
    }

    @Override
    public void tick() {
        openTicks++;
        if (!thinking) shown++;
        if (!thinking && shown == fullText.length()) rebuildChoices();
    }

    @Override
    public void onClose() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("entity", entityId);
        PacketDistributor.sendToServer(new NpcPayloads.Server("dialog_close", tag));
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, OreChrome.OVERLAY);
        int[] box = sheet();
        float slide = Math.min(1f, (openTicks + partialTick) / 8f);
        slide = 1f - (1f - slide) * (1f - slide);
        int x = box[0];
        int y = box[1] + (int) ((1f - slide) * 28);
        int panelW = box[2];
        int panelH = box[3];
        graphics.fill(x, y, x + panelW, y + panelH, OreChrome.CANVAS);
        graphics.fill(x, y, x + panelW, y + 44, OreChrome.SURFACE);
        graphics.fill(x, y + 43, x + panelW, y + 44, OreChrome.EDGE);
        graphics.fill(x + 14, y + 8, x + 42, y + 36, OreChrome.GREEN);
        String mark = npcName == null || npcName.isBlank() ? "N" : npcName.substring(0, 1);
        graphics.drawCenteredString(font, mark, x + 28, y + 14, OreChrome.WHITE);
        graphics.drawString(font, npcName, x + 50, y + 16, OreChrome.INK, false);
        String body = thinking ? thinkingText() : fullText.substring(0, Math.min(fullText.length(), Math.max(0, shown / speed)));
        int bubbleW = Math.max(80, panelW - 96);
        int lines = Math.max(1, font.split(Component.literal(body), bubbleW - 16).size());
        int bubbleH = Math.min(panelH - 132, 16 + lines * 10);
        OreChrome.bubble(graphics, x + 50, y + 56, bubbleW, bubbleH, OreChrome.SURFACE);
        graphics.drawWordWrap(font, Component.literal(body), x + 58, y + 62, bubbleW - 16, OreChrome.INK);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int[] sheet() {
        int panelW = Math.min(460, Math.max(280, width - 24));
        int panelH = Math.min(420, Math.max(220, height - 24));
        return new int[] { (width - panelW) / 2, (height - panelH) / 2, panelW, panelH };
    }

    private String thinkingText() {
        int dots = (openTicks / 8) % 4;
        return "……" + ".".repeat(dots);
    }

    private static int withAlpha(int argb, float alpha) {
        int a = Math.round(Math.max(0.15f, Math.min(1f, alpha)) * ((argb >>> 24) == 0 ? 255 : ((argb >>> 24) & 0xFF)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }
}
