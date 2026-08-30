package net.exmo.exworld.client.screen;

/** Pure map-space navigation math shared by the GUI and its regression harness. */
final class MapViewport {
    private MapViewport() {}

    static double cellSize(double viewportWidth, double viewportHeight, int mapSize, double zoom) {
        return cellSize(viewportWidth, viewportHeight, mapSize, mapSize, zoom);
    }

    static double cellSize(double viewportWidth, double viewportHeight, int mapWidth, int mapHeight, double zoom) {
        return Math.max(0.25, Math.min((viewportWidth - 18.0) / mapWidth,
                (viewportHeight - 12.0) / mapHeight) * zoom);
    }

    static Center zoomAround(Center current, double mouseX, double mouseY, double viewportCenterX,
                             double viewportCenterY, double oldCell, double newCell) {
        double anchoredMapX = current.x + (mouseX - viewportCenterX) / oldCell;
        double anchoredMapZ = current.z + (mouseY - viewportCenterY) / oldCell;
        return new Center(anchoredMapX - (mouseX - viewportCenterX) / newCell,
                anchoredMapZ - (mouseY - viewportCenterY) / newCell);
    }

    /**
     * {@link net.minecraft.client.gui.GuiGraphics#blit} derives UVs from its declared texture extent.
     * The destination extent therefore also has to be declared as the texture extent when scaling the
     * complete atlas; otherwise values above the native atlas size wrap and repeat the map.
     */
    static int atlasTextureExtent(int destinationExtent) { return Math.max(1, destinationExtent); }

    record Center(double x, double z) {}
}
