package com.wan.gmmod.content.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.wan.gmmod.GuimiMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务注册表：从 {@code data/guimi_mod/tasks/*.json} 加载全部任务定义。
 * <p>
 * 每次世界加载（服务端 {@code AddReloadListenerEvent} 触发）时重新加载，
 * 保证 JSON 数据驱动热更新。客户端仅在打开任务书时按需同步（见数据包），
 * 本表服务端权威。
 */
public final class TaskRegistry {

    private static final Map<ResourceLocation, Task> TASKS = new LinkedHashMap<>();

    private TaskRegistry() {}

    /** 按 ID 查询任务，不存在返回 null。 */
    public static Task get(ResourceLocation id) {
        return TASKS.get(id);
    }

    /** 全部任务（按加载顺序）。 */
    public static List<Task> all() {
        return List.copyOf(TASKS.values());
    }

    /** 当前是否已加载任务数据（用于客户端判断服务端尚未同步的情况）。 */
    public static boolean loaded() {
        return !TASKS.isEmpty();
    }

    /**
     * 从资源管理器加载全部任务 JSON。幂等：加载前清空旧数据。
     */
    public static void load(ResourceManager manager) {
        TASKS.clear();
        Map<ResourceLocation, Resource> resources = manager.listResources(
                "tasks", s -> s.getPath().endsWith(".json"));
        resources.forEach((path, resource) -> {
            try (Reader reader = resource.openAsReader()) {
                BufferedReader buffered = reader instanceof BufferedReader br ? br : new BufferedReader(reader);
                JsonElement json = JsonParser.parseReader(buffered);
                List<Task> tasks = Task.CODEC.listOf().parse(JsonOps.INSTANCE, json).resultOrPartial(
                        err -> GuimiMod.LOGGER.warn("任务数据 {} 解析失败: {}", path, err)).orElse(List.of());
                for (Task task : tasks) {
                    TASKS.put(task.id(), task);
                }
            } catch (IOException e) {
                GuimiMod.LOGGER.warn("读取任务数据 {} 失败: {}", path, e.toString());
            }
        });
        GuimiMod.LOGGER.info("任务注册表已加载 {} 条任务", TASKS.size());
    }

    /** 便捷：按字符串 ID 解析并查询。 */
    public static Task get(String id) {
        return TASKS.get(ResourceLocation.tryParse(id));
    }

    /** 全部任务（不可变，供调试）。 */
    public static Map<ResourceLocation, Task> asMap() {
        return Collections.unmodifiableMap(TASKS);
    }
}
