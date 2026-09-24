package net.exmo.lotm.client;

import net.exmo.lotm.effect.LotmEffects;
import net.exmo.lotm.spell.TrueSightSpell;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import java.util.HashSet;
import java.util.Set;

/** While true sight is active, hidden bodies are drawn for this client only. */
public final class TrueSightClient {
    private static final Set<Integer> RESTORED = new HashSet<>();

    private TrueSightClient() {}

    public static void before(RenderLivingEvent.Pre<?, ?> event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        var sight = minecraft.player.getEffect(LotmEffects.TRUE_SIGHT);
        if (sight == null) return;
        LivingEntity entity = event.getEntity();
        double range = sight.getAmplifier() >= 1 ? 16.0 : TrueSightSpell.RANGE;
        if (entity == minecraft.player || entity.distanceToSqr(minecraft.player) > range * range) return;
        if (!entity.isInvisible() && !entity.hasEffect(MobEffects.INVISIBILITY) && !entity.isInvisibleTo(minecraft.player)) return;
        if (entity.isInvisible()) {
            entity.setInvisible(false);
            RESTORED.add(entity.getId());
        }
    }

    public static void after(RenderLivingEvent.Post<?, ?> event) {
        if (RESTORED.remove(event.getEntity().getId())) event.getEntity().setInvisible(true);
    }
}
