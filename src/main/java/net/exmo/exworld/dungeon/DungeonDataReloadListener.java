package net.exmo.exworld.dungeon;

import com.google.gson.*;
import net.exmo.exworld.Exworld;
import net.exmo.exworld.battle.arena.ArenaDefinition;
import net.exmo.exworld.dungeon.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.*;

public final class DungeonDataReloadListener extends SimpleJsonResourceReloadListener {
    public DungeonDataReloadListener(){super(new Gson(),"exworld_dungeons");}
    public static void register(AddReloadListenerEvent event){event.addListener(new DungeonDataReloadListener());}
    @Override protected void apply(Map<ResourceLocation,JsonElement> entries,ResourceManager resources,ProfilerFiller profiler){
        try{List<DungeonDefinition> values=new ArrayList<>();for(var entry:entries.entrySet()){JsonObject json=entry.getValue().getAsJsonObject();values.add(parse(entry.getKey(),json));}if(!values.isEmpty())DungeonContent.replace(values);Exworld.LOGGER.info("Loaded {} dungeon definitions",values.size());}
        catch(Exception error){Exworld.LOGGER.error("Rejected dungeon content reload",error);}
    }
    private static DungeonDefinition parse(ResourceLocation file,JsonObject json){
        String id=json.has("id")?json.get("id").getAsString():file.toString();List<DungeonRoomDefinition> rooms=new ArrayList<>();
        for(JsonElement element:json.getAsJsonArray("rooms")){JsonObject value=element.getAsJsonObject();Set<ArenaDefinition.GridPoint> blocked=new LinkedHashSet<>();if(value.has("blocked"))for(JsonElement point:value.getAsJsonArray("blocked")){JsonArray p=point.getAsJsonArray();blocked.add(new ArenaDefinition.GridPoint(p.get(0).getAsInt(),p.get(1).getAsInt()));}
            List<DungeonEnemyDefinition> enemies=new ArrayList<>();if(value.has("enemies"))for(JsonElement elementEnemy:value.getAsJsonArray("enemies")){JsonObject enemy=elementEnemy.getAsJsonObject();List<String> deck=new ArrayList<>();if(enemy.has("deck"))enemy.getAsJsonArray("deck").forEach(card->deck.add(card.getAsString()));enemies.add(new DungeonEnemyDefinition(enemy.get("id").getAsString(),enemy.get("entity_type").getAsString(),enemy.has("name")?enemy.get("name").getAsString():enemy.get("id").getAsString(),enemy.has("max_health")?enemy.get("max_health").getAsFloat():40,enemy.has("max_mana")?enemy.get("max_mana").getAsFloat():60,enemy.has("initiative")?enemy.get("initiative").getAsDouble():10,enemy.has("movement_points")?enemy.get("movement_points").getAsInt():4,enemy.has("initial_hand_size")?enemy.get("initial_hand_size").getAsInt():3,deck));}
            int width=value.has("width")?value.get("width").getAsInt():32,depth=value.has("depth")?value.get("depth").getAsInt():24;
            ArenaDefinition.GridPoint entrance=point(value,"entrance",new ArenaDefinition.GridPoint(width/2,0)),exit=point(value,"exit",new ArenaDefinition.GridPoint(width/2,depth-1));
            List<ArenaDefinition.GridPoint> enemySpawns=new ArrayList<>();if(value.has("enemy_spawns"))for(JsonElement spawn:value.getAsJsonArray("enemy_spawns")){JsonArray p=spawn.getAsJsonArray();enemySpawns.add(new ArenaDefinition.GridPoint(p.get(0).getAsInt(),p.get(1).getAsInt()));}
            rooms.add(new DungeonRoomDefinition(value.get("id").getAsString(),DungeonRoomType.valueOf(value.get("type").getAsString().toUpperCase(Locale.ROOT)),width,depth,value.has("floor_y")?value.get("floor_y").getAsInt():64,value.has("arena")?value.get("arena").getAsString():"exworld:flat_18",value.has("alert_radius")?value.get("alert_radius").getAsInt():8,blocked,enemies,value.has("gold")?value.get("gold").getAsInt():0,entrance,exit,enemySpawns));}
        return new DungeonDefinition(id,json.has("name_key")?json.get("name_key").getAsString():id,json.has("slot_size")?json.get("slot_size").getAsInt():512,rooms);
    }
    private static ArenaDefinition.GridPoint point(JsonObject object,String key,ArenaDefinition.GridPoint fallback){if(!object.has(key))return fallback;JsonArray value=object.getAsJsonArray(key);return new ArenaDefinition.GridPoint(value.get(0).getAsInt(),value.get(1).getAsInt());}
}
