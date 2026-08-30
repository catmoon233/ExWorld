package net.exmo.exworld.dungeon.model;

import java.util.*;

public record DungeonDefinition(String id, String nameKey, int slotSize, List<DungeonRoomDefinition> rooms) {
    public DungeonDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Dungeon id is required");
        slotSize = 512; rooms = List.copyOf(rooms);
        if (rooms.isEmpty()) throw new IllegalArgumentException("Dungeon needs at least one room");
    }
    public Optional<DungeonRoomDefinition> room(String roomId) { return rooms.stream().filter(room -> room.id().equals(roomId)).findFirst(); }
}
