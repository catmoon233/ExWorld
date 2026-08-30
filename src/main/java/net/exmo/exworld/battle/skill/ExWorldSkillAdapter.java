package net.exmo.exworld.battle.skill;

import net.exmo.exworld.battle.combat.BattleDamageType;
import net.exmo.exworld.battle.model.BattleCell;
import net.exmo.exworld.battle.model.BattleCells;
import net.exmo.exworld.battle.model.BattleStatus;
import net.exmo.exworld.battle.weapon.WeaponFamily;

import java.util.List;

/** Built-in deterministic melee/heal implementation used by entities and pure rule tests. */
public final class ExWorldSkillAdapter implements SkillAdapter {
    @Override public SkillDefinition resolveForActor(net.exmo.exworld.battle.BattleSession session,
                                                     net.exmo.exworld.battle.combatant.Combatant actor,
                                                     SkillDefinition definition) {
        if (!BattleSkillIds.BASIC_STRIKE.equals(definition.id())) return definition;
        if (actor.weaponFamily() == WeaponFamily.HEAVY) return definition.withCostAndRange(2, 3);
        if (actor.weaponFamily() == WeaponFamily.SWORD) return definition.withCostAndRange(1, 2);
        return definition.withCostAndRange(1, 2);
    }

    @Override public SkillResult execute(SkillUse use) {
        return switch (use.definition().id()) {
            case BattleSkillIds.BASIC_ATTACK -> basicAttack(use);
            case BattleSkillIds.DODGE -> dodge(use);
            case BattleSkillIds.GUARD -> guard(use, 1);
            case BattleSkillIds.INTERCEPT -> intercept(use);
            case BattleSkillIds.QUICK_MEDITATION -> meditate(use);
            case BattleSkillIds.BASIC_STRIKE -> basicStrike(use);
            case BattleSkillIds.CLEAVE -> cleave(use);
            case BattleSkillIds.LEAP_SLASH -> leapSlash(use);
            case BattleSkillIds.TAUNT -> taunt(use);
            case BattleSkillIds.RAISE_SHIELD -> guard(use, 2);
            case BattleSkillIds.BATTLE_AURA -> battleAura(use);
            default -> legacy(use);
        };
    }

    private static SkillResult basicAttack(SkillUse use) {
        if (use.target() == null) return SkillResult.failure("battle.command.target_required");
        return SkillResult.success(use.actor().weaponAttack());
    }

    private static SkillResult dodge(SkillUse use) {
        if (use.targetCell() == null || !BattleCells.adjacentRing(use.actor().cell(), use.targetCell()))
            return SkillResult.failure("battle.command.target_required");
        int[] direction = BattleCells.direction(use.actor().cell(), use.targetCell());
        use.actor().setDodgeDirection(direction[0], direction[1]);
        use.session().replaceStatus(use.actor(), use.actor(), stance(BattleSkillIds.DODGE_STATUS, "status.exworld.dodge", 1, false));
        return SkillResult.effect();
    }

    private static SkillResult guard(SkillUse use, int stacks) {
        use.session().replaceStatus(use.actor(), use.actor(), stance(BattleSkillIds.GUARDING, "status.exworld.guarding", stacks, false));
        return SkillResult.effect();
    }

    private static SkillResult intercept(SkillUse use) {
        if (use.targetCell() == null || !BattleCells.adjacentRing(use.actor().cell(), use.targetCell()))
            return SkillResult.failure("battle.command.target_required");
        int remaining = use.actor().unlimitedActionPoints() ? 1 : use.actor().actionPointsRemaining();
        if (remaining < 1) return SkillResult.failure("battle.command.no_action_points");
        int[] direction = BattleCells.direction(use.actor().cell(), use.targetCell());
        use.actor().setInterceptDirection(direction[0], direction[1]);
        use.session().replaceStatus(use.actor(), use.actor(), stance(BattleSkillIds.INTERCEPT_STATUS, "status.exworld.intercept", remaining, false));
        return SkillResult.effect();
    }

    private static SkillResult meditate(SkillUse use) {
        return SkillResult.effect();
    }

    private static SkillResult basicStrike(SkillUse use) {
        WeaponFamily family = use.actor().weaponFamily();
        if (family != WeaponFamily.SWORD && family != WeaponFamily.HEAVY)
            return SkillResult.failure("battle.command.wrong_weapon");
        if (use.target() == null) return SkillResult.failure("battle.command.target_required");
        int multiplier = family == WeaponFamily.HEAVY ? 3 : 2;
        double amount = use.actor().strengthLevel() * multiplier + use.actor().weaponAttack();
        return SkillResult.success(amount);
    }

    private static SkillResult cleave(SkillUse use) {
        if (use.actor().weaponFamily() != WeaponFamily.SWORD) return SkillResult.failure("battle.command.wrong_weapon");
        if (use.targetCell() == null || !BattleCells.adjacentRing(use.actor().cell(), use.targetCell()))
            return SkillResult.failure("battle.command.target_required");
        double amount = (use.actor().strengthLevel() + use.actor().weaponAttack()) / 2.0D;
        List<BattleCell> cells = BattleCells.cleaveCells(use.actor().cell(), use.targetCell());
        use.session().hitHostiles(use.actor(), cells, use.definition(), amount, BattleDamageType.PHYSICAL, cells);
        return SkillResult.effect();
    }

    private static SkillResult leapSlash(SkillUse use) {
        if (use.target() == null) return SkillResult.failure("battle.command.target_required");
        double amount = use.actor().strengthLevel() * 5 + use.actor().weaponAttack() * 1.2D;
        return SkillResult.success(amount);
    }

    private static SkillResult taunt(SkillUse use) {
        int remaining = use.actor().unlimitedActionPoints() ? 1 : use.actor().actionPointsRemaining();
        if (remaining < 1) return SkillResult.failure("battle.command.no_action_points");
        int stacks = Math.max(1, (int) Math.ceil(remaining / 2.0D));
        use.session().replaceStatus(use.actor(), use.actor(),
                new BattleStatus(BattleSkillIds.TAUNT_STATUS, "status.exworld.taunt", stacks, Integer.MAX_VALUE, true, true));
        return SkillResult.effect();
    }

    private static SkillResult battleAura(SkillUse use) {
        if (use.actor().weaponFamily() == WeaponFamily.NONE) return SkillResult.failure("battle.command.wrong_weapon");
        use.session().replaceStatus(use.actor(), use.actor(),
                new BattleStatus(BattleSkillIds.BATTLE_AURA_STATUS, "status.exworld.battle_aura", 1, Integer.MAX_VALUE, true, true));
        return SkillResult.effect();
    }

    private static BattleStatus stance(String id, String nameKey, int stacks, boolean permanent) {
        return new BattleStatus(id, nameKey, stacks, permanent ? Integer.MAX_VALUE : 1, true, permanent);
    }

    private static SkillResult legacy(SkillUse use) {
        if (use.definition().id().equals("exworld:dash")) {
            if (use.targetCell() == null) return SkillResult.failure("battle.command.target_required");
            return use.session().scheduleDash(use.actor(), use.targetCell());
        }
        if (use.cardDefinition() != null) {
            double amount = use.cardDefinition().effect("damage");
            if (amount > 0) {
                if (use.target() == null) return SkillResult.failure("battle.command.target_required");
                return SkillResult.success(amount);
            }
            double block = use.cardDefinition().effect("block");
            if (block > 0) use.actor().addBlock((float) block);
            double mana = use.cardDefinition().effect("mana");
            if (mana > 0) use.actor().restoreMana((float) mana);
            int draw = (int) use.cardDefinition().effect("draw");
            if (draw > 0) use.actor().deck().drawCards(draw);
            if (use.cardDefinition().effect("purge") > 0) use.actor().deck().purgeOne();
            int strength = (int) use.cardDefinition().effect("status_strength");
            if (strength > 0) use.actor().addStrength(strength);
            if (strength > 0) use.session().applyStatus(use.actor(), use.actor(), new BattleStatus("exworld:strength", "status.exworld.strength", strength, Integer.MAX_VALUE, true, true));
            if (use.cardDefinition().effect("status_card") > 0 && use.target() != null)
                use.target().deck().addToDrawPile("exworld:slimed");
            if (use.cardDefinition().effect("curse_card") > 0) {
                use.actor().deck().addToDrawPile("exworld:regret");
                use.actor().addPermanentCard("exworld:regret");
            }
            if (use.cardDefinition().type() == net.exmo.exworld.battle.card.CardDefinition.CardType.POWER)
                use.actor().markPowerUsed(use.cardDefinition().id());
            if (block > 0 || mana > 0 || draw > 0 || strength > 0 || use.cardDefinition().effect("purge") > 0
                    || use.cardDefinition().effect("status_card") > 0 || use.cardDefinition().effect("curse_card") > 0)
                return SkillResult.effect();
        }
        return switch (use.definition().targetType()) {
            case SELF, ALLY -> {
                if (use.target() == null) yield SkillResult.failure("battle.command.target_required");
                double amount = Math.max(0.0, use.definition().power());
                yield SkillResult.success(amount);
            }
            case ENEMY -> {
                if (use.target() == null) yield SkillResult.failure("battle.command.target_required");
                double amount = Math.max(0.0, use.definition().power() + use.actor().strength());
                if (use.definition().id().equals("exworld:guarded_strike"))
                    use.session().applyStatus(use.actor(), use.actor(), new BattleStatus("exworld:guarded", "status.exworld.guarded", 1, 2, true));
                yield SkillResult.success(amount);
            }
            case CELL -> SkillResult.success(0);
        };
    }
}