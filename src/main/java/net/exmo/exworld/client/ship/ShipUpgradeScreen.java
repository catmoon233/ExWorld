package net.exmo.exworld.client.ship;

import net.exmo.exworld.network.ShipNetwork;
import net.exmo.exworld.ship.model.ShipHull;
import net.exmo.exworld.ship.model.ShipOccupancy;
import net.exmo.exworld.ship.model.ShipPart;
import net.exmo.exworld.ship.model.ShipTemplate;
import net.exmo.exworld.ship.storage.ShipNbtCodec;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Player-facing 部位升级. Only variants whose occupancy matches the selected part are listed. */
public final class ShipUpgradeScreen extends Screen {
    private final int entityId;
    private final ShipTemplate template;
    private final Map<String, ShipHull> variants = new LinkedHashMap<>();
    private int selectedPart;
    private float yaw = 130, pitch = 20, zoom = 12;
    private boolean dragging;

    public ShipUpgradeScreen(int entityId, byte[] data, List<String> variantIds, List<byte[]> variantHulls) {
        super(Component.translatable("screen.exworld.ship_upgrade"));
        this.entityId = entityId;
        this.template = ShipNbtCodec.decodeTemplate(data);
        for (int i = 0; i < variantIds.size(); i++) {
            variants.put(variantIds.get(i), ShipNbtCodec.decodeHull(variantHulls.get(i)));
        }
    }

    @Override
    protected void init() {
        int y = 48;
        for (int i = 0; i < template.parts().size(); i++) {
            final int index = i;
            addRenderableWidget(Button.builder(Component.literal(template.parts().get(i).name()), b -> {
                selectedPart = index; clearWidgets(); init();
            }).bounds(24, y, 140, 18).build());
            y += 20;
        }
        if (template.parts().isEmpty()) return;
        ShipPart part = template.parts().get(Math.min(selectedPart, template.parts().size() - 1));
        int x = width - 200, vy = 48;
        for (var entry : variants.entrySet()) {
            if (!part.allows(entry.getKey())) continue;
            if (!ShipOccupancy.sameShape(part.occupancy(), entry.getValue().occupancy())) continue;
            String id = entry.getKey();
            addRenderableWidget(Button.builder(Component.literal(id), b ->
                    ShipNetwork.upgrade(entityId, part.id(), id))
                    .bounds(x, vy, 170, 18).build());
            vy += 20;
        }
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (mx > 170 && mx < width - 210) { dragging = true; return true; }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean mouseReleased(double mx, double my, int button) { dragging = false; return super.mouseReleased(mx, my, button); }

    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) { yaw += dx; pitch = Math.max(-80, Math.min(80, pitch + (float) dy)); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        zoom = Math.max(6, Math.min(40, zoom + (float) sy));
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mx, int my, float delta) {
        renderBackground(graphics, mx, my, delta);
        graphics.drawString(font, title, 24, 16, 0xFFFFD98A);
        graphics.drawString(font, Component.translatable("screen.exworld.ship_upgrade_hint"), 24, height - 18, 0xFF9FA6A8);
        List<ShipPart> parts = template.parts();
        ShipPreviewRenderer.render(graphics, template.hull(), parts, Integer.MIN_VALUE, 170, 30, width - 390, height - 60, yaw, pitch, zoom);
        super.render(graphics, mx, my, delta);
    }
}
