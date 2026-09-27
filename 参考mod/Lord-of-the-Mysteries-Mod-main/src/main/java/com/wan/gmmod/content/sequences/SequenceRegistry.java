package com.wan.gmmod.content.sequences;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.abilities.AbilityRegistry;
import com.wan.gmmod.content.abilities.AirBulletAbility;
import com.wan.gmmod.content.abilities.BoneSofteningAbility;
import com.wan.gmmod.content.abilities.CharacteristicHerdingAbility;
import com.wan.gmmod.content.abilities.ClownFightingMasteryAbility;
import com.wan.gmmod.content.abilities.DamageTransferAbility;
import com.wan.gmmod.content.abilities.DisguiseAbility;
import com.wan.gmmod.content.abilities.FlameControlAbility;
import com.wan.gmmod.content.abilities.FlameJumpAbility;
import com.wan.gmmod.content.abilities.FlameMasteryAbility;
import com.wan.gmmod.content.abilities.FlyingCardAbility;
import com.wan.gmmod.content.abilities.IllusionAbility;
import com.wan.gmmod.content.abilities.MarionetteCombatAbility;
import com.wan.gmmod.content.abilities.PaperSubstituteAbility;
import com.wan.gmmod.content.abilities.SeerDivinationAbility;
import com.wan.gmmod.content.abilities.SeerIntuitionAbility;
import com.wan.gmmod.content.abilities.SharedVisionAbility;
import com.wan.gmmod.content.abilities.SpiritThreadControlAbility;
import com.wan.gmmod.content.abilities.SpiritThreadVisionAbility;
import com.wan.gmmod.content.abilities.WaterBreathingAbility;
// ===== 魔女途径能力 =====
import com.wan.gmmod.content.abilities.BlackFlameAbility;
import com.wan.gmmod.content.abilities.CharmAbility;
import com.wan.gmmod.content.abilities.CharmAuraAbility;
import com.wan.gmmod.content.abilities.CocoonAbility;
import com.wan.gmmod.content.abilities.ConstitutionBoostAbility;
import com.wan.gmmod.content.abilities.CurseAbility;
import com.wan.gmmod.content.abilities.EagleEyeAbility;
import com.wan.gmmod.content.abilities.FrostArmorAbility;
import com.wan.gmmod.content.abilities.FrostControlAbility;
import com.wan.gmmod.content.abilities.GenderTransitionAbility;
import com.wan.gmmod.content.abilities.IceSpearAbility;
import com.wan.gmmod.content.abilities.LightFootstepsAbility;
import com.wan.gmmod.content.abilities.MirrorDivinationAbility;
import com.wan.gmmod.content.abilities.MirrorMasteryAbility;
import com.wan.gmmod.content.abilities.MirrorSubstituteAbility;
import com.wan.gmmod.content.abilities.MisdirectAbility;
import com.wan.gmmod.content.abilities.PersuadeAbility;
import com.wan.gmmod.content.abilities.ShadowHidingAbility;
import com.wan.gmmod.content.abilities.SpiderSilkAbility;
import com.wan.gmmod.content.abilities.WandSubstituteAbility;
import com.wan.gmmod.content.abilities.WeakpointStrikeAbility;
import com.wan.gmmod.content.abilities.WitchInvisibilityAbility;
// ===== 战争之红途径能力 =====
import com.wan.gmmod.content.war.BodyEnhancementAbility;
import com.wan.gmmod.content.war.BurningWallAbility;
import com.wan.gmmod.content.war.DelayedBlastAbility;
import com.wan.gmmod.content.war.FireInjectionAbility;
import com.wan.gmmod.content.war.FireRavenAbility;
import com.wan.gmmod.content.war.FireballAbility;
import com.wan.gmmod.content.war.FlameArmorAbility;
import com.wan.gmmod.content.war.FlameLeapAbility;
import com.wan.gmmod.content.war.FlameWeaponAbility;
import com.wan.gmmod.content.war.GiantFireballAbility;
import com.wan.gmmod.content.war.InciteAbility;
import com.wan.gmmod.content.war.IntellectBoostAbility;
import com.wan.gmmod.content.war.MisleadAbility;
import com.wan.gmmod.content.war.ProvokeAbility;
import com.wan.gmmod.content.war.PyroFireResistanceAbility;
import com.wan.gmmod.content.war.WarConstitutionAbility;
import com.wan.gmmod.content.war.WarMarkerAbility;
import com.wan.gmmod.content.war.WhiteFlameSpearAbility;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 诡秘序列注册表。
 * <p>
 * 维护所有序列的 ID 索引，以及「途径 + 序列号」的二级索引，便于按途径或等级查询。
 * 具体数据与批量注册逻辑见 {@link Sequences}。
 */
public class SequenceRegistry {
    /** ID -> 序列 */
    private static final Map<ResourceLocation, Sequence> SEQUENCES = new HashMap<>();
    /** 途径 -> (序列号 -> 序列)，用于按途径/等级精确查询 */
    private static final Map<Sequences.Pathway, Map<Integer, Sequence>> BY_PATHWAY = new EnumMap<>(Sequences.Pathway.class);

    public static Sequence get(ResourceLocation id) {
        return SEQUENCES.get(id);
    }

    public static void register(Sequence sequence) {
        SEQUENCES.put(sequence.getId(), sequence);
        BY_PATHWAY.computeIfAbsent(sequence.getPathway(), k -> new HashMap<>())
                .put(sequence.getLevel(), sequence);
    }

    /** 按途径 + 序列号查询序列 */
    public static Sequence get(Sequences.Pathway pathway, int level) {
        Map<Integer, Sequence> map = BY_PATHWAY.get(pathway);
        return map == null ? null : map.get(level);
    }

    /** 获取某途径的全部序列（无序） */
    public static List<Sequence> getSequencesOf(Sequences.Pathway pathway) {
        Map<Integer, Sequence> map = BY_PATHWAY.get(pathway);
        return map == null ? Collections.emptyList() : new ArrayList<>(map.values());
    }

    /** 已注册序列总数 */
    public static int size() {
        return SEQUENCES.size();
    }

    /** 幂等标记：init 可能在客户端 commonSetup 与服务端启动被重复调用，避免重复注册。 */
    private static boolean initialized = false;

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        // 批量注册全部 22 途径 × 10 序列
        Sequences.init();
        // 序列9「占卜家」拥有灵摆直觉（被动）与危机占卜（主动）能力
        AbilityRegistry.register(GuimiMod.id("fool_9"), new SeerIntuitionAbility());
        AbilityRegistry.register(GuimiMod.id("fool_9"), new SeerDivinationAbility());

        // 序列8「小丑」：格斗精通（被动）+ 飞牌（主动，也可持纸右键触发）
        AbilityRegistry.register(GuimiMod.id("fool_8"), new ClownFightingMasteryAbility());
        AbilityRegistry.register(GuimiMod.id("fool_8"), new FlyingCardAbility());

        // 序列7「魔术师」：伤害转移 / 水下呼吸 / 骨骼软化（被动），
        // 火焰跳跃 30m / 空气弹 5 点 / 纸人替身 / 操纵火焰 / 制造幻觉（主动）
        AbilityRegistry.register(GuimiMod.id("fool_7"), new DamageTransferAbility());
        AbilityRegistry.register(GuimiMod.id("fool_7"), new WaterBreathingAbility());
        AbilityRegistry.register(GuimiMod.id("fool_7"), new BoneSofteningAbility());
        AbilityRegistry.register(GuimiMod.id("fool_7"), new FlameJumpAbility("flame_jump", 30));
        AbilityRegistry.register(GuimiMod.id("fool_7"), new AirBulletAbility("air_bullet", 5.0F));
        AbilityRegistry.register(GuimiMod.id("fool_7"), new PaperSubstituteAbility("paper_substitute", 600));
        AbilityRegistry.register(GuimiMod.id("fool_7"), new FlameControlAbility("flame_control", 1.0F));
        AbilityRegistry.register(GuimiMod.id("fool_7"), new IllusionAbility());

        // 序列6「无面人」：变形（主动）+ 魔术师能力升级版
        // （火焰跳跃 45m / 空气弹 8 点 / 操纵火焰 +30% / 纸人替身冷却大幅缩短）；伤害转移效果不变沿用
        AbilityRegistry.register(GuimiMod.id("fool_6"), new DisguiseAbility());
        AbilityRegistry.register(GuimiMod.id("fool_6"), new FlameJumpAbility("flame_jump_advanced", 45));
        AbilityRegistry.register(GuimiMod.id("fool_6"), new AirBulletAbility("air_bullet_advanced", 8.0F));
        AbilityRegistry.register(GuimiMod.id("fool_6"), new FlameControlAbility("flame_control_advanced", 1.3F));
        AbilityRegistry.register(GuimiMod.id("fool_6"), new PaperSubstituteAbility("paper_substitute_advanced", 150));

        // 序列5「秘偶大师」：灵体之线视野 / 操控、秘偶战斗、火焰操纵强化；
        // 火焰跳跃沿用序列6的 45m 升级版（能力解锁为低序列并集）
        AbilityRegistry.register(GuimiMod.id("fool_5"), new SpiritThreadVisionAbility());
        AbilityRegistry.register(GuimiMod.id("fool_5"), new SpiritThreadControlAbility());
        AbilityRegistry.register(GuimiMod.id("fool_5"), new MarionetteCombatAbility());
        AbilityRegistry.register(GuimiMod.id("fool_5"), new FlameMasteryAbility());
        AbilityRegistry.register(GuimiMod.id("fool_5"), new SharedVisionAbility());

        // ===== 魔女途径 =====
        // 序列9「刺客」：轻盈步伐 / 鹰眼视力 / 阴影躲藏 / 弱点打击（全被动）
        AbilityRegistry.register(GuimiMod.id("witch_9"), new LightFootstepsAbility());
        AbilityRegistry.register(GuimiMod.id("witch_9"), new EagleEyeAbility());
        AbilityRegistry.register(GuimiMod.id("witch_9"), new ShadowHidingAbility());
        AbilityRegistry.register(GuimiMod.id("witch_9"), new WeakpointStrikeAbility());

        // 序列8「教唆者」：体质增强（被动）+ 魅惑 / 误导 / 说服
        AbilityRegistry.register(GuimiMod.id("witch_8"), new ConstitutionBoostAbility());
        AbilityRegistry.register(GuimiMod.id("witch_8"), new CharmAbility());
        AbilityRegistry.register(GuimiMod.id("witch_8"), new MisdirectAbility());
        AbilityRegistry.register(GuimiMod.id("witch_8"), new PersuadeAbility());

        // 序列7「女巫」：性别转换（被动）+ 隐形 / 黑焰(单朵6点) / 冰霜 / 魔镜占卜 / 镜子替身 / 魔杖替身
        AbilityRegistry.register(GuimiMod.id("witch_7"), new GenderTransitionAbility());
        AbilityRegistry.register(GuimiMod.id("witch_7"), new WitchInvisibilityAbility());
        AbilityRegistry.register(GuimiMod.id("witch_7"), new BlackFlameAbility("black_flame", 1, 6.0F));
        AbilityRegistry.register(GuimiMod.id("witch_7"), new FrostControlAbility());
        AbilityRegistry.register(GuimiMod.id("witch_7"), new MirrorDivinationAbility());
        AbilityRegistry.register(GuimiMod.id("witch_7"), new MirrorSubstituteAbility());
        AbilityRegistry.register(GuimiMod.id("witch_7"), new WandSubstituteAbility());

        // 序列6「欢愉魔女」：魅力提升 / 镜子魔法强化（被动）+ 蛛丝操控 / 蚕茧 / 冰晶长枪 / 冰霜护甲 / 诅咒
        // 黑焰升级版（7 朵霰射、每朵 8 点）自动替代序列7 单朵版
        AbilityRegistry.register(GuimiMod.id("witch_6"), new CharmAuraAbility());
        AbilityRegistry.register(GuimiMod.id("witch_6"), new MirrorMasteryAbility());
        AbilityRegistry.register(GuimiMod.id("witch_6"), new SpiderSilkAbility());
        AbilityRegistry.register(GuimiMod.id("witch_6"), new CocoonAbility());
        AbilityRegistry.register(GuimiMod.id("witch_6"), new BlackFlameAbility("black_flame_advanced", 7, 8.0F));
        AbilityRegistry.register(GuimiMod.id("witch_6"), new IceSpearAbility());
        AbilityRegistry.register(GuimiMod.id("witch_6"), new FrostArmorAbility());
        AbilityRegistry.register(GuimiMod.id("witch_6"), new CurseAbility());

        // ===== 战争之红途径 =====
        // 序列9「猎人」：身体强化 / 痕迹追踪 / 弱点洞察 / 野外知识 / 直觉预警（全被动）
        AbilityRegistry.register(GuimiMod.id("war_9"), new BodyEnhancementAbility());
        AbilityRegistry.register(GuimiMod.id("war_9"), new WarMarkerAbility(GuimiMod.id("trace_tracking")));
        AbilityRegistry.register(GuimiMod.id("war_9"), new WarMarkerAbility(GuimiMod.id("weakness_insight")));
        AbilityRegistry.register(GuimiMod.id("war_9"), new WarMarkerAbility(GuimiMod.id("wild_knowledge")));
        AbilityRegistry.register(GuimiMod.id("war_9"), new WarMarkerAbility(GuimiMod.id("danger_sense")));

        // 序列8「挑衅者」：体质强化 / 洞察弱点升级（被动）+ 挑衅（主动）
        AbilityRegistry.register(GuimiMod.id("war_8"), new WarConstitutionAbility());
        AbilityRegistry.register(GuimiMod.id("war_8"), new WarMarkerAbility(GuimiMod.id("weakness_insight_advanced")));
        AbilityRegistry.register(GuimiMod.id("war_8"), new ProvokeAbility());

        // 序列7「纵火家」：火焰抗性 / 灵视强化 / 危险直觉升级 / 火焰附魔（被动）
        // + 火球术 / 火焰护甲 / 火焰武器 / 延时爆炸 / 注火 / 火鸦术 / 炽白之枪 / 燃烧之墙 / 巨大火球（主动）
        AbilityRegistry.register(GuimiMod.id("war_7"), new PyroFireResistanceAbility());
        AbilityRegistry.register(GuimiMod.id("war_7"), new WarMarkerAbility(GuimiMod.id("spirit_vision_enhance")));
        AbilityRegistry.register(GuimiMod.id("war_7"), new WarMarkerAbility(GuimiMod.id("danger_sense_advanced")));
        AbilityRegistry.register(GuimiMod.id("war_7"), new WarMarkerAbility(GuimiMod.id("flame_enchant")));
        AbilityRegistry.register(GuimiMod.id("war_7"), new FireballAbility());
        AbilityRegistry.register(GuimiMod.id("war_7"), new FlameArmorAbility());
        AbilityRegistry.register(GuimiMod.id("war_7"), new FlameWeaponAbility());
        AbilityRegistry.register(GuimiMod.id("war_7"), new DelayedBlastAbility());
        AbilityRegistry.register(GuimiMod.id("war_7"), new FireInjectionAbility());
        AbilityRegistry.register(GuimiMod.id("war_7"), new FireRavenAbility());
        AbilityRegistry.register(GuimiMod.id("war_7"), new WhiteFlameSpearAbility());
        AbilityRegistry.register(GuimiMod.id("war_7"), new BurningWallAbility());
        AbilityRegistry.register(GuimiMod.id("war_7"), new GiantFireballAbility());

        // 序列6「阴谋家」：智力提升 / 火焰塑形 / 炽白压缩强化 / 火焰体质 / 情报网（被动）
        // + 误导 / 煽动 / 火焰跃迁（主动）
        AbilityRegistry.register(GuimiMod.id("war_6"), new IntellectBoostAbility());
        AbilityRegistry.register(GuimiMod.id("war_6"), new WarMarkerAbility(GuimiMod.id("flame_shaping")));
        AbilityRegistry.register(GuimiMod.id("war_6"), new WarMarkerAbility(GuimiMod.id("white_compression")));
        AbilityRegistry.register(GuimiMod.id("war_6"), new WarMarkerAbility(GuimiMod.id("fire_body")));
        AbilityRegistry.register(GuimiMod.id("war_6"), new WarMarkerAbility(GuimiMod.id("intel_network")));
        AbilityRegistry.register(GuimiMod.id("war_6"), new MisleadAbility());
        AbilityRegistry.register(GuimiMod.id("war_6"), new InciteAbility());
        AbilityRegistry.register(GuimiMod.id("war_6"), new FlameLeapAbility());

        // ===== 全部 22 条途径（序列 9~6）=====
        // 统一由 PathwayAbilities 注册：倒吊人 / 空想家 / 暴君 / 太阳 / 白塔 / 黄昏巨人 / 黑暗 / 死神
        // 及原实验途径（完美者 / 隐者 / 命运之轮 / 审判者 / 黑皇帝 / 被缚者 / 深渊 / 月亮 / 母亲 / 错误 / 门）
        // 愚者 / 魔女 / 战争之红 直接注册于本文件上方，全部直接生效，无实验门控
        com.wan.gmmod.content.pathways.PathwayAbilities.init();

        // ===== 扭曲能力（黑皇帝·序列6 腐化男爵 完整版 / 门、命运之轮·序列6 弱化版）=====
        com.wan.gmmod.content.distortion.DistortionAbility.init();

        // 聚合定律：天使级「特性放牧」被动能力注册到各途径序列 0 ~ 2（序列2 及以上自动生效）
        CharacteristicHerdingAbility herding = new CharacteristicHerdingAbility();
        for (Sequences.Pathway pathway : Sequences.Pathway.values()) {
            for (int level = 0; level <= 2; level++) {
                AbilityRegistry.register(pathway.sequenceId(level), herding);
            }
        }
    }
}
