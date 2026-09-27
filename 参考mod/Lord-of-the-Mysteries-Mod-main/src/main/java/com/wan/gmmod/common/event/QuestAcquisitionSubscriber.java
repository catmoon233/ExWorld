package com.wan.gmmod.common.event;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.NunEntity;
import com.wan.gmmod.content.entities.PriestEntity;
import com.wan.gmmod.content.quest.QuestManager;
import com.wan.gmmod.content.quest.Task;
import com.wan.gmmod.content.quest.TaskRegistry;
import com.wan.gmmod.content.quest.TaskType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支线任务获取方式。
 * <p>
 * 除了任务书中手动接取外，支线任务还可以通过以下途径获得：
 * <ul>
 *   <li><b>村民 / NPC 托付</b>：右键村民、神父或修女，对方会托付给你一个可接取的支线任务；</li>
 *   <li><b>偶遇特殊事件</b>：旅途中随机触发神秘事件，直接获得一个支线任务。</li>
 * </ul>
 * 接取统一走 {@link QuestManager#accept}（服务端权威、校验条件并同步）。
 */
// Quests are disabled.
public class QuestAcquisitionSubscriber {

    /** 已经托付过任务的实体（每个实体只会托付一次支线任务）。 */
    private static final Set<UUID> OFFERED_ENTITIES = new HashSet<>();
    /** 玩家上一次触发偶遇事件的游戏时刻。 */
    private static final Map<UUID, Long> LAST_EVENT_TICK = new ConcurrentHashMap<>();

    /** 偶遇事件：距上次触发至少间隔（刻）。 */
    private static final int EVENT_MIN_INTERVAL = 20 * 60 * 5; // 5 分钟
    /** 偶遇事件：每次检测窗口内的触发概率。 */
    private static final float EVENT_CHANCE = 0.05F;
    /** 偶遇事件的检测频率（刻）。 */
    private static final int EVENT_CHECK_INTERVAL = 60; // 每 3 秒检测一次

    /** 玩家当前还可接取的支线任务（未接取、未完成、且满足条件）。 */
    private static List<Task> eligibleSideQuests(ServerPlayer player) {
        var data = QuestManager.data(player);
        return TaskRegistry.all().stream()
                .filter(t -> !t.story() && t.type() != TaskType.MAIN)
                .filter(t -> !data.hasActive(t.id().toString()) && !data.hasCompleted(t.id().toString()))
                .filter(t -> QuestManager.conditionsMet(player, t))
                .toList();
    }

    // ===== 村民 / NPC 托付 =====

    /** 右键村民、神父或修女：托付一份随机支线任务（每位实体仅一次）。 */
    @SubscribeEvent
    public static void onNpcInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        if (!(event.getTarget() instanceof Villager)
                && !(event.getTarget() instanceof PriestEntity)
                && !(event.getTarget() instanceof NunEntity)) {
            return;
        }
        net.minecraft.world.entity.Entity npc = event.getTarget();
        if (!OFFERED_ENTITIES.add(npc.getUUID())) {
            sp.sendSystemMessage(Component.translatable("message.guimi_mod.quest.npc_nothing"));
            return;
        }
        List<Task> pool = eligibleSideQuests(sp);
        if (pool.isEmpty()) {
            sp.sendSystemMessage(Component.translatable("message.guimi_mod.quest.npc_nothing"));
            return;
        }
        Task task = pool.get(sp.getRandom().nextInt(pool.size()));
        QuestManager.accept(sp, task);
        sp.sendSystemMessage(Component.translatable("message.guimi_mod.quest.npc_offer",
                Component.translatable(task.name())));
    }

    // ===== 偶遇特殊事件 =====

    /** 周期性偶遇：在野外小概率触发神秘事件，直接获得一个支线任务。 */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        if (sp.tickCount % EVENT_CHECK_INTERVAL != 0) {
            return;
        }
        long now = sp.level().getGameTime();
        if (now < lastEventTicks(sp) + EVENT_MIN_INTERVAL) {
            return;
        }
        if (lastEventTicks(sp) != 0L && sp.getRandom().nextFloat() >= EVENT_CHANCE) {
            return;
        }
        List<Task> pool = eligibleSideQuests(sp);
        if (pool.isEmpty()) {
            return;
        }
        LAST_EVENT_TICK.put(sp.getUUID(), now);
        Task task = pool.get(sp.getRandom().nextInt(pool.size()));
        QuestManager.accept(sp, task);
        sp.sendSystemMessage(Component.translatable("message.guimi_mod.quest.event_encounter",
                Component.translatable(task.name())));
    }

    private static long lastEventTicks(ServerPlayer sp) {
        return LAST_EVENT_TICK.getOrDefault(sp.getUUID(), 0L);
    }
}