package net.exmo.lotm;

import net.exmo.lotm.sequence.PassiveContext;
import net.exmo.lotm.sequence.PassiveDefinition;
import net.exmo.lotm.sequence.PassiveRegistry;
import net.exmo.lotm.sequence.PassiveTrigger;
import net.exmo.lotm.sequence.RegisterSequencesEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;

public final class HunterPassives {
    public static final ResourceLocation EYE = ResourceLocation.fromNamespaceAndPath("lotm", "hunters_eye");
    public static final ResourceLocation REFLEX = ResourceLocation.fromNamespaceAndPath("lotm", "provoke_reflex");

    private HunterPassives() {}

    @SubscribeEvent
    public static void register(RegisterSequencesEvent event) {
        register();
    }

    public static void register() {
        PassiveRegistry.register(new PassiveDefinition(
                EYE,
                "passive.lotm.hunters_eye",
                "passive.lotm.hunters_eye.desc",
                "minecraft:spyglass",
                PassiveTrigger.ATTACK,
                0,
                HunterPassives::huntersEye));
        PassiveRegistry.register(new PassiveDefinition(
                REFLEX,
                "passive.lotm.provoke_reflex",
                "passive.lotm.provoke_reflex.desc",
                "minecraft:shield",
                PassiveTrigger.HURT,
                0,
                HunterPassives::provokeReflex));
    }

    private static boolean huntersEye(ServerPlayer player, PassiveContext context) {
        if (context.damageEvent() == null || !quarry(context.other())) return false;
        context.damageEvent().setAmount(context.damageEvent().getAmount() * 1.10F);
        return true;
    }

    private static boolean provokeReflex(ServerPlayer player, PassiveContext context) {
        if (context.damageEvent() == null || context.amount() <= 0.0F) return false;
        if (context.damageEvent().getSource().is(DamageTypes.THORNS)) return false;
        if (!(context.other() instanceof LivingEntity attacker) || !attacker.isAlive() || attacker == player) return false;
        if (player.getRandom().nextFloat() >= 0.35F) return false;
        attacker.hurt(player.damageSources().thorns(player), Math.max(1.0F, context.amount() * 0.40F));
        return true;
    }

    private static boolean quarry(net.minecraft.world.entity.Entity entity) {
        return entity instanceof Enemy || entity instanceof Animal || entity instanceof WaterAnimal;
    }
}
