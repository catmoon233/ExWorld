package net.exmo.exworld.battle.compat;

import net.exmo.exworld.battle.combat.BattleEffectResolver;
import net.exmo.exworld.battle.combatant.Combatant;
import net.exmo.exworld.battle.skill.SkillDefinition;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

/** Uses LivingEntity.hurt/heal so armor, shields, effects, enchantments and other mods see normal events. */
public final class MinecraftBattleEffectResolver implements BattleEffectResolver {
    private final Supplier<MinecraftServer> server;

    public MinecraftBattleEffectResolver(Supplier<MinecraftServer> server,
                                         net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> ignoredLegacyLevel) {
        this.server = server;
    }

    @Override public double damage(Combatant actor, Combatant target, SkillDefinition skill, double requested) {
        LivingEntity victim = living(target); LivingEntity attacker = living(actor);
        if (victim == null) return logicalDamage(target, requested);
        float before = victim.getHealth();
        float amount = (float) Math.max(0, requested * target.incomingDamageMultiplier());
        DamageSource inherited = BattleDamageContext.source();
        DamageSource source = inherited != null ? inherited : damageSource(victim, attacker, skill);
        boolean fatal = BattleDamageContext.call(source, () -> {
            victim.hurt(source, amount);
            return BattleDamageContext.fatal();
        });
        float health = fatal ? 0 : Math.max(0, victim.getHealth());
        target.syncHealth(victim.getMaxHealth(), health);
        return fatal ? before : Math.max(0, before - health);
    }

    @Override public double heal(Combatant actor, Combatant target, SkillDefinition skill, double requested) {
        LivingEntity living = living(target);
        if (living == null) { float before=target.health();target.heal((float)requested);return target.health()-before; }
        float before = living.getHealth();
        BattleDamageContext.call(living.damageSources().magic(), () -> { living.heal((float) Math.max(0, requested)); return true; });
        target.syncHealth(living.getMaxHealth(), living.getHealth());
        return Math.max(0, living.getHealth() - before);
    }

    private LivingEntity living(Combatant combatant) {
        MinecraftServer current = server.get(); if (current == null) return null;
        if (combatant.playerId() != null) return current.getPlayerList().getPlayer(combatant.playerId());
        for (ServerLevel level : current.getAllLevels()) {
            Entity entity = level.getEntity(combatant.id());
            if (entity instanceof LivingEntity living) return living;
        }
        return null;
    }
    private static DamageSource damageSource(LivingEntity victim, LivingEntity attacker, SkillDefinition skill) {
        if (attacker instanceof Player player && isMelee(skill)) return victim.damageSources().playerAttack(player);
        if (attacker != null && isMelee(skill)) return victim.damageSources().mobAttack(attacker);
        return attacker == null ? victim.damageSources().magic()
                : victim.damageSources().source(net.minecraft.world.damagesource.DamageTypes.MAGIC, attacker);
    }
    private static boolean isMelee(SkillDefinition skill) {
        return skill.physical() || skill.id().equals("exworld:basic_attack") || skill.id().equals("exworld:guarded_strike")
                || (skill.adapterId().equals("exworld") && skill.range() <= 1);
    }
    private static double logicalDamage(Combatant target,double requested){float before=target.health();target.damage((float)requested);return before-target.health();}
}
