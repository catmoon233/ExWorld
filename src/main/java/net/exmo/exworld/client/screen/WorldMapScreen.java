package net.exmo.exworld.client.screen;

import net.exmo.exworld.world.map.RegionOutlineIndex;
import net.exmo.exworld.world.model.MapAnchor;
import net.exmo.exworld.world.model.MapRegion;
import net.exmo.exworld.world.model.WorldDimensions;
import net.exmo.exworld.world.model.WorldSnapshot;
import net.exmo.exworld.world.model.MapTile;
import net.exmo.exworld.network.RequestWorldGroupEditorPayload;
import net.exmo.exworld.network.MapTeleportPayload;
import net.exmo.exworld.Config;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Fast full-size biome atlas with merged region outlines and stable double-precision navigation. */
public final class WorldMapScreen extends Screen {
    private static final int GOLD = 0xFFD6B56A;
    private static final int IVORY = 0xFFF0E7D1;
    private static final int MUTED = 0xFF9FA6A8;
    private static final double MIN_ZOOM = 0.6;
    private static final double MAX_ZOOM = 7.0;
    private final WorldSnapshot snapshot;
    private final Map<Long, MapTile> tileGrid = new HashMap<>();
    private final Map<String, MapTile> tileById = new HashMap<>();
    private final Map<String, MapRegion> regionsById = new HashMap<>();
    private final Map<String, MapTile> iconTiles = new HashMap<>();
    private final List<RegionIcon> regionIcons;
    private final Map<Long, RegionIcon> regionIconsByCoordinate;
    private final RegionOutlineIndex regionOutlines;
    private final MapTile current;
    private final boolean archipelago;
    private MapTile selected;
    private BiomeAtlasTexture biomeAtlas;
    private double zoom = 1.0;
    /** Map-space coordinate held at the viewport center; it is independent of GUI scale and pixel rounding. */
    private double viewCenterX;
    private double viewCenterZ;
    private boolean teleportOpen;
    private int teleportX;
    private int teleportZ;
    private int popupX;
    private int popupY;
    public WorldMapScreen(WorldSnapshot snapshot) {
        super(Component.translatable("screen.exworld.world_map"));
        this.snapshot = snapshot;
        snapshot.tiles().forEach(tile -> {
            tileGrid.put(key(tile.mapX(), tile.mapZ()), tile);
            tileById.put(tile.id(), tile);
            iconTiles.putIfAbsent(tile.regionId(), tile);
        });
        snapshot.regions().forEach(region -> regionsById.put(region.id(), region));
        this.regionIcons = snapshot.regions().stream()
                .filter(region -> region.configured() && !region.icon().isBlank())
                .map(region -> new RegionIcon(region, iconTiles.get(region.id())))
                .filter(icon -> icon.tile != null)
                .toList();
        this.regionIconsByCoordinate = new HashMap<>(regionIcons.size());
        regionIcons.forEach(icon -> regionIconsByCoordinate.put(key(icon.tile.mapX(), icon.tile.mapZ()), icon));
        this.regionOutlines = new RegionOutlineIndex(snapshot.tiles());
        this.archipelago = snapshot.archipelago();
        this.current = snapshot.tiles().stream().filter(t -> t.id().equals(snapshot.currentTileId())).findFirst()
                .orElse(snapshot.tiles().getFirst());
        this.selected = current;
        this.viewCenterX = selected.mapX();
        this.viewCenterZ = selected.mapZ();
    }

    @Override
    protected void init() {
        if (biomeAtlas == null && !archipelago && !Config.decryptionMode) biomeAtlas = new BiomeAtlasTexture(snapshot);
        Layout layout = layout();
        int bottomY = height - layout.footerHeight + Math.max(8, (layout.footerHeight - 20) / 2);
        if (!Config.decryptionMode || admin()) {
            addRenderableWidget(Button.builder(Component.literal("编辑区域组"), b ->
                    PacketDistributor.sendToServer(new RequestWorldGroupEditorPayload()))
                    .bounds(Math.max(12, width - 278), bottomY, 96, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.locate_current"), b -> selectCurrent())
                .bounds(width - 174, bottomY, 92, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width - 76, bottomY, 64, 20).build());
    }

    private void selectCurrent() {
        snapshot.tiles().stream().filter(tile -> tile.id().equals(snapshot.currentTileId())).findFirst().ifPresent(tile -> {
            selected = tile;
            viewCenterX = tile.mapX();
            viewCenterZ = tile.mapZ();
        });
    }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        renderFrame(graphics, layout);
        graphics.enableScissor(0, layout.headerHeight, width, height - layout.footerHeight);
        renderMap(graphics, layout, mouseX, mouseY);
        graphics.disableScissor();
        renderHeader(graphics, layout, mouseX, mouseY);
        renderFooter(graphics, layout);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (teleportOpen) renderTeleportPopup(graphics);
        MapTile hovered = tileAt(mouseX, mouseY, layout);
        if (hovered != null && !Config.decryptionMode) renderTileTooltip(graphics, hovered, mouseX, mouseY);
    }

    private void renderFrame(GuiGraphics graphics, Layout layout) {
        graphics.fill(0, 0, width, height, 0xFF080C10);
        graphics.fill(0, layout.headerHeight, width, height - layout.footerHeight, 0xFF11191D);
        // Subtle latitude bands make empty margins feel like a cartographic drafting table.
        for (int y = layout.headerHeight + 12; y < height - layout.footerHeight; y += 24) {
            graphics.fill(0, y, width, y + 1, 0x101C3036);
        }
        graphics.fill(0, 0, width, layout.headerHeight, 0xFA151B20);
        graphics.fill(0, height - layout.footerHeight, width, height, 0xFA181D20);
        graphics.fill(0, layout.headerHeight - 1, width, layout.headerHeight, 0x805E676B);
        graphics.fill(0, height - layout.footerHeight, width, height - layout.footerHeight + 1, 0x805E676B);
        graphics.fill(0, layout.headerHeight, 2, height - layout.footerHeight, 0xFFB99352);
    }

    private void renderMap(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        if (Config.decryptionMode) {
            renderConfiguredFills(graphics, layout);
            renderMergedRegionOutlines(graphics, layout);
            renderRegionIcons(graphics, layout);
            renderCurrentMarker(graphics, layout);
            return;
        }
        if (archipelago) {
            renderArchipelagoRegions(graphics, layout);
        } else {
            MapRect map = mapRect(layout);
            if (biomeAtlas != null) graphics.blit(biomeAtlas.location(), map.x, map.y, 0, 0,
                    map.width, map.height, MapViewport.atlasTextureExtent(map.width), MapViewport.atlasTextureExtent(map.height));
            renderMergedRegionOutlines(graphics, layout);
        }
        renderRegionIcons(graphics, layout);
        renderSelection(graphics, layout, mouseX, mouseY);
        renderAnchors(graphics, layout);
    }

    /**
     * The original region layer reused for the island world: every explored tile belongs to its nearest island's
     * unnamed region, so the map reads as connected island territories instead of biome zones. Island centres show
     * their kind label once zoomed in.
     */
    private void renderArchipelagoRegions(GuiGraphics graphics, Layout layout) {
        double cell = cellSize(layout);
        VisibleRange range = visibleRange(layout);
        if (range.tileCount() > 2_048) return;
        for (int mapZ = range.minZ; mapZ <= range.maxZ; mapZ++) {
            for (int mapX = range.minX; mapX <= range.maxX; mapX++) {
                MapTile tile = tileGrid.get(key(mapX, mapZ));
                if (tile == null) continue;
                TileRect rect = tileRect(tile, layout);
                if (!visible(rect, layout)) continue;
                int color = 0x38000000 | darken(regionColor(tile.regionId()), 0.30);
                graphics.fill(rect.x + 1, rect.y + 1, rect.right() - 1, rect.bottom() - 1, color);
                if (tile.island() && cell >= 9.0 && rect.width >= 18) {
                    graphics.drawCenteredString(font, tile.sites(), rect.centerX(), rect.centerY() - 4, IVORY);
                }
            }
        }
    }
    /** Only edges against another region or the atlas exterior are drawn; internal square seams disappear. */
    private void renderMergedRegionOutlines(GuiGraphics graphics, Layout layout) {
        if (cellSize(layout) < 5.0) return;
        int thickness = zoom >= 3.0 ? 2 : 1;
        VisibleRange range = visibleRange(layout);
        if (range.tileCount() > 2_048) return;
        for (int mapZ = range.minZ; mapZ <= range.maxZ; mapZ++) {
            for (int mapX = range.minX; mapX <= range.maxX; mapX++) {
                MapTile tile = tileGrid.get(key(mapX, mapZ));
                if (tile == null || !configured(tile)) continue;
                TileRect rect = tileRect(tile, layout);
                int color = 0xE6000000 | darken(regionColor(tile.regionId()), 0.52);
                int mask = regionOutlines.mask(tile);
                if ((mask & RegionOutlineIndex.NORTH) != 0) graphics.fill(rect.x, rect.y, rect.right(), rect.y + thickness, color);
                if ((mask & RegionOutlineIndex.SOUTH) != 0) graphics.fill(rect.x, rect.bottom(), rect.right(), rect.bottom() + thickness, color);
                if ((mask & RegionOutlineIndex.WEST) != 0) graphics.fill(rect.x, rect.y, rect.x + thickness, rect.bottom(), color);
                if ((mask & RegionOutlineIndex.EAST) != 0) graphics.fill(rect.right(), rect.y, rect.right() + thickness, rect.bottom(), color);
            }
        }
    }

    /** Activated anchors stay visible on every layer and use their physical position inside a world tile. */
    private void renderAnchors(GuiGraphics graphics, Layout layout) {
        double cell = cellSize(layout);
        for (MapAnchor anchor : snapshot.anchors()) {
            if (!tileById.containsKey(anchor.tileId())) continue;
            double mapX = WorldDimensions.mapCoordinate(anchor.x() + 0.5, snapshot.groupChunks());
            double mapZ = WorldDimensions.mapCoordinate(anchor.z() + 0.5, snapshot.groupChunks());
            int x = roundStable(width / 2.0 + (mapX - viewCenterX) * cell);
            int y = roundStable(layout.viewportCenterY() + (mapZ - viewCenterZ) * cell);
            if (x < -10 || x > width + 10 || y < layout.headerHeight - 10
                    || y > height - layout.footerHeight + 10) continue;
            int radius = zoom >= 3.0 ? 4 : 3;
            graphics.fill(x - radius - 1, y - radius - 1, x + radius + 2, y + radius + 2, 0xEA080D10);
            graphics.fill(x - radius, y - radius, x + radius + 1, y + radius + 1, GOLD);
            graphics.fill(x - 1, y - radius - 2, x + 2, y + radius + 3, IVORY);
            graphics.fill(x - radius - 2, y - 1, x + radius + 3, y + 2, IVORY);
            if (zoom >= 4.0 && width >= 420) {
                int labelWidth = font.width(anchor.name()) + 6;
                graphics.fill(x + 7, y - 6, x + 7 + labelWidth, y + 6, 0xD80A1014);
                graphics.drawString(font, anchor.name(), x + 10, y - 4, IVORY, false);
            }
        }
    }

    /** A configured built-in/ASCII icon is rendered once at its group's representative world tile. */
    private void renderRegionIcons(GuiGraphics graphics, Layout layout) {
        if (cellSize(layout) < 3.0) return;
        if (regionIcons.size() <= 512) {
            regionIcons.forEach(icon -> renderRegionIcon(graphics, layout, icon));
            return;
        }
        VisibleRange range = visibleRange(layout);
        if (range.tileCount() > 2_048) return;
        for (int mapZ = range.minZ; mapZ <= range.maxZ; mapZ++) {
            for (int mapX = range.minX; mapX <= range.maxX; mapX++) {
                RegionIcon icon = regionIconsByCoordinate.get(key(mapX, mapZ));
                if (icon != null) renderRegionIcon(graphics, layout, icon);
            }
        }
    }

    private void renderRegionIcon(GuiGraphics graphics, Layout layout, RegionIcon icon) {
        TileRect rect = tileRect(icon.tile, layout);
        if (!visible(rect, layout)) return;
        int radius = Math.max(3, Math.min(7, Math.min(rect.width, rect.height) / 3));
        int x = rect.centerX(), y = rect.centerY();
        graphics.fill(x - radius, y - radius, x + radius + 1, y + radius + 1, 0xD8080D10);
        graphics.drawCenteredString(font, icon.region.icon(), x, y - 4, GOLD);
    }

    private void renderSelection(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        MapTile hovered = tileAt(mouseX, mouseY, layout);
        Map<String, MapTile> highlighted = new java.util.LinkedHashMap<>();
        highlighted.put(selected.id(), selected);
        highlighted.put(current.id(), current);
        if (hovered != null) highlighted.put(hovered.id(), hovered);
        for (MapTile tile : highlighted.values()) {
            boolean active = tile.id().equals(selected.id());
            boolean current = tile.id().equals(snapshot.currentTileId());
            boolean isHovered = tile == hovered;
            if (!active && !current && !isHovered) continue;
            TileRect rect = tileRect(tile, layout);
            if (isHovered) graphics.fill(rect.x, rect.y, rect.right(), rect.bottom(), 0x266FDFE5);
            outline(graphics, rect.inset(2), active ? 2 : 1, active ? GOLD : current ? IVORY : 0xB0D4E6E8);
            if (layout.showLabels && (archipelago || configured(tile)) && rect.width >= 25) {
                String label = archipelago
                        ? (tile.island() ? tile.sites() : (groupName(tile).isBlank() ? tile.biome().displayName() : groupName(tile)))
                        : groupName(tile) + " · " + tile.biome().displayName();
                int labelWidth = Math.min(Math.max(34, rect.width * 2), font.width(label) + 8);
                int labelX = rect.centerX() - labelWidth / 2;
                graphics.fill(labelX, rect.centerY() - 6, labelX + labelWidth, rect.centerY() + 6, 0xD812181C);
                graphics.drawCenteredString(font, label, rect.centerX(), rect.centerY() - 4, IVORY);
            }
        }
    }

    private TileRect tileRect(MapTile tile, Layout layout) {
        double cell = cellSize(layout);
        int x1 = roundStable(width / 2.0 + (tile.mapX() - 0.5 - viewCenterX) * cell);
        int y1 = roundStable(layout.viewportCenterY() + (tile.mapZ() - 0.5 - viewCenterZ) * cell);
        int x2 = roundStable(width / 2.0 + (tile.mapX() + 0.5 - viewCenterX) * cell);
        int y2 = roundStable(layout.viewportCenterY() + (tile.mapZ() + 0.5 - viewCenterZ) * cell);
        return new TileRect(x1, y1, Math.max(1, x2 - x1), Math.max(1, y2 - y1));
    }

    private MapRect mapRect(Layout layout) {
        double cell = cellSize(layout);
        int x1 = roundStable(width / 2.0 + (snapshot.mapMinimumX() - 0.5 - viewCenterX) * cell);
        int y1 = roundStable(layout.viewportCenterY() + (snapshot.mapMinimumZ() - 0.5 - viewCenterZ) * cell);
        int x2 = roundStable(width / 2.0 + (snapshot.mapMinimumX() + snapshot.mapWidth() - 0.5 - viewCenterX) * cell);
        int y2 = roundStable(layout.viewportCenterY() + (snapshot.mapMinimumZ() + snapshot.mapHeight() - 0.5 - viewCenterZ) * cell);
        return new MapRect(x1, y1, Math.max(1, x2 - x1), Math.max(1, y2 - y1));
    }

    private double cellSize(Layout layout) {
        return MapViewport.cellSize(width, layout.viewportHeight(), snapshot.mapWidth(), snapshot.mapHeight(), zoom);
    }

    private void renderHeader(GuiGraphics graphics, Layout layout, int mouseX, int mouseY) {
        int titleX = 12;
        if (width >= 470) {
            graphics.drawString(font, title, titleX, 10, IVORY, false);
            graphics.drawString(font, Component.literal(Math.round(zoom * 100.0) + "%  ·  "
                    + (archipelago ? "浮岛图" : "群系图") + " / 滚轮缩放 / 中键拖动"), titleX, 24, MUTED, false);
        }
        if (Config.decryptionMode) {
            String readout = cursorLabel(mouseX, mouseY, layout);
            graphics.drawString(font, readout, width - font.width(readout) - 12, 10, IVORY, false);
        } else if (width >= 610) {
            int progressWidth = Math.min(130, Math.max(60, width / 6));
            int progressX = width - progressWidth - 14;
            int progress = progress();
            graphics.drawString(font, Component.translatable(snapshot.pregenerationEnabled()
                    ? "screen.exworld.generation" : "screen.exworld.generation_demand", progress), progressX, 10, MUTED, false);
            graphics.fill(progressX, 27, progressX + progressWidth, 30, 0xFF3C4549);
            graphics.fill(progressX, 27, progressX + progressWidth * progress / 100, 30, GOLD);
        }
    }
    private void renderFooter(GuiGraphics graphics, Layout layout) {
        int top = height - layout.footerHeight;
        MapRegion region = regionFor(selected);
        String title;
        if (archipelago) {
            String regionLabel = region.name().isBlank() ? "" : region.name() + " · ";
            title = regionLabel + (selected.island() ? selected.sites() : selected.biome().displayName());
        } else {
            title = (region.configured() ? region.name() + " · " : "") + selected.biome().displayName();
        }
        graphics.drawString(font, title, 14, top + 10, GOLD, false);
        String details = "[" + selected.mapX() + ", " + selected.mapZ() + "]";
        if (region.configured() && !region.site().isBlank()) details += "  据点：" + region.site();
        graphics.drawString(font, Component.literal(details), 14, top + 25, MUTED, false);
        if (region.configured() && !region.resources().isBlank() && layout.footerHeight >= 58) {
            graphics.drawString(font, Component.literal("资源：" + region.resources()), 14, top + 40, MUTED, false);
        }
    }

    private void renderTileTooltip(GuiGraphics graphics, MapTile tile, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        MapRegion region = regionFor(tile);
        if (archipelago) {
            if (!region.name().isBlank()) lines.add(Component.literal(region.name()).withColor(GOLD));
            lines.add(Component.literal(tile.island() ? tile.sites() : tile.biome().displayName()));
        } else {
            if (region.configured()) lines.add(Component.literal(region.name()).withColor(GOLD));
            lines.add(Component.translatable("screen.exworld.biome", tile.biome().displayName()));
        }
        if (region.configured() && !region.site().isBlank()) lines.add(Component.literal("据点：" + region.site()));
        if (region.configured() && !region.resources().isBlank()) lines.add(Component.literal("资源：" + region.resources()));
        List<MapAnchor> anchors = snapshot.anchors().stream().filter(anchor -> anchor.tileId().equals(tile.id())).toList();
        if (!anchors.isEmpty()) lines.add(Component.translatable("screen.exworld.anchors",
                anchors.stream().map(MapAnchor::name).collect(java.util.stream.Collectors.joining("、"))));
        graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (teleportOpen && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && inTeleportButton(mouseX, mouseY)) {
            PacketDistributor.sendToServer(new MapTeleportPayload(teleportX, teleportZ));
            teleportOpen = false;
            onClose();
            return true;
        }
        Layout layout = layout();
        if (Config.decryptionMode && minecraft != null && minecraft.player != null && minecraft.player.isCreative()
                && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && inViewport(mouseY, layout)) {
            int[] world = worldAt(mouseX, mouseY, layout);
            teleportX = world[0];
            teleportZ = world[1];
            popupX = (int) mouseX;
            popupY = (int) mouseY;
            teleportOpen = true;
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) teleportOpen = false;
        MapTile tile = tileAt(mouseX, mouseY, layout);
        if (tile != null && button == 0) { selected = tile; return true; }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderConfiguredFills(GuiGraphics graphics, Layout layout) {
        VisibleRange range = visibleRange(layout);
        if (range.tileCount() > 2_048) return;
        for (int mapZ = range.minZ; mapZ <= range.maxZ; mapZ++) {
            for (int mapX = range.minX; mapX <= range.maxX; mapX++) {
                MapTile tile = tileGrid.get(key(mapX, mapZ));
                if (tile == null || !configured(tile)) continue;
                TileRect rect = tileRect(tile, layout);
                if (!visible(rect, layout)) continue;
                graphics.fill(rect.x + 1, rect.y + 1, rect.right() - 1, rect.bottom() - 1, 0x66000000 | regionColor(tile.regionId()));
            }
        }
    }

    private void renderCurrentMarker(GuiGraphics graphics, Layout layout) {
        TileRect rect = tileRect(current, layout);
        graphics.fill(rect.centerX() - 2, rect.centerY() - 2, rect.centerX() + 3, rect.centerY() + 3, GOLD);
    }

    private void renderTeleportPopup(GuiGraphics graphics) {
        int w = 92;
        int h = 22;
        int x = Math.min(popupX, width - w - 8);
        int y = Math.min(popupY, height - h - 8);
        graphics.fill(x, y, x + w, y + h, 0xF012181C);
        graphics.fill(x, y, x + w, y + 1, GOLD);
        graphics.drawCenteredString(font, Component.translatable("screen.exworld.teleport_here"), x + w / 2, y + 7, IVORY);
    }

    private boolean inTeleportButton(double mouseX, double mouseY) {
        int w = 92;
        int h = 22;
        int x = Math.min(popupX, width - w - 8);
        int y = Math.min(popupY, height - h - 8);
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private int[] worldAt(double mouseX, double mouseY, Layout layout) {
        double cell = cellSize(layout);
        int mapX = (int) Math.floor(viewCenterX + (mouseX - width / 2.0) / cell + 0.5);
        int mapZ = (int) Math.floor(viewCenterZ + (mouseY - layout.viewportCenterY()) / cell + 0.5);
        return new int[]{WorldDimensions.groupCenter(mapX, snapshot.groupChunks()), WorldDimensions.groupCenter(mapZ, snapshot.groupChunks())};
    }

    private String cursorLabel(int mouseX, int mouseY, Layout layout) {
        if (!inViewport(mouseY, layout)) return Component.translatable("screen.exworld.map_cursor_empty").getString();
        int[] world = worldAt(mouseX, mouseY, layout);
        double dx = minecraft != null && minecraft.player != null ? world[0] - minecraft.player.getX() : world[0];
        double dz = minecraft != null && minecraft.player != null ? world[1] - minecraft.player.getZ() : world[1];
        return Component.translatable("screen.exworld.map_cursor", compass(dx, dz), world[0], world[1]).getString();
    }

    private static String compass(double dx, double dz) {
        String[] names = {"北", "东北", "东", "东南", "南", "西南", "西", "西北"};
        double angle = Math.atan2(dx, -dz);
        int index = Math.floorMod((int) Math.round(angle / (Math.PI / 4.0)), names.length);
        return names[index];
    }

    private boolean admin() {
        return minecraft != null && minecraft.player != null && (minecraft.player.isCreative() || minecraft.player.hasPermissions(2));
    }


    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 2 && inViewport(mouseY, layout())) {
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
        if (!inViewport(mouseY, layout)) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        double oldCell = cellSize(layout);
        double oldZoom = zoom;
        zoom = Mth.clamp(zoom * Math.pow(1.13, scrollY), MIN_ZOOM, MAX_ZOOM);
        if (zoom == oldZoom) return true;
        double newCell = cellSize(layout);
        MapViewport.Center center = MapViewport.zoomAround(new MapViewport.Center(viewCenterX, viewCenterZ),
                mouseX, mouseY, width / 2.0, layout.viewportCenterY(), oldCell, newCell);
        viewCenterX = center.x();
        viewCenterZ = center.z();
        return true;
    }

    private MapTile tileAt(double mouseX, double mouseY, Layout layout) {
        if (!inViewport(mouseY, layout)) return null;
        double cell = cellSize(layout);
        int mapX = (int) Math.floor(viewCenterX + (mouseX - width / 2.0) / cell + 0.5);
        int mapZ = (int) Math.floor(viewCenterZ + (mouseY - layout.viewportCenterY()) / cell + 0.5);
        return tileGrid.get(key(mapX, mapZ));
    }

    private VisibleRange visibleRange(Layout layout) {
        double cell = cellSize(layout);
        if (archipelago) {
            int minX = (int) Math.floor(viewCenterX - width / 2.0 / cell) - 1;
            int maxX = (int) Math.ceil(viewCenterX + width / 2.0 / cell) + 1;
            double halfHeight = layout.viewportHeight() / 2.0 / cell;
            int minZ = (int) Math.floor(viewCenterZ - halfHeight) - 1;
            int maxZ = (int) Math.ceil(viewCenterZ + halfHeight) + 1;
            return new VisibleRange(minX, minZ, maxX, maxZ);
        }
        int minX = Math.max(snapshot.mapMinimumX(), (int) Math.floor(viewCenterX - width / 2.0 / cell) - 1);
        int maxX = Math.min(snapshot.mapMinimumX() + snapshot.mapWidth() - 1,
                (int) Math.ceil(viewCenterX + width / 2.0 / cell) + 1);
        double halfHeight = layout.viewportHeight() / 2.0 / cell;
        int minZ = Math.max(snapshot.mapMinimumZ(), (int) Math.floor(viewCenterZ - halfHeight) - 1);
        int maxZ = Math.min(snapshot.mapMinimumZ() + snapshot.mapHeight() - 1,
                (int) Math.ceil(viewCenterZ + halfHeight) + 1);
        return new VisibleRange(minX, minZ, maxX, maxZ);
    }

    private static long key(int x, int z) { return (long) x << 32 ^ z & 0xFFFFFFFFL; }
    private String groupName(MapTile tile) {
        return regionFor(tile).name();
    }
    private MapRegion regionFor(MapTile tile) {
        return regionsById.getOrDefault(tile.regionId(), new MapRegion(tile.regionId(), tile.regionId(), ""));
    }
    private boolean configured(MapTile tile) { return regionFor(tile).configured(); }
    private static int roundStable(double value) { return (int) Math.floor(value + 0.5); }
    private static boolean visible(TileRect rect, Layout layout) {
        return rect.right() > 0 && rect.x < layout.screenWidth && rect.bottom() > layout.headerHeight
                && rect.y < layout.screenHeight - layout.footerHeight;
    }
    private static boolean inViewport(double mouseY, Layout layout) {
        return mouseY >= layout.headerHeight && mouseY < layout.screenHeight - layout.footerHeight;
    }
    private static int regionColor(String id) {
        int[] palette = {0x79A4A8, 0xAE936D, 0x729176, 0xA3787B, 0x8984A8, 0xA3A073, 0x709590, 0xA27B96};
        return palette[Math.floorMod(id.hashCode(), palette.length)];
    }
    private static int darken(int rgb, double factor) {
        int red = (int) (((rgb >> 16) & 0xFF) * factor);
        int green = (int) (((rgb >> 8) & 0xFF) * factor);
        int blue = (int) ((rgb & 0xFF) * factor);
        return red << 16 | green << 8 | blue;
    }
    private static void outline(GuiGraphics graphics, TileRect rect, int thickness, int color) {
        if (rect.width <= thickness * 2 || rect.height <= thickness * 2) return;
        graphics.fill(rect.x, rect.y, rect.right(), rect.y + thickness, color);
        graphics.fill(rect.x, rect.bottom() - thickness, rect.right(), rect.bottom(), color);
        graphics.fill(rect.x, rect.y + thickness, rect.x + thickness, rect.bottom() - thickness, color);
        graphics.fill(rect.right() - thickness, rect.y + thickness, rect.right(), rect.bottom() - thickness, color);
    }
    private Layout layout() {
        int header = height < 300 ? 38 : 44;
        int footer = height < 260 ? 44 : height < 360 ? 58 : 70;
        return new Layout(header, footer, width, height, width >= 380, width >= 540 && footer >= 58);
    }
    private int progress() { return snapshot.totalChunks() == 0 ? 100 : snapshot.generatedChunks() * 100 / snapshot.totalChunks(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void removed() { if (biomeAtlas != null) { biomeAtlas.close(); biomeAtlas = null; } }
    private record Layout(int headerHeight, int footerHeight, int screenWidth, int screenHeight,
                          boolean showLabels, boolean showDetails) {
        int viewportHeight() { return screenHeight - headerHeight - footerHeight; }
        int viewportCenterY() { return headerHeight + viewportHeight() / 2; }
    }
    private record MapRect(int x, int y, int width, int height) {}
    private record VisibleRange(int minX, int minZ, int maxX, int maxZ) {
        int tileCount() { return (maxX - minX + 1) * (maxZ - minZ + 1); }
    }
    private record RegionIcon(MapRegion region, MapTile tile) {}
    private record TileRect(int x, int y, int width, int height) {
        int right() { return x + width; }
        int bottom() { return y + height; }
        int centerX() { return x + width / 2; }
        int centerY() { return y + height / 2; }
        TileRect inset(int value) { return new TileRect(x + value, y + value, width - value * 2, height - value * 2); }
    }
}
