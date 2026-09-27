package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.registry.ModItems;
import com.wan.gmmod.content.entities.FlyingCardEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 小丑「飞牌」——序列 8 主动能力，支持双发射模式（模式存于
 * {@link ModAttachments#CARD_SCATTER_MODE}，按切换键发包翻转）。
 * <ul>
 *   <li>精准单点（默认）：消耗 1 张纸/纸牌，发射一枚高速飞牌（伤害 3，可爆头）；</li>
 *   <li>散射：一次消耗 3~5 张（序列 ≤6 为 5 张），30 度扇形发射多张纸牌
 *       （单张伤害 2 + 切割流血，射程缩短约 20%）。</li>
 * </ul>
 * 消耗时优先使用纸牌（{@link ModItems#PAPER_CARD}），其次原版纸；
 * 也可手持纸/纸牌右键直接触发（见 {@link com.wan.gmmod.common.event.AbilityEventSubscriber}）。
 */
public class FlyingCardAbility extends Ability {
    /** 散射扇形总角度（度） */
    private static final float SCATTER_ANGLE = 30.0F;

    public FlyingCardAbility() {
        // 消耗 1 灵性，冷却 10 刻（0.5 秒），主动能力
        super(GuimiMod.id("flying_card"), 1, 10, true);
    }

    @Override
    public void onActivate(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        boolean scatter = player.getData(ModAttachments.CARD_SCATTER_MODE);
        // 散射消耗量随等级：序列 8/7 消耗 3 张，序列 6 及更高（数值更小）消耗 5 张、多发 5 张
        int sequence = player.getData(ModAttachments.SEQUENCE_LEVEL);
        int count = scatter ? (sequence > 0 && sequence <= 6 ? 5 : 3) : 1;

        if (!consumePaper(player, count)) {
            player.displayClientMessage(Component.translatable("ability.guimi_mod.flying_card.no_paper"), true);
            return;
        }

        if (scatter) {
            // 扇形均匀分布 count 张纸牌，速度略低、散布明显
            float step = SCATTER_ANGLE / (count - 1);
            float start = -SCATTER_ANGLE / 2.0F;
            for (int i = 0; i < count; i++) {
                FlyingCardEntity card = new FlyingCardEntity(player.level(), player);
                card.setScatter(true);
                card.shootFromRotation(player, player.getXRot(),
                        player.getYRot() + start + step * i, 0.0F, 2.4F, 1.5F);
                player.level().addFreshEntity(card);
            }
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 1.0F, 1.1F);
        } else {
            // 精准单点：高速、笔直
            FlyingCardEntity card = new FlyingCardEntity(player.level(), player);
            card.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 0.3F);
            player.level().addFreshEntity(card);
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8F, 1.5F);
        }
    }

    /** 从背包消耗 {@code count} 张纸类弹药，优先纸牌、其次原版纸；不足时不消耗并返回 false。 */
    private static boolean consumePaper(Player player, int count) {
        if (player.isCreative()) {
            return true;
        }
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.PAPER_CARD.get()) || stack.is(Items.PAPER)) {
                total += stack.getCount();
            }
        }
        if (total < count) {
            return false;
        }
        int remaining = count;
        // 第一轮：优先消耗纸牌；第二轮：消耗原版纸
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                boolean match = pass == 0 ? stack.is(ModItems.PAPER_CARD.get()) : stack.is(Items.PAPER);
                if (match) {
                    int take = Math.min(remaining, stack.getCount());
                    stack.shrink(take);
                    remaining -= take;
                }
            }
        }
        return true;
    }
}
