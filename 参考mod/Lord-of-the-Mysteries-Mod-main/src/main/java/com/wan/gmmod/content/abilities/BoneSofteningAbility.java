package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 魔术师「骨骼软化」——序列 7 被动能力。
 * <ul>
 *   <li>挣脱束缚：蛛网、细雪等减速方块中移动速度 +200%；</li>
 *   <li>钻洞：潜行且前方存在 1 格高空间时自动匍匐（碰撞箱压至 0.6 高），
 *       移动速度 -50%、无法攻击 / 使用物品；松开潜行键即恢复，
 *       头顶仍被阻挡时保持匍匐直至爬出（原版姿态逻辑天然处理挤出）。</li>
 * </ul>
 * 匍匐借助 NeoForge 的 {@code setForcedPose(Pose.SWIMMING)}：与原版爬行动画一致，
 * 护甲渲染不丢失，与无面人变形亦无冲突。双端执行以保证移动手感平滑。
 */
public class BoneSofteningAbility extends Ability {
    /** 钻洞减速修饰符（-50% 总移速），仅服务端挂载 */
    private static final ResourceLocation CRAWL_SLOW_ID = GuimiMod.id("bone_crawl_slow");

    public BoneSofteningAbility() {
        super(GuimiMod.id("bone_softening"));
    }

    @Override
    public void onPassiveTick(Player player) {
        BlockState state = player.getInBlockState();
        if (state.is(Blocks.COBWEB) || state.is(Blocks.POWDER_SNOW)) {
            // 抵消束缚方块的减速：水平 +200%，竖直略升便于向上挣脱
            Vec3 dm = player.getDeltaMovement();
            player.setDeltaMovement(dm.x * 3.0, dm.y * 1.5, dm.z * 3.0);
        }
        tickCrawl(player);
    }

    /** 钻洞状态机：进入 / 保持 / 退出匍匐。 */
    private static void tickCrawl(Player player) {
        boolean crawling = player.getData(ModAttachments.BONE_CRAWLING);
        boolean shouldCrawl;
        if (crawling) {
            // 保持：仍在潜行，或头顶过低暂时无法站起（继续爬直到出洞）
            shouldCrawl = player.isShiftKeyDown() || underLowCeiling(player);
        } else {
            // 进入：潜行 + 前方存在 1 格高的可钻空间（或已被压在低顶之下）
            shouldCrawl = player.isShiftKeyDown() && (frontLowGap(player) || underLowCeiling(player));
        }
        if (shouldCrawl == crawling) {
            return;
        }
        player.setData(ModAttachments.BONE_CRAWLING, shouldCrawl);
        // 强制匍匐姿态（0.6 高碰撞箱、原版爬行模型压缩动画）
        player.setForcedPose(shouldCrawl ? Pose.SWIMMING : null);
        player.refreshDimensions();
        if (!player.level().isClientSide) {
            AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.removeModifier(CRAWL_SLOW_ID);
                if (shouldCrawl) {
                    speed.addTransientModifier(new AttributeModifier(CRAWL_SLOW_ID,
                            -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
            }
        }
    }

    /** 前方脚部为可通行空间、且其上方被方块封住（典型 1 格高洞口）。 */
    private static boolean frontLowGap(Player player) {
        Level level = player.level();
        BlockPos front = player.blockPosition().relative(player.getDirection());
        boolean feetFree = level.getBlockState(front).getCollisionShape(level, front).isEmpty();
        boolean headBlocked = !level.getBlockState(front.above())
                .getCollisionShape(level, front.above()).isEmpty();
        return feetFree && headBlocked;
    }

    /** 若以站立碰撞箱放置会与方块相交，说明头顶过低无法站起。 */
    private static boolean underLowCeiling(Player player) {
        return !player.level().noCollision(player,
                player.getDimensions(Pose.STANDING).makeBoundingBox(player.position()).deflate(1.0E-7));
    }
}
