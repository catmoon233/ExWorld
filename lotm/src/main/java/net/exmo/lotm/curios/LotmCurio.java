package net.exmo.lotm.curios;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/** Curios item that only applies modifiers in its own slot, and never writes stack data from a tick. */
public class LotmCurio extends Item implements ICurioItem {
    @FunctionalInterface
    public interface Ticker {
        void tick(SlotContext context, ItemStack stack);
    }

    public record Bonus(String name, Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {}

    private final String slot;
    private final String descriptionKey;
    private final List<Bonus> bonuses;
    private final Ticker ticker;
    private final SoundEvent equipSound;

    public LotmCurio(Properties properties, String slot, String descriptionKey, List<Bonus> bonuses, Ticker ticker, SoundEvent equipSound) {
        super(properties);
        this.slot = slot;
        this.descriptionKey = descriptionKey;
        this.bonuses = List.copyOf(bonuses);
        this.ticker = ticker;
        this.equipSound = equipSound;
    }

    public String slot() {
        return slot;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(descriptionKey).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (ticker != null) ticker.tick(slotContext, stack);
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return slot.equals(slotContext.identifier());
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return canEquip(slotContext, stack);
    }

    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
        return new ICurio.SoundInfo(equipSound, 0.8f, 1.1f);
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        if (!slot.equals(slotContext.identifier()) || bonuses.isEmpty()) {
            return ICurioItem.super.getAttributeModifiers(slotContext, id, stack);
        }
        ImmutableMultimap.Builder<Holder<Attribute>, AttributeModifier> builder = ImmutableMultimap.builder();
        String path = BuiltInRegistries.ITEM.getKey(this).getPath();
        String index = slotContext.index() < 0 ? "tip" : Integer.toString(slotContext.index());
        for (Bonus bonus : bonuses) {
            ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath("lotm",
                    path + "_" + slotContext.identifier() + "_" + index + "_" + bonus.name());
            builder.put(bonus.attribute(), new AttributeModifier(modifierId, bonus.amount(), bonus.operation()));
        }
        return builder.build();
    }


    public static SoundEvent chain() {
        return SoundEvents.ARMOR_EQUIP_CHAIN.value();
    }

    public static SoundEvent iron() {
        return SoundEvents.ARMOR_EQUIP_IRON.value();
    }

    public static SoundEvent gold() {
        return SoundEvents.ARMOR_EQUIP_GOLD.value();
    }

    public static SoundEvent leather() {
        return SoundEvents.ARMOR_EQUIP_LEATHER.value();
    }
}
