package com.wan.gmmod.content.sequences;

import com.wan.gmmod.GuimiMod;
import net.minecraft.resources.ResourceLocation;

/**
 * 诡秘之主 22 条途径与全部序列（0~9）的定义与批量注册。
 * <p>
 * 每条途径以 {@link Pathway} 枚举占位，携带：
 * <ul>
 *     <li>{@code key}：英文标识，用于生成序列 ID（如 {@code fool_9}）；</li>
 *     <li>{@code displayName}：途径中文名（如「愚者」）；</li>
 *     <li>{@code sequenceNames}：序列 9 → 0 的名称数组（共 10 项）。</li>
 * </ul>
 * 通过 {@link #init()} 一次性把 22×10=220 条序列批量注册到 {@link SequenceRegistry}。
 * <p>
 * 约定：序列号（{@code level}）越小越强，序列 9 为入门，序列 0 为途径「真神」。
 * 晋升前置等级 {@code requiredLevel}：序列 9 为 0（起始），其余为 {@code level + 1}。
 */
public class Sequences {

    /**
     * 22 条途径枚举。数组按 <b>序列 9 → 序列 0</b> 顺序填写（共 10 项）。
     */
    public enum Pathway {
        FOOL("fool", "愚者",
                "占卜家", "小丑", "魔术师", "无面人", "秘偶大师",
                "诡法师", "古代学者", "奇迹师", "诡秘侍者", "愚者"),
        ERROR("error", "错误",
                "偷盗者", "诈骗师", "解密学者", "盗火人", "窃梦家",
                "寄生者", "欺瞒导师", "命运木马", "时之虫", "错误"),
        DOOR("door", "门",
                "学徒", "戏法大师", "占星人", "记录官", "旅行家",
                "秘法师", "漫游者", "旅法师", "星之匙", "门"),
        PARAGON("paragon", "完美者",
                "通识者", "考古学家", "鉴定师", "机械专家", "天文学家",
                "炼金术士", "奥秘学者", "知识导师", "启蒙者", "完美者"),
        HANGED_MAN("hanged_man", "倒吊人",
                "秘祈人", "倾听者", "隐修士", "蔷薇主教", "牧羊人",
                "黑骑士", "三首圣堂", "秽语长老", "暗天使", "倒吊人"),
        SUN("sun", "太阳",
                "歌颂者", "祈光人", "太阳神官", "公证人", "光之祭司",
                "无暗者", "正义导师", "逐光者", "纯白天使", "太阳"),
        TYRANT("tyrant", "暴君",
                "水手", "暴怒之民", "航海家", "风眷者", "海洋歌者",
                "灾难主祭", "海王", "天灾", "雷神", "暴君"),
        WHITE_TOWER("white_tower", "白塔",
                "阅读者", "推理学员", "守知者", "博学者", "秘术导师",
                "预言家", "洞悉者", "智天使", "全知之眼", "白塔"),
        VISIONARY("visionary", "空想家",
                "观众", "读心者", "心理医生", "催眠师", "梦境行者",
                "操纵师", "织梦人", "洞察者", "作家", "空想家"),
        DEATH("death", "死神",
                "收尸人", "掘墓人", "通灵者", "死灵导师", "看门人",
                "不死者", "摆渡人", "死亡执政官", "苍白皇帝", "死神"),
        DARKNESS("darkness", "黑暗",
                "不眠者", "午夜诗人", "梦魇", "安魂师", "灵巫",
                "守夜人", "恐惧主教", "隐秘之仆", "厄难骑士", "黑暗"),
        GIANT("giant", "黄昏巨人",
                "战士", "格斗家", "武器大师", "黎明骑士", "守护者",
                "猎魔者", "银骑士", "荣耀者", "神明之手", "黄昏巨人"),
        WAR("war", "红祭司",
                "猎人", "挑衅者", "纵火家", "阴谋家", "收割者",
                "铁血骑士", "战争主教", "天气术士", "征服者", "红祭司"),
        HERMIT("hermit", "隐者",
                "窥秘人", "格斗学者", "巫师", "卷轴教授", "星象师",
                "神秘学家", "预言大师", "贤者", "知识之妖", "隐者"),
        MOON("moon", "月亮",
                "药师", "驯兽师", "吸血鬼", "魔药教授", "深红学者",
                "巫王", "召唤大师", "创生者", "美神", "月亮"),
        MOTHER("mother", "母亲",
                "耕种者", "医师", "丰收祭司", "生物学家", "德鲁伊",
                "古代炼金师", "抬棺人", "荒芜主母", "自然行者", "母亲"),
        ABYSS("abyss", "深渊",
                "罪犯", "冷血者", "连环杀手", "恶魔", "欲望使徒",
                "地狱信使", "呓语者", "鲜血大公", "黑之皇帝", "深渊"),
        CHAINED("chained", "被缚者",
                "囚犯", "疯子", "狼人", "活尸", "怨魂",
                "木偶", "沉默门徒", "古代邪物", "神孽", "被缚者"),
        WITCH("witch", "魔女",
                "刺客", "教唆者", "女巫", "欢愉", "痛苦",
                "绝望", "不老", "灾难", "末日", "魔女"),
        JUSTICE("justice", "审判者",
                "仲裁人", "治安官", "审讯者", "法官", "惩戒骑士",
                "律令法师", "混乱猎手", "平衡者", "秩序之手", "审判者"),
        BLACK_EMPEROR("black_emperor", "黑皇帝",
                "律师", "野蛮人", "贿赂者", "腐化男爵", "混乱导师",
                "堕落伯爵", "狂乱法师", "熵之公爵", "弑序亲王", "黑皇帝"),
        WHEEL("wheel", "命运之轮",
                "怪物", "机器", "幸运儿", "灾祸教士", "赢家",
                "厄运法师", "混乱行者", "先知", "水银之蛇", "命运之轮");

        /** 途径英文标识（生成序列 ID 前缀） */
        private final String key;
        /** 途径中文名 */
        private final String displayName;
        /** 序列 9 → 0 的名称（共 10 项，索引 0 为序列 9，索引 9 为序列 0） */
        private final String[] sequenceNames;

        Pathway(String key, String displayName, String... sequenceNames9to0) {
            this.key = key;
            this.displayName = displayName;
            this.sequenceNames = sequenceNames9to0;
        }

        public String getKey() { return key; }

        public String getDisplayName() { return displayName; }

        /**
         * 获取指定序列号对应的序列名称。
         *
         * @param level 序列号（0~9）
         */
        public String getSequenceName(int level) {
            return sequenceNames[9 - level];
        }

        /** 生成指定序列号的 {@link ResourceLocation} ID，如 {@code guimi_mod:fool_9} */
        public ResourceLocation sequenceId(int level) {
            return GuimiMod.id(key + "_" + level);
        }

        /**
         * 相近途径分组（对应旧日 / 相邻途径）。同组途径的非凡特性会相互聚合。
         * 未出现在任何组中的途径（如空想家、命运之轮）无相邻途径。
         * <p>
         * 静态初始化在全部枚举常量构造完成后执行，引用常量安全。
         */
        private static final java.util.List<java.util.EnumSet<Pathway>> PROXIMATE_GROUPS = java.util.List.of(
                java.util.EnumSet.of(FOOL, DOOR, ERROR),                    // 占卜家、学徒、偷盗者
                java.util.EnumSet.of(HANGED_MAN, TYRANT, SUN, WHITE_TOWER), // 倒吊人、暴君、太阳、白塔
                java.util.EnumSet.of(DARKNESS, DEATH, GIANT),              // 黑夜、死神、巨人
                java.util.EnumSet.of(WITCH, WAR),                          // 魔女、红祭司
                java.util.EnumSet.of(JUSTICE, BLACK_EMPEROR),              // 仲裁人、黑皇帝
                java.util.EnumSet.of(HERMIT, PARAGON),                     // 窥秘人、通识者
                java.util.EnumSet.of(CHAINED, ABYSS),                      // 被缚者、深渊
                java.util.EnumSet.of(MOON, MOTHER)                         // 药师（月亮）、母亲
        );

        /**
         * 判断另一途径是否与本途径相近（含自身）。
         * 相近途径的非凡特性会通过命运调整相互靠近（聚合定律）。
         *
         * @param other 另一途径
         * @return 同途径或同属一个相近组时为 {@code true}
         */
        public boolean isProximate(Pathway other) {
            if (other == null) {
                return false;
            }
            if (other == this) {
                return true;
            }
            for (java.util.EnumSet<Pathway> group : PROXIMATE_GROUPS) {
                if (group.contains(this) && group.contains(other)) {
                    return true;
                }
            }
            return false;
        }

        /** 返回与本途径相近的所有途径（含自身）。 */
        public java.util.Set<Pathway> proximatePathways() {
            java.util.EnumSet<Pathway> result = java.util.EnumSet.of(this);
            for (java.util.EnumSet<Pathway> group : PROXIMATE_GROUPS) {
                if (group.contains(this)) {
                    result.addAll(group);
                }
            }
            return result;
        }
    }

    /** 每条途径的序列数（0~9） */
    public static final int MAX_LEVEL = 9;

    /**
     * 按途径英文标识查找 {@link Pathway}。
     *
     * @param key 途径标识（如 {@code "fool"}），空串或未知返回 {@code null}
     */
    public static Pathway fromKey(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        for (Pathway pathway : Pathway.values()) {
            if (pathway.key.equals(key)) {
                return pathway;
            }
        }
        return null;
    }

    /**
     * 批量注册所有途径的全部序列到 {@link SequenceRegistry}。
     */
    public static void init() {
        for (Pathway pathway : Pathway.values()) {
            registerPathway(pathway);
        }
        GuimiMod.LOGGER.info("已批量注册 {} 条途径 × {} 序列", Pathway.values().length, MAX_LEVEL + 1);
    }

    /**
     * 注册单条途径的序列 9 → 0。
     */
    private static void registerPathway(Pathway pathway) {
        for (int level = MAX_LEVEL; level >= 0; level--) {
            String name = pathway.getSequenceName(level);
            // 晋升前置等级：序列 9 为起始（0），其余需要先达到上一序列（level + 1）
            int requiredLevel = (level == MAX_LEVEL) ? 0 : level + 1;
            String description = "sequence.guimi_mod." + pathway.getKey() + "." + level + ".intro";
            SequenceRegistry.register(new Sequence(
                    pathway.sequenceId(level), name, level, requiredLevel, pathway, description));
        }
    }
    /** 途径非空即已就职。序列零的等级也是 0，不能再用等级判断凡人。 */
    public static boolean employed(String pathwayKey) {
        return pathwayKey != null && !pathwayKey.isEmpty();
    }

}
