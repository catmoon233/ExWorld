package net.exmo.exworld.battle.compat;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.exmo.exworld.battle.skill.SkillAdapter;
import net.exmo.exworld.battle.BattleSystem;
import net.exmo.exworld.battle.skill.SkillResult;
import net.exmo.exworld.battle.skill.SkillUse;
import net.exmo.exworld.battle.model.BattleStatus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Whitelist adapter. Definitions use adapterId "iron:<spell-id>" and are registered explicitly. */
public final class IronSpellAdapter implements SkillAdapter {
    /** Self-targeting heals are resolved through the authoritative combat model instead of Iron's own healing. */
    private static final Set<String> HEAL_SPELLS = Set.of(
            "irons_spellbooks:heal", "irons_spellbooks:greater_heal", "irons_spellbooks:blessing_of_life");

    private final String spellId;
    private final Map<CastKey,Integer> recastStages = new ConcurrentHashMap<>();
    public IronSpellAdapter(String spellId) { this.spellId = spellId; }

    @Override public boolean requiresMana(net.exmo.exworld.battle.BattleSession session,
                                          net.exmo.exworld.battle.combatant.Combatant actor,
                                          net.exmo.exworld.battle.skill.SkillDefinition definition) {
        if (actor.playerId() == null) {
            return recastStages.getOrDefault(new CastKey(session.id().value(), actor.id()), 0) == 0;
        }
        ServerPlayer player = BattleSystem.player(actor.playerId());
        var spell = SpellRegistry.getSpell(spellId);
        return player == null || spell == SpellRegistry.none()
                || recastStages.getOrDefault(new CastKey(session.id().value(), actor.id()), 0) == 0;
    }

    @Override public SkillResult execute(SkillUse use) {
        var spell = SpellRegistry.getSpell(spellId);
        if (spell == SpellRegistry.none()) return SkillResult.failure("battle.command.unknown_iron_spell");
        LivingEntity caster = use.actor().playerId() == null
                ? BattleSystem.livingEntity(use.session(), use.actor().id())
                : BattleSystem.player(use.actor().playerId());
        if (caster == null) return SkillResult.failure("battle.command.actor_unavailable");
        Vec3 target = BattleSystem.aimPosition(use.session(), use.target(), use.targetCell(), use.definition().targetType());
        boolean selfTarget = use.definition().targetType() == net.exmo.exworld.battle.skill.SkillDefinition.TargetType.SELF
                || (use.target() != null && use.target().id().equals(use.actor().id()));
        BattleSystem.lockCastAim(caster, target, spell.getEffectiveCastTime(use.definition().level(), caster), selfTarget);
        if (spellId.equals("irons_spellbooks:frost_step")) {
            SkillResult movement = use.session().teleportDisplacement(use.actor(), use.targetCell());
            if (movement.success()) BattleSystem.presentTacticalIronEffect(caster, target, true);
            return movement;
        }
        if (spellId.equals("irons_spellbooks:summon_vex")) {
            var summon = use.session().summon(use.actor(), use.targetCell(), "Summoned Vex").orElse(null);
            if (summon == null) return SkillResult.failure("battle.command.no_summon_cell");
            BattleSystem.spawnBattleVex(use.session(), summon);
            BattleSystem.presentTacticalIronEffect(caster, target, false);
            return SkillResult.success(0);
        }
        // Players use Iron's normal delayed cast lifecycle. Mobs use the library's LivingEntity
        // lifecycle; ExWorld's action lock owns the battle presentation duration.
        boolean expectHit = !HEAL_SPELLS.contains(spellId) && !spellId.equals("irons_spellbooks:shield")
                && !spellId.equals("irons_spellbooks:slow");
        boolean started;
        if (caster instanceof ServerPlayer player) {
            started = ManaMutationContext.call(() -> spell.attemptInitiateCast(ItemStack.EMPTY, use.definition().level(),
                    player.level(), player, CastSource.COMMAND, false, "exworld_battle"));
        } else {
            MagicData magicData = MagicData.getPlayerMagicData(caster);
            if (!spell.checkPreCastConditions(caster.level(), use.definition().level(), caster, magicData))
                return SkillResult.failure("battle.command.iron_cast_rejected");
            if (expectHit) BattleSystem.expectIronHit(use.session().id(), use.actor().id(),
                    use.target() == null ? null : use.target().id(), use.definition().id());
            spell.onCast(caster.level(), use.definition().level(), caster, CastSource.MOB, magicData);
            spell.onServerCastComplete(caster.level(), use.definition().level(), caster, magicData, false);
            started = true;
        }
        if (!started) return SkillResult.failure("battle.command.iron_cast_rejected");
        // Iron owns casting presentation; ExWorld mirrors support outcomes into its authoritative combat model.
        double debugPower = Math.max(0, use.definition().power()) * .4;
        if (HEAL_SPELLS.contains(spellId)) {
            return SkillResult.success(debugPower);
        }
        if (spellId.equals("irons_spellbooks:shield")) {
            use.session().applyStatus(use.actor(), use.actor(), new BattleStatus("exworld:iron_shield", "status.exworld.iron_shield", 1, 2, true));
            return SkillResult.success(0);
        }
        if (spellId.equals("irons_spellbooks:slow") && use.target() != null) {
            use.session().applyStatus(use.actor(), use.target(), new BattleStatus("exworld:iron_slow", "status.exworld.iron_slow", 1, 2, false));
            return SkillResult.success(0);
        }
        if (expectHit && caster instanceof ServerPlayer)
            BattleSystem.expectIronHit(use.session().id(), use.actor().id(), use.target() == null ? null : use.target().id(), use.definition().id());
        int configuredRecasts = spell.getRecastCount(use.definition().level(), caster);
        if (configuredRecasts > 0) {
            CastKey key = new CastKey(use.session().id().value(), use.actor().id());
            int stage = recastStages.merge(key, 1, Integer::sum);
            boolean finalStage = stage >= configuredRecasts;
            if (finalStage) recastStages.remove(key);
            return SkillResult.staged(0, finalStage, stage == 1);
        }
        return SkillResult.success(0);
    }

    @Override public int actionTicks(SkillUse use, SkillResult result) {
        LivingEntity caster = use.actor().playerId() == null
                ? BattleSystem.livingEntity(use.session(), use.actor().id())
                : BattleSystem.player(use.actor().playerId());
        var spell = SpellRegistry.getSpell(spellId);
        if (caster == null || spell == SpellRegistry.none()) return 8;
        return IronSpellTiming.actionTicks(spell.getEffectiveCastTime(use.definition().level(), caster));
    }

    private record CastKey(java.util.UUID battleId, java.util.UUID actorId) {}
}
