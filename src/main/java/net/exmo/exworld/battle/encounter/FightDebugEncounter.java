package net.exmo.exworld.battle.encounter;

import net.exmo.exworld.battle.api.EncounterRequest;
import net.exmo.exworld.battle.attribute.BattleAttributes;
import net.exmo.exworld.battle.card.PlayerCardModule;
import net.exmo.exworld.battle.model.*;
import net.exmo.exworld.world.WorldSystem;
import net.exmo.exworld.monster.MonsterProfileRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;

import java.util.*;

/** Builds the debug encounter request from live entities while keeping trigger code free of battle construction rules. */
public final class FightDebugEncounter {
    public static final String PLAYER_FACTION = "players";
    public static final String ENEMY_FACTION = "enemies";
    private static final List<String> ENEMY_DECK = List.of(
            "exworld:guarded_strike", "exworld:first_aid", "exworld:defend",
            "exworld:gain_energy", "exworld:quick_thought", "exworld:iron_will");

    private FightDebugEncounter() {}

    public static EncounterRequest create(List<ServerPlayer> players, List<LivingEntity> enemies,
                                          EngagementAdvantage advantage, PlayerCardModule cards, long seed) {
        String openingFaction = switch (advantage) {
            case PLAYER_AMBUSH -> PLAYER_FACTION;
            case ENEMY_AMBUSH -> ENEMY_FACTION;
            case INITIATIVE -> null;
        };
        List<EncounterRequest.CombatantSeed> seeds=new ArrayList<>();
        for(int i=0;i<players.size();i++){ServerPlayer player=players.get(i);float maxMana=(float)player.getAttributeValue(AttributeRegistry.MAX_MANA),mana=MagicData.getPlayerMagicData(player).getMana();float regen=maxMana*(float)player.getAttributeValue(AttributeRegistry.MANA_REGEN)*.01F*20F;
            seeds.add(new EncounterRequest.CombatantSeed(player.getUUID(),player.getUUID(),player.getGameProfile().getName(),PLAYER_FACTION,new BattleCell(3,Math.max(2,9+(i-(players.size()-1)/2)*2),64),player.getMaxHealth(),player.getHealth(),maxMana,mana,regen,player.getAttributeValue(BattleAttributes.INITIATIVE),(int)player.getAttributeValue(BattleAttributes.MOVEMENT_POINTS),(int)player.getAttributeValue(BattleAttributes.INITIAL_HAND_SIZE),cards.battleDeck(player.getServer(),player.getUUID()),cards.battleDeckStars(player.getServer(),player.getUUID()),(int)player.getAttributeValue(BattleAttributes.ACTION_POINTS)));}
        for(int i=0;i<enemies.size();i++){LivingEntity enemy=enemies.get(i);var profile=MonsterProfileRegistry.active().map(registry->registry.resolve(enemy)).orElseGet(net.exmo.exworld.monster.ResolvedMonsterProfile::defaultHostile);String name=profile.displayName().isBlank()?enemy.getDisplayName().getString():profile.displayName();List<String> deck=profile.skills().stream().map(net.exmo.exworld.monster.MonsterProfilePatch.Skill::id).toList();List<Integer> stars=profile.skills().stream().map(net.exmo.exworld.monster.MonsterProfilePatch.Skill::star).toList();seeds.add(new EncounterRequest.CombatantSeed(enemy.getUUID(),null,name,ENEMY_FACTION,new BattleCell(14,Math.max(2,9+(i-(enemies.size()-1)/2)*2),64),profile.maxHealth(),profile.health(),profile.maxMana(),profile.mana(),profile.manaPerPhase(),profile.initiative(),profile.movementPoints(),profile.initialHandSize(),deck,stars,profile.actionPoints(),profile.drawPerPhase()));}
        Map<EncounterRequest.FactionPair, FactionRelation> relations = Map.of(
                new EncounterRequest.FactionPair(PLAYER_FACTION, ENEMY_FACTION), FactionRelation.HOSTILE,
                new EncounterRequest.FactionPair(ENEMY_FACTION, PLAYER_FACTION), FactionRelation.HOSTILE);
        Map<UUID,EncounterRequest.ReturnPoint> returns=new LinkedHashMap<>();for(ServerPlayer player:players)returns.put(player.getUUID(),new EncounterRequest.ReturnPoint(player.level().dimension().location().toString(),player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot()));
        return new EncounterRequest("exworld:fight_debug", "exworld:flat_18", seeds, relations,
                returns, seed,
                Map.of("engagement_advantage", advantage.name(), "deployment_ticks", "0",
                        "biome", players.isEmpty() ? "prairie" : WorldSystem.currentBiomeId(players.getFirst())), openingFaction);
    }
}
