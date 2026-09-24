package net.exmo.lotm;

import net.exmo.lotm.sequence.PassiveContext;
import net.exmo.lotm.sequence.PassiveDefinition;
import net.exmo.lotm.sequence.PassiveRegistry;
import net.exmo.lotm.sequence.PassiveTrigger;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/** Sun pathway passives. Undead damage and wither immunity are event-driven so they always apply. */
public final class SunPassives {
    public static final ResourceLocation COURAGE = ResourceLocation.fromNamespaceAndPath("lotm", "courage_resonance");
    public static final ResourceLocation HOLY = ResourceLocation.fromNamespaceAndPath("lotm", "holy_affinity");

    private SunPassives() {}

    public static void register() {
        PassiveRegistry.register(new PassiveDefinition(
                COURAGE,
                "passive.lotm.courage_resonance",
                "passive.lotm.courage_resonance.desc",
                "minecraft:golden_apple",
                PassiveTrigger.TICK,
                100,
                SunPassives::sunlight));
        PassiveRegistry.register(new PassiveDefinition(
                HOLY,
                "passive.lotm.holy_affinity",
                "passive.lotm.holy_affinity.desc",
                "minecraft:glowstone",
                PassiveTrigger.TICK,
                0,
                SunPassives::stripWither));
    }

    private static boolean sunlight(ServerPlayer player, PassiveContext context) {
        if (!inSun(player)) return false;
        if (player.getHealth() < player.getMaxHealth()) {
            player.heal(1.0F);
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 3, 0.25, 0.3, 0.25, 0.01);
            }
        }
        return true;
    }

    private static boolean stripWither(ServerPlayer player, PassiveContext context) {
        if (!player.hasEffect(MobEffects.WITHER)) return false;
        player.removeEffect(MobEffects.WITHER);
        return false;
    }

    static boolean inSun(ServerPlayer player) {
        var pos = player.blockPosition();
        return player.level().isDay() && player.level().canSeeSky(pos) && !player.level().isRainingAt(pos);
    }

    @SubscribeEvent
    public static void undeadAndWither(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && LotmSupport.hasPassive(player, HOLY)) {
            var source = event.getSource();
            if (source.is(DamageTypes.WITHER) && source.getEntity() == null) {
                event.setCanceled(true);
                player.removeEffect(MobEffects.WITHER);
                return;
            }
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim) || !LotmSupport.undead(victim)) return;
        float bonus = 1.0F;
        if (LotmSupport.hasPassive(attacker, COURAGE)) bonus += 0.05F;
        if (LotmSupport.hasPassive(attacker, HOLY)) bonus += 0.15F;
        if (bonus > 1.0F) event.setAmount(event.getAmount() * bonus);
    }

    @SubscribeEvent
    public static void blockWither(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance() == null || event.getEffectInstance().getEffect() != MobEffects.WITHER) return;
        if (event.getEntity() instanceof ServerPlayer player && LotmSupport.hasPassive(player, HOLY)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}
