package com.wan.gmmod.content.sequences;

import net.minecraft.resources.ResourceLocation;

/**
 * 单条诡秘序列的数据结构。
 * <p>
 * 每条序列包含：唯一 {@link #id ID}、{@link #name 名称}、
 * {@link #level 等级}（即序列号 0~9，数字越小越强）、
 * {@link #requiredLevel 晋升前置等级}、所属 {@link #pathway 途径} 与 {@link #description 描述}。
 */
public class Sequence {
    private final ResourceLocation id;
    private final String name;
    private final int level;
    private final int requiredLevel;
    private final Sequences.Pathway pathway;
    private final String description;

    public Sequence(ResourceLocation id, String name, int level, int requiredLevel,
                    Sequences.Pathway pathway, String description) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.requiredLevel = requiredLevel;
        this.pathway = pathway;
        this.description = description;
    }

    public ResourceLocation getId() { return id; }
    public String getName() { return name; }
    public int getLevel() { return level; }
    public int getRequiredLevel() { return requiredLevel; }
    public Sequences.Pathway getPathway() { return pathway; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return "Sequence{" + id + ", " + name + ", 序列" + level + ", " + pathway + "}";
    }
}
