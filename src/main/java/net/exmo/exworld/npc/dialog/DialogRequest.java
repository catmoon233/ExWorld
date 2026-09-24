package net.exmo.exworld.npc.dialog;

import java.util.List;

public record DialogRequest(String npcName, String nodeId, String relations, String playerName, List<String> recent) {
    public DialogRequest {
        npcName = npcName == null ? "" : npcName;
        nodeId = nodeId == null ? "" : nodeId;
        relations = relations == null ? "" : relations;
        playerName = playerName == null ? "" : playerName;
        recent = recent == null ? List.of() : List.copyOf(recent);
    }
}
