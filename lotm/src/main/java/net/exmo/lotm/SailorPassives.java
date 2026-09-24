package net.exmo.lotm;

import net.exmo.lotm.sequence.PassiveDefinition;
import net.exmo.lotm.sequence.PassiveRegistry;
import net.exmo.lotm.sequence.PassiveTrigger;
import net.exmo.lotm.effect.LotmEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Sailor passives. Water speed is synced each tick; storm guard arms on hit and pays out on the next swing. */
public final class SailorPassives {
    public static final ResourceLocation OCEAN = ResourceLocation.fromNamespaceAndPath("lotm", "ocean_affinity");
    public static final ResourceLocation STORM_GUARD = ResourceLocation.fromNamespaceAndPath("lotm", "storm_guard");
    public static final ResourceLocation AFFINITY_SWIM = ResourceLocation.fromNamespaceAndPath("lotm", "ocean_affinity_swim");
    public static final ResourceLocation BLESSING_SWIM = ResourceLocation.fromNamespaceAndPath("lotm", "sea_blessing_swim");
    public static final String STORM_FLAG = "lotm_storm_guard";
    public static final float STORM_BONUS = 1.2F;

    private SailorPassives() {}

    public static void register() {
        PassiveRegistry.register(new PassiveDefinition(
                OCEAN,
                "passive.lotm.ocean_affinity",
                "passive.lotm.ocean_affinity.desc",
                "minecraft:heart_of_the_sea",
                PassiveTrigger.TICK,
                0,
                (player, context) -> false));
        PassiveRegistry.register(new PassiveDefinition(
                STORM_GUARD,
                "passive.lotm.storm_guard",
                "passive.lotm.storm_guard.desc",
                "minecraft:shield",
                PassiveTrigger.HURT,
                0,
                (player, context) -> {
                    player.getPersistentData().putBoolean(STORM_FLAG, true);
                    return false;
                }));
    }

    public static void syncWater(Player player, boolean affinity) {
        if (player == null) return;
        AttributeInstance swim = player.getAttribute(NeoForgeMod.SWIM_SPEED);
        if (swim == null) return;
        boolean inWater = player.isInWater() || player.isUnderWater();
        boolean blessing = player.hasEffect(LotmEffects.SEA_BLESSING);
        if (!inWater) {
            swim.removeModifier(AFFINITY_SWIM);
            swim.removeModifier(BLESSING_SWIM);
            return;
        }
        if (affinity) {
            swim.addOrUpdateTransientModifier(new AttributeModifier(AFFINITY_SWIM, 0.2, AttributeModifier.Operation.ADD_VALUE));
        } else {
            swim.removeModifier(AFFINITY_SWIM);
        }
        if (blessing && !player.onGround()) {
            float speed = player.getSpeed();
            float waterInput = 0.02F + (speed - 0.02F) * 0.5F;
            float wanted = affinity ? 1.2F : 1.0F;
            float needed = speed <= 0.001F ? wanted : (speed * wanted) / Math.max(0.02F, waterInput);
            double bonus = Math.max(0.0, needed - 1.0 - (affinity ? 0.2 : 0.0));
            swim.addOrUpdateTransientModifier(new AttributeModifier(BLESSING_SWIM, bonus, AttributeModifier.Operation.ADD_VALUE));
        } else {
            swim.removeModifier(BLESSING_SWIM);
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        syncWater(player, LotmSupport.hasPassive(player, OCEAN));
    }

    @SubscribeEvent
    public static void payout(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (event.getEntity() == player || !event.getSource().is(DamageTypes.PLAYER_ATTACK)) return;
        if (!player.getPersistentData().getBoolean(STORM_FLAG)) return;
        if (!LotmSupport.hasPassive(player, STORM_GUARD)) {
            player.getPersistentData().remove(STORM_FLAG);
            return;
        }
        event.setAmount(event.getAmount() * STORM_BONUS);
        player.getPersistentData().remove(STORM_FLAG);
    }
}
