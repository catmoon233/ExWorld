package net.exmo.lotm.curios;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;

/** Combat triggers. Item cooldowns are per item type and are never written onto the equipped stack. */
public final class CurioEvents {
    private static final int CROSS_COOLDOWN = 20 * 120;
    private static final int COIN_COOLDOWN = 20 * 5;
    private static final int BRAND_COOLDOWN = 20 * 4;
    private static final int GLOVES_COOLDOWN = 20 * 3;
    private static final int MANTLE_COOLDOWN = 20 * 30;

    private CurioEvents() {}

    @SubscribeEvent
    public static void beforeHealthLoss(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity instanceof Player player) || event.getNewDamage() <= 0f) return;
        foolCoin(event, player);
        holyCross(event, player);
    }

    @SubscribeEvent
    public static void afterHealthLoss(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide() || event.getNewDamage() <= 0f) return;
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof Player player && event.getEntity() != player) {
            brand(player, event.getEntity());
            gloves(player, event.getEntity());
        }
        if (event.getEntity() instanceof Player player) duskMantle(player);
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player) || player.level().isClientSide()) return;
        if (event.getEntity() == player || !equipped(player, LotmCurios.DEATH_KNELL_PENDANT.get())) return;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0, false, true, true));
    }

    private static void foolCoin(LivingDamageEvent.Pre event, Player player) {
        Item coin = LotmCurios.FOOL_SILVER_COIN.get();
        if (player.getCooldowns().isOnCooldown(coin) || !equipped(player, coin)) return;
        if (player.getRandom().nextFloat() >= 0.20f) return;
        event.setNewDamage(event.getNewDamage() * 0.5f);
        player.getCooldowns().addCooldown(coin, COIN_COOLDOWN);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.4f, 1.4f);
    }

    private static void holyCross(LivingDamageEvent.Pre event, Player player) {
        Item cross = LotmCurios.HOLY_SPIRIT_CROSS.get();
        if (player.getCooldowns().isOnCooldown(cross)) return;
        if (event.getNewDamage() < player.getHealth() + player.getAbsorptionAmount()) return;
        var inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) return;
        SlotResult chosen = null;
        for (SlotResult result : inventory.findCurios(cross)) {
            if (result.slotContext().cosmetic()) continue;
            ItemStack stack = result.stack();
            if (!stack.isEmpty() && stack.getDamageValue() < stack.getMaxDamage()) {
                chosen = result;
                break;
            }
        }
        if (chosen == null) return;
        event.setNewDamage(0f);
        player.getCooldowns().addCooldown(cross, CROSS_COOLDOWN);
        SlotContext slot = chosen.slotContext();
        if (player.level() instanceof ServerLevel level) {
            chosen.stack().hurtAndBreak(1, level, player, item -> CuriosApi.broadcastCurioBreakEvent(slot));
            inventory.setEquippedCurio(slot.identifier(), slot.index(), chosen.stack());
        }
        player.invulnerableTime = Math.max(player.invulnerableTime, 20);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.45f, 1.6f);
        player.displayClientMessage(Component.translatable("item.lotm.holy_spirit_cross.saved"), true);
    }

    private static void brand(Player player, LivingEntity victim) {
        Item brand = LotmCurios.RED_PRIEST_BRAND.get();
        if (player.getCooldowns().isOnCooldown(brand) || !equipped(player, brand)) return;
        victim.igniteForSeconds(3f);
        player.getCooldowns().addCooldown(brand, BRAND_COOLDOWN);
    }

    private static void gloves(Player player, LivingEntity victim) {
        Item gloves = LotmCurios.THIEF_GLOVES.get();
        if (player.getCooldowns().isOnCooldown(gloves) || !equipped(player, gloves)) return;
        if (player.getRandom().nextFloat() >= 0.25f) return;
        victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, true, true));
        player.getCooldowns().addCooldown(gloves, GLOVES_COOLDOWN);
    }

    private static void duskMantle(Player player) {
        Item mantle = LotmCurios.DUSK_MANTLE.get();
        if (!player.isAlive() || player.getHealth() > player.getMaxHealth() * 0.3f) return;
        if (player.getCooldowns().isOnCooldown(mantle) || !equipped(player, mantle)) return;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0, false, true, true));
        player.getCooldowns().addCooldown(mantle, MANTLE_COOLDOWN);
    }

    private static boolean equipped(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity)
                .map(inventory -> inventory.findFirstCurio(item).isPresent())
                .orElse(false);
    }
}
