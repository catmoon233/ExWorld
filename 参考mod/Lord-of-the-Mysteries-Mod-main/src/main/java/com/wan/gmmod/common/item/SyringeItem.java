package com.wan.gmmod.common.item;

import com.wan.gmmod.common.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/**
 * 针管：可采集血液的采血器具。
 * <ul>
 *   <li>对准有对应血液 / 脊髓液材料的生物右键 → 采集该生物的血液；</li>
 *   <li>对人形生物（村民 / 伊利法师 / 玩家）右键 → 采集「他人血液」；</li>
 *   <li>对空右键（没有生物）→ 抽取「自身血液」。</li>
 * </ul>
 * 针管可重复使用，采集后有 1 秒冷却。
 */
public class SyringeItem extends Item {

    /** 采集冷却（刻）。 */
    private static final int COOLDOWN_TICKS = 20;

    public SyringeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // 对空右键：抽取自身血液
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide()) {
            giveBlood(level, player, ModItems.ZI_SHEN_XUE_YE::get, "message.guimi_mod.syringe.self");
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                  LivingEntity target, InteractionHand hand) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.FAIL;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        // 人形生物（村民 / 伊利法师 / 其他玩家）→ 他人血液
        if (target instanceof AbstractVillager || target instanceof AbstractIllager
                || (target instanceof Player && target != player)) {
            giveBlood(level, player, ModItems.TA_REN_XUE_YE::get, "message.guimi_mod.syringe.other");
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            return InteractionResult.SUCCESS;
        }
        // 有对应血液 / 脊髓液材料的生物 → 采集其血液
        Supplier<? extends Item> blood = bloodOf(target);
        if (blood != null) {
            giveBlood(level, player, blood, "message.guimi_mod.syringe.mob");
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            return InteractionResult.SUCCESS;
        }
        player.displayClientMessage(Component.translatable("message.guimi_mod.syringe.no_blood"), true);
        return InteractionResult.FAIL;
    }

    /** 生物 → 血液 / 脊髓液材料映射；无对应材料返回 {@code null}。 */
    private Supplier<? extends Item> bloodOf(Entity entity) {
        if (entity instanceof com.wan.gmmod.content.entities.SilverWarBearEntity) {
            return ModItems.MAT_198::get;
        }
        if (entity instanceof com.wan.gmmod.content.entities.SkinlessBloodCatEntity) {
            return ModItems.MAT_062::get;
        }
        if (entity instanceof com.wan.gmmod.content.entities.AdultUnicornEntity) {
            return ModItems.MAT_042::get;
        }
        if (entity instanceof com.wan.gmmod.content.entities.AdultPegasusEntity) {
            return ModItems.MAT_055::get;
        }
        if (entity instanceof com.wan.gmmod.content.entities.DawnRoosterEntity) {
            return ModItems.MAT_239::get;
        }
        if (entity instanceof com.wan.gmmod.content.entities.WolfmanEntity) {
            return ModItems.MAT_122::get;
        }
        if (entity instanceof com.wan.gmmod.content.entities.WhiteFoxEntity) {
            return ModItems.MAT_126::get;
        }
        if (entity instanceof com.wan.gmmod.content.entities.FireSalamanderEntity) {
            return ModItems.MAT_100::get;
        }
        if (entity instanceof com.wan.gmmod.content.entities.OneEyedBullEntity) {
            return ModItems.MAT_120::get;
        }
        if (entity instanceof com.wan.gmmod.content.entities.ThousandFacedHunterEntity) {
            return ModItems.THOUSAND_FACED_HUNTER_BLOOD::get;
        }
        if (entity instanceof com.wan.gmmod.content.entities.LavaDemonEntity) {
            return ModItems.MAT_028::get;
        }
        if (entity instanceof com.wan.gmmod.content.entities.NightmareEyeEntity) {
            return ModItems.MAT_012::get;
        }
        // 原版鱿鱼 → 拉瓦章鱼血液
        if (entity instanceof net.minecraft.world.entity.animal.Squid) {
            return ModItems.LAVA_OCTOPUS_BLOOD::get;
        }
        return null;
    }

    /** 发放血液物品（背包满则掉落在玩家脚下）并播放采集音效。 */
    private void giveBlood(Level level, Player player, Supplier<? extends Item> blood, String messageKey) {
        ItemStack bloodStack = new ItemStack(blood.get());
        if (!player.getInventory().add(bloodStack)) {
            player.drop(bloodStack, false);
        }
        level.playSound(null, player.blockPosition(),
                SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.8F, 1.4F);
        player.displayClientMessage(Component.translatable(messageKey,
                bloodStack.getHoverName()), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.guimi_mod.syringe.usage"));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
