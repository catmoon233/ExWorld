package com.wan.gmmod.common.event;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.abilities.SkillManager;
import com.wan.gmmod.content.entities.MermaidEntity;
import com.wan.gmmod.content.entities.NunEntity;
import com.wan.gmmod.content.entities.PaperFigurineEntity;
import com.wan.gmmod.content.entities.PriestEntity;
import com.wan.gmmod.content.entities.ShadowCreatureEntity;
import com.wan.gmmod.content.entities.SpiritEntity;
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
import com.wan.gmmod.content.sequences.Sequence;
import com.wan.gmmod.content.sequences.SequenceRegistry;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import com.wan.gmmod.common.registry.ModEntities;
import com.wan.gmmod.common.registry.ModSounds;
import com.wan.gmmod.content.meditation.MeditationManager;
import com.wan.gmmod.content.marionette.MarionetteManager;
import com.wan.gmmod.content.spiritwall.SpiritWallManager;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.pools.LegacySinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = GuimiMod.MODID)
public class GameEventSubscriber {
    // ===== 灵性恢复 / 枯竭风险相关常量 =====
    /** 平时缓慢自行恢复的间隔（刻），每次 +1 */
    private static final int PASSIVE_REGEN_INTERVAL = 200;
    /** 睡眠时高效恢复的间隔（刻） */
    private static final int SLEEP_REGEN_INTERVAL = 20;
    /** 睡眠时每次恢复量 */
    private static final int SLEEP_REGEN_AMOUNT = 5;
    /** 灵性 <= 此值视为「干涸」，开始累计枯竭风险 */
    private static final int DEPLETION_THRESHOLD = 5;
    /** 一级枯竭（约 30s）：偶发幻听 + 轻度反胃 */
    private static final int DEPLETION_TIER1 = 600;
    /** 二级枯竭（约 2min）：加重 debuff + 低语 */
    private static final int DEPLETION_TIER2 = 2400;
    /** 三级枯竭（约 5min）：失控征兆，可能招来怨灵 */
    private static final int DEPLETION_TIER3 = 6000;

// ===== 教堂：运行时把「大教堂」注入原版村庄 town_centers（中心广场）池 =====
    // 沿街 houses 池放不下 ~25x36 的大建筑（会被 JigsawPlacement 的碰撞检测拒绝），
    // 因此改为注入村庄中心广场池，让大教堂作为村落中心生成。
private static final String[] CHURCH_BIOMES = { "plains", "desert", "savanna", "snowy", "taiga" };
    /** 中心池里教堂的目标权重（重复加入份数）。原版中心权重约 204，取 800 以压倒性成为中心 */
    private static final int CHURCH_WEIGHT = 800;
    private static Field templatesField;

    /** LegacySinglePoolElement 的构造器是 protected，通过子类调用以获取教堂元素。 */
    private static final class ChurchSingleElement extends LegacySinglePoolElement {
        ChurchSingleElement(ResourceLocation location, Holder<StructureProcessorList> processors) {
            super(Either.left(location), processors, StructureTemplatePool.Projection.RIGID, Optional.empty());
        }
    }

    private static Field getTemplatesField() {
        if (templatesField == null) {
            try {
                Field f = StructureTemplatePool.class.getDeclaredField("templates");
                f.setAccessible(true);
                templatesField = f;
            } catch (Throwable t) {
                GuimiMod.LOGGER.error("教堂: 无法反射 StructureTemplatePool.templates", t);
            }
        }
        return templatesField;
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
    }

    private static void injectChurchIntoVillages(MinecraftServer server) {
        try {
            Registry<StructureTemplatePool> poolRegistry = server.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
            // 空处理器列表，保持教堂不苔藓不风化
            Registry<StructureProcessorList> procRegistry = server.registryAccess().registryOrThrow(Registries.PROCESSOR_LIST);
            Holder<StructureProcessorList> emptyProcessors = procRegistry.getHolderOrThrow(
                    ResourceKey.create(Registries.PROCESSOR_LIST, ResourceLocation.withDefaultNamespace("empty")));

            for (String biome : CHURCH_BIOMES) {
                String poolName = "village/" + biome + "/town_centers";
                StructureTemplatePool pool = poolRegistry.get(
                        ResourceKey.create(Registries.TEMPLATE_POOL, ResourceLocation.withDefaultNamespace(poolName)));
                if (pool == null) {
                    GuimiMod.LOGGER.error("教堂: 找不到村庄池 {}", poolName);
                    continue;
                }
                ObjectArrayList<StructurePoolElement> templates = (ObjectArrayList<StructurePoolElement>) getTemplatesField().get(pool);
                if (templates == null) {
                    GuimiMod.LOGGER.error("教堂: {} 的 templates 字段为 null", poolName);
                    continue;
                }
                // 直接把中心池清空并只放入大教堂，保证（幂等）每次都是唯一的、必然被选中的村庄中心
                templates.clear();
                ChurchSingleElement element = new ChurchSingleElement(
                        ResourceLocation.fromNamespaceAndPath("guimi_mod", "church_" + biome),
                        emptyProcessors);
                for (int i = 0; i < CHURCH_WEIGHT; i++) {
                    templates.add(element);
                }
                GuimiMod.LOGGER.info("教堂: 已向 {} 注入大教堂(中心) x{}", poolName, templates.size());
            }
        } catch (Throwable t) {
            GuimiMod.LOGGER.error("教堂注入失败", t);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        GuimiMod.LOGGER.info("诡秘之主: 玩家 {} 序列等级 {}",
                event.getEntity().getGameProfile().getName(),
                event.getEntity().getData(ModAttachments.SEQUENCE_LEVEL));

                }
    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SPIRIT.get(), SpiritEntity.createAttributes().build());
        event.put(ModEntities.WRAITH.get(), WraithEntity.createAttributes().build());
        event.put(ModEntities.MERMAID.get(), MermaidEntity.createAttributes().build());
        event.put(ModEntities.NUN.get(), NunEntity.createAttributes().build());
        event.put(ModEntities.PRIEST.get(), PriestEntity.createAttributes().build());
        event.put(ModEntities.PAPER_FIGURINE.get(), PaperFigurineEntity.createAttributes().build());
        event.put(ModEntities.SHADOW_CREATURE.get(), ShadowCreatureEntity.createAttributes().build());
        event.put(ModEntities.WOLFMAN.get(), WolfmanEntity.createAttributes().build());
        event.put(ModEntities.HUMAN_SKIN_SHADOW.get(), HumanSkinShadowEntity.createAttributes().build());
        event.put(ModEntities.EVIL_PANTHER.get(), EvilPantherEntity.createAttributes().build());
        event.put(ModEntities.THOUSAND_FACED_HUNTER.get(), ThousandFacedHunterEntity.createAttributes().build());
        event.put(ModEntities.WHITE_FOX.get(), WhiteFoxEntity.createAttributes().build());
        event.put(ModEntities.WIDOW_SPIDER.get(), WidowSpiderEntity.createAttributes().build());
        event.put(ModEntities.HORNACHIS_GOAT.get(), HornachisGoatEntity.createAttributes().build());
        event.put(ModEntities.LAVA_DEMON.get(), LavaDemonEntity.createAttributes().build());
        event.put(ModEntities.MR_K.get(), MrKEntity.createAttributes().build());
        event.put(ModEntities.BROWN_SILK_SOLEN.get(), BrownSilkSolenEntity.createAttributes().build());
        event.put(ModEntities.ABYSS_DEMON.get(), AbyssDemonEntity.createAttributes().build());
        event.put(ModEntities.EVIL_BLACK_CAT.get(), EvilBlackCatEntity.createAttributes().build());
        event.put(ModEntities.DEATH_RAVEN.get(), DeathRavenEntity.createAttributes().build());
        event.put(ModEntities.RAIN_BIRD.get(), RainBirdEntity.createAttributes().build());
        event.put(ModEntities.NIGHTMARE_SHADOW.get(), NightmareShadowEntity.createAttributes().build());
        event.put(ModEntities.VENGEFUL_SHADOW.get(), VengefulShadowEntity.createAttributes().build());
        event.put(ModEntities.LIVING_CORPSE.get(), LivingCorpseEntity.createAttributes().build());
        event.put(ModEntities.FIRE_SALAMANDER.get(), FireSalamanderEntity.createAttributes().build());
        event.put(ModEntities.GRAY_BIRD_GRANDMA.get(), GrayBirdGrandmaEntity.createAttributes().build());
        event.put(ModEntities.ONE_EYED_BULL.get(), OneEyedBullEntity.createAttributes().build());
        event.put(ModEntities.ROTTEN_SHEPHERD.get(), RottenShepherdEntity.createAttributes().build());
        event.put(ModEntities.BLACK_SPOTTED_FROG.get(), BlackSpottedFrogEntity.createAttributes().build());
        event.put(ModEntities.FROG_MEAT_PUPPET.get(), FrogMeatPuppetEntity.createAttributes().build());
        event.put(ModEntities.BLACK_SCALE_SHARK.get(), BlackScaleSharkEntity.createAttributes().build());
        // 配方材料来源生物（新增）
        event.put(ModEntities.SILVER_WAR_BEAR.get(), com.wan.gmmod.content.entities.SilverWarBearEntity.createAttributes().build());
        event.put(ModEntities.SKINLESS_BLOOD_CAT.get(), com.wan.gmmod.content.entities.SkinlessBloodCatEntity.createAttributes().build());
        event.put(ModEntities.ADULT_UNICORN.get(), com.wan.gmmod.content.entities.AdultUnicornEntity.createAttributes().build());
        event.put(ModEntities.ADULT_PEGASUS.get(), com.wan.gmmod.content.entities.AdultPegasusEntity.createAttributes().build());
        event.put(ModEntities.DAWN_ROOSTER.get(), com.wan.gmmod.content.entities.DawnRoosterEntity.createAttributes().build());
        event.put(ModEntities.NIGHTMARE_EYE.get(), com.wan.gmmod.content.entities.NightmareEyeEntity.createAttributes().build());
    }

    /** 修女 / 神父生成规则：地表（与普通陆地生物相同） */
    @SubscribeEvent
    public static void onRegisterSpawnPlacements(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.NUN.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.Mob::checkMobSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.PRIEST.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.Mob::checkMobSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.MERMAID.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.animal.WaterAnimal::checkSurfaceWaterAnimalSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // 狼人：夜晚在地表生成（checkMonsterSpawnRules 限制黑夜 + 亮度）
        event.register(ModEntities.WOLFMAN.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);

        // 新增生物生成规则
        event.register(ModEntities.HUMAN_SKIN_SHADOW.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.EVIL_PANTHER.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.THOUSAND_FACED_HUNTER.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.WHITE_FOX.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.Mob::checkMobSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.WIDOW_SPIDER.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.HORNACHIS_GOAT.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.Mob::checkMobSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.LAVA_DEMON.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.MR_K.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.Mob::checkMobSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.BROWN_SILK_SOLEN.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.ABYSS_DEMON.get(),
                net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.EVIL_BLACK_CAT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.DEATH_RAVEN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.RAIN_BIRD.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.NIGHTMARE_SHADOW.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.VENGEFUL_SHADOW.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.LIVING_CORPSE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.FIRE_SALAMANDER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.GRAY_BIRD_GRANDMA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.ONE_EYED_BULL.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.ROTTEN_SHEPHERD.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.BLACK_SPOTTED_FROG.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.FROG_MEAT_PUPPET.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.BLACK_SCALE_SHARK.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        int seqLevel = player.getData(ModAttachments.SEQUENCE_LEVEL);
        if (!Sequences.employed(player.getData(ModAttachments.PATHWAY))) return;

        // 按玩家真实途径 + 当前等级取已解锁能力并集（含低序列保留的被动），逐一驱动被动 tick
        Sequences.Pathway pathway = Sequences.fromKey(player.getData(ModAttachments.PATHWAY));
        if (pathway == null) return;
        Sequence currentSeq = SequenceRegistry.get(pathway, seqLevel);
        if (currentSeq == null) return;

        List<Ability> abilities = SkillManager.getUnlockedAbilities(player);
        for (Ability ability : abilities) {
            ability.onPassiveTick(player);
        }
        player = event.getEntity();
            if (player.level().isClientSide) return;

            if (!com.wan.gmmod.content.vision.SpiritVision.allowed(player)) {
                com.wan.gmmod.content.vision.SpiritVision.turnOff(player);
            } else if (Boolean.TRUE.equals(player.getData(ModAttachments.SPIRIT_VISION)) && player.tickCount % 100 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false, false));
            }

            if (player.tickCount % 100 == 0) {
                com.wan.gmmod.content.spirituality.SpiritualityManager.applyCap(player);
            }

            // 灵性之墙维持
            if (player instanceof ServerPlayer sp) {
                SpiritWallManager.tickPlayer(sp);
                // 冥想维持
                MeditationManager.tickPlayer(sp);
                // 灵体之线操控 / 秘偶维护（秘偶大师）
                MarionetteManager.tickPlayer(sp);
                // 魔女途径：隐形过渡 / 蛛丝蚕茧 / 冰霜护甲维持
                com.wan.gmmod.content.witch.WitchPathwayManager.tickPlayer(sp);
                // 战争之红途径：弱点洞察 / 痕迹追踪 / 直觉预警 / 火焰状态维持
                com.wan.gmmod.content.war.WarPathwayManager.tickPlayer(sp);
                // 光之风暴：维持使用者的光刃旋风动画与持续切割伤害
                com.wan.gmmod.content.abilities.LightStormAbility.tick(sp);
                // 技能栏升级迁移：晋升后基础版槽位自动替换为升级版
                if (sp.tickCount % 100 == 0) {
                    SkillManager.migrateUpgradedSlots(sp);
                }
            }

    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        // 记录玩家最近受伤时刻，供占卜「战斗状态」安全判定
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            player.setData(ModAttachments.LAST_DAMAGE_TICK, player.level().getGameTime());
            // 受伤时中断冥想
            if (player instanceof ServerPlayer sp) {
                MeditationManager.onPlayerHurt(sp);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            String actingId = player.getData(ModAttachments.ACTING_SEQUENCE_ID);
            if (actingId == null || actingId.isEmpty()) return;
            Sequence seq = SequenceRegistry.get(ResourceLocation.tryParse(actingId));
            if (seq == null) return;
            int add = 1;
            if (event.getEntity() instanceof Monster) {
                add = 3;
            }
            int progress = player.getData(ModAttachments.ACTING_PROGRESS);
            if (progress >= 100 && !player.level().isClientSide) {
                player.displayClientMessage(Component.translatable("msg.guimi_mod.acting_complete"), false);
                return;
            }
            player.setData(ModAttachments.ACTING_PROGRESS, Math.min(100, progress + add));
        }
    }

    /**
     * 灵性恢复与枯竭风险处理（服务端，仅对已就职玩家生效）。
     * <ul>
     *   <li>平时随时间缓慢自行恢复；</li>
     *   <li>睡眠是高效恢复手段；</li>
     *   <li>灵性长时间干涸会分级触发幻听、debuff、失控征兆。</li>
     * </ul>
     */
    private static void handleSpirituality(Player player) {
        int spirituality = SpiritualityManager.get(player);
        int depletionTicks = player.getData(ModAttachments.SPIRITUALITY_DEPLETION_TICKS);

        // 恢复：睡眠高效，平时缓慢（上限随序列成长，见 SpiritualityManager）
        int maxSpirituality = com.wan.gmmod.content.spirituality.SpiritualityManager.getMax(player);
        if (spirituality < maxSpirituality) {
            if (player.isSleeping()) {
                if (player.tickCount % SLEEP_REGEN_INTERVAL == 0) {
                    spirituality = Math.min(maxSpirituality, spirituality + SLEEP_REGEN_AMOUNT);
                    SpiritualityManager.set(player, spirituality);
                }
            } else if (player.tickCount % PASSIVE_REGEN_INTERVAL == 0) {
                spirituality = Math.min(maxSpirituality, spirituality + 1);
                SpiritualityManager.set(player, spirituality);
            }
        }

        // 枯竭风险追踪：干涸时累加，恢复后逐步平复（睡眠时平复更快）
        if (spirituality <= DEPLETION_THRESHOLD) {
            depletionTicks++;
            player.setData(ModAttachments.SPIRITUALITY_DEPLETION_TICKS, depletionTicks);
            applyDepletionEffects(player, depletionTicks);
        } else if (depletionTicks > 0) {
            int recover = player.isSleeping() ? 5 : 2;
            player.setData(ModAttachments.SPIRITUALITY_DEPLETION_TICKS, Math.max(0, depletionTicks - recover));
        }
    }

    /** 根据枯竭累计刻数分级施加幻听 / debuff / 失控征兆。 */
    private static void applyDepletionEffects(Player player, int depletionTicks) {
        if (depletionTicks < DEPLETION_TIER1) return;
        RandomSource random = player.getRandom();

        if (depletionTicks >= DEPLETION_TIER3) {
            // 失控征兆：强烈 debuff + 频繁幻听 + 低概率招来怨灵
            if (player.tickCount % 60 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 1, false, false));
            }
            if (player.tickCount % 80 == 0) {
                playHallucination(player);
            }
            if (player instanceof ServerPlayer sp && sp.tickCount % 100 == 0 && random.nextFloat() < 0.15f) {
                sp.sendSystemMessage(Component.translatable("spirituality.guimi_mod.losing_control"));
                trySpawnWraith(sp);
            }
        } else if (depletionTicks >= DEPLETION_TIER2) {
            // 加重 debuff + 低语
            if (player.tickCount % 80 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 0, false, false));
            }
            if (player.tickCount % 140 == 0) {
                playHallucination(player);
                player.sendSystemMessage(Component.translatable("spirituality.guimi_mod.whispers"));
            }
        } else {
            // 一级：偶发幻听 + 轻度反胃
            if (player.tickCount % 200 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false));
                playHallucination(player);
            }
        }
    }

    /** 向玩家本人发送幻听音效（个人化，只有该玩家能听到）。 */
    private static void playHallucination(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        RandomSource random = sp.getRandom();
        sp.connection.send(new ClientboundSoundPacket(
                Holder.direct(ModSounds.HALLUCINATION.get()),
                SoundSource.AMBIENT,
                sp.getX(), sp.getY(), sp.getZ(),
                0.8f, 0.9f + random.nextFloat() * 0.2f,
                random.nextLong()));
    }

    /** 失控征兆：在玩家附近的空中召来一只怨灵并锁定玩家。 */
    private static void trySpawnWraith(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        WraithEntity wraith = ModEntities.WRAITH.get().create(level);
        if (wraith == null) return;
        RandomSource random = player.getRandom();
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = 6.0 + random.nextDouble() * 6.0;
        double x = player.getX() + Math.cos(angle) * dist;
        double z = player.getZ() + Math.sin(angle) * dist;
        double y = player.getY() + 1.0 + random.nextDouble() * 2.0;
        wraith.moveTo(x, y, z, random.nextFloat() * 360f, 0);
        wraith.setTarget(player);
        level.addFreshEntity(wraith);
    }
}