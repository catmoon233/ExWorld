package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.exmo.lotm.LotmSupport;
import net.exmo.lotm.ThiefPassives;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
/** Steal one hotbar item or one beneficial effect. Players can fumble the item into an empty bottle. */

public final class ThiefTouchSpell extends InstantSpell {
    public static final double RANGE = 4.5;
    public static final float FUMBLE_CHANCE = 0.35F;

    public ThiefTouchSpell() {
        super("thief_touch", 20.0, 15, SchoolRegistry.ELDRITCH_RESOURCE);
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.FOX_TELEPORT);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (target(entity) != null) return true;
        LotmSupport.tell(entity, "spell.lotm.thief_touch.no_target");
        return false;
    }

    @Override
    protected void cast(ServerLevel level, int spellLevel, LivingEntity caster) {
        LivingEntity found = target(caster);
        if (found == null) return;
        boolean wantItem = caster.getRandom().nextBoolean();
        boolean stole = wantItem ? stealItem(level, caster, found) || stealEffect(level, caster, found)
                : stealEffect(level, caster, found) || stealItem(level, caster, found);
        if (!stole) LotmSupport.tell(caster, "spell.lotm.thief_touch.empty");
    }

    private static LivingEntity target(LivingEntity caster) {
        LivingEntity living = LotmSupport.lookedAt(caster, RANGE);
        if (living == null || living.getTags().contains(ThiefPassives.AFTERIMAGE)) return null;
        return living;
    }

    private static boolean stealItem(ServerLevel level, LivingEntity caster, LivingEntity victim) {
        List<Slot> slots = pockets(victim);
        if (slots.isEmpty()) return false;
        if (victim instanceof Player player && (player.isCreative() || caster.getRandom().nextFloat() < FUMBLE_CHANCE)) {
            give(caster, new ItemStack(Items.GLASS_BOTTLE));
             level.playSound(null, caster.blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.7F, 1.4F);
            LotmSupport.tell(caster, "spell.lotm.thief_touch.bottle");
            return true;
        }
        Slot slot = slots.get(caster.getRandom().nextInt(slots.size()));
        ItemStack stolen = slot.stack().split(1);
        if (stolen.isEmpty()) return false;
        give(caster, stolen);
        level.playSound(null, caster.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.3F);
        LotmSupport.tell(caster, "spell.lotm.thief_touch.item", stolen.getHoverName());
        return true;
    }

    private static boolean stealEffect(ServerLevel level, LivingEntity caster, LivingEntity victim) {
        List<MobEffectInstance> buffs = new ArrayList<>();
        for (MobEffectInstance instance : victim.getActiveEffects()) {
            if (instance.getDuration() <= 0) continue;
            if (instance.getEffect().value().isBeneficial()) buffs.add(instance);
        }
        if (buffs.isEmpty()) return false;
        MobEffectInstance stolen = buffs.get(caster.getRandom().nextInt(buffs.size()));
        Holder<MobEffect> effect = stolen.getEffect();
        victim.removeEffect(effect);
        MobEffectInstance current = caster.getEffect(effect);
        boolean upgrade = current == null
                || stolen.getAmplifier() > current.getAmplifier()
                || (stolen.getAmplifier() == current.getAmplifier() && stolen.getDuration() > current.getDuration());
        if (upgrade) {
            caster.removeEffect(effect);
            caster.addEffect(new MobEffectInstance(stolen));
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.7F, 1.3F);
        LotmSupport.tell(caster, "spell.lotm.thief_touch.effect", effect.value().getDisplayName());
        return true;
    }

    private static List<Slot> pockets(LivingEntity victim) {
        List<Slot> slots = new ArrayList<>();
        if (victim instanceof ServerPlayer player) {
            var inventory = player.getInventory();
            for (int index = 0; index < 9; index++) {
                ItemStack stack = inventory.getItem(index);
                if (!stack.isEmpty()) slots.add(new Slot(stack));
            }
            return slots;
        }
        for (EquipmentSlot equipment : EquipmentSlot.values()) {
            ItemStack stack = victim.getItemBySlot(equipment);
            if (!stack.isEmpty()) slots.add(new Slot(stack));
        }
        return slots;
    }

    private static void give(LivingEntity caster, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (caster instanceof Player player && player.getInventory().add(stack)) return;
        caster.spawnAtLocation(stack);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.thief_touch.guide"));
    }

    private record Slot(ItemStack stack) {}
}
