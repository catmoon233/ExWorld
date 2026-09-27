package com.wan.gmmod.content.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 任务定义（JSON 数据驱动）。每一条任务对应一个任务实例。
 * <p>
 * {@code id} 为任务唯一标识（如 {@code guimi_mod:first_potion}）；
 * {@code name}/{@code description} 为翻译键；
 * {@code story} 为 true 表示剧情任务（主线），不自动接取，需任务书手动接取，且不可放弃。
 */
public record Task(ResourceLocation id, String name, String description,
                   TaskType type, List<TaskObjective> objectives,
                   List<TaskReward> rewards, TaskConditions conditions, boolean story) {

    public static final Codec<Task> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(Task::id),
            Codec.STRING.fieldOf("name").forGetter(Task::name),
            Codec.STRING.fieldOf("description").forGetter(Task::description),
            Codec.STRING.xmap(TaskType::fromKey, TaskType::getKey).fieldOf("type").forGetter(Task::type),
            TaskObjective.CODEC.listOf().optionalFieldOf("objectives", List.of()).forGetter(Task::objectives),
            TaskReward.CODEC.listOf().optionalFieldOf("rewards", List.of()).forGetter(Task::rewards),
            TaskConditions.CODEC.optionalFieldOf("conditions", TaskConditions.none()).forGetter(Task::conditions),
            Codec.BOOL.optionalFieldOf("story", false).forGetter(Task::story)
    ).apply(inst, Task::new));

    /** 该任务是否可放弃（非主线）。 */
    public boolean canAbandon() {
        return type != TaskType.MAIN;
    }
}
