package net.exmo.lotm.client;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Checks the faded night-red blend without a depth mask. */
public final class NightSkyLightHarness {
    public static void main(String[] args) {
        Vector3f midnight = vanillaSky(0.2F);
        NightSkyLight.tintSkyLight(midnight, 0.2F, 0.0F);
        check(midnight.x() > midnight.z() && midnight.z() > midnight.y(), "midnight sky light is faded red");
        check(midnight.z() > 0.25F, "blue is not crushed to a full blood moon");
        check(midnight.x() < 0.60F, "red stays below Enhanced Celestials' 0x990000 sky light");

        Vector3f day = vanillaSky(1.0F);
        Vector3f dayBefore = new Vector3f(day);
        NightSkyLight.tintSkyLight(day, 1.0F, 0.0F);
        check(day.equals(dayBefore, 1.0e-5F), "daylight sky light is unchanged");

        Vector3f rain = vanillaSky(0.2F);
        Vector3f rainBefore = new Vector3f(rain);
        NightSkyLight.tintSkyLight(rain, 0.2F, 1.0F);
        check(rain.equals(rainBefore, 1.0e-5F), "rain removes the red sky light");

        Vector3f moon = NightSkyLight.moonColor(0.2F, 0.0F);
        check(moon.x() > moon.y() && moon.y() > 0.45F, "moon is rose, not a flat dark-red disc");
        check(NightSkyLight.moonColor(1.0F, 0.0F).equals(new Vector3f(1.0F), 1.0e-5F), "day moon color is white");

        Vec3 dome = NightSkyLight.tintDome(new Vec3(0.05, 0.05, 0.12), 0.2F, 0.0F);
        check(dome.x > 0.05 && dome.x - 0.05 < 0.10, "sky dome wash stays faint");
        check(NightSkyLight.tintDome(new Vec3(0.4, 0.6, 1.0), 1.0F, 0.0F).equals(new Vec3(0.4, 0.6, 1.0)),
                "day sky color is unchanged");
    }

    private static Vector3f vanillaSky(float skyDarken) {
        return new Vector3f(skyDarken, skyDarken, 1.0F).lerp(new Vector3f(1.0F, 1.0F, 1.0F), 0.35F);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
