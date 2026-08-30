package net.exmo.exworld.client.perspective;

public final class CursorProjectionTestHarness {
    public static void main(String[] args) {
        float configuredFov = 70.0F;
        float slowedRenderedFov = 38.0F;
        require(CursorProjection.fovForCursorRay(configuredFov, slowedRenderedFov) == slowedRenderedFov,
                "cursor projection must use the current rendered FOV after a POV-changing effect");
        require(CursorProjection.fovForCursorRay(configuredFov, Float.NaN) == configuredFov,
                "cursor projection falls back to the configured FOV before a rendered value exists");
        System.out.println("CURSOR_PROJECTION_TEST_OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
