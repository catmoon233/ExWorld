package net.exmo.exworld.dungeon;

import net.exmo.exworld.battle.api.BattleId;
import net.exmo.exworld.battle.api.EncounterRequest;
import net.exmo.exworld.dungeon.model.*;
import java.util.*;

/** Mutable aggregate for one party's isolated dungeon run. */
public final class DungeonRun {
    private final UUID id; private final String dungeonId; private final long seed;
    private final int slotX, slotZ; private final List<UUID> members; private final String biomeId;
    private final Map<String, UUID> enemyEntities = new LinkedHashMap<>();
    private final Map<UUID, EncounterRequest.ReturnPoint> returns = new LinkedHashMap<>();
    private final Set<String> claimedTreasures = new LinkedHashSet<>();
    private final Set<String> completedRooms = new LinkedHashSet<>();
    private DungeonRunState state = DungeonRunState.ENTERING;
    private int roomIndex; private boolean bossDefeated; private BattleId battleId;

    public DungeonRun(UUID id, String dungeonId, long seed, int slotX, int slotZ, List<UUID> members) {
        this(id, dungeonId, seed, slotX, slotZ, members, "prairie");
    }
    public DungeonRun(UUID id, String dungeonId, long seed, int slotX, int slotZ, List<UUID> members, String biomeId) {
        this.id=id; this.dungeonId=dungeonId; this.seed=seed; this.slotX=slotX; this.slotZ=slotZ; this.members=new ArrayList<>(members);
        this.biomeId=biomeId == null || biomeId.isBlank() ? "prairie" : biomeId;
    }
    public UUID id(){return id;} public String dungeonId(){return dungeonId;} public long seed(){return seed;}
    public int slotX(){return slotX;} public int slotZ(){return slotZ;} public List<UUID> members(){return List.copyOf(members);}
    public String biomeId(){return biomeId;}
    public Map<UUID, EncounterRequest.ReturnPoint> returns(){return Map.copyOf(returns);}
    public void addReturn(UUID player, EncounterRequest.ReturnPoint point){returns.put(player,point);}
    public DungeonRunState state(){return state;} public void state(DungeonRunState value){state=value;}
    public int roomIndex(){return roomIndex;} public void roomIndex(int value){roomIndex=value;}
    public boolean bossDefeated(){return bossDefeated;} public void bossDefeated(boolean value){bossDefeated=value;}
    public Set<String> claimedTreasures(){return Set.copyOf(claimedTreasures);}
    public boolean claimTreasure(String room){return claimedTreasures.add(room);}
    public Set<String> completedRooms(){return Set.copyOf(completedRooms);}
    public boolean completeRoom(String room){return completedRooms.add(room);}
    public BattleId battleId(){return battleId;} public void battleId(BattleId value){battleId=value;}
    public Map<String, UUID> enemyEntities(){return Map.copyOf(enemyEntities);}
    public UUID enemyEntity(String key){return enemyEntities.get(key);}
    public void enemyEntity(String key, UUID entityId){if(entityId==null)enemyEntities.remove(key);else enemyEntities.put(key,entityId);}
    public DungeonSnapshot snapshot(String roomId){return new DungeonSnapshot(id,dungeonId,state,seed,slotX,slotZ,roomIndex,roomId,Set.copyOf(members),claimedTreasures,bossDefeated,battleId);}

}
