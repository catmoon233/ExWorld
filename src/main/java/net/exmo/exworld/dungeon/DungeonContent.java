package net.exmo.exworld.dungeon;

import net.exmo.exworld.dungeon.model.DungeonDefinition;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class DungeonContent {
    private static final Map<String, DungeonDefinition> DEFINITIONS = new ConcurrentHashMap<>();
    private DungeonContent() {}
    public static void replace(Collection<DungeonDefinition> definitions) {
        Map<String,DungeonDefinition> next=new LinkedHashMap<>();definitions.forEach(value->next.put(value.id(),value));
        DEFINITIONS.clear();DEFINITIONS.putAll(next);
    }
    public static Optional<DungeonDefinition> definition(String id){return Optional.ofNullable(DEFINITIONS.get(id));}
    public static Collection<DungeonDefinition> definitions(){return List.copyOf(DEFINITIONS.values());}
}
