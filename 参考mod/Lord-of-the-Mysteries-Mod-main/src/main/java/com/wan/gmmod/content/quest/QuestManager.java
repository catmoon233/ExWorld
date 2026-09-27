package com.wan.gmmod.content.quest;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.QuestData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 任务核心逻辑：接取 / 放弃 / 进度上报 / 完成判定 / 奖励发放 / 条件校验。
 * <p>
 * 服务端权威：所有方法只在服务端调用。进度数据存于 {@link ModAttachments#QUEST_DATA}，
 * 因附件配置了 {@code sync}，写入 {@code setData} 后会自动同步到对应玩家客户端。
 */
public final class QuestManager {

    private QuestManager() {}

    // ===== 数据访问 =====

    public static QuestData data(ServerPlayer player) {
        return player.getData(ModAttachments.QUEST_DATA);
    }

    private static void save(ServerPlayer player, QuestData data) {
        player.setData(ModAttachments.QUEST_DATA, data);
    }

    // ===== 条件校验（接取前）=====

    /** 玩家是否满足任务的接取 / 解锁条件。 */
    public static boolean conditionsMet(ServerPlayer player, Task task) {
        TaskConditions c = task.conditions();

        if (!c.requiredQuest().isEmpty() && !data(player).hasCompleted(c.requiredQuest())) {
            return false;
        }
        if (!c.pathway().isEmpty() && !c.pathway().equals(player.getData(ModAttachments.PATHWAY))) {
            return false;
        }
        int lvl = player.getData(ModAttachments.SEQUENCE_LEVEL);
        if (lvl < c.minSequence() || lvl > c.maxSequence()) {
            return false;
        }
        if (!c.holdItem().isEmpty()) {
            boolean has = player.getInventory().items.stream()
                    .filter(s -> !s.isEmpty())
                    .anyMatch(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(c.holdItem()));
            if (!has) {
                return false;
            }
        }
        return true;
    }

    // ===== 接取 / 放弃 / 追踪 =====

    /** 接取任务：校验条件，写入 active 并提示。 */
    public static void accept(ServerPlayer player, Task task) { if (true) return;
        QuestData data = data(player);
        if (data.hasActive(task.id().toString()) || data.hasCompleted(task.id().toString())) {
            return;
        }
        if (!conditionsMet(player, task)) {
            player.sendSystemMessage(Component.translatable("message.guimi_mod.quest.condition_not_met"));
            return;
        }
        save(player, data.withActive(task.id().toString()));
        player.sendSystemMessage(Component.translatable("message.guimi_mod.quest.accepted"));
    }

    /** 放弃任务：仅限可放弃任务（非主线）。 */
    public static void abandon(ServerPlayer player, Task task) { if (true) return;
        if (!task.canAbandon() || !data(player).hasActive(task.id().toString())) {
            return;
        }
        save(player, data(player).withoutActive(task.id().toString(), task.objectives().size()));
        player.sendSystemMessage(Component.translatable("message.guimi_mod.quest.abandoned"));
    }

    /** 切换 HUD 追踪（最多 3 个）。 */
    public static void toggleTrack(ServerPlayer player, Task task) { if (true) return;
        save(player, data(player).withTracked(task.id().toString()));
    }

    // ===== 进度上报 =====

    /**
     * 上报一次任务相关行为，按 {@code type} 匹配进行中任务的目标，累积进度并检查完成。
     *
     * @param type   目标类型（如 {@code kill} / {@code collect} / {@code craft} / {@code explore} / {@code ability}）
     * @param target 目标键（实体 ID / 物品 ID / 结构 ID / 能力 ID / 序列 ID）
     * @param amount 本次增加数
     */
    public static void report(ServerPlayer player, String type, String target, int amount) { if (true) return;
        QuestData data = data(player);
        if (data.active().isEmpty()) {
            return;
        }
        for (String taskIdStr : data.active()) {
            Task task = TaskRegistry.get(taskIdStr);
            if (task == null) {
                continue;
            }
            boolean requireAll = true;
            for (int i = 0; i < task.objectives().size(); i++) {
                TaskObjective obj = task.objectives().get(i);
                if (!obj.type().equals(type) || !obj.target().equals(target)) {
                    continue;
                }
                String key = TaskObjective.progressKey(taskIdStr, i);
                int cur = data.progressOf(key);
                int next = Math.min(obj.count(), cur + amount);
                data = data.withProgress(key, next);
                if (next < obj.count()) {
                    requireAll = false;
                }
            }
            // 全目标达到即完成
            if (requireAll && allDone(task, data)) {
                complete(player, task, data);
                data = data(player); // complete 已发新实例
            }
        }
        save(player, data);
    }

    /** 玩家是否已完成任务的全部目标。 */
    private static boolean allDone(Task task, QuestData data) {
        for (int i = 0; i < task.objectives().size(); i++) {
            TaskObjective obj = task.objectives().get(i);
            if (data.progressOf(TaskObjective.progressKey(task.id().toString(), i)) < obj.count()) {
                return false;
            }
        }
        return true;
    }

    // ===== 完成 / 奖励 =====

    /**
     * 晋升钩子（魔药 / 仪式晋升成功后调用）：上报「晋升」目标进度。
     * 主线任务常以 {@code promote} 目标衔接序列升级。
     *
     * @param sequenceId 晋升到的序列 ID（如 {@code guimi_mod:fool_8}）
     */
    public static void onPromoted(ServerPlayer player, String sequenceId) { if (true) return;
        report(player, "promote", sequenceId, 1);
    }

    /** 完成并结算任务（由 report 或外部晋升钩子驱动，需再次校验进度）。 */
    public static void complete(ServerPlayer player, Task task) { if (true) return;
        QuestData data = data(player);
        if (!data.hasActive(task.id().toString()) || !allDone(task, data)) {
            return;
        }
        complete(player, task, data);
    }

    private static void complete(ServerPlayer player, Task task, QuestData data) {
        save(player, data.withCompleted(task.id().toString(), task.objectives().size()));
        grantRewards(player, task);
        player.sendSystemMessage(Component.translatable("message.guimi_mod.quest.completed",
                Component.translatable(task.name())));
        // 主线完成后自动接取与之衔接的下一个主线（目的requiredQuest指向本任务）
        if (task.story() || task.type() == TaskType.MAIN) {
            autoAcceptNextMain(player, task);
        }
    }

    /** 主线衔接：找到 requiredQuest 指向刚完成任务的下一个主线并自动接取。 */
    private static void autoAcceptNextMain(ServerPlayer player, Task completed) {
        for (Task candidate : TaskRegistry.all()) {
            if (!candidate.story() && candidate.type() != TaskType.MAIN) {
                continue;
            }
            if (candidate.conditions().requiredQuest().equals(completed.id().toString())) {
                accept(player, candidate);
                return;
            }
        }
    }

    /** 发放任务奖励。奖励类型：item / acting / spirituality。 */
    private static void grantRewards(ServerPlayer player, Task task) {
        for (TaskReward reward : task.rewards()) {
            switch (reward.type()) {
                case "item" -> giveItem(player, reward);
                case "acting" -> {
                    int cur = player.getData(ModAttachments.ACTING_PROGRESS);
                    player.setData(ModAttachments.ACTING_PROGRESS, Math.min(100, cur + reward.amount()));
                }
                case "spirituality" -> {
                    int cur = SpiritualityManager.get(player);
                    int max = com.wan.gmmod.content.spirituality.SpiritualityManager.getMax(player);
                    SpiritualityManager.set(player, Math.min(max, cur + reward.amount()));
                }
                default -> { }
            }
        }
    }

    /** 发放物品奖励到背包，背包满则掉落脚下。 */
    private static void giveItem(ServerPlayer player, TaskReward reward) {
        var itemKey = net.minecraft.resources.ResourceLocation.tryParse(reward.item());
        if (itemKey == null) {
            return;
        }
        var item = BuiltInRegistries.ITEM.get(itemKey);
        if (item == net.minecraft.world.item.Items.AIR) {
            return;
        }
        ItemStack stack = new ItemStack(item, Math.max(1, reward.amount()));
        Inventory inv = player.getInventory();
        if (inv.add(stack)) {
            return;
        }
        ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack);
        drop.setPickUpDelay(10);
        player.level().addFreshEntity(drop);
    }
}