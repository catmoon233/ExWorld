package net.exmo.lotm.effect;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LotmEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, "lotm");
    public static final DeferredHolder<MobEffect, InfluenceEffect> GUIDANCE_REDIRECT =
            EFFECTS.register("guidance_redirect", () -> new InfluenceEffect(MindMarks.Kind.REDIRECT, 0xB388FF));
    public static final DeferredHolder<MobEffect, InfluenceEffect> GUIDANCE_FOLLOW =
            EFFECTS.register("guidance_follow", () -> new InfluenceEffect(MindMarks.Kind.FOLLOW, 0x8ECAE6));
    public static final DeferredHolder<MobEffect, InfluenceEffect> MIND_MIMIC =
            EFFECTS.register("mind_mimic", () -> new InfluenceEffect(MindMarks.Kind.MIMIC, 0xFF8FAB));
    public static final DeferredHolder<MobEffect, TrueSightEffect> TRUE_SIGHT = EFFECTS.register("true_sight", TrueSightEffect::new);

     public static final DeferredHolder<MobEffect, ResourceSenseEffect> RESOURCE_SENSE = EFFECTS.register("resource_sense", ResourceSenseEffect::new);
     public static final DeferredHolder<MobEffect, PreciseHarvestEffect> PRECISE_HARVEST = EFFECTS.register("precise_harvest", PreciseHarvestEffect::new);
    public static final DeferredHolder<MobEffect, MindScrambleEffect> MIND_SCRAMBLE = EFFECTS.register("mind_scramble", MindScrambleEffect::new);
     public static final DeferredHolder<MobEffect, SeaBlessingEffect> SEA_BLESSING = EFFECTS.register("sea_blessing", SeaBlessingEffect::new);
     public static final DeferredHolder<MobEffect, SpiritVisionEffect> SPIRIT_VISION = EFFECTS.register("spirit_vision", SpiritVisionEffect::new);
     public static final DeferredHolder<MobEffect, KnowledgeStrikeEffect> KNOWLEDGE_STRIKE = EFFECTS.register("knowledge_strike", KnowledgeStrikeEffect::new);
    private LotmEffects() {}

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}
