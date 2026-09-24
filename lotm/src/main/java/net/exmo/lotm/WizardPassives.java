package net.exmo.lotm;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import net.exmo.lotm.sequence.PassiveDefinition;
import net.exmo.lotm.sequence.PassiveRegistry;
import net.exmo.lotm.sequence.PassiveTrigger;
import net.exmo.lotm.effect.LotmEffects;
import net.exmo.lotm.spell.KnowledgeStrikeSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

/** Wizard passives. Mana discount, analyzed strikes, and the block riposte live on events. */
public final class WizardPassives {
    public static final ResourceLocation LORE = ResourceLocation.fromNamespaceAndPath("lotm", "mystic_lore");
    public static final ResourceLocation ANALYSIS = ResourceLocation.fromNamespaceAndPath("lotm", "combat_analysis");
    public static final String RIPOSTE_FLAG = "lotm_riposte";
    public static final float RIPOSTE_BONUS = 1.15F;

    private WizardPassives() {}

    public static void register() {
        PassiveRegistry.register(new PassiveDefinition(
                LORE,
                "passive.lotm.mystic_lore",
                "passive.lotm.mystic_lore.desc",
                "minecraft:book",
                PassiveTrigger.TICK,
                0,
                (player, context) -> false));
        PassiveRegistry.register(new PassiveDefinition(
                ANALYSIS,
                "passive.lotm.combat_analysis",
                "passive.lotm.combat_analysis.desc",
                "minecraft:iron_sword",
                PassiveTrigger.TICK,
                0,
                (player, context) -> false));
    }

    @SubscribeEvent
    public static void cheaper(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!LotmSupport.hasPassive(player, LORE)) return;
        int cost = event.getManaCost();
        if (cost <= 1) return;
        event.setManaCost(Math.max(1, (int) Math.floor(cost * 0.95)));
    }

    @SubscribeEvent
    public static void blocked(LivingShieldBlockEvent event) {
        if (!event.getBlocked() || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (!LotmSupport.hasPassive(player, ANALYSIS)) return;
        player.getPersistentData().putBoolean(RIPOSTE_FLAG, true);
    }

    @SubscribeEvent
    public static void analyzedHit(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (event.getEntity() == player || !event.getSource().is(DamageTypes.PLAYER_ATTACK)) return;
        float amount = event.getAmount();
        boolean changed = false;
        if (player.hasEffect(LotmEffects.KNOWLEDGE_STRIKE)) {
            amount *= KnowledgeStrikeSpell.CRIT;
            event.getContainer().addModifier(DamageContainer.Reduction.ARMOR,
                    (container, reduction) -> reduction * KnowledgeStrikeSpell.ARMOR_KEPT);
            player.removeEffect(LotmEffects.KNOWLEDGE_STRIKE);
            if (event.getEntity() instanceof LivingEntity living) player.crit(living);
            changed = true;
        }
        if (player.getPersistentData().getBoolean(RIPOSTE_FLAG)) {
            if (LotmSupport.hasPassive(player, ANALYSIS)) {
                amount *= RIPOSTE_BONUS;
                changed = true;
            }
            player.getPersistentData().remove(RIPOSTE_FLAG);
        }
        if (changed) event.setAmount(amount);
    }
}
