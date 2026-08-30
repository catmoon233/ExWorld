package net.exmo.exworld.battle.data;

import com.google.gson.*;
import net.exmo.exworld.Exworld;
import net.exmo.exworld.battle.BattleSystem;
import net.exmo.exworld.battle.arena.ArenaDefinition;
import net.exmo.exworld.battle.card.CardDefinition;
import net.exmo.exworld.battle.skill.SkillDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.*;

/** Parses and validates the complete datapack batch before one atomic content swap. */
public final class BattleDataReloadListener extends SimpleJsonResourceReloadListener {
    public BattleDataReloadListener() { super(new Gson(), "exworld_battle"); }
    public static void register(AddReloadListenerEvent event) { event.addListener(new BattleDataReloadListener()); }

    @Override protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resources, ProfilerFiller profiler) {
        try {
            List<ArenaDefinition> arenas = new ArrayList<>(); List<SkillDefinition> skills = new ArrayList<>(); List<CardDefinition> cards = new ArrayList<>();List<RewardPoolDefinition> rewards=new ArrayList<>();List<IntroProfile> intros=new ArrayList<>();List<VfxDefinition> effects=new ArrayList<>();
            Set<String> ids = new HashSet<>();
            for (var entry : entries.entrySet()) {
                JsonObject json = entry.getValue().getAsJsonObject(); String kind = json.get("kind").getAsString();
                if (kind.equals("arena")) { ArenaDefinition arena = parseArena(entry.getKey(), json); if (!ids.add("arena:" + arena.id())) throw new IllegalArgumentException("Duplicate arena " + arena.id()); arenas.add(arena); }
                else if (kind.equals("skill")) { SkillDefinition skill = parseSkill(entry.getKey(), json); if (!ids.add("skill:" + skill.id())) throw new IllegalArgumentException("Duplicate skill " + skill.id()); skills.add(skill); }
                else if (kind.equals("card")) { CardDefinition card = parseCard(entry.getKey(), json); if (!ids.add("card:" + card.id())) throw new IllegalArgumentException("Duplicate card " + card.id()); cards.add(card); }
                else if(kind.equals("reward_pool")){RewardPoolDefinition value=parseReward(entry.getKey(),json);if(!ids.add("reward:"+value.id()))throw new IllegalArgumentException("Duplicate reward "+value.id());rewards.add(value);}
                else if(kind.equals("intro_profile")){IntroProfile value=parseIntro(entry.getKey(),json);if(!ids.add("intro:"+value.id()))throw new IllegalArgumentException("Duplicate intro "+value.id());intros.add(value);}
                else if(kind.equals("vfx")){VfxDefinition value=parseVfx(entry.getKey(),json);if(!ids.add("vfx:"+value.id()))throw new IllegalArgumentException("Duplicate vfx "+value.id());effects.add(value);}
                else throw new IllegalArgumentException("Unknown kind " + kind + " in " + entry.getKey());
            }
            BattleSystem.applyContent(arenas, skills, cards,rewards,intros,effects);
            Exworld.LOGGER.info("Atomically loaded {} battle arenas, {} skills and {} cards", arenas.size(), skills.size(), cards.size());
        } catch (Exception error) {
            Exworld.LOGGER.error("Rejected complete ExWorld battle content reload; previous snapshot remains active", error);
        }
    }

    private static ArenaDefinition parseArena(ResourceLocation file, JsonObject json) {
        String id = json.has("id") ? json.get("id").getAsString() : file.toString(); int width = json.get("width").getAsInt(), depth = json.get("depth").getAsInt();
        int baseY = json.has("base_y") ? json.get("base_y").getAsInt() : 64, maxStep = json.has("max_step_height") ? json.get("max_step_height").getAsInt() : 1;
        Set<ArenaDefinition.GridPoint> blocked = new LinkedHashSet<>(); if (json.has("blocked")) for (JsonElement value : json.getAsJsonArray("blocked")) { JsonArray point = value.getAsJsonArray(); blocked.add(new ArenaDefinition.GridPoint(point.get(0).getAsInt(), point.get(1).getAsInt())); }
        return new ArenaDefinition(id, width, depth, baseY, maxStep, Map.of(), blocked, Map.of());
    }
    private static SkillDefinition parseSkill(ResourceLocation file, JsonObject json) {
        String id = json.has("id") ? json.get("id").getAsString() : file.toString();
        return new SkillDefinition(id, json.get("name_key").getAsString(), json.get("icon").getAsString(), json.get("adapter").getAsString(),
                json.get("mana_cost").getAsInt(), json.get("range").getAsInt(), SkillDefinition.TargetType.valueOf(json.get("target_type").getAsString().toUpperCase(Locale.ROOT)),
                !json.has("requires_line_of_sight") || json.get("requires_line_of_sight").getAsBoolean(), json.has("pierces_units") && json.get("pierces_units").getAsBoolean(),
                json.has("ignores_terrain") && json.get("ignores_terrain").getAsBoolean(), json.has("power") ? json.get("power").getAsDouble() : 0, json.has("level") ? json.get("level").getAsInt() : 1);
    }
    private static CardDefinition parseCard(ResourceLocation file, JsonObject json) {
        String id = json.has("id") ? json.get("id").getAsString() : file.toString();
        Set<String> tags = new LinkedHashSet<>(); if (json.has("tags")) json.getAsJsonArray("tags").forEach(value -> tags.add(value.getAsString()));
        Map<String, Double> effects = new LinkedHashMap<>();
        if (json.has("effects")) json.getAsJsonObject("effects").entrySet().forEach(entry -> effects.put(entry.getKey(), entry.getValue().getAsDouble()));
        List<CardDefinition.StarTier> stars = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("stars")) {
            JsonObject value = element.getAsJsonObject(); Map<String, Double> parameters = new LinkedHashMap<>();
            if (value.has("parameters")) value.getAsJsonObject("parameters").entrySet().forEach(entry -> parameters.put(entry.getKey(), entry.getValue().getAsDouble()));
            stars.add(new CardDefinition.StarTier(value.get("mana_cost").getAsInt(), value.get("range").getAsInt(), parameters));
        }
        CardDefinition.CardType type = json.has("type") ? CardDefinition.CardType.valueOf(json.get("type").getAsString().toUpperCase(Locale.ROOT)) : null;
        Set<CardDefinition.CardKeyword> keywords = new LinkedHashSet<>();
        if (json.has("keywords")) for (JsonElement value : json.getAsJsonArray("keywords")) keywords.add(CardDefinition.CardKeyword.valueOf(value.getAsString().toUpperCase(Locale.ROOT)));
        return new CardDefinition(id, json.get("skill").getAsString(), json.get("name_key").getAsString(), json.get("description_key").getAsString(),
                json.get("icon").getAsString(), json.has("rarity") ? json.get("rarity").getAsString() : "common", tags,
                json.has("consumable") && json.get("consumable").getAsBoolean(), stars, type, keywords, effects);
    }
    private static RewardPoolDefinition parseReward(ResourceLocation file,JsonObject json){String id=json.has("id")?json.get("id").getAsString():file.toString();List<RewardPoolDefinition.Entry> entries=new ArrayList<>();for(JsonElement element:json.getAsJsonArray("entries")){JsonObject value=element.getAsJsonObject();entries.add(new RewardPoolDefinition.Entry(value.get("card").getAsString(),value.has("weight")?value.get("weight").getAsInt():1,value.has("rarity")?value.get("rarity").getAsString():"common"));}return new RewardPoolDefinition(id,json.has("gold")?json.get("gold").getAsInt():0,entries);}
    private static IntroProfile parseIntro(ResourceLocation file,JsonObject json){String id=json.has("id")?json.get("id").getAsString():file.toString();return new IntroProfile(id,json.has("fade_ticks")?json.get("fade_ticks").getAsInt():8,json.has("travel_ticks")?json.get("travel_ticks").getAsInt():20,json.has("hold_ticks")?json.get("hold_ticks").getAsInt():15,json.has("overview_ticks")?json.get("overview_ticks").getAsInt():25,json.has("fov")?json.get("fov").getAsFloat():55,!json.has("black_bars")||json.get("black_bars").getAsBoolean());}
    private static VfxDefinition parseVfx(ResourceLocation file,JsonObject json){String id=json.has("id")?json.get("id").getAsString():file.toString();Map<VfxDefinition.Track,String> particles=new EnumMap<>(VfxDefinition.Track.class),sounds=new EnumMap<>(VfxDefinition.Track.class);if(json.has("particles"))json.getAsJsonObject("particles").entrySet().forEach(entry->particles.put(VfxDefinition.Track.valueOf(entry.getKey().toUpperCase(Locale.ROOT)),entry.getValue().getAsString()));if(json.has("sounds"))json.getAsJsonObject("sounds").entrySet().forEach(entry->sounds.put(VfxDefinition.Track.valueOf(entry.getKey().toUpperCase(Locale.ROOT)),entry.getValue().getAsString()));return new VfxDefinition(id,particles,sounds);}
}
