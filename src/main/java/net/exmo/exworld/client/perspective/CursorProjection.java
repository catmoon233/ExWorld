package net.exmo.exworld.client.perspective;

import org.joml.Vector3f;

/** Shared screen-to-world projection used by the free dungeon cursor and the battle grid cursor. */
public final class CursorProjection {
    private CursorProjection() {}

    /**
     * Chooses the field of view used for a cursor ray. The configured value is retained as a fallback
     * because early client initialization can briefly have no rendered camera value.
     */
    public static float fovForCursorRay(float configuredFov, float renderedFov) {
        return Float.isFinite(renderedFov) && renderedFov > 0.0F ? renderedFov : configuredFov;
    }

    public static Vector3f direction(Vector3f look, Vector3f left, Vector3f up,
                                     double ndcX, double ndcY, double aspect, float fovDegrees) {
        double tangent = Math.tan(Math.toRadians(fovDegrees) * .5D);
        return look.add(left.mul((float) (-ndcX * tangent * aspect)))
                .add(up.mul((float) (ndcY * tangent))).normalize();
    }
}
