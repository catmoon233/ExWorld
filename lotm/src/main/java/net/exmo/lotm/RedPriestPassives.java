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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Red Priest passives. Frenzy modifiers are removed here when the sequence or the low-health window ends. */
public final class RedPriestPassives {
    public static final ResourceLocation BEAST = ResourceLocation.fromNamespaceAndPath("lotm", "beast_instinct");
    public static final ResourceLocation FRENZY = ResourceLocation.fromNamespaceAndPath("lotm", "battle_frenzy");
    private static final ResourceLocation FRENZY_DAMAGE = ResourceLocation.fromNamespaceAndPath("lotm", "battle_frenzy_damage");
    private static final ResourceLocation FRENZY_SPEED = ResourceLocation.fromNamespaceAndPath("lotm", "battle_frenzy_speed");
    private static final double SENSE_RANGE = 16.0;

    private RedPriestPassives() {}

    public static void register() {
        PassiveRegistry.register(new PassiveDefinition(
                BEAST,
                "passive.lotm.beast_instinct",
                "passive.lotm.beast_instinct.desc",
                "minecraft:bow",
                PassiveTrigger.HURT,
                80,
                RedPriestPassives::dangerSense));
        PassiveRegistry.register(new PassiveDefinition(
                FRENZY,
                "passive.lotm.battle_frenzy",
                "passive.lotm.battle_frenzy.desc",
                "minecraft:iron_sword",
                PassiveTrigger.TICK,
                0,
                (player, context) -> {
                    syncFrenzy(player);
                    return false;
                }));
    }

    private static boolean dangerSense(ServerPlayer player, PassiveContext context) {
        if (context.other() instanceof LivingEntity attacker && attacker.distanceToSqr(player) <= 32.0 * 32.0 && isThreat(player, attacker)) {
            glow(attacker);
        }
        AABB box = player.getBoundingBox().inflate(SENSE_RANGE);
        for (LivingEntity living : player.level().getEntitiesOfClass(LivingEntity.class, box,
                candidate -> candidate != player && candidate.distanceToSqr(player) <= SENSE_RANGE * SENSE_RANGE && isThreat(player, candidate))) {
            glow(living);
        }
        return true;
    }

    private static boolean isThreat(ServerPlayer player, LivingEntity living) {
        if (!living.isAlive() || LotmSupport.allied(player, living)) return false;
        if (living instanceof Enemy || LotmSupport.hostile(living)) return true;
        return living == player.getLastHurtByMob() || player == living.getLastHurtByMob();
    }

    private static void glow(LivingEntity living) {
        living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, false, true));
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().getGameTime() % 10L != 0L) return;
        syncFrenzy(player);
    }

    private static void syncFrenzy(ServerPlayer player) {
        boolean active = LotmSupport.hasPassive(player, FRENZY) && player.getHealth() < player.getMaxHealth() * 0.5F;
        set(player, Attributes.ATTACK_DAMAGE, FRENZY_DAMAGE, active);
        set(player, Attributes.ATTACK_SPEED, FRENZY_SPEED, active);
    }

    private static void set(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                            ResourceLocation id, boolean active) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        if (!active) {
            instance.removeModifier(id);
            return;
        }
        instance.addOrUpdateTransientModifier(new AttributeModifier(id, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
