package net.exmo.exworld.battle.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Whitelist registry: unsupported inventory items remain visible but have no battle adapter. */
public final class BattleItemRegistry {
    private final List<BattleItemAdapter> adapters = new ArrayList<>();

    public BattleItemRegistry register(BattleItemAdapter adapter) {
        adapters.add(adapter); return this;
    }
    public Optional<BattleItemAdapter> find(ItemStack stack) {
        return adapters.stream().filter(adapter -> adapter.matches(stack)).findFirst();
    }
    public static BattleItemRegistry defaults() {
        return new BattleItemRegistry()
                .register(new SimpleItemAdapter("minecraft:golden_apple", "self", BattleItemRegistry::useGoldenApple))
                .register(new SimpleItemAdapter("minecraft:enchanted_golden_apple", "self", BattleItemRegistry::useEnchantedGoldenApple))
                .register(new SimpleItemAdapter("minecraft:ender_pearl", "cell", BattleItemRegistry::useEnderPearl))
                .register(new SimpleItemAdapter("minecraft:potion", "self", BattleItemRegistry::usePotion))
                .register(new SimpleItemAdapter("minecraft:splash_potion", "cell", BattleItemRegistry::usePotion))
                .register(new SimpleItemAdapter("minecraft:lingering_potion", "cell", BattleItemRegistry::usePotion))
                .register(new SimpleItemAdapter("minecraft:wind_charge", "cell", BattleItemRegistry::useWindCharge))
                // This id-based registration is safe before NeoForge finishes deferred item registration.
                .register(new SimpleItemAdapter("exworld:battle_elixir", "self", BattleItemRegistry::useBattleElixir));
    }

    private static String useGoldenApple(BattleItemUseContext context) { return applyApple(context, false); }
    private static String useEnchantedGoldenApple(BattleItemUseContext context) { return applyApple(context, true); }
    private static String applyApple(BattleItemUseContext context, boolean enchanted) {
        LivingEntity living = context.player();
        living.heal(4);
        if (enchanted) {
            living.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 400, 1));
            living.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION, 2400, 3));
            living.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 600, 0));
            living.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 600, 0));
        } else {
            living.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 100, 0));
            living.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION, 1200, 0));
        }
        context.actor().syncHealth(living.getMaxHealth(), living.getHealth());
        return "";
    }

    private static String useEnderPearl(BattleItemUseContext context) {
        return context.session().teleportDisplacement(context.actor(), context.targetCell()).success() ? "" : "battle.command.no_path";
    }

    private static String usePotion(BattleItemUseContext context) {
        PotionContents contents = context.stack().get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        if (contents == null) return "battle.command.item_unadapted";
        boolean area = context.stack().is(Items.SPLASH_POTION) || context.stack().is(Items.LINGERING_POTION);
        if (!area) {
            contents.forEachEffect(effect -> applyPotionEffect(context.player(), context.player(), effect));
            context.session().syncLivingHealth(context.actor().id(), context.player().getMaxHealth(), context.player().getHealth());
        }
        else context.session().combatants().forEach(target -> {
            if (target.downed() || target.cell().distanceTo(context.targetCell()) > 1) return;
            LivingEntity living = net.exmo.exworld.battle.BattleSystem.livingEntity(context.session(), target.id());
            if (living != null) {
                contents.forEachEffect(effect -> applyPotionEffect(context.player(), living, effect));
                context.session().syncLivingHealth(target.id(), living.getMaxHealth(), living.getHealth());
            }
        });
        return "";
    }

    private static void applyPotionEffect(LivingEntity source, LivingEntity target, MobEffectInstance effect) {
        var mobEffect = effect.getEffect().value();
        if (mobEffect.isInstantenous()) {
            mobEffect.applyInstantenousEffect(source, source, target, effect.getAmplifier(), 1.0D);
        } else {
            target.addEffect(new MobEffectInstance(effect));
        }
    }

    private static String useWindCharge(BattleItemUseContext context) {
        context.session().damageHostilesAt(context.actor(), context.targetCell(), 1, 3,
                "minecraft:wind_charge", "item.minecraft.wind_charge");
        return "";
    }

    private static String useBattleElixir(BattleItemUseContext context) {
        LivingEntity living = context.player();
        living.heal(6);
        living.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 200, 0));
        context.actor().syncHealth(living.getMaxHealth(), living.getHealth());
        return "";
    }

    private record SimpleItemAdapter(String id, String targetType, java.util.function.Function<BattleItemUseContext, String> effect)
            implements BattleItemAdapter {
        @Override public boolean matches(ItemStack stack) {
            return !stack.isEmpty() && id.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        }
        @Override public String use(BattleItemUseContext context) { return effect.apply(context); }
        @Override public String validate(BattleItemUseContext context) {
            if (!"cell".equals(targetType)) return "";
            if (context.targetCell() == null) return "battle.command.target_required";
            if (!context.actor().cell().withinRadius(context.targetCell(), 8)) return "battle.command.out_of_range";
            if (!context.session().arena().definition().contains(new net.exmo.exworld.battle.arena.ArenaDefinition.GridPoint(
                    context.targetCell().x(), context.targetCell().z()))) return "battle.command.no_path";
            if (id.equals("minecraft:ender_pearl") && context.session().arena().occupant(context.targetCell()).isPresent())
                return "battle.command.no_path";
            return "";
        }
    }
}
