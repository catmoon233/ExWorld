package net.exmo.lotm;

import net.exmo.lotm.sequence.PassiveContext;
import net.exmo.lotm.sequence.PassiveDefinition;
import net.exmo.lotm.sequence.PassiveRegistry;
import net.exmo.lotm.sequence.PassiveTrigger;
import net.exmo.lotm.effect.LotmEffects;
import net.exmo.lotm.spell.PreciseHarvestSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.List;

/** Fate Circle passives. Keen senses are attributes; danger sense refreshes while hurt. */
public final class FateCirclePassives {
    public static final net.minecraft.resources.ResourceLocation KEEN_SENSES =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("lotm", "keen_senses");
    public static final net.minecraft.resources.ResourceLocation DANGER_SENSE =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("lotm", "danger_premonition");

    private FateCirclePassives() {}

    public static void register() {
        PassiveRegistry.register(new PassiveDefinition(
                KEEN_SENSES,
                "passive.lotm.keen_senses",
                "passive.lotm.keen_senses.desc",
                "minecraft:spyglass",
                PassiveTrigger.TICK,
                0,
                (player, context) -> false));
        PassiveRegistry.register(new PassiveDefinition(
                DANGER_SENSE,
                "passive.lotm.danger_premonition",
                "passive.lotm.danger_premonition.desc",
                "minecraft:clock",
                PassiveTrigger.TICK,
                40,
                FateCirclePassives::danger));
    }

    private static boolean danger(ServerPlayer player, PassiveContext context) {
        if (player.getHealth() > player.getMaxHealth() * 0.3F) return false;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1, false, true, true));
        return true;
    }

    @SubscribeEvent
    public static void preciseHarvest(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) return;
        if (!player.hasEffect(LotmEffects.PRECISE_HARVEST)) return;
        player.removeEffect(LotmEffects.PRECISE_HARVEST);
        ServerLevel level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        ItemStack tool = event.getTool().isEmpty() ? new ItemStack(Items.NETHERITE_PICKAXE) : event.getTool().copy();
        Holder<Enchantment> silk = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.SILK_TOUCH);
        tool.enchant(silk, 1);
        List<ItemStack> silkDrops = Block.getDrops(state, level, pos, event.getBlockEntity(), player, tool);
        if (silkDrops.isEmpty()) {
            bonus(event, level, pos, firstDrop(event));
            return;
        }
        event.getDrops().clear();
        for (ItemStack stack : silkDrops) add(event, level, pos, stack.copy());
        if (player.getRandom().nextFloat() < PreciseHarvestSpell.EXTRA_CHANCE) {
            bonus(event, level, pos, silkDrops.get(0));
        }
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    private static ItemStack firstDrop(BlockDropsEvent event) {
        for (ItemEntity entity : event.getDrops()) {
            if (entity != null && !entity.getItem().isEmpty()) return entity.getItem();
        }
        return ItemStack.EMPTY;
    }

    private static void bonus(BlockDropsEvent event, ServerLevel level, BlockPos pos, ItemStack source) {
        if (source == null || source.isEmpty()) return;
        add(event, level, pos, source.copyWithCount(1));
    }

    private static void add(BlockDropsEvent event, ServerLevel level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        entity.setDefaultPickUpDelay();
        event.getDrops().add(entity);
    }
}
