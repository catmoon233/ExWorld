package net.exmo.lotm;

import net.exmo.lotm.sequence.PassiveDefinition;
import net.exmo.lotm.sequence.PassiveRegistry;
import net.exmo.lotm.sequence.PassiveTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Door pathway passives. Portal sense is drawn on the client; this class keeps the noise pull. */
public final class ApprenticePassives {
    public static final ResourceLocation SPIRIT = ResourceLocation.fromNamespaceAndPath("lotm", "spirit_sense");
    public static final ResourceLocation SPATIAL = ResourceLocation.fromNamespaceAndPath("lotm", "spatial_intuition");
    private static final List<Pull> PULLS = new ArrayList<>();

    private ApprenticePassives() {}

    public static void register() {
        PassiveRegistry.register(new PassiveDefinition(
                SPIRIT,
                "passive.lotm.spirit_sense",
                "passive.lotm.spirit_sense.desc",
                "minecraft:ender_eye",
                PassiveTrigger.TICK,
                0,
                (player, context) -> false));
        PassiveRegistry.register(new PassiveDefinition(
                SPATIAL,
                "passive.lotm.spatial_intuition",
                "passive.lotm.spatial_intuition.desc",
                "minecraft:ender_pearl",
                PassiveTrigger.TICK,
                0,
                (player, context) -> false));
    }

    public static boolean hasSpatial(Player player) {
        return LotmSupport.hasPassive(player, SPATIAL);
    }

    public static double doorRange(LivingEntity caster) {
        return caster instanceof Player player && hasSpatial(player) ? 10.0 : 8.0;
    }

    public static void attract(ServerLevel level, Vec3 point, int ticks) {
        AABB box = new AABB(point, point).inflate(12.0);
        long until = level.getGameTime() + ticks;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, mob -> mob.isAlive() && LotmSupport.hostile(mob))) {
            if (mob.distanceToSqr(point) > 12.0 * 12.0) continue;
            mob.setTarget(null);
            PULLS.add(new Pull(mob.getUUID(), level.dimension(), point, until));
        }
    }

    @SubscribeEvent
    public static void pull(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || PULLS.isEmpty()) return;
        long now = level.getGameTime();
        Iterator<Pull> pulls = PULLS.iterator();
        while (pulls.hasNext()) {
            Pull pull = pulls.next();
            if (!pull.dimension.equals(level.dimension())) continue;
            if (now >= pull.until || !(level.getEntity(pull.mob) instanceof Mob mob) || !mob.isAlive()) {
                pulls.remove();
                continue;
            }
            mob.setTarget(null);
            mob.getLookControl().setLookAt(pull.point);
            mob.getNavigation().moveTo(pull.point.x, pull.point.y, pull.point.z, 1.05);
            Vec3 to = pull.point.subtract(mob.position());
            float yaw = (float) (Mth.atan2(to.z, to.x) * (180.0 / Math.PI)) - 90.0F;
            mob.setYRot(yaw);
            mob.setYHeadRot(yaw);
        }
    }

    private record Pull(java.util.UUID mob, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, Vec3 point, long until) {}
}
