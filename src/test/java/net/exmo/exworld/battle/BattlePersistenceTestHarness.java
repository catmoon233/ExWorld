package net.exmo.exworld.battle;

import net.exmo.exworld.battle.api.*;
import net.exmo.exworld.battle.arena.*;
import net.exmo.exworld.battle.model.*;
import net.exmo.exworld.battle.persistence.BattleNbtCodec;
import net.exmo.exworld.battle.skill.SkillRegistry;

import java.util.*;

public final class BattlePersistenceTestHarness {
    public static void main(String[] args) {
        UUID player=UUID.randomUUID(),enemy=UUID.randomUUID(); ArenaDefinition arena=ArenaDefinition.flat("persist",8,64);
        EncounterRequest.CombatantSeed a=new EncounterRequest.CombatantSeed(player,player,player.toString(),"players",new BattleCell(0,0,64),50,50,100,100,10,20,6,3,
                List.of("exworld:guarded_strike","exworld:first_aid","exworld:guarded_strike","exworld:first_aid","exworld:guarded_strike"),9);
        EncounterRequest.CombatantSeed b=new EncounterRequest.CombatantSeed(enemy,null,enemy.toString(),"enemies",new BattleCell(5,0,64),50,50,100,100,10,10,6,3,
                List.of("exworld:guarded_strike","exworld:first_aid","exworld:guarded_strike","exworld:first_aid","exworld:guarded_strike"), List.of(1,1,1,1,1), -1, 2,
                Set.of(CombatantAttribute.AIRBORNE));
        Map<EncounterRequest.FactionPair,FactionRelation> relations=Map.of(new EncounterRequest.FactionPair("players","enemies"),FactionRelation.HOSTILE,new EncounterRequest.FactionPair("enemies","players"),FactionRelation.HOSTILE);
        EncounterRequest request=new EncounterRequest("persist","persist",List.of(a,b),relations,Map.of(),42,Map.of(),"players");
        BattleSession session=new BattleSession(BattleId.create(),request,new ArenaGrid(arena),SkillRegistry.defaults(),0,0);session.finishDeployment();
        BattleSnapshot snapshot=session.snapshot();check(session.submit(new BattleCommand.Move(snapshot.battleId(),snapshot.revision(),UUID.randomUUID(),player,new BattleCell(2,0,64))).accepted(),"move accepted");
        session.tick();session.tick();int elapsed=session.snapshot().motions().getFirst().elapsedTicks();
        BattleSnapshot saved=session.snapshot();
        BattleSession restored=new BattleSession(saved.battleId(),request,new ArenaGrid(arena),SkillRegistry.defaults(),saved.arenaOriginX(),saved.arenaOriginZ(),
                session.combatants(),saved.factionOrder(),saved.state(),saved.revision(),saved.eventSequence(),saved.round(),session.factionIndex(),saved.phaseTicksRemaining(),saved.readyPlayers());
        restored.restoreRuntime(session.actions().moves(),session.introSkippedPlayers(),session.pendingResult().orElse(null),session.persistedRewards(),session.persistedStatistics(),session.phaseDamage(),session.battleTicks());
        check(restored.snapshot().motions().getFirst().elapsedTicks()==elapsed,"movement elapsed ticks persist");
        check(restored.snapshot().combatants().get(player).cell().x()==0,"uncommitted movement restores origin cell");
        for(int i=elapsed;i<2*net.exmo.exworld.battle.action.BattleActionTimeline.TICKS_PER_CELL;i++)restored.tick();
        check(restored.snapshot().combatants().get(player).cell().x()==2,"restored movement commits destination once");
        BattleSession nbtRestored = BattleNbtCodec.load(BattleNbtCodec.save(session), arena, SkillRegistry.defaults());
        check(nbtRestored.combatant(enemy).orElseThrow().hasAttribute(CombatantAttribute.AIRBORNE),
                "combatant attributes survive battle NBT recovery");
        session.combatant(player).orElseThrow().setStrengthLevel(4);
        session.combatant(player).orElseThrow().setWeaponAttack(7);
        session.combatant(player).orElseThrow().setDodgeDirection(1, 0);
        session.combatant(player).orElseThrow().setInterceptDirection(0, -1);
        BattleSession nbtCombatant = BattleNbtCodec.load(BattleNbtCodec.save(session), arena, SkillRegistry.defaults());
        check(nbtCombatant.combatant(player).orElseThrow().strengthLevel() == 4, "strength level survives battle NBT recovery");
        check(Math.abs(nbtCombatant.combatant(player).orElseThrow().weaponAttack() - 7) < .001, "weapon attack survives battle NBT recovery");
        check(nbtCombatant.combatant(player).orElseThrow().dodgeDx() == 1, "dodge facing survives battle NBT recovery");
        check(nbtCombatant.combatant(player).orElseThrow().interceptDz() == -1, "intercept facing survives battle NBT recovery");

    }
    private static EncounterRequest.CombatantSeed seed(UUID id,UUID player,String faction,int x,double initiative){return new EncounterRequest.CombatantSeed(id,player,id.toString(),faction,new BattleCell(x,0,64),50,50,100,100,10,initiative,6,3,List.of("exworld:guarded_strike","exworld:first_aid","exworld:guarded_strike","exworld:first_aid","exworld:guarded_strike"));}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
