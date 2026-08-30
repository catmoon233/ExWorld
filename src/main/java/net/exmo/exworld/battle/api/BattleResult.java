package net.exmo.exworld.battle.api;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record BattleResult(BattleId battleId, Outcome outcome, Set<UUID> survivors, Set<UUID> downed,
                           Set<String> completedObjectives, Map<String, String> resultFlags,
                           Map<UUID, PlayerSummary> statistics, Map<UUID, RewardSummary> rewards) {
    public BattleResult {
        survivors=Set.copyOf(survivors);downed=Set.copyOf(downed);completedObjectives=Set.copyOf(completedObjectives);
        resultFlags=Map.copyOf(resultFlags);statistics=Map.copyOf(statistics);rewards=Map.copyOf(rewards);
    }
    /** Compatibility constructor retained for map modules that only consume outcome and flags. */
    public BattleResult(BattleId battleId,Outcome outcome,Set<UUID> survivors,Set<UUID> downed,Set<String> objectives,Map<String,String> flags){this(battleId,outcome,survivors,downed,objectives,flags,Map.of(),Map.of());}
    public record PlayerSummary(double damageDealt,double damageTaken,double healing,int cardsUsed){}
    public record RewardSummary(String selectedCard,int gold,boolean granted){}
    public enum Outcome { VICTORY, DEFEAT, ESCAPED, ABORTED }
}
