package net.exmo.lotm;

import net.exmo.lotm.sequence.SequenceService;
import net.exmo.lotm.sequence.SequenceSkill;
import net.exmo.lotm.sequence.SkillKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.WeakHashMap;

/** Timed taunt, instigation, stealth and charm. Registered once from {@link Lotm}. */
public final class LotmRuntime {
    public static final ResourceLocation SHADOW = ResourceLocation.fromNamespaceAndPath("lotm", "shadow_affinity");
    public static final ResourceLocation CHARM = ResourceLocation.fromNamespaceAndPath("lotm", "charm");
    private static final ResourceLocation TAUNT_ARMOR = ResourceLocation.fromNamespaceAndPath("lotm", "taunt_shred");
    private static final ResourceLocation SHADOW_SPEED = ResourceLocation.fromNamespaceAndPath("lotm", "shadow_step");
    private static final AttributeModifier SHADOW_MODIFIER =
            new AttributeModifier(SHADOW_SPEED, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    private static final AttributeModifier ARMOR_SHRED =
            new AttributeModifier(TAUNT_ARMOR, -4.0, AttributeModifier.Operation.ADD_VALUE);
    private static final int TAUNT_TICKS = 100;
    private static final int INSTIGATE_TICKS = 100;
    public static final int STEALTH_TICKS = 240;

    private static final List<Forced> FORCED = new ArrayList<>();
    private static final List<Shred> SHREDS = new ArrayList<>();
    private static final List<UUID> STEALTH = new ArrayList<>();
    private static final WeakHashMap<MerchantOffer, CharmMark> CHARM_MARKS = new WeakHashMap<>();
    private static boolean registered;

    private LotmRuntime() {}

    public static void register() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.register(LotmRuntime.class);
    }

    public static void taunt(LivingEntity caster, LivingEntity target) {
        if (!(caster.level() instanceof ServerLevel level)) return;
        force(level, target, caster.getUUID(), Kind.TAUNT, TAUNT_TICKS);
        shred(level, target, TAUNT_TICKS);
        pull(target, caster);
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, target.getX(), target.getEyeY(), target.getZ(), 6, 0.3, 0.2, 0.3, 0.0);
    }

    public static void instigate(LivingEntity caster, LivingEntity target) {
        if (!(caster.level() instanceof ServerLevel level)) return;
        if (target instanceof Player) {
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, INSTIGATE_TICKS, 0));
        }
        force(level, target, caster.getUUID(), Kind.INSTIGATE, INSTIGATE_TICKS);
        level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getEyeY(), target.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
    }

    public static void stealth(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, STEALTH_TICKS, 0, false, false, true));
        if (!STEALTH.contains(player.getUUID())) STEALTH.add(player.getUUID());
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            HunterTraps.tick(level);
            tickForced(level);
            tickShreds(level);
        }
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        shadowSpeed(player);
        if (STEALTH.contains(player.getUUID()) && !player.hasEffect(MobEffects.INVISIBILITY)) {
            STEALTH.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void hurt(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            softenFall(player, event);
            if (event.getAmount() > 0.0F) breakStealth(player);
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && (event.getSource().is(DamageTypeTags.IS_PLAYER_ATTACK) || event.getSource().is(DamageTypeTags.IS_PROJECTILE))) {
            breakStealth(attacker);
        }
    }

    @SubscribeEvent
    public static void attack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) breakStealth(player);
    }

    @SubscribeEvent
    public static void openTrade(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getContainer() instanceof MerchantMenu menu)) return;
        if (!owns(player, CHARM)) return;
        for (MerchantOffer offer : menu.getOffers()) applyCharm(offer);
    }

    @SubscribeEvent
    public static void closeTrade(PlayerContainerEvent.Close event) {
        if (!(event.getContainer() instanceof MerchantMenu menu)) return;
        for (MerchantOffer offer : menu.getOffers()) revertCharm(offer);
    }

    private static void applyCharm(MerchantOffer offer) {
        CharmMark mark = CHARM_MARKS.get(offer);
        if (mark != null && offer.getSpecialPriceDiff() == mark.specialAfter()) return;
        int delta = charmDelta(offer);
        if (delta == 0) return;
        offer.addToSpecialPriceDiff(delta);
        CHARM_MARKS.put(offer, new CharmMark(delta, offer.getSpecialPriceDiff()));
    }

    private static void revertCharm(MerchantOffer offer) {
        CharmMark mark = CHARM_MARKS.remove(offer);
        if (mark != null && offer.getSpecialPriceDiff() == mark.specialAfter()) {
            offer.addToSpecialPriceDiff(-mark.applied());
        }
    }

    static int charmDelta(MerchantOffer offer) {
        int base = Math.max(0, offer.getBaseCostA().getCount());
        int discount = Math.max(1, base / 5);
        int room = offer.getCostA().getCount() - 1;
        if (room <= 0) return 0;
        return -Math.min(discount, room);
    }

    private static void softenFall(ServerPlayer player, LivingIncomingDamageEvent event) {
        if (!owns(player, SHADOW) || !event.getSource().is(DamageTypeTags.IS_FALL)) return;
        event.setAmount(event.getAmount() * 0.5F);
    }

    private static void shadowSpeed(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        boolean active = owns(player, SHADOW) && player.isShiftKeyDown();
        if (active) speed.addOrUpdateTransientModifier(SHADOW_MODIFIER);
        else speed.removeModifier(SHADOW_SPEED);
    }

    private static void breakStealth(ServerPlayer player) {
        if (!STEALTH.remove(player.getUUID())) return;
        player.removeEffect(MobEffects.INVISIBILITY);
    }

    private static void force(ServerLevel level, LivingEntity target, UUID caster, Kind kind, int ticks) {
        long until = level.getGameTime() + ticks;
        FORCED.removeIf(forced -> forced.entity.equals(target.getUUID()) && forced.kind == kind);
        FORCED.add(new Forced(level.dimension(), target.getUUID(), caster, kind, until, 0L));
        if (kind == Kind.TAUNT) {
            Player player = level.getServer().getPlayerList().getPlayer(caster);
            if (player != null) pull(target, player);
        }
    }

    private static void shred(ServerLevel level, LivingEntity target, int ticks) {
        AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.addOrUpdateTransientModifier(ARMOR_SHRED);
        long until = level.getGameTime() + ticks;
        SHREDS.removeIf(shred -> shred.entity.equals(target.getUUID()));
        SHREDS.add(new Shred(level.dimension(), target.getUUID(), until));
    }

    private static void tickForced(ServerLevel level) {
        long now = level.getGameTime();
        Iterator<Forced> forced = FORCED.iterator();
        while (forced.hasNext()) {
            Forced effect = forced.next();
            if (!effect.dimension.equals(level.dimension())) continue;
            if (now >= effect.until || !(level.getEntity(effect.entity) instanceof LivingEntity target) || !target.isAlive()) {
                forced.remove();
                continue;
            }
            if (now < effect.nextPulse) continue;
            effect.nextPulse = now + (effect.kind == Kind.TAUNT ? 5L : 20L);
            if (effect.kind == Kind.TAUNT) {
                Player caster = level.getServer().getPlayerList().getPlayer(effect.focus);
                if (caster != null) pull(target, caster);
            } else {
                swingAtRandom(level, target);
            }
        }
    }

    private static void tickShreds(ServerLevel level) {
        long now = level.getGameTime();
        Iterator<Shred> shreds = SHREDS.iterator();
        while (shreds.hasNext()) {
            Shred shred = shreds.next();
            if (!shred.dimension.equals(level.dimension())) continue;
            if (now < shred.until) continue;
            if (level.getEntity(shred.entity) instanceof LivingEntity living) {
                AttributeInstance armor = living.getAttribute(Attributes.ARMOR);
                if (armor != null) armor.removeModifier(TAUNT_ARMOR);
            }
            shreds.remove();
        }
    }

    private static void pull(LivingEntity target, LivingEntity caster) {
        target.setLastHurtByMob(caster);
        if (target instanceof Mob mob) {
            mob.setTarget(caster);
            mob.setAggressive(true);
        }
        if (target instanceof NeutralMob neutral && caster instanceof Player player) {
            neutral.setPersistentAngerTarget(player.getUUID());
            neutral.setRemainingPersistentAngerTime(TAUNT_TICKS);
        }
    }

    private static void swingAtRandom(ServerLevel level, LivingEntity target) {
        AABB box = target.getBoundingBox().inflate(8.0);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, box,
                living -> living != target && living.isAlive() && !living.isSpectator());
        if (nearby.isEmpty()) return;
        LivingEntity victim = nearby.get(level.random.nextInt(nearby.size()));
        pull(target, victim);
        if (target instanceof Mob mob && target.distanceToSqr(victim) <= 9.0) mob.doHurtTarget(victim);
        level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getEyeY(), target.getZ(), 4, 0.2, 0.2, 0.2, 0.01);
    }

    static boolean owns(Player player, ResourceLocation passiveId) {
        if (player == null || passiveId == null) return false;
        for (SequenceSkill skill : SequenceService.unlockedSkills(player)) {
            if (skill.kind() == SkillKind.PASSIVE && passiveId.equals(skill.ref())) return true;
        }
        return false;
    }

    private enum Kind { TAUNT, INSTIGATE }

    private static final class Forced {
        private final ResourceKey<Level> dimension;
        private final UUID entity;
        private final UUID focus;
        private final Kind kind;
        private final long until;
        private long nextPulse;

        private Forced(ResourceKey<Level> dimension, UUID entity, UUID focus, Kind kind, long until, long nextPulse) {
            this.dimension = dimension;
            this.entity = entity;
            this.focus = focus;
            this.kind = kind;
            this.until = until;
            this.nextPulse = nextPulse;
        }
    }

    private record Shred(ResourceKey<Level> dimension, UUID entity, long until) {}

    private record CharmMark(int applied, int specialAfter) {}
}
