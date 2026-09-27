package net.exmo.exkeys.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.List;

final class PolicyList extends ContainerObjectSelectionList<PolicyList.Row> {
    private final KeyPolicyScreen screen;

    PolicyList(KeyPolicyScreen screen, Minecraft minecraft, int x, int y, int width, int height) {
        super(minecraft, width, height, y, 22);
        this.screen = screen;
        setX(x);
    }

    void setRows(List<RowModel> rows, double scroll) {
        clearEntries();
        for (RowModel row : rows) addEntry(new Row(row));
        setScrollAmount(scroll);
    }

    @Override
    public int getRowWidth() {
        return Math.max(120, getWidth() - 16);
    }

    sealed interface RowModel {
        record Category(String label) implements RowModel {}

        record Binding(String id, String name, String bound, boolean hide, boolean block, boolean opTrigger, boolean opDisplay) implements RowModel {}
    }

    final class Row extends ContainerObjectSelectionList.Entry<Row> {
        private final RowModel model;
        private int left;
        private int top;
        private int rowWidth;
        private int rowHeight;
        private final int[] chipX = new int[4];
        private final int[] chipW = new int[4];

        Row(RowModel model) {
            this.model = model;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            this.left = left;
            this.top = top;
            this.rowWidth = width;
            this.rowHeight = height;
            if (model instanceof RowModel.Category category) {
                graphics.fill(left, top + 2, left + width, top + height - 1, 0xC02A241C);
                graphics.drawString(minecraft.font, category.label(), left + 6, top + 7, 0xFFE4C98A, false);
                return;
            }
            RowModel.Binding binding = (RowModel.Binding) model;
            layoutChips();
            if (hovering) graphics.fill(left, top, left + width, top + height, 0x22FFFFFF);
            int textRight = chipX[0] - 6;
            int boundWidth = Math.min(72, textRight - left - 86);
            int nameMax = textRight - left - 10;
            if (boundWidth >= 28) {
                graphics.drawString(minecraft.font, trim(binding.bound(), boundWidth), textRight - boundWidth, top + 7, 0xFFB7AA96, false);
                nameMax = textRight - boundWidth - left - 12;
            }
            graphics.drawString(minecraft.font, trim(binding.name(), Math.max(24, nameMax)), left + 6, top + 7, 0xFFF4EFE6, false);
            drawChip(graphics, 0, top + 3, Component.translatable("exkeys.chip.hide").getString(), binding.hide(), 0xFF8D6230, mouseX, mouseY);
            drawChip(graphics, 1, top + 3, Component.translatable("exkeys.chip.block").getString(), binding.block(), 0xFF7A3E3E, mouseX, mouseY);
            drawChip(graphics, 2, top + 3, Component.translatable("exkeys.chip.op_trigger").getString(), binding.opTrigger(), 0xFF3E5C86, mouseX, mouseY);
            drawChip(graphics, 3, top + 3, Component.translatable("exkeys.chip.op_display").getString(), binding.opDisplay(), 0xFF3E6A4E, mouseX, mouseY);
        }
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !(model instanceof RowModel.Binding binding) || !screen.canEdit()) return false;
            if (mouseY < top || mouseY > top + rowHeight) return false;
            layoutChips();
            int chipY = top + 3;
            if (hit(mouseX, mouseY, 0, chipY)) {
                boolean hide = !binding.hide();
                screen.setRule(binding.id(), hide, binding.block(), binding.opTrigger(), hide ? false : binding.opDisplay());
                return true;
            }
            if (hit(mouseX, mouseY, 1, chipY)) {
                boolean block = !binding.block();
                screen.setRule(binding.id(), binding.hide(), block, block ? false : binding.opTrigger(), binding.opDisplay());
                return true;
            }
            if (hit(mouseX, mouseY, 2, chipY)) {
                boolean opTrigger = !binding.opTrigger();
                screen.setRule(binding.id(), binding.hide(), opTrigger ? false : binding.block(), opTrigger, binding.opDisplay());
                return true;
            }
            if (hit(mouseX, mouseY, 3, chipY)) {
                boolean opDisplay = !binding.opDisplay();
                screen.setRule(binding.id(), opDisplay ? false : binding.hide(), binding.block(), binding.opTrigger(), opDisplay);
                return true;
            }
            return false;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }

        private void layoutChips() {
            String hide = Component.translatable("exkeys.chip.hide").getString();
            String block = Component.translatable("exkeys.chip.block").getString();
            String opTrigger = Component.translatable("exkeys.chip.op_trigger").getString();
            String opDisplay = Component.translatable("exkeys.chip.op_display").getString();
            chipW[0] = chipWidth(hide);
            chipW[1] = chipWidth(block);
            chipW[2] = chipWidth(opTrigger);
            chipW[3] = chipWidth(opDisplay);
            int gap = 4;
            int cursor = left + rowWidth - 4;
            for (int i = 3; i >= 0; i--) {
                cursor -= chipW[i];
                chipX[i] = cursor;
                cursor -= gap;
            }
        }

        private void drawChip(GuiGraphics graphics, int index, int y, String label, boolean on, int onColor, int mouseX, int mouseY) {
            int x = chipX[index];
            int width = chipW[index];
            boolean enabled = screen.canEdit();
            boolean hover = enabled && hit(mouseX, mouseY, index, y);
            int fill = !enabled ? 0xFF2A2724 : on ? onColor : hover ? 0xFF3A342C : 0xFF241F1B;
            int border = !enabled ? 0xFF5C574F : on ? 0xFFF0D7A2 : 0xFF7A6E60;
            graphics.fill(x, y, x + width, y + 16, fill);
            graphics.fill(x, y, x + width, y + 1, border);
            graphics.fill(x, y + 15, x + width, y + 16, border);
            graphics.fill(x, y, x + 1, y + 16, border);
            graphics.fill(x + width - 1, y, x + width, y + 16, border);
            int textWidth = minecraft.font.width(label);
            int color = !enabled ? 0xFF8A847C : on ? 0xFFFFF6E8 : 0xFFD5CBBC;
            graphics.drawString(minecraft.font, label, x + Math.max(2, (width - textWidth) / 2), y + 4, color, false);
        }

        private boolean hit(double mouseX, double mouseY, int index, int y) {
            int x = chipX[index];
            int width = chipW[index];
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 16;
        }

        private int chipWidth(String label) {
            return Math.max(44, minecraft.font.width(label) + 10);
        }

        private String trim(String text, int maxWidth) {
            if (text == null) return "";
            if (maxWidth < 8) return "";
            if (minecraft.font.width(text) <= maxWidth) return text;
            String value = text;
            while (value.length() > 1 && minecraft.font.width(value + "…") > maxWidth) {
                value = value.substring(0, value.length() - 1);
            }
            return value + "…";
        }
    }
}
