package net.exmo.exworld.battle.persistence;

import net.exmo.exworld.battle.api.EncounterRequest;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/** Durable return tickets survive disconnects, cleanup and server restart. */
public final class BattleReturnSavedData extends SavedData {
    public static final Factory<BattleReturnSavedData> FACTORY = new Factory<>(BattleReturnSavedData::new, BattleReturnSavedData::load);
    private final Map<UUID, Ticket> tickets = new LinkedHashMap<>();
    public void put(UUID player, EncounterRequest.ReturnPoint point, GameType mode) { tickets.put(player,new Ticket(point,mode));setDirty(); }
    public Optional<Ticket> get(UUID player){return Optional.ofNullable(tickets.get(player));}
    public void remove(UUID player){if(tickets.remove(player)!=null)setDirty();}
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries){ListTag values=new ListTag();tickets.forEach((id,ticket)->{var point=ticket.point();CompoundTag value=new CompoundTag();value.putUUID("player",id);value.putString("dimension",point.dimension());value.putDouble("x",point.x());value.putDouble("y",point.y());value.putDouble("z",point.z());value.putFloat("yaw",point.yaw());value.putFloat("pitch",point.pitch());value.putString("mode",ticket.mode().name());values.add(value);});tag.put("tickets",values);return tag;}
    private static BattleReturnSavedData load(CompoundTag tag,HolderLookup.Provider registries){BattleReturnSavedData data=new BattleReturnSavedData();ListTag values=tag.getList("tickets",Tag.TAG_COMPOUND);for(int i=0;i<values.size();i++){CompoundTag value=values.getCompound(i);var point=new EncounterRequest.ReturnPoint(value.getString("dimension"),value.getDouble("x"),value.getDouble("y"),value.getDouble("z"),value.getFloat("yaw"),value.getFloat("pitch"));GameType mode;try{mode=GameType.valueOf(value.getString("mode"));}catch(Exception ignored){mode=GameType.SURVIVAL;}data.tickets.put(value.getUUID("player"),new Ticket(point,mode));}return data;}
    public record Ticket(EncounterRequest.ReturnPoint point, GameType mode){}
}
