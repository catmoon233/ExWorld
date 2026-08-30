package net.exmo.exworld.battle;

import net.exmo.exworld.battle.api.*;
import net.exmo.exworld.battle.model.BattleState;

import java.util.*;

/** Server-authoritative AI adapter. Tactical ranking lives in BattleAiPlanner. */
public final class BattleAiScheduler {
    private BattleAiScheduler() {}

    public static void tick(BattleSession session, long now) {
        BattleSnapshot initial = session.snapshot();
        if (initial.state() != BattleState.FACTION_PHASE) return;
        List<BattleSnapshot.CombatantView> active = initial.combatants().values().stream()
                .filter(view -> !view.downed() && view.factionId().equals(initial.activeFaction())
                        && (view.playerId() == null || view.autoBattle())).toList();
        boolean acted = false;
        boolean pending = false;
        for (BattleSnapshot.CombatantView actor : active) {
            if (session.actionBusy(actor.id())) {
                pending = true;
                continue;
            }
            BattleSnapshot snapshot = session.snapshot();
            BattleSnapshot.CombatantView liveActor = snapshot.combatants().get(actor.id());
            if (liveActor == null || liveActor.downed()) continue;

            boolean accepted = false;
            for (BattleAiPlanner.AiDecision decision : BattleAiPlanner.plan(session, snapshot, liveActor)) {
                if (decision.type() == BattleAiPlanner.DecisionType.FINISH_PHASE) break;
                CommandReceipt receipt = submit(session, snapshot, decision);
                if (!receipt.accepted()) continue;
                acted = true;
                accepted = true;
                pending |= session.actionBusy(liveActor.id());
                break;
            }
            if (!accepted && liveActor.playerId() != null && !snapshot.readyPlayers().contains(liveActor.playerId())) {
                CommandReceipt receipt = session.submit(new BattleCommand.SetReady(snapshot.battleId(), snapshot.revision(),
                        UUID.randomUUID(), liveActor.id(), true));
                acted |= receipt.accepted();
            }
        }

        boolean manualPlayer = initial.combatants().values().stream().anyMatch(view -> !view.downed()
                && view.factionId().equals(initial.activeFaction()) && view.playerId() != null && !view.autoBattle()
                && !initial.readyPlayers().contains(view.playerId()));
        if (!acted && !pending && !manualPlayer) session.finishActiveFaction();
    }

    /** Compatibility overload for callers compiled against the old scheduler seam. */
    public static void tick(BattleSession session, long now, Map<UUID, Long> ignoredNextActionTicks) {
        tick(session, now);
    }

    private static CommandReceipt submit(BattleSession session, BattleSnapshot snapshot,
                                         BattleAiPlanner.AiDecision decision) {
        return switch (decision.type()) {
            case USE_SKILL -> {
                BattleAiPlanner.AiTarget target = decision.target();
                yield session.submit(new BattleCommand.UseSkill(snapshot.battleId(), snapshot.revision(), UUID.randomUUID(),
                        decision.actorId(), decision.cardInstanceId(), target.targetCell(), target.targetId()));
            }
            case MOVE -> session.submit(new BattleCommand.Move(snapshot.battleId(), snapshot.revision(), UUID.randomUUID(),
                    decision.actorId(), decision.target().targetCell()));
            case FINISH_PHASE -> CommandReceipt.rejected(UUID.randomUUID(), "battle.command.ai_finished", snapshot.revision());
        };
    }
}
