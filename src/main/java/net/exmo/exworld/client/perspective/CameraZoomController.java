package net.exmo.exworld.client.perspective;

/** Owns zoom state so input, ticks and render interpolation never write the same value. */
final class CameraZoomController {
    static final float MIN_DISTANCE = 3.0F;
    static final float MAX_DISTANCE = 160.0F;
    private static final float RESPONSE = 0.24F;
    private float target;
    private float previous;
    private float current;

    CameraZoomController(float initial) { reset(initial); }

    void reset(float distance) {
        target = current = previous = clamp(distance, MIN_DISTANCE, MAX_DISTANCE);
    }

    void scroll(double delta) {
        target = clamp(target - (float) delta * 1.4F, MIN_DISTANCE, MAX_DISTANCE);
    }

    void tick() {
        previous = current;
        current += (target - current) * RESPONSE;
        if (Math.abs(target - current) < 0.002F) current = target;
    }

    float sample(float partialTick) {
        float alpha = clamp(partialTick, 0.0F, 1.0F);
        return previous + alpha * (current - previous);
    }
    float current() { return current; }
    float target() { return target; }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
