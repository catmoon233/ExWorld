package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LotmSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, "lotm");
    public static final DeferredHolder<AbstractSpell, AbstractSpell> WARRIOR_SLASH = SPELLS.register("warrior_slash", WarriorSlashSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> PUGILIST_COMBO = SPELLS.register("pugilist_combo", PugilistComboSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> WEAPON_BREAK = SPELLS.register("weapon_break", WeaponBreakSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SPIRITUAL_GRAFFITI = SPELLS.register("spiritual_graffiti", SpiritualGraffitiSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TRUE_SIGHT = SPELLS.register("true_sight", TrueSightSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SWIFT = SPELLS.register("swift", SwiftSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> NATURAL_GROWTH = SPELLS.register("natural_growth", NaturalGrowthSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> HEALING_HANDS = SPELLS.register("healing_hands", HealingHandsSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TRACKING_ARROW = SPELLS.register("tracking_arrow", TrackingArrowSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TAUNT_ROAR = SPELLS.register("taunt_roar", TauntRoarSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> STEAL_TOUCH = SPELLS.register("steal_touch", StealTouchSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MAD_WHISPER = SPELLS.register("mad_whisper", MadWhisperSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SUBTLE_GUIDANCE = SPELLS.register("subtle_guidance", SubtleGuidanceSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MIND_READ = SPELLS.register("mind_read", MindReadSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MIND_MIMIC = SPELLS.register("mind_mimic", MindMimicSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> HYMN = SPELLS.register("hymn", HymnSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> PURIFYING_LIGHT = SPELLS.register("purifying_light", PurifyingLightSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> EXORCISM = SPELLS.register("exorcism", ExorcismSpell::new);

     public static final DeferredHolder<AbstractSpell, AbstractSpell> RESOURCE_SENSE = SPELLS.register("resource_sense", ResourceSenseSpell::new);
     public static final DeferredHolder<AbstractSpell, AbstractSpell> PRECISE_HARVEST = SPELLS.register("precise_harvest", PreciseHarvestSpell::new);
     public static final DeferredHolder<AbstractSpell, AbstractSpell> STREET_BRAWL = SPELLS.register("street_brawl", StreetBrawlSpell::new);
     public static final DeferredHolder<AbstractSpell, AbstractSpell> DEVOUR = SPELLS.register("devour", DevourSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> PLACE_TRAP = SPELLS.register("place_trap", PlaceTrapSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> TAUNT = SPELLS.register("taunt", TauntSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> STEALTH = SPELLS.register("stealth", StealthSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> INSTIGATE = SPELLS.register("instigate", InstigateSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> THIEF_TOUCH = SPELLS.register("thief_touch", ThiefTouchSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> MIND_INTERFERENCE = SPELLS.register("mind_interference", MindInterferenceSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> THOUGHT_MISLEAD = SPELLS.register("thought_mislead", ThoughtMisleadSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> OPEN_DOOR = SPELLS.register("open_door", OpenDoorSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> FLASH = SPELLS.register("flash", FlashSpell::new);
    public static final DeferredHolder<AbstractSpell, AbstractSpell> LOUD_NOISE = SPELLS.register("loud_noise", LoudNoiseSpell::new);
     public static final DeferredHolder<AbstractSpell, AbstractSpell> SEA_BLESSING = SPELLS.register("sea_blessing", SeaBlessingSpell::new);
     public static final DeferredHolder<AbstractSpell, AbstractSpell> STORM_WRATH = SPELLS.register("storm_wrath", StormWrathSpell::new);
     public static final DeferredHolder<AbstractSpell, AbstractSpell> SPIRIT_VISION = SPELLS.register("spirit_vision", SpiritVisionSpell::new);
     public static final DeferredHolder<AbstractSpell, AbstractSpell> KNOWLEDGE_STRIKE = SPELLS.register("knowledge_strike", KnowledgeStrikeSpell::new);
    private LotmSpells() {}

    public static void register(IEventBus bus) {
        SPELLS.register(bus);
    }
}
