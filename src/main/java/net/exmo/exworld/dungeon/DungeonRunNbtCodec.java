package net.exmo.exworld.dungeon;

import net.exmo.exworld.battle.api.BattleId;
import net.exmo.exworld.battle.api.EncounterRequest;
import net.exmo.exworld.dungeon.model.DungeonRunState;
import net.minecraft.nbt.*;

import java.util.*;

/** Minecraft persistence adapter kept outside the pure dungeon aggregate. */
public final class DungeonRunNbtCodec {
    private DungeonRunNbtCodec() {}

    public static CompoundTag save(DungeonRun run) {
        CompoundTag tag=new CompoundTag();tag.putUUID("id",run.id());tag.putString("dungeon",run.dungeonId());tag.putLong("seed",run.seed());tag.putInt("slot_x",run.slotX());tag.putInt("slot_z",run.slotZ());tag.putString("biome",run.biomeId());tag.putString("state",run.state().name());tag.putInt("room_index",run.roomIndex());tag.putBoolean("boss",run.bossDefeated());
        ListTag memberTags=new ListTag();run.members().forEach(value->{CompoundTag t=new CompoundTag();t.putUUID("id",value);memberTags.add(t);});tag.put("members",memberTags);
        ListTag returnTags=new ListTag();run.returns().forEach((player,point)->{CompoundTag t=new CompoundTag();t.putUUID("player",player);t.putString("dimension",point.dimension());t.putDouble("x",point.x());t.putDouble("y",point.y());t.putDouble("z",point.z());t.putFloat("yaw",point.yaw());t.putFloat("pitch",point.pitch());returnTags.add(t);});tag.put("returns",returnTags);
        ListTag claimed=new ListTag();run.claimedTreasures().forEach(value->claimed.add(StringTag.valueOf(value)));tag.put("claimed",claimed);
        ListTag completed=new ListTag();run.completedRooms().forEach(value->completed.add(StringTag.valueOf(value)));tag.put("completed",completed);
        ListTag enemies=new ListTag();run.enemyEntities().forEach((key,id)->{CompoundTag value=new CompoundTag();value.putString("key",key);value.putUUID("id",id);enemies.add(value);});tag.put("enemies",enemies);
        if(run.battleId()!=null)tag.putUUID("battle",run.battleId().value());return tag;
    }

    public static DungeonRun load(CompoundTag tag){
        List<UUID> members=new ArrayList<>();ListTag memberTags=tag.getList("members",Tag.TAG_COMPOUND);for(int i=0;i<memberTags.size();i++)members.add(memberTags.getCompound(i).getUUID("id"));
        DungeonRun run=new DungeonRun(tag.getUUID("id"),tag.getString("dungeon"),tag.getLong("seed"),tag.getInt("slot_x"),tag.getInt("slot_z"),members,tag.contains("biome",Tag.TAG_STRING)?tag.getString("biome"):"prairie");
        try{run.state(DungeonRunState.valueOf(tag.getString("state")));}catch(Exception ignored){}run.roomIndex(tag.getInt("room_index"));run.bossDefeated(tag.getBoolean("boss"));
        ListTag returns=tag.getList("returns",Tag.TAG_COMPOUND);for(int i=0;i<returns.size();i++){CompoundTag t=returns.getCompound(i);run.addReturn(t.getUUID("player"),new EncounterRequest.ReturnPoint(t.getString("dimension"),t.getDouble("x"),t.getDouble("y"),t.getDouble("z"),t.getFloat("yaw"),t.getFloat("pitch")));}
        ListTag claimed=tag.getList("claimed",Tag.TAG_STRING);for(int i=0;i<claimed.size();i++)run.claimTreasure(claimed.getString(i));
        ListTag completed=tag.getList("completed",Tag.TAG_STRING);for(int i=0;i<completed.size();i++)run.completeRoom(completed.getString(i));
        ListTag enemies=tag.getList("enemies",Tag.TAG_COMPOUND);for(int i=0;i<enemies.size();i++){CompoundTag value=enemies.getCompound(i);run.enemyEntity(value.getString("key"),value.getUUID("id"));}
        if(tag.hasUUID("battle"))run.battleId(new BattleId(tag.getUUID("battle")));return run;
    }
}
