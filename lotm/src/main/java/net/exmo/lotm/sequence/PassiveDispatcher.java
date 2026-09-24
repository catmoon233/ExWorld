package net.exmo.lotm.sequence;

import net.exmo.exworld.Exworld;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class PassiveDispatcher {
    private PassiveDispatcher() {}

    @SubscribeEvent
    public static void hurt(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            fire(player, PassiveTrigger.HURT, event.getSource().getEntity(), event.getAmount(), event);
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != event.getEntity()) {
            fire(attacker, PassiveTrigger.ATTACK, event.getEntity(), event.getAmount(), event);
        }
    }

    @SubscribeEvent
    public static void kill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && event.getEntity() != attacker) {
            fire(attacker, PassiveTrigger.KILL, event.getEntity(), 0.0F, null);
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().getGameTime() % 20L != 0L) return;
        fire(player, PassiveTrigger.TICK, null, player.getHealth(), null);
    }

    public static void fire(ServerPlayer player, PassiveTrigger trigger, net.minecraft.world.entity.Entity other,
                            float amount, LivingIncomingDamageEvent damageEvent) {
        if (player == null || trigger == null) return;
        PlayerSequenceData data = SequenceService.data(player);
        if (!data.hasSequence()) return;
        long now = player.level().getGameTime();
        PassiveContext context = new PassiveContext(trigger, player, other, amount, damageEvent);
        for (SequenceSkill skill : SequenceService.unlockedSkills(player)) {
            if (skill.kind() != SkillKind.PASSIVE) continue;
            PassiveDefinition passive = PassiveRegistry.get(skill.ref()).orElse(null);
            if (passive == null || passive.trigger() != trigger) continue;
            if (now < data.readyAt(passive.id())) continue;
            try {
                if (passive.effect().apply(player, context)) {
                    data.setReadyAt(passive.id(), now + passive.cooldownTicks());
                }
            } catch (RuntimeException exception) {
                Exworld.LOGGER.error("Sequence passive {} failed", passive.id(), exception);
            }
        }
    }

    public static boolean isLiving(net.minecraft.world.entity.Entity entity) {
        return entity instanceof LivingEntity living && living.isAlive();
    }
}
