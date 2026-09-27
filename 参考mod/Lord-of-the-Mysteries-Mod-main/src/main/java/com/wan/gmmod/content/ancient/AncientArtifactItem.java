package com.wan.gmmod.content.ancient;

import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.registry.ModDataComponents;
import com.wan.gmmod.common.registry.ModEntities;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 「有少许神秘力量的古代物品」通用物品类。
 * <p>
 * 系列内的具体实例（破碎的圣像手指 / 疯人院入院记录 / 焦灼的圣袍边角 / 血染的六便士）
 * 共用本类，仅靠 {@link AncientArtifactData} 组件中的 variant 区分行为与文案；
 * 新增实例时注册新物品并传入新 variant 即可，无需新增物品类。
 * <p>
 * 各实例行为：
 * <ul>
 *   <li>{@code asylum_record} 疯人院入院记录：阅读后随机窥得一条途径能力线索，
 *       污染值 +10，并有概率吸引该途径的失控者（附近生成怨影类怪物）。</li>
 *   <li>{@code bloodstained_sixpence} 血染的六便士：掷币问运——正面提升幸运，
 *       背面吸引灾祸（附近袭来怪物）。硬币不消耗，但有投掷冷却。</li>
 *   <li>{@code broken_icon_finger} 破碎的圣像手指：炼药材料，无主动技能。</li>
 *   <li>{@code scorched_robe_fragment} 焦灼的圣袍边角：手持时近战附加火焰伤害，
 *       5% 概率反噬引燃自己（见 {@link com.wan.gmmod.common.event.AncientArtifactEventSubscriber}）。</li>
 * </ul>
 */
public class AncientArtifactItem extends Item {

    public final String variant;

    public AncientArtifactItem(String variant, Properties properties) {
        super(properties.component(ModDataComponents.ANCIENT_ARTIFACT, new AncientArtifactData(variant)));
        this.variant = variant;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            switch (variant) {
                case "asylum_record" -> readAsylumRecord(server, player, stack);
                case "bloodstained_sixpence" -> flipSixpence(server, player, stack);
                default -> {
                    return InteractionResultHolder.pass(stack);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** 疯人院入院记录：窥探呓语 → 线索 + 污染 + 失控者盯上。 */
    private void readAsylumRecord(ServerLevel level, Player player, ItemStack stack) {
        RandomSource random = level.getRandom();
        Sequences.Pathway pathway = Sequences.Pathway.values()[random.nextInt(Sequences.Pathway.values().length)];
        player.displayClientMessage(Component.translatable(
                "message.guimi_mod.asylum_read", pathway.getDisplayName(), pathway.getSequenceName(9)), false);



        level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(),
                20, 0.4, 0.6, 0.4, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.WANDERING_TRADER_DISAPPEARED,
                SoundSource.PLAYERS, 0.8F, 0.6F);

        // 吸引该途径的失控者：三成概率立刻在附近生成一只怨影类怪物
        if (random.nextFloat() < 0.3F) {
            EntityType<?> type = switch (random.nextInt(3)) {
                case 0 -> ModEntities.NIGHTMARE_SHADOW.get();
                case 1 -> ModEntities.VENGEFUL_SHADOW.get();
                default -> ModEntities.WRAITH.get();
            };
            var pos = findSpawnPos(level, player);
            if (pos != null) {
                type.spawn(level, pos, MobSpawnType.EVENT);
                player.displayClientMessage(Component.translatable("message.guimi_mod.asylum_attracted"), false);
            }
        }
        stack.shrink(1);
    }

    /** 在玩家周围 10~16 格寻找可生成位置。 */
    private net.minecraft.core.BlockPos findSpawnPos(ServerLevel level, Player player) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < 8; i++) {
            int dx = random.nextInt(33) - 16;
            int dz = random.nextInt(33) - 16;
            net.minecraft.core.BlockPos pos = player.blockPosition().offset(dx, 0, dz);
            pos = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pos);
            if (pos.distToCenterSqr(player.position()) > 64.0) {
                return pos;
            }
        }
        return null;
    }

    /** 血染的六便士：掷币问运。硬币不消耗。 */
    private void flipSixpence(ServerLevel level, Player player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return;
        }
        player.getCooldowns().addCooldown(this, 60);
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.8F, 1.4F);
        if (level.getRandom().nextBoolean()) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 6000, 0));
            player.displayClientMessage(Component.translatable("message.guimi_mod.sixpence_heads"), false);
        } else {
            player.displayClientMessage(Component.translatable("message.guimi_mod.sixpence_tails"), false);
            level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.PLAYERS, 0.8F, 1.6F);
            int spawned = 0;
            for (int i = 0; i < 2; i++) {
                var pos = findSpawnPos(level, player);
                if (pos == null) {
                    continue;
                }
                EntityType<?> type = level.getRandom().nextBoolean()
                        ? ModEntities.WRAITH.get()
                        : ModEntities.SPIRIT.get();
                if (type.spawn(level, pos, MobSpawnType.EVENT) != null) {
                    spawned++;
                }
            }
            if (spawned > 0) {
                // 让袭来者立刻锁定掷币者
                List<? extends net.minecraft.world.entity.monster.Monster> near = level.getEntitiesOfClass(
                        net.minecraft.world.entity.monster.Monster.class,
                        new AABB(player.blockPosition()).inflate(24.0),
                        m -> m.getTarget() == null);
                for (var monster : near) {
                    monster.setTarget(player);
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.guimi_mod.ancient." + variant));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    /** 物品 ID（供事件订阅者判定变体）。 */
    public static boolean is(ItemStack stack, String variant) {
        return stack.getItem() instanceof AncientArtifactItem item && item.variant.equals(variant);
    }
}
