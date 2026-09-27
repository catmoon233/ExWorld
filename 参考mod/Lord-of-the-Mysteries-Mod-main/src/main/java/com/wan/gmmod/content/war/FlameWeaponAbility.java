package com.wan.gmmod.content.war;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.registry.ModItems;
import com.wan.gmmod.content.abilities.Ability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 「火焰武器」——纵火家（战争之红途径 · 序列 7）主动。
 * <p>
 * 凝聚火焰成临时武器（伤害 6~8，附带火焰附加），持续 60 秒后由
 * {@code WarPathwayManager} 依据 {@code FLAME_WEAPON_END} 从背包移除。冷却 20 秒。
 */
public class FlameWeaponAbility extends Ability {
    /** 持续 60 秒。 */
    public static final int DURATION = 60 * 20;

    public FlameWeaponAbility() {
        super(GuimiMod.id("flame_weapon"), 10, 20 * 20, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack weapon = new ItemStack(ModItems.FLAME_WEAPON.get());
        if (!sp.getInventory().add(weapon)) {
            sp.drop(weapon, false);
        }
        sp.setData(ModAttachments.FLAME_WEAPON_END, level.getGameTime() + DURATION);
        level.playSound(null, sp.blockPosition(),
                SoundEvents.BLAZE_AMBIENT, SoundSource.PLAYERS, 0.8F, 1.4F);
        sp.displayClientMessage(Component.translatable("message.guimi_mod.flame_weapon.on"), true);
    }
}
