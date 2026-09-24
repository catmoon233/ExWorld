package net.exmo.lotm;

import net.exmo.lotm.sequence.PassiveContext;
import net.exmo.lotm.sequence.PassiveDefinition;
import net.exmo.lotm.sequence.PassiveRegistry;
import net.exmo.lotm.sequence.PassiveTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

/** Spectator pathway passives. The health pips are drawn on the client from the synced skill list. */
public final class SpectatorPassives {
    public static final ResourceLocation KEEN_OBSERVATION = ResourceLocation.fromNamespaceAndPath("lotm", "keen_observation");
    public static final ResourceLocation MIND_INTUITION = ResourceLocation.fromNamespaceAndPath("lotm", "mind_intuition");

    private SpectatorPassives() {}

    public static void register() {
        PassiveRegistry.register(new PassiveDefinition(
                KEEN_OBSERVATION,
                "passive.lotm.keen_observation",
                "passive.lotm.keen_observation.desc",
                "minecraft:spyglass",
                PassiveTrigger.TICK,
                0,
                (player, context) -> false));
        PassiveRegistry.register(new PassiveDefinition(
                MIND_INTUITION,
                "passive.lotm.mind_intuition",
                "passive.lotm.mind_intuition.desc",
                "minecraft:ender_eye",
                PassiveTrigger.HURT,
                0,
                SpectatorPassives::weakenAttacker));
    }

    private static boolean weakenAttacker(ServerPlayer player, PassiveContext context) {
        if (!(context.other() instanceof LivingEntity attacker) || !attacker.isAlive()) return false;
        if (player.getRandom().nextFloat() >= 0.08F) return false;
        attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
        return true;
    }

    @SubscribeEvent
    public static void extraExperience(LivingExperienceDropEvent event) {
        if (!(event.getAttackingPlayer() instanceof ServerPlayer player)) return;
        if (!LotmSupport.hasPassive(player, KEEN_OBSERVATION)) return;
        int dropped = event.getDroppedExperience();
        if (dropped <= 0) return;
        int bonus = Math.round(dropped * 0.1F);
        if (bonus == 0 && player.getRandom().nextFloat() < dropped * 0.1F) bonus = 1;
        if (bonus > 0) event.setDroppedExperience(dropped + bonus);
    }
}
