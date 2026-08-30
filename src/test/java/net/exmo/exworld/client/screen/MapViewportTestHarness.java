package net.exmo.exworld.client.screen;

/** Regression harness for small-window zoom responsiveness and cursor anchoring. */
public final class MapViewportTestHarness {
    public static void main(String[] args) {
        double oldCell = MapViewport.cellSize(320, 146, 128, 0.60);
        double newCell = MapViewport.cellSize(320, 146, 128, 0.78);
        require(newCell > oldCell, "zoom must remain responsive when the atlas cell is below one pixel");

        MapViewport.Center before = new MapViewport.Center(7.25, -12.75);
        double mouseX = 267.0;
        double mouseY = 93.0;
        double centerX = 160.0;
        double centerY = 73.0;
        double anchorX = before.x() + (mouseX - centerX) / oldCell;
        double anchorZ = before.z() + (mouseY - centerY) / oldCell;
        MapViewport.Center after = MapViewport.zoomAround(before, mouseX, mouseY, centerX, centerY, oldCell, newCell);
        require(close(anchorX, after.x() + (mouseX - centerX) / newCell), "cursor map X moved during zoom");
        require(close(anchorZ, after.z() + (mouseY - centerY) / newCell), "cursor map Z moved during zoom");

        // A GUI blit's texture extent controls UV normalization, independently from the native atlas resolution.
        // It must match the destination extent so zooming samples the atlas once instead of wrapping it repeatedly.
        require(MapViewport.atlasTextureExtent(896) == 896,
                "zoomed terrain atlas must retain a 0..1 UV span instead of repeating");
        System.out.println("MAP_VIEWPORT_TEST_OK oldCell=" + oldCell + " newCell=" + newCell);
    }

    private static boolean close(double left, double right) { return Math.abs(left - right) < 1.0E-9; }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
