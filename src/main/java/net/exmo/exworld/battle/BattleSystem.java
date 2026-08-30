package net.exmo.exworld.battle;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.exmo.exworld.Exworld;
import net.exmo.exworld.content.ExWorldContent;
import net.exmo.exworld.battle.api.*;
import net.exmo.exworld.battle.attribute.BattleAttributes;
import net.exmo.exworld.battle.compat.IronManaEvents;
import net.exmo.exworld.battle.compat.BattleDamageContext;
import net.exmo.exworld.battle.compat.MinecraftBattleEffectResolver;
import net.exmo.exworld.battle.compat.IronHitWindow;
import net.exmo.exworld.battle.compat.IronSpellAdapter;
import net.exmo.exworld.battle.compat.ManaMutationContext;
import net.exmo.exworld.battle.arena.ArenaDefinition;
import net.exmo.exworld.battle.card.PlayerCardCollection;
import net.exmo.exworld.battle.card.PlayerCardModule;
import net.exmo.exworld.battle.combatant.Combatant;
import net.exmo.exworld.battle.encounter.FightDebugEncounter;
import net.exmo.exworld.battle.model.BattleCell;
import net.exmo.exworld.battle.model.BattleFacing;
import net.exmo.exworld.battle.model.BattleMovementHeading;
import net.exmo.exworld.battle.model.EngagementAdvantage;
import net.exmo.exworld.battle.model.BattleState;
import net.exmo.exworld.battle.model.BattleStatus;
import net.exmo.exworld.battle.model.FactionRelation;
import net.exmo.exworld.battle.skill.SkillDefinition;
import net.exmo.exworld.battle.skill.SkillRegistry;
import net.exmo.exworld.battle.persistence.BattleNbtCodec;
import net.exmo.exworld.battle.persistence.BattleSavedData;
import net.exmo.exworld.battle.persistence.BattleReturnSavedData;
import net.exmo.exworld.battle.item.MinecraftBattleItemAccess;
import net.exmo.exworld.battle.item.BattleItemAdapter;
import net.exmo.exworld.equipment.PlayerEquipmentModule;
import net.exmo.exworld.equipment.PlayerEquipmentSavedData;
import net.exmo.exworld.network.BattleNetwork;
import net.exmo.exworld.network.CardCollectionPayload;
import net.exmo.exworld.network.CardCollectionActionPayload;
import net.exmo.exworld.monster.MonsterProfileRegistry;
import net.exmo.exworld.network.EquipmentActionPayload;
import net.exmo.exworld.battle.card.CardCollectionSnapshot;
import net.exmo.exworld.battle.party.DebugPartyManager;
import net.exmo.exworld.battle.party.PartySnapshot;
import net.exmo.exworld.dungeon.DungeonSystem;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** NeoForge adapter around the pure BattleEngine. */
public final class BattleSystem {
    private static final int POTION_TICKS_PER_ROUND = 20 * 10;
    public static final ResourceKey<Level> BATTLE_LEVEL = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "battle"));
    private static MinecraftServer activeServer;
    private static final SkillRegistry SKILLS = SkillRegistry.defaults();
    private static final MinecraftBattleEffectResolver EFFECTS = new MinecraftBattleEffectResolver(() -> activeServer, BATTLE_LEVEL);
    private static final BattleEngine ENGINE = new BattleEngine(SKILLS, EFFECTS);
    private static final PlayerCardModule PLAYER_CARDS = new PlayerCardModule(SKILLS);
    private static final PlayerEquipmentModule PLAYER_EQUIPMENT = new PlayerEquipmentModule();
    private static final MinecraftBattleItemAccess BATTLE_ITEMS = new MinecraftBattleItemAccess(PLAYER_EQUIPMENT);
    private static final DebugPartyManager PARTIES = new DebugPartyManager();
    private static final Map<UUID, BattleId> PARTICIPATION = new ConcurrentHashMap<>();
    private static final Map<UUID, EncounterRequest.ReturnPoint> RETURNS = new ConcurrentHashMap<>();
    private static final Map<UUID, GameType> PREVIOUS_GAME_MODES = new ConcurrentHashMap<>();
    private static final Map<BattleId, Long> LAST_SYNCED_REVISIONS = new HashMap<>();
    private static final Map<BattleId, Integer> LAST_SYNCED_SECONDS = new HashMap<>();
    private static final Map<PendingIronKey, PendingIronHit> PENDING_IRON_HITS = new HashMap<>();
    private static final Set<BattleId> AUXILIARIES_CLEARED = new HashSet<>();
    private static final Map<UUID, CastAim> CAST_AIMS = new HashMap<>();
    private static final Set<UUID> FIGHT_DEBUG_ENABLED = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> MULTI_MONSTER_DISABLED = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> STARTING_ENCOUNTERS = ConcurrentHashMap.newKeySet();
    private static boolean restored;
    private static Set<String> contentArenaIds = Set.of();
    private static Set<String> contentSkillIds = Set.of();
    private static Set<String> contentCardIds = Set.of();

    static {
        registerIron("irons_spellbooks:magic_arrow", 16, 18, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:firebolt", 12, 10, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:heal", 4, 30, SkillDefinition.TargetType.SELF, 1);
        registerIron("irons_spellbooks:fireball", 12, 28, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:chain_lightning", 10, 35, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:shield", 3, 22, SkillDefinition.TargetType.CELL, 1);
        registerIron("irons_spellbooks:cone_of_cold", 8, 26, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:slow", 8, 18, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:healing_circle", 8, 32, SkillDefinition.TargetType.CELL, 1);
        registerIron("irons_spellbooks:wall_of_fire", 10, 34, SkillDefinition.TargetType.CELL, 1);
        registerIron("irons_spellbooks:frost_step", 6, 24, SkillDefinition.TargetType.CELL, 1);
        registerIron("irons_spellbooks:summon_vex", 6, 40, SkillDefinition.TargetType.CELL, 1);
        // 投射物伤害
        registerIron("irons_spellbooks:icicle", 14, 12, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:snowball", 14, 15, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:magic_missile", 16, 10, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:poison_arrow", 16, 22, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:fire_arrow", 16, 22, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:magma_bomb", 12, 25, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:guiding_bolt", 14, 18, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:lightning_bolt", 16, 30, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:eldritch_blast", 14, 35, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:blood_needles", 12, 22, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:wither_skull", 14, 20, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:acid_orb", 14, 22, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:spectral_hammer", 12, 15, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:ball_lightning", 12, 20, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:firecracker", 12, 20, SkillDefinition.TargetType.ENEMY, 1);
        // 持续射线
        registerIron("irons_spellbooks:fire_breath", 6, 20, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:ray_of_frost", 12, 20, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:poison_breath", 6, 20, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:dragon_breath", 6, 25, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:electrocute", 8, 18, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:ray_of_siphoning", 10, 18, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:sunbeam", 14, 30, SkillDefinition.TargetType.ENEMY, 1);
        // 以自身为中心的爆发
        registerIron("irons_spellbooks:shockwave", 0, 26, SkillDefinition.TargetType.SELF, 1);
        registerIron("irons_spellbooks:frostwave", 0, 26, SkillDefinition.TargetType.SELF, 1);
        // 前方扇形/锥形范围
        registerIron("irons_spellbooks:stomp", 4, 24, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:divine_smite", 6, 25, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:ice_spikes", 8, 22, SkillDefinition.TargetType.ENEMY, 1);
        // 治疗
        registerIron("irons_spellbooks:greater_heal", 0, 40, SkillDefinition.TargetType.SELF, 1, 20);
        registerIron("irons_spellbooks:blessing_of_life", 0, 15, SkillDefinition.TargetType.SELF, 1, 8);
        // 增益
        registerIron("irons_spellbooks:fortify", 0, 30, SkillDefinition.TargetType.SELF, 1);
        registerIron("irons_spellbooks:haste", 0, 25, SkillDefinition.TargetType.SELF, 1);
        registerIron("irons_spellbooks:oakskin", 0, 20, SkillDefinition.TargetType.SELF, 1);
        registerIron("irons_spellbooks:frostbite", 0, 30, SkillDefinition.TargetType.SELF, 1);
        // 控制
        registerIron("irons_spellbooks:root", 10, 25, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:blight", 12, 30, SkillDefinition.TargetType.ENEMY, 1);
        registerIron("irons_spellbooks:poison_splash", 12, 22, SkillDefinition.TargetType.ENEMY, 1);
        ENGINE.onResult(BattleSystem::handleResult);
        ENGINE.onEvent(BattleSystem::handleBattleEvent);
    }

    private BattleSystem() {}
    public static BattleInterface battles() { return ENGINE; }
    /** Public semantic event seam for addons that need all active battle sessions. */
    public static IEventBus battleEvents() { return NeoForge.EVENT_BUS; }
    public static List<UUID> partyMembers(UUID member) { return PARTIES.members(member); }
    public static UUID partyLeader(UUID member) { return PARTIES.leader(member); }
    /** Starts a battle in the host chosen by the request and binds its live entities. */
    public static BattleId startHostedEncounter(EncounterRequest request, List<ServerPlayer> players) {
        BattleId battleId = ENGINE.startEncounter(request);
        BattleSession session = ENGINE.session(battleId).orElseThrow();
        attachPlayerSystems(session, players, true);
        session.combatants().forEach(combatant -> PARTICIPATION.put(combatant.id(), battleId));
        ServerLevel level = levelFor(session);
        if (level == null) throw new IllegalStateException("Battle host dimension is not loaded: " + request.host().dimension());
        for (ServerPlayer player : players) {
            PARTICIPATION.put(player.getUUID(), battleId);
            PREVIOUS_GAME_MODES.putIfAbsent(player.getUUID(), player.gameMode.getGameModeForPlayer());
            player.setGameMode(GameType.ADVENTURE);
            Vec3 position = worldPosition(session, session.combatant(player.getUUID()).orElseThrow());
            if (player.level() != level) player.teleportTo(level, position.x(), position.y(), position.z(), Set.of(), 90, 45);
            else { player.teleportTo(position.x(), position.y(), position.z()); player.setYRot(90); player.setXRot(45); }
        }
        for (Combatant combatant : session.combatants()) {
            if (combatant.playerId() != null) continue;
            Entity entity = level.getEntity(combatant.id());
            if (entity instanceof Mob mob) mob.setNoAi(true);
            if (entity != null) {
                Vec3 position = worldPosition(session, combatant);
                entity.teleportTo(position.x(), position.y(), position.z());
            }
        }
        if (request.host().ownsArena()) buildArena(level, session.snapshot());
        players.forEach(player -> BattleNetwork.sendSnapshot(player, session.snapshot()));
        return battleId;
    }

    private static void attachPlayerSystems(BattleSession session, Collection<ServerPlayer> players, boolean initializeItemQuota) {
        session.itemAccess(BATTLE_ITEMS);
        for (ServerPlayer player : players) {
            Combatant actor = session.combatant(player.getUUID()).orElse(null);
            if (actor == null) continue;
            PlayerEquipmentSavedData.Slots slots = PLAYER_EQUIPMENT.slots(player.getServer(), player.getUUID());
            int active = 0;
            String held = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
            if (held.equals(slots.first())) active = 1;
            else if (held.equals(slots.second())) active = 2;
            if (initializeItemQuota) actor.configureEquipment(slots.first(), slots.second(), active);
            BATTLE_ITEMS.autoEquipWeaponOne(session, actor, player);
            String heldItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
            actor.setWeaponFamily(net.exmo.exworld.battle.weapon.WeaponFamily.of(heldItem));
            actor.setStrengthLevel(Math.max(1, (int) Math.round(player.getAttributeValue(BattleAttributes.STRENGTH_LEVEL))));
            actor.setWeaponAttack(actor.weaponFamily() == net.exmo.exworld.battle.weapon.WeaponFamily.NONE
                    ? 0 : Math.max(0, player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)));
            if (initializeItemQuota) {
                double configured = player.getAttributeValue(BattleAttributes.ITEM_USES_PER_PHASE);
                actor.setItemUseLimit((int) Math.max(0, Math.floor(configured)));
                actor.setItemUsesRemaining(actor.itemUseLimit());
            }
        }
    }
    public static void registerArena(net.exmo.exworld.battle.arena.ArenaDefinition definition) { ENGINE.registerArena(definition); }
    public static void registerSkill(SkillDefinition definition) { SKILLS.register(definition); }
    /** Validation seam for server-owned monster packages. */
    public static boolean isKnownSkill(String id) { return SKILLS.definition(id).isPresent() || SKILLS.knownCard(id); }
    /** Extension seam for other ExWorld modules or addons to register custom battle consumables. */
    public static void registerBattleItemAdapter(BattleItemAdapter adapter) { BATTLE_ITEMS.register(adapter); }
    /** Registers the opt-in development card catalog once the config has finished loading. */
    public static void registerDebugCards() { SKILLS.registerDebugCardCatalog(); }
    public static synchronized void applyContent(Collection<net.exmo.exworld.battle.arena.ArenaDefinition> arenas,
                                                 Collection<SkillDefinition> definitions,
                                                 Collection<net.exmo.exworld.battle.card.CardDefinition> cards,
                                                 Collection<net.exmo.exworld.battle.data.RewardPoolDefinition> rewards,
                                                 Collection<net.exmo.exworld.battle.data.IntroProfile> intros,
                                                 Collection<net.exmo.exworld.battle.data.VfxDefinition> effects) {
        for (SkillDefinition definition : definitions) if (!SKILLS.hasAdapter(definition.adapterId()))
            throw new IllegalArgumentException("Unknown skill adapter " + definition.adapterId() + " for " + definition.id());
        Set<String> skillIds = new HashSet<>(); SKILLS.definitions().forEach(value -> skillIds.add(value.id())); definitions.forEach(value -> skillIds.add(value.id()));
        for (var card : cards) if (!skillIds.contains(card.skillId())) throw new IllegalArgumentException("Unknown skill " + card.skillId() + " for card " + card.id());
        Set<String> cardIds=new HashSet<>(skillIds);cards.forEach(card->cardIds.add(card.id()));for(var reward:rewards)for(var entry:reward.entries())if(!cardIds.contains(entry.cardId()))throw new IllegalArgumentException("Unknown reward card "+entry.cardId()+" in "+reward.id());
        ENGINE.replaceArenas(contentArenaIds, arenas);
        SKILLS.replaceOverlay(contentSkillIds, definitions);
        SKILLS.replaceCardOverlay(contentCardIds, cards);
        SKILLS.replaceAuxiliary(rewards,intros,effects);
        contentArenaIds = arenas.stream().map(net.exmo.exworld.battle.arena.ArenaDefinition::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
        contentSkillIds = definitions.stream().map(SkillDefinition::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
        contentCardIds = cards.stream().map(net.exmo.exworld.battle.card.CardDefinition::id).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    public static PlayerCardModule playerCards() { return PLAYER_CARDS; }
    public static void equipmentAction(ServerPlayer player, EquipmentActionPayload payload) {
        if (payload.action() != EquipmentActionPayload.Action.REQUEST && isParticipating(player.getUUID())) return;
        switch (payload.action()) {
            case REQUEST -> { }
            case SET -> PLAYER_EQUIPMENT.set(player, payload.weaponSlot(), payload.inventorySlot());
            case CLEAR -> PLAYER_EQUIPMENT.clear(player, payload.weaponSlot());
        }
        BattleNetwork.sendEquipment(player, PLAYER_EQUIPMENT.slots(player.getServer(), player.getUUID()));
    }
    public static void cardCollectionAction(ServerPlayer player, CardCollectionActionPayload payload) {
        if (isParticipating(player.getUUID()) && payload.action() != CardCollectionActionPayload.Action.REQUEST) return;
        PlayerCardCollection collection = PLAYER_CARDS.collection(player.getServer(), player.getUUID()); boolean changed = false;
        switch (payload.action()) {
            case REQUEST -> {}
            case ADD_TO_DECK -> changed = collection.addToDeck(payload.deckSlot(), payload.cardId());
            case REMOVE_FROM_DECK -> changed = collection.removeFromDeck(payload.deckSlot(), payload.cardId());
            case SET_ACTIVE -> changed = collection.setActiveDeck(payload.deckSlot());
            case RENAME -> changed = collection.renameDeck(payload.deckSlot(), payload.value());
            case FUSE -> changed = collection.fuse(payload.materials()).isPresent();
        }
        if (changed) PLAYER_CARDS.markChanged(player.getServer());
        List<CardCollectionSnapshot.CardSummary> definitions = new ArrayList<>(SKILLS.cardDefinitions().stream().map(card ->
                new CardCollectionSnapshot.CardSummary(card.id(),card.nameKey(),card.descriptionKey(),card.rarity(),
                        java.util.stream.Stream.concat(card.tags().stream(), java.util.stream.Stream.of("type:" + card.type().name().toLowerCase(Locale.ROOT))).distinct().toList(),card.stars())).toList());
        Set<String> configured = definitions.stream().map(CardCollectionSnapshot.CardSummary::id).collect(java.util.stream.Collectors.toSet());
        SKILLS.definitions().stream().filter(skill -> !configured.contains(skill.id())).forEach(skill -> {
            Map<String,Double> parameters=skill.power()>0?Map.of("power",skill.power()):Map.of();
            var tier=new net.exmo.exworld.battle.card.CardDefinition.StarTier(skill.manaCost(),skill.range(),parameters);
            String description=skill.id().startsWith("exworld:debug_card_")?"skill.exworld.debug_family_"+(((Integer.parseInt(skill.id().substring(skill.id().length()-2))-1)%4)+1)+".description"
                    :skill.nameKey().startsWith("spell.")?skill.nameKey()+".guide":skill.nameKey()+".description";
            definitions.add(new CardCollectionSnapshot.CardSummary(skill.id(),skill.nameKey(),description,"debug",
                    List.of(SKILLS.isDebug(skill.id())?"debug":"standard", "type:skill"),List.of(tier,tier,tier,tier,tier)));
        });
        BattleNetwork.sendCollection(player, new CardCollectionSnapshot((int) Math.min(Integer.MAX_VALUE, net.exmo.exworld.progress.PlayerProgressSystem.vault().balance(player.getServer(), player.getUUID(), net.exmo.exworld.progress.PlayerResourceVault.GOLD)), collection.activeDeck(), List.copyOf(collection.instances()), collection.decks(),definitions));
    }
    public static ServerPlayer player(UUID playerId) { return activeServer == null ? null : activeServer.getPlayerList().getPlayer(playerId); }
    public static boolean isParticipating(UUID entityId) { return PARTICIPATION.containsKey(entityId); }
    public static Optional<BattleId> battleOf(UUID entityId) { return Optional.ofNullable(PARTICIPATION.get(entityId)); }
    public static Optional<BattleSnapshot> snapshotFor(UUID entityId) { return battleOf(entityId).flatMap(ENGINE::snapshot); }
    public static ServerLevel levelFor(BattleSession session) {
        if (activeServer == null || session == null) return null;
        return levelFor(session.request().host().dimension());
    }
    public static ServerLevel levelFor(String dimension) {
        try {
            return activeServer == null ? null : activeServer.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimension)));
        } catch (Exception ignored) {
            return null;
        }
    }
    /** Resolves a battle combatant to its authoritative live entity for compatibility adapters. */
    public static LivingEntity livingEntity(BattleSession session, UUID combatantId) {
        Combatant combatant = session.combatant(combatantId).orElse(null);
        if (combatant == null || activeServer == null) return null;
        if (combatant.playerId() != null) return activeServer.getPlayerList().getPlayer(combatant.playerId());
        ServerLevel level = levelFor(session);
        Entity entity = level == null ? null : level.getEntity(combatantId);
        return entity instanceof LivingEntity living ? living : null;
    }
    public static void expectIronHit(BattleId battleId, UUID actorId, UUID targetId, String skillId) {
        if (activeServer == null) return;
        IronHitWindow.Policy policy = IronHitWindow.policyFor(skillId);
        long lifetime = policy == IronHitWindow.Policy.REPEATING ? 20 * 20L : 20 * 10L;
        PENDING_IRON_HITS.put(new PendingIronKey(actorId, skillId), new PendingIronHit(battleId, actorId, skillId,
                activeServer.getTickCount() + lifetime, new IronHitWindow(policy, targetId)));
    }
    public static Vec3 worldPosition(BattleSession session, BattleCell cell) {
        BattleSnapshot snapshot = session.snapshot();
        return new Vec3(snapshot.arenaOriginX() + cell.x() + .5, cell.floorY() + 1.0, snapshot.arenaOriginZ() + cell.z() + .5);
    }
    /** Resolves a combatant's tactical floor position plus its physical elevation. */
    public static Vec3 worldPosition(BattleSession session, Combatant combatant) {
        return worldPosition(session, combatant.cell()).add(0, combatant.elevation(), 0);
    }
    public static Vec3 aimPosition(BattleSession session, Combatant target, BattleCell targetCell,
                                   SkillDefinition.TargetType targetType) {
        if (activeServer != null && target != null && targetType != SkillDefinition.TargetType.CELL) {
            ServerLevel level = levelFor(session);
            Entity entity = level == null ? null : entityFor(level, target.id());
            if (entity != null) return targetType == SkillDefinition.TargetType.ENEMY
                    ? entity.position().add(0, entity.getBbHeight() * .58, 0) : entity.getEyePosition();
        }
        return worldPosition(session, targetCell).add(0, targetType == SkillDefinition.TargetType.CELL ? .08 : .85, 0);
    }
    public static Vec3 aimPosition(BattleSession session, Combatant target, BattleCell targetCell) {
        return aimPosition(session, target, targetCell, target == null ? SkillDefinition.TargetType.CELL : SkillDefinition.TargetType.ENEMY);
    }
    public static void lockCastAim(LivingEntity actor, Vec3 targetPosition, int durationTicks, boolean selfTarget) {
        if (selfTarget) {
            CAST_AIMS.remove(actor.getUUID());
            actor.setDeltaMovement(Vec3.ZERO);
            actor.hasImpulse = true;
            return;
        }
        aimEntity(actor, targetPosition);
        actor.setDeltaMovement(Vec3.ZERO);
        actor.hasImpulse = true;
        long now = activeServer == null ? 0 : activeServer.getTickCount();
        CAST_AIMS.put(actor.getUUID(), new CastAim(targetPosition, now + Math.max(3, durationTicks + 3L)));
    }
    public static void aimEntity(LivingEntity actor, Vec3 targetPosition) {
        actor.lookAt(EntityAnchorArgument.Anchor.EYES, targetPosition);
        Vec3 delta = targetPosition.subtract(actor.getEyePosition());
        if (delta.lengthSqr() < 1.0E-6) return;
        float yaw = (float) (Mth.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)) * Mth.RAD_TO_DEG);
        actor.setYRot(yaw); actor.setYHeadRot(yaw); actor.yBodyRot = yaw; actor.setXRot(pitch);
    }
    public static void presentTacticalIronEffect(ServerPlayer player, Vec3 target, boolean movement) {
        if (!(player.level() instanceof ServerLevel level)) return;
        presentTacticalIronEffect((LivingEntity) player, target, movement);
    }
    public static void presentTacticalIronEffect(LivingEntity caster, Vec3 target, boolean movement) {
        if (!(caster.level() instanceof ServerLevel level)) return;
        level.sendParticles(movement ? ParticleTypes.SNOWFLAKE : ParticleTypes.ENCHANT,
                target.x,target.y+.6,target.z,movement?28:40,.55,.65,.55,.05);
        level.playSound(null,target.x,target.y,target.z,movement ? net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT
                : net.minecraft.sounds.SoundEvents.EVOKER_CAST_SPELL,
                caster instanceof Enemy ? SoundSource.HOSTILE : SoundSource.PLAYERS,.9F,1.1F);
    }
    public static void spawnBattleVex(BattleSession session, Combatant combatant) {
        if (activeServer == null) return; ServerLevel level=levelFor(session); if(level==null)return;
        Vex vex=EntityType.VEX.create(level);if(vex==null)return;Vec3 position=worldPosition(session,combatant);
        vex.setUUID(combatant.id());vex.setCustomName(Component.literal(combatant.name()));vex.setNoAi(true);vex.moveTo(position);level.addFreshEntity(vex);
        PARTICIPATION.put(combatant.id(),session.id());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        activeServer = event.getServer();
        if (MonsterProfileRegistry.active().isEmpty()) MonsterProfileRegistry.install(activeServer);
        BATTLE_ITEMS.tick();
        if (!restored) { DungeonSystem.restoreForBattle(event.getServer()); restore(event.getServer()); restored = true; }
        long now = event.getServer().getTickCount();
        if (now % 10 == 0) syncAllParties(event.getServer());
        // Give an already-active AI phase an action opportunity before its timer advances. Action locks make
        // the post-transition pass below idempotent for phases that were already active at tick start.
        ENGINE.sessions().forEach(session -> BattleAiScheduler.tick(session, now));
        // Combat is authoritative foreground simulation. ServerTickEvent.hasTime() only reports whether
        // optional background work fits in the current tick; gating combat on it can starve AI for an
        // entire phase on a normally loaded server.
        ENGINE.tick();
        PENDING_IRON_HITS.values().removeIf(hit -> hit.expiresAt() < event.getServer().getTickCount());
        Set<UUID> onlinePlayers = event.getServer().getPlayerList().getPlayers().stream()
                .map(ServerPlayer::getUUID).collect(java.util.stream.Collectors.toUnmodifiableSet());
        for (BattleSession session : ENGINE.sessions()) {
            session.combatants().forEach(actor -> BATTLE_ITEMS.refreshActiveWeapon(session, actor));
            BattleSnapshot snapshot = session.snapshot();
            if (snapshot.state() == BattleState.REWARD && session.autoSettleDisconnectedPlayers(onlinePlayers)) {
                snapshot = session.snapshot();
            }
            BattleAiScheduler.tick(session, now);
            snapshot = session.snapshot();
            if (snapshot.state().outcome() && AUXILIARIES_CLEARED.add(snapshot.battleId())) {
                ServerLevel battleLevel = levelFor(session);
                if (battleLevel != null) clearBattleAuxiliaries(battleLevel, snapshot.battleId(), snapshot);
            }
            if (snapshot.state().terminal()) continue;
            syncEntities(event.getServer(), session, snapshot);
            int seconds = Math.max(0, snapshot.phaseTicksRemaining() / 20);
            boolean structuralUpdate = snapshot.revision() != LAST_SYNCED_REVISIONS.getOrDefault(snapshot.battleId(), -1L)
                    || seconds != LAST_SYNCED_SECONDS.getOrDefault(snapshot.battleId(), -1);
            boolean movementUpdate = !snapshot.motions().isEmpty();
            if (structuralUpdate) {
                saved(event.getServer()).put(snapshot.battleId().value(), BattleNbtCodec.save(session));
                LAST_SYNCED_REVISIONS.put(snapshot.battleId(), snapshot.revision());
                LAST_SYNCED_SECONDS.put(snapshot.battleId(), seconds);
            }
            if (structuralUpdate || movementUpdate) {
                BattleSnapshot synchronizedSnapshot = snapshot;
                snapshot.combatants().values().stream().map(BattleSnapshot.CombatantView::playerId).filter(Objects::nonNull)
                        .map(id -> event.getServer().getPlayerList().getPlayer(id)).filter(Objects::nonNull)
                        .forEach(player -> BattleNetwork.sendSnapshot(player, synchronizedSnapshot));
            }
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        net.exmo.exworld.monster.MonsterCommands.register(event.getDispatcher());
        event.getDispatcher().register(Commands.literal("exworldbattle")
                .then(Commands.literal("demo").executes(context -> startDemo(context.getSource().getPlayerOrException())))
                .then(Commands.literal("ready").executes(context -> submitReady(context.getSource().getPlayerOrException(), true))
                        .then(Commands.argument("ready", BoolArgumentType.bool())
                                .executes(context -> submitReady(context.getSource().getPlayerOrException(), BoolArgumentType.getBool(context, "ready")))))
                .then(Commands.literal("auto").then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> submitAuto(context.getSource().getPlayerOrException(), BoolArgumentType.getBool(context, "enabled")))))
                .then(Commands.literal("escape").executes(context -> submitEscape(context.getSource().getPlayerOrException()))));
        event.getDispatcher().register(Commands.literal("party")
                .then(Commands.literal("invite").then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .executes(context -> partyInvite(context.getSource().getPlayerOrException(), GameProfileArgument.getGameProfiles(context, "player").iterator().next().getId()))))
                .then(Commands.literal("accept").executes(context -> partyAccept(context.getSource().getPlayerOrException())))
                .then(Commands.literal("decline").executes(context -> partyDecline(context.getSource().getPlayerOrException())))
                .then(Commands.literal("leave").executes(context -> partyLeave(context.getSource().getPlayerOrException())))
                .then(Commands.literal("kick").then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .executes(context -> partyKick(context.getSource().getPlayerOrException(), GameProfileArgument.getGameProfiles(context, "player").iterator().next().getId()))))
                .then(Commands.literal("list").executes(context -> partyList(context.getSource().getPlayerOrException()))));
        event.getDispatcher().register(Commands.literal("fight").requires(source -> source.hasPermission(2))
                .then(Commands.literal("debug")
                        .then(Commands.literal("on").executes(context -> setFightDebug(context.getSource().getPlayerOrException(), true)))
                        .then(Commands.literal("off").executes(context -> setFightDebug(context.getSource().getPlayerOrException(), false)))
                        .then(Commands.literal("status").executes(context -> debugStatus(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("multi_monster")
                                .then(Commands.literal("on").executes(context -> setMultiMonster(context.getSource().getPlayerOrException(), true)))
                                .then(Commands.literal("off").executes(context -> setMultiMonster(context.getSource().getPlayerOrException(), false)))
                                .then(Commands.literal("status").executes(context -> multiMonsterStatus(context.getSource().getPlayerOrException()))))
                        .then(Commands.literal("start")
                                .executes(context -> startTrainingDebug(context.getSource().getPlayerOrException(), EngagementAdvantage.INITIATIVE))
                                .then(Commands.literal("initiative").executes(context -> startTrainingDebug(context.getSource().getPlayerOrException(), EngagementAdvantage.INITIATIVE)))
                                .then(Commands.literal("ambush").executes(context -> startTrainingDebug(context.getSource().getPlayerOrException(), EngagementAdvantage.PLAYER_AMBUSH)))
                                .then(Commands.literal("attacked").executes(context -> startTrainingDebug(context.getSource().getPlayerOrException(), EngagementAdvantage.ENEMY_AMBUSH)))
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(context -> startDebugBattle(context.getSource().getPlayerOrException(), asLiving(EntityArgument.getEntity(context, "target")), EngagementAdvantage.INITIATIVE))
                                        .then(Commands.literal("initiative").executes(context -> startDebugBattle(context.getSource().getPlayerOrException(), asLiving(EntityArgument.getEntity(context, "target")), EngagementAdvantage.INITIATIVE)))
                                        .then(Commands.literal("ambush").executes(context -> startDebugBattle(context.getSource().getPlayerOrException(), asLiving(EntityArgument.getEntity(context, "target")), EngagementAdvantage.PLAYER_AMBUSH)))
                                        .then(Commands.literal("attacked").executes(context -> startDebugBattle(context.getSource().getPlayerOrException(), asLiving(EntityArgument.getEntity(context, "target")), EngagementAdvantage.ENEMY_AMBUSH)))))
                        .then(Commands.literal("phase").then(Commands.literal("next").executes(context -> debugNextPhase(context.getSource().getPlayerOrException()))))
                        .then(Commands.literal("intro")
                                .then(Commands.literal("skip").executes(context -> debugSkipIntro(context.getSource().getPlayerOrException())))
                                .then(Commands.literal("replay").executes(context -> debugReplayIntro(context.getSource().getPlayerOrException()))))
                        .then(Commands.literal("result")
                                .then(Commands.literal("open").executes(context -> debugOpenResult(context.getSource().getPlayerOrException())))
                                .then(Commands.literal("status").executes(context -> debugResultStatus(context.getSource().getPlayerOrException()))))
                        .then(Commands.literal("finish")
                                .then(Commands.literal("win").executes(context -> debugFinish(context.getSource().getPlayerOrException(), BattleResult.Outcome.VICTORY)))
                                .then(Commands.literal("lose").executes(context -> debugFinish(context.getSource().getPlayerOrException(), BattleResult.Outcome.DEFEAT)))
                                .then(Commands.literal("escape").executes(context -> debugFinish(context.getSource().getPlayerOrException(), BattleResult.Outcome.ESCAPED)))
                                .then(Commands.literal("abort").executes(context -> debugFinish(context.getSource().getPlayerOrException(), BattleResult.Outcome.ABORTED))))
                          .then(Commands.literal("warrior_blade").executes(context -> giveWarriorBlade(context.getSource().getPlayerOrException())))
                          .then(Commands.literal("consumables").executes(context -> giveBattleConsumables(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("cards")
                                .then(Commands.literal("open").executes(context -> openCardCollection(context.getSource().getPlayerOrException())))
                                .then(Commands.literal("seed").executes(context -> seedDebugCards(context.getSource().getPlayerOrException())))
                                .then(Commands.literal("grant_all").executes(context -> grantAllCards(context.getSource().getPlayerOrException())))
                                .then(Commands.literal("reset_starter").executes(context -> resetStarterCards(context.getSource().getPlayerOrException())))
                                .then(Commands.literal("list").executes(context -> listDebugCards(context.getSource().getPlayerOrException())))
                                .then(Commands.literal("give").then(Commands.argument("card", StringArgumentType.word()).suggests((context, builder) -> SharedSuggestionProvider.suggest(SKILLS.allCardIds(), builder))
                                        .executes(context -> giveCard(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "card"), 1))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 999))
                                                .executes(context -> giveCard(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "card"), IntegerArgumentType.getInteger(context, "amount"))))))
                                .then(Commands.literal("deck")
                                        .then(Commands.literal("show").executes(context -> showDeck(context.getSource().getPlayerOrException())))
                                        .then(Commands.literal("clear").executes(context -> clearDeck(context.getSource().getPlayerOrException())))
                                        .then(Commands.literal("add").then(Commands.argument("card", StringArgumentType.word()).suggests((context, builder) -> SharedSuggestionProvider.suggest(SKILLS.allCardIds(), builder))
                                                .executes(context -> addDeckCard(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "card")))))
                                        .then(Commands.literal("remove").then(Commands.argument("card", StringArgumentType.word()).suggests((context, builder) -> SharedSuggestionProvider.suggest(SKILLS.allCardIds(), builder))
                                                .executes(context -> removeDeckCard(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "card")))))))));
    }

    public static CommandReceipt submit(ServerPlayer sender, BattleCommand command) {
        if (!Objects.equals(sender.getUUID(), ENGINE.session(command.battleId()).flatMap(s -> s.combatant(command.actorId()))
                .map(Combatant::playerId).orElse(null))) {
            CommandReceipt rejected = CommandReceipt.rejected(command.commandId(), "battle.command.not_owner", -1);
            sender.displayClientMessage(Component.translatable(rejected.reason()), true);
            return rejected;
        }
        ENGINE.session(command.battleId()).ifPresent(session -> session.combatant(command.actorId()).ifPresent(actor -> BATTLE_ITEMS.refreshActiveWeapon(session, actor)));
        CommandReceipt receipt = ENGINE.submit(command);
        ENGINE.snapshot(command.battleId()).ifPresent(snapshot -> BattleNetwork.sendSnapshot(sender, snapshot));
        if (!receipt.accepted()) sender.displayClientMessage(Component.translatable(receipt.reason()), true);
        return receipt;
    }

    @SubscribeEvent public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BattleNetwork.sendEquipment(player, PLAYER_EQUIPMENT.slots(player.getServer(), player.getUUID()));
            syncParty(player);
            DungeonSystem.onPlayerLogin(player);
            Optional<BattleSnapshot> snapshot=snapshotFor(player.getUUID());
            if(snapshot.isPresent()) {
                Optional<BattleSession> session = battleOf(player.getUUID()).flatMap(ENGINE::session);
                if (session.isPresent()) {
                    attachPlayerSystems(session.get(), List.of(player), false);
                    BattleNetwork.sendSnapshot(player, session.get().snapshot());
                } else BattleNetwork.sendSnapshot(player,snapshot.get());
            }
            else returnFromTicket(player);
        }
    }
    @SubscribeEvent public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) snapshotFor(player.getUUID()).flatMap(snapshot -> snapshot.combatants().values().stream()
                .filter(view -> player.getUUID().equals(view.playerId())).findFirst()).ifPresent(view -> {
            ENGINE.submit(new BattleCommand.SetAutoBattle(PARTICIPATION.get(player.getUUID()), snapshotFor(player.getUUID()).orElseThrow().revision(),
                    UUID.randomUUID(), view.id(), true));
        });
    }

    private static int partyInvite(ServerPlayer inviter, UUID targetId) {
        ServerPlayer target=inviter.getServer().getPlayerList().getPlayer(targetId);if(target==null){inviter.sendSystemMessage(Component.translatable("party.exworld.offline"));return 0;}
        var result=PARTIES.invite(inviter.getUUID(),inviter.getGameProfile().getName(),target.getUUID(),target.getGameProfile().getName(),inviter.getServer().getTickCount());
        if(result==DebugPartyManager.Result.OK){inviter.sendSystemMessage(Component.translatable("party.exworld.invited",target.getName()));target.sendSystemMessage(Component.translatable("party.exworld.invite_received",inviter.getName()));syncParty(inviter);syncParty(target);return 1;}
        inviter.sendSystemMessage(Component.translatable("party.exworld.error."+result.name().toLowerCase(Locale.ROOT)));return 0;
    }
    private static int partyAccept(ServerPlayer player){var result=PARTIES.accept(player.getUUID(),player.getServer().getTickCount());if(result==DebugPartyManager.Result.OK){broadcastParty(player,Component.translatable("party.exworld.joined",player.getName()));syncAllParties(player.getServer());return 1;}player.sendSystemMessage(Component.translatable("party.exworld.error."+result.name().toLowerCase(Locale.ROOT)));return 0;}
    private static int partyDecline(ServerPlayer player){var result=PARTIES.decline(player.getUUID());player.sendSystemMessage(Component.translatable(result==DebugPartyManager.Result.OK?"party.exworld.declined":"party.exworld.error.no_invite"));return result==DebugPartyManager.Result.OK?1:0;}
    private static int partyLeave(ServerPlayer player){List<UUID> before=PARTIES.members(player.getUUID());var result=PARTIES.leave(player.getUUID());if(result!=DebugPartyManager.Result.OK){player.sendSystemMessage(Component.translatable("party.exworld.error."+result.name().toLowerCase(Locale.ROOT)));syncParty(player);return 0;}before.stream().map(id->player.getServer().getPlayerList().getPlayer(id)).filter(Objects::nonNull).forEach(member->member.sendSystemMessage(Component.translatable("party.exworld.left",player.getName())));syncAllParties(player.getServer());return 1;}
    private static int partyKick(ServerPlayer leader,UUID target){var result=PARTIES.kick(leader.getUUID(),target);if(result==DebugPartyManager.Result.OK){ServerPlayer kicked=leader.getServer().getPlayerList().getPlayer(target);if(kicked!=null)kicked.sendSystemMessage(Component.translatable("party.exworld.kicked"));syncAllParties(leader.getServer());return 1;}leader.sendSystemMessage(Component.translatable("party.exworld.error."+result.name().toLowerCase(Locale.ROOT)));return 0;}
    private static int partyList(ServerPlayer player){List<UUID> members=PARTIES.members(player.getUUID());player.sendSystemMessage(Component.translatable("party.exworld.list",members.size(),DebugPartyManager.MAX_MEMBERS));player.sendSystemMessage(Component.literal(String.join(", ",members.stream().map(PARTIES::name).toList())));return members.size();}
    private static void broadcastParty(ServerPlayer player,Component message){PARTIES.members(player.getUUID()).stream().map(id->player.getServer().getPlayerList().getPlayer(id)).filter(Objects::nonNull).forEach(member->member.sendSystemMessage(message));}
    private static void syncAllParties(MinecraftServer server){server.getPlayerList().getPlayers().forEach(BattleSystem::syncParty);}
    private static void syncParty(ServerPlayer viewer){UUID leader=PARTIES.leader(viewer.getUUID());List<PartySnapshot.Member> members=new ArrayList<>();for(UUID id:PARTIES.members(viewer.getUUID())){ServerPlayer member=viewer.getServer().getPlayerList().getPlayer(id);boolean online=member!=null;float health=online?member.getHealth():0,maxHealth=online?member.getMaxHealth():1,mana=online?MagicData.getPlayerMagicData(member).getMana():0,maxMana=online?(float)member.getAttributeValue(AttributeRegistry.MAX_MANA):1;double distance=online&&member.level()==viewer.level()?member.distanceTo(viewer):-1;members.add(new PartySnapshot.Member(id,online?member.getGameProfile().getName():PARTIES.name(id),id.equals(leader),online,health,maxHealth,mana,maxMana,distance));}BattleNetwork.sendParty(viewer,new PartySnapshot(members));}

    private static int setMultiMonster(ServerPlayer player,boolean enabled){if(enabled)MULTI_MONSTER_DISABLED.remove(player.getUUID());else MULTI_MONSTER_DISABLED.add(player.getUUID());player.sendSystemMessage(Component.translatable(enabled?"fight.exworld.multi_monster_on":"fight.exworld.multi_monster_off"));return 1;}
    private static int multiMonsterStatus(ServerPlayer player){boolean enabled=!MULTI_MONSTER_DISABLED.contains(player.getUUID());player.sendSystemMessage(Component.translatable("fight.exworld.multi_monster_status",enabled));return enabled?1:0;}

    @SubscribeEvent public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        snapshotFor(player.getUUID()).ifPresent(snapshot -> snapshot.combatants().values().stream()
                .filter(view -> player.getUUID().equals(view.playerId())).findFirst().ifPresent(view -> {
            if (view.downed()) player.setHealth(Math.max(1.0F, player.getHealth()));
        }));
    }

    @SubscribeEvent public static void onToss(ItemTossEvent event) {
        if (!isParticipating(event.getPlayer().getUUID())) return;
        event.setCanceled(true);
        if (!event.getEntity().getItem().isEmpty()) event.getPlayer().getInventory().placeItemBackInInventory(event.getEntity().getItem());
    }
    @SubscribeEvent public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) { if (isParticipating(event.getEntity().getUUID())) event.setCanceled(true); }
    @SubscribeEvent public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) { if (isParticipating(event.getEntity().getUUID())) event.setCanceled(true); }
    @SubscribeEvent public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) { if (isParticipating(event.getEntity().getUUID())) event.setCanceled(true); }
    @SubscribeEvent public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) { if (isParticipating(event.getEntity().getUUID())) event.setCanceled(true); }
    @SubscribeEvent public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) { if (isParticipating(event.getEntity().getUUID())) event.setCanceled(true); }
    @SubscribeEvent public static void onDamage(LivingIncomingDamageEvent event) {
        if (isParticipating(event.getEntity().getUUID())) {
            if (BattleDamageContext.active()) return;
            Entity source = responsibleAttacker(event.getSource());
            String sourceSkill = event.getSource() instanceof SpellDamageSource spellDamage
                    ? "iron:" + spellDamage.spell().getSpellId() : null;
            PendingIronHit pending = source == null ? null : sourceSkill == null
                    ? PENDING_IRON_HITS.values().stream().filter(hit -> hit.actorId().equals(source.getUUID())).findFirst().orElse(null)
                    : PENDING_IRON_HITS.get(new PendingIronKey(source.getUUID(), sourceSkill));
            long tick = activeServer == null ? 0 : activeServer.getTickCount();
            if (pending != null && pending.accepts(event.getEntity().getUUID(), tick)) {
                BattleSession session = ENGINE.session(pending.battleId()).orElse(null);
                boolean applied = session != null && BattleDamageContext.call(event.getSource(), () ->
                        session.resolveExternalHit(pending.actorId(), event.getEntity().getUUID(), pending.skillId(), event.getAmount()));
                if (applied) {
                    pending.mark(event.getEntity().getUUID(), tick);
                    if (pending.complete()) PENDING_IRON_HITS.remove(new PendingIronKey(pending.actorId(), pending.skillId()));
                }
            }
            event.setCanceled(true); return;
        }
        Entity attacker = responsibleAttacker(event.getSource());
        if (attacker != null && isParticipating(attacker.getUUID())) { event.setCanceled(true); return; }
        if (event.getEntity() instanceof ServerPlayer player && attacker instanceof LivingEntity enemy
                && fightDebugEnabled(player) && validDebugEnemy(player, enemy)) {
            event.setCanceled(true);
            startDebugBattle(player, enemy, EngagementAdvantage.fromWorldAttacker(attacker.getUUID(), player.getUUID()));
        } else if (attacker instanceof ServerPlayer player && event.getEntity() instanceof LivingEntity enemy
                && fightDebugEnabled(player) && validDebugEnemy(player, enemy)) {
            event.setCanceled(true);
            startDebugBattle(player, enemy, EngagementAdvantage.fromWorldAttacker(attacker.getUUID(), player.getUUID()));
        }
    }

    private static Entity responsibleAttacker(net.minecraft.world.damagesource.DamageSource source) {
        Entity causing = source.getEntity();
        if (causing instanceof Projectile projectile && projectile.getOwner() != null) return projectile.getOwner();
        if (causing != null) return causing;
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile && projectile.getOwner() != null) return projectile.getOwner();
        return direct;
    }
    @SubscribeEvent public static void onBattleDeath(LivingDeathEvent event) {
        if (!BattleDamageContext.active() || !isParticipating(event.getEntity().getUUID())) return;
        BattleDamageContext.markFatal();
        event.setCanceled(true);
        event.getEntity().setHealth(1.0F);
    }
    @SubscribeEvent public static void onBattleHeal(LivingHealEvent event) {
        if (isParticipating(event.getEntity().getUUID()) && !BattleDamageContext.active()) event.setCanceled(true);
    }
    @SubscribeEvent public static void onBattleEffectAdded(MobEffectEvent.Added event) {
        LivingEntity living=event.getEntity();BattleId battleId=PARTICIPATION.get(living.getUUID());if(battleId==null)return;
        ENGINE.session(battleId).ifPresent(session->importTurnEffect(session,living,event.getEffectInstance()));
    }
    @SubscribeEvent public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || (event.getLevel().dimension() != BATTLE_LEVEL && event.getLevel().dimension() != DungeonSystem.DUNGEON_LEVEL)) return;
        if (event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity) { event.setCanceled(true); return; }
        tagBattleAuxiliary(event.getEntity());
    }
    @SubscribeEvent public static void onServerStopped(ServerStoppedEvent event) {
        MonsterProfileRegistry.clear(event.getServer());
        activeServer = null; restored = false; PARTICIPATION.clear(); RETURNS.clear(); PREVIOUS_GAME_MODES.clear();
        LAST_SYNCED_REVISIONS.clear(); LAST_SYNCED_SECONDS.clear(); STARTING_ENCOUNTERS.clear();
        FIGHT_DEBUG_ENABLED.clear(); MULTI_MONSTER_DISABLED.clear(); PARTIES.clear(); ENGINE.clear(); CAST_AIMS.clear();
        PENDING_IRON_HITS.clear(); AUXILIARIES_CLEARED.clear();
    }

    private static int startDemo(ServerPlayer player) {
        PLAYER_CARDS.ensureStarterDeck(player.getServer(), player.getUUID());
        Zombie enemy = EntityType.ZOMBIE.create(player.level());
        if (enemy == null) return 0;
        enemy.setCustomName(Component.literal("Training Zombie")); enemy.moveTo(player.getX() + 2, player.getY(), player.getZ());
        player.level().addFreshEntity(enemy);
        return startDebugBattle(player, enemy, EngagementAdvantage.INITIATIVE);
    }

    private static int setFightDebug(ServerPlayer player, boolean enabled) {
        if (enabled) FIGHT_DEBUG_ENABLED.add(player.getUUID());
        else FIGHT_DEBUG_ENABLED.remove(player.getUUID());
        player.sendSystemMessage(Component.translatable(enabled ? "fight.exworld.debug_enabled" : "fight.exworld.debug_disabled"));
        return 1;
    }

    private static boolean fightDebugEnabled(ServerPlayer player) { return FIGHT_DEBUG_ENABLED.contains(player.getUUID()); }

    private static boolean validDebugEnemy(ServerPlayer player, LivingEntity entity) {
        return entity != player && !(entity instanceof ServerPlayer) && entity.isAlive() && !isParticipating(entity.getUUID())
                && entity instanceof Enemy;
    }

    private static LivingEntity asLiving(Entity entity) {
        if (entity instanceof LivingEntity living) return living;
        throw new IllegalArgumentException("fight target must be a living entity");
    }

    private static LivingEntity aimedLiving(ServerPlayer player) {
        Vec3 start = player.getEyePosition(); Vec3 end = start.add(player.getLookAngle().scale(32));
        var hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player.level(), player, start, end,
                player.getBoundingBox().expandTowards(player.getLookAngle().scale(32)).inflate(1), entity -> entity instanceof LivingEntity && entity != player);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static int startTrainingDebug(ServerPlayer player, EngagementAdvantage advantage) {
        LivingEntity target = aimedLiving(player);
        boolean spawned = false;
        if (target == null) {
            Zombie training = EntityType.ZOMBIE.create(player.level());
            if (training == null) { player.sendSystemMessage(Component.translatable("fight.exworld.no_target")); return 0; }
            training.setCustomName(Component.literal("Training Zombie")); training.moveTo(player.getX() + 2, player.getY(), player.getZ());
            player.level().addFreshEntity(training); target = training; spawned = true;
        }
        int result = startDebugBattle(player, target, advantage);
        if (spawned && result == 0 && target.isAlive()) target.discard();
        return result;
    }

    private static synchronized int startDebugBattle(ServerPlayer player, LivingEntity enemy, EngagementAdvantage advantage) {
        if (!validDebugEnemy(player, enemy)) return 0;
        List<ServerPlayer> players=PARTIES.members(player.getUUID()).stream().map(id->player.getServer().getPlayerList().getPlayer(id)).filter(Objects::nonNull).toList();
        if(players.isEmpty()||players.stream().anyMatch(member->isParticipating(member.getUUID())||STARTING_ENCOUNTERS.contains(member.getUUID())))return 0;
        players.forEach(member->STARTING_ENCOUNTERS.add(member.getUUID()));
        List<LivingEntity> enemies=new ArrayList<>();enemies.add(enemy);List<LivingEntity> spawnedExtras=new ArrayList<>();
        BattleSession startedSession=null;
        try {
            MinecraftServer server = player.getServer(); ServerLevel battleLevel = server.getLevel(BATTLE_LEVEL);
            if (battleLevel == null) { player.sendSystemMessage(Component.translatable("battle.exworld.missing_dimension")); return 0; }
            players.forEach(member->PLAYER_CARDS.ensureStarterDeck(server,member.getUUID()));
            if(advantage==EngagementAdvantage.ENEMY_AMBUSH&&!MULTI_MONSTER_DISABLED.contains(player.getUUID()))spawnExtraAmbushers(enemy,players.size(),server.getTickCount(),enemies,spawnedExtras);
            EncounterRequest request = FightDebugEncounter.create(players,enemies,advantage,PLAYER_CARDS,
                    server.overworld().getGameTime() ^ player.getUUID().getLeastSignificantBits() ^ enemy.getUUID().getMostSignificantBits());
            BattleId battleId = ENGINE.startEncounter(request); BattleSession session = ENGINE.session(battleId).orElseThrow();startedSession=session;
            attachPlayerSystems(session, players, true);
            buildArena(battleLevel, session.snapshot());
            session.combatants().forEach(combatant->PARTICIPATION.put(combatant.id(),battleId));
            for(ServerPlayer member:players){PARTICIPATION.put(member.getUUID(),battleId);EncounterRequest.ReturnPoint point=request.returnPoints().get(member.getUUID());RETURNS.put(member.getUUID(),point);GameType previous=member.gameMode.getGameModeForPlayer();PREVIOUS_GAME_MODES.put(member.getUUID(),previous);returnTickets(server).put(member.getUUID(),point,previous);member.setGameMode(GameType.ADVENTURE);Vec3 pos=worldPosition(session,session.combatant(member.getUUID()).orElseThrow().cell());member.teleportTo(battleLevel,pos.x,pos.y,pos.z,Set.of(),90,45);}
            for(LivingEntity hostile:enemies)if(moveOrCloneEnemy(hostile,battleLevel,session)==null){abortStart(players,session);return 0;}
            for(ServerPlayer member:players){BattleNetwork.sendSnapshot(member,session.snapshot());member.sendSystemMessage(Component.translatable("fight.exworld.started_party",players.size(),enemies.size(),advantage.name(),session.snapshot().activeFaction()));}
            return 1;
        } catch (Exception error) {
            Exworld.LOGGER.error("Could not start fight debug encounter", error);
            if(startedSession!=null)abortStart(players,startedSession);
            player.sendSystemMessage(Component.translatable("fight.exworld.start_failed", error.getMessage())); return 0;
        } finally { players.forEach(member->STARTING_ENCOUNTERS.remove(member.getUUID()));if(!isParticipating(player.getUUID()))spawnedExtras.stream().filter(Entity::isAlive).forEach(Entity::discard); }
    }

    private static void spawnExtraAmbushers(LivingEntity template,int partySize,long seed,List<LivingEntity> enemies,List<LivingEntity> spawned){Random random=new Random(seed^template.getUUID().getLeastSignificantBits());int extras=random.nextInt(Math.min(3,Math.max(1,partySize))+1);for(int i=0;i<extras;i++){Entity entity=template.getType().create(template.level());if(!(entity instanceof LivingEntity living))continue;living.moveTo(template.getX()+2+i,template.getY(),template.getZ()+((i&1)==0?2:-2));living.setYRot(template.getYRot());template.level().addFreshEntity(living);enemies.add(living);spawned.add(living);}}

    private static Entity moveOrCloneEnemy(LivingEntity enemy, ServerLevel battleLevel, BattleSession session) {
        Vec3 enemyPos = worldPosition(session, session.combatant(enemy.getUUID()).orElseThrow().cell());
        if (enemy instanceof net.minecraft.world.entity.Mob mob) mob.setNoAi(true);
        Entity moved = enemy.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(battleLevel, enemyPos, Vec3.ZERO,
                enemy.getYRot(), enemy.getXRot(), net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
        return moved;
    }

    private static void abortStart(List<ServerPlayer> players, BattleSession session) {
            ServerLevel battleLevel=levelFor(session);
        session.combatants().stream().filter(combatant->combatant.playerId()==null).forEach(combatant->{if(battleLevel!=null){Entity entity=battleLevel.getEntity(combatant.id());if(entity!=null)entity.discard();}});
        if(battleLevel!=null){clearBattleAuxiliaries(battleLevel,session.id(),session.snapshot());clearArena(battleLevel,session.snapshot());}
        ENGINE.remove(session.id());
        session.combatants().forEach(combatant -> CAST_AIMS.remove(combatant.id()));
        saved(players.getFirst().getServer()).remove(session.id().value());
        LAST_SYNCED_REVISIONS.remove(session.id()); LAST_SYNCED_SECONDS.remove(session.id());
        AUXILIARIES_CLEARED.remove(session.id());
        session.combatants().forEach(combatant -> PARTICIPATION.remove(combatant.id()));
        for(ServerPlayer player:players){PARTICIPATION.remove(player.getUUID());RETURNS.remove(player.getUUID());EncounterRequest.ReturnPoint point = session.request().returnPoints().get(player.getUUID());
        if (point != null) {
            ServerLevel level = player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(point.dimension())));
            if (level != null) player.teleportTo(level, point.x(), point.y(), point.z(), Set.of(), point.yaw(), point.pitch());
        }
        GameType mode = PREVIOUS_GAME_MODES.remove(player.getUUID()); if (mode != null) player.setGameMode(mode);
        returnTickets(player.getServer()).remove(player.getUUID());}
    }

    private static int debugStatus(ServerPlayer player) {
        PlayerCardCollection cards = PLAYER_CARDS.collection(player.getServer(), player.getUUID());
        player.sendSystemMessage(Component.translatable("fight.exworld.status", fightDebugEnabled(player), cards.owned().size(), cards.deck().size()));
        snapshotFor(player.getUUID()).ifPresent(snapshot -> player.sendSystemMessage(Component.translatable("fight.exworld.session_status",
                snapshot.battleId().value().toString(), snapshot.state().name(), snapshot.round(), snapshot.activeFaction(), snapshot.revision())));
        return 1;
    }

    private static int debugNextPhase(ServerPlayer player) {
        BattleSession session = battleOf(player.getUUID()).flatMap(ENGINE::session).orElse(null);
        if (session == null) return 0;
        session.finishActiveFaction(); session.tick();
        player.sendSystemMessage(Component.translatable("fight.exworld.phase_advanced", session.snapshot().activeFaction())); return 1;
    }
    private static int debugSkipIntro(ServerPlayer player) {
        BattleSnapshot snapshot = snapshotFor(player.getUUID()).orElse(null); if (snapshot == null) return 0;
        return submit(player, new BattleCommand.SkipIntro(snapshot.battleId(), snapshot.revision(), UUID.randomUUID(), actorFor(snapshot, player.getUUID()))).accepted() ? 1 : 0;
    }
    private static int debugReplayIntro(ServerPlayer player) {
        BattleSession session = battleOf(player.getUUID()).flatMap(ENGINE::session).orElse(null); if (session == null) return 0;
        session.replayIntro(); return session.state() == BattleState.INTRO ? 1 : 0;
    }
    private static int debugOpenResult(ServerPlayer player) {
        BattleSession session = battleOf(player.getUUID()).flatMap(ENGINE::session).orElse(null); if (session == null) return 0;
        session.openDebugResult(BattleResult.Outcome.VICTORY); return session.state() == BattleState.REWARD ? 1 : 0;
    }
    private static int debugResultStatus(ServerPlayer player) {
        BattleSnapshot snapshot = snapshotFor(player.getUUID()).orElse(null); if (snapshot == null || snapshot.result() == null) return 0;
        var view = snapshot.result().players().get(player.getUUID());
        player.sendSystemMessage(Component.literal("Result=" + snapshot.result().outcome() + " selected=" + (view == null ? "-" : view.selectedCandidate())
                + " confirmed=" + (view != null && view.confirmed()) + " timeout=" + snapshot.result().ticksRemaining())); return 1;
    }
    private static int openCardCollection(ServerPlayer player) {
        cardCollectionAction(player, new CardCollectionActionPayload(CardCollectionActionPayload.Action.REQUEST, 0, null, List.of(), "")); return 1;
    }

    private static int giveWarriorBlade(ServerPlayer player) {
        player.addItem(new ItemStack(ExWorldContent.WARRIOR_BLADE.get()));
        player.sendSystemMessage(Component.translatable("fight.exworld.warrior_blade_granted"));
        return 1;
    }

    private static int giveBattleConsumables(ServerPlayer player) {
        player.addItem(new ItemStack(net.minecraft.world.item.Items.WIND_CHARGE, 8));
        player.addItem(new ItemStack(ExWorldContent.BATTLE_ELIXIR.get(), 4));
        player.sendSystemMessage(Component.translatable("fight.exworld.battle_consumables_granted"));
        return 1;
    }

    private static int debugFinish(ServerPlayer player, BattleResult.Outcome outcome) {
        BattleSession session = battleOf(player.getUUID()).flatMap(ENGINE::session).orElse(null);
        if (session == null) return 0;
        session.finishDebug(outcome); return 1;
    }

    private static int seedDebugCards(ServerPlayer player) {
        PlayerCardCollection cards = PLAYER_CARDS.ensureStarterDeck(player.getServer(), player.getUUID());
        player.sendSystemMessage(Component.translatable("fight.exworld.cards_seeded", cards.owned().size(), cards.deck().size())); return 1;
    }

    private static int grantAllCards(ServerPlayer player) {
        PlayerCardCollection cards = PLAYER_CARDS.grantAllCards(player.getServer(), player.getUUID());
        player.sendSystemMessage(Component.translatable("fight.exworld.cards_granted_all", cards.owned().size(), cards.deck().size())); return 1;
    }

    private static int resetStarterCards(ServerPlayer player) {
        PlayerCardCollection cards = PLAYER_CARDS.resetDebugCardsToStarter(player.getServer(), player.getUUID());
        player.sendSystemMessage(Component.translatable("fight.exworld.cards_reset_starter", cards.owned().size(), cards.deck().size())); return 1;
    }

    private static int listDebugCards(ServerPlayer player) {
        List<String> ids = SKILLS.debugCardIds();
        player.sendSystemMessage(Component.literal("Debug cards (" + ids.size() + "): " + String.join(", ", ids))); return ids.size();
    }

    private static int giveCard(ServerPlayer player, String rawId, int amount) {
        String id = normalizeCardId(rawId);
        try {
            int total = PLAYER_CARDS.grant(player.getServer(), player.getUUID(), id, amount);
            player.sendSystemMessage(Component.translatable("fight.exworld.card_granted", id, amount, total)); return amount;
        } catch (IllegalArgumentException error) { player.sendSystemMessage(Component.literal(error.getMessage())); return 0; }
    }

    private static int showDeck(ServerPlayer player) {
        PlayerCardCollection cards = PLAYER_CARDS.collection(player.getServer(), player.getUUID());
        player.sendSystemMessage(Component.translatable("fight.exworld.deck_header", cards.deck().size(), PlayerCardCollection.MIN_DECK_SIZE,
                PlayerCardCollection.MAX_DECK_SIZE, cards.deckPlayable()));
        player.sendSystemMessage(Component.literal(cards.deck().isEmpty() ? "(empty)" : String.join(", ", cards.deck()))); return cards.deck().size();
    }

    private static int clearDeck(ServerPlayer player) {
        PLAYER_CARDS.clearDeck(player.getServer(), player.getUUID());
        player.sendSystemMessage(Component.translatable("fight.exworld.deck_cleared")); return 1;
    }

    private static int addDeckCard(ServerPlayer player, String rawId) {
        String id = normalizeCardId(rawId);
        try {
            if (!PLAYER_CARDS.addToDeck(player.getServer(), player.getUUID(), id)) {
                player.sendSystemMessage(Component.translatable("fight.exworld.deck_add_failed", id)); return 0;
            }
            player.sendSystemMessage(Component.translatable("fight.exworld.deck_added", id)); return 1;
        } catch (IllegalArgumentException error) { player.sendSystemMessage(Component.literal(error.getMessage())); return 0; }
    }

    private static int removeDeckCard(ServerPlayer player, String rawId) {
        String id = normalizeCardId(rawId);
        if (!PLAYER_CARDS.removeFromDeck(player.getServer(), player.getUUID(), id)) {
            player.sendSystemMessage(Component.translatable("fight.exworld.deck_remove_failed", id)); return 0;
        }
        player.sendSystemMessage(Component.translatable("fight.exworld.deck_removed", id)); return 1;
    }

    private static String normalizeCardId(String rawId) {
        if (rawId.matches("\\d{1,2}")) return "exworld:debug_card_" + String.format(Locale.ROOT, "%02d", Integer.parseInt(rawId));
        return rawId.contains(":") ? rawId : "exworld:" + rawId;
    }

    private static int submitReady(ServerPlayer player, boolean ready) {
        BattleSnapshot snapshot = snapshotFor(player.getUUID()).orElse(null); if (snapshot == null) return 0;
        UUID actor = actorFor(snapshot, player.getUUID());
        return submit(player, new BattleCommand.SetReady(snapshot.battleId(), snapshot.revision(), UUID.randomUUID(), actor, ready)).accepted() ? 1 : 0;
    }
    private static int submitAuto(ServerPlayer player, boolean enabled) {
        BattleSnapshot snapshot = snapshotFor(player.getUUID()).orElse(null); if (snapshot == null) return 0;
        return submit(player, new BattleCommand.SetAutoBattle(snapshot.battleId(), snapshot.revision(), UUID.randomUUID(), actorFor(snapshot, player.getUUID()), enabled)).accepted() ? 1 : 0;
    }
    private static int submitEscape(ServerPlayer player) {
        BattleSnapshot snapshot = snapshotFor(player.getUUID()).orElse(null); if (snapshot == null) return 0;
        return submit(player, new BattleCommand.Escape(snapshot.battleId(), snapshot.revision(), UUID.randomUUID(), actorFor(snapshot, player.getUUID()))).accepted() ? 1 : 0;
    }
    private static UUID actorFor(BattleSnapshot snapshot, UUID playerId) {
        return snapshot.combatants().values().stream().filter(view -> playerId.equals(view.playerId())).map(BattleSnapshot.CombatantView::id).findFirst().orElseThrow();
    }

    private static void syncEntities(MinecraftServer server, BattleSession session, BattleSnapshot snapshot) {
        ServerLevel level = levelFor(session); if (level == null) return;
        for (BattleSnapshot.CombatantView view : snapshot.combatants().values()) {
            Entity entity = view.playerId() == null ? level.getEntity(view.id()) : server.getPlayerList().getPlayer(view.playerId());
            if (!(entity instanceof LivingEntity living)) continue;
            living.getActiveEffects().stream().toList().forEach(effect -> {
                String id=net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).toString();
                if(view.statuses().stream().noneMatch(status->status.id().equals(id)))importTurnEffect(session,living,effect);
            });
            Combatant combatant = session.combatant(view.id()).orElse(null);
            Vec3 pos = combatant == null ? worldPosition(session, view.cell()) : worldPosition(session, combatant);
            boolean moving = snapshot.motions().stream().anyMatch(motion -> motion.actorId().equals(view.id()));
            if (!moving && living.position().distanceToSqr(pos) > .16) living.teleportTo(pos.x, pos.y, pos.z);
            BattleSnapshot.MotionView motion = snapshot.motions().stream().filter(candidate -> candidate.actorId().equals(view.id())).findFirst().orElse(null);
            if (motion != null) faceMovement(living, motion);
            else {
                CastAim castAim = CAST_AIMS.get(view.id());
                if (castAim != null && castAim.expiresAt() >= server.getTickCount()) aimEntity(living, castAim.position());
                else { CAST_AIMS.remove(view.id()); facePreferredTarget(session, snapshot, view, living); }
            }
            if (view.downed()) living.setHealth(Math.max(1.0F, living.getHealth()));
            else session.syncLivingHealth(view.id(), living.getMaxHealth(), living.getHealth());
            if (view.playerId() != null) {
                ServerPlayer player = (ServerPlayer) living;
                MagicData data = MagicData.getPlayerMagicData(player);
                if (Math.abs(data.getMana() - view.mana()) > .01F) ManaMutationContext.run(() -> data.setMana(view.mana()));
            }
        }
    }

    private static void faceMovement(LivingEntity entity, BattleSnapshot.MotionView motion) {
        if (motion.path().isEmpty()) return;
        int segment = Math.min(motion.path().size() - 1, (int) ((long) motion.elapsedTicks() * motion.path().size()
                / Math.max(1, motion.durationTicks())));
        BattleCell from = segment == 0 ? motion.start() : motion.path().get(segment - 1);
        BattleCell to = motion.path().get(segment);
        float yaw = BattleMovementHeading.yawDegrees(from, to);
        entity.setYRot(yaw); entity.setYHeadRot(yaw); entity.yBodyRot = yaw;
    }

    private static void importTurnEffect(BattleSession session,LivingEntity living,net.minecraft.world.effect.MobEffectInstance effect){
        String id=net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).toString();
        int rounds=effect.isInfiniteDuration()?99:Math.max(1,(effect.getDuration()+POTION_TICKS_PER_ROUND-1)/POTION_TICKS_PER_ROUND);
        boolean turnManaged = isTurnManagedEffect(id);
        session.applyExternalStatus(living.getUUID(), turnManaged
                ? new BattleStatus(id, effect.getDescriptionId(), effect.getAmplifier() + 1, rounds,
                effect.getEffect().value().isBeneficial(), false, true, effect.isInfiniteDuration() || effect.getDuration() > 10_000)
                : new BattleStatus(id, effect.getDescriptionId(), effect.getAmplifier() + 1, rounds, effect.getEffect().value().isBeneficial()));
        if(turnManaged)living.removeEffect(effect.getEffect());
    }
    private static boolean isTurnManagedEffect(String id){return id.equals("minecraft:poison")||id.equals("minecraft:wither")
            ||id.equals("minecraft:regeneration")||id.equals("minecraft:slowness");}

    private static void facePreferredTarget(BattleSession session, BattleSnapshot snapshot,
                                            BattleSnapshot.CombatantView actor, LivingEntity entity) {
        BattleSnapshot.CombatantView target = previousHostileTarget(session, snapshot, actor);
        if (target == null) target = nearestHostile(session, snapshot, actor);
        if (target == null) return;
        turnToward(entity, actor.cell(), target.cell());
    }

    private static void turnToward(LivingEntity entity, BattleCell from, BattleCell target) {
        float yaw = BattleFacing.approachYaw(entity.getYRot(), from, target, 12.0F);
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
        entity.yBodyRot = yaw;
        entity.setXRot(BattleFacing.approachPitch(entity.getXRot(), 0.0F, 12.0F));
    }

    private static BattleSnapshot.CombatantView nearestHostile(BattleSession session, BattleSnapshot snapshot, BattleSnapshot.CombatantView actor) {
        return snapshot.combatants().values().stream().filter(candidate -> hostile(session, actor, candidate))
                .min(Comparator.comparingInt(candidate -> actor.cell().distanceTo(candidate.cell()))).orElse(null);
    }

    private static BattleSnapshot.CombatantView previousHostileTarget(BattleSession session, BattleSnapshot snapshot, BattleSnapshot.CombatantView actor) {
        return BattleFacing.skillTargetIdsNewestFirst(snapshot.events(), actor.id()).stream()
                .map(snapshot.combatants()::get)
                .filter(candidate -> hostile(session, actor, candidate))
                .findFirst().orElse(null);
    }

    private static boolean hostile(BattleSession session, BattleSnapshot.CombatantView actor, BattleSnapshot.CombatantView candidate) {
        return candidate != null && !candidate.downed() && !candidate.id().equals(actor.id())
                && session.request().relations().getOrDefault(new EncounterRequest.FactionPair(actor.factionId(), candidate.factionId()), FactionRelation.HOSTILE) == FactionRelation.HOSTILE;
    }

    private static void handleResult(BattleResult result) {
        BattleSession session = ENGINE.session(result.battleId()).orElse(null); if (session == null || activeServer == null) return;
        BattleSnapshot.ResultView resultView = session.snapshot().result();
        if (resultView != null) resultView.players().forEach((playerId, reward) -> {
            Combatant combatant = session.combatants().stream().filter(value -> playerId.equals(value.playerId())).findFirst().orElse(null);
            Collection<String> permanentCards = combatant == null ? Set.of() : combatant.permanentCardAdditions();
            PLAYER_CARDS.awardBattleResult(activeServer, result.battleId().value().toString(), playerId,
                    reward.selectedCandidate(), reward.gold(), permanentCards);
        });
        if (session.request().host().dungeon()) {
            DungeonSystem.onBattleResult(session, result);
        }
        for (var entry : session.request().returnPoints().entrySet()) {
            ServerPlayer player = activeServer.getPlayerList().getPlayer(entry.getKey()); if (player == null) continue;
            boolean returned = session.request().host().dungeon() || returnFromTicket(player);
            if (session.request().host().dungeon()) {
                GameType previous = PREVIOUS_GAME_MODES.remove(player.getUUID());
                if (previous != null) player.setGameMode(previous);
            }
            if(returned) BattleNetwork.sendClear(player, result.outcome().name());
        }
        session.combatants().forEach(combatant -> {
            CAST_AIMS.remove(combatant.id());
            PARTICIPATION.remove(combatant.id());
            if (combatant.playerId() != null) RETURNS.remove(combatant.playerId());
            if (combatant.playerId() == null) {
        ServerLevel level = levelFor(session); if (level != null) { Entity entity = level.getEntity(combatant.id()); if (entity != null) entity.discard(); }
            }
        });
        ServerLevel battleLevel = levelFor(session);
        if (battleLevel != null) { clearBattleAuxiliaries(battleLevel, session.id(), session.snapshot()); if (session.request().host().ownsArena()) clearArena(battleLevel, session.snapshot()); }
        ENGINE.remove(result.battleId());
        PENDING_IRON_HITS.values().removeIf(hit -> hit.battleId().equals(result.battleId()));
        AUXILIARIES_CLEARED.remove(result.battleId());
        saved(activeServer).remove(result.battleId().value());
        LAST_SYNCED_REVISIONS.remove(result.battleId());
        LAST_SYNCED_SECONDS.remove(result.battleId());
    }

    private static void handleBattleEvent(BattleEvent event) {
        if (activeServer == null || event.type() == BattleEvent.Type.MOVE) return;
        BattleId battleId = PARTICIPATION.get(event.actorId());
        BattleSession session = battleId == null ? null : ENGINE.session(battleId).orElse(null);
        ServerLevel level = levelFor(session);
        if (session == null || level == null) return;
        if (event.type() != BattleEvent.Type.SKILL && event.type() != BattleEvent.Type.DAMAGE
                && event.type() != BattleEvent.Type.HEAL && event.type() != BattleEvent.Type.DOWNED) return;
        Entity actor = entityFor(level, event.actorId());
        Entity target = event.targetId() == null ? null : entityFor(level, event.targetId());
        if (event.type() == BattleEvent.Type.DOWNED) {
            Vec3 death = target == null
                    ? event.toCell() == null ? Vec3.ZERO : worldPosition(session, event.toCell()).add(0, .7, 0)
                    : target.position().add(0, target.getBbHeight() * .5, 0);
            level.sendParticles(ParticleTypes.POOF, death.x, death.y, death.z, 34, .45, .65, .45, .08);
            level.sendParticles(ParticleTypes.SOUL, death.x, death.y + .15, death.z, 14, .3, .45, .3, .035);
            level.playSound(null, death.x, death.y, death.z, SoundEvents.GENERIC_EXPLODE,
                    target instanceof Enemy ? SoundSource.HOSTILE : SoundSource.PLAYERS, .55F, 1.35F);
            return;
        }
        boolean ironSpell = event.skillId().startsWith("iron:");
        boolean impactOnly = event.type() == BattleEvent.Type.DAMAGE || event.type() == BattleEvent.Type.HEAL;
        boolean meleeAttack = isMeleeAttack(session, event, target);
        if (meleeAttack && actor instanceof LivingEntity living) {
            if (target != null) living.lookAt(EntityAnchorArgument.Anchor.EYES, target.position().add(0, target.getBbHeight() * .5, 0));
            living.swing(InteractionHand.MAIN_HAND, true);
        }
        if (target instanceof LivingEntity living && event.amount() > 0 && event.type() == BattleEvent.Type.DAMAGE) level.broadcastEntityEvent(living, (byte) 2);
        Vec3 effect = target == null ? actor == null ? Vec3.ZERO : actor.position() : target.position().add(0, target.getBbHeight() * .55, 0);
        boolean melee = isMeleeSkill(session, event);
        if (!ironSpell || event.type() == BattleEvent.Type.HEAL) level.sendParticles(event.type() == BattleEvent.Type.HEAL ? ParticleTypes.HEART : melee ? ParticleTypes.SWEEP_ATTACK : ParticleTypes.ENCHANTED_HIT,
                effect.x, effect.y, effect.z, melee ? 2 : 12, .35, .35, .35, .03);
        playBattleSound(level, event, actor, target, effect, melee, ironSpell);
        if (impactOnly) return;
        Component message = Component.translatable("battle.exworld.cast_broadcast", event.actorName(), event.targetName().isBlank()
                ? Component.translatable("hud.exworld.target_cell") : event.targetName(), Component.translatable(event.skillNameKey()));
        session.snapshot().combatants().values().stream().map(BattleSnapshot.CombatantView::playerId).filter(Objects::nonNull).distinct()
                .map(id -> activeServer.getPlayerList().getPlayer(id)).filter(Objects::nonNull).forEach(player -> player.sendSystemMessage(message));
    }

    private static boolean isMeleeAttack(BattleSession session, BattleEvent event, Entity target) {
        if (event.type() != BattleEvent.Type.SKILL || target == null) return false;
        Combatant actor = session.combatant(event.actorId()).orElse(null);
        return actor != null && actor.playerId() == null && isMeleeSkill(session, event)
                && session.combatant(event.targetId()).map(value -> session.request().relations().getOrDefault(
                new EncounterRequest.FactionPair(actor.factionId(), value.factionId()),
                FactionRelation.HOSTILE) == FactionRelation.HOSTILE).orElse(false);
    }

    private static boolean isMeleeSkill(BattleSession session, BattleEvent event) {
        if (event.type() != BattleEvent.Type.SKILL) return false;
        return session.skill(event.skillId())
                .map(definition -> definition.targetType() == SkillDefinition.TargetType.ENEMY && definition.range() <= 1)
                .orElseGet(() -> event.skillId().equals("exworld:basic_attack") || event.skillId().equals("exworld:guarded_strike")
                        || event.skillId().matches("exworld:debug_card_(0[159]|1[37]|2[159]|3[13])"));
    }

    private static void playBattleSound(ServerLevel level, BattleEvent event, Entity actor, Entity target, Vec3 effect,
                                        boolean melee, boolean ironSpell) {
        Vec3 origin = event.type() == BattleEvent.Type.SKILL && actor != null ? actor.position() : effect;
        var sound = switch (event.type()) {
            case SKILL -> melee ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.EVOKER_CAST_SPELL;
            case DAMAGE -> SoundEvents.GENERIC_HURT;
            case HEAL -> SoundEvents.AMETHYST_BLOCK_CHIME;
            default -> null;
        };
        if (sound == null) return;
        float volume = event.type() == BattleEvent.Type.SKILL && ironSpell ? .7F : 1.35F;
        float pitch = event.type() == BattleEvent.Type.HEAL ? 1.35F : event.type() == BattleEvent.Type.DAMAGE ? .9F : 1.0F;
        SoundSource source = target instanceof Enemy ? SoundSource.HOSTILE : SoundSource.PLAYERS;
        level.playSound(null, origin.x, origin.y, origin.z, sound, source, volume, pitch);
    }

    private static Entity entityFor(ServerLevel level, UUID combatantId) {
        Combatant combatant = ENGINE.sessions().stream().flatMap(session -> session.combatant(combatantId).stream()).findFirst().orElse(null);
        if (combatant != null && combatant.playerId() != null) return activeServer.getPlayerList().getPlayer(combatant.playerId());
        return level.getEntity(combatantId);
    }

    private static final class PendingIronHit {
        private final BattleId battleId; private final UUID actorId; private final String skillId; private final long expiresAt;
        private final IronHitWindow window;
        private PendingIronHit(BattleId battleId, UUID actorId, String skillId, long expiresAt, IronHitWindow window) {
            this.battleId=battleId;this.actorId=actorId;this.skillId=skillId;this.expiresAt=expiresAt;this.window=window;
        }
        BattleId battleId(){return battleId;} UUID actorId(){return actorId;}
        String skillId(){return skillId;} long expiresAt(){return expiresAt;}
        boolean accepts(UUID actual,long tick){return window.accepts(actual,tick);}
        void mark(UUID actual,long tick){window.record(actual,tick);}
        boolean complete(){return window.complete();}
    }

    private record PendingIronKey(UUID actorId, String skillId) {}
    private record CastAim(Vec3 position, long expiresAt) {}

    private static void buildArena(ServerLevel level, BattleSnapshot snapshot) {
        if (level == null) return;
        clearBattleAuxiliaries(level, snapshot.battleId(), snapshot);
        int minX = snapshot.arenaOriginX(), minZ = snapshot.arenaOriginZ(), size = snapshot.arenaSize();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = 0; x < size; x++) for (int z = 0; z < size; z++) {
            level.setBlockAndUpdate(cursor.set(minX + x, 64, minZ + z), ((x + z) & 1) == 0 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState());
        }
    }

    private static final String AUXILIARY_TAG_PREFIX = "exworld.battle.auxiliary.";

    private static void tagBattleAuxiliary(Entity entity) {
        ResourceLocation typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (!"irons_spellbooks".equals(typeId.getNamespace()) || PARTICIPATION.containsKey(entity.getUUID())) return;
        ENGINE.sessions().stream().filter(session -> insideArena(session.snapshot(), entity.position())).findFirst()
                .ifPresent(session -> {
                    if (session.snapshot().state().outcome() || session.snapshot().state() == BattleState.REWARD
                            || session.snapshot().state() == BattleState.RETURNING || session.snapshot().state().terminal()) entity.discard();
                    else entity.addTag(AUXILIARY_TAG_PREFIX + session.id().value());
                });
    }

    private static void clearBattleAuxiliaries(ServerLevel level, BattleId battleId, BattleSnapshot snapshot) {
        String sessionTag = AUXILIARY_TAG_PREFIX + battleId.value();
        List<Entity> remove = new ArrayList<>();
        level.getAllEntities().forEach(entity -> {
            ResourceLocation typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            boolean tagged = entity.getTags().contains(sessionTag);
            boolean staleIronEffect = "irons_spellbooks".equals(typeId.getNamespace())
                    && !PARTICIPATION.containsKey(entity.getUUID()) && insideArena(snapshot, entity.position());
            if (tagged || staleIronEffect) remove.add(entity);
        });
        remove.forEach(Entity::discard);
    }

    private static boolean insideArena(BattleSnapshot snapshot, Vec3 position) {
        AABB bounds = new AABB(snapshot.arenaOriginX() - 4, -64, snapshot.arenaOriginZ() - 4,
                snapshot.arenaOriginX() + snapshot.arenaSize() + 4, 384,
                snapshot.arenaOriginZ() + snapshot.arenaSize() + 4);
        return bounds.contains(position);
    }

    private static void clearArena(ServerLevel level, BattleSnapshot snapshot) {
        int minX = snapshot.arenaOriginX(), minZ = snapshot.arenaOriginZ(), size = snapshot.arenaSize();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = 0; x < size; x++) for (int z = 0; z < size; z++)
            level.setBlock(cursor.set(minX + x, 64, minZ + z), Blocks.AIR.defaultBlockState(), 3);
    }

    private static void registerIron(String spellId, int range, int mana, SkillDefinition.TargetType target, int level) {
        registerIron(spellId, range, mana, target, level, spellId.endsWith(":heal") ? 10 : 8);
    }
    private static void registerIron(String spellId, int range, int mana, SkillDefinition.TargetType target, int level, double power) {
        String adapterId = "iron:" + spellId;
        SKILLS.registerAdapter(adapterId, new IronSpellAdapter(spellId));
        String path = ResourceLocation.parse(spellId).getPath();
        SKILLS.registerDebug(new SkillDefinition(adapterId, "spell." + spellId.replace(':', '.'),
                "irons_spellbooks:textures/gui/spell_icons/" + path + ".png", adapterId, mana, range, target,
                target != SkillDefinition.TargetType.SELF, false, false, power, level));
    }

    private static BattleSavedData saved(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(BattleSavedData.FACTORY, "exworld_battle_sessions");
    }
    private static BattleReturnSavedData returnTickets(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(BattleReturnSavedData.FACTORY,"exworld_battle_returns");}
    private static boolean returnFromTicket(ServerPlayer player){
        var data=returnTickets(player.getServer());var ticket=data.get(player.getUUID()).orElse(null);if(ticket==null)return false;
        var point=ticket.point();ServerLevel level=player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(point.dimension())));if(level==null)level=player.getServer().overworld();
        player.teleportTo(level,point.x(),point.y(),point.z(),Set.of(),point.yaw(),point.pitch());player.setGameMode(ticket.mode());
        PREVIOUS_GAME_MODES.remove(player.getUUID());data.remove(player.getUUID());return true;
    }
    private static void restore(MinecraftServer server) {
        ServerLevel battleLevel = server.getLevel(BATTLE_LEVEL);
        for (net.minecraft.nbt.CompoundTag tag : saved(server).sessions()) {
            try {
                var arena = ENGINE.arena(tag.getString("arena")).orElseGet(() -> {
                    if (!tag.hasUUID("dungeon_run")) throw new IllegalStateException("Unknown arena " + tag.getString("arena"));
                    ArenaDefinition restoredArena = DungeonSystem.battleArena(tag.getUUID("dungeon_run"), tag.getString("dungeon_room"))
                            .orElseGet(() -> ArenaDefinition.flat(tag.getString("arena"), 18, tag.getInt("host_floor_y")));
                    ENGINE.registerArena(restoredArena); return restoredArena;
                });
                BattleSession session = BattleNbtCodec.load(tag, arena, SKILLS.snapshot(), EFFECTS); ENGINE.restore(session);
                List<ServerPlayer> onlinePlayers = session.combatants().stream().map(Combatant::playerId).filter(Objects::nonNull)
                        .map(id -> server.getPlayerList().getPlayer(id)).filter(Objects::nonNull).toList();
                attachPlayerSystems(session, onlinePlayers, false);
                BattleSnapshot snapshot = session.snapshot();
                ServerLevel hostLevel = levelFor(session);
                session.combatants().forEach(combatant -> {
                    PARTICIPATION.put(combatant.id(), session.id());
                    if (combatant.playerId() != null) PARTICIPATION.put(combatant.playerId(), session.id());
                    if (hostLevel != null && combatant.playerId() == null && hostLevel.getEntity(combatant.id()) instanceof Mob mob) mob.setNoAi(true);
                    if (combatant.playerId() != null && activeServer.getPlayerList().getPlayer(combatant.playerId()) instanceof ServerPlayer player) {
                        PREVIOUS_GAME_MODES.putIfAbsent(player.getUUID(), player.gameMode.getGameModeForPlayer());
                        player.setGameMode(GameType.ADVENTURE);
                    }
                });
        if (battleLevel != null && session.request().host().ownsArena()) buildArena(battleLevel, snapshot);
            } catch (Exception error) {
                Exworld.LOGGER.error("Could not restore battle {}; leaving it saved for recovery", tag.hasUUID("id") ? tag.getUUID("id") : "unknown", error);
            }
        }
    }

    public static void registerEvents() {
        NeoForge.EVENT_BUS.register(BattleSystem.class); NeoForge.EVENT_BUS.register(IronManaEvents.class);
        NeoForge.EVENT_BUS.addListener(net.exmo.exworld.battle.data.BattleDataReloadListener::register);
    }
}
