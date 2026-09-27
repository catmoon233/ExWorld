package com.wan.gmmod.common.capability;

import com.mojang.serialization.Codec;
import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.data.CauldronBrewingData;
import com.wan.gmmod.common.capability.data.CooldownData;
import com.wan.gmmod.common.capability.data.DisguiseData;
import com.wan.gmmod.common.capability.data.DisguiseUnlocks;
import com.wan.gmmod.common.capability.data.DistortionZoneData;
import com.wan.gmmod.common.capability.data.InterferenceFieldData;
import com.wan.gmmod.common.capability.data.QuestData;
import com.wan.gmmod.common.capability.data.SkillBarData;
import com.wan.gmmod.content.brewing.RecipeKnowledgeData;
import com.wan.gmmod.content.characteristics.CharacteristicsPool;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, GuimiMod.MODID);

    // 提取常量，避免魔法数字
    public static final int MAX_SANITY = 100;
    public static final int MAX_POLLUTION = 100;
    public static final int DEFAULT_SPIRITUALITY = 100;

    // 序列等级：0 = 未就职，9 = 序列9，依次向上
    public static final Supplier<AttachmentType<Integer>> SEQUENCE_LEVEL =
            ATTACHMENT_TYPES.register("sequence_level",
                    () -> AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT).sync(ByteBufCodecs.INT)
                            .copyOnDeath()
                            .build()
            );

    // 灵性值
    public static final Supplier<AttachmentType<Integer>> SPIRITUALITY =
            ATTACHMENT_TYPES.register("spirituality",
                    () -> AttachmentType.builder(() -> 0)// 未就职时灵性为0
                            .serialize(Codec.INT)
                            .sync(ByteBufCodecs.INT)// 修复：添加网络编解码器
                            .build()
            );

    // 理智值（san值，越低越危险，范围建议 0~100）
    public static final Supplier<AttachmentType<Integer>> SANITY =
            ATTACHMENT_TYPES.register("sanity",
                    () -> AttachmentType.builder(() -> MAX_SANITY)
                            .serialize(Codec.INT)
                            .sync(ByteBufCodecs.INT) // 修复
                            .build()
            );

    // 污染值（0~100，越高越危险）
    public static final Supplier<AttachmentType<Integer>> POLLUTION =
            ATTACHMENT_TYPES.register("pollution",
                    () -> AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT)
                            .sync(ByteBufCodecs.INT) // 修复
                            .build()
            );

    // 扮演进度（0~100），仅当前序列有效
    public static final Supplier<AttachmentType<Integer>> ACTING_PROGRESS =
            ATTACHMENT_TYPES.register("acting_progress",
                    () -> AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT)
                            .sync(ByteBufCodecs.INT) // 修复
                            .build()
            );

    public static final Supplier<AttachmentType<String>> ACTING_SEQUENCE_ID =
            ATTACHMENT_TYPES.register("acting_sequence",
                    () -> AttachmentType.builder(() -> "")
                            .serialize(Codec.STRING)
                            .sync(ByteBufCodecs.STRING_UTF8)// 修复：字符串需要指定编码器
                            .build()
            );

    // 途径标识（如 "fool"），空字符串表示尚未选择途径。
    // 供 GameEventSubscriber 按玩家真实途径匹配序列能力。
    public static final Supplier<AttachmentType<String>> PATHWAY =
            ATTACHMENT_TYPES.register("pathway",
                    () -> AttachmentType.builder(() -> "")
                            .serialize(Codec.STRING)
                            .sync(ByteBufCodecs.STRING_UTF8)
                            .copyOnDeath()
                            .build()
            );

    // 占卜冷却结束时的游戏刻（gameTime），服务端用于冷却判定
    public static final Supplier<AttachmentType<Long>> DIVINATION_COOLDOWN_END =
            ATTACHMENT_TYPES.register("divination_cooldown_end",
                    () -> AttachmentType.builder(() -> 0L)
                            .serialize(Codec.LONG)
                            .build()
            );

    // 最近一次受到伤害的游戏刻（gameTime），用于「战斗状态」安全判定
    public static final Supplier<AttachmentType<Long>> LAST_DAMAGE_TICK =
            ATTACHMENT_TYPES.register("last_damage_tick",
                    () -> AttachmentType.builder(() -> 0L)
                            .serialize(Codec.LONG)
                            .build()
            );

    // 灵视开关：开启后玩家获得夜视，并能看见灵体（SpiritBeing）。
    // 需要同步到客户端，供实体渲染器判断是否渲染灵体。
    public static final Supplier<AttachmentType<Boolean>> SPIRIT_VISION =
            ATTACHMENT_TYPES.register("spirit_vision",
                    () -> AttachmentType.builder(() -> false)
                            .serialize(Codec.BOOL)
                            .sync(ByteBufCodecs.BOOL)
                            .build()
            );

    // 灵性枯竭累计刻数：灵性长时间处于干涸状态时累加，
    // 用于分级触发幻听、debuff、失控征兆。灵性恢复后逐步归零。
    public static final Supplier<AttachmentType<Integer>> SPIRITUALITY_DEPLETION_TICKS =
            ATTACHMENT_TYPES.register("spirituality_depletion_ticks",
                    () -> AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT)
                            .build()
            );

    // 冥想修炼累计周期数：每完成一个冥想恢复周期（2 秒）+1，
    // 用于灵性上限的「冥想修炼加成」（见 SpiritualityManager）。持久化且死亡不丢失。
    public static final Supplier<AttachmentType<Integer>> MEDITATION_TRAINING =
            ATTACHMENT_TYPES.register("meditation_training",
                    () -> AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT)
                            .sync(ByteBufCodecs.INT)
                            .copyOnDeath()
                            .build()
            );

    // 是否亲耳听过真正的美人鱼歌声：在野生美人鱼歌声范围内停留足够时间后永久写入。
    // 拥有此标记的玩家使用替代能力时才能获得真 Buff（见 MermaidSongManager）。
    public static final Supplier<AttachmentType<Boolean>> HAS_HEARD_MERMAID_SONG =
            ATTACHMENT_TYPES.register("has_heard_mermaid_song",
                    () -> AttachmentType.builder(() -> false)
                            .serialize(Codec.BOOL)
                            .copyOnDeath()
                            .build()
            );

    // 在真正美人鱼歌声范围内的累计暴露刻数，达到阈值（30 秒）后写入亲历标记。
    public static final Supplier<AttachmentType<Integer>> MERMAID_SONG_EXPOSURE_TICKS =
            ATTACHMENT_TYPES.register("mermaid_song_exposure_ticks",
                    () -> AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT)
                            .build()
            );

    // 当前「美人鱼的歌声」Buff 是否为真：仅服务端读写，不同步、不告知玩家。
    // 真假 Buff 外观完全相同，服用魔药时才悄悄判定。
    public static final Supplier<AttachmentType<Boolean>> MERMAID_SONG_GENUINE =
            ATTACHMENT_TYPES.register("mermaid_song_genuine",
                    () -> AttachmentType.builder(() -> false)
                            .serialize(Codec.BOOL)
                            .build()
            );

    // 技能栏配置：15 个槽位，每槽存一个能力 ID。晋升 / 死亡后保留配置，故 copyOnDeath。
    public static final Supplier<AttachmentType<SkillBarData>> SKILL_BAR =
            ATTACHMENT_TYPES.register("skill_bar",
                    () -> AttachmentType.builder(SkillBarData::empty)
                            .serialize(SkillBarData.CODEC)
                            .sync(SkillBarData.STREAM_CODEC)
                            .copyOnDeath()
                            .build()
            );

    // 技能冷却：能力 ID -> 冷却结束游戏刻。服务端权威，同步供 HUD 显示冷却蒙版 / 倒计时。
    public static final Supplier<AttachmentType<CooldownData>> SKILL_COOLDOWNS =
            ATTACHMENT_TYPES.register("skill_cooldowns",
                    () -> AttachmentType.builder(CooldownData::empty)
                            .serialize(CooldownData.CODEC)
                            .sync(CooldownData.STREAM_CODEC)
                            .build()
            );

    // 伤害转移虚假血量（魔术师被动）：致命伤时写入 30，每秒持续扣除 1 直到扣完，
    // 期间受到的伤害优先扣除虚假血量。同步到客户端供 HUD 显示。
    public static final Supplier<AttachmentType<Integer>> FAKE_HEALTH =
            ATTACHMENT_TYPES.register("fake_health",
                    () -> AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT)
                            .sync(ByteBufCodecs.INT)
                            .build()
            );

    // 当前秘偶的实体 UUID 字符串（秘偶大师），空串表示没有秘偶。仅服务端读写。
    public static final Supplier<AttachmentType<String>> MARIONETTE_UUID =
            ATTACHMENT_TYPES.register("marionette_uuid",
                    () -> AttachmentType.builder(() -> "")
                            .serialize(Codec.STRING)
                            .build()
            );

    // 灵体之线操控中的目标 UUID 字符串，空串表示没有正在操控的目标。仅服务端读写。
    public static final Supplier<AttachmentType<String>> THREAD_TARGET_UUID =
            ATTACHMENT_TYPES.register("thread_target_uuid",
                    () -> AttachmentType.builder(() -> "")
                            .serialize(Codec.STRING)
                            .build()
            );

    // 灵体之线操控完成（秘偶化生效）的游戏刻，0 表示无进行中的操控。仅服务端读写。
    public static final Supplier<AttachmentType<Long>> THREAD_CONTROL_END =
            ATTACHMENT_TYPES.register("thread_control_end",
                    () -> AttachmentType.builder(() -> 0L)
                            .serialize(Codec.LONG)
                            .build()
            );

    // 是否正在以「共享视野」操控秘偶（秘偶大师）。仅服务端会话内有效，不持久化；
    // 客户端状态由 MarionetteViewPacket 单独下发（见 MarionetteControlClientState）。
    public static final Supplier<AttachmentType<Boolean>> MARIONETTE_CONTROLLING =
            ATTACHMENT_TYPES.register("marionette_controlling",
                    () -> AttachmentType.builder(() -> false)
                            .build()
            );

    // 秘偶掌控度（累计掌控刻数，共享视野操控中 3 倍速累积）：随时间推移掌控加深，
    // 期满触发彻底秘偶化。随存档持久化，仅服务端读写。
    public static final Supplier<AttachmentType<Integer>> MARIONETTE_MASTERY =
            ATTACHMENT_TYPES.register("marionette_mastery",
                    () -> AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT)
                            .build()
            );

    // 是否已将当前秘偶「彻底秘偶化」：不再挣扎反噬、免除僵硬受控标记，
    // 可在更远距离躲于幕后操纵其战斗。随存档持久化，仅服务端读写。
    public static final Supplier<AttachmentType<Boolean>> MARIONETTE_THOROUGH =
            ATTACHMENT_TYPES.register("marionette_thorough",
                    () -> AttachmentType.builder(() -> false)
                            .serialize(Codec.BOOL)
                            .build()
            );

    // 骨骼软化「钻洞」匍匐状态：双端各自按相同条件计算，不持久化、不同步。
    public static final Supplier<AttachmentType<Boolean>> BONE_CRAWLING =
            ATTACHMENT_TYPES.register("bone_crawling",
                    () -> AttachmentType.builder(() -> false)
                            .build()
            );

    // 纸牌发射模式（小丑「飞牌」）：false = 精准单点，true = 散射。
    // 同步到客户端供技能栏 HUD 在图标右下角绘制模式小图标。
    public static final Supplier<AttachmentType<Boolean>> CARD_SCATTER_MODE =
            ATTACHMENT_TYPES.register("card_scatter_mode",
                    () -> AttachmentType.builder(() -> false)
                            .serialize(Codec.BOOL)
                            .sync(ByteBufCodecs.BOOL)
                            .copyOnDeath()
                            .build()
            );

    // 当前变形状态（无面人）：NONE / PLAYER / MOB。同步到客户端供 PlayerRenderer 替换模型 / 纹理。
    // 变形不影响碰撞箱与战斗数值，仅改变外观。晋升 / 死亡后保留，故 copyOnDeath。
    public static final Supplier<AttachmentType<DisguiseData>> DISGUISE_STATE =
            ATTACHMENT_TYPES.register("disguise_state",
                    () -> AttachmentType.builder(DisguiseData::none)
                            .serialize(DisguiseData.CODEC)
                            .sync(DisguiseData.STREAM_CODEC)
                            .copyOnDeath()
                            .build()
            );

    // 已解锁的人形怪物外观集合（怪物图鉴）：击杀 / 观察 / 初始赠送写入。
    // 同步到客户端供变形界面列出可选项。永久保留，故 copyOnDeath。
    public static final Supplier<AttachmentType<DisguiseUnlocks>> UNLOCKED_MOB_DISGUISES =
            ATTACHMENT_TYPES.register("unlocked_mob_disguises",
                    () -> AttachmentType.builder(DisguiseUnlocks::empty)
                            .serialize(DisguiseUnlocks.CODEC)
                            .sync(DisguiseUnlocks.STREAM_CODEC)
                            .copyOnDeath()
                            .build()
            );

    // ===== 魔女途径（刺客 / 教唆者 / 女巫 / 欢愉魔女）相关附件 =====

    // 弱点打击（刺客被动）背刺冷却结束游戏刻。仅服务端读写。
    public static final Supplier<AttachmentType<Long>> BACKSTAB_COOLDOWN_END =
            ATTACHMENT_TYPES.register("backstab_cooldown_end",
                    () -> AttachmentType.builder(() -> 0L)
                            .serialize(Codec.LONG)
                            .build()
            );

    // 女巫「隐形」过渡开始游戏刻，0 表示未在过渡中。仅服务端读写。
    public static final Supplier<AttachmentType<Long>> WITCH_INVIS_START =
            ATTACHMENT_TYPES.register("witch_invis_start",
                    () -> AttachmentType.builder(() -> 0L)
                            .build()
            );

    // 女巫「隐形」完全隐身结束游戏刻，0 表示未隐身。攻击 / 受伤会提前清零。仅服务端读写。
    public static final Supplier<AttachmentType<Long>> WITCH_INVIS_END =
            ATTACHMENT_TYPES.register("witch_invis_end",
                    () -> AttachmentType.builder(() -> 0L)
                            .build()
            );

    // 蛛丝蚕茧（欢愉魔女）结束游戏刻，0 表示未结茧。期间无敌 + 回复但无法移动攻击；
    // 火焰伤害会提前打破并造成双倍伤害。仅服务端读写。
    public static final Supplier<AttachmentType<Long>> COCOON_END =
            ATTACHMENT_TYPES.register("cocoon_end",
                    () -> AttachmentType.builder(() -> 0L)
                            .build()
            );

    // 冰霜护甲（欢愉魔女）结束游戏刻，期间吸收 30% 伤害。仅服务端读写。
    public static final Supplier<AttachmentType<Long>> FROST_ARMOR_END =
            ATTACHMENT_TYPES.register("frost_armor_end",
                    () -> AttachmentType.builder(() -> 0L)
                            .build()
            );

    // 镜子替身锚点："维度ID;x;y;z"，空串表示未绑定。绑定时消耗灵性，触发后镜子碎裂（清空）。
    public static final Supplier<AttachmentType<String>> MIRROR_ANCHOR =
            ATTACHMENT_TYPES.register("mirror_anchor",
                    () -> AttachmentType.builder(() -> "")
                            .serialize(Codec.STRING)
                            .copyOnDeath()
                            .build()
            );

    // 误导（教唆者）第一步已选目标的 UUID 字符串，空串表示尚未选择。仅服务端读写。
    public static final Supplier<AttachmentType<String>> MISDIRECT_SOURCE =
            ATTACHMENT_TYPES.register("misdirect_source",
                    () -> AttachmentType.builder(() -> "")
                            .build()
            );

    // 女性形态（女巫「性别转换」）：晋升女巫时写入，永久保留（除非切换途径）。
    // 同步到客户端供渲染层与 Female Gender Mod 兼容层使用。
    public static final Supplier<AttachmentType<Boolean>> FEMALE_FORM =
            ATTACHMENT_TYPES.register("female_form",
                    () -> AttachmentType.builder(() -> false)
                            .serialize(Codec.BOOL)
                            .sync(ByteBufCodecs.BOOL)
                            .copyOnDeath()
                            .build()
            );

    // 非凡特性全局池（世界级）：记录各途径各等级的特性总量，用于守恒计算。
    // 挂在 Level 上，服务端权威、随世界存档持久化，无需同步。
    public static final Supplier<AttachmentType<CharacteristicsPool>> CHARACTERISTICS_POOL =
            ATTACHMENT_TYPES.register("characteristics_pool",
                    () -> AttachmentType.builder(CharacteristicsPool::new)
                            .serialize(CharacteristicsPool.CODEC)
                            .build()
            );

    // 炼药锅状态（世界级）：记录纯水锅坐标与搅拌次数。服务端权威、随存档持久化，无需同步。
    public static final Supplier<AttachmentType<CauldronBrewingData>> CAULDRON_BREWING =
            ATTACHMENT_TYPES.register("cauldron_brewing",
                    () -> AttachmentType.builder(CauldronBrewingData::empty)
                            .serialize(CauldronBrewingData.CODEC)
                            .build()
            );

    // ===== 战争之红途径（猎人 / 挑衅者 / 纵火家 / 阴谋家）相关附件 =====

    // 已掌握情报的生物类型（弱点洞察分析完成后写入，逗号分隔的实体类型 ID）。
    // 挑衅 / 洞察弱点升级版据此判定「了解程度」。永久保留，故 copyOnDeath。
    public static final Supplier<AttachmentType<String>> KNOWN_MOBS =
            ATTACHMENT_TYPES.register("known_mobs",
                    () -> AttachmentType.builder(() -> "")
                            .serialize(Codec.STRING)
                            .copyOnDeath()
                            .build()
            );

    // 直觉预警：背后（或 360°）有威胁靠近时为 true，同步到客户端绘制屏幕边缘微红。
    public static final Supplier<AttachmentType<Boolean>> DANGER_SENSE =
            ATTACHMENT_TYPES.register("danger_sense",
                    () -> AttachmentType.builder(() -> false)
                            .sync(ByteBufCodecs.BOOL)
                            .build()
            );

    // 基础毒药涂抹剩余次数：近战命中时施加中毒并递减。
    public static final Supplier<AttachmentType<Integer>> POISON_BLADE_HITS =
            ATTACHMENT_TYPES.register("poison_blade_hits",
                    () -> AttachmentType.builder(() -> 0)
                            .serialize(Codec.INT)
                            .build()
            );

    // 火焰护甲结束游戏刻：期间近战攻击者受 2 点反伤，冰冻 / 毒气伤害减半。仅服务端读写。
    public static final Supplier<AttachmentType<Long>> FLAME_ARMOR_END =
            ATTACHMENT_TYPES.register("flame_armor_end",
                    () -> AttachmentType.builder(() -> 0L)
                            .build()
            );

    // 火焰武器到期游戏刻：到期后从背包移除临时火焰武器。仅服务端读写。
    public static final Supplier<AttachmentType<Long>> FLAME_WEAPON_END =
            ATTACHMENT_TYPES.register("flame_weapon_end",
                    () -> AttachmentType.builder(() -> 0L)
                            .build()
            );

    // 火焰体质（火焰形态）结束游戏刻：期间免疫物理伤害、冰冻伤害翻倍。仅服务端读写。
    public static final Supplier<AttachmentType<Long>> FIRE_FORM_END =
            ATTACHMENT_TYPES.register("fire_form_end",
                    () -> AttachmentType.builder(() -> 0L)
                            .build()
            );

    // 火焰体质自动触发冷却结束游戏刻。仅服务端读写。
    public static final Supplier<AttachmentType<Long>> FIRE_FORM_CD_END =
            ATTACHMENT_TYPES.register("fire_form_cd_end",
                    () -> AttachmentType.builder(() -> 0L)
                            .build()
            );

    // 注火武装窗口结束游戏刻：期间的下一次近战命中会向目标注入火焰。仅服务端读写。
    public static final Supplier<AttachmentType<Long>> FIRE_INJECTION_ARM_END =
            ATTACHMENT_TYPES.register("fire_injection_arm_end",
                    () -> AttachmentType.builder(() -> 0L)
                            .build()
            );

    // 火球术蓄力开始游戏刻，0 表示未在蓄力。仅服务端读写。
    public static final Supplier<AttachmentType<Long>> FIREBALL_CHARGE_START =
            ATTACHMENT_TYPES.register("fireball_charge_start",
                    () -> AttachmentType.builder(() -> 0L)
                            .build()
            );

    // 巨大火球蓄力开始游戏刻，0 表示未在蓄力。仅服务端读写。
    public static final Supplier<AttachmentType<Long>> GIANT_FIREBALL_CHARGE_START =
            ATTACHMENT_TYPES.register("giant_fireball_charge_start",
                    () -> AttachmentType.builder(() -> 0L)
                            .build()
            );

    // 误导（阴谋家）第一步已选目标的 UUID 字符串，空串表示尚未选择。仅服务端读写。
    public static final Supplier<AttachmentType<String>> MISLEAD_SOURCE =
            ATTACHMENT_TYPES.register("mislead_source",
                    () -> AttachmentType.builder(() -> "")
                            .build()
            );

    // ===== 魔镜占卜（占卜 / 反占卜 / 通灵）相关附件 =====

    // 魔镜占卜三种模式共享的冷却结束游戏刻。仅服务端读写。
    public static final Supplier<AttachmentType<Long>> MIRROR_COOLDOWN_END =
            ATTACHMENT_TYPES.register("mirror_cooldown_end",
                    () -> AttachmentType.builder(() -> 0L)
                            .serialize(Codec.LONG)
                            .build()
            );

    // 通灵灵体交易任务："revenge;<怨灵UUID>" 或 "burial;<群系标记>"，空串表示无任务。
    // 完成后发放奖励并清空（见 SpiritCommune / MirrorEventSubscriber）。
    public static final Supplier<AttachmentType<String>> SPIRIT_TASK =
            ATTACHMENT_TYPES.register("spirit_task",
                    () -> AttachmentType.builder(() -> "")
                            .serialize(Codec.STRING)
                            .copyOnDeath()
                            .build()
            );

    // 反占卜灵性干扰场（世界级）：本维度所有干扰场的中心坐标与过期时间。
    // 服务端权威、随存档持久化，无需同步。
    public static final Supplier<AttachmentType<InterferenceFieldData>> INTERFERENCE_FIELDS =
            ATTACHMENT_TYPES.register("interference_fields",
                    () -> AttachmentType.builder(InterferenceFieldData::empty)
                            .serialize(InterferenceFieldData.CODEC)
                            .build()
            );

    // 任务进度数据（玩家级）：进行中 / 已完成任务列表、各目标进度、HUD 追踪列表。
    // 服务端权威写入，同步到客户端供任务书 GUI 与 HUD 追踪显示。死亡保留。
    public static final Supplier<AttachmentType<QuestData>> QUEST_DATA =
            ATTACHMENT_TYPES.register("quest_data",
                    () -> AttachmentType.builder(QuestData::empty)
                            .serialize(QuestData.CODEC)
                            .sync(QuestData.STREAM_CODEC)
                            .copyOnDeath()
                            .build()
            );

    // ===== 扭曲（腐化男爵 / 门 / 命运之轮 高序列规则级能力）相关附件 =====

    // 扭曲区域（世界级）：本维度所有封闭屏障 / 隔绝房间。
    // 服务端权威、随存档持久化，无需同步。
    public static final Supplier<AttachmentType<DistortionZoneData>> DISTORTION_ZONES =
            ATTACHMENT_TYPES.register("distortion_zones",
                    () -> AttachmentType.builder(DistortionZoneData::empty)
                            .serialize(DistortionZoneData.CODEC)
                            .build()
            );
    // 灵性符咒念咒开始游戏刻（>0 表示正在念咒，期间移动会打断）。仅服务端读写。
    public static final Supplier<AttachmentType<Long>> TALISMAN_CHANT_START =
            ATTACHMENT_TYPES.register("talisman_chant_start",
                    () -> AttachmentType.builder(() -> 0L)
                            .serialize(Codec.LONG)
                            .build()
                    );

    // 已研读的魔药配方集合（配方卷轴阅读后写入）：炼药锅合成前校验。死亡保留。
    public static final Supplier<AttachmentType<RecipeKnowledgeData>> READ_RECIPES =
            ATTACHMENT_TYPES.register("read_recipes",
                    () -> AttachmentType.builder(RecipeKnowledgeData::empty)
                            .serialize(RecipeKnowledgeData.CODEC)
                            .copyOnDeath()
                            .build()
                    );

    // 便利方法：注册到总线
    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }
}
