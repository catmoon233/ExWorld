package com.wan.gmmod.content.quest;

/**
 * 任务类型。
 * <p>
 * 决定任务在任务书 GUI 中的配色与行为：主线（金）/ 仪式（蓝）/ 收集（绿）/
 * 调查（青）/ 狩猎（橙）/ 阵营（红）/ 日记（紫）。{@code MAIN} 为剧情主线，
 * 不可放弃；其余类型为支线，可在任务书中放弃。
 */
public enum TaskType {
    MAIN("main", 0xFFD700),
    RITUAL("ritual", 0x5AA9E6),
    COLLECTION("collection", 0x55CC55),
    INVESTIGATION("investigation", 0x55CCCC),
    HUNTING("hunting", 0xFFAA55),
    FACTION("faction", 0xFF5555),
    DIARY("diary", 0xCC55FF);

    private final String key;
    private final int color;

    TaskType(String key, int color) {
        this.key = key;
        this.color = color;
    }

    public String getKey() {
        return key;
    }

    public int getColor() {
        return color;
    }

    public static TaskType fromKey(String key) {
        for (TaskType type : values()) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        return COLLECTION;
    }
}
