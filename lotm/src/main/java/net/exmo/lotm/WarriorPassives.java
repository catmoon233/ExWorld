package net.exmo.lotm;

import net.exmo.lotm.sequence.PassiveContext;
import net.exmo.lotm.sequence.PassiveDefinition;
import net.exmo.lotm.sequence.PassiveRegistry;
import net.exmo.lotm.sequence.PassiveTrigger;
import net.exmo.lotm.sequence.RegisterSequencesEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;

public final class WarriorPassives {
    public static final ResourceLocation INSTINCT = ResourceLocation.fromNamespaceAndPath("lotm", "battle_instinct");
    public static final ResourceLocation PRESSURE = ResourceLocation.fromNamespaceAndPath("lotm", "close_pressure");
    public static final ResourceLocation READ = ResourceLocation.fromNamespaceAndPath("lotm", "read_the_blow");

    private WarriorPassives() {}

    @SubscribeEvent
    public static void register(RegisterSequencesEvent event) {
        register();
    }

    public static void register() {
        PassiveRegistry.register(new PassiveDefinition(
                INSTINCT,
                "passive.lotm.battle_instinct",
                "passive.lotm.battle_instinct.desc",
                "minecraft:iron_sword",
                PassiveTrigger.HURT,
                160,
                (player, context) -> {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0));
                    player.heal(2.0F);
                    return true;
                }));
        PassiveRegistry.register(new PassiveDefinition(
                PRESSURE,
                "passive.lotm.close_pressure",
                "passive.lotm.close_pressure.desc",
                "minecraft:iron_axe",
                PassiveTrigger.ATTACK,
                100,
                (player, context) -> {
                    if (!(context.other() instanceof LivingEntity target) || !target.isAlive()) return false;
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                    return true;
                }));
        PassiveRegistry.register(new PassiveDefinition(
                READ,
                "passive.lotm.read_the_blow",
                "passive.lotm.read_the_blow.desc",
                "minecraft:shield",
                PassiveTrigger.HURT,
                240,
                WarriorPassives::readTheBlow));
    }

    private static boolean readTheBlow(net.minecraft.server.level.ServerPlayer player, PassiveContext context) {
        if (context.damageEvent() == null) return false;
        context.damageEvent().setAmount(context.damageEvent().getAmount() * 0.7F);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 0));
        return true;
    }
}
