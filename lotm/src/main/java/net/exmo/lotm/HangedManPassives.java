package net.exmo.lotm;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.exmo.lotm.sequence.PassiveContext;
import net.exmo.lotm.sequence.PassiveDefinition;
import net.exmo.lotm.sequence.PassiveRegistry;
import net.exmo.lotm.sequence.PassiveTrigger;
import net.exmo.lotm.effect.LotmEffects;
import net.exmo.lotm.spell.LotmSpells;
import net.exmo.lotm.spell.StealTouchSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

/** Hanged Man passives. Hidden sense reuses true sight at 16 blocks; dodge has no internal cooldown. */
public final class HangedManPassives {
    public static final ResourceLocation HIDDEN = ResourceLocation.fromNamespaceAndPath("lotm", "hidden_perception");
    public static final ResourceLocation SPIRIT = ResourceLocation.fromNamespaceAndPath("lotm", "spiritual_intuition");
    public static final double SENSE_RANGE = 16.0;

    private HangedManPassives() {}

    public static void register() {
        PassiveRegistry.register(new PassiveDefinition(
                HIDDEN,
                "passive.lotm.hidden_perception",
                "passive.lotm.hidden_perception.desc",
                "minecraft:ender_eye",
                PassiveTrigger.TICK,
                0,
                HangedManPassives::sense));
        PassiveRegistry.register(new PassiveDefinition(
                SPIRIT,
                "passive.lotm.spiritual_intuition",
                "passive.lotm.spiritual_intuition.desc",
                "minecraft:rabbit_foot",
                PassiveTrigger.HURT,
                0,
                HangedManPassives::dodge));
    }

    private static boolean sense(ServerPlayer player, PassiveContext context) {
        MobEffectInstance current = player.getEffect(LotmEffects.TRUE_SIGHT);
        if (current != null && current.getAmplifier() < 1 && current.getDuration() > 30) return false;
        if (current == null || current.getDuration() < 30) {
            player.addEffect(new MobEffectInstance(LotmEffects.TRUE_SIGHT, 40, 1, true, false, false));
        }
        return false;
    }

    private static boolean dodge(ServerPlayer player, PassiveContext context) {
        if (context.damageEvent() == null || context.damageEvent().getAmount() <= 0.0F) return false;
        if (player.getRandom().nextFloat() >= 0.10F) return false;
        context.damageEvent().setAmount(0.0F);
        context.damageEvent().setCanceled(true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.55F, 1.4F);
        return false;
    }

    @SubscribeEvent
    public static void experience(PlayerXpEvent.XpChange event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!LotmSupport.hasPassive(player, HIDDEN)) return;
        int amount = event.getAmount();
        if (amount <= 0) return;
        int bonus = amount / 10;
        if (player.getRandom().nextInt(10) < amount % 10) bonus++;
        if (bonus > 0) event.setAmount(amount + bonus);
    }

    @SubscribeEvent
    public static void rightClick(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if (!(event.getTarget() instanceof LivingEntity target) || target == player) return;
        if (!LotmSupport.hasPassive(player, HIDDEN) && !ownsSteal(player)) return;
        if (!ownsSteal(player)) return;
        if (reserved(target) && !player.isShiftKeyDown()) return;
        AbstractSpell spell = LotmSpells.STEAL_TOUCH.get();
        MagicData magic = MagicData.getPlayerMagicData(player);
        if (magic.getPlayerCooldowns().isOnCooldown(spell)) return;
        int cost = spell.getManaCost(1);
        if (magic.getMana() < cost) {
            player.displayClientMessage(Component.translatable("spell.lotm.steal_touch.no_mana"), true);
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) return;
        if (StealTouchSpell.beneficial(target).isEmpty()) {
            player.displayClientMessage(Component.translatable("spell.lotm.steal_touch.empty"), true);
            return;
        }
        if (StealTouchSpell.steal(level, player, target) == null) return;
        magic.setMana(magic.getMana() - cost);
        magic.getPlayerCooldowns().addCooldown(spell, spell.getSpellCooldown());
        magic.getPlayerCooldowns().syncToPlayer(player);
        player.swing(InteractionHand.MAIN_HAND, true);
        event.setCanceled(true);
    }

    private static boolean ownsSteal(ServerPlayer player) {
        return net.exmo.lotm.sequence.SequenceService.ownsSpell(player, LotmSpells.STEAL_TOUCH.getId());
    }

    private static boolean reserved(LivingEntity target) {
        return target instanceof AbstractVillager || target instanceof AbstractHorse || target instanceof TamableAnimal;
    }
}
