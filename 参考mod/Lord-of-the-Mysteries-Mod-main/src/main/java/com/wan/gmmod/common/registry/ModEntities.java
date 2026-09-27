package com.wan.gmmod.common.registry;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.AirBulletEntity;
import com.wan.gmmod.content.entities.BlackFlameEntity;
import com.wan.gmmod.content.entities.BulletEntity;
import com.wan.gmmod.content.entities.IceSpearEntity;
import com.wan.gmmod.content.entities.FireRavenEntity;
import com.wan.gmmod.content.entities.FlameOrbEntity;
import com.wan.gmmod.content.entities.FlameSpearEntity;
import com.wan.gmmod.content.entities.FlameTrapEntity;
import com.wan.gmmod.content.entities.FlyingCardEntity;
import com.wan.gmmod.content.entities.MermaidEntity;
import com.wan.gmmod.content.entities.NunEntity;
import com.wan.gmmod.content.entities.PaperFigurineEntity;
import com.wan.gmmod.content.entities.PriestEntity;
import com.wan.gmmod.content.entities.ShadowCreatureEntity;
import com.wan.gmmod.content.entities.SpiritEntity;
import com.wan.gmmod.content.entities.TalismanProjectileEntity;
import com.wan.gmmod.content.entities.WraithEntity;
import com.wan.gmmod.content.entities.WolfmanEntity;
import com.wan.gmmod.content.entities.HumanSkinShadowEntity;
import com.wan.gmmod.content.entities.EvilPantherEntity;
import com.wan.gmmod.content.entities.ThousandFacedHunterEntity;
import com.wan.gmmod.content.entities.WhiteFoxEntity;
import com.wan.gmmod.content.entities.WidowSpiderEntity;
import com.wan.gmmod.content.entities.HornachisGoatEntity;
import com.wan.gmmod.content.entities.LavaDemonEntity;
import com.wan.gmmod.content.entities.MrKEntity;
import com.wan.gmmod.content.entities.BrownSilkSolenEntity;
import com.wan.gmmod.content.entities.AbyssDemonEntity;
import com.wan.gmmod.content.entities.EvilBlackCatEntity;
import com.wan.gmmod.content.entities.DeathRavenEntity;
import com.wan.gmmod.content.entities.RainBirdEntity;
import com.wan.gmmod.content.entities.NightmareShadowEntity;
import com.wan.gmmod.content.entities.VengefulShadowEntity;
import com.wan.gmmod.content.entities.LivingCorpseEntity;
import com.wan.gmmod.content.entities.FireSalamanderEntity;
import com.wan.gmmod.content.entities.GrayBirdGrandmaEntity;
import com.wan.gmmod.content.entities.OneEyedBullEntity;
import com.wan.gmmod.content.entities.RottenShepherdEntity;
import com.wan.gmmod.content.entities.BlackSpottedFrogEntity;
import com.wan.gmmod.content.entities.FrogMeatPuppetEntity;
import com.wan.gmmod.content.entities.BlackScaleSharkEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, GuimiMod.MODID);

    public static final Supplier<EntityType<SpiritEntity>> SPIRIT = ENTITIES.register("spirit",
            () -> EntityType.Builder.of(SpiritEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f)
                    .build("spirit"));

    // 怨灵：主动攻击玩家的灵体怪物（GeckoLib 动画实体）
    public static final Supplier<EntityType<WraithEntity>> WRAITH = ENTITIES.register("wraith",
            () -> EntityType.Builder.of(WraithEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.9f)
                    .build("wraith"));

    // 修女：温和的中立居民（GeckoLib 动画实体），在村庄 / 平原自然生成
    public static final Supplier<EntityType<NunEntity>> NUN = ENTITIES.register("nun",
            () -> EntityType.Builder.<NunEntity>of(NunEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.9f)
                    .build("nun"));

    // 神父：温和的中立居民（GeckoLib 动画实体），在村庄 / 平原自然生成
    public static final Supplier<EntityType<PriestEntity>> PRIEST = ENTITIES.register("priest",
            () -> EntityType.Builder.<PriestEntity>of(PriestEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.9f)
                    .build("priest"));

    // 美人鱼：海洋生物群系低概率生成的水生生物（GeckoLib 动画实体），
    // 周期性演唱「美人鱼的歌声」，为秘偶大师晋升仪式提供环境条件
    public static final Supplier<EntityType<MermaidEntity>> MERMAID = ENTITIES.register("mermaid",
            () -> EntityType.Builder.of(MermaidEntity::new, MobCategory.WATER_CREATURE)
                    .sized(0.6f, 1.8f)
                    .build("mermaid"));

    // 飞牌：小丑「飞牌」能力的弹射物
    public static final Supplier<EntityType<FlyingCardEntity>> FLYING_CARD = ENTITIES.register("flying_card",
            () -> EntityType.Builder.<FlyingCardEntity>of(FlyingCardEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("flying_card"));

    // 空气弹：魔术师 / 无面人「空气弹」能力的无形弹射物
    public static final Supplier<EntityType<AirBulletEntity>> AIR_BULLET = ENTITIES.register("air_bullet",
            () -> EntityType.Builder.<AirBulletEntity>of(AirBulletEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("air_bullet"));

    // 子弹：丧钟发射的弹射物，外观与效果由装填的子弹物品决定
    public static final Supplier<EntityType<BulletEntity>> BULLET = ENTITIES.register("bullet",
            () -> EntityType.Builder.<BulletEntity>of(BulletEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("bullet"));

    // 纸人：纸人替身能力的静态锚点实体
    public static final Supplier<EntityType<PaperFigurineEntity>> PAPER_FIGURINE = ENTITIES.register("paper_figurine",
            () -> EntityType.Builder.of(PaperFigurineEntity::new, MobCategory.MISC)
                    .sized(0.5f, 1.6f)
                    .clientTrackingRange(8)
                    .build("paper_figurine"));

    // 黑焰：女巫「操控黑焰」的弹射物，仅对有灵性的目标有效
    public static final Supplier<EntityType<BlackFlameEntity>> BLACK_FLAME = ENTITIES.register("black_flame",
            () -> EntityType.Builder.<BlackFlameEntity>of(BlackFlameEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("black_flame"));

    // 冰晶长枪：欢愉魔女「冰霜强化」的投掷物
    public static final Supplier<EntityType<IceSpearEntity>> ICE_SPEAR = ENTITIES.register("ice_spear",
            () -> EntityType.Builder.<IceSpearEntity>of(IceSpearEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("ice_spear"));

    // 火球：纵火家「火球术」「巨大火球」的弹射物
    public static final Supplier<EntityType<FlameOrbEntity>> FLAME_ORB = ENTITIES.register("flame_orb",
            () -> EntityType.Builder.<FlameOrbEntity>of(FlameOrbEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("flame_orb"));

    // 火鸦：纵火家「火鸦术」召唤的追踪火焰
    public static final Supplier<EntityType<FireRavenEntity>> FIRE_RAVEN = ENTITIES.register("fire_raven",
            () -> EntityType.Builder.<FireRavenEntity>of(FireRavenEntity::new, MobCategory.MISC)
                    .sized(0.35f, 0.35f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("fire_raven"));

    // 炽白之枪：纵火家「炽白之枪」投掷的穿透长枪
    public static final Supplier<EntityType<FlameSpearEntity>> FLAME_SPEAR = ENTITIES.register("flame_spear",
            () -> EntityType.Builder.<FlameSpearEntity>of(FlameSpearEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("flame_spear"));

// 火焰陷阱：纵火家「延时爆炸」放置的陷阱
    public static final Supplier<EntityType<FlameTrapEntity>> FLAME_TRAP = ENTITIES.register("flame_trap",
            () -> EntityType.Builder.<FlameTrapEntity>of(FlameTrapEntity::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f)
                    .clientTrackingRange(8)
                    .updateInterval(10)
                    .build("flame_trap"));

    // 阴影生物：倒吊人「序列 7 隐修士」召唤阴影生物技能的暗影兽（GeckoLib 动画实体）
    public static final Supplier<EntityType<ShadowCreatureEntity>> SHADOW_CREATURE =
            ENTITIES.register("shadow_creature",
                    () -> EntityType.Builder.<ShadowCreatureEntity>of(ShadowCreatureEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.9f)
                            .clientTrackingRange(8)
                            .build("shadow_creature"));

    // 狼人：被缚者「序列 7 狼人」对应的敌对生物（GeckoLib 动画实体），夜晚自然生成
    public static final Supplier<EntityType<WolfmanEntity>> WOLFMAN =
            ENTITIES.register("wolfman",
                    () -> EntityType.Builder.<WolfmanEntity>of(WolfmanEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.9f)
                            .clientTrackingRange(8)
                            .build("wolfman"));

    // 灵性符咒：念咒灌注后由玩家投掷的投射物，落点激发祈求对象效果
    public static final Supplier<EntityType<TalismanProjectileEntity>> TALISMAN =
            ENTITIES.register("talisman",
                    () -> EntityType.Builder.<TalismanProjectileEntity>of(TalismanProjectileEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("talisman"));

    // ========== 新增生物 ==========

    // 人皮幽影：漂浮的灵体怪物
    public static final Supplier<EntityType<HumanSkinShadowEntity>> HUMAN_SKIN_SHADOW =
            ENTITIES.register("human_skin_shadow",
                    () -> EntityType.Builder.<HumanSkinShadowEntity>of(HumanSkinShadowEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(8)
                            .build("human_skin_shadow"));

    // 邪纹黑豹：迅捷的黑暗猎豹
    public static final Supplier<EntityType<EvilPantherEntity>> EVIL_PANTHER =
            ENTITIES.register("evil_panther",
                    () -> EntityType.Builder.<EvilPantherEntity>of(EvilPantherEntity::new, MobCategory.MONSTER)
                            .sized(2.0f, 1.8f)
                            .clientTrackingRange(8)
                            .build("evil_panther"));

    // 千面狩猎者（五头龙）：会吐龙息的巨型飞龙
    public static final Supplier<EntityType<ThousandFacedHunterEntity>> THOUSAND_FACED_HUNTER =
            ENTITIES.register("thousand_faced_hunter",
                    () -> EntityType.Builder.<ThousandFacedHunterEntity>of(ThousandFacedHunterEntity::new, MobCategory.MONSTER)
                            .sized(3.0f, 2.5f)
                            .clientTrackingRange(16)
                            .build("thousand_faced_hunter"));

    // 白尾赤狐：温顺的白色狐狸
    public static final Supplier<EntityType<WhiteFoxEntity>> WHITE_FOX =
            ENTITIES.register("white_fox",
                    () -> EntityType.Builder.<WhiteFoxEntity>of(WhiteFoxEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 0.8f)
                            .clientTrackingRange(8)
                            .build("white_fox"));

    // 寡妇巨蛛：会吐蛛网的巨型蜘蛛
    public static final Supplier<EntityType<WidowSpiderEntity>> WIDOW_SPIDER =
            ENTITIES.register("widow_spider",
                    () -> EntityType.Builder.<WidowSpiderEntity>of(WidowSpiderEntity::new, MobCategory.MONSTER)
                            .sized(1.0f, 0.8f)
                            .clientTrackingRange(8)
                            .build("widow_spider"));

    // 霍纳奇斯灰山羊：山地山羊
    public static final Supplier<EntityType<HornachisGoatEntity>> HORNACHIS_GOAT =
            ENTITIES.register("hornachis_goat",
                    () -> EntityType.Builder.<HornachisGoatEntity>of(HornachisGoatEntity::new, MobCategory.CREATURE)
                            .sized(0.8f, 1.2f)
                            .clientTrackingRange(8)
                            .build("hornachis_goat"));

    // 岩浆之魔：岩浆中诞生的恶魔
    public static final Supplier<EntityType<LavaDemonEntity>> LAVA_DEMON =
            ENTITIES.register("lava_demon",
                    () -> EntityType.Builder.<LavaDemonEntity>of(LavaDemonEntity::new, MobCategory.MONSTER)
                            .sized(0.8f, 1.8f)
                            .clientTrackingRange(8)
                            .build("lava_demon"));

    // 极光会-K先生：极光会的人形成员
    public static final Supplier<EntityType<MrKEntity>> MR_K =
            ENTITIES.register("mr_k",
                    () -> EntityType.Builder.<MrKEntity>of(MrKEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(8)
                            .build("mr_k"));

// 魔女教会-布朗丝·索伦：魔女途径序列6欢愉魔女，远程施法者
    public static final Supplier<EntityType<BrownSilkSolenEntity>> BROWN_SILK_SOLEN =
            ENTITIES.register("brown_silk_solen",
                    () -> EntityType.Builder.<BrownSilkSolenEntity>of(BrownSilkSolenEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.9f)
                            .clientTrackingRange(8)
                            .build("brown_silk_solen"));

    // 深渊恶魔：来自深渊的巨型恶魔
    public static final Supplier<EntityType<AbyssDemonEntity>> ABYSS_DEMON =
            ENTITIES.register("abyss_demon",
                    () -> EntityType.Builder.<AbyssDemonEntity>of(AbyssDemonEntity::new, MobCategory.MONSTER)
                            .sized(2.0f, 8.0f)
                            .clientTrackingRange(16)
                            .build("abyss_demon"));

    // 厄运黑猫：带来厄运的黑猫
    public static final Supplier<EntityType<EvilBlackCatEntity>> EVIL_BLACK_CAT =
            ENTITIES.register("evil_black_cat",
                    () -> EntityType.Builder.<EvilBlackCatEntity>of(EvilBlackCatEntity::new, MobCategory.MONSTER).sized(0.6f, 0.8f).build("evil_black_cat"));
    // 告死乌鸦：预告死亡的乌鸦
    public static final Supplier<EntityType<DeathRavenEntity>> DEATH_RAVEN =
            ENTITIES.register("death_raven",
                    () -> EntityType.Builder.<DeathRavenEntity>of(DeathRavenEntity::new, MobCategory.MONSTER).sized(0.6f, 0.8f).build("death_raven"));
    // 告雨鸟：预报降雨的鸟
    public static final Supplier<EntityType<RainBirdEntity>> RAIN_BIRD =
            ENTITIES.register("rain_bird",
                    () -> EntityType.Builder.of(RainBirdEntity::new, MobCategory.CREATURE).sized(0.6f, 0.8f).build("rain_bird"));
    // 噩梦之影：噩梦中的暗影
    public static final Supplier<EntityType<NightmareShadowEntity>> NIGHTMARE_SHADOW =
            ENTITIES.register("nightmare_shadow",
                    () -> EntityType.Builder.<NightmareShadowEntity>of(NightmareShadowEntity::new, MobCategory.MONSTER).sized(0.6f, 1.8f).build("nightmare_shadow"));
    // 复仇之影：复仇的暗影
    public static final Supplier<EntityType<VengefulShadowEntity>> VENGEFUL_SHADOW =
            ENTITIES.register("vengeful_shadow",
                    () -> EntityType.Builder.<VengefulShadowEntity>of(VengefulShadowEntity::new, MobCategory.MONSTER).sized(0.6f, 1.8f).build("vengeful_shadow"));
    // 活尸：行走的尸体
    public static final Supplier<EntityType<LivingCorpseEntity>> LIVING_CORPSE =
            ENTITIES.register("living_corpse",
                    () -> EntityType.Builder.<LivingCorpseEntity>of(LivingCorpseEntity::new, MobCategory.MONSTER).sized(0.6f, 1.9f).build("living_corpse"));
    // 火蝾螈：火焰中诞生的蝾螈
    public static final Supplier<EntityType<FireSalamanderEntity>> FIRE_SALAMANDER =
            ENTITIES.register("fire_salamander",
                    () -> EntityType.Builder.<FireSalamanderEntity>of(FireSalamanderEntity::new, MobCategory.MONSTER).sized(0.6f, 0.6f).build("fire_salamander"));
    // 灰鸟祖母：古老的灰色巨鸟
    public static final Supplier<EntityType<GrayBirdGrandmaEntity>> GRAY_BIRD_GRANDMA =
            ENTITIES.register("gray_bird_grandma",
                    () -> EntityType.Builder.<GrayBirdGrandmaEntity>of(GrayBirdGrandmaEntity::new, MobCategory.MONSTER).sized(0.6f, 1.2f).build("gray_bird_grandma"));
    // 独眼白牛：独眼的白色巨牛
    public static final Supplier<EntityType<OneEyedBullEntity>> ONE_EYED_BULL =
            ENTITIES.register("one_eyed_bull",
                    () -> EntityType.Builder.of(OneEyedBullEntity::new, MobCategory.CREATURE).sized(0.8f, 1.4f).build("one_eyed_bull"));
    // 腐烂牧者：腐烂的牧羊人
    public static final Supplier<EntityType<RottenShepherdEntity>> ROTTEN_SHEPHERD =
            ENTITIES.register("rotten_shepherd",
                    () -> EntityType.Builder.<RottenShepherdEntity>of(RottenShepherdEntity::new, MobCategory.MONSTER).sized(0.6f, 1.9f).build("rotten_shepherd"));
    // 黑斑青蛙：带黑斑的青蛙
    public static final Supplier<EntityType<BlackSpottedFrogEntity>> BLACK_SPOTTED_FROG =
            ENTITIES.register("black_spotted_frog",
                    () -> EntityType.Builder.of(BlackSpottedFrogEntity::new, MobCategory.CREATURE).sized(0.6f, 0.4f).build("black_spotted_frog"));
    // 黑斑青蛙肉布套人：被操控的蛙人傀儡
    public static final Supplier<EntityType<FrogMeatPuppetEntity>> FROG_MEAT_PUPPET =
            ENTITIES.register("frog_meat_puppet",
                    () -> EntityType.Builder.<FrogMeatPuppetEntity>of(FrogMeatPuppetEntity::new, MobCategory.MONSTER).sized(0.6f, 1.8f).build("frog_meat_puppet"));
    // 黑鳞鲨：黑色鳞片的鲨鱼
    public static final Supplier<EntityType<BlackScaleSharkEntity>> BLACK_SCALE_SHARK =
            ENTITIES.register("black_scale_shark",
                    () -> EntityType.Builder.<BlackScaleSharkEntity>of(BlackScaleSharkEntity::new, MobCategory.MONSTER).sized(0.8f, 0.8f).build("black_scale_shark"));

    // ===== 配方材料来源生物（新增）=====

    // 银白战熊：审判者途径材料的来源猛兽
    public static final Supplier<EntityType<com.wan.gmmod.content.entities.SilverWarBearEntity>> SILVER_WAR_BEAR =
            ENTITIES.register("silver_war_bear",
                    () -> EntityType.Builder.<com.wan.gmmod.content.entities.SilverWarBearEntity>of(com.wan.gmmod.content.entities.SilverWarBearEntity::new, MobCategory.MONSTER).sized(1.9f, 2.6f).build("silver_war_bear"));
    // 无皮血猫：隐者途径材料的来源异变生物
    public static final Supplier<EntityType<com.wan.gmmod.content.entities.SkinlessBloodCatEntity>> SKINLESS_BLOOD_CAT =
            ENTITIES.register("skinless_blood_cat",
                    () -> EntityType.Builder.<com.wan.gmmod.content.entities.SkinlessBloodCatEntity>of(com.wan.gmmod.content.entities.SkinlessBloodCatEntity::new, MobCategory.MONSTER).sized(0.7f, 0.8f).build("skinless_blood_cat"));
    // 成年独角兽：月亮途径材料的来源中立生物
    public static final Supplier<EntityType<com.wan.gmmod.content.entities.AdultUnicornEntity>> ADULT_UNICORN =
            ENTITIES.register("adult_unicorn",
                    () -> EntityType.Builder.of(com.wan.gmmod.content.entities.AdultUnicornEntity::new, MobCategory.CREATURE).sized(1.2f, 1.8f).build("adult_unicorn"));
    // 成年飞马：月亮途径材料的来源飞行生物
    public static final Supplier<EntityType<com.wan.gmmod.content.entities.AdultPegasusEntity>> ADULT_PEGASUS =
            ENTITIES.register("adult_pegasus",
                    () -> EntityType.Builder.of(com.wan.gmmod.content.entities.AdultPegasusEntity::new, MobCategory.CREATURE).sized(1.2f, 1.8f).build("adult_pegasus"));
    // 黎明雄鸡：太阳途径材料的来源温顺家禽
    public static final Supplier<EntityType<com.wan.gmmod.content.entities.DawnRoosterEntity>> DAWN_ROOSTER =
            ENTITIES.register("dawn_rooster",
                    () -> EntityType.Builder.of(com.wan.gmmod.content.entities.DawnRoosterEntity::new, MobCategory.CREATURE).sized(0.5f, 0.8f).build("dawn_rooster"));
    // 噩梦邪眼：隐者途径材料的来源双形态飞行怪物
    public static final Supplier<EntityType<com.wan.gmmod.content.entities.NightmareEyeEntity>> NIGHTMARE_EYE =
            ENTITIES.register("nightmare_eye",
                    () -> EntityType.Builder.<com.wan.gmmod.content.entities.NightmareEyeEntity>of(com.wan.gmmod.content.entities.NightmareEyeEntity::new, MobCategory.MONSTER).sized(0.9f, 0.9f).clientTrackingRange(8).build("nightmare_eye"));

}