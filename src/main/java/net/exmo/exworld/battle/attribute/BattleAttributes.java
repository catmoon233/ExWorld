package net.exmo.exworld.battle.attribute;

import net.exmo.exworld.Exworld;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BattleAttributes {
    private static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, Exworld.MODID);
    public static final DeferredHolder<Attribute, Attribute> INITIATIVE = register("initiative", 10, 0, 100_000);
    public static final DeferredHolder<Attribute, Attribute> MOVEMENT_POINTS = register("movement_points", 6, 0, 1_000);
    public static final DeferredHolder<Attribute, Attribute> INITIAL_HAND_SIZE = register("initial_hand_size", 3, 1, 10);
    public static final DeferredHolder<Attribute, Attribute> BASIC_ATTACK_RANGE = register("basic_attack_range", 1, 0, 1_000);
    public static final DeferredHolder<Attribute, Attribute> SIGHT_RANGE = register("sight_range", 12, 0, 1_000);
    public static final DeferredHolder<Attribute, Attribute> HEALING_POWER = register("healing_power", 1, 0, 100);
    public static final DeferredHolder<Attribute, Attribute> STATUS_POWER = register("status_power", 1, 0, 100);
    public static final DeferredHolder<Attribute, Attribute> STATUS_RESISTANCE = register("status_resistance", 1, 0, 100);
    public static final DeferredHolder<Attribute, Attribute> CRITICAL_CHANCE = register("critical_chance", .05, 0, 1);
    public static final DeferredHolder<Attribute, Attribute> CRITICAL_DAMAGE = register("critical_damage", 1.5, 1, 100);
    public static final DeferredHolder<Attribute, Attribute> THREAT_GENERATION = register("threat_generation", 1, 0, 100);
    public static final DeferredHolder<Attribute, Attribute> ITEM_USES_PER_PHASE = register("item_uses_per_phase", 1, 0, 100);
    public static final DeferredHolder<Attribute, Attribute> ACTION_POINTS = register("action_points", 3, 0, 16);
    public static final DeferredHolder<Attribute, Attribute> STRENGTH_LEVEL = register("strength_level", 1, 1, 99);

    private BattleAttributes() {}
    public static void register(IEventBus bus) { ATTRIBUTES.register(bus); bus.addListener(BattleAttributes::modifyAttributes); }
    private static DeferredHolder<Attribute, Attribute> register(String id, double base, double min, double max) {
        return ATTRIBUTES.register(id, () -> new RangedAttribute("attribute.exworld." + id, base, min, max).setSyncable(true));
    }
    private static void modifyAttributes(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(type -> ATTRIBUTES.getEntries().forEach(attribute -> event.add(type, attribute)));
    }
}
