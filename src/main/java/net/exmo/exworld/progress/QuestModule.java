package net.exmo.exworld.progress;

import java.time.Instant;
import java.util.*;
import net.exmo.exworld.battle.BattleSystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Deep player-story module. Callers can only grant/administer tasks, provide trusted observations, or request a view;
 * graph traversal, audit history, branch locking and reward idempotency stay local here.
 */
public final class QuestModule {
    private final PlayerResourceVault vault;
    private RewardDistributor rewards;
    public QuestModule(PlayerResourceVault vault) { this.vault = vault; }
    void rewards(RewardDistributor value) { rewards = value; }
    public boolean grant(ServerPlayer player, ResourceLocation questId, String reason) {
        QuestDefinition definition = QuestContent.definition(questId).orElse(null); if (definition == null) return false;
        PlayerProgress state = vault.state(player.getServer(), player.getUUID());
        if (definition.kind() == QuestKind.MAIN && state.quests.values().stream().anyMatch(value -> value.status() == QuestStatus.ACTIVE && QuestContent.definition(value.questId()).map(QuestDefinition::kind).orElse(null) == QuestKind.MAIN)) return false;
        if (state.quests.containsKey(questId)) return false;
        state.quests.put(questId, new QuestInstance(questId, definition.entry(), QuestStatus.ACTIVE, Instant.now())); log(player, questId, "granted", reason); vault.saved(player.getServer()).changed(); return true;
    }
    public boolean revoke(ServerPlayer player, ResourceLocation id, String reason) { return remove(player, id, "revoked", reason); }
    public boolean fail(ServerPlayer player, ResourceLocation id, String reason) { return status(player, id, QuestStatus.FAILED, "failed", reason); }
    public boolean reset(ServerPlayer player, ResourceLocation id, String reason) {
        QuestDefinition definition = QuestContent.definition(id).orElse(null); QuestInstance instance = state(player).quests.get(id); if (definition == null || instance == null) return false;
        instance.status(QuestStatus.ACTIVE); instance.nodeId(definition.entry()); instance.selectedBranches().clear(); log(player, id, "reset", reason); vault.saved(player.getServer()).changed(); return true;
    }
    public boolean advance(ServerPlayer player, ResourceLocation id, String requestedNext, String reason) {
        QuestInstance instance = state(player).quests.get(id); QuestDefinition definition = QuestContent.definition(id).orElse(null); if (instance == null || definition == null || instance.status() != QuestStatus.ACTIVE) return false;
        QuestNode node = definition.node(instance.nodeId()); if (node == null) { instance.status(QuestStatus.CONTENT_MISSING); log(player, id, "content_missing", instance.nodeId()); vault.saved(player.getServer()).changed(); return false; }
        if (!node.objectives().isEmpty() && !complete(node, instance)) return false;
        if (node.next().isEmpty()) { finish(player, definition, instance, node, reason); return true; }
        String target = requestedNext == null || requestedNext.isBlank() ? (node.next().size() == 1 ? node.next().getFirst() : "") : requestedNext;
        if (!node.next().contains(target)) return false;
        if (rewards != null) rewards.deliver(player, "quest:" + id + ":" + node.id(), node.titleKey(), node.rewards());
        if (node.next().size() > 1) { instance.selectedBranches().add(node.id() + "->" + target); log(player, id, "branch", target); }
        instance.nodeId(target); log(player, id, "advanced", target + ":" + reason); vault.saved(player.getServer()).changed(); return true;
    }
    public boolean chooseBranch(ServerPlayer player, ResourceLocation id, String next) { return advance(player, id, next, "player_choice"); }
    public void observe(ServerPlayer player, QuestEvent event) {
        PlayerProgress state = state(player); boolean changed = false;
        for (QuestInstance instance : List.copyOf(state.quests.values())) {
            if (instance.status() != QuestStatus.ACTIVE) continue; QuestDefinition definition = QuestContent.definition(instance.questId()).orElse(null);
            if (definition == null || definition.node(instance.nodeId()) == null) { instance.status(QuestStatus.CONTENT_MISSING); log(player, instance.questId(), "content_missing", instance.nodeId()); changed = true; continue; }
            QuestNode node = definition.node(instance.nodeId()); boolean nodeChanged = false;
            for (int index = 0; index < node.objectives().size(); index++) { QuestObjective objective = node.objectives().get(index); if (!matches(player, objective, event)) continue; int current = instance.progress().getOrDefault(index, 0); int next = Math.min(objective.amount(), current + Math.max(1, event.amount())); if (next != current) { instance.progress().put(index, next); log(player, instance.questId(), "progress", node.id() + ":" + index + "=" + next + "/" + objective.amount()); changed = true; nodeChanged = true; } }
            if (nodeChanged && complete(node, instance)) { log(player, instance.questId(), "node_completed", node.id()); if (node.next().size() <= 1) advance(player, instance.questId(), node.next().isEmpty() ? "" : node.next().getFirst(), "objectives"); else vault.saved(player.getServer()).changed(); }
        }
        if (changed) vault.saved(player.getServer()).changed();
    }
    /** Inventory objectives have no explicit vanilla delta, so a cheap periodic truth sample is used. */
    public void checkInventory(ServerPlayer player) {
        for (QuestInstance instance : state(player).quests.values()) { if (instance.status() != QuestStatus.ACTIVE) continue; QuestDefinition definition = QuestContent.definition(instance.questId()).orElse(null); if (definition == null) continue; QuestNode node = definition.node(instance.nodeId()); if (node == null) continue;
            for (int index = 0; index < node.objectives().size(); index++) { QuestObjective objective = node.objectives().get(index); if (objective.type() != QuestObjective.Type.INVENTORY) continue; int count = net.exmo.exworld.inventory.PlayerBackpack.of(player).count(objective.target()); int current = Math.min(objective.amount(), count); if (instance.progress().getOrDefault(index, 0) != current) { instance.progress().put(index, current); log(player, instance.questId(), "progress", node.id() + ":" + index + "=" + current + "/" + objective.amount()); vault.saved(player.getServer()).changed(); } }
            boolean hasInventory = node.objectives().stream().anyMatch(objective -> objective.type() == QuestObjective.Type.INVENTORY);
            if (hasInventory && complete(node, instance) && node.next().size() <= 1) advance(player, instance.questId(), node.next().isEmpty() ? "" : node.next().getFirst(), "inventory");
        }
    }
    public void validateContent(ServerPlayer player) { for (QuestInstance instance : state(player).quests.values()) if (instance.status() == QuestStatus.ACTIVE && QuestContent.definition(instance.questId()).map(value -> value.node(instance.nodeId()) == null).orElse(true)) { instance.status(QuestStatus.CONTENT_MISSING); log(player, instance.questId(), "content_missing", instance.nodeId()); vault.saved(player.getServer()).changed(); } }
    public QuestSnapshot quest(ServerPlayer player, ResourceLocation questId) { return snapshot(player).stream().filter(value -> value.id().equals(questId)).findFirst().orElse(null); }
    public List<QuestSnapshot> snapshot(ServerPlayer player) {
        List<QuestSnapshot> values = new ArrayList<>(); for (QuestInstance instance : state(player).quests.values()) { QuestDefinition definition = QuestContent.definition(instance.questId()).orElse(null); if (definition == null) { values.add(new QuestSnapshot(instance.questId(), QuestKind.SIDE, QuestStatus.CONTENT_MISSING, instance.questId().toString(), "", instance.nodeId(), "", "", List.of(), List.of(), null)); continue; } QuestNode node = definition.node(instance.nodeId()); List<QuestSnapshot.Objective> objectives = new ArrayList<>(); ResourceLocation nav = null; if (node != null) for (int i=0;i<node.objectives().size();i++) { QuestObjective objective=node.objectives().get(i); objectives.add(new QuestSnapshot.Objective(objective.type(), objective.target().toString(), instance.progress().getOrDefault(i,0), objective.amount(), objective.dimension(), objective.x(),objective.y(),objective.z())); if (nav == null && objective.type()==QuestObjective.Type.LOCATION) nav=objective.target(); } values.add(new QuestSnapshot(instance.questId(),definition.kind(),instance.status(),definition.titleKey(),definition.descriptionKey(),instance.nodeId(),node==null?"":node.titleKey(),node==null?"":node.descriptionKey(),objectives,node==null?List.of():node.next(),nav)); }
        return values;
    }
    public List<MailboxMessage> mailbox(ServerPlayer player) { return List.copyOf(state(player).mailbox); }
    public List<QuestTimelineEntry> timeline(ServerPlayer player) { return List.copyOf(state(player).timeline); }
    public void pin(ServerPlayer player, ResourceLocation quest) { if (state(player).quests.containsKey(quest)) { state(player).pinnedQuest = quest; vault.saved(player.getServer()).changed(); } }
    public ResourceLocation pinned(ServerPlayer player) { return state(player).pinnedQuest; }
    private void finish(ServerPlayer player, QuestDefinition definition, QuestInstance instance, QuestNode node, String reason) { if (rewards != null) rewards.deliver(player,"quest:" + definition.id() + ":" + node.id(),node.titleKey(),node.rewards()); instance.status(QuestStatus.COMPLETED); log(player,definition.id(),"completed",reason); vault.saved(player.getServer()).changed(); }
    private boolean complete(QuestNode node, QuestInstance instance) { for (int i=0;i<node.objectives().size();i++) if (instance.progress().getOrDefault(i,0)<node.objectives().get(i).amount()) return false; return true; }
    private boolean matches(ServerPlayer player, QuestObjective target, QuestEvent event) { if (target.type()!=event.type() || !target.target().equals(event.target())) return false; if (!target.tag().isBlank() && !target.tag().equals(event.tag())) return false; if (target.type()==QuestObjective.Type.LOCATION && (!target.dimension().isBlank()&&!target.dimension().equals(event.dimension()) || Math.pow(target.x()-event.x(),2)+Math.pow(target.y()-event.y(),2)+Math.pow(target.z()-event.z(),2)>target.radius()*target.radius())) return false; return true; }
    private PlayerProgress state(ServerPlayer player) { return vault.state(player.getServer(),player.getUUID()); }
    private boolean remove(ServerPlayer player, ResourceLocation id, String type, String reason) { if (state(player).quests.remove(id)==null) return false; log(player,id,type,reason); vault.saved(player.getServer()).changed(); return true; }
    private boolean status(ServerPlayer player, ResourceLocation id, QuestStatus status, String type, String reason) { QuestInstance instance=state(player).quests.get(id); if(instance==null)return false;instance.status(status);log(player,id,type,reason);vault.saved(player.getServer()).changed();return true; }
    private void log(ServerPlayer player, ResourceLocation id, String type, String detail) { state(player).timeline.add(new QuestTimelineEntry(Instant.now(),player.level().getGameTime(),id,type,detail)); }
}
