package net.exmo.lotm;

import net.exmo.lotm.sequence.PassiveContext;
import net.exmo.lotm.sequence.PassiveDefinition;
import net.exmo.lotm.sequence.PassiveRegistry;
import net.exmo.lotm.sequence.PassiveTrigger;
import net.exmo.lotm.effect.LotmEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Marauder passives: short-blade hits, villager discount, and a little extra experience. */
public final class ThiefPassives {
    public static final ResourceLocation AGILE = ResourceLocation.fromNamespaceAndPath("lotm", "agile_hands");
    public static final ResourceLocation ELOQUENCE = ResourceLocation.fromNamespaceAndPath("lotm", "eloquence");
    public static final String AFTERIMAGE = "lotm_afterimage";
    public static final TagKey<Item> SHORT_BLADES = TagKey.create(BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath("lotm", "short_blades"));
    private static final List<Echo> ECHOES = new ArrayList<>();

    private ThiefPassives() {}

    public static void register() {
        PassiveRegistry.register(new PassiveDefinition(
                AGILE,
                "passive.lotm.agile_hands",
                "passive.lotm.agile_hands.desc",
                "minecraft:iron_sword",
                PassiveTrigger.ATTACK,
                0,
                ThiefPassives::shortBlade));
        PassiveRegistry.register(new PassiveDefinition(
                ELOQUENCE,
                "passive.lotm.eloquence",
                "passive.lotm.eloquence.desc",
                "minecraft:emerald",
                PassiveTrigger.TICK,
                0,
                ThiefPassives::discount));
    }

    public static void leaveAfterimage(ServerLevel level, LivingEntity caster) {
        ArmorStand stand = new ArmorStand(level, caster.getX(), caster.getY(), caster.getZ());
        stand.setYRot(caster.getYRot());
        stand.setShowArms(true);
        stand.setNoBasePlate(true);
        stand.setNoGravity(true);
        stand.setCustomName(caster.getName());
        stand.setCustomNameVisible(true);
        stand.setGlowingTag(true);
        stand.addTag(AFTERIMAGE);
        stand.getPersistentData().putString("Owner", caster.getUUID().toString());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack worn = caster.getItemBySlot(slot);
            if (!worn.isEmpty()) stand.setItemSlot(slot, worn.copy());
        }
        level.addFreshEntity(stand);
        ECHOES.add(new Echo(stand.getUUID(), level.dimension(), level.getGameTime() + 100));
    }

    private static boolean shortBlade(ServerPlayer player, PassiveContext context) {
        if (context.damageEvent() == null || !isShortBlade(player.getMainHandItem())) return false;
        context.damageEvent().setAmount(context.damageEvent().getAmount() * 1.1F);
        return true;
    }

    static boolean isShortBlade(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.is(SHORT_BLADES)) return true;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.contains("dagger") || path.contains("knife") || path.contains("dirk")
                || path.contains("stiletto") || path.contains("short_sword") || path.contains("shortsword")
                || path.contains("kukri");
    }

    private static boolean discount(ServerPlayer player, PassiveContext context) {
        MobEffectInstance current = player.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
        if (current == null || current.getDuration() < 40) {
            player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 80, 0, true, false, true));
        }
        return false;
    }

    @SubscribeEvent
    public static void experience(LivingExperienceDropEvent event) {
        if (!(event.getAttackingPlayer() instanceof ServerPlayer player)) return;
        if (!LotmSupport.hasPassive(player, ELOQUENCE)) return;
        int dropped = event.getDroppedExperience();
        event.setDroppedExperience(Math.max(dropped, (int) Math.ceil(dropped * 1.05)));
    }

    @SubscribeEvent
    public static void hurt(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (event.getAmount() > 0.0F && entity.hasEffect(LotmEffects.MIND_SCRAMBLE)) {
            entity.removeEffect(LotmEffects.MIND_SCRAMBLE);
        }
        if (!(entity instanceof ArmorStand stand) || !stand.getTags().contains(AFTERIMAGE)) return;
        event.setCanceled(true);
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity living ? living : null;
        String owner = stand.getPersistentData().getString("Owner");
        if (attacker != null && (owner.isEmpty() || !owner.equals(attacker.getUUID().toString()))) {
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        }
        stand.discard();
    }

    @SubscribeEvent
    public static void expire(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || ECHOES.isEmpty()) return;
        long now = level.getGameTime();
        Iterator<Echo> echoes = ECHOES.iterator();
        while (echoes.hasNext()) {
            Echo echo = echoes.next();
            if (!echo.dimension.equals(level.dimension())) continue;
            if (!(level.getEntity(echo.id) instanceof ArmorStand stand) || !stand.isAlive() || now >= echo.until) {
                if (level.getEntity(echo.id) instanceof ArmorStand stand) stand.discard();
                echoes.remove();
                continue;
            }
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH, stand.getX(), stand.getY() + 1.0, stand.getZ(), 2, 0.2, 0.4, 0.2, 0.0);
        }
    }

    private record Echo(java.util.UUID id, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, long until) {}
}
