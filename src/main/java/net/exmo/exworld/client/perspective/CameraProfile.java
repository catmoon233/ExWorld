package net.exmo.exworld.client.perspective;

/** Complete camera policy used by the perspective module and future gameplay overrides. */
public record CameraProfile(boolean dungeonView, float yaw, float pitch, float distance, boolean cameraRelativeMovement) {
    public static final CameraProfile EXPLORATION = new CameraProfile(true, 45.0F, 50.0F, 12.0F, true);
    public static final CameraProfile VANILLA = new CameraProfile(false, 0.0F, 0.0F, 4.0F, false);
}
