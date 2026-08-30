package net.exmo.exworld.dungeon;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.exmo.exworld.Exworld;
import net.exmo.exworld.content.ExWorldContent;
import net.exmo.exworld.battle.BattleSession;
import net.exmo.exworld.battle.BattleSystem;
import net.exmo.exworld.battle.api.*;
import net.exmo.exworld.battle.arena.ArenaDefinition;
import net.exmo.exworld.battle.combatant.Combatant;
import net.exmo.exworld.battle.card.PlayerCardModule;
import net.exmo.exworld.battle.model.*;
import net.exmo.exworld.network.BattleNetwork;
import net.exmo.exworld.dungeon.model.*;
import net.exmo.exworld.world.WorldSystem;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class DungeonSystem {
    public static final ResourceKey<Level> DUNGEON_LEVEL = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "dungeon"));
    public static final String DEFAULT_DUNGEON = "exworld:training_dungeon";
    private static final String RUN_TAG = "exworld.dungeon.run.";
    private static final Map<UUID, DungeonRun> RUNS = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> PLAYER_RUNS = new ConcurrentHashMap<>();
    private static final Set<UUID> STARTING = new HashSet<>();
    private static boolean restored;

    private DungeonSystem() {}
    public static Optional<DungeonRun> run(UUID player) { UUID id=PLAYER_RUNS.get(player); return id==null?Optional.empty():Optional.ofNullable(RUNS.get(id)); }
    public static boolean active(UUID player) { return PLAYER_RUNS.containsKey(player); }
    public static ServerLevel level(MinecraftServer server) { return server.getLevel(DUNGEON_LEVEL); }
    public static void registerEvents() { NeoForge.EVENT_BUS.register(DungeonSystem.class); NeoForge.EVENT_BUS.addListener(DungeonDataReloadListener::register); }
    public static void restoreForBattle(MinecraftServer server) { if (!restored) { restore(server); restored = true; } }
    public static Optional<ArenaDefinition> battleArena(UUID runId, String roomId) {
        DungeonRun run=RUNS.get(runId); if(run==null)return Optional.empty();
        return DungeonContent.definition(run.dungeonId()).flatMap(definition->definition.rooms().stream().filter(room->room.id().equals(roomId)).findFirst())
                .map(room->new ArenaDefinition("exworld:dungeon_"+run.id()+"_"+room.id(),18,18,room.floorY(),1,Map.of(),room.blocked(),Map.of()));
    }

    @SubscribeEvent public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server=event.getServer(); if(!restored){restore(server);restored=true;}
        for(DungeonRun run:List.copyOf(RUNS.values())){
            if(run.state()==DungeonRunState.EXPLORING) tickExploration(server,run);
            if(run.state()==DungeonRunState.CLEANUP||run.state()==DungeonRunState.FAILED) cleanup(server,run);
            if(RUNS.containsKey(run.id())) saved(server).put(run);
        }
    }
    private static void tickExploration(MinecraftServer server,DungeonRun run) {
        DungeonDefinition definition=DungeonContent.definition(run.dungeonId()).orElse(null); ServerLevel level=level(server);
        if(definition==null||level==null||run.roomIndex()>=definition.rooms().size())return;
        DungeonRoomDefinition room=definition.rooms().get(run.roomIndex());
        List<ServerPlayer> players=run.members().stream().map(server.getPlayerList()::getPlayer).filter(Objects::nonNull).filter(p->p.level()==level).toList();
        if(players.isEmpty()){run.state(DungeonRunState.FAILED);return;}
        sync(players, run, room.id());
        if(room.type()==DungeonRoomType.TREASURE&&!run.claimedTreasures().contains(room.id())){
            BlockPos center=roomCenter(run,room);for(ServerPlayer player:players)if(player.blockPosition().distSqr(center)<16)claimTreasure(player,run,room);
        }
        if(room.type()==DungeonRoomType.EXIT&&run.bossDefeated()){
            BlockPos exit=new BlockPos(DungeonSceneBuilder.originX(run,run.roomIndex())+room.exit().x(),room.floorY(),DungeonSceneBuilder.originZ(run)+room.exit().z());
            for(ServerPlayer player:players)if(player.blockPosition().distSqr(exit)<16)complete(server,run);
        }
        int roomOriginX = DungeonSceneBuilder.originX(run, run.roomIndex());
        if ((room.type() == DungeonRoomType.ENTRANCE || room.type() == DungeonRoomType.TREASURE)
                && players.stream().anyMatch(player -> player.getX() >= roomOriginX + room.width() - 2)) {
            advanceRoom(server, run, definition);
            return;
        }
        for(ServerPlayer player:players){
            if(run.state()!=DungeonRunState.EXPLORING)break;
            Entity enemy=level.getEntities(player,player.getBoundingBox().inflate(room.alertRadius()),value->value instanceof Enemy&&value.isAlive()&&value.getTags().contains(RUN_TAG+run.id())&&!BattleSystem.isParticipating(value.getUUID())).stream().findFirst().orElse(null);
            if(enemy instanceof LivingEntity living){startEncounter(server,run,player,living,EngagementAdvantage.INITIATIVE);break;}
        }
    }
    private static BlockPos roomCenter(DungeonRun run,DungeonRoomDefinition room){return new BlockPos(DungeonSceneBuilder.originX(run,run.roomIndex())+room.width()/2,room.floorY(),DungeonSceneBuilder.originZ(run)+room.depth()/2);}
    private static void claimTreasure(ServerPlayer player,DungeonRun run,DungeonRoomDefinition room){
        if(run.claimTreasure(room.id())){for(UUID id:run.members()){ServerPlayer member=player.getServer().getPlayerList().getPlayer(id);BattleSystem.playerCards().awardDungeonGold(player.getServer(),run.id().toString(),room.id(),id,room.goldReward());if(member!=null)member.sendSystemMessage(Component.translatable("dungeon.exworld.treasure",room.goldReward()));}advanceRoom(player.getServer(),run,DungeonContent.definition(run.dungeonId()).orElseThrow());}
    }
    private static void complete(MinecraftServer server, DungeonRun run) {
        run.state(DungeonRunState.COMPLETED);
        for (UUID id : run.members()) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) { EncounterRequest.ReturnPoint point = run.returns().get(id); if (point != null) teleport(player, point); player.sendSystemMessage(Component.translatable("dungeon.exworld.completed")); BattleNetwork.sendDungeon(player,null); }
        }
        run.state(DungeonRunState.CLEANUP);
    }

    private static synchronized void startEncounter(MinecraftServer server,DungeonRun run,ServerPlayer trigger,LivingEntity enemy,EngagementAdvantage advantage) {
        if(run.state()!=DungeonRunState.EXPLORING||STARTING.contains(run.id())||BattleSystem.isParticipating(trigger.getUUID()))return;
        DungeonDefinition definition=DungeonContent.definition(run.dungeonId()).orElse(null);if(definition==null||run.roomIndex()>=definition.rooms().size())return;
        DungeonRoomDefinition room=definition.rooms().get(run.roomIndex());List<ServerPlayer> players=run.members().stream().map(server.getPlayerList()::getPlayer).filter(Objects::nonNull).toList();
        if(players.isEmpty()||players.stream().anyMatch(p->BattleSystem.isParticipating(p.getUUID())))return;STARTING.add(run.id());
        try{
            PlayerCardModule cards=BattleSystem.playerCards();List<EncounterRequest.CombatantSeed> seeds=new ArrayList<>();
            for(int i=0;i<players.size();i++){ServerPlayer player=players.get(i);cards.ensureStarterDeck(server,player.getUUID());seeds.add(new EncounterRequest.CombatantSeed(player.getUUID(),player.getUUID(),player.getGameProfile().getName(),"players",new BattleCell(2,4+i*2,room.floorY()),player.getMaxHealth(),player.getHealth(),100,100,12,20-i,6,3,cards.battleDeck(server,player.getUUID()),cards.battleDeckStars(server,player.getUUID()),(int)player.getAttributeValue(net.exmo.exworld.battle.attribute.BattleAttributes.ACTION_POINTS)));}
            DungeonEnemyDefinition enemyDefinition=chooseEnemy(room,run.seed(),enemy);
            seeds.add(new EncounterRequest.CombatantSeed(enemy.getUUID(),null,enemyDefinition.name(),"enemies",new BattleCell(14,4,room.floorY()),enemyDefinition.maxHealth(),Math.max(1,enemy.getHealth()),enemyDefinition.maxMana(),enemyDefinition.maxMana(),12,enemyDefinition.initiative(),enemyDefinition.movementPoints(),enemyDefinition.initialHandSize(),enemyDefinition.deck()));
            Map<EncounterRequest.FactionPair,FactionRelation> relations=Map.of(new EncounterRequest.FactionPair("players","enemies"),FactionRelation.HOSTILE,new EncounterRequest.FactionPair("enemies","players"),FactionRelation.HOSTILE);
            Map<UUID,EncounterRequest.ReturnPoint> returns=new LinkedHashMap<>();for(ServerPlayer player:players)returns.put(player.getUUID(),point(player));
            int originX=DungeonSceneBuilder.originX(run,run.roomIndex()),originZ=DungeonSceneBuilder.originZ(run);String opening=advantage==EngagementAdvantage.ENEMY_AMBUSH?"enemies":"players";
            String arenaId="exworld:dungeon_"+run.id()+"_"+room.id();
            BattleSystem.registerArena(new ArenaDefinition(arenaId,18,18,room.floorY(),1,Map.of(),room.blocked(),Map.of()));
            EncounterRequest request=new EncounterRequest("exworld:dungeon_"+run.dungeonId(),arenaId,seeds,relations,returns,run.seed()^enemy.getUUID().getLeastSignificantBits(),Map.of("deployment_ticks","0","biome",run.biomeId()),opening,null,null,BattleHost.dungeon("exworld:dungeon",originX,originZ,room.floorY(),run.id(),room.id()));
            BattleId battleId=BattleSystem.startHostedEncounter(request,players);run.battleId(battleId);run.state(DungeonRunState.BATTLE);sync(players,run,room.id());
        }catch(Exception error){Exworld.LOGGER.error("Could not start dungeon encounter",error);run.state(DungeonRunState.FAILED);}finally{STARTING.remove(run.id());}
    }
    private static DungeonEnemyDefinition chooseEnemy(DungeonRoomDefinition room,long seed,LivingEntity entity){if(!room.enemies().isEmpty())return DungeonEncounterPicker.pick(room.enemies(),seed);return new DungeonEnemyDefinition("fallback",BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(),entity.getName().getString(),entity.getMaxHealth(),60,10,4,3,List.of());}
    private static EncounterRequest.ReturnPoint point(ServerPlayer player){return new EncounterRequest.ReturnPoint(player.level().dimension().location().toString(),player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot());}
    private static void teleport(ServerPlayer player,EncounterRequest.ReturnPoint point){ServerLevel target=BattleSystem.levelFor(point.dimension());if(target!=null)player.teleportTo(target,point.x(),point.y(),point.z(),Set.of(),point.yaw(),point.pitch());}

    public static void onBattleResult(BattleSession session,BattleResult result){
        UUID runId=session.request().host().dungeonRunId();DungeonRun run=runId==null?null:RUNS.get(runId);if(run==null)return;
        if(result.outcome()!=BattleResult.Outcome.VICTORY){run.state(DungeonRunState.FAILED);return;}
        DungeonDefinition definition=DungeonContent.definition(run.dungeonId()).orElse(null);if(definition==null){run.state(DungeonRunState.FAILED);return;}
        DungeonRoomDefinition room=definition.rooms().get(run.roomIndex());run.completeRoom(room.id());if(room.type()==DungeonRoomType.BOSS)run.bossDefeated(true);run.battleId(null);run.roomIndex(run.roomIndex()+1);run.state(run.roomIndex()>=definition.rooms().size()?DungeonRunState.COMPLETED:DungeonRunState.EXPLORING);if(run.state()==DungeonRunState.EXPLORING){advanceRoomPlayers(run,definition);setExplorationAi(BattleSystem.levelFor(session),run,definition,run.roomIndex());syncRun(session,run,definition.rooms().get(run.roomIndex()).id());}
    }

    @SubscribeEvent public static void onDamage(LivingIncomingDamageEvent event){
        if(!(event.getEntity().level() instanceof ServerLevel level)||level.dimension()!=DUNGEON_LEVEL)return;
        Entity source=event.getSource().getEntity(); if(source instanceof Projectile projectile&&projectile.getOwner()!=null)source=projectile.getOwner();
        if(source instanceof ServerPlayer player&&active(player.getUUID())&&event.getEntity() instanceof LivingEntity enemy&&enemy instanceof Enemy&&!BattleSystem.isParticipating(player.getUUID()))
            run(player.getUUID()).filter(value->enemy.getTags().contains(RUN_TAG+value.id())).ifPresent(value->{event.setCanceled(true);startEncounter(player.getServer(),value,player,enemy,EngagementAdvantage.PLAYER_AMBUSH);});
        else if(event.getEntity() instanceof ServerPlayer player&&active(player.getUUID())&&source instanceof LivingEntity enemy&&enemy instanceof Enemy&&!BattleSystem.isParticipating(player.getUUID()))
            run(player.getUUID()).filter(value->enemy.getTags().contains(RUN_TAG+value.id())).ifPresent(value->{event.setCanceled(true);startEncounter(player.getServer(),value,player,enemy,EngagementAdvantage.ENEMY_AMBUSH);});
    }
    @SubscribeEvent public static void onStopped(ServerStoppedEvent event){RUNS.clear();PLAYER_RUNS.clear();STARTING.clear();restored=false;}
    public static void onPlayerLogin(ServerPlayer player){DungeonRun run=run(player.getUUID()).orElse(null);if(run==null)BattleNetwork.sendDungeon(player,null);else sync(List.of(player),run,run.roomIndex()>=0?DungeonContent.definition(run.dungeonId()).map(value->value.rooms().get(Math.min(run.roomIndex(),value.rooms().size()-1)).id()).orElse("current"):"current");}

    public static int enter(ServerPlayer leader,String dungeonId,BlockPos entrance){if(entrance!=null&&leader.blockPosition().distSqr(entrance)>256)return 0;return enter(leader,dungeonId);}
    public static int enter(ServerPlayer leader,String dungeonId){
        if(active(leader.getUUID())||BattleSystem.isParticipating(leader.getUUID())||!BattleSystem.partyLeader(leader.getUUID()).equals(leader.getUUID()))return 0;DungeonDefinition definition=DungeonContent.definition(dungeonId).orElse(null);if(definition==null)return 0;List<UUID> members=BattleSystem.partyMembers(leader.getUUID());if(members.isEmpty())members=List.of(leader.getUUID());if(members.size()>4||!members.contains(leader.getUUID())||members.stream().anyMatch(DungeonSystem::active)||members.stream().anyMatch(id->leader.getServer().getPlayerList().getPlayer(id)==null)||members.stream().anyMatch(BattleSystem::isParticipating))return 0;ServerLevel level=level(leader.getServer());if(level==null)return 0;
        DungeonSlotAllocator.Slot slot=DungeonSlotAllocator.allocate(RUNS.values());DungeonRun run=new DungeonRun(UUID.randomUUID(),dungeonId,leader.getServer().overworld().getGameTime()^leader.getUUID().getLeastSignificantBits(),slot.x(),slot.z(),members,WorldSystem.currentBiomeId(leader));
        for(UUID id:members){ServerPlayer player=leader.getServer().getPlayerList().getPlayer(id);run.addReturn(id,point(player));PLAYER_RUNS.put(id,run.id());}RUNS.put(run.id(),run);DungeonSceneBuilder.build(level,run,definition);spawnEnemies(level,run,definition);forceChunks(level,run,definition,true);run.state(DungeonRunState.EXPLORING);setExplorationAi(level,run,definition,run.roomIndex());DungeonRoomDefinition room=definition.rooms().getFirst();
        for(UUID id:members){ServerPlayer player=leader.getServer().getPlayerList().getPlayer(id);player.teleportTo(level,DungeonSceneBuilder.originX(run,0)+room.entrance().x()+.5,room.floorY()+1,DungeonSceneBuilder.originZ(run)+room.entrance().z()+.5,0,0);}syncRun(run,room.id(),leader.getServer());persist(leader.getServer(),run);return 1;
    }
    private static void spawnEnemies(ServerLevel level,DungeonRun run,DungeonDefinition definition){for(int index=0;index<definition.rooms().size();index++){DungeonRoomDefinition room=definition.rooms().get(index);for(int i=0;i<room.enemies().size();i++){DungeonEnemyDefinition value=room.enemies().get(i);String key=index+":"+i;UUID existing=run.enemyEntity(key);Entity entity=existing==null?null:level.getEntity(existing);LivingEntity living=entity instanceof LivingEntity found?found:null;if(living==null){EntityType<?> type=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(value.entityType()));entity=type.create(level);if(!(entity instanceof LivingEntity created))continue;living=created;run.enemyEntity(key,living.getUUID());level.addFreshEntity(entity);}living.addTag(RUN_TAG+run.id());living.setCustomName(Component.literal(value.name()));var maxHealth=living.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);if(maxHealth!=null){maxHealth.setBaseValue(value.maxHealth());living.setHealth(Math.min(value.maxHealth(),living.getHealth()<=0?value.maxHealth():living.getHealth()));}var spawn=room.enemySpawns().isEmpty()?new ArenaDefinition.GridPoint(room.width()/2+i,room.depth()/2):room.enemySpawns().get(Math.min(i,room.enemySpawns().size()-1));living.moveTo(DungeonSceneBuilder.originX(run,index)+spawn.x()+.5,room.floorY()+1,DungeonSceneBuilder.originZ(run)+spawn.z()+.5);}}}
    private static void setExplorationAi(ServerLevel level,DungeonRun run,DungeonDefinition definition,int activeRoom){for(int index=0;index<definition.rooms().size();index++){DungeonRoomDefinition room=definition.rooms().get(index);for(int i=0;i<room.enemies().size();i++){Entity entity=level.getEntity(run.enemyEntity(index+":"+i));if(entity instanceof Mob mob)mob.setNoAi(index!=activeRoom);}}}
    public static int leave(ServerPlayer player){DungeonRun run=run(player.getUUID()).orElse(null);if(run==null)return 0;run.state(DungeonRunState.FAILED);return 1;}
    private static void cleanup(MinecraftServer server,DungeonRun run){if(run.state()==DungeonRunState.FAILED)for(UUID id:run.members()){PLAYER_RUNS.remove(id);ServerPlayer player=server.getPlayerList().getPlayer(id);if(player!=null){EncounterRequest.ReturnPoint point=run.returns().get(id);if(point!=null)teleport(player,point);player.sendSystemMessage(Component.translatable("dungeon.exworld.failed"));BattleNetwork.sendDungeon(player,null);}}ServerLevel level=level(server);if(level!=null)level.getAllEntities().forEach(entity->{if(entity.getTags().contains(RUN_TAG+run.id()))entity.discard();});DungeonContent.definition(run.dungeonId()).ifPresent(definition->{forceChunks(level,run,definition,false);DungeonSceneBuilder.clear(level,run,definition);});run.members().forEach(PLAYER_RUNS::remove);RUNS.remove(run.id());saved(server).remove(run.id());}
    private static void advanceRoom(MinecraftServer server,DungeonRun run,DungeonDefinition definition){if(run.roomIndex()+1>=definition.rooms().size())return;run.completeRoom(definition.rooms().get(run.roomIndex()).id());run.roomIndex(run.roomIndex()+1);ServerLevel level=level(server);setExplorationAi(level,run,definition,run.roomIndex());advanceRoomPlayers(run,definition);}
    private static void advanceRoomPlayers(DungeonRun run,DungeonDefinition definition){DungeonRoomDefinition room=definition.rooms().get(run.roomIndex());for(UUID id:run.members()){ServerPlayer player=BattleSystem.player(id);if(player!=null){ServerLevel level=level(player.getServer());if(level!=null)player.teleportTo(level,DungeonSceneBuilder.originX(run,run.roomIndex())+room.entrance().x()+.5,room.floorY()+1,DungeonSceneBuilder.originZ(run)+room.entrance().z()+.5,0,0);}}}
    private static void forceChunks(ServerLevel level,DungeonRun run,DungeonDefinition definition,boolean forced){if(level==null||definition.rooms().isEmpty())return;DungeonRoomDefinition first=definition.rooms().getFirst(),last=definition.rooms().getLast();int minX=DungeonSceneBuilder.originX(run,0),maxX=DungeonSceneBuilder.originX(run,definition.rooms().size()-1)+last.width(),minZ=DungeonSceneBuilder.originZ(run),maxZ=minZ+Math.max(first.depth(),last.depth());for(int cx=minX>>4;cx<=maxX>>4;cx++)for(int cz=minZ>>4;cz<=maxZ>>4;cz++)level.setChunkForced(cx,cz,forced);}
    private static void persist(MinecraftServer server,DungeonRun run){saved(server).put(run);}
    private static DungeonSavedData saved(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(DungeonSavedData.FACTORY,"exworld_dungeon_runs");}
    private static void restore(MinecraftServer server){for(DungeonRun run:saved(server).runs()){if(run.state().terminal())continue;DungeonDefinition definition=DungeonContent.definition(run.dungeonId()).orElse(null);ServerLevel level=level(server);if(definition==null||level==null){run.state(DungeonRunState.FAILED);continue;}RUNS.put(run.id(),run);run.members().forEach(id->PLAYER_RUNS.put(id,run.id()));DungeonSceneBuilder.build(level,run,definition);spawnEnemies(level,run,definition);forceChunks(level,run,definition,true);if(run.state()==DungeonRunState.ENTERING)run.state(DungeonRunState.EXPLORING);if(run.state()!=DungeonRunState.BATTLE)setExplorationAi(level,run,definition,run.roomIndex());}}
    private static void syncRun(BattleSession session,DungeonRun run,String roomId){if(session==null)return;sync(session.combatants().stream().map(Combatant::playerId).filter(Objects::nonNull).map(BattleSystem::player).filter(Objects::nonNull).toList(),run,roomId);}
    private static void syncRun(DungeonRun run,String roomId,MinecraftServer server){sync(run.members().stream().map(server.getPlayerList()::getPlayer).filter(Objects::nonNull).toList(),run,roomId);}
    private static void sync(List<ServerPlayer> players,DungeonRun run,String roomId){for(ServerPlayer player:players)BattleNetwork.sendDungeon(player,run.snapshot(roomId));}
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("exworld").then(Commands.literal("dungeon")
                .then(Commands.literal("enter").then(Commands.argument("id",StringArgumentType.word())
                        .executes(c->enter(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"id")))))
                .then(Commands.literal("status").executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();DungeonRun r=run(p.getUUID()).orElse(null);p.sendSystemMessage(Component.literal(r==null?"No dungeon run":r.snapshot("current").toString()));return r==null?0:1;}))
                .then(Commands.literal("leave").executes(c->leave(c.getSource().getPlayerOrException())))
                .then(Commands.literal("place").requires(source->source.hasPermission(2)).executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();BlockPos pos=p.blockPosition().relative(p.getDirection(),2);p.serverLevel().setBlockAndUpdate(pos,ExWorldContent.DUNGEON_ENTRANCE.get().defaultBlockState());return 1;}))));
    }
}
