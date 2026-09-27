package com.wan.gmmod.client.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.data.QuestData;
import com.wan.gmmod.content.quest.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户端任务缓存：保存服务端下发的任务定义，供任务书界面读取。
 * <p>
 * 由 {@code QuestSyncPacket} 填充。任务进度 / 已接取状态直接读玩家附件的
 * {@link QuestData}（服务端已同步），因此本类只需保存 Task 元数据。
 */
public final class QuestClientState {

    private static final Map<String, Task> TASKS = new HashMap<>();

    private QuestClientState() {}

    /** 从 JSON 数组字符串加载任务定义（客户端）。 */
    public static void load(String tasksJson) {
        try {
            JsonElement json = JsonParser.parseString(tasksJson);
            List<Task> parsed = Task.CODEC.listOf().parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(err -> GuimiMod.LOGGER.warn("解析任务同步数据失败: {}", err))
                    .orElse(java.util.Collections.emptyList());
            TASKS.clear();
            for (Task task : parsed) {
                TASKS.put(task.id().toString(), task);
            }
        } catch (Exception e) {
            GuimiMod.LOGGER.warn("加载任务同步数据异常: {}", e.toString());
        }
    }

    public static Task get(String id) {
        return TASKS.get(id);
    }

    public static List<Task> all() {
        return List.copyOf(TASKS.values());
    }

    public static boolean loaded() {
        return !TASKS.isEmpty();
    }

    /** 全部任务键（不可变，供调试）。 */
    public static Map<String, Task> asMap() {
        return Collections.unmodifiableMap(TASKS);
    }
}