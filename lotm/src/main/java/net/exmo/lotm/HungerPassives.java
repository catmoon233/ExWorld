package net.exmo.lotm;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.exmo.lotm.sequence.PassiveDefinition;
import net.exmo.lotm.sequence.PassiveRegistry;
import net.exmo.lotm.sequence.PassiveTrigger;
import net.exmo.lotm.sequence.SequenceService;
import net.exmo.lotm.spell.DevourSpell;
import net.exmo.lotm.spell.LotmSpells;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Primordial Hunger passives and the right-click devour cast. */
public final class HungerPassives {
    public static final ResourceLocation ADAPT = ResourceLocation.fromNamespaceAndPath("lotm", "environment_adapt");
    public static final ResourceLocation TOXIN = ResourceLocation.fromNamespaceAndPath("lotm", "toxin_resist");

    private HungerPassives() {}

    public static void register() {
        PassiveRegistry.register(new PassiveDefinition(
                ADAPT,
                "passive.lotm.environment_adapt",
                "passive.lotm.environment_adapt.desc",
                "minecraft:leather_boots",
                PassiveTrigger.TICK,
                0,
                (player, context) -> false));
        PassiveRegistry.register(new PassiveDefinition(
                TOXIN,
                "passive.lotm.toxin_resist",
                "passive.lotm.toxin_resist.desc",
                "minecraft:milk_bucket",
                PassiveTrigger.TICK,
                0,
                (player, context) -> false));
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) return;
        if (LotmSupport.hasPassive(player, TOXIN)) {
            player.removeEffect(MobEffects.POISON);
            player.removeEffect(MobEffects.HUNGER);
        }
        if (player.isCreative() || !LotmSupport.hasPassive(player, ADAPT) || !harsh(player)) return;
        player.getFoodData().setExhaustion(0.0F);
    }

    @SubscribeEvent
    public static void toxin(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!LotmSupport.hasPassive(player, TOXIN)) return;
        var effect = event.getEffectInstance().getEffect();
        if (effect == MobEffects.POISON || effect == MobEffects.HUNGER) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void devour(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) return;
        ItemStack stack = event.getItemStack();
        if (stack.get(DataComponents.FOOD) == null) return;
        if (!SequenceService.ownsSpell(player, LotmSpells.DEVOUR.getId())) return;
        AbstractSpell spell = LotmSpells.DEVOUR.get();
        MagicData magic = MagicData.getPlayerMagicData(player);
        if (magic.isCasting() || magic.getPlayerCooldowns().isOnCooldown(spell)) return;
        int cost = spell.getManaCost(1);
        if (magic.getMana() < cost) return;
        if (DevourSpell.edible(player, stack) == null) return;
        if (!DevourSpell.eat(player, event.getHand())) return;
        magic.setMana(magic.getMana() - cost);
        magic.getPlayerCooldowns().addCooldown(spell, spell.getSpellCooldown());
        magic.getPlayerCooldowns().syncToPlayer(player);
        player.stopUsingItem();
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
    }

    private static boolean harsh(Player player) {
        if (player.level().dimension() == Level.NETHER) return true;
        return player.level().getBiome(player.blockPosition()).unwrapKey()
                .map(key -> key.location().getPath().contains("desert"))
                .orElse(false);
    }
}
