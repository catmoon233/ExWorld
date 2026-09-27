package com.wan.gmmod.content.divination;

import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.item.CharacteristicItem;
import com.wan.gmmod.common.registry.ModEntities;
import com.wan.gmmod.content.entities.SpiritEntity;
import com.wan.gmmod.content.entities.WraithEntity;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 通灵逻辑（魔镜「通灵」模式，全部在服务端执行）。
 * <p>
 * 以 8 米内的亡灵生物 / 灵体为目标，加权随机三种结果：
 * <ul>
 *   <li><b>信息通灵</b>（50%）：随机线索——附近宝箱方位、非凡特性物品方位、
 *   伤害该亡灵者的信息、预设剧情线索；</li>
 *   <li><b>灵体交易</b>（30%）：领取一个灵体任务（复仇 / 埋骨），
 *   完成后发放奖励（见 {@code MirrorEventSubscriber}）；</li>
 *   <li><b>灵体召唤</b>（20%）：召唤一只友好灵体跟随协战 5 分钟。</li>
 * </ul>
 * 序列加成：死神途径信息通灵权重 +30；魔女序列 4 以上有 10% 概率
 * 直接召唤强大的灵界生物作为临时盟友。
 */
public final class SpiritCommune {
    /** 盟友灵体存在时长（5 分钟） */
    private static final int ALLY_DURATION = 5 * 60 * 20;
    /** 宝箱线索扫描半径（水平） */
    private static final int CHEST_SCAN_RADIUS = 32;
    /** 宝箱线索扫描半径（垂直） */
    private static final int CHEST_SCAN_HEIGHT = 8;
    /** 特性物品线索扫描半径 */
    private static final double CHARACTERISTIC_RADIUS = 64.0;

    /** 遗骨物品的 CUSTOM_DATA 标记键 */
    public static final String RELIC_KEY = "guimi_relic";
    /** 任务类型：复仇（"revenge;&lt;怨灵UUID&gt;"） */
    public static final String TASK_REVENGE = "revenge";
    /** 任务类型：埋骨（"burial;&lt;群系标记&gt;"） */
    public static final String TASK_BURIAL = "burial";

    private SpiritCommune() {
    }

    // ---------------------------------------------------------------------
    // 入口
    // ---------------------------------------------------------------------

    /** 对目标亡灵生物 / 灵体执行一次通灵。 */
    public static void perform(ServerPlayer sp, LivingEntity target) {
        ServerLevel level = sp.serverLevel();
        RandomSource random = level.random;
        playCommuneVisuals(level, sp, target);

        // 魔女序列 4 以上（绝望魔女）：10% 概率直接召唤强大灵界生物
        int seq = sp.getData(ModAttachments.SEQUENCE_LEVEL);
        boolean witch = Sequences.Pathway.WITCH.getKey().equals(sp.getData(ModAttachments.PATHWAY));
        if (witch && seq > 0 && seq <= 4 && random.nextInt(100) < 10) {
            summonAlly(sp, true);
            return;
        }

        // 加权随机：信息 50 / 交易 30 / 召唤 20；死神途径信息更准确（+30）
        int infoWeight = 50;
        if (Sequences.Pathway.DEATH.getKey().equals(sp.getData(ModAttachments.PATHWAY))) {
            infoWeight += 30;
        }
        int roll = random.nextInt(infoWeight + 30 + 20);
        if (roll < infoWeight) {
            giveClue(sp, level, target);
        } else if (roll < infoWeight + 30) {
            assignTask(sp, level, target);
        } else {
            summonAlly(sp, false);
        }
    }

    // ---------------------------------------------------------------------
    // A. 信息通灵
    // ---------------------------------------------------------------------

    /** 从可用线索池中随机抽取一条发给玩家。 */
    private static void giveClue(ServerPlayer sp, ServerLevel level, LivingEntity target) {
        List<Component> clues = new ArrayList<>();

        // 附近宝箱的大致方向
        BlockPos chest = findNearestChest(level, sp.blockPosition());
        if (chest != null) {
            clues.add(Component.translatable("message.guimi_mod.commune.clue.chest",
                    PendulumDivination.directionComponent(sp, chest.getCenter())));
        }
        // 附近非凡特性物品实体的方位
        ItemEntity characteristic = findCharacteristicItem(level, sp);
        if (characteristic != null) {
            clues.add(Component.translatable("message.guimi_mod.commune.clue.characteristic",
                    PendulumDivination.directionComponent(sp, characteristic.position())));
        }
        // 伤害该亡灵者的信息
        LivingEntity attacker = target.getLastHurtByMob();
        if (attacker != null && attacker.isAlive()) {
            clues.add(Component.translatable("message.guimi_mod.commune.clue.killer",
                    attacker.getDisplayName()));
        }
        // 预设剧情线索
        clues.add(Component.translatable(
                "message.guimi_mod.commune.clue.lore_" + (level.random.nextInt(3) + 1)));

        Component clue = clues.get(level.random.nextInt(clues.size()));
        sp.sendSystemMessage(clue);
    }

    /** 在玩家周围（水平 ±32、垂直 ±8）寻找最近的宝箱。 */
    @Nullable
    private static BlockPos findNearestChest(ServerLevel level, BlockPos center) {
        BlockPos nearest = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-CHEST_SCAN_RADIUS, -CHEST_SCAN_HEIGHT, -CHEST_SCAN_RADIUS),
                center.offset(CHEST_SCAN_RADIUS, CHEST_SCAN_HEIGHT, CHEST_SCAN_RADIUS))) {
            if (level.getBlockState(pos).getBlock() instanceof ChestBlock) {
                double dist = pos.distSqr(center);
                if (dist < bestDist) {
                    bestDist = dist;
                    nearest = pos.immutable();
                }
            }
        }
        return nearest;
    }

    /** 在玩家周围 64 格内寻找掉落的非凡特性物品实体。 */
    @Nullable
    private static ItemEntity findCharacteristicItem(ServerLevel level, ServerPlayer sp) {
        AABB area = sp.getBoundingBox().inflate(CHARACTERISTIC_RADIUS);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area,
                ie -> ie.getItem().getItem() instanceof CharacteristicItem);
        return items.isEmpty() ? null : items.get(0);
    }

    // ---------------------------------------------------------------------
    // B. 灵体交易（任务）
    // ---------------------------------------------------------------------

    /** 领取一个灵体任务：复仇（击杀怨灵）或埋骨（携遗骨到指定群系）。 */
    private static void assignTask(ServerPlayer sp, ServerLevel level, LivingEntity target) {
        if (!sp.getData(ModAttachments.SPIRIT_TASK).isEmpty()) {
            sp.sendSystemMessage(Component.translatable("message.guimi_mod.commune.task.active"));
            return;
        }
        if (level.random.nextBoolean()) {
            // 复仇：在附近召来一只敌对怨灵，击杀后完成任务
            WraithEntity wraith = ModEntities.WRAITH.get().create(level);
            if (wraith == null) {
                giveClue(sp, level, target);
                return;
            }
            RandomSource random = level.random;
            double angle = random.nextDouble() * Math.PI * 2;
            double x = sp.getX() + Math.cos(angle) * 8.0;
            double z = sp.getZ() + Math.sin(angle) * 8.0;
            wraith.moveTo(x, sp.getY() + 1.5, z, random.nextFloat() * 360f, 0);
            wraith.setTarget(sp);
            level.addFreshEntity(wraith);
            sp.setData(ModAttachments.SPIRIT_TASK, TASK_REVENGE + ";" + wraith.getUUID());
            sp.sendSystemMessage(Component.translatable("message.guimi_mod.commune.task.revenge"));
        } else {
            // 埋骨：给予遗骨，携带它抵达指定群系后完成任务
            String[] tokens = {"forest", "desert", "mountain"};
            String token = tokens[level.random.nextInt(tokens.length)];
            ItemStack relic = new ItemStack(Items.BONE);
            CustomData.update(DataComponents.CUSTOM_DATA, relic, tag -> tag.putBoolean(RELIC_KEY, true));
            relic.set(DataComponents.CUSTOM_NAME,
                    Component.translatable("item.guimi_mod.spirit_relic"));
            if (!sp.addItem(relic)) {
                sp.drop(relic, false);
            }
            sp.setData(ModAttachments.SPIRIT_TASK, TASK_BURIAL + ";" + token);
            sp.sendSystemMessage(Component.translatable("message.guimi_mod.commune.task.burial",
                    Component.translatable("message.guimi_mod.commune.biome." + token)));
        }
    }

    /** 物品是否为通灵任务遗骨。 */
    public static boolean isRelic(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean(RELIC_KEY);
    }

    /** 坐标所在群系是否匹配埋骨任务的群系标记。 */
    public static boolean matchesBiomeToken(ServerLevel level, BlockPos pos, String token) {
        var biome = level.getBiome(pos);
        return switch (token) {
            case "forest" -> biome.is(BiomeTags.IS_FOREST);
            case "desert" -> biome.is(Biomes.DESERT);
            case "mountain" -> biome.is(BiomeTags.IS_MOUNTAIN);
            default -> false;
        };
    }

    /** 任务完成：发放随机奖励并清空任务（由 MirrorEventSubscriber 调用）。 */
    public static void completeTask(ServerPlayer sp, String taskType) {
        sp.setData(ModAttachments.SPIRIT_TASK, "");
        sp.sendSystemMessage(Component.translatable(
                "message.guimi_mod.commune.task." + taskType + "_done"));

        ItemStack[] rewards = {
                new ItemStack(Items.ECHO_SHARD, 2),
                new ItemStack(Items.AMETHYST_SHARD, 3),
                new ItemStack(Items.GOLD_INGOT, 3),
                new ItemStack(Items.EXPERIENCE_BOTTLE, 2)
        };
        ItemStack reward = rewards[sp.getRandom().nextInt(rewards.length)];
        if (!sp.addItem(reward)) {
            sp.drop(reward, false);
        }
        sp.sendSystemMessage(Component.translatable("message.guimi_mod.commune.task.reward"));
    }

    // ---------------------------------------------------------------------
    // C. 灵体召唤
    // ---------------------------------------------------------------------

    /** 召唤一只友好灵体跟随协战 5 分钟；powerful 时属性大幅强化。 */
    private static void summonAlly(ServerPlayer sp, boolean powerful) {
        ServerLevel level = sp.serverLevel();
        SpiritEntity spirit = ModEntities.SPIRIT.get().create(level);
        if (spirit == null) {
            return;
        }
        RandomSource random = level.random;
        double angle = random.nextDouble() * Math.PI * 2;
        spirit.moveTo(sp.getX() + Math.cos(angle) * 2.0, sp.getY(),
                sp.getZ() + Math.sin(angle) * 2.0, random.nextFloat() * 360f, 0);
        spirit.setAlly(sp, level.getGameTime() + ALLY_DURATION);
        if (powerful) {
            var health = spirit.getAttribute(Attributes.MAX_HEALTH);
            var damage = spirit.getAttribute(Attributes.ATTACK_DAMAGE);
            if (health != null) health.setBaseValue(40.0);
            if (damage != null) damage.setBaseValue(8.0);
            spirit.setHealth(spirit.getMaxHealth());
        }
        level.addFreshEntity(spirit);
        sp.sendSystemMessage(Component.translatable(
                powerful ? "message.guimi_mod.commune.summon.powerful"
                        : "message.guimi_mod.commune.summon.ally"));
    }

    // ---------------------------------------------------------------------
    // 视觉
    // ---------------------------------------------------------------------

    /** 通灵视觉：幽蓝灵魂粒子环绕玩家与目标 + 灵魂逸散音效。 */
    private static void playCommuneVisuals(ServerLevel level, ServerPlayer sp, LivingEntity target) {
        level.sendParticles(ParticleTypes.SOUL,
                sp.getX(), sp.getY() + 1.2, sp.getZ(), 20, 0.6, 0.8, 0.6, 0.02);
        level.sendParticles(ParticleTypes.SCULK_SOUL,
                target.getX(), target.getY() + 1.0, target.getZ(), 16, 0.4, 0.7, 0.4, 0.02);
        level.playSound(null, sp.blockPosition(), SoundEvents.SOUL_ESCAPE.value(),
                SoundSource.PLAYERS, 1.0F, 0.8F);
    }
}
