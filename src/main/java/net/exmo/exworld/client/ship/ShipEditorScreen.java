package net.exmo.exworld.client.ship;

import net.exmo.exworld.network.ShipNetwork;
import net.exmo.exworld.ship.model.PartSelection;
import net.exmo.exworld.ship.model.ShipHull;
import net.exmo.exworld.ship.model.ShipOccupancy;
import net.exmo.exworld.ship.model.ShipPart;
import net.exmo.exworld.ship.model.ShipTemplate;
import net.exmo.exworld.ship.storage.ShipNbtCodec;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Visual 飞船模板 editor: 3D preview, part brush, allow-list variants. */
public final class ShipEditorScreen extends Screen {
    private static final int[] COLORS = {0xFF55AAFF, 0xFFFF8866, 0xFF88DD77, 0xFFEEDD55, 0xFFCC77FF, 0xFF66E0E0};
    private final List<String> knownIds;
    private final List<String> knownNames;
    private ShipTemplate template;
    private EditBox idBox, nameBox, partName, speedBox, variantBox;
    private int selectedPart;
    private int selectedVoxel = Integer.MIN_VALUE;
    private final Set<Integer> brush = new HashSet<>();
    private float yaw = 140, pitch = 25, zoom = 14;
    private boolean dragging;
    private Map<Integer, ShipPreviewRenderer.Pick> picks = Map.of();

    public ShipEditorScreen(String currentId, byte[] data, List<String> ids, List<String> names) {
        super(Component.translatable("screen.exworld.ship_editor"));
        this.knownIds = new ArrayList<>(ids);
        this.knownNames = new ArrayList<>(names);
        ShipTemplate decoded = data.length == 0 ? new ShipTemplate("new_ship", "新飞船", ShipHull.empty(), List.of(), PartSelection.empty())
                : ShipNbtCodec.decodeTemplate(data);
        this.template = decoded;
    }

    @Override
    protected void init() {
        int side = width - 220;
        idBox = box("id", side, 28, 190); idBox.setValue(template.id());
        nameBox = box("name", side, 52, 190); nameBox.setValue(template.name());
        partName = box("part", side, 120, 190);
        speedBox = box("speed", side, 144, 90); speedBox.setValue("0.4");
        variantBox = box("variant", side, 192, 190);
        loadPart();
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.ship_new_part"), b -> {
            collect();
            List<ShipPart> parts = new ArrayList<>(template.parts());
            parts.add(new ShipPart("part_" + parts.size(), "部位", COLORS[parts.size() % COLORS.length], new int[0], List.of(), 0.4));
            selectedPart = parts.size() - 1;
            template = template.withParts(parts);
            rebuild();
        }).bounds(side, height - 88, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.ship_brush"), b -> brush()).bounds(side + 96, height - 88, 94, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.ship_add_variant"), b -> addVariant()).bounds(side, height - 64, 190, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.ship_save"), b -> save()).bounds(side, height - 40, 60, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.ship_capture"), b ->
                ShipNetwork.action("CAPTURE", idBox.getValue(), 0))
                .bounds(side + 64, height - 40, 60, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.ship_lift"), b ->
                ShipNetwork.action("MATERIALIZE", idBox.getValue(), 0))
                .bounds(side + 128, height - 40, 62, 20).build());
        int y = 76;
        for (int i = 0; i < template.parts().size(); i++) {
            final int index = i;
            addRenderableWidget(Button.builder(Component.literal(template.parts().get(i).name()), b -> {
                collect(); selectedPart = index; loadPart();
            }).bounds(24, y, 110, 16).build());
            y += 18;
        }
    }

    private EditBox box(String hint, int x, int y, int w) {
        EditBox box = new EditBox(font, x, y, w, 18, Component.literal(hint));
        box.setHint(Component.literal(hint));
        addRenderableWidget(box);
        return box;
    }

    private void rebuild() { clearWidgets(); init(); }

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

    private void brush() {
        if (template.parts().isEmpty()) return;
        collect();
        Set<Integer> occupancy = new HashSet<>();
        for (int packed : template.parts().get(selectedPart).occupancy()) occupancy.add(packed);
        occupancy.addAll(brush);
        if (selectedVoxel != Integer.MIN_VALUE) occupancy.add(selectedVoxel);
        List<ShipPart> parts = new ArrayList<>(template.parts());
        parts.set(selectedPart, parts.get(selectedPart).withOccupancy(occupancy.stream().mapToInt(Integer::intValue).toArray()));
        template = template.withParts(parts);
        brush.clear();
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

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (mx < width - 230 && my > 24 && my < height - 50) {
            if (button == 0) {
                int packed = ShipPreviewRenderer.pick(picks, (int) mx, (int) my);
                if (packed != Integer.MIN_VALUE) {
                    selectedVoxel = packed;
                    brush.add(packed);
                    return true;
                }
                dragging = true;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) { yaw += dx; pitch = Math.max(-80, Math.min(80, pitch + (float) dy)); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        zoom = Math.max(6, Math.min(40, zoom + (float) sy));
        return true;
    }

    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER) { brush(); return true; }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mx, int my, float delta) {
        renderBackground(graphics, mx, my, delta);
        graphics.fill(0, 0, width - 220, height, 0xCC10141C);
        graphics.fill(width - 220, 0, width, height, 0xEE161B26);
        graphics.drawString(font, title, 24, 10, 0xFFFFD98A);
        graphics.drawString(font, Component.translatable("screen.exworld.ship_preview_hint"), 24, height - 18, 0xFF9FA6A8);
        picks = ShipPreviewRenderer.render(graphics, template.hull(), template.parts(), selectedVoxel, 20, 20, width - 260, height - 50, yaw, pitch, zoom);
        graphics.drawString(font, Component.translatable("screen.exworld.ship_parts"), 24, 60, 0xFFE6EAF0);
        graphics.drawString(font, Component.literal(template.hull().size() + " blocks"), width - 210, 10, 0xFFB8C3D1);
        super.render(graphics, mx, my, delta);
    }
}
