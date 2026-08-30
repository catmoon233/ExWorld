package net.exmo.exworld.dungeon.model;

import java.util.List;

public record DungeonEnemyDefinition(String id, String entityType, String name,
                                     float maxHealth, float maxMana, double initiative,
                                     int movementPoints, int initialHandSize, List<String> deck) {
    public DungeonEnemyDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Enemy id is required");
        if (entityType == null || entityType.isBlank()) throw new IllegalArgumentException("Enemy entity type is required");
        maxHealth = Math.max(1, maxHealth); maxMana = Math.max(0, maxMana);
        movementPoints = Math.max(0, movementPoints); initialHandSize = Math.max(0, initialHandSize);
        deck = List.copyOf(deck);
    }
}
