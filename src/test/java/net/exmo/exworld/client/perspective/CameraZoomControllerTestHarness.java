package net.exmo.exworld.client.perspective;

/** Deterministic regression harness for discrete-wheel zoom jitter. */
public final class CameraZoomControllerTestHarness {
    public static void main(String[] args) {
        CameraZoomController zoom = new CameraZoomController(12.0F);
        zoom.scroll(1.0);
        float target = zoom.target();
        require(target < 12.0F, "wheel-up must zoom in");
        float previous = zoom.current();
        for (int tick = 0; tick < 24; tick++) {
            zoom.tick();
            float current = zoom.current();
            require(current <= previous, "zoom must be monotonic and never rebound");
            require(current >= target, "zoom must not overshoot its target");
            float interpolated = zoom.sample(0.5F);
            require(interpolated >= current && interpolated <= previous,
                    "partial-tick sample must remain between adjacent tick states");
            previous = current;
        }
        zoom.scroll(-1000.0);
        require(zoom.target() == CameraZoomController.MAX_DISTANCE, "zoom-out must clamp to max");
        zoom.scroll(1000.0);
        require(zoom.target() == CameraZoomController.MIN_DISTANCE, "zoom-in must clamp to min");
        System.out.println("CAMERA_ZOOM_TEST_OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
