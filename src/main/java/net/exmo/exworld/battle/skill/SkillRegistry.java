package net.exmo.exworld.battle.skill;

import net.exmo.exworld.battle.card.CardDefinition;
import net.exmo.exworld.battle.data.*;

import java.util.*;

/** Data-facing registry plus the two real adapter seams: built-in skills and Iron spells. */
public final class SkillRegistry {
    private final Map<String, SkillDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, SkillAdapter> adapters = new HashMap<>();
    private final Set<String> debugCards = new LinkedHashSet<>();
    private final Map<String, CardDefinition> cards = new LinkedHashMap<>();
    private final Map<String, RewardPoolDefinition> rewardPools = new LinkedHashMap<>();
    private final Map<String, IntroProfile> introProfiles = new LinkedHashMap<>();
    private final Map<String, VfxDefinition> vfx = new LinkedHashMap<>();

    public SkillRegistry() { adapters.put("exworld", new ExWorldSkillAdapter()); }
    public void registerAdapter(String id, SkillAdapter adapter) { adapters.put(id, adapter); }
    public void replaceDefinitions(Collection<SkillDefinition> next) {
        definitions.clear(); next.forEach(definition -> definitions.put(definition.id(), definition));
    }
    public void register(SkillDefinition definition) { definitions.put(definition.id(), definition); }
    public void replaceOverlay(Set<String> previousIds, Collection<SkillDefinition> next) {
        Map<String, SkillDefinition> replacement = new LinkedHashMap<>(definitions);
        previousIds.forEach(replacement::remove); next.forEach(value -> replacement.put(value.id(), value));
        definitions.clear(); definitions.putAll(replacement);
    }
    public void registerDebug(SkillDefinition definition) { register(definition); debugCards.add(definition.id()); }
    public Optional<SkillDefinition> definition(String id) { return Optional.ofNullable(definitions.get(id)); }
    public Optional<SkillAdapter> adapter(String id) { return Optional.ofNullable(adapters.get(id)); }
    public boolean hasAdapter(String id) { return adapters.containsKey(id); }
    public Collection<SkillDefinition> definitions() { return List.copyOf(definitions.values()); }
    public List<String> debugCardIds() { return debugCards.stream().filter(definitions::containsKey).toList(); }
    public boolean isDebug(String id) { return debugCards.contains(id); }
    public void registerCard(CardDefinition definition) { cards.put(definition.id(), definition); }
    public void replaceCardOverlay(Set<String> previousIds, Collection<CardDefinition> next) {
        Map<String, CardDefinition> replacement = new LinkedHashMap<>(cards); previousIds.forEach(replacement::remove);
        next.forEach(value -> replacement.put(value.id(), value)); cards.clear(); cards.putAll(replacement);
    }
    public Optional<CardDefinition> card(String id) { return Optional.ofNullable(cards.get(id)); }
    public boolean retained(String id) { return card(id).map(value -> value.tags().contains("retain") || value.tags().contains("retained")).orElse(false); }
    public boolean knownCard(String id) { return cards.containsKey(id) || definitions.containsKey(id); }
    public Collection<CardDefinition> cardDefinitions() { return List.copyOf(cards.values()); }
    public List<String> standardCardIds() { return cards.values().stream().filter(card -> !isDebug(card.id())).map(CardDefinition::id).toList(); }
    /** All card ids exposed to administrative commands, including debug ids only when debug content is enabled. */
    public List<String> allCardIds() {
        LinkedHashSet<String> ids = new LinkedHashSet<>(cards.keySet());
        ids.addAll(debugCardIds());
        return List.copyOf(ids);
    }
    public CardDefinition.CardType cardType(String id) { return card(id).map(CardDefinition::type).orElse(CardDefinition.CardType.SKILL); }
    public void replaceAuxiliary(Collection<RewardPoolDefinition> rewards,Collection<IntroProfile> intros,Collection<VfxDefinition> effects){rewardPools.clear();rewards.forEach(v->rewardPools.put(v.id(),v));introProfiles.clear();intros.forEach(v->introProfiles.put(v.id(),v));vfx.clear();effects.forEach(v->vfx.put(v.id(),v));}
    public Optional<RewardPoolDefinition> rewardPool(String id){return Optional.ofNullable(rewardPools.get(id));}
    public IntroProfile introProfile(String id){return id==null?IntroProfile.defaults():Optional.ofNullable(introProfiles.get(id)).orElse(IntroProfile.defaults());}
    public Optional<VfxDefinition> vfx(String id){return Optional.ofNullable(vfx.get(id));}
    public SkillDefinition resolve(String cardId, int star) {
        CardDefinition card = cards.get(cardId);
        SkillDefinition base = definition(card == null ? cardId : card.skillId()).orElseThrow(() -> new IllegalArgumentException("Unknown skill/card " + cardId));
        if (card == null) return base;
        CardDefinition.StarTier tier = card.tier(star);
        double power = tier.adapterParameters().getOrDefault("power", base.power());
        return new SkillDefinition(base.id(), card.nameKey(), card.icon(), base.adapterId(), tier.manaCost(), tier.range(),
                base.targetType(), base.requiresLineOfSight(), base.piercesUnits(), base.ignoresTerrain(), power, base.level(),
                base.aiRole(), base.chebyshevRange(), base.physical());
    }
    /** Active battles receive a frozen registry so datapack reloads only affect future sessions. */
    public SkillRegistry snapshot() {
        SkillRegistry copy = new SkillRegistry(); copy.adapters.clear(); copy.adapters.putAll(adapters);
        copy.definitions.putAll(definitions); copy.debugCards.addAll(debugCards); copy.cards.putAll(cards);copy.rewardPools.putAll(rewardPools);copy.introProfiles.putAll(introProfiles);copy.vfx.putAll(vfx); return copy;
    }

    /** Production-safe factory: the old debug catalog is opt-in through defaults(true). */
    public static SkillRegistry defaults() { return defaults(false); }

    /** Registers the development-only debug card catalog into an existing registry. */
    public void registerDebugCardCatalog() { registerDebugCards(this); }

    /** Built-in content used by the game. Debug content is opt-in so production decks stay meaningful. */
    public static SkillRegistry defaults(boolean includeDebug) {
        SkillRegistry registry = new SkillRegistry();
        registry.register(new SkillDefinition("exworld:basic_attack", "skill.exworld.basic_attack",
                "exworld:textures/gui/skill/basic_attack.png", "exworld", 0, 1,
                SkillDefinition.TargetType.ENEMY, true, false, false, 0, 1,
                SkillDefinition.AiRole.ATTACK, true, true));
        registry.register(new SkillDefinition("exworld:guarded_strike", "skill.exworld.guarded_strike",
                "exworld:textures/gui/skill/guarded_strike.png", "exworld", 15, 1,
                SkillDefinition.TargetType.ENEMY, true, false, false, 12, 1));
        registry.register(new SkillDefinition("exworld:first_aid", "skill.exworld.first_aid",
                "exworld:textures/gui/skill/first_aid.png", "exworld", 20, 4,
                SkillDefinition.TargetType.ALLY, true, false, false, 10, 1));
        registry.register(new SkillDefinition("exworld:dash", "skill.exworld.dash",
                "exworld:textures/gui/skill/dash.png", "exworld", 0, 4,
                SkillDefinition.TargetType.CELL, false, true, false, 0, 1));
        registerBuiltInCards(registry);
        if (includeDebug) registerDebugCards(registry);
        return registry;
    }

    private static void registerBuiltInCards(SkillRegistry registry) {
        registry.registerCard(card("exworld:basic_attack", "exworld:basic_attack", "skill.exworld.basic_attack", "common", CardDefinition.CardType.ATTACK, Set.of("attack"), Map.of()));
        registry.registerCard(card("exworld:guarded_strike", "exworld:guarded_strike", "skill.exworld.guarded_strike", "common", CardDefinition.CardType.ATTACK, Set.of("attack"), Map.of()));
        registry.registerCard(card("exworld:first_aid", "exworld:first_aid", "skill.exworld.first_aid", "common", CardDefinition.CardType.SKILL, Set.of("skill"), Map.of()));
        registry.registerCard(card("exworld:dash", "exworld:dash", "skill.exworld.dash", "rare", CardDefinition.CardType.SKILL, Set.of("skill", "retain", "exhaust"), Map.of()));
        registry.register(new SkillDefinition("exworld:defend", "skill.exworld.defend", "exworld:textures/gui/skill/defend.png", "exworld", 1, 0, SkillDefinition.TargetType.SELF, false, false, false, 0, 1));
        registry.registerCard(card("exworld:defend", "exworld:defend", "skill.exworld.defend", "common", CardDefinition.CardType.SKILL, Set.of("skill"), Map.of("block", 5D)));
        registry.register(new SkillDefinition("exworld:gain_energy", "skill.exworld.gain_energy", "exworld:textures/gui/skill/energy.png", "exworld", 0, 0, SkillDefinition.TargetType.SELF, false, false, false, 0, 1));
        registry.registerCard(card("exworld:gain_energy", "exworld:gain_energy", "skill.exworld.gain_energy", "common", CardDefinition.CardType.SKILL, Set.of("skill"), Map.of("mana", 2D)));
        registry.register(new SkillDefinition("exworld:quick_thought", "skill.exworld.quick_thought", "exworld:textures/gui/skill/draw.png", "exworld", 1, 0, SkillDefinition.TargetType.SELF, false, false, false, 0, 1));
        registry.registerCard(card("exworld:quick_thought", "exworld:quick_thought", "skill.exworld.quick_thought", "common", CardDefinition.CardType.SKILL, Set.of("skill"), Map.of("draw", 2D)));
        registry.register(new SkillDefinition("exworld:purge", "skill.exworld.purge", "exworld:textures/gui/skill/purge.png", "exworld", 1, 0, SkillDefinition.TargetType.SELF, false, false, false, 0, 1));
        registry.registerCard(card("exworld:purge", "exworld:purge", "skill.exworld.purge", "uncommon", CardDefinition.CardType.SKILL, Set.of("skill", "exhaust"), Map.of("purge", 1D)));
        registry.register(new SkillDefinition("exworld:iron_will", "skill.exworld.iron_will", "exworld:textures/gui/skill/iron_will.png", "exworld", 2, 0, SkillDefinition.TargetType.SELF, false, false, false, 0, 1));
        registry.registerCard(card("exworld:iron_will", "exworld:iron_will", "skill.exworld.iron_will", "uncommon", CardDefinition.CardType.POWER, Set.of("power", "exhaust"), Map.of("status_strength", 1D)));
        registry.registerCard(card("exworld:slimed", "exworld:basic_attack", "card.exworld.slimed", "status", CardDefinition.CardType.STATUS, Set.of("status", "unplayable", "ethereal"), Map.of()));
        registry.registerCard(card("exworld:regret", "exworld:basic_attack", "card.exworld.regret", "curse", CardDefinition.CardType.CURSE, Set.of("curse", "unplayable"), Map.of()));
        registry.register(new SkillDefinition("exworld:contaminate", "skill.exworld.contaminate", "exworld:textures/gui/skill/contaminate.png", "exworld", 1, 4, SkillDefinition.TargetType.ENEMY, true, false, false, 0, 1));
        registry.registerCard(card("exworld:contaminate", "exworld:contaminate", "skill.exworld.contaminate", "uncommon", CardDefinition.CardType.SKILL, Set.of("skill", "exhaust"), Map.of("status_card", 1D)));
        registry.register(new SkillDefinition("exworld:dark_bargain", "skill.exworld.dark_bargain", "exworld:textures/gui/skill/dark_bargain.png", "exworld", 0, 0, SkillDefinition.TargetType.SELF, false, false, false, 0, 1));
        registry.registerCard(card("exworld:dark_bargain", "exworld:dark_bargain", "skill.exworld.dark_bargain", "rare", CardDefinition.CardType.SKILL, Set.of("skill", "exhaust"), Map.of("mana", 3D, "curse_card", 1D)));
        registry.register(new SkillDefinition("exworld:brace", "skill.exworld.brace", "exworld:textures/gui/skill/brace.png", "exworld", 1, 0, SkillDefinition.TargetType.SELF, false, false, false, 0, 1));
        registry.registerCard(card("exworld:brace", "exworld:brace", "skill.exworld.brace", "common", CardDefinition.CardType.SKILL, Set.of("skill"), Map.of("block", 8D)));
        registry.register(new SkillDefinition("exworld:adrenaline", "skill.exworld.adrenaline", "exworld:textures/gui/skill/adrenaline.png", "exworld", 0, 0, SkillDefinition.TargetType.SELF, false, false, false, 0, 1));
        registry.registerCard(card("exworld:adrenaline", "exworld:adrenaline", "skill.exworld.adrenaline", "uncommon", CardDefinition.CardType.SKILL, Set.of("skill"), Map.of("mana", 1D, "draw", 1D)));
        registry.register(new SkillDefinition("exworld:deep_breath", "skill.exworld.deep_breath", "exworld:textures/gui/skill/deep_breath.png", "exworld", 1, 0, SkillDefinition.TargetType.SELF, false, false, false, 0, 1));
        registry.registerCard(card("exworld:deep_breath", "exworld:deep_breath", "skill.exworld.deep_breath", "common", CardDefinition.CardType.SKILL, Set.of("skill"), Map.of("block", 4D, "draw", 2D)));
        registry.register(new SkillDefinition("exworld:cleanse", "skill.exworld.cleanse", "exworld:textures/gui/skill/cleanse.png", "exworld", 1, 0, SkillDefinition.TargetType.SELF, false, false, false, 0, 1));
        registry.registerCard(card("exworld:cleanse", "exworld:cleanse", "skill.exworld.cleanse", "uncommon", CardDefinition.CardType.SKILL, Set.of("skill", "exhaust"), Map.of("purge", 1D, "draw", 1D)));
        registry.register(new SkillDefinition("exworld:insight", "skill.exworld.insight", "exworld:textures/gui/skill/insight.png", "exworld", 2, 0, SkillDefinition.TargetType.SELF, false, false, false, 0, 1));
        registry.registerCard(card("exworld:insight", "exworld:insight", "skill.exworld.insight", "rare", CardDefinition.CardType.SKILL, Set.of("skill", "exhaust"), Map.of("draw", 3D)));
        registry.register(new SkillDefinition("exworld:focus", "skill.exworld.focus", "exworld:textures/gui/skill/focus.png", "exworld", 1, 0, SkillDefinition.TargetType.SELF, false, false, false, 0, 1));
        registry.registerCard(card("exworld:focus", "exworld:focus", "skill.exworld.focus", "uncommon", CardDefinition.CardType.POWER, Set.of("power", "exhaust"), Map.of("status_strength", 2D)));
        registry.register(new SkillDefinition("exworld:battle_trance", "skill.exworld.battle_trance", "exworld:textures/gui/skill/battle_trance.png", "exworld", 2, 0, SkillDefinition.TargetType.SELF, false, false, false, 0, 1));
        registry.registerCard(card("exworld:battle_trance", "exworld:battle_trance", "skill.exworld.battle_trance", "rare", CardDefinition.CardType.POWER, Set.of("power", "exhaust"), Map.of("status_strength", 1D, "draw", 2D)));
        registry.register(new SkillDefinition("exworld:ritual", "skill.exworld.ritual", "exworld:textures/gui/skill/ritual.png", "exworld", 2, 0, SkillDefinition.TargetType.SELF, false, false, false, 0, 1));
        registry.registerCard(card("exworld:ritual", "exworld:ritual", "skill.exworld.ritual", "rare", CardDefinition.CardType.POWER, Set.of("power", "exhaust"), Map.of("status_strength", 1D, "mana", 2D)));
        registry.register(new SkillDefinition("exworld:battle_plan", "skill.exworld.battle_plan", "exworld:textures/gui/skill/battle_plan.png", "exworld", 1, 0, SkillDefinition.TargetType.SELF, false, false, false, 0, 1));
        registry.registerCard(card("exworld:battle_plan", "exworld:battle_plan", "skill.exworld.battle_plan", "uncommon", CardDefinition.CardType.SKILL, Set.of("skill", "retain"), Map.of("draw", 2D)));
        registerGenericAndWarriorCards(registry);
    }

    private static void registerGenericAndWarriorCards(SkillRegistry registry) {
        registry.register(melee("exworld:dodge", "skill.exworld.dodge", 1, 1, SkillDefinition.TargetType.CELL, false, false));
        registry.registerCard(card("exworld:dodge", "exworld:dodge", "skill.exworld.dodge", "common", CardDefinition.CardType.SKILL, Set.of("skill", "generic"), Map.of()));
        registry.register(melee("exworld:guard", "skill.exworld.guard", 1, 0, SkillDefinition.TargetType.SELF, false, false));
        registry.registerCard(card("exworld:guard", "exworld:guard", "skill.exworld.guard", "common", CardDefinition.CardType.SKILL, Set.of("skill", "generic"), Map.of()));
        registry.register(melee("exworld:intercept", "skill.exworld.intercept", 0, 1, SkillDefinition.TargetType.CELL, false, false));
        registry.registerCard(card("exworld:intercept", "exworld:intercept", "skill.exworld.intercept", "common", CardDefinition.CardType.SKILL, Set.of("skill", "generic"), Map.of()));
        registry.register(melee("exworld:quick_meditation", "skill.exworld.quick_meditation", 1, 0, SkillDefinition.TargetType.SELF, false, false));
        registry.registerCard(card("exworld:quick_meditation", "exworld:quick_meditation", "skill.exworld.quick_meditation", "common", CardDefinition.CardType.SKILL, Set.of("skill", "generic"), Map.of()));
        registry.register(melee("exworld:basic_strike", "skill.exworld.basic_strike", 1, 2, SkillDefinition.TargetType.ENEMY, true, true));
        registry.registerCard(card("exworld:basic_strike", "exworld:basic_strike", "skill.exworld.basic_strike", "common", CardDefinition.CardType.ATTACK, Set.of("attack", "weapon"), Map.of()));
        registry.register(melee("exworld:cleave", "skill.exworld.cleave", 1, 1, SkillDefinition.TargetType.CELL, false, true));
        registry.registerCard(card("exworld:cleave", "exworld:cleave", "skill.exworld.cleave", "common", CardDefinition.CardType.ATTACK, Set.of("attack", "weapon"), Map.of()));
        registry.register(melee("exworld:leap_slash", "skill.exworld.leap_slash", 2, 1, SkillDefinition.TargetType.ENEMY, true, true));
        registry.registerCard(card("exworld:leap_slash", "exworld:leap_slash", "skill.exworld.leap_slash", "common", CardDefinition.CardType.ATTACK, Set.of("attack", "weapon"), Map.of()));
        registry.register(melee("exworld:taunt", "skill.exworld.taunt", 1, 0, SkillDefinition.TargetType.SELF, false, false));
        registry.registerCard(card("exworld:taunt", "exworld:taunt", "skill.exworld.taunt", "common", CardDefinition.CardType.SKILL, Set.of("skill", "weapon"), Map.of()));
        registry.register(melee("exworld:raise_shield", "skill.exworld.raise_shield", 1, 0, SkillDefinition.TargetType.SELF, false, false));
        registry.registerCard(card("exworld:raise_shield", "exworld:raise_shield", "skill.exworld.raise_shield", "common", CardDefinition.CardType.SKILL, Set.of("skill", "weapon"), Map.of()));
        registry.register(melee("exworld:battle_aura", "skill.exworld.battle_aura", 2, 0, SkillDefinition.TargetType.SELF, false, false));
        registry.registerCard(card("exworld:battle_aura", "exworld:battle_aura", "skill.exworld.battle_aura", "common", CardDefinition.CardType.SKILL, Set.of("skill", "weapon"), Map.of()));
    }

    private static SkillDefinition melee(String id, String name, int mana, int range, SkillDefinition.TargetType target,
                                         boolean los, boolean physical) {
        boolean ring = range > 0;
        return new SkillDefinition(id, name, "exworld:textures/gui/skill/card.png", "exworld", mana, range, target,
                los, false, false, 0, 1, ring ? SkillDefinition.AiRole.ATTACK : SkillDefinition.AiRole.BUFF, ring, physical);
    }

    private static CardDefinition card(String id, String skill, String name, String rarity, CardDefinition.CardType type,
                                       Set<String> tags, Map<String, Double> effects) {
        CardDefinition.StarTier tier = new CardDefinition.StarTier(0, 1, Map.of());
        int mana = switch (id) {
            case "exworld:guarded_strike" -> 15;
            case "exworld:first_aid" -> 20;
            case "exworld:defend", "exworld:quick_thought", "exworld:purge", "exworld:contaminate",
                 "exworld:brace", "exworld:deep_breath", "exworld:cleanse", "exworld:battle_plan",
                 "exworld:dodge", "exworld:guard", "exworld:quick_meditation", "exworld:basic_strike",
                 "exworld:cleave", "exworld:taunt", "exworld:raise_shield" -> 1;
            case "exworld:iron_will", "exworld:insight", "exworld:battle_trance", "exworld:ritual",
                 "exworld:leap_slash", "exworld:battle_aura" -> 2;
            case "exworld:focus" -> 1;
            default -> 0;
        };
        int range = switch (id) {
            case "exworld:first_aid", "exworld:contaminate", "exworld:dash" -> 4;
            case "exworld:guarded_strike", "exworld:basic_attack", "exworld:dodge", "exworld:intercept",
                 "exworld:cleave", "exworld:leap_slash" -> 1;
            case "exworld:basic_strike" -> 2;
            default -> 0;
        };
        CardDefinition.StarTier configured = new CardDefinition.StarTier(mana, range, Map.of());
        return new CardDefinition(id, skill, name, name + ".description", "exworld:textures/gui/skill/card.png", rarity,
                tags, type == CardDefinition.CardType.POWER, List.of(configured, configured, configured, configured, configured), type,
                tags.contains("exhaust") ? Set.of(CardDefinition.CardKeyword.EXHAUST) : Set.of(), effects);
    }

    /** Stable ids intentionally cover melee, ranged, healing and cell-target validation in debug battles. */
    private static void registerDebugCards(SkillRegistry registry) {
        for (int number = 1; number <= 36; number++) {
            int family = (number - 1) % 4;
            SkillDefinition.TargetType target = switch (family) {
                case 2 -> SkillDefinition.TargetType.ALLY;
                case 3 -> SkillDefinition.TargetType.CELL;
                default -> SkillDefinition.TargetType.ENEMY;
            };
            int range = switch (family) { case 0 -> 1; case 1, 2 -> 4 + number % 3; default -> 3 + number % 4; };
            int mana = 4 + (number % 6) * 3;
            double power = target == SkillDefinition.TargetType.CELL ? 0 : (5 + number) * .4;
            String suffix = String.format(Locale.ROOT, "%02d", number);
            registry.registerDebug(new SkillDefinition("exworld:debug_card_" + suffix, "skill.exworld.debug_card_" + suffix,
                    "exworld:textures/gui/skill/debug_card.png", "exworld", mana, range, target,
                    target != SkillDefinition.TargetType.CELL, number % 9 == 0, number % 12 == 0, power, 1));
        }
    }
}
