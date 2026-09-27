package com.wan.gmmod.common.item;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.abilities.AbilityTargeting;
import com.wan.gmmod.content.abilities.SkillManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * 火焰武器——纵火家（战争之红途径 · 序列 7）「火焰武器」凝聚的临时武器。
 * <p>
 * 基础形态为火焰剑（伤害 6~8，命中点燃目标 4 秒），持续 60 秒后自动消散
 * （由 WarPathwayManager 依据 {@code FLAME_WEAPON_END} 附件清理）。
 * <p>
 * 阴谋家（序列 6）「火焰塑形」被动解锁后可潜行右键切换形态：
 * <ul>
 *   <li>剑：基础形态；</li>
 *   <li>鞭：右键可鞭击 8 米内视线目标（射程 +5 米）；</li>
 *   <li>马刀：近战伤害 +2。</li>
 * </ul>
 */
public class FlameWeaponItem extends SwordItem {

    /** 形态：0=剑，1=鞭，2=马刀。存于 CUSTOM_DATA 的该键下。 */
    public static final String FORM_KEY = "gm_flame_form";
    public static final int FORM_SWORD = 0;
    public static final int FORM_WHIP = 1;
    public static final int FORM_SABER = 2;

    /** 鞭形态的攻击射程（近战 3 米 + 塑形 5 米）。 */
    private static final double WHIP_RANGE = 8.0D;

    public FlameWeaponItem(Properties properties) {
        super(Tiers.IRON, properties
                .attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))
                .fireResistant());
    }

    /** 读取当前形态。 */
    public static int getForm(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? FORM_SWORD : data.copyTag().getInt(FORM_KEY);
    }

    /** 写入形态。 */
    public static void setForm(ItemStack stack, int form) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(FORM_KEY, form));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // 火焰附加：命中点燃目标 4 秒
        target.igniteForSeconds(4);
        // 马刀形态：额外 +2 伤害
        if (getForm(stack) == FORM_SABER && attacker instanceof Player player) {
            target.hurt(player.damageSources().playerAttack(player), 2.0F);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean shapingUnlocked = SkillManager.isUnlocked(player, GuimiMod.id("flame_shaping"));

        // 潜行右键：切换形态（需已解锁「火焰塑形」），无冷却
        if (player.isShiftKeyDown()) {
            if (!shapingUnlocked) {
                return InteractionResultHolder.pass(stack);
            }
            if (!level.isClientSide) {
                int next = (getForm(stack) + 1) % 3;
                setForm(stack, next);
                String formKey = switch (next) {
                    case FORM_WHIP -> "message.guimi_mod.flame_form_whip";
                    case FORM_SABER -> "message.guimi_mod.flame_form_saber";
                    default -> "message.guimi_mod.flame_form_sword";
                };
                player.sendSystemMessage(Component.translatable(formKey));
                level.playSound(null, player.blockPosition(),
                        SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6F, 1.5F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        // 鞭形态右键：鞭击 8 米内视线目标
        if (getForm(stack) == FORM_WHIP) {
            if (!level.isClientSide) {
                LivingEntity target = AbilityTargeting.pickLivingEntity(player, WHIP_RANGE);
                if (target != null) {
                    target.hurt(player.damageSources().playerAttack(player), 7.0F);
                    target.igniteForSeconds(4);
                    level.playSound(null, target.blockPosition(),
                            SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8F, 1.3F);
                    player.getCooldowns().addCooldown(this, 10);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }
}
