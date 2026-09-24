package net.exmo.lotm.client;

import net.exmo.lotm.client.sequence.ClientSequenceState;
import net.exmo.lotm.SpectatorPassives;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Health pips above nearby creatures: green high, yellow mid, red low. */
public final class KeenObservationClient {
    private KeenObservationClient() {}

    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.isPaused()) return;
        if ((minecraft.player.tickCount % 10) != 0 || !active()) return;
        var box = minecraft.player.getBoundingBox().inflate(24.0);
        for (LivingEntity living : minecraft.level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (living == minecraft.player || living instanceof ArmorStand) continue;
            float ratio = living.getMaxHealth() <= 0.0F ? 0.0F : living.getHealth() / living.getMaxHealth();
            int color = ratio > 0.66F ? 0x55FF55 : ratio > 0.33F ? 0xFFDD33 : 0xFF4444;
            minecraft.level.addParticle(new DustParticleOptions(new Vector3f(((color >> 16) & 255) / 255.0F, ((color >> 8) & 255) / 255.0F, (color & 255) / 255.0F), 1.0F),
                    living.getX(), living.getY() + living.getBbHeight() + 0.35, living.getZ(), 0.0, 0.02, 0.0);
        }
    }

    private static boolean active() {
        var snapshot = ClientSequenceState.snapshot();
        if (snapshot == null || !snapshot.hasSequence()) return false;
        String id = SpectatorPassives.KEEN_OBSERVATION.toString();
        for (var entry : snapshot.entries()) {
            for (var skill : entry.skills()) {
                if (id.equals(skill.id())) return true;
            }
        }
        return false;
    }
}
