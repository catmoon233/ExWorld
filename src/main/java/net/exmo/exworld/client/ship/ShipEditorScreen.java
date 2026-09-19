package net.exmo.exworld.client.ship;

import net.exmo.exworld.network.ShipNetwork;
import net.exmo.exworld.ship.model.PartSelection;
import net.exmo.exworld.ship.model.ShipHull;
import net.exmo.exworld.ship.model.ShipOccupancy;
import net.exmo.exworld.ship.model.ShipPart;
import net.exmo.exworld.ship.model.ShipTemplate;
import net.exmo.exworld.ship.storage.ShipNbtCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Visual 飞船模板 editor: 3D preview, click-to-paint part voxels, allow-list variants. */
public final class ShipEditorScreen extends Screen {
    private static final int[] COLORS = {0xFF55AAFF, 0xFFFF8866, 0xFF88DD77, 0xFFEEDD55, 0xFFCC77FF, 0xFF66E0E0};
    private final List<String> knownIds;
    private ShipTemplate template;
    private EditBox idBox, nameBox, partName, speedBox, variantBox;
    private int selectedPart;
    private int selectedVoxel = Integer.MIN_VALUE;
    private float yaw = 140, pitch = 25, zoom = 14;
    private boolean dragging;
    private Map<Integer, ShipPreviewRenderer.Pick> picks = Map.of();

    public ShipEditorScreen(String currentId, byte[] data, List<String> ids, List<String> names) {
        super(Component.translatable("screen.exworld.ship_editor"));
        this.knownIds = new ArrayList<>(ids);
        this.template = data.length == 0
                ? new ShipTemplate("new_ship", "新飞船", ShipHull.empty(), List.of(), PartSelection.empty())
                : ShipNbtCodec.decodeTemplate(data);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected void init() {
        int side = width - 230;
        idBox = box("screen.exworld.ship_id", side + 8, 84, 214);
        idBox.setValue(template.id());
        nameBox = box("screen.exworld.ship_name", side + 8, 104, 214);
        nameBox.setValue(template.name());
        partName = box("screen.exworld.ship_part_name", side + 8, height - 112, 100);
        speedBox = box("screen.exworld.ship_speed", side + 118, height - 112, 104);
        speedBox.setValue("0.4");
        variantBox = box("screen.exworld.ship_variants", side + 8, height - 90, 214);
        loadPart();

        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.ship_load_temple"), b ->
                        ShipNetwork.action("OPEN_EDITOR", "temple", 0))
                .bounds(side + 8, 126, 214, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.ship_new_part"), b -> newPart())
                .bounds(side + 8, height - 66, 68, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.ship_clear_part"), b -> clearPart())
                .bounds(side + 80, height - 66, 68, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.ship_add_variant"), b -> addVariant())
                .bounds(side + 152, height - 66, 70, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.ship_save"), b -> save())
                .bounds(side + 8, height - 42, 68, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.ship_capture"), b ->
                        ShipNetwork.action("CAPTURE", idBox.getValue(), 0))
                .bounds(side + 80, height - 42, 68, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.ship_lift"), b ->
                        ShipNetwork.action("MATERIALIZE", idBox.getValue(), 0))
                .bounds(side + 152, height - 42, 70, 18).build());

        int y = 172;
        for (int i = 0; i < template.parts().size(); i++) {
            if (y > height - 124) break;
            final int index = i;
            addRenderableWidget(Button.builder(partLabel(template.parts().get(i)), b -> {
                collect();
                selectedPart = index;
                loadPart();
            }).bounds(24, y, 150, 16).build());
            y += 18;
        }
    }

    private Component partLabel(ShipPart part) {
        Component swatch = Component.literal("■").withStyle(Style.EMPTY.withColor(part.color() & 0xFFFFFF));
        return swatch.copy().append(Component.literal(" " + part.name() + " · " + part.occupancy().length)
                .withStyle(ChatFormatting.WHITE));
    }

    private EditBox box(String hintKey, int x, int y, int w) {
        EditBox box = new EditBox(font, x, y, w, 16, Component.literal(""));
        box.setHint(Component.translatable(hintKey));
        addRenderableWidget(box);
        return box;
    }

    private void loadPart() {
        if (template.parts().isEmpty() || selectedPart >= template.parts().size()) return;
        ShipPart part = template.parts().get(selectedPart);
        partName.setValue(part.name());
        speedBox.setValue(Double.toString(part.speed()));
        variantBox.setValue(String.join(",", part.allowedVariants()));
    }

    private void collect() {
        List<ShipPart> parts = new ArrayList<>(template.parts());
        if (!parts.isEmpty() && selectedPart < parts.size()) {
            ShipPart part = parts.get(selectedPart);
            double speed = 0.4;
            try { speed = Double.parseDouble(speedBox.getValue()); } catch (Exception ignored) {}
            List<String> variants = new ArrayList<>();
            for (String bit : variantBox.getValue().split(",")) if (!bit.isBlank()) variants.add(bit.trim());
            parts.set(selectedPart, new ShipPart(part.id(), partName.getValue(), part.color(), part.occupancy(), variants, speed));
        }
        template = new ShipTemplate(idBox.getValue().isBlank() ? "new_ship" : idBox.getValue(), nameBox.getValue(),
                template.hull(), parts, template.selection());
    }

    private void newPart() {
        collect();
        List<ShipPart> parts = new ArrayList<>(template.parts());
        parts.add(new ShipPart("part_" + parts.size(), "部位 " + (parts.size() + 1), COLORS[parts.size() % COLORS.length],
                new int[0], List.of(), 0.4));
        selectedPart = parts.size() - 1;
        template = template.withParts(parts);
        rebuild();
    }

    private void clearPart() {
        if (template.parts().isEmpty()) return;
        collect();
        List<ShipPart> parts = new ArrayList<>(template.parts());
        parts.set(selectedPart, parts.get(selectedPart).withOccupancy(new int[0]));
        template = template.withParts(parts);
        rebuild();
    }

    private void toggleVoxel(int packed) {
        if (template.parts().isEmpty()) return;
        collect();
        List<ShipPart> parts = new ArrayList<>(template.parts());
        TreeSet<Integer> occupancy = new TreeSet<>();
        for (int voxel : parts.get(selectedPart).occupancy()) occupancy.add(voxel);
        if (!occupancy.add(packed)) occupancy.remove(packed);
        parts.set(selectedPart, parts.get(selectedPart).withOccupancy(ShipOccupancy.sorted(occupancy)));
        template = template.withParts(parts);
        selectedVoxel = packed;
        refreshParts();
    }

    private void removeVoxel(int packed) {
        if (template.parts().isEmpty()) return;
        collect();
        List<ShipPart> parts = new ArrayList<>(template.parts());
        TreeSet<Integer> occupancy = new TreeSet<>();
        for (int voxel : parts.get(selectedPart).occupancy()) occupancy.add(voxel);
        occupancy.remove(packed);
        parts.set(selectedPart, parts.get(selectedPart).withOccupancy(ShipOccupancy.sorted(occupancy)));
        template = template.withParts(parts);
        selectedVoxel = packed;
        refreshParts();
    }

    private void refreshParts() {
        for (var widget : List.copyOf(children())) {
            if (widget instanceof Button button && button.getY() >= 24 && button.getX() < width - 230) removeWidget(button);
        }
        int y = 172;
        for (int i = 0; i < template.parts().size(); i++) {
            if (y > height - 124) break;
            final int index = i;
            addRenderableWidget(Button.builder(partLabel(template.parts().get(i)), b -> {
                collect();
                selectedPart = index;
                loadPart();
            }).bounds(24, y, 150, 16).build());
            y += 18;
        }
    }

    private void addVariant() {
        collect();
        if (template.parts().isEmpty() || knownIds.isEmpty()) return;
        String candidate = knownIds.getFirst();
        for (String id : knownIds) if (!id.equals(template.id())) { candidate = id; break; }
        List<String> variants = new ArrayList<>(template.parts().get(selectedPart).allowedVariants());
        if (!variants.contains(candidate)) variants.add(candidate);
        List<ShipPart> parts = new ArrayList<>(template.parts());
        parts.set(selectedPart, parts.get(selectedPart).withAllowedVariants(variants));
        template = template.withParts(parts);
        variantBox.setValue(String.join(",", variants));
    }

    private void save() {
        collect();
        ShipNetwork.saveTemplate(ShipNbtCodec.encodeTemplate(template));
    }

    private void rebuild() { clearWidgets(); init(); }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (mx < width - 240 && my > 26 && my < height - 24) {
            int packed = ShipPreviewRenderer.pick(picks, (int) mx, (int) my);
            if (packed != Integer.MIN_VALUE) {
                if (button == 0) toggleVoxel(packed);
                else if (button == 1) removeVoxel(packed);
                return true;
            }
            if (button == 0) { dragging = true; return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) { yaw += dx; pitch = Math.max(-80, Math.min(80, pitch + (float) dy)); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        zoom = Math.max(6, Math.min(40, zoom + (float) sy));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_DELETE) { selectedVoxel = Integer.MIN_VALUE; return true; }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mx, int my, float delta) {
        renderBackground(graphics, mx, my, delta);
        int side = width - 230;
        graphics.fill(0, 0, side, height, 0xCC10141C);
        graphics.fill(side, 0, width, height, 0xEE161B26);
        graphics.drawString(font, title, 24, 8, 0xFFFFD98A);
        graphics.drawString(font, Component.translatable("screen.exworld.ship_hint_toggle"), 24, height - 14, 0xFF9FA6A8);

        // 步骤指引
        graphics.drawString(font, Component.translatable("screen.exworld.ship_steps"), side + 8, 26, 0xFFFFD98A);
        graphics.drawString(font, Component.translatable("screen.exworld.ship_step_1"), side + 8, 40, 0xFFB8C3D1);
        graphics.drawString(font, Component.translatable("screen.exworld.ship_step_2"), side + 8, 52, 0xFFB8C3D1);
        graphics.drawString(font, Component.translatable("screen.exworld.ship_step_3"), side + 8, 64, 0xFFB8C3D1);
        graphics.drawString(font, Component.literal(template.hull().size() + " blocks"), side + 8, 8, 0xFFB8C3D1);

        picks = ShipPreviewRenderer.render(graphics, template.hull(), template.parts(), selectedVoxel,
                20, 28, side - 40, height - 64, yaw, pitch, zoom);

        graphics.drawString(font, Component.translatable("screen.exworld.ship_parts"), 24, 144, 0xFFE6EAF0);
        if (!template.parts().isEmpty() && selectedPart < template.parts().size()) {
            ShipPart part = template.parts().get(selectedPart);
            graphics.fill(24, 156, 34, 166, part.color() | 0xFF000000);
            graphics.drawString(font, Component.literal(part.name() + " · " + part.occupancy().length + " 方块"),
                    40, 156, 0xFFE6EAF0);
        }
        super.render(graphics, mx, my, delta);
    }
}
