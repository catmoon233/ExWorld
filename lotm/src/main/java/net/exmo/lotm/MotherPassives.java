package net.exmo.lotm;

import net.exmo.exworld.battle.BattleSystem;
import net.exmo.lotm.sequence.PassiveContext;
import net.exmo.lotm.sequence.PassiveDefinition;
import net.exmo.lotm.sequence.PassiveRegistry;
import net.exmo.lotm.sequence.PassiveTrigger;
import net.exmo.lotm.sequence.SequenceService;
import net.exmo.lotm.sequence.SequenceSkill;
import net.exmo.lotm.sequence.SkillKind;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Great Mother passives. Potion duration and healing are event-driven; earth regen uses the tick trigger. */
public final class MotherPassives {
    public static final ResourceLocation EARTH = ResourceLocation.fromNamespaceAndPath("lotm", "earth_power");
    public static final ResourceLocation AFFINITY = ResourceLocation.fromNamespaceAndPath("lotm", "potion_affinity");
    private static final Map<UUID, List<MobEffectInstance>> PENDING = new HashMap<>();
    private static boolean extending;

    private MotherPassives() {}

    public static void register() {
        PassiveRegistry.register(new PassiveDefinition(
                EARTH,
                "passive.lotm.earth_power",
                "passive.lotm.earth_power.desc",
                "minecraft:grass_block",
                PassiveTrigger.TICK,
                0,
                MotherPassives::earth));
        PassiveRegistry.register(new PassiveDefinition(
                AFFINITY,
                "passive.lotm.potion_affinity",
                "passive.lotm.potion_affinity.desc",
                "minecraft:potion",
                PassiveTrigger.TICK,
                0,
                (player, context) -> false));
    }

    private static boolean earth(ServerPlayer player, PassiveContext context) {
        if (player.getHealth() >= player.getMaxHealth()) return false;
        if (BattleSystem.isParticipating(player.getUUID())) return false;
        BlockPos feet = player.blockPosition();
        if (!soil(player.level().getBlockState(feet)) && !soil(player.level().getBlockState(feet.below()))) return false;
        player.heal(0.5F);
        return false;
    }

    private static boolean soil(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.FARMLAND)
                || state.is(Blocks.DIRT_PATH)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.MUD);
    }

    @SubscribeEvent
    public static void extendPotions(MobEffectEvent.Added event) {
        if (extending) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!has(player, AFFINITY) || BattleSystem.isParticipating(player.getUUID())) return;
        MobEffectInstance instance = event.getEffectInstance();
        int duration = instance.getDuration();
        if (duration <= 1 || instance.isInfiniteDuration()) return;
        int bonus = Math.max(1, (int) Math.round(duration * 0.2));
        PENDING.computeIfAbsent(player.getUUID(), key -> new ArrayList<>()).add(new MobEffectInstance(
                instance.getEffect(),
                duration + bonus,
                instance.getAmplifier(),
                instance.isAmbient(),
                instance.isVisible(),
                instance.showIcon()));
    }

    @SubscribeEvent
    public static void flush(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        List<MobEffectInstance> pending = PENDING.remove(player.getUUID());
        if (pending == null || pending.isEmpty()) return;
        extending = true;
        try {
            for (MobEffectInstance instance : pending) player.addEffect(instance);
        } finally {
            extending = false;
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void heal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) return;
        if (!has(player, AFFINITY) || BattleSystem.isParticipating(player.getUUID())) return;
        event.setAmount(event.getAmount() * 1.1F);
    }

    private static boolean has(ServerPlayer player, ResourceLocation passive) {
        for (SequenceSkill skill : SequenceService.unlockedSkills(player)) {
            if (skill.kind() == SkillKind.PASSIVE && passive.equals(skill.ref())) return true;
        }
        return false;
    }
}
