package net.exmo.exworld.battle;

import net.exmo.exworld.battle.api.*;
import net.exmo.exworld.battle.arena.*;
import net.exmo.exworld.battle.card.DeckState;
import net.exmo.exworld.battle.card.PlayerCardCollection;
import net.exmo.exworld.battle.card.PlayerCardModule;
import net.exmo.exworld.battle.card.CardDefinition;
import net.exmo.exworld.battle.combatant.Combatant;
import net.exmo.exworld.battle.compat.IronHitWindow;
import net.exmo.exworld.battle.compat.IronSpellTiming;
import net.exmo.exworld.battle.model.*;
import net.exmo.exworld.battle.skill.SkillDefinition;
import net.exmo.exworld.battle.skill.SkillRegistry;
import net.exmo.exworld.battle.skill.SkillResult;
import net.exmo.exworld.battle.weapon.WeaponDefinition;
import net.exmo.exworld.battle.weapon.WeaponPassive;

import java.util.*;

public final class BattleRulesTestHarness {
    public static void main(String[] args) {
        pathfindingPrefersStraightRoutes();
        pathfindingAndReservation(); pathfindingRoutesAroundCombatants(); snapshotsExposeBlockedCells(); tacticalTeleportIgnoresMovementBudget(); stagedSkillLifecycle(); concurrentPartyCommands(); turnBasedPotionStatuses(); potionLevelsDecay(); circularSkillRadius(); lineOfSight(); airborneUnitsDoNotOccludeRangedSkills(); ironHitPolicies(); deckCycle(); dashPassiveAndFreeMovement(); passiveCarrierIsIndependent(); phaseHandCleanup(); effectResolverUsesFinalDamage(); initialHandSize(); playerCardCollection(); cardInstancesAndFusion(); cardStarConfigurationAndFreeze(); starterCardDistribution(); cardContentAndLoopRules();         debugPartyRules(); phaseAndCommandRules(); readyCanBeWithdrawn(); emptyDeploymentDoesNotDelayOpeningAi(); firstPhaseWaitsForAlliedAi(); enemyOpeningAiActsImmediately(); aiActionsArePaced(); ironCastKeepsAutoBattlePhaseLocked(); aiHealsLowestHealthAlly(); aiUsesSelfSkillWithoutEnemy(); aiChoosesCellSkillCoverage(); aiMovesIntoCastRange(); aiFallsBackToBasicAttack(); aiFinishesWhenNoActionExists(); actionPointsLimitMonster(); resultFlow(); disconnectedPlayerDoesNotBlockResult(); resultTimeoutAutoSettles(); defeatedPlayerCanConfirmResult(); multipleFactionOrder(); engagementAdvantage(); debugCardCatalog(); genericAndWarriorSkills();
    }

    private static void passiveCarrierIsIndependent() {
        net.exmo.exworld.battle.weapon.WeaponPassiveEngine engine = new net.exmo.exworld.battle.weapon.WeaponPassiveEngine();
        engine.register(new WeaponDefinition("test:carrier", "item.test.carrier", "test_passive"), new WeaponPassive() {
            @Override public String id() { return "test_passive"; }
            @Override public void onAttackCard(BattleSession session, Combatant actor) { actor.incrementPassiveCounter(id()); }
        });
        check("test_passive".equals(engine.passiveId("test:carrier")), "custom passive carrier resolves independently");
        check(engine.passiveId("minecraft:iron_sword").isEmpty(), "unregistered item has no passive despite being a weapon");
    }

    private static void ironHitPolicies() {
        UUID target = UUID.randomUUID(), second = UUID.randomUUID();
        IronHitWindow cone = new IronHitWindow(IronHitWindow.policyFor("iron:irons_spellbooks:cone_of_cold"), target);
        check(cone.accepts(target, 10), "continuous Iron spell accepts its first damage tick");
        cone.record(target, 10);
        check(!cone.accepts(target, 10), "continuous Iron spell deduplicates overlapping hit parts in one tick");
        check(cone.accepts(target, 11), "continuous Iron spell accepts the same target on a later tick");
        IronHitWindow area = new IronHitWindow(IronHitWindow.policyFor("iron:irons_spellbooks:fireball"), target);
        check(area.accepts(target, 20), "area spell accepts aimed target"); area.record(target, 20);
        check(area.accepts(second, 20), "area spell accepts each additional target once");
        IronHitWindow single = new IronHitWindow(IronHitWindow.Policy.SINGLE, target);
        check(!single.accepts(second, 1) && single.accepts(target, 1), "single-target spell remains target locked");
        single.record(target, 1); check(single.complete(), "single-target hit closes its window");
    }

    private static void pathfindingAndReservation() {
        ArenaDefinition.GridPoint north = new ArenaDefinition.GridPoint(1, 0), west = new ArenaDefinition.GridPoint(0, 1);
        ArenaGrid grid = new ArenaGrid(new ArenaDefinition("test", 5, 5, 64, 1, Map.of(), Set.of(north, west), Map.of()));
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(); check(grid.place(a, new BattleCell(0, 0, 64)), "place a");
        check(grid.reservePath(a, new BattleCell(0, 0, 64), new BattleCell(1, 1, 64), 1).isEmpty(), "no diagonal corner cutting");
        ArenaGrid open = new ArenaGrid(ArenaDefinition.flat("open", 5, 64)); check(open.place(a, new BattleCell(0, 0, 64)), "place open a");
        check(open.place(b, new BattleCell(4, 4, 64)), "place open b");
        check(open.reservePath(a, new BattleCell(0, 0, 64), new BattleCell(2, 2, 64), 2).isPresent(), "eight way path");
        check(open.reservePath(b, new BattleCell(4, 4, 64), new BattleCell(2, 2, 64), 2).isEmpty(), "first reservation wins");
    }

    private static void pathfindingPrefersStraightRoutes() {
        ArenaGrid grid = new ArenaGrid(ArenaDefinition.flat("straight", 10, 64));
        UUID mover = UUID.randomUUID();
        BattleCell start = new BattleCell(3, 3, 64), destination = new BattleCell(6, 3, 64);
        check(grid.place(mover, start), "place straight-route mover");
        check(grid.reservePath(mover, start, destination, 3).orElseThrow().equals(List.of(
                        new BattleCell(4, 3, 64), new BattleCell(5, 3, 64), destination)),
                "open horizontal movement uses the direct route instead of a zig-zag of equal cost");
    }

    private static void pathfindingRoutesAroundCombatants() {
        ArenaGrid grid = new ArenaGrid(ArenaDefinition.flat("reroute", 5, 64));
        UUID mover = UUID.randomUUID(), blocker = UUID.randomUUID();
        BattleCell start = new BattleCell(0, 0, 64), blocked = new BattleCell(1, 0, 64), destination = new BattleCell(2, 0, 64);
        check(grid.place(mover, start) && grid.place(blocker, blocked), "place mover and blocking combatant");
        List<BattleCell> path = grid.reservePath(mover, start, destination, 4).orElseThrow();
        check(path.getLast().equals(destination), "pathfinder reaches the requested free destination around a combatant");
        check(!path.contains(blocked), "pathfinder never routes through an occupied combat cell");
    }

    private static void snapshotsExposeBlockedCells() {
        UUID player = UUID.randomUUID(), enemy = UUID.randomUUID();
        ArenaDefinition.GridPoint blocked = new ArenaDefinition.GridPoint(2, 2);
        ArenaDefinition arena = new ArenaDefinition("preview-terrain", 5, 5, 64, 1, Map.of(), Set.of(blocked), Map.of());
        BattleSession session = new BattleSession(BattleId.create(), request(List.of(
                seed(player, player, "players", 0, 0, 10), seed(enemy, null, "enemies", 4, 4, 1))),
                new ArenaGrid(arena), SkillRegistry.defaults(), 0, 0);
        check(session.snapshot().blockedCells().contains(new BattleCell(2, 2, 64)),
                "clients receive blocked terrain so a path preview cannot suggest an invalid route");
    }

    private static void tacticalTeleportIgnoresMovementBudget() {
        UUID player=UUID.randomUUID(),enemy=UUID.randomUUID();
        EncounterRequest.CombatantSeed immobile=new EncounterRequest.CombatantSeed(player,player,"teleporter","players",new BattleCell(0,0,64),
                40,40,100,100,10,20,0,3,List.of("exworld:guarded_strike"));
        BattleSession session=new BattleSession(BattleId.create(),request(List.of(immobile,seed(enemy,null,"enemies",5,5,1))),
                new ArenaGrid(ArenaDefinition.flat("teleport",8,64)),SkillRegistry.defaults(),0,0);
        var actor=session.combatant(player).orElseThrow();
        check(session.teleportDisplacement(actor,new BattleCell(4,4,64)).success(),"tactical teleport works with zero movement points");
        check(actor.cell().equals(new BattleCell(4,4,64))&&actor.movementRemaining()==0,"teleport commits instantly without movement cost");
        check(session.snapshot().motions().isEmpty(),"teleport does not create a walking timeline");
    }

    private static void stagedSkillLifecycle() {
        UUID player=UUID.randomUUID(),enemy=UUID.randomUUID();SkillRegistry registry=SkillRegistry.defaults();int[] stage={0};
        registry.register(new net.exmo.exworld.battle.skill.SkillDefinition("test:staged","test.staged","","test:staged",12,6,
                net.exmo.exworld.battle.skill.SkillDefinition.TargetType.CELL,false,true,true,0,1));
        registry.registerAdapter("test:staged",use->++stage[0]<3?net.exmo.exworld.battle.skill.SkillResult.staged(0,false,stage[0]==1)
                :net.exmo.exworld.battle.skill.SkillResult.staged(0,true,false));
        EncounterRequest.CombatantSeed caster=new EncounterRequest.CombatantSeed(player,player,"caster","players",new BattleCell(0,0,64),
                40,40,100,100,10,20,6,3,List.of("test:staged"));
        BattleSession session=new BattleSession(BattleId.create(),request(List.of(caster,seed(enemy,null,"enemies",6,6,1))),
                new ArenaGrid(ArenaDefinition.flat("staged",8,64)),registry,0,0);session.finishDeployment();UUID card=session.snapshot().combatants().get(player).hand().stream().filter(c->c.skillId().equals("test:staged")).findFirst().orElseThrow().instanceId();
        for(int i=0;i<3;i++){BattleSnapshot snapshot=session.snapshot();check(session.submit(new BattleCommand.UseSkill(snapshot.battleId(),snapshot.revision(),UUID.randomUUID(),player,card,new BattleCell(i+1,1,64),null)).accepted(),"staged skill accepts placement "+i);for(int tick=0;tick<8;tick++)session.tick();}
        var actor=session.combatant(player).orElseThrow();check(actor.mana()==88,"staged skill charges mana only on its first placement");
        check(actor.deck().card(card).isEmpty(),"staged skill consumes its card only after the final placement");
    }

    private static void concurrentPartyCommands() {
        UUID first=UUID.randomUUID(),second=UUID.randomUUID(),enemy=UUID.randomUUID();BattleSession session=new BattleSession(BattleId.create(),
                request(List.of(seed(first,first,"players",0,0,30),seed(second,second,"players",0,2,25),seed(enemy,null,"enemies",6,1,1))),
                new ArenaGrid(ArenaDefinition.flat("party-concurrency",8,64)),SkillRegistry.defaults(),0,0);session.finishDeployment();
        BattleSnapshot shared=session.snapshot();
        check(session.submit(new BattleCommand.Move(shared.battleId(),shared.revision(),UUID.randomUUID(),first,new BattleCell(1,0,64))).accepted(),"first party command succeeds");
        check(session.submit(new BattleCommand.Move(shared.battleId(),shared.revision(),UUID.randomUUID(),second,new BattleCell(1,2,64))).accepted(),"second party command from the same snapshot is revalidated instead of rejected as stale");
    }

    private static void turnBasedPotionStatuses() {
        UUID player=UUID.randomUUID(),enemy=UUID.randomUUID();BattleSession session=new BattleSession(BattleId.create(),
                request(List.of(seed(player,player,"players",0,0,30),seed(enemy,null,"enemies",4,0,1))),
                new ArenaGrid(ArenaDefinition.flat("potion",8,64)),SkillRegistry.defaults(),0,0);
        session.applyExternalStatus(player,new BattleStatus("minecraft:poison","effect.minecraft.poison",1,2,false));
        session.applyExternalStatus(player,new BattleStatus("minecraft:slowness","effect.minecraft.slowness",1,2,false));
        float before=session.combatant(player).orElseThrow().health();session.finishDeployment();var actor=session.combatant(player).orElseThrow();
        check(actor.health()<before,"poison damages once when the owner's faction phase begins");
        check(actor.movementRemaining()==Math.max(1,actor.movementPoints()/2),"slowness converts to turn movement reduction");
    }

    private static void potionLevelsDecay() {
        UUID player = UUID.randomUUID(); Combatant combatant = new Combatant(seed(player, player, "players", 0, 0, 30), 0L);
        combatant.addStatus(new BattleStatus("minecraft:poison", "effect.minecraft.poison", 2, 10, false, false, true, false));
        combatant.beginPhase();
        check(combatant.statuses().stream().filter(status -> status.id().equals("minecraft:poison")).findFirst().orElseThrow().stacks() == 1,
                "turn-managed potion effects lose one level each owner phase");
        combatant.beginPhase();
        check(combatant.statuses().stream().noneMatch(status -> status.id().equals("minecraft:poison")),
                "a potion effect disappears after its level would fall below one");
        combatant.addStatus(new BattleStatus("minecraft:regeneration", "effect.minecraft.regeneration", 3, 60, true, false, true, true));
        combatant.beginPhase();
        check(combatant.statuses().stream().filter(status -> status.id().equals("minecraft:regeneration")).findFirst().orElseThrow().stacks() == 3,
                "potion effects lasting over 10000 ticks preserve their level between phases");
    }

    private static void lineOfSight() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        ArenaGrid terrain = new ArenaGrid(new ArenaDefinition("los", 5, 5, 64, 1, Map.of(), Set.of(new ArenaDefinition.GridPoint(2, 2)), Map.of()));
        terrain.place(a, new BattleCell(0, 0, 64)); terrain.place(b, new BattleCell(4, 4, 64));
        check(!terrain.hasLineOfSight(new BattleCell(0,0,64), new BattleCell(4,4,64), a, b, false, false), "terrain blocks los");
        check(terrain.hasLineOfSight(new BattleCell(0,0,64), new BattleCell(4,4,64), a, b, false, true), "ignore terrain");
    }

    private static void airborneUnitsDoNotOccludeRangedSkills() {
        UUID player = UUID.randomUUID(), vex = UUID.randomUUID(), enemy = UUID.randomUUID();
        EncounterRequest.CombatantSeed aerialVex = new EncounterRequest.CombatantSeed(vex, null, "vex", "players", new BattleCell(2, 0, 64),
                18, 18, 40, 40, 8, 9, 5, 1, List.of("exworld:basic_attack"), List.of(1), -1, DeckState.DRAW_PER_PHASE,
                Set.of(CombatantAttribute.AIRBORNE));
        BattleSession session = new BattleSession(BattleId.create(), request(List.of(seed(player, player, "players", 0, 0, 10), aerialVex,
                seed(enemy, null, "enemies", 4, 0, 1))), new ArenaGrid(ArenaDefinition.flat("airborne", 5, 64)), SkillRegistry.defaults(), 0, 0);
        check(session.combatant(vex).orElseThrow().elevation() == 3.0D, "airborne unit renders three blocks above its battle floor");
        session.combatant(player).orElseThrow().addBlock(7.0F);
        check(session.snapshot().combatants().get(player).block() == 7.0F,
                "the authoritative snapshot exposes a combatant's damage absorption to the HUD");
        check(session.hasLineOfSight(new BattleCell(0, 0, 64), new BattleCell(4, 0, 64), player, enemy, false, false),
                "an airborne summon retains a tactical cell but does not block a ranged line of sight");
    }

    private static void circularSkillRadius() {
        BattleCell center = new BattleCell(4, 4, 64);
        check(center.withinRadius(new BattleCell(7, 4, 64), 3), "axis cell is inside circular radius");
        check(!center.withinRadius(new BattleCell(7, 7, 64), 3), "corner outside Euclidean radius is rejected");
        check(center.withinRadius(new BattleCell(6, 6, 64), 3), "near diagonal remains inside circular radius");
    }

    private static void deckCycle() {
        List<String> configured = List.of("a", "b", "c", "d", "e", "f");
        List<String> expectedOpeningOrder = new ArrayList<>(configured); Collections.shuffle(expectedOpeningOrder, new Random(9));
        DeckState deck = new DeckState(configured, 9); deck.drawInitial(5);
        check(deck.hand().stream().map(net.exmo.exworld.battle.card.SkillCard::skillId).toList().equals(expectedOpeningOrder.subList(0, 5)),
                "a fresh draw pile is shuffled from the battle seed before the opening draw");
        check(deck.handWithInnate().size() == 6, "five cards plus innate");
        UUID first = deck.hand().getFirst().instanceId(); check(deck.consume(first), "consume drawn card");
        deck.drawForPhase(); check(deck.hand().size() == 6, "draw remaining and reshuffle discard");
        check(deck.hand().stream().anyMatch(card -> card.instanceId().equals(first)), "discard reshuffled deterministically");
    }

    private static void dashPassiveAndFreeMovement() {
        UUID player = UUID.randomUUID(), enemy = UUID.randomUUID();
        EncounterRequest.CombatantSeed seed = new EncounterRequest.CombatantSeed(player, player, "blade", "players", new BattleCell(0, 0, 64),
                60, 60, 100, 100, 10, 20, 6, 3, List.of("exworld:guarded_strike", "exworld:guarded_strike"), 5);
        BattleSession session = new BattleSession(BattleId.create(), request(List.of(seed, seed(enemy, null, "enemies", 1, 1, 1))),
                new ArenaGrid(ArenaDefinition.flat("dash", 8, 64)), SkillRegistry.defaults(), 0, 0);
        Combatant actor = session.combatant(player).orElseThrow(); actor.configureEquipment("exworld:warrior_blade", "", 1);
        session.finishDeployment();
        for (int i = 0; i < 2; i++) {
            BattleSnapshot snapshot = session.snapshot();
            UUID card = snapshot.combatants().get(player).hand().stream().filter(value -> value.skillId().equals("exworld:guarded_strike")).findFirst().orElseThrow().instanceId();
            check(session.submit(new BattleCommand.UseSkill(snapshot.battleId(), snapshot.revision(), UUID.randomUUID(), player, card, new BattleCell(1, 0, 64), enemy)).accepted(), "warrior blade attack succeeds");
            for (int tick = 0; tick < 8; tick++) session.tick();
        }
        check(actor.attackCardCount() == 2, "two successful attack cards advance warrior blade progress");
        check(actor.deck().handWithInnate().stream().anyMatch(value -> value.skillId().equals("exworld:dash")), "warrior blade generates dash");
        int movement = actor.movementRemaining();
        BattleSnapshot dash = session.snapshot(); UUID dashCard = dash.combatants().get(player).hand().stream().filter(value -> value.skillId().equals("exworld:dash")).findFirst().orElseThrow().instanceId();
        check(session.submit(new BattleCommand.UseSkill(dash.battleId(), dash.revision(), UUID.randomUUID(), player, dashCard, new BattleCell(3, 0, 64), null)).accepted(), "dash accepts a reachable four-cell destination");
        for (int tick = 0; tick < 25; tick++) session.tick();
        check(actor.movementRemaining() == movement, "dash does not consume ordinary movement");
    }

    private static void phaseHandCleanup() {
        List<String> configured = List.of("keep", "drop", "next", "last");
        List<String> order = new ArrayList<>(configured); Collections.shuffle(order, new Random(19));
        DeckState deck = new DeckState(configured, 19); deck.drawInitial(2);
        UUID retained = deck.hand().getFirst().instanceId(); String retainedSkill = deck.hand().getFirst().skillId();
        deck.endPhase(card -> card.instanceId().equals(retained));
        check(deck.hand().size() == 1 && deck.hand().getFirst().instanceId().equals(retained), "retained card remains in hand");
        check(deck.discardPile().size() == 1, "ordinary hand cards discard at phase end");
        deck.drawForPhase();
        check(deck.hand().stream().map(net.exmo.exworld.battle.card.SkillCard::skillId).toList().equals(List.of(retainedSkill, order.get(2), order.get(3))),
                "next phase continues from the opening shuffle's draw order");
    }

    private static void effectResolverUsesFinalDamage(){
        UUID player=UUID.randomUUID(),enemy=UUID.randomUUID();var registry=SkillRegistry.defaults();var resolver=new net.exmo.exworld.battle.combat.BattleEffectResolver(){
            public double damage(net.exmo.exworld.battle.combatant.Combatant actor,net.exmo.exworld.battle.combatant.Combatant target,net.exmo.exworld.battle.skill.SkillDefinition skill,double requested){target.damage((float)(requested*.5));return requested*.5;}
            public double heal(net.exmo.exworld.battle.combatant.Combatant actor,net.exmo.exworld.battle.combatant.Combatant target,net.exmo.exworld.battle.skill.SkillDefinition skill,double requested){target.heal((float)requested);return requested;}};
        BattleSession session=new BattleSession(BattleId.create(),request(List.of(seed(player,player,"players",0,0,20),seed(enemy,null,"enemies",1,0,10))),new ArenaGrid(ArenaDefinition.flat("effects",8,64)),registry,0,0,resolver);session.finishDeployment();var snap=session.snapshot();var card=snap.combatants().get(player).hand().stream().filter(c->c.skillId().equals("exworld:guarded_strike")).findFirst().orElseThrow();
        check(session.submit(new BattleCommand.UseSkill(snap.battleId(),snap.revision(),UUID.randomUUID(),player,card.instanceId(),new BattleCell(1,0,64),enemy)).accepted(),"effect resolver skill succeeds");
        check(Math.abs(session.snapshot().events().getLast().amount()-6)<.001,"battle events use final post-defence damage from resolver");
    }

    private static void initialHandSize() {
        DeckState defaultDeck = new DeckState(List.of("a", "b", "c", "d", "e", "f"), 7); defaultDeck.drawInitial();
        check(defaultDeck.hand().size() == 3, "default initial hand is three cards");
        DeckState attributedDeck = new DeckState(List.of("a", "b", "c", "d", "e", "f"), 7); attributedDeck.drawInitial(4);
        check(attributedDeck.hand().size() == 4, "initial hand accepts character attribute value");
    }

    private static void debugPartyRules() {
        var parties=new net.exmo.exworld.battle.party.DebugPartyManager();UUID leader=UUID.randomUUID(),second=UUID.randomUUID(),third=UUID.randomUUID(),fourth=UUID.randomUUID(),fifth=UUID.randomUUID();
        check(parties.invite(leader,"leader",second,"second",0)==net.exmo.exworld.battle.party.DebugPartyManager.Result.OK,"party leader can invite");
        check(parties.accept(second,1)==net.exmo.exworld.battle.party.DebugPartyManager.Result.OK&&parties.members(leader).size()==2,"invited player joins the same party");
        check(parties.invite(second,"second",third,"third",2)==net.exmo.exworld.battle.party.DebugPartyManager.Result.NOT_LEADER,"only party leader can invite");
        for(UUID member:List.of(third,fourth)){check(parties.invite(leader,"leader",member,member.toString(),3)==net.exmo.exworld.battle.party.DebugPartyManager.Result.OK,"party accepts another invite");check(parties.accept(member,4)==net.exmo.exworld.battle.party.DebugPartyManager.Result.OK,"party member accepts invite");}
        check(parties.invite(leader,"leader",fifth,"fifth",5)==net.exmo.exworld.battle.party.DebugPartyManager.Result.FULL,"debug party is capped at four members");
        check(parties.leave(leader)==net.exmo.exworld.battle.party.DebugPartyManager.Result.OK&&parties.leader(second).equals(second),"leadership transfers when leader leaves");
        check(parties.kick(second,third)==net.exmo.exworld.battle.party.DebugPartyManager.Result.OK&&!parties.members(second).contains(third),"new leader can kick a member");
    }

    private static void firstPhaseWaitsForAlliedAi() {
        UUID player = UUID.randomUUID(), ally = UUID.randomUUID(), enemy = UUID.randomUUID();
        EncounterRequest request = request(List.of(
                seed(player, player, "players", 0, 0, 30),
                seed(ally, null, "players", 1, 0, 30),
                seed(enemy, null, "enemies", 5, 0, 20)));
        BattleSession session = new BattleSession(BattleId.create(), request,
                new ArenaGrid(ArenaDefinition.flat("first-phase-ai", 8, 64)), SkillRegistry.defaults(), 0, 0);
        session.finishDeployment();
        BattleSnapshot first = session.snapshot();
        check(session.submit(new BattleCommand.SetReady(first.battleId(), first.revision(), UUID.randomUUID(), player, true)).accepted(),
                "player can finish their input while an allied AI still has a turn");
        session.tick();
        check(session.snapshot().state() == BattleState.FACTION_PHASE && session.snapshot().activeFaction().equals("players"),
                "player ready cannot skip allied AI on the opening faction phase");
    }

    private static void emptyDeploymentDoesNotDelayOpeningAi() {
        UUID player = UUID.randomUUID(), enemy = UUID.randomUUID();
        EncounterRequest base = request(List.of(seed(player, player, "players", 0, 0, 90), seed(enemy, null, "enemies", 7, 0, 1)));
        EncounterRequest attacked = new EncounterRequest(base.encounterId(), base.arenaId(), base.combatants(), base.relations(),
                base.returnPoints(), base.seed(), Map.of("deployment_ticks", "0"), "enemies");
        BattleSession session = new BattleSession(BattleId.create(), attacked,
                new ArenaGrid(ArenaDefinition.flat("no-empty-deployment", 10, 64)), SkillRegistry.defaults(), 0, 0);
        session.finishIntro();
        check(session.snapshot().state() == BattleState.FACTION_PHASE && session.snapshot().activeFaction().equals("enemies"),
                "an encounter without deployment commands enters the attacker opening phase immediately after intro");
    }

    private static void enemyOpeningAiActsImmediately() {
        UUID player = UUID.randomUUID(), enemy = UUID.randomUUID();
        EncounterRequest base = request(List.of(seed(player, player, "players", 0, 0, 90), seed(enemy, null, "enemies", 7, 0, 1)));
        EncounterRequest attacked = new EncounterRequest(base.encounterId(), base.arenaId(), base.combatants(), base.relations(),
                base.returnPoints(), base.seed(), base.variables(), "enemies");
        BattleSession session = new BattleSession(BattleId.create(), attacked,
                new ArenaGrid(ArenaDefinition.flat("enemy-opening-ai", 10, 64)), SkillRegistry.defaults(), 0, 0);
        check(session.snapshot().activeFaction().equals("enemies") && session.snapshot().factionOrder().getFirst().equals("enemies"),
                "requested world attacker is already first during intro snapshots");
        session.finishDeployment();
        BattleAiScheduler.tick(session, 1, new HashMap<>());
        check(session.snapshot().events().stream().anyMatch(event -> event.actorId().equals(enemy)
                        && (event.type() == BattleEvent.Type.MOVE || event.type() == BattleEvent.Type.SKILL)),
                "pure monster opening phase starts an action on its first scheduler pass");
        check(session.snapshot().phaseTicksRemaining() == BattleSession.DEFAULT_PHASE_TICKS,
                "monster acts before the opening phase timer loses a tick");
    }

    private static void aiActionsArePaced() {
        UUID player=UUID.randomUUID(),enemy=UUID.randomUUID();
        EncounterRequest base=request(List.of(seed(player,player,"players",0,0,20),seed(enemy,null,"enemies",1,0,90)));
        EncounterRequest attacked=new EncounterRequest(base.encounterId(),base.arenaId(),base.combatants(),base.relations(),
                base.returnPoints(),base.seed(),base.variables(),"enemies");
        BattleSession session=new BattleSession(BattleId.create(),attacked,new ArenaGrid(ArenaDefinition.flat("paced-ai",8,64)),SkillRegistry.defaults(),0,0);
        session.finishDeployment();Map<UUID,Long> schedule=new HashMap<>();
        BattleAiScheduler.tick(session,1,schedule);
        long first=session.snapshot().events().stream().filter(event->event.actorId().equals(enemy)&&event.type()==BattleEvent.Type.SKILL).count();
        BattleAiScheduler.tick(session,6,schedule);
        long immediate=session.snapshot().events().stream().filter(event->event.actorId().equals(enemy)&&event.type()==BattleEvent.Type.SKILL).count();
        check(first==1&&immediate==1,"AI cannot submit a second spell while the first cast lock is active");
        for (int tick = 0; tick < 8; tick++) session.tick();
        BattleAiScheduler.tick(session,15,schedule);
        long afterLock=session.snapshot().events().stream().filter(event->event.actorId().equals(enemy)&&event.type()==BattleEvent.Type.SKILL).count();
        check(afterLock==2,"AI submits the next card immediately after its cast lock ends");
    }

    private static void ironCastKeepsAutoBattlePhaseLocked() {
        UUID caster = UUID.randomUUID(), enemy = UUID.randomUUID();
        SkillRegistry registry = SkillRegistry.defaults();
        registry.register(new SkillDefinition("test:iron_cast", "test.iron_cast", "", "test:iron_cast", 0, 8,
                SkillDefinition.TargetType.ENEMY, false, true, true, 0, 1));
        registry.registerAdapter("test:iron_cast", new net.exmo.exworld.battle.skill.SkillAdapter() {
            @Override public SkillResult execute(net.exmo.exworld.battle.skill.SkillUse use) { return SkillResult.success(0); }
            @Override public int actionTicks(net.exmo.exworld.battle.skill.SkillUse use, SkillResult result) {
                return IronSpellTiming.actionTicks(20);
            }
        });
        EncounterRequest.CombatantSeed casterSeed = new EncounterRequest.CombatantSeed(caster, caster, "caster", "players",
                new BattleCell(0, 0, 64), 50, 50, 100, 100, 20, 90, 6, 2,
                List.of("test:iron_cast", "test:iron_cast"));
        BattleSession session = new BattleSession(BattleId.create(), request(List.of(casterSeed,
                seed(enemy, null, "enemies", 5, 0, 1))), new ArenaGrid(ArenaDefinition.flat("ai-iron-cast", 10, 64)),
                registry, 0, 0);
        session.finishDeployment();
        BattleSnapshot beforeAuto = session.snapshot();
        check(session.submit(new BattleCommand.SetAutoBattle(beforeAuto.battleId(), beforeAuto.revision(), UUID.randomUUID(), caster, true)).accepted(),
                "auto battle can be enabled for the cast-lock regression");
        BattleAiScheduler.tick(session, 1, new HashMap<>());
        for (int tick = 2; tick <= 19; tick++) {
            session.tick();
            BattleAiScheduler.tick(session, tick, new HashMap<>());
        }
        long duringCast = session.snapshot().events().stream().filter(event -> event.actorId().equals(caster)
                && event.type() == BattleEvent.Type.SKILL).count();
        check(duringCast == 1, "auto battle cannot submit the next card while an Iron-style cast is still in progress");
        check(session.snapshot().state() == BattleState.FACTION_PHASE,
                "auto battle cannot end the faction phase while an Iron-style cast is still in progress");
    }

    private static void aiHealsLowestHealthAlly() {
        UUID healer = UUID.randomUUID(), ally = UUID.randomUUID(), player = UUID.randomUUID();
        EncounterRequest.CombatantSeed healerSeed = new EncounterRequest.CombatantSeed(healer, null, "healer", "enemies",
                new BattleCell(0, 0, 64), 50, 50, 100, 100, 20, 90, 6, 1, List.of("exworld:first_aid"));
        EncounterRequest.CombatantSeed allySeed = new EncounterRequest.CombatantSeed(ally, null, "ally", "enemies",
                new BattleCell(1, 0, 64), 50, 20, 100, 100, 20, 80, 6, 0, List.of());
        BattleSession session = new BattleSession(BattleId.create(), request(List.of(healerSeed, allySeed,
                seed(player, player, "players", 7, 0, 1))), new ArenaGrid(ArenaDefinition.flat("ai-heal", 10, 64)),
                SkillRegistry.defaults(), 0, 0);
        session.finishDeployment();
        BattleAiScheduler.tick(session, 1, new HashMap<>());
        BattleEvent skill = session.snapshot().events().stream().filter(event -> event.actorId().equals(healer)
                && event.type() == BattleEvent.Type.SKILL).findFirst().orElseThrow();
        check(skill.targetId().equals(ally), "AI healing targets the lowest-health friendly unit");
    }

    private static void aiUsesSelfSkillWithoutEnemy() {
        UUID caster = UUID.randomUUID();
        SkillRegistry registry = SkillRegistry.defaults();
        registry.register(new SkillDefinition("test:self_buff", "test.self_buff", "", "test:self_buff", 0, 0,
                SkillDefinition.TargetType.SELF, false, true, true, 0, 1));
        registry.registerAdapter("test:self_buff", use -> SkillResult.success(0));
        EncounterRequest.CombatantSeed casterSeed = new EncounterRequest.CombatantSeed(caster, null, "caster", "monsters",
                new BattleCell(2, 2, 64), 50, 50, 100, 100, 20, 10, 6, 1, List.of("test:self_buff"));
        BattleSession session = new BattleSession(BattleId.create(), request(List.of(casterSeed,
                seed(UUID.randomUUID(), UUID.randomUUID(), "players", 7, 7, 1))),
                new ArenaGrid(ArenaDefinition.flat("ai-self", 8, 64)), registry, 0, 0);
        session.finishDeployment();
        BattleAiScheduler.tick(session, 1, new HashMap<>());
        BattleEvent skill = session.snapshot().events().stream().filter(event -> event.actorId().equals(caster)
                && event.type() == BattleEvent.Type.SKILL).findFirst().orElseThrow();
        check(skill.targetId().equals(caster), "AI self-targeting skill does not require an enemy target");
    }

    private static void aiChoosesCellSkillCoverage() {
        UUID caster = UUID.randomUUID(), player = UUID.randomUUID();
        SkillRegistry registry = SkillRegistry.defaults();
        registry.register(new SkillDefinition("test:blast", "test.blast", "", "test:blast", 0, 8,
                SkillDefinition.TargetType.CELL, false, true, true, 0, 1));
        registry.registerAdapter("test:blast", use -> SkillResult.success(0));
        EncounterRequest.CombatantSeed casterSeed = new EncounterRequest.CombatantSeed(caster, null, "caster", "monsters",
                new BattleCell(1, 1, 64), 50, 50, 100, 100, 20, 10, 6, 1, List.of("test:blast"));
        BattleSession session = new BattleSession(BattleId.create(), request(List.of(casterSeed,
                seed(player, player, "players", 4, 1, 1))), new ArenaGrid(ArenaDefinition.flat("ai-cell", 10, 64)),
                registry, 0, 0);
        session.finishDeployment();
        BattleAiScheduler.tick(session, 1, new HashMap<>());
        BattleEvent skill = session.snapshot().events().stream().filter(event -> event.actorId().equals(caster)
                && event.type() == BattleEvent.Type.SKILL).findFirst().orElseThrow();
        check(skill.toCell() != null && skill.toCell().distanceTo(new BattleCell(4, 1, 64)) <= 1,
                "AI cell skill targets a cell covering the enemy");
    }

    private static void aiMovesIntoCastRange() {
        UUID caster = UUID.randomUUID(), player = UUID.randomUUID();
        SkillRegistry registry = SkillRegistry.defaults();
        registry.register(new SkillDefinition("test:ray", "test.ray", "", "test:ray", 0, 2,
                SkillDefinition.TargetType.ENEMY, false, true, true, 1, 1));
        registry.registerAdapter("test:ray", use -> SkillResult.success(1));
        EncounterRequest.CombatantSeed casterSeed = new EncounterRequest.CombatantSeed(caster, null, "caster", "monsters",
                new BattleCell(0, 0, 64), 50, 50, 100, 100, 20, 10, 3, 1, List.of("test:ray"));
        BattleSession session = new BattleSession(BattleId.create(), request(List.of(casterSeed,
                seed(player, player, "players", 5, 0, 1))), new ArenaGrid(ArenaDefinition.flat("ai-range", 10, 64)),
                registry, 0, 0);
        session.finishDeployment();
        BattleAiScheduler.tick(session, 1, new HashMap<>());
        BattleEvent move = session.snapshot().events().stream().filter(event -> event.actorId().equals(caster)
                && event.type() == BattleEvent.Type.MOVE).findFirst().orElseThrow();
        check(move.toCell().x() >= 3, "ranged AI moves toward a position from which it can cast");
    }

    private static void aiFallsBackToBasicAttack() {
        UUID monster = UUID.randomUUID(), player = UUID.randomUUID();
        BattleSession session = new BattleSession(BattleId.create(), request(List.of(
                new EncounterRequest.CombatantSeed(monster, null, "monster", "monsters", new BattleCell(0, 0, 64),
                        50, 50, 0, 0, 0, 10, 6, 0, List.of()),
                seed(player, player, "players", 1, 0, 1))), new ArenaGrid(ArenaDefinition.flat("ai-basic", 8, 64)),
                SkillRegistry.defaults(), 0, 0);
        session.finishDeployment();
        BattleAiScheduler.tick(session, 1, new HashMap<>());
        check(session.snapshot().events().stream().anyMatch(event -> event.actorId().equals(monster)
                        && event.type() == BattleEvent.Type.SKILL && event.skillId().equals("exworld:basic_attack")),
                "AI uses the innate basic attack only after configured cards are unavailable");
    }

    private static void aiFinishesWhenNoActionExists() {
        UUID monster = UUID.randomUUID(), player = UUID.randomUUID();
        BattleSession session = new BattleSession(BattleId.create(), request(List.of(
                new EncounterRequest.CombatantSeed(monster, null, "monster", "monsters", new BattleCell(0, 0, 64),
                        50, 50, 0, 0, 0, 10, 0, 0, List.of()),
                seed(player, player, "players", 7, 0, 1))), new ArenaGrid(ArenaDefinition.flat("ai-finish", 8, 64)),
                SkillRegistry.defaults(), 0, 0);
        session.finishDeployment();
        BattleAiScheduler.tick(session, 1, new HashMap<>());
        check(session.snapshot().state() == BattleState.RESOLVING,
                "AI ends its faction phase when it has no legal skill or movement");
    }

    private static void actionPointsLimitMonster() {
        UUID monster = UUID.randomUUID(), player = UUID.randomUUID();
        EncounterRequest.CombatantSeed monsterSeed = new EncounterRequest.CombatantSeed(monster, null, "monster", "monsters",
                new BattleCell(0, 0, 64), 50, 50, 100, 100, 20, 10, 6, 1,
                List.of("exworld:guarded_strike"), 0);
        BattleSession session = new BattleSession(BattleId.create(), request(List.of(monsterSeed,
                seed(player, player, "players", 1, 0, 1))), new ArenaGrid(ArenaDefinition.flat("ai-action-points", 8, 64)),
                SkillRegistry.defaults(), 0, 0);
        session.finishDeployment();
        check(session.combatant(monster).orElseThrow().actionPoints() == 0, "explicit monster action_points is retained");
        BattleAiScheduler.tick(session, 1, new HashMap<>());
        check(session.snapshot().events().stream().noneMatch(event -> event.actorId().equals(monster)
                        && event.type() == BattleEvent.Type.SKILL),
                "monster AI cannot act when its action budget is zero");
    }

    private static void phaseAndCommandRules() {
        UUID player = UUID.randomUUID(), enemy = UUID.randomUUID(); BattleId id = BattleId.create();
        EncounterRequest request = request(List.of(seed(player, player, "players", 0, 0, 20), seed(enemy, null, "enemies", 3, 0, 10)));
        BattleSession session = new BattleSession(id, request, new ArenaGrid(ArenaDefinition.flat("arena", 8, 64)), SkillRegistry.defaults(), 0, 0);
        session.finishDeployment(); BattleSnapshot snapshot = session.snapshot(); check(snapshot.activeFaction().equals("players"), "initiative order");
        UUID card = snapshot.combatants().get(player).hand().stream().filter(value -> value.skillId().equals("exworld:guarded_strike")).findFirst().orElseThrow().instanceId();
        BattleCommand future = new BattleCommand.Move(id, snapshot.revision() + 1, UUID.randomUUID(), player, new BattleCell(1, 0, 64));
        check(!session.submit(future).accepted(), "future revision rejected");
        UUID commandId = UUID.randomUUID(); BattleCommand move = new BattleCommand.Move(id, snapshot.revision(), commandId, player, new BattleCell(1, 0, 64));
        CommandReceipt moved = session.submit(move); check(moved.accepted(), "active player moves"); check(session.submit(move).equals(moved), "command idempotence");
        check(session.snapshot().combatants().get(player).cell().x() == 0, "logical cell stays at movement origin during presentation");
        BattleSnapshot movedSnapshot = session.snapshot();
        CommandReceipt cast = session.submit(new BattleCommand.UseSkill(id, movedSnapshot.revision(), UUID.randomUUID(), player, card, new BattleCell(3,0,64), enemy));
        check(!cast.accepted(), "moving actor remains busy until presentation finishes");
        for (int i = 0; i < net.exmo.exworld.battle.action.BattleActionTimeline.TICKS_PER_CELL; i++) session.tick();
        check(session.snapshot().combatants().get(player).cell().x() == 1, "logical cell commits once when movement completes");
        BattleSnapshot beforeSkill = session.snapshot();
        UUID attack = beforeSkill.combatants().get(player).hand().stream().filter(value -> value.skillId().equals("exworld:guarded_strike")).findFirst().orElseThrow().instanceId();
        check(session.submit(new BattleCommand.UseSkill(id, beforeSkill.revision(), UUID.randomUUID(), player, attack, new BattleCell(2,0,64), enemy)).accepted(), "in-range skill succeeds");
        check(session.snapshot().events().getLast().skillId().equals("exworld:guarded_strike"), "successful skill creates battle log event");
        check(session.snapshot().combatants().get(player).statuses().stream().anyMatch(status -> status.id().equals("exworld:guarded")), "skill applies entity status");
        BattleSession movingSession = new BattleSession(BattleId.create(), request(List.of(seed(player, player, "players", 0, 0, 20), seed(UUID.randomUUID(), null, "enemies", 6, 0, 10))), new ArenaGrid(ArenaDefinition.flat("move", 8, 64)), SkillRegistry.defaults(), 0, 0);
        movingSession.finishDeployment(); BattleSnapshot moving = movingSession.snapshot();
        check(movingSession.submit(new BattleCommand.Move(moving.battleId(), moving.revision(), UUID.randomUUID(), player, new BattleCell(2, 0, 64))).accepted(), "movement succeeds");
        BattleEvent moveEvent = movingSession.snapshot().events().getLast();
        check(moveEvent.type() == BattleEvent.Type.MOVE && moveEvent.fromCell().x() == 0 && moveEvent.toCell().x() == 2, "movement event carries animation endpoints");
        session.submit(new BattleCommand.SetReady(id, session.snapshot().revision(), UUID.randomUUID(), player, true));
        for (int i = 0; i <= BattleSession.READY_GRACE_TICKS; i++) session.tick();
        check(!session.snapshot().activeFaction().equals("players"), "ready advances phase");
    }

    private static void readyCanBeWithdrawn() {
        UUID player = UUID.randomUUID(), enemy = UUID.randomUUID();
        BattleSession session = new BattleSession(BattleId.create(),
                request(List.of(seed(player, player, "players", 0, 0, 20), seed(enemy, null, "enemies", 3, 0, 10))),
                new ArenaGrid(ArenaDefinition.flat("ready-grace", 8, 64)), SkillRegistry.defaults(), 0, 0);
        session.finishDeployment();
        BattleSnapshot initial = session.snapshot();
        check(session.submit(new BattleCommand.SetReady(initial.battleId(), initial.revision(), UUID.randomUUID(), player, true)).accepted(),
                "player can end their action");
        session.tick();
        BattleSnapshot ended = session.snapshot();
        check(ended.state() == BattleState.FACTION_PHASE && ended.activeFaction().equals("players"),
                "ending action keeps a short withdrawal window before phase resolution");
        check(session.submit(new BattleCommand.SetReady(ended.battleId(), ended.revision(), UUID.randomUUID(), player, false)).accepted(),
                "continue action withdraws readiness during the grace window");
        session.tick();
        check(session.snapshot().activeFaction().equals("players") && !session.snapshot().readyPlayers().contains(player),
                "withdrawn readiness prevents the phase from advancing");
    }

    private static void multipleFactionOrder() {
        UUID player = UUID.randomUUID();
        EncounterRequest request = request(List.of(seed(player, player, "alpha", 0, 0, 10), seed(UUID.randomUUID(), null, "gamma", 2, 0, 30), seed(UUID.randomUUID(), null, "beta", 4, 0, 20)));
        BattleSession session = new BattleSession(BattleId.create(), request, new ArenaGrid(ArenaDefinition.flat("arena", 8, 64)), SkillRegistry.defaults(), 0, 0); session.finishDeployment();
        check(session.snapshot().factionOrder().equals(List.of("gamma", "beta", "alpha")), "three faction stable initiative order");
        BattleSession intro = new BattleSession(BattleId.create(), request, new ArenaGrid(ArenaDefinition.flat("intro", 8, 64)), SkillRegistry.defaults(), 0, 0);
        check(intro.snapshot().intro().totalTicks() == 8 + 35 * 3 + 25, "intro duration expands for every faction");
    }

    private static void engagementAdvantage() {
        UUID player = UUID.randomUUID(), enemy = UUID.randomUUID();
        check(EngagementAdvantage.fromWorldAttacker(player, player) == EngagementAdvantage.PLAYER_AMBUSH,
                "world player attack grants the player opening faction");
        check(EngagementAdvantage.fromWorldAttacker(enemy, player) == EngagementAdvantage.ENEMY_AMBUSH,
                "world enemy attack grants the enemy opening faction");
        EncounterRequest base = request(List.of(seed(player, player, "players", 0, 0, 5), seed(enemy, null, "enemies", 3, 0, 50)));
        EncounterRequest ambush = new EncounterRequest(base.encounterId(), base.arenaId(), base.combatants(), base.relations(),
                base.returnPoints(), base.seed(), base.variables(), "players");
        BattleSession session = new BattleSession(BattleId.create(), ambush, new ArenaGrid(ArenaDefinition.flat("arena", 8, 64)), SkillRegistry.defaults(), 0, 0);
        session.finishDeployment(); check(session.snapshot().activeFaction().equals("players"), "ambush overrides initiative for opening phase");
        session.finishActiveFaction(); session.tick(); check(session.snapshot().activeFaction().equals("enemies"), "fixed order continues after ambush opening");
    }

    private static void playerCardCollection() {
        PlayerCardCollection cards = new PlayerCardCollection(); cards.grant("a", 2); cards.grant("b", 4);
        check(cards.addToDeck("a") && cards.addToDeck("a"), "owned copies can enter deck");
        check(!cards.addToDeck("a"), "deck cannot exceed owned copies");
        check(cards.replaceDeck(List.of("a", "a", "b", "b", "b")), "valid five card deck replaces selection");
        check(cards.deckPlayable(), "five card deck playable");
        check(!cards.revoke("a", 1) && cards.copiesInDeck("a") == 2, "deck references protect owned instances");
    }

    private static void cardInstancesAndFusion() {
        PlayerCardCollection cards = new PlayerCardCollection(); cards.grant("fusion", 4);
        List<UUID> ids = cards.instances().stream().map(card -> card.id()).toList();
        check(cards.addToDeck(0, ids.getFirst()), "instance can be referenced by a deck preset");
        check(cards.fuse(ids.subList(0, 3)).isEmpty(), "referenced instance cannot be fusion material");
        check(cards.removeFromDeck(0, ids.getFirst()), "instance can be removed from preset");
        var fused = cards.fuse(ids.subList(0, 3)).orElseThrow();
        check(fused.star() == 2 && cards.count("fusion") == 2, "three equal one-star cards fuse into one two-star card");
        check(cards.decks().size() == 5 && cards.renameDeck(4, "Control"), "five independently named deck presets exist");
        cards.purgeCards(Set.of("fusion")); check(cards.count("fusion")==0 && cards.decks().stream().noneMatch(deck->!deck.cardIds().isEmpty()), "debug reset can purge referenced instances from every preset");
    }

    private static void resultFlow() {
        UUID player = UUID.randomUUID(), enemy = UUID.randomUUID();
        BattleSession session = new BattleSession(BattleId.create(), request(List.of(seed(player, player, "players", 0, 0, 20),
                seed(enemy, null, "enemies", 3, 0, 10))), new ArenaGrid(ArenaDefinition.flat("result", 8, 64)), SkillRegistry.defaults(), 0, 0);
        int[] publications = {0}; session.onResult(ignored -> publications[0]++); session.finishDeployment();
        session.finishDebug(BattleResult.Outcome.VICTORY);
        check(session.state() == BattleState.VICTORY, "outcome presentation remains on battlefield");
        for (int i = 0; i < BattleSession.OUTCOME_PRESENTATION_TICKS; i++) session.tick();
        BattleSnapshot reward = session.snapshot(); check(reward.state() == BattleState.REWARD, "outcome advances to reward page");
        BattleSnapshot.PlayerResultView view = reward.result().players().get(player);
        check(view != null && view.candidates().size() == 3 && view.gold() == 100, "victory offers three cards and gold");
        check(session.submit(new BattleCommand.SelectReward(reward.battleId(), reward.revision(), UUID.randomUUID(), player, view.candidates().getFirst())).accepted(), "reward selection accepted");
        BattleSnapshot selected = session.snapshot();
        check(session.submit(new BattleCommand.ConfirmResult(selected.battleId(), selected.revision(), UUID.randomUUID(), player)).accepted(), "result confirmation accepted");
        session.tick(); check(session.state() == BattleState.RETURNING && publications[0] == 0, "all confirmed players enter a synchronized returning stage");
        for (int i = 0; i < BattleSession.RETURNING_TICKS; i++) session.tick();
        check(session.state() == BattleState.CLEANUP && publications[0] == 1, "cleanup publishes the final result once");
        session.tick(); check(publications[0] == 1, "return callback is idempotent");
    }

    private static void disconnectedPlayerDoesNotBlockResult() {
        UUID online = UUID.randomUUID(), disconnected = UUID.randomUUID(), enemy = UUID.randomUUID();
        BattleSession session = new BattleSession(BattleId.create(), request(List.of(
                seed(online, online, "players", 0, 0, 20), seed(disconnected, disconnected, "players", 1, 0, 20),
                seed(enemy, null, "enemies", 3, 0, 10))), new ArenaGrid(ArenaDefinition.flat("disconnect-result", 8, 64)),
                SkillRegistry.defaults(), 0, 0);
        session.finishDeployment(); session.finishDebug(BattleResult.Outcome.VICTORY);
        for (int i = 0; i < BattleSession.OUTCOME_PRESENTATION_TICKS; i++) session.tick();
        check(session.autoSettleDisconnectedPlayers(Set.of(online)), "disconnected result is settled by the server");
        BattleSnapshot reward = session.snapshot();
        BattleSnapshot.PlayerResultView offlineResult = reward.result().players().get(disconnected);
        check(offlineResult.confirmed() && offlineResult.selectedCandidate() != null,
                "disconnected winner receives the deterministic fallback reward");
        BattleSnapshot.PlayerResultView onlineResult = reward.result().players().get(online);
        check(session.submit(new BattleCommand.SelectReward(reward.battleId(), reward.revision(), UUID.randomUUID(), online,
                onlineResult.candidates().getFirst())).accepted(), "online player can still choose their reward");
        BattleSnapshot selected = session.snapshot();
        check(session.submit(new BattleCommand.ConfirmResult(selected.battleId(), selected.revision(), UUID.randomUUID(), online)).accepted(),
                "online player confirms without waiting for the disconnected teammate");
        session.tick();
        check(session.state() == BattleState.RETURNING, "disconnected teammate no longer keeps the result page open");
    }

    private static void resultTimeoutAutoSettles() {
        UUID player = UUID.randomUUID(), enemy = UUID.randomUUID();
        BattleSession session = new BattleSession(BattleId.create(), request(List.of(seed(player, player, "players", 0, 0, 20),
                seed(enemy, null, "enemies", 3, 0, 10))), new ArenaGrid(ArenaDefinition.flat("timeout-result", 8, 64)),
                SkillRegistry.defaults(), 0, 0);
        session.finishDeployment(); session.finishDebug(BattleResult.Outcome.VICTORY);
        for (int i = 0; i < BattleSession.OUTCOME_PRESENTATION_TICKS + BattleSession.RESULT_TIMEOUT_TICKS; i++) session.tick();
        BattleSnapshot snapshot = session.snapshot();
        BattleSnapshot.PlayerResultView result = snapshot.result().players().get(player);
        check(snapshot.state() == BattleState.RETURNING, "result timeout advances to returning automatically");
        check(result.confirmed() && result.selectedCandidate() != null,
                "result timeout auto-selects the first reward and confirms it");
    }

    private static void defeatedPlayerCanConfirmResult() {
        UUID player = UUID.randomUUID(), enemy = UUID.randomUUID();
        EncounterRequest.CombatantSeed defeated = new EncounterRequest.CombatantSeed(player, player, "Defeated", "players",
                new BattleCell(0, 0, 64), 50, 0, 100, 100, 20, 20, 6,
                List.of("exworld:guarded_strike", "exworld:first_aid", "exworld:guarded_strike", "exworld:first_aid", "exworld:guarded_strike"));
        BattleSession session = new BattleSession(BattleId.create(), request(List.of(defeated, seed(enemy, null, "enemies", 3, 0, 10))),
                new ArenaGrid(ArenaDefinition.flat("defeat-result", 8, 64)), SkillRegistry.defaults(), 0, 0);
        session.finishDeployment();
        session.finishDebug(BattleResult.Outcome.DEFEAT);
        for (int i = 0; i < BattleSession.OUTCOME_PRESENTATION_TICKS; i++) session.tick();
        BattleSnapshot reward = session.snapshot();
        check(reward.state() == BattleState.REWARD && reward.combatants().get(player).downed(), "defeated player reaches result page while downed");
        check(session.submit(new BattleCommand.ConfirmResult(reward.battleId(), reward.revision(), UUID.randomUUID(), player)).accepted(),
                "downed player can click continue on defeat result");
    }

    private static void cardStarConfigurationAndFreeze() {
        SkillRegistry registry = SkillRegistry.defaults();
        registry.registerCard(new CardDefinition("test:ranked", "exworld:guarded_strike", "card.test.ranked", "card.test.ranked.desc", "test:ranked.png",
                "rare", Set.of("melee"), false, List.of(
                new CardDefinition.StarTier(12,1,Map.of("power",4D)), new CardDefinition.StarTier(11,1,Map.of("power",6D)),
                new CardDefinition.StarTier(10,2,Map.of("power",8D)), new CardDefinition.StarTier(9,2,Map.of("power",10D)),
                new CardDefinition.StarTier(8,3,Map.of("power",12D)))));
        var ranked = registry.resolve("test:ranked", 5); check(ranked.manaCost()==8 && ranked.range()==3 && ranked.power()==12, "five-star tier uses explicit values");
        SkillRegistry frozen = registry.snapshot(); registry.register(new net.exmo.exworld.battle.skill.SkillDefinition("exworld:guarded_strike", "changed", "", "exworld", 99, 9,
                net.exmo.exworld.battle.skill.SkillDefinition.TargetType.ENEMY, true,false,false,99,1));
        check(frozen.resolve("test:ranked",5).manaCost()==8, "active battle content snapshot remains frozen after reload");
        registry.registerCard(new CardDefinition("test:retain","exworld:guarded_strike","n","d","i","common",Set.of("retain"),false,List.of(new CardDefinition.StarTier(1,1,Map.of()),new CardDefinition.StarTier(1,1,Map.of()),new CardDefinition.StarTier(1,1,Map.of()),new CardDefinition.StarTier(1,1,Map.of()),new CardDefinition.StarTier(1,1,Map.of()))));check(registry.retained("test:retain"),"retain tag controls phase cleanup");
        boolean rejected=false;try{new CardDefinition("bad","missing","n","d","i","common",Set.of(),false,List.of(new CardDefinition.StarTier(1,1,Map.of())));}catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,"card definitions require all five star tiers");
    }

    private static void starterCardDistribution() {
        SkillRegistry skills = SkillRegistry.defaults(true);
        PlayerCardModule module = new PlayerCardModule(skills);
        PlayerCardCollection cards = new PlayerCardCollection();
        check(module.ensureStarterDeck(cards), "fresh collection receives a starter deck");
        check(cards.owned().size() == 8, "starter initialization does not grant all debug cards");
        check(cards.deck().equals(skills.debugCardIds().subList(0, 8)), "starter deck is deterministic");
        check(!module.ensureStarterDeck(cards), "starter initialization is idempotent");
        check(cards.owned().size() == 8, "repeated initialization grants no extra cards");
        skills.debugCardIds().forEach(id -> { if (cards.count(id) == 0) cards.grant(id, 1); });
        check(cards.owned().size() == 36, "explicit catalog grant remains available for debug coverage");
    }

    private static void cardContentAndLoopRules() {
        SkillRegistry production = SkillRegistry.defaults();
        check(production.debugCardIds().isEmpty(), "production registry keeps debug cards disabled");
        check(production.cardType("exworld:basic_attack") == CardDefinition.CardType.ATTACK, "basic attack is an attack card");
        check(production.cardType("exworld:iron_will") == CardDefinition.CardType.POWER, "iron will is a power card");
        check(production.card("exworld:slimed").orElseThrow().playable() == false, "status card is unplayable");
        check(production.card("exworld:regret").orElseThrow().type() == CardDefinition.CardType.CURSE, "curse card is permanent content");
        check(production.card("exworld:brace").orElseThrow().effect("block") == 8D, "brace exposes a concrete block effect");
        check(production.card("exworld:deep_breath").orElseThrow().effect("draw") == 2D, "deep breath is a concrete block-and-draw skill");
        check(production.card("exworld:insight").orElseThrow().has(CardDefinition.CardKeyword.EXHAUST), "insight exhausts after drawing");
        check(production.card("exworld:battle_trance").orElseThrow().type() == CardDefinition.CardType.POWER, "battle trance is a power card");
        check(production.card("exworld:battle_plan").orElseThrow().has(CardDefinition.CardKeyword.RETAIN), "battle plan retains itself");
        check(production.allCardIds().containsAll(List.of("exworld:focus", "exworld:insight", "exworld:regret")), "administrative catalog exposes all standard cards");

        DeckState deck = new DeckState(List.of("exworld:quick_thought"), 99L);
        deck.drawInitial(1); deck.endPhase(ignored -> false);
        check(deck.drawCards(10_000) == 1, "large draw requests terminate without a reshuffle loop");
        CardDefinition keywords = new CardDefinition("test:keywords", "exworld:defend", "n", "d", "i", "common",
                Set.of("retain", "ethereal"), false, Collections.nCopies(5, new CardDefinition.StarTier(1, 0, Map.of())));
        check(keywords.has(CardDefinition.CardKeyword.RETAIN) && keywords.has(CardDefinition.CardKeyword.ETHEREAL), "card keywords normalize from tags");
    }

    private static void debugCardCatalog() {
        SkillRegistry skills = SkillRegistry.defaults(true);
        check(skills.debugCardIds().size() == 36, "default registry contains the 36 built-in debug cards");
        check(skills.debugCardIds().getFirst().equals("exworld:debug_card_01"), "debug ids stable");
        check(skills.debugCardIds().getLast().equals("exworld:debug_card_36"), "debug ids complete");
        check(Math.abs(skills.definition("exworld:debug_card_01").orElseThrow().power() - 2.4) < .001, "debug power is reduced exactly sixty percent");
    }

    private static void genericAndWarriorSkills() {
        genericAttackUsesWeaponAndChebyshev();
        dodgeStepsOutOfHitCells();
        dodgeStillHitIfBlocked();
        guardReducesPhysicalAndHaltsDash();
        interceptCutsPathAndReturnsMovement();
        quickMeditationRestoresMana();
        basicStrikeFollowsWeaponFamily();
        cleaveHitsThreeCells();
        leapSlashFormula();
        tauntForcesAi();
        raiseShieldWritesGuardTwo();
        battleAuraSpendsManaAndClearsOnSwitch();
        playerActionPointBudget();
    }

    private static void genericAttackUsesWeaponAndChebyshev() {
        UUID player = UUID.randomUUID(), enemy = UUID.randomUUID();
        BattleSession session = battle(skillSeed(player, player, "players", 0, 0, 30, -1, 100, List.of("exworld:defend")),
                skillSeed(enemy, null, "enemies", 1, 1, 1, 3, 100, List.of("exworld:defend")));
        Combatant actor = session.combatant(player).orElseThrow();
        actor.setWeaponAttack(8);
        float mana = actor.mana();
        check(play(session, player, "exworld:basic_attack", new BattleCell(1, 1, 64), enemy), "diagonal Chebyshev attack is legal");
        check(Math.abs(session.combatant(enemy).orElseThrow().health() - 42) < .001, "generic attack deals configured weapon attack");
        check(Math.abs(actor.mana() - mana) < .001, "generic attack costs no mana");
    }

    private static void dodgeStepsOutOfHitCells() {
        UUID player = UUID.randomUUID(), enemy = UUID.randomUUID();
        BattleSession session = battle(skillSeed(player, player, "players", 4, 4, 30, -1, 100, List.of("exworld:dodge", "exworld:dodge", "exworld:dodge")),
                skillSeed(enemy, null, "enemies", 5, 4, 1, 3, 100, List.of("exworld:defend")));
        session.combatant(enemy).orElseThrow().setWeaponAttack(10);
        check(play(session, player, "exworld:dodge", new BattleCell(4, 3, 64), null), "dodge accepts a ring cell");
        check(session.combatant(player).orElseThrow().dodgeDz() == -1, "dodge stores the selected facing");
        passToNextFaction(session);
        check(play(session, enemy, "exworld:basic_attack", new BattleCell(4, 4, 64), player), "enemy attacks the original cell");
        waitIdle(session);
        check(session.combatant(player).orElseThrow().cell().equals(new BattleCell(4, 3, 64)), "dodge steps along the stored facing");
        check(Math.abs(session.combatant(player).orElseThrow().health() - 50) < .001, "leaving the hit cell grants immunity");
    }

    private static void dodgeStillHitIfBlocked() {
        UUID player = UUID.randomUUID(), enemy = UUID.randomUUID();
        BattleSession session = battle(skillSeed(player, player, "players", 4, 4, 30, -1, 100, List.of("exworld:dodge", "exworld:dodge", "exworld:dodge")),
                skillSeed(enemy, null, "enemies", 5, 4, 1, 3, 100, List.of("exworld:defend")));
        session.combatant(enemy).orElseThrow().setWeaponAttack(10);
        check(play(session, player, "exworld:dodge", new BattleCell(5, 4, 64), null), "dodge toward an occupied cell is still a legal facing");
        passToNextFaction(session);
        check(play(session, enemy, "exworld:basic_attack", new BattleCell(4, 4, 64), player), "enemy still attacks the occupied origin");
        waitIdle(session);
        check(session.combatant(player).orElseThrow().cell().equals(new BattleCell(4, 4, 64)), "blocked dodge stays on the hit cell");
        check(Math.abs(session.combatant(player).orElseThrow().health() - 40) < .001, "a dodge that stays in the hit cell still takes damage");
    }

    private static void guardReducesPhysicalAndHaltsDash() {
        UUID player = UUID.randomUUID(), enemy = UUID.randomUUID();
        BattleSession session = battle(skillSeed(player, player, "players", 1, 0, 30, -1, 100, List.of("exworld:guard", "exworld:guard", "exworld:guard")),
                skillSeed(enemy, null, "enemies", 0, 0, 1, 3, 100, List.of("exworld:defend")));
        session.combatant(enemy).orElseThrow().setWeaponAttack(10);
        check(play(session, player, "exworld:guard", new BattleCell(1, 0, 64), player), "guard is a self skill");
        check(stacks(session, player, "exworld:guarding") == 1, "guard writes guarding 1");
        passToNextFaction(session);
        check(play(session, enemy, "exworld:basic_attack", new BattleCell(1, 0, 64), player), "enemy lands a physical hit on the guarding player");
        waitIdle(session);
        check(Math.abs(session.combatant(player).orElseThrow().health() - 41) < .001, "guarding 1 reduces physical damage by 10 percent");

        UUID dasher = UUID.randomUUID(), blocker = UUID.randomUUID();
        BattleSession dash = battle(skillSeed(blocker, blocker, "players", 2, 0, 30, -1, 100, List.of("exworld:guard", "exworld:guard", "exworld:guard")),
                skillSeed(dasher, null, "enemies", 0, 0, 1, 3, 100, List.of("exworld:dash", "exworld:dash", "exworld:dash")));
        check(play(dash, blocker, "exworld:guard", new BattleCell(2, 0, 64), blocker), "guard is ready before the enemy dash");
        passToNextFaction(dash);
        check(play(dash, dasher, "exworld:dash", new BattleCell(4, 0, 64), null), "enemy dash is accepted toward a cell beyond the guard");
        waitIdle(dash);
        check(dash.combatant(dasher).orElseThrow().cell().equals(new BattleCell(1, 0, 64)), "guarding stops skill displacement in front of the player");
    }

    private static void interceptCutsPathAndReturnsMovement() {
        UUID player = UUID.randomUUID(), enemy = UUID.randomUUID();
        BattleSession session = battle(skillSeed(player, player, "players", 1, 0, 30, 3, 100, List.of("exworld:intercept", "exworld:intercept", "exworld:intercept")),
                skillSeed(enemy, null, "enemies", 5, 0, 1, 3, 100, List.of("exworld:defend")));
        Combatant interceptor = session.combatant(player).orElseThrow();
        Combatant mover = session.combatant(enemy).orElseThrow();
        mover.setWeaponAttack(10);
        check(play(session, player, "exworld:intercept", new BattleCell(2, 0, 64), null), "intercept spends remaining action points");
        check(stacks(session, player, "exworld:intercept") == 3, "intercept rank equals remaining action points");
        check(interceptor.actionPointsRemaining() == 0, "intercept drains the action-point budget");
        passToNextFaction(session);
        int movement = mover.movementRemaining();
        BattleSnapshot snap = session.snapshot();
        check(session.submit(new BattleCommand.Move(snap.battleId(), snap.revision(), UUID.randomUUID(), enemy, new BattleCell(3, 0, 64))).accepted(),
                "enemy ordinary move crosses the intercept ray");
        waitIdle(session);
        check(interceptor.cell().equals(new BattleCell(3, 0, 64)), "interceptor relocates to the intersection");
        check(mover.cell().equals(new BattleCell(4, 0, 64)), "enemy stops in front of the interceptor");
        check(mover.movementRemaining() == movement - 1, "unwalked movement is not spent");
        check(Math.abs(interceptor.health() - 38.5) < .001, "interceptor takes enemy weapon attack times 1 + rank * 5 percent");
    }

    private static void quickMeditationRestoresMana() {
        UUID player = UUID.randomUUID(), enemy = UUID.randomUUID();
        EncounterRequest.CombatantSeed playerSeed = new EncounterRequest.CombatantSeed(player, player, player.toString(), "players",
                new BattleCell(0, 0, 64), 50, 50, 100, 20, 0, 30, 6, 3,
                List.of("exworld:quick_meditation", "exworld:quick_meditation", "exworld:quick_meditation"), -1);
        BattleSession session = battle(playerSeed, skillSeed(enemy, null, "enemies", 6, 0, 1, 3, 100, List.of("exworld:defend")));
        check(play(session, player, "exworld:quick_meditation", new BattleCell(0, 0, 64), player), "quick meditation is a self skill");
        check(Math.abs(session.combatant(player).orElseThrow().mana() - 29) < .001, "meditation spends 1 mana then restores max(10, 5 percent of cap)");
    }

    private static void basicStrikeFollowsWeaponFamily() {
        UUID player = UUID.randomUUID(), enemy = UUID.randomUUID();
        BattleSession sword = battle(skillSeed(player, player, "players", 0, 0, 30, -1, 100, List.of("exworld:basic_strike", "exworld:basic_strike", "exworld:basic_strike")),
                skillSeed(enemy, null, "enemies", 2, 0, 1, 3, 100, List.of("exworld:defend")));
        Combatant actor = sword.combatant(player).orElseThrow();
        actor.configureEquipment("minecraft:iron_sword", "", 1);
        actor.setWeaponAttack(10);
        actor.setStrengthLevel(1);
        BattleSnapshot snap = sword.snapshot();
        check(snap.combatants().get(player).hand().stream().anyMatch(card -> card.skillId().equals("exworld:basic_strike") && card.manaCost() == 1 && card.range() == 2),
                "sword basic strike advertises 1 mana and range 2");
        check(play(sword, player, "exworld:basic_strike", new BattleCell(2, 0, 64), enemy), "sword strike reaches Chebyshev 2");
        check(Math.abs(sword.combatant(enemy).orElseThrow().health() - 38) < .001, "sword strike deals strength * 2 + weapon");
        check(Math.abs(actor.mana() - 99) < .001, "sword strike costs 1 mana");

        UUID heavyUser = UUID.randomUUID(), heavyTarget = UUID.randomUUID();
        BattleSession heavy = battle(skillSeed(heavyUser, heavyUser, "players", 0, 0, 30, -1, 100, List.of("exworld:basic_strike", "exworld:basic_strike", "exworld:basic_strike")),
                skillSeed(heavyTarget, null, "enemies", 1, 0, 1, 3, 100, List.of("exworld:defend")));
        Combatant bruiser = heavy.combatant(heavyUser).orElseThrow();
        bruiser.configureEquipment("minecraft:iron_axe", "", 1);
        bruiser.setWeaponAttack(10);
        bruiser.setStrengthLevel(1);
        check(play(heavy, heavyUser, "exworld:basic_strike", new BattleCell(1, 0, 64), heavyTarget), "heavy strike reaches Chebyshev 3");
        check(Math.abs(heavy.combatant(heavyTarget).orElseThrow().health() - 37) < .001, "heavy strike deals strength * 3 + weapon before exhaustion");
        check(Math.abs(bruiser.mana() - 98) < .001, "heavy strike costs 2 mana");
        check(stacks(heavy, heavyUser, "exworld:exhaustion") == 1, "heavy strike applies exhaustion after the hit");
        waitIdle(heavy);
        check(play(heavy, heavyUser, "exworld:basic_attack", new BattleCell(1, 0, 64), heavyTarget), "exhausted unit can still attack");
        check(Math.abs(heavy.combatant(heavyTarget).orElseThrow().health() - 34.5) < .001, "exhaustion reduces subsequent physical output to 25 percent");
    }

    private static void cleaveHitsThreeCells() {
        UUID player = UUID.randomUUID(), a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        BattleSession session = battle(List.of(
                skillSeed(player, player, "players", 4, 4, 30, -1, 100, List.of("exworld:cleave", "exworld:cleave", "exworld:cleave")),
                skillSeed(a, null, "enemies", 5, 4, 1, 3, 100, List.of("exworld:defend")),
                skillSeed(b, null, "enemies", 5, 3, 1, 3, 100, List.of("exworld:defend")),
                skillSeed(c, null, "enemies", 5, 5, 1, 3, 100, List.of("exworld:defend"))));
        Combatant actor = session.combatant(player).orElseThrow();
        actor.configureEquipment("minecraft:iron_sword", "", 1);
        actor.setWeaponAttack(10);
        actor.setStrengthLevel(1);
        check(play(session, player, "exworld:cleave", new BattleCell(5, 4, 64), null), "cleave accepts a ring cell");
        check(Math.abs(session.combatant(a).orElseThrow().health() - 44.5) < .001, "cleave hits the selected cell");
        check(Math.abs(session.combatant(b).orElseThrow().health() - 44.5) < .001, "cleave hits the previous ring cell");
        check(Math.abs(session.combatant(c).orElseThrow().health() - 44.5) < .001, "cleave hits the next ring cell");
    }

    private static void leapSlashFormula() {
        UUID player = UUID.randomUUID(), enemy = UUID.randomUUID();
        BattleSession session = battle(skillSeed(player, player, "players", 0, 0, 30, -1, 100, List.of("exworld:leap_slash", "exworld:leap_slash", "exworld:leap_slash")),
                skillSeed(enemy, null, "enemies", 1, 0, 1, 3, 100, List.of("exworld:defend")));
        Combatant actor = session.combatant(player).orElseThrow();
        actor.setWeaponAttack(10);
        actor.setStrengthLevel(1);
        check(play(session, player, "exworld:leap_slash", new BattleCell(1, 0, 64), enemy), "leap slash hits an adjacent enemy");
        check(Math.abs(session.combatant(enemy).orElseThrow().health() - 33) < .001, "leap slash deals strength * 5 + weapon * 1.2");
    }

    private static void tauntForcesAi() {
        UUID player = UUID.randomUUID(), decoy = UUID.randomUUID(), monster = UUID.randomUUID();
        BattleSession session = battle(List.of(
                skillSeed(player, player, "players", 1, 0, 30, 3, 100, List.of("exworld:taunt", "exworld:taunt", "exworld:taunt")),
                skillSeed(decoy, decoy, "players", 0, 1, 30, 3, 100, List.of("exworld:defend")),
                skillSeed(monster, null, "monsters", 0, 0, 1, 3, 0, List.of())));
        check(play(session, player, "exworld:taunt", new BattleCell(1, 0, 64), player), "taunt is a self skill");
        check(stacks(session, player, "exworld:taunt") == 2, "taunt stacks equal ceil of remaining action points over 2");
        passToNextFaction(session);
        BattleAiScheduler.tick(session, 1, new HashMap<>());
        check(session.snapshot().events().stream().anyMatch(event -> event.actorId().equals(monster)
                        && event.type() == BattleEvent.Type.SKILL && event.skillId().equals("exworld:basic_attack")
                        && player.equals(event.targetId())),
                "AI must attack the highest taunt when no other forced target exists");
        check(session.snapshot().events().stream().noneMatch(event -> event.actorId().equals(monster)
                        && event.type() == BattleEvent.Type.SKILL && decoy.equals(event.targetId())),
                "AI does not spend its attack on a non-taunt adjacent decoy");
    }

    private static void raiseShieldWritesGuardTwo() {
        UUID player = UUID.randomUUID(), enemy = UUID.randomUUID();
        BattleSession session = battle(skillSeed(player, player, "players", 0, 0, 30, -1, 100, List.of("exworld:guard", "exworld:raise_shield", "exworld:raise_shield")),
                skillSeed(enemy, null, "enemies", 6, 0, 1, 3, 100, List.of("exworld:defend")));
        check(play(session, player, "exworld:guard", new BattleCell(0, 0, 64), player), "existing guard is rank 1");
        waitIdle(session);
        check(play(session, player, "exworld:raise_shield", new BattleCell(0, 0, 64), player), "raise shield overwrites guard");
        check(stacks(session, player, "exworld:guarding") == 2, "raise shield writes guarding 2 instead of stacking to 3");
    }

    private static void battleAuraSpendsManaAndClearsOnSwitch() {
        UUID player = UUID.randomUUID(), enemy = UUID.randomUUID();
        BattleSession session = battle(skillSeed(player, player, "players", 0, 0, 30, -1, 100, List.of("exworld:battle_aura", "exworld:battle_aura", "exworld:battle_aura")),
                skillSeed(enemy, null, "enemies", 1, 0, 1, 3, 100, List.of("exworld:defend")));
        Combatant actor = session.combatant(player).orElseThrow();
        actor.configureEquipment("minecraft:iron_sword", "minecraft:iron_axe", 1);
        actor.setWeaponAttack(10);
        check(play(session, player, "exworld:battle_aura", new BattleCell(0, 0, 64), player), "battle aura is a self skill");
        check(stacks(session, player, "exworld:battle_aura") == 1, "battle aura is a lasting status");
        waitIdle(session);
        check(play(session, player, "exworld:basic_attack", new BattleCell(1, 0, 64), enemy), "aura carrier can still attack");
        check(Math.abs(session.combatant(enemy).orElseThrow().health() - 38) < .001, "aura adds 20 percent of weapon attack");
        check(Math.abs(actor.mana() - 97) < .001, "aura costs 2 to play and 1 more on the hit");
        check(session.syncActiveWeapon(actor, 2), "switching weapons is legal");
        check(stacks(session, player, "exworld:battle_aura") == 0, "switching weapons clears battle aura");
    }

    private static void playerActionPointBudget() {
        UUID player = UUID.randomUUID(), enemy = UUID.randomUUID();
        BattleSession session = battle(skillSeed(player, player, "players", 0, 0, 30, 3, 100, List.of("exworld:defend")),
                skillSeed(enemy, null, "enemies", 7, 0, 1, 3, 100, List.of("exworld:defend")));
        BattleSnapshot snap = session.snapshot();
        check(session.submit(new BattleCommand.Move(snap.battleId(), snap.revision(), UUID.randomUUID(), player, new BattleCell(1, 0, 64))).accepted(), "first budgeted action is accepted");
        waitIdle(session);
        snap = session.snapshot();
        check(session.submit(new BattleCommand.Move(snap.battleId(), snap.revision(), UUID.randomUUID(), player, new BattleCell(2, 0, 64))).accepted(), "second budgeted action is accepted");
        waitIdle(session);
        snap = session.snapshot();
        check(session.submit(new BattleCommand.Move(snap.battleId(), snap.revision(), UUID.randomUUID(), player, new BattleCell(3, 0, 64))).accepted(), "third budgeted action is accepted");
        waitIdle(session);
        snap = session.snapshot();
        check(!session.submit(new BattleCommand.Move(snap.battleId(), snap.revision(), UUID.randomUUID(), player, new BattleCell(4, 0, 64))).accepted(),
                "a fourth action is refused when the player budget is 3");
    }

    private static BattleSession battle(EncounterRequest.CombatantSeed... seeds) {
        return battle(List.of(seeds));
    }

    private static BattleSession battle(List<EncounterRequest.CombatantSeed> seeds) {
        BattleSession session = new BattleSession(BattleId.create(), request(seeds),
                new ArenaGrid(ArenaDefinition.flat("skills", 12, 64)), SkillRegistry.defaults(), 0, 0);
        session.finishDeployment();
        return session;
    }

    private static EncounterRequest.CombatantSeed skillSeed(UUID id, UUID player, String faction, int x, int z,
                                                            double initiative, int actionPoints, float mana, List<String> deck) {
        int hand = Math.min(5, deck.size());
        return new EncounterRequest.CombatantSeed(id, player, id.toString(), faction, new BattleCell(x, z, 64),
                50, 50, 100, mana, 20, initiative, 6, hand, deck, actionPoints);
    }

    private static boolean play(BattleSession session, UUID actor, String skillId, BattleCell cell, UUID target) {
        BattleSnapshot snap = session.snapshot();
        UUID card = snap.combatants().get(actor).hand().stream().filter(view -> view.skillId().equals(skillId))
                .findFirst().orElseThrow().instanceId();
        return session.submit(new BattleCommand.UseSkill(snap.battleId(), snap.revision(), UUID.randomUUID(), actor, card, cell, target)).accepted();
    }

    private static void waitIdle(BattleSession session) {
        for (int i = 0; i < 60; i++) {
            if (session.actions().moves().isEmpty() && session.actionLocks().isEmpty()) return;
            session.tick();
        }
    }

    private static void passToNextFaction(BattleSession session) {
        waitIdle(session);
        session.finishActiveFaction();
        for (int i = 0; i < 20; i++) {
            session.tick();
            if (session.snapshot().state() == BattleState.FACTION_PHASE) return;
        }
    }

    private static int stacks(BattleSession session, UUID id, String status) {
        return session.snapshot().combatants().get(id).statuses().stream()
                .filter(view -> view.id().equals(status)).mapToInt(BattleSnapshot.StatusView::stacks).findFirst().orElse(0);
    }

    private static EncounterRequest request(List<EncounterRequest.CombatantSeed> seeds) {
        Set<String> factions = new HashSet<>(); seeds.forEach(seed -> factions.add(seed.factionId()));
        Map<EncounterRequest.FactionPair, FactionRelation> relations = new HashMap<>(); factions.forEach(a -> factions.forEach(b -> relations.put(new EncounterRequest.FactionPair(a,b), a.equals(b) ? FactionRelation.FRIENDLY : FactionRelation.HOSTILE)));
        return new EncounterRequest("test", "arena", seeds, relations, Map.of(), 42, Map.of());
    }
    private static EncounterRequest.CombatantSeed seed(UUID id, UUID player, String faction, int x, int z, double initiative) {
        return new EncounterRequest.CombatantSeed(id, player, id.toString(), faction, new BattleCell(x,z,64), 50, 50, 100, 100, 20, initiative, 6, List.of("exworld:guarded_strike", "exworld:first_aid", "exworld:guarded_strike", "exworld:first_aid", "exworld:guarded_strike"));
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
