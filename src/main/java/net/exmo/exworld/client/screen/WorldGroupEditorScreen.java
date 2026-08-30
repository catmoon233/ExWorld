package net.exmo.exworld.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.exmo.exworld.network.SaveWorldGroupEditPayload;
import net.exmo.exworld.network.WorldGroupEditorPayload;
import net.exmo.exworld.world.model.ManualChunkGroupLayout;
import net.exmo.exworld.world.model.MapRegion;
import net.exmo.exworld.world.model.MapTile;
import net.exmo.exworld.world.model.WorldSnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Collaborative manual group editor. It owns a local draft only; Save sends one complete partition back to the
 * server, where {@link ManualChunkGroupLayout} remains the source of truth for validation and persistence.
 *
 * <p>Layout: a biome atlas on the left shows the world's rough appearance, tinted by group colour and outlined by
 * group boundaries; a sidebar on the right lists every group and exposes the selected group's fields directly
 * (no hidden tabs).</p>
 */
public final class WorldGroupEditorScreen extends Screen {
    private static final int GOLD = 0xFFD6B56A;
    private static final int IVORY = 0xFFF0E7D1;
    private static final int MUTED = 0xFF9FA6A8;
    private static final int HOVER = 0xFF6FDFE5;
    private static final int DANGER = 0xFFFF786B;

    private static final List<String> ICONS = List.of("", "*", "+", "#", "!", "@", "$", "^", "~", "X", "?");
    private static final int ICON_COLS = 8;
    private static final int ICON_W = 24;
    private static final int ICON_H = 20;
    private static final int ICON_GAP = 2;
    private static final int ROW_HEIGHT = 20;

    private static final int NORTH = 1;
    private static final int EAST = 1 << 1;
    private static final int SOUTH = 1 << 2;
    private static final int WEST = 1 << 3;

    private final WorldSnapshot snapshot;
    private final String serverError;
    private final Map<String, MapTile> tilesById = new LinkedHashMap<>();
    private final Map<Long, MapTile> tilesByCoordinate = new LinkedHashMap<>();
    private final Map<String, DraftGroup> groups = new LinkedHashMap<>();
    private final Map<String, String> tileOwners = new LinkedHashMap<>();
    private final Set<String> selectedTiles = new LinkedHashSet<>();

    private boolean manualGroups;
    private String activeGroupId;
    private EditBox groupName;
    private EditBox groupSite;
    private EditBox groupResources;
    private BiomeAtlasTexture biomeAtlas;
    private GroupEditorAtlasTexture groupAtlas;
    private double zoom = 1.0;
    private double viewCenterX;
    private double viewCenterZ;
    private int nextGroupNumber = 1;
    private int listScroll = 0;

    public WorldGroupEditorScreen(WorldGroupEditorPayload payload) {
        super(Component.literal("区域组编辑"));
        this.snapshot = payload.snapshot();
        this.serverError = payload.error();
        this.manualGroups = snapshot.manualGroups();
        snapshot.tiles().forEach(tile -> {
            tilesById.put(tile.id(), tile);
            tilesByCoordinate.put(key(tile.mapX(), tile.mapZ()), tile);
        });
        initialiseDraft();
        MapTile current = tilesById.get(snapshot.currentTileId());
        this.viewCenterX = current == null ? 0 : current.mapX();
        this.viewCenterZ = current == null ? 0 : current.mapZ();
    }

    private void initialiseDraft() {
        Map<String, MapRegion> info = new LinkedHashMap<>();
        snapshot.regions().forEach(region -> info.put(region.id(), region));
        for (MapTile tile : snapshot.tiles()) {
            MapRegion region = info.getOrDefault(tile.regionId(), new MapRegion(tile.regionId(), tile.regionId(), ""));
            groups.computeIfAbsent(region.id(), id -> new DraftGroup(id, region.name(), region.icon(), region.site(),
                    region.resources(), region.configured())).tileIds.add(tile.id());
        }
        rebuildOwners();
        activeGroupId = groups.keySet().stream().findFirst().orElse(null);
    }

    @Override
    protected void init() {
        Layout layout = layout();
        Sidebar sb = layout.sidebar;
        if (biomeAtlas == null) biomeAtlas = new BiomeAtlasTexture(snapshot);
        if (groupAtlas == null) refreshAtlas();

        DraftGroup group = active().orElse(null);
        int x = sb.x + 8;
        int w = sb.width - 16;
        int labelW = 34;

        groupName = new EditBox(font, x + labelW, sb.detailY + 14, w - labelW - 4, 20, Component.literal("名称"));
        groupName.setMaxLength(48);
        groupName.setResponder(value -> active().ifPresent(g -> g.name = value));
        groupName.setValue(group == null ? "" : group.name);
        groupName.setEditable(group != null && manualGroups);
        addRenderableWidget(groupName);

        groupSite = new EditBox(font, x + labelW, sb.detailY + 100, w - labelW - 4, 20, Component.literal("据点"));
        groupSite.setMaxLength(96);
        groupSite.setResponder(value -> active().ifPresent(g -> g.site = value));
        groupSite.setValue(group == null ? "" : group.site);
        groupSite.setEditable(group != null && manualGroups);
        addRenderableWidget(groupSite);

        groupResources = new EditBox(font, x + labelW, sb.detailY + 124, w - labelW - 4, 20, Component.literal("资源"));
        groupResources.setMaxLength(160);
        groupResources.setResponder(value -> active().ifPresent(g -> g.resources = value));
        groupResources.setValue(group == null ? "" : group.resources);
        groupResources.setEditable(group != null && manualGroups);
        addRenderableWidget(groupResources);

        int saveW = Math.max(100, sb.width / 2 - 10);
        addRenderableWidget(Button.builder(Component.literal("保存并应用"), b -> save())
                .bounds(sb.x + 8, sb.saveY, saveW, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(sb.x + 8 + saveW + 8, sb.saveY, sb.width - 16 - saveW - 8, 20).build());
    }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        graphics.fill(0, 0, width, height, 0xFF080C10);
        graphics.fill(8, 36, layout.mapRight(), height - 14, 0xFF0F171B);
        graphics.fill(layout.sidebar.x - 6, 36, width - 8, height - 14, 0xFF131C21);
        graphics.drawString(font, title, 12, 12, IVORY, false);
        graphics.drawString(font, manualGroups ? "手动模式：未归组世界格会在保存时变为单格组。"
                        : "自动模式：保存时按群系斑块重新生成分组（详情不可编辑）。", 12, 24,
                manualGroups ? MUTED : 0xFFFF986E, false);

        MapTile hovered = tileAt(mouseX, mouseY, layout);
        renderMap(graphics, layout, hovered);
        renderSidebar(graphics, layout, mouseX, mouseY);
        renderFooterHint(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (hovered != null) renderTileTooltip(graphics, hovered, mouseX, mouseY);
    }

    private void renderMap(GuiGraphics graphics, Layout layout, MapTile hovered) {
        double cell = cellSize(layout);
        graphics.enableScissor(layout.mapLeft, layout.mapTop, layout.mapWidth, layout.mapHeight);
        TileRect atlas = atlasRect(layout, cell);
        if (biomeAtlas != null) {
            graphics.blit(biomeAtlas.location(), atlas.x(), atlas.y(), 0, 0, atlas.width(), atlas.height(),
                    MapViewport.atlasTextureExtent(atlas.width()), MapViewport.atlasTextureExtent(atlas.height()));
        }
        if (groupAtlas != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.blit(groupAtlas.location(), atlas.x(), atlas.y(), 0, 0, atlas.width(), atlas.height(),
                    MapViewport.atlasTextureExtent(atlas.width()), MapViewport.atlasTextureExtent(atlas.height()));
        }
        if (cell >= 5.0) renderGroupOutlines(graphics, layout, cell);
        if (cell >= 2.0) renderSelection(graphics, layout, cell, hovered);
        if (cell >= 6.0) renderGroupIcons(graphics, layout, cell);
        graphics.disableScissor();
        outline(graphics, new TileRect(layout.mapLeft, layout.mapTop, layout.mapWidth, layout.mapHeight), 1, 0xFF5E676B);
    }

    private void renderGroupOutlines(GuiGraphics graphics, Layout layout, double cell) {
        int thickness = cell >= 10.0 ? 2 : 1;
        VisibleRange range = visibleRange(layout, cell);
        for (int mapZ = range.minZ; mapZ <= range.maxZ; mapZ++) {
            for (int mapX = range.minX; mapX <= range.maxX; mapX++) {
                MapTile tile = tilesByCoordinate.get(key(mapX, mapZ));
                if (tile == null) continue;
                TileRect rect = tileRect(tile, layout, cell);
                int color = 0xE6000000 | darken(groupColorOf(tile), 0.42);
                int mask = outlineMask(tile);
                if ((mask & NORTH) != 0) graphics.fill(rect.x, rect.y, rect.right(), rect.y + thickness, color);
                if ((mask & SOUTH) != 0) graphics.fill(rect.x, rect.bottom() - thickness, rect.right(), rect.bottom(), color);
                if ((mask & WEST) != 0) graphics.fill(rect.x, rect.y, rect.x + thickness, rect.bottom(), color);
                if ((mask & EAST) != 0) graphics.fill(rect.right() - thickness, rect.y, rect.right(), rect.bottom(), color);
            }
        }
    }

    private void renderSelection(GuiGraphics graphics, Layout layout, double cell, MapTile hovered) {
        for (String tileId : selectedTiles) {
            MapTile tile = tilesById.get(tileId);
            if (tile == null) continue;
            TileRect rect = tileRect(tile, layout, cell);
            if (!visible(rect, layout)) continue;
            graphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), 0x2EFFD97C);
            outline(graphics, rect, 1, GOLD);
        }
        if (cell >= 3.0) active().ifPresent(group -> {
            for (String tileId : group.tileIds) {
                MapTile tile = tilesById.get(tileId);
                if (tile == null) continue;
                TileRect rect = tileRect(tile, layout, cell);
                if (visible(rect, layout)) graphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), 0x12FFFFFF);
            }
        });
        if (hovered != null) {
            TileRect rect = tileRect(hovered, layout, cell);
            if (visible(rect, layout)) {
                graphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), 0x206FDFE5);
                outline(graphics, rect, 1, HOVER);
            }
        }
        MapTile current = tilesById.get(snapshot.currentTileId());
        if (current != null) {
            TileRect rect = tileRect(current, layout, cell);
            if (visible(rect, layout)) {
                int cx = rect.centerX(), cy = rect.centerY();
                graphics.fill(cx - 3, cy - 1, cx + 4, cy, IVORY);
                graphics.fill(cx - 1, cy - 3, cx, cy + 4, IVORY);
            }
        }
    }

    private void renderGroupIcons(GuiGraphics graphics, Layout layout, double cell) {
        for (DraftGroup group : groups.values()) {
            if (!group.configured || group.icon.isEmpty() || group.tileIds.isEmpty()) continue;
            MapTile tile = tilesById.get(group.tileIds.iterator().next());
            if (tile == null) continue;
            TileRect rect = tileRect(tile, layout, cell);
            if (visible(rect, layout)) graphics.drawCenteredString(font, group.icon, rect.centerX(), rect.centerY() - 4, IVORY);
        }
    }

    private void renderSidebar(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        Sidebar sb = layout.sidebar;
        renderButton(graphics, sb.x + 8, sb.modeY, sb.width - 16, 20,
                "分组模式：" + (manualGroups ? "手动" : "自动"), true,
                inRect(mouseX, mouseY, sb.x + 8, sb.modeY, sb.width - 16, 20));
        graphics.drawString(font, "区域组列表（" + groups.size() + "）", sb.x + 8, sb.listHeaderY, GOLD, false);
        renderButton(graphics, sb.x + sb.width - 78, sb.listHeaderY - 3, 70, 16, "＋ 新建组",
                manualGroups && !selectedTiles.isEmpty(),
                inRect(mouseX, mouseY, sb.x + sb.width - 78, sb.listHeaderY - 3, 70, 16));
        renderGroupList(graphics, layout, mouseX, mouseY);
        renderDetail(graphics, layout, mouseX, mouseY);
        renderActionButtons(graphics, layout, mouseX, mouseY);
    }

    private void renderGroupList(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        Sidebar sb = layout.sidebar;
        int x = sb.x + 8;
        int w = sb.width - 16;
        int y = sb.listY;
        int h = sb.listHeight;
        List<DraftGroup> list = orderedGroups();
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, list.size() - h / ROW_HEIGHT));
        graphics.enableScissor(x, y, x + w, y + h);
        for (int i = 0; i < list.size(); i++) {
            int rowY = y + (i - listScroll) * ROW_HEIGHT;
            if (rowY + ROW_HEIGHT < y || rowY > y + h) continue;
            DraftGroup group = list.get(i);
            boolean active = group.id.equals(activeGroupId);
            boolean hovered = inRect(mouseX, mouseY, x, rowY, w, ROW_HEIGHT);
            graphics.fill(x, rowY, x + w, rowY + ROW_HEIGHT - 1, active ? 0xFF2A3740 : hovered ? 0xFF1F2B31 : 0xFF17222A);
            if (active) graphics.fill(x, rowY, x + 2, rowY + ROW_HEIGHT - 1, GOLD);
            graphics.drawString(font, group.icon.isEmpty() ? "·" : group.icon, x + 5, rowY + 5,
                    group.configured ? GOLD : MUTED, false);
            String label = group.name + " · " + group.tileIds.size();
            graphics.drawString(font, font.plainSubstrByWidth(label, w - 34), x + 22, rowY + 5,
                    active ? IVORY : 0xFFC7CDCF, false);
        }
        graphics.disableScissor();
        outline(graphics, new TileRect(x - 1, y - 1, w + 2, h + 2), 1, 0xFF2C383E);
    }

    private void renderDetail(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        Sidebar sb = layout.sidebar;
        int x = sb.x + 8;
        int w = sb.width - 16;
        int y = sb.detailY;
        DraftGroup group = active().orElse(null);

        graphics.drawString(font, "区域组详情", x, y + 10, GOLD, false);
        String members = group == null ? "未选择" : group.tileIds.size() + " 格";
        graphics.drawString(font, members, x + w - font.width(members), y + 10, MUTED, false);
        graphics.drawString(font, "名称", x, y + 20, MUTED, false);
        graphics.drawString(font, "图标", x, y + 50, MUTED, false);
        graphics.drawString(font, "据点", x, y + 106, MUTED, false);
        graphics.drawString(font, "资源", x, y + 130, MUTED, false);

        for (int i = 0; i < ICONS.size(); i++) {
            int col = i % ICON_COLS;
            int row = i / ICON_COLS;
            int cx = x + col * (ICON_W + ICON_GAP);
            int cy = y + 52 + row * (ICON_H + ICON_GAP);
            boolean selected = group != null && group.icon.equals(ICONS.get(i));
            boolean hovered = inRect(mouseX, mouseY, cx, cy, ICON_W, ICON_H);
            graphics.fill(cx, cy, cx + ICON_W, cy + ICON_H, selected ? 0xFF2A3740 : hovered ? 0xFF1F2B31 : 0xFF17222A);
            outline(graphics, new TileRect(cx, cy, ICON_W, ICON_H), 1, selected ? GOLD : 0xFF2C383E);
            graphics.drawCenteredString(font, ICONS.get(i).isEmpty() ? "—" : ICONS.get(i), cx + ICON_W / 2,
                    cy + (ICON_H - 9) / 2 + 1, selected ? IVORY : MUTED);
        }
        renderButton(graphics, x, y + 148, w, 20, "M 地图显示：" + (group != null && group.configured ? "开" : "关"),
                group != null, inRect(mouseX, mouseY, x, y + 148, w, 20));
    }

    private void renderActionButtons(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        Sidebar sb = layout.sidebar;
        int x = sb.x + 8;
        int bw = (sb.width - 16 - 6) / 2;
        boolean editable = manualGroups;
        renderButton(graphics, x, sb.actionY, bw, 20, "并入当前组", editable && !selectedTiles.isEmpty(),
                inRect(mouseX, mouseY, x, sb.actionY, bw, 20));
        renderButton(graphics, x + bw + 6, sb.actionY, bw, 20, "拆分为单格", editable && !selectedTiles.isEmpty(),
                inRect(mouseX, mouseY, x + bw + 6, sb.actionY, bw, 20));
        renderButton(graphics, x, sb.actionY + 24, bw, 20, "删除当前组", editable && active().isPresent(),
                inRect(mouseX, mouseY, x, sb.actionY + 24, bw, 20));
        renderButton(graphics, x + bw + 6, sb.actionY + 24, bw, 20, "清空选择", !selectedTiles.isEmpty(),
                inRect(mouseX, mouseY, x + bw + 6, sb.actionY + 24, bw, 20));
    }

    private void renderButton(GuiGraphics graphics, int x, int y, int w, int h, String label, boolean enabled, boolean hovered) {
        int bg = enabled ? (hovered ? 0xFF2A3740 : 0xFF1F2B31) : 0xFF141D22;
        int fg = enabled ? (hovered ? IVORY : 0xFFC7CDCF) : 0xFF5E676B;
        graphics.fill(x, y, x + w, y + h, bg);
        outline(graphics, new TileRect(x, y, w, h), 1, enabled ? 0xFF3C484E : 0xFF2C383E);
        graphics.drawCenteredString(font, label, x + w / 2, y + (h - 9) / 2 + 1, fg);
    }

    private void renderFooterHint(GuiGraphics graphics) {
        if (!serverError.isBlank()) graphics.drawString(font, "保存被拒绝：" + serverError, 12, height - 36, DANGER, false);
        graphics.drawString(font, "左键选择 · Shift 多选 · 右键拆分为单格 · 中键拖动 · 滚轮缩放", 12, height - 22, MUTED, false);
    }

    private void renderTileTooltip(GuiGraphics graphics, MapTile tile, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        DraftGroup group = groups.get(ownerOf(tile));
        if (group != null) lines.add(Component.literal(group.name).withColor(GOLD));
        lines.add(Component.literal(tile.biome().displayName()).withColor(0xFFC7CDCF));
        if (group != null && !group.site.isBlank()) lines.add(Component.literal("据点：" + group.site).withColor(MUTED));
        graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Layout layout = layout();
        Sidebar sb = layout.sidebar;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (inRect(mouseX, mouseY, sb.x + 8, sb.modeY, sb.width - 16, 20)) { toggleMode(); return true; }
            if (inRect(mouseX, mouseY, sb.x + sb.width - 78, sb.listHeaderY - 3, 70, 16)) { createGroupFromSelection(); return true; }
            if (inList(mouseX, mouseY, layout)) { clickList(mouseX, mouseY, layout); return true; }
            int icon = iconAt(mouseX, mouseY, layout);
            if (icon >= 0) { setIcon(ICONS.get(icon)); return true; }
            if (inRect(mouseX, mouseY, sb.x + 8, sb.detailY + 148, sb.width - 16, 20)) { toggleConfigured(); return true; }
            if (clickActionButton(mouseX, mouseY, layout)) return true;
        }
        MapTile tile = tileAt(mouseX, mouseY, layout);
        if (tile != null && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) { selectTile(tile, hasShiftDown()); return true; }
        if (tile != null && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && manualGroups) { splitTile(tile); return true; }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && inMap(mouseX, mouseY, layout())) {
            double cell = cellSize(layout());
            viewCenterX -= dragX / cell;
            viewCenterZ -= dragY / cell;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Layout layout = layout();
        if (inList(mouseX, mouseY, layout)) {
            listScroll = Math.max(0, listScroll - (int) Math.signum(scrollY));
            return true;
        }
        if (!inMap(mouseX, mouseY, layout)) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        double oldCell = cellSize(layout);
        double oldZoom = zoom;
        zoom = Mth.clamp(zoom * Math.pow(1.13, scrollY), 0.6, 8.0);
        if (zoom == oldZoom) return true;
        double newCell = cellSize(layout);
        MapViewport.Center center = MapViewport.zoomAround(new MapViewport.Center(viewCenterX, viewCenterZ), mouseX, mouseY,
                layout.mapCenterX(), layout.mapCenterY(), oldCell, newCell);
        viewCenterX = center.x();
        viewCenterZ = center.z();
        return true;
    }

    private void toggleMode() {
        manualGroups = !manualGroups;
        syncWidgets();
    }

    private void toggleConfigured() {
        active().ifPresent(group -> {
            group.configured = !group.configured;
            refreshAtlas();
        });
    }

    private void setIcon(String icon) {
        active().ifPresent(group -> {
            group.icon = icon;
            if (!icon.isEmpty()) group.configured = true;
            refreshAtlas();
        });
    }

    private void selectTile(MapTile tile, boolean additive) {
        if (!additive) {
            selectedTiles.clear();
            activeGroupId = ownerOf(tile);
            syncWidgets();
        }
        selectedTiles.add(tile.id());
    }

    private void splitTile(MapTile tile) {
        selectedTiles.clear();
        selectedTiles.add(tile.id());
        splitSelection();
    }

    private void clickList(double mouseX, double mouseY, Layout layout) {
        Sidebar sb = layout.sidebar;
        int index = listScroll + (int) ((mouseY - sb.listY) / ROW_HEIGHT);
        List<DraftGroup> list = orderedGroups();
        if (index < 0 || index >= list.size()) return;
        activeGroupId = list.get(index).id;
        selectedTiles.clear();
        syncWidgets();
        centerOn(list.get(index));
    }

    private boolean clickActionButton(double mouseX, double mouseY, Layout layout) {
        Sidebar sb = layout.sidebar;
        int x = sb.x + 8;
        int bw = (sb.width - 16 - 6) / 2;
        if (inRect(mouseX, mouseY, x, sb.actionY, bw, 20)) { mergeSelectionIntoActive(); return true; }
        if (inRect(mouseX, mouseY, x + bw + 6, sb.actionY, bw, 20)) { splitSelection(); return true; }
        if (inRect(mouseX, mouseY, x, sb.actionY + 24, bw, 20)) { deleteActiveGroup(); return true; }
        if (inRect(mouseX, mouseY, x + bw + 6, sb.actionY + 24, bw, 20)) { selectedTiles.clear(); return true; }
        return false;
    }

    private int iconAt(double mouseX, double mouseY, Layout layout) {
        Sidebar sb = layout.sidebar;
        int x = sb.x + 8;
        for (int i = 0; i < ICONS.size(); i++) {
            int cx = x + (i % ICON_COLS) * (ICON_W + ICON_GAP);
            int cy = sb.detailY + 52 + (i / ICON_COLS) * (ICON_H + ICON_GAP);
            if (inRect(mouseX, mouseY, cx, cy, ICON_W, ICON_H)) return i;
        }
        return -1;
    }

    private void syncWidgets() {
        DraftGroup group = active().orElse(null);
        if (groupName != null) {
            groupName.setValue(group == null ? "" : group.name);
            groupName.setEditable(group != null && manualGroups);
        }
        if (groupSite != null) {
            groupSite.setValue(group == null ? "" : group.site);
            groupSite.setEditable(group != null && manualGroups);
        }
        if (groupResources != null) {
            groupResources.setValue(group == null ? "" : group.resources);
            groupResources.setEditable(group != null && manualGroups);
        }
    }

    private void centerOn(DraftGroup group) {
        double sumX = 0, sumZ = 0;
        int count = 0;
        for (String tileId : group.tileIds) {
            MapTile tile = tilesById.get(tileId);
            if (tile == null) continue;
            sumX += tile.mapX();
            sumZ += tile.mapZ();
            count++;
        }
        if (count > 0) {
            viewCenterX = sumX / count;
            viewCenterZ = sumZ / count;
        }
    }

    private void createGroupFromSelection() {
        if (!manualGroups || selectedTiles.isEmpty()) return;
        removeSelectedFromOwners();
        String id;
        do { id = "manual_custom_" + nextGroupNumber++; } while (groups.containsKey(id));
        DraftGroup created = new DraftGroup(id, "新区域组", "", "", "", true);
        created.tileIds.addAll(selectedTiles);
        groups.put(id, created);
        activeGroupId = id;
        rebuildOwners();
        refreshAtlas();
        syncWidgets();
    }

    private void mergeSelectionIntoActive() {
        DraftGroup active = active().orElse(null);
        if (!manualGroups || active == null || selectedTiles.isEmpty()) return;
        removeSelectedFromOwners();
        active.tileIds.addAll(selectedTiles);
        rebuildOwners();
        refreshAtlas();
        syncWidgets();
    }

    private void splitSelection() {
        if (!manualGroups || selectedTiles.isEmpty()) return;
        removeSelectedFromOwners();
        String first = null;
        for (String tileId : selectedTiles) {
            String id;
            do { id = "manual_custom_" + nextGroupNumber++; } while (groups.containsKey(id));
            MapTile tile = tilesById.get(tileId);
            DraftGroup group = new DraftGroup(id, "世界格 [" + tile.mapX() + ", " + tile.mapZ() + "]", "", "", "", true);
            group.tileIds.add(tileId);
            groups.put(id, group);
            if (first == null) first = id;
        }
        activeGroupId = first;
        rebuildOwners();
        refreshAtlas();
        syncWidgets();
    }

    private void deleteActiveGroup() {
        DraftGroup active = active().orElse(null);
        if (!manualGroups || active == null) return;
        selectedTiles.clear();
        selectedTiles.addAll(active.tileIds);
        groups.remove(active.id);
        splitSelection();
    }

    private void removeSelectedFromOwners() {
        for (String tileId : selectedTiles) {
            String owner = tileOwners.get(tileId);
            DraftGroup group = groups.get(owner);
            if (group != null) group.tileIds.remove(tileId);
        }
        groups.values().removeIf(group -> group.tileIds.isEmpty());
    }

    private void rebuildOwners() {
        tileOwners.clear();
        groups.forEach((id, group) -> group.tileIds.forEach(tileId -> tileOwners.put(tileId, id)));
    }

    private void refreshAtlas() {
        if (groupAtlas != null) groupAtlas.close();
        Map<String, Boolean> configured = new LinkedHashMap<>();
        groups.forEach((id, group) -> configured.put(id, group.configured));
        groupAtlas = new GroupEditorAtlasTexture(snapshot, tileOwners, configured);
    }

    private void save() {
        List<ManualChunkGroupLayout.Group> draft = groups.values().stream()
                .map(group -> new ManualChunkGroupLayout.Group(group.id, group.name, group.icon, group.site,
                        group.resources, group.configured, List.copyOf(group.tileIds))).toList();
        PacketDistributor.sendToServer(new SaveWorldGroupEditPayload(manualGroups, snapshot.groupRevision(), draft));
    }

    private Optional<DraftGroup> active() { return Optional.ofNullable(groups.get(activeGroupId)); }

    private List<DraftGroup> orderedGroups() {
        return groups.values().stream()
                .sorted(Comparator.comparing((DraftGroup group) -> group.name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(group -> group.id))
                .toList();
    }

    private Layout layout() {
        int header = 44;
        int footer = 42;
        int sideWidth = Math.min(324, Math.max(248, width / 4));
        int sideX = width - sideWidth - 12;
        int mapLeft = 12;
        int mapTop = header + 6;
        int mapWidth = Math.max(140, sideX - 12 - mapLeft);
        int mapHeight = Math.max(140, height - mapTop - footer - 8);
        int sideTop = header;
        int sideBottom = height - footer;
        int modeY = sideTop + 4;
        int listHeaderY = sideTop + 30;
        int listY = sideTop + 50;
        int saveHeight = 22;
        int actionHeight = 48;
        int detailHeight = 172;
        int saveY = sideBottom - saveHeight - 2;
        int actionY = saveY - actionHeight - 6;
        int detailY = actionY - detailHeight - 6;
        int listHeight = Math.max(48, detailY - listY - 6);
        return new Layout(mapLeft, mapTop, mapWidth, mapHeight,
                new Sidebar(sideX, sideWidth, modeY, listHeaderY, listY, listHeight, detailY, actionY, saveY));
    }

    private double cellSize(Layout layout) {
        return MapViewport.cellSize(layout.mapWidth, layout.mapHeight, snapshot.mapWidth(), snapshot.mapHeight(), zoom);
    }

    private TileRect tileRect(MapTile tile, Layout layout, double cell) {
        int x1 = round(layout.mapCenterX() + (tile.mapX() - .5 - viewCenterX) * cell);
        int y1 = round(layout.mapCenterY() + (tile.mapZ() - .5 - viewCenterZ) * cell);
        int x2 = round(layout.mapCenterX() + (tile.mapX() + .5 - viewCenterX) * cell);
        int y2 = round(layout.mapCenterY() + (tile.mapZ() + .5 - viewCenterZ) * cell);
        return new TileRect(x1, y1, Math.max(1, x2 - x1), Math.max(1, y2 - y1));
    }

    private MapTile tileAt(double mouseX, double mouseY, Layout layout) {
        if (!inMap(mouseX, mouseY, layout)) return null;
        double cell = cellSize(layout);
        int mapX = (int) Math.floor(viewCenterX + (mouseX - layout.mapCenterX()) / cell + .5);
        int mapZ = (int) Math.floor(viewCenterZ + (mouseY - layout.mapCenterY()) / cell + .5);
        return tilesByCoordinate.get(key(mapX, mapZ));
    }

    private VisibleRange visibleRange(Layout layout, double cell) {
        int minX = Math.max(snapshot.mapMinimumX(), (int) Math.floor(viewCenterX - layout.mapWidth / 2.0 / cell) - 1);
        int maxX = Math.min(snapshot.mapMinimumX() + snapshot.mapWidth() - 1,
                (int) Math.ceil(viewCenterX + layout.mapWidth / 2.0 / cell) + 1);
        int minZ = Math.max(snapshot.mapMinimumZ(), (int) Math.floor(viewCenterZ - layout.mapHeight / 2.0 / cell) - 1);
        int maxZ = Math.min(snapshot.mapMinimumZ() + snapshot.mapHeight() - 1,
                (int) Math.ceil(viewCenterZ + layout.mapHeight / 2.0 / cell) + 1);
        return new VisibleRange(minX, minZ, maxX, maxZ);
    }

    private int outlineMask(MapTile tile) {
        String owner = ownerOf(tile);
        int mask = 0;
        if (!owner.equals(ownerAt(tile.mapX(), tile.mapZ() - 1))) mask |= NORTH;
        if (!owner.equals(ownerAt(tile.mapX() + 1, tile.mapZ()))) mask |= EAST;
        if (!owner.equals(ownerAt(tile.mapX(), tile.mapZ() + 1))) mask |= SOUTH;
        if (!owner.equals(ownerAt(tile.mapX() - 1, tile.mapZ()))) mask |= WEST;
        return mask;
    }

    private String ownerOf(MapTile tile) { return tileOwners.getOrDefault(tile.id(), tile.regionId()); }

    private String ownerAt(int x, int z) {
        MapTile neighbour = tilesByCoordinate.get(key(x, z));
        return neighbour == null ? "" : ownerOf(neighbour);
    }

    private int groupColorOf(MapTile tile) { return groupColor(ownerOf(tile)); }

    static int groupColor(String id) {
        int[] palette = {0x688A8D, 0xA28B67, 0x66846A, 0x936E71, 0x7F78A0, 0x989566, 0x628884, 0x96708B};
        return palette[Math.floorMod(id.hashCode(), palette.length)];
    }

    private static int darken(int rgb, double factor) {
        int red = (int) (((rgb >> 16) & 0xFF) * factor);
        int green = (int) (((rgb >> 8) & 0xFF) * factor);
        int blue = (int) ((rgb & 0xFF) * factor);
        return red << 16 | green << 8 | blue;
    }

    private static void outline(GuiGraphics graphics, TileRect rect, int thickness, int color) {
        if (rect.width <= 0 || rect.height <= 0) return;
        graphics.fill(rect.x, rect.y, rect.right(), rect.y + thickness, color);
        graphics.fill(rect.x, rect.bottom() - thickness, rect.right(), rect.bottom(), color);
        graphics.fill(rect.x, rect.y, rect.x + thickness, rect.bottom(), color);
        graphics.fill(rect.right() - thickness, rect.y, rect.right(), rect.bottom(), color);
    }

    private static int round(double value) { return (int) Math.floor(value + .5); }

    private TileRect atlasRect(Layout layout, double cell) {
        int x1 = round(layout.mapCenterX() + (snapshot.mapMinimumX() - .5 - viewCenterX) * cell);
        int y1 = round(layout.mapCenterY() + (snapshot.mapMinimumZ() - .5 - viewCenterZ) * cell);
        int x2 = round(layout.mapCenterX() + (snapshot.mapMinimumX() + snapshot.mapWidth() - .5 - viewCenterX) * cell);
        int y2 = round(layout.mapCenterY() + (snapshot.mapMinimumZ() + snapshot.mapHeight() - .5 - viewCenterZ) * cell);
        return new TileRect(x1, y1, Math.max(1, x2 - x1), Math.max(1, y2 - y1));
    }

    private static boolean visible(TileRect rect, Layout layout) {
        return rect.right() > layout.mapLeft && rect.x < layout.mapRight() && rect.bottom() > layout.mapTop
                && rect.y < layout.mapBottom();
    }

    private static boolean inMap(double x, double y, Layout layout) {
        return x >= layout.mapLeft && x < layout.mapRight() && y >= layout.mapTop && y < layout.mapBottom();
    }

    private static boolean inList(double x, double y, Layout layout) {
        Sidebar sb = layout.sidebar;
        return x >= sb.x + 8 && x < sb.x + 8 + (sb.width - 16) && y >= sb.listY && y < sb.listY + sb.listHeight;
    }

    private static boolean inRect(double x, double y, int rx, int ry, int rw, int rh) {
        return x >= rx && x < rx + rw && y >= ry && y < ry + rh;
    }

    private static long key(int x, int z) { return (long) x << 32 ^ z & 0xFFFFFFFFL; }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void removed() {
        if (biomeAtlas != null) { biomeAtlas.close(); biomeAtlas = null; }
        if (groupAtlas != null) { groupAtlas.close(); groupAtlas = null; }
    }

    private static final class DraftGroup {
        private final String id;
        private String name;
        private String icon;
        private String site;
        private String resources;
        private boolean configured;
        private final LinkedHashSet<String> tileIds = new LinkedHashSet<>();

        private DraftGroup(String id, String name, String icon, String site, String resources, boolean configured) {
            this.id = id;
            this.name = name;
            this.icon = icon;
            this.site = site;
            this.resources = resources;
            this.configured = configured;
        }
    }

    private record Layout(int mapLeft, int mapTop, int mapWidth, int mapHeight, Sidebar sidebar) {
        int mapRight() { return mapLeft + mapWidth; }
        int mapBottom() { return mapTop + mapHeight; }
        int mapCenterX() { return mapLeft + mapWidth / 2; }
        int mapCenterY() { return mapTop + mapHeight / 2; }
    }

    private record Sidebar(int x, int width, int modeY, int listHeaderY, int listY, int listHeight,
                           int detailY, int actionY, int saveY) {}

    private record VisibleRange(int minX, int minZ, int maxX, int maxZ) {}

    private record TileRect(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
        int centerX() { return x + width / 2; }
        int centerY() { return y + height / 2; }
    }
}
