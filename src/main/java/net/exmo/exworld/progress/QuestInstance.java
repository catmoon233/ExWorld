package net.exmo.exworld.progress;

import java.time.Instant;
import java.util.*;
import net.minecraft.resources.ResourceLocation;

/** Per-player mutable state; definition data deliberately remains outside the save. */
public final class QuestInstance {
    private final ResourceLocation questId;
    private QuestStatus status;
    private String nodeId;
    private final Map<Integer, Integer> progress = new LinkedHashMap<>();
    private final Set<String> selectedBranches = new LinkedHashSet<>();
    private final Instant grantedAt;

    public QuestInstance(ResourceLocation questId, String nodeId, QuestStatus status, Instant grantedAt) {
        this.questId = questId; this.nodeId = nodeId; this.status = status; this.grantedAt = grantedAt;
    }
    public ResourceLocation questId() { return questId; } public QuestStatus status() { return status; }
    public void status(QuestStatus value) { status = value; } public String nodeId() { return nodeId; }
    public void nodeId(String value) { nodeId = value; progress.clear(); } public Map<Integer, Integer> progress() { return progress; }
    public Set<String> selectedBranches() { return selectedBranches; } public Instant grantedAt() { return grantedAt; }
}
