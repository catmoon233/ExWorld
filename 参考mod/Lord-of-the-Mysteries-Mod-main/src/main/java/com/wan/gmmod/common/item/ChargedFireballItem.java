package com.wan.gmmod.common.item;

import com.wan.gmmod.content.war.FireballAbility;
import com.wan.gmmod.content.war.GiantFireballAbility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 凝聚火球——纵火家（战争之红途径 · 序列 7）「火球术」「巨大火球」蓄力时握在手中的凝聚态。
 * <p>
 * 触发能力开始蓄力时由能力发放到主手（手臂上即显示凝聚纹理），右键掷出：
 * <ul>
 *   <li>普通凝聚火球：短蓄赤红 / 蓄满炽白（{@link FireballAbility#release}）；</li>
 *   <li>巨大凝聚火球：蓄力达标后掷出超大火球（{@link GiantFireballAbility#release}）。</li>
 * </ul>
 * 物品与蓄力附件严格同步（丢弃即取消、超窗自动消散），由
 * {@code WarPathwayManager#syncChargedOrbs} 每刻维护，无法囤积或复制。
 */
public class ChargedFireballItem extends Item {

    /** 是否为巨大火球凝聚态。 */
    private final boolean giant;

    public ChargedFireballItem(Properties properties, boolean giant) {
        super(properties.fireResistant());
        this.giant = giant;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (giant) {
                GiantFireballAbility.release(sp);
            } else {
                FireballAbility.release(sp);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
