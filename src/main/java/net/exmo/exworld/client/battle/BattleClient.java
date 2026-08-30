package net.exmo.exworld.client.battle;

import com.mojang.blaze3d.platform.InputConstants;
import net.exmo.exworld.Exworld;
import net.exmo.exworld.battle.api.BattleSnapshot;
import net.exmo.exworld.battle.api.BattleEvent;
import net.exmo.exworld.battle.compat.StylizedDamagePresence;
import net.exmo.exworld.battle.model.BattleCell;
import net.exmo.exworld.battle.model.BattleFacing;
import net.exmo.exworld.battle.model.BattleMovementHeading;
import net.exmo.exworld.client.perspective.CameraProfile;
import net.exmo.exworld.client.perspective.DungeonPerspective;
import net.exmo.exworld.mixin.client.GameRendererAccessor;
import net.exmo.exworld.client.battle.skybox.BattleSkyboxController;
import net.exmo.exworld.network.BattleIntentPayload;
import net.exmo.exworld.subtitle.SubtitlePayload;
import net.exmo.exworld.subtitle.client.SubtitleHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.exmo.exworld.client.battle.screen.BattleResultScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.Set;
import java.util.UUID;
import java.util.Optional;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class BattleClient {
    private static final ResourceLocation CAMERA_OWNER = ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "battle");
    private static final Set<ResourceLocation> HIDDEN_LAYERS = Set.of(VanillaGuiLayers.CROSSHAIR, VanillaGuiLayers.HOTBAR,
            VanillaGuiLayers.JUMP_METER, VanillaGuiLayers.EXPERIENCE_BAR, VanillaGuiLayers.EXPERIENCE_LEVEL,
            VanillaGuiLayers.PLAYER_HEALTH, VanillaGuiLayers.ARMOR_LEVEL, VanillaGuiLayers.FOOD_LEVEL,
            VanillaGuiLayers.VEHICLE_HEALTH, VanillaGuiLayers.AIR_LEVEL, VanillaGuiLayers.SELECTED_ITEM_NAME,
            VanillaGuiLayers.CHAT);
    private static volatile BattleSnapshot snapshot;
    private static UUID selectedCard;
    private static UUID glowingEntity;
    private static boolean glowingEntityWasGlowing;
    private static long lastSeenEvent;
    private static final long CAST_GLOW_TICKS = 10L;
    private static final int ALLY_CAST_GLOW_COLOR = 0x4A9DFF;
    private static final int HOSTILE_CAST_GLOW_COLOR = 0xFF4D5A;
    private static final List<FloatingNumber> floatingNumbers = new ArrayList<>();
    private static final List<CardCastAnimation> cardCastAnimations = new ArrayList<>();
    private static final List<SkillCaption> skillCaptions = new ArrayList<>();
    private static final java.util.Map<UUID,BattleMotionClock> motionClocks = new java.util.HashMap<>();
    private static final java.util.Map<UUID, CastGlow> castGlows = new java.util.HashMap<>();
    private static PileOverlay pileOverlay=PileOverlay.NONE;
    private static boolean itemsOpen;
    private static int selectedInventorySlot = -1;
    private static String selectedItemId = "";
    private static UUID voiceHighlightedCard;
    private static int voiceHighlightAge;
    private static BattleCameraPan cameraPan = BattleCameraPan.CENTERED;

    private BattleClient() {}
    public static void register() {
        NeoForge.EVENT_BUS.addListener(BattleClient::hideVanillaHud);
        NeoForge.EVENT_BUS.addListener(BattleClient::interaction);
        NeoForge.EVENT_BUS.addListener(BattleClient::mouse);
        NeoForge.EVENT_BUS.addListener(BattleClient::scroll);
        NeoForge.EVENT_BUS.addListener(BattleClient::screenOpening);
        NeoForge.EVENT_BUS.addListener(BattleClient::key);
    }
    public static boolean active() { return snapshot != null; }
    public static BattleSnapshot snapshot() { return snapshot; }
    public static UUID selectedCard() { return selectedCard; }
    public static boolean shouldGlow(Entity entity) {
        if (!active() || entity == null) return false;
        BattleSnapshot.CombatantView combatant = snapshot.combatants().get(entity.getUUID());
        return combatant != null && !combatant.downed()
                && (entity.getUUID().equals(glowingEntity) || shouldCastGlow(entity));
    }

    /** Direct glow-state hook used by the entity mixin, independent of the vanilla outline gate. */
    public static boolean shouldCastGlow(Entity entity) {
        if (!active() || entity == null) return false;
        BattleSnapshot.CombatantView combatant = snapshot.combatants().get(entity.getUUID());
        return combatant != null && !combatant.downed() && isCastGlowActive(entity.getUUID());
    }

    /** Returns the temporary outline color for a unit that has just used a card, or zero for normal outlines. */
    public static int castGlowColor(Entity entity) {
        if (!shouldCastGlow(entity)) return 0;
        CastGlow glow = castGlows.get(entity.getUUID());
        return glow != null && glow.expiresAt() > clientGameTime() ? glow.color() : 0;
    }
    public static boolean shouldHide(Entity entity) {
        BattleSnapshot current = snapshot;
        BattleSnapshot.CombatantView view = current == null || entity == null ? null : current.combatants().get(entity.getUUID());
        return view != null && view.downed();
    }
    public static void selectCard(UUID card) {
        if (itemTargeting()) return;
        selectedCard = card != null && card.equals(selectedCard) ? null : card;
    }

    public static void install(BattleSnapshot value) {
        BattleSnapshot previous = snapshot;
        BattleSnapshot.CardView selectedBefore = selectedCardView();
        snapshot = value;
        if (previous == null || !previous.battleId().equals(value.battleId())) cameraPan = BattleCameraPan.CENTERED;
        BattleSkyboxController.onBattleStarted(value);
        if (previous != null && value.state() == net.exmo.exworld.battle.model.BattleState.FACTION_PHASE
                && (previous.state() != net.exmo.exworld.battle.model.BattleState.FACTION_PHASE
                || previous.round() != value.round() || !previous.activeFaction().equals(value.activeFaction()))) {
            boolean local = value.combatants().values().stream().anyMatch(view -> view.playerId() != null
                    && Minecraft.getInstance().player != null && view.playerId().equals(Minecraft.getInstance().player.getUUID())
                    && view.factionId().equals(value.activeFaction()));
            SubtitleHud.enqueue(new SubtitlePayload(
                    Component.translatable(local ? "hud.exworld.your_turn" : "hud.exworld.faction_turn", value.activeFaction()),
                    Component.translatable("hud.exworld.battle_round", value.round()), 35, local ? 0xFF6EDAA2 : 0xFFF06068));
        }
        long clientTick=Minecraft.getInstance().level==null?0:Minecraft.getInstance().level.getGameTime();
        value.motions().forEach(motion->motionClocks.computeIfAbsent(motion.actorId(),ignored->new BattleMotionClock()).sync(motion.elapsedTicks(),clientTick));
        motionClocks.keySet().removeIf(id->value.motions().stream().noneMatch(motion->motion.actorId().equals(id)));
        BattleSnapshot.CombatantView actor = localCombatant();
        if (actor == null || actor.hand().stream().noneMatch(card -> card.instanceId().equals(selectedCard) && card.playable())) selectedCard = null;
        float distance = Math.max(18, value.arenaSize() * .85F);
        DungeonPerspective.setOverride(CAMERA_OWNER, new CameraProfile(true, 45, 62, distance, false));
        net.exmo.exworld.client.camera.AdvancedCameraDirector.syncBattleIntro(value);
        Minecraft minecraft = Minecraft.getInstance();
        if (value.state() == net.exmo.exworld.battle.model.BattleState.REWARD
                && !(minecraft.screen instanceof BattleResultScreen)) minecraft.setScreen(new BattleResultScreen());
        else if (value.state() != net.exmo.exworld.battle.model.BattleState.REWARD
                && minecraft.screen instanceof BattleResultScreen) minecraft.setScreen(null);
        if (previous == null) {
            if (!value.events().isEmpty()) lastSeenEvent = value.events().getLast().sequence();
        } else {
            value.events().stream().filter(event -> event.sequence() > lastSeenEvent)
                    .forEach(event -> present(event, selectedBefore));
            if (!value.events().isEmpty()) lastSeenEvent = Math.max(lastSeenEvent, value.events().getLast().sequence());
        }
    }
    public static void clear(String outcome) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof BattleResultScreen) minecraft.setScreen(null);
        BattleSkyboxController.onBattleEnded();
        clearGlow(); clearCastGlows(); floatingNumbers.clear(); cardCastAnimations.clear();skillCaptions.clear();motionClocks.clear();pileOverlay=PileOverlay.NONE; itemsOpen = false; selectedInventorySlot = -1; selectedItemId = ""; voiceHighlightedCard = null; voiceHighlightAge = 0; cameraPan = BattleCameraPan.CENTERED; BattleHud.resetLog(); snapshot = null; selectedCard = null; lastSeenEvent = 0;
        net.exmo.exworld.client.camera.AdvancedCameraDirector.clear(); DungeonPerspective.clearOverride(CAMERA_OWNER);
    }

    public static void tick() {
        BattleSkyboxController.tick();
        if (!active()) { clearGlow(); clearCastGlows(); return; }
        tickCastGlows();
        tickPresentation();
        tickWalkAnimations();
        if (voiceHighlightedCard != null && ++voiceHighlightAge > 20) voiceHighlightedCard = null;
        BattleSnapshot.CombatantView hovered = hoveredCombatant();
        UUID next = hovered == null ? null : hovered.id();
        if (!java.util.Objects.equals(next, glowingEntity)) {
            clearGlow();
            if (next != null) {
                Entity entity = entity(next);
                if (entity != null) {
                    glowingEntity = next;
                    CastGlow castGlow = castGlows.get(next);
                    glowingEntityWasGlowing = castGlow == null ? entity.isCurrentlyGlowing() : castGlow.wasGlowing();
                    entity.setGlowingTag(true);
                }
            }
        }
    }
    /** Called from KeyboardInput after vanilla has sampled movement keys; battle never moves the player. */
    public static void panCamera(float forward, float strafe) {
        if (snapshot == null) return;
        cameraPan = BattleCameraPan.move(cameraPan, DungeonPerspective.cameraYaw(), forward, strafe, snapshot.arenaSize());
    }
    public static BattleCameraPan cameraPan() { return cameraPan; }

    public static BattleSnapshot.CombatantView localCombatant() {
        Minecraft minecraft = Minecraft.getInstance();
        if (snapshot == null || minecraft.player == null) return null;
        return snapshot.combatants().values().stream().filter(view -> minecraft.player.getUUID().equals(view.playerId())).findFirst().orElse(null);
    }

    public static void setReady(boolean ready) { send(BattleIntentPayload.Kind.READY, null, null, ready, null); }
    public static void setAuto(boolean enabled) { send(BattleIntentPayload.Kind.AUTO_BATTLE, null, null, enabled, null); }
    public static void escape() { send(BattleIntentPayload.Kind.ESCAPE, null, null, false, null); }
    public static boolean itemsOpen() { return itemsOpen; }
    public static void toggleItems() { itemsOpen = !itemsOpen; selectedInventorySlot = -1; selectedItemId = ""; }
    public static int selectedInventorySlot() { return selectedInventorySlot; }
    public static boolean itemTargeting() { return selectedInventorySlot >= 0; }
    public static void chooseItem(int itemIndex) {
        BattleSnapshot.CombatantView actor = localCombatant();
        List<BattleSnapshot.ItemView> items = actor == null ? List.of() : BattleHud.usableItems(actor.items());
        if (itemIndex < 0 || itemIndex >= items.size()) return;
        BattleSnapshot.ItemView item = items.get(itemIndex);
        selectedCard = null;
        selectedInventorySlot = item.inventorySlot(); selectedItemId = item.itemId();
        if ("self".equals(item.targetType())) useSelectedItem(null);
    }
    public static void switchWeapon(int slot) { sendValue(BattleIntentPayload.Kind.SWITCH_WEAPON, Integer.toString(slot)); }
    private static void useSelectedItem(BattleCell target) {
        BattleSnapshot current = snapshot; BattleSnapshot.CombatantView actor = localCombatant();
        if (current == null || actor == null || selectedInventorySlot < 0) return;
        BattleCell cell = target == null ? actor.cell() : target;
        PacketDistributor.sendToServer(new BattleIntentPayload(current.battleId(), current.revision(), UUID.randomUUID(), actor.id(),
                BattleIntentPayload.Kind.USE_ITEM, cell.x(), cell.z(), cell.floorY(), null, null, false, selectedItemId, selectedInventorySlot));
        selectedInventorySlot = -1; selectedItemId = "";
    }
    public static void skipIntro() { send(BattleIntentPayload.Kind.SKIP_INTRO, null, null, false, null); }
    public static void selectReward(String candidate) { sendValue(BattleIntentPayload.Kind.SELECT_REWARD, candidate); }
    public static void confirmResult() { sendValue(BattleIntentPayload.Kind.CONFIRM_RESULT, ""); }
    public static Component selectCardFromVoice(String matchedId) {
        BattleSnapshot current = snapshot;
        BattleSnapshot.CombatantView actor = localCombatant();
        if (current == null || actor == null) return Component.translatable("voice.exworld.no_battle");
        if (current.state() != net.exmo.exworld.battle.model.BattleState.FACTION_PHASE
                || actor.downed() || !actor.factionId().equals(current.activeFaction())
                || current.readyPlayers().contains(actor.playerId())) {
            return Component.translatable("voice.exworld.card_unavailable");
        }
        BattleSnapshot.CardView card = findVoiceCard(actor, matchedId);
        if (card == null) return Component.translatable("voice.exworld.card_not_found", matchedId);
        if (!card.playable()) {
            return Component.translatable(card.manaCost() > actor.mana()
                    ? "voice.exworld.card_no_mana" : "voice.exworld.card_unavailable");
        }
        selectedCard = card.instanceId();
        voiceHighlightedCard = card.instanceId();
        voiceHighlightAge = 0;
        return Component.translatable("voice.exworld.card_selected", Component.translatable(card.nameKey()));
    }

    public static Component voiceEndTurn() {
        BattleSnapshot current = snapshot;
        BattleSnapshot.CombatantView actor = localCombatant();
        if (current == null || actor == null) return Component.translatable("voice.exworld.no_battle");
        if (current.state() != net.exmo.exworld.battle.model.BattleState.FACTION_PHASE
                || actor.downed() || !actor.factionId().equals(current.activeFaction())
                || current.readyPlayers().contains(actor.playerId())) {
            return Component.translatable("voice.exworld.turn_unavailable");
        }
        setReady(true);
        selectedCard = null;
        return Component.translatable("voice.exworld.turn_ended");
    }

    public static Component voiceSkipIntro() {
        BattleSnapshot current = snapshot;
        BattleSnapshot.CombatantView actor = localCombatant();
        if (current == null || actor == null) return Component.translatable("voice.exworld.no_battle");
        if (current.state() != net.exmo.exworld.battle.model.BattleState.INTRO || actor.downed()) {
            return Component.translatable("voice.exworld.intro_unavailable");
        }
        skipIntro();
        return Component.translatable("voice.exworld.intro_skipped");
    }

    public static boolean isVoiceHighlighted(UUID cardId) {
        return voiceHighlightedCard != null && voiceHighlightedCard.equals(cardId);
    }

    public static float voiceHighlightProgress() {
        return voiceHighlightedCard == null ? 0 : Mth.clamp(voiceHighlightAge / 20.0F, 0, 1);
    }

    private static BattleSnapshot.CardView findVoiceCard(BattleSnapshot.CombatantView actor, String matchedId) {
        String exact = matchedId == null ? "" : matchedId.trim();
        BattleSnapshot.CardView found = actor.hand().stream().filter(card -> card.skillId().equalsIgnoreCase(exact)).findFirst().orElse(null);
        if (found != null) return found;
        String path = exact;
        int separator = path.indexOf(':');
        if (separator >= 0) path = path.substring(separator + 1);
        String finalPath = path.toLowerCase(java.util.Locale.ROOT);
        List<BattleSnapshot.CardView> matches = actor.hand().stream().filter(card -> {
            String skill = card.skillId();
            int split = skill.indexOf(':');
            return (split >= 0 ? skill.substring(split + 1) : skill).equalsIgnoreCase(finalPath);
        }).toList();
        return matches.size() == 1 ? matches.getFirst() : null;
    }
    public static void clickWorld() {
        BattleSnapshot.CombatantView actor = localCombatant(); BattleCell cell = hoveredCell();
        if (actor == null || cell == null) return;
        if (itemTargeting()) { useSelectedItem(cell); return; }
        BattleSnapshot.CardView card = actor.hand().stream().filter(view -> view.instanceId().equals(selectedCard)).findFirst().orElse(null);
        if (card == null) {
            BattleTargeting.Intent intent = BattleTargeting.move(cell);
            send(BattleIntentPayload.Kind.MOVE, intent.cell(), null, false, intent.targetId());
            return;
        }
        List<BattleTargeting.Occupant> occupants = snapshot.combatants().values().stream()
                .filter(view -> !view.downed()).map(view -> new BattleTargeting.Occupant(view.id(), view.cell())).toList();
        BattleTargeting.Intent intent = BattleTargeting.skill(cell, card.targetType(), actor.id(), occupants);
        send(BattleIntentPayload.Kind.USE_SKILL, intent.cell(), selectedCard, false, intent.targetId());
    }

    /** Returns the unit actually occupying the cursor's tactical cell, independent of rendered model bounds. */
    public static BattleSnapshot.CombatantView combatantAt(BattleCell cell) {
        BattleSnapshot current = snapshot;
        if (current == null || cell == null) return null;
        return current.combatants().values().stream().filter(view -> !view.downed()
                && BattleTargeting.sameCell(view.cell(), cell)).findFirst().orElse(null);
    }

    /** Mirrors the server's grid route for immediate movement feedback; the server remains authoritative on click. */
    public static Optional<List<BattleCell>> movementPreviewPath() {
        BattleSnapshot current = snapshot;
        BattleSnapshot.CombatantView actor = localCombatant();
        BattleCell destination = hoveredCell();
        if (current == null || actor == null || destination == null || selectedCardView() != null || itemTargeting()) return Optional.empty();
        List<BattleCell> unavailable = new ArrayList<>(current.blockedCells());
        current.combatants().values().stream().filter(view -> !view.downed() && !view.id().equals(actor.id()))
                .map(BattleSnapshot.CombatantView::cell).forEach(unavailable::add);
        current.motions().stream().filter(motion -> !motion.actorId().equals(actor.id())).forEach(motion -> {
            unavailable.add(motion.start()); unavailable.addAll(motion.path());
        });
        return BattlePathPreview.findPath(current.arenaSize(), actor.cell(), destination, actor.movementRemaining(), unavailable);
    }

    /** Indicates whether the selected card can target this tactical cell before rendering its cursor cost. */
    public static boolean selectedCardCanTarget(BattleCell cell) {
        BattleSnapshot current = snapshot;
        BattleSnapshot.CombatantView actor = localCombatant();
        BattleSnapshot.CardView card = selectedCardView();
        if (current == null || actor == null || card == null || cell == null || !card.playable()
                || current.state() != net.exmo.exworld.battle.model.BattleState.FACTION_PHASE
                || actor.downed() || !actor.factionId().equals(current.activeFaction())
                || current.readyPlayers().contains(actor.playerId())) return false;
        List<BattleTargeting.Occupant> occupants = current.combatants().values().stream().filter(view -> !view.downed())
                .map(view -> new BattleTargeting.Occupant(view.id(), view.cell())).toList();
        BattleTargeting.Intent intent = BattleTargeting.skill(cell, card.targetType(), actor.id(), occupants);
        if (card.targetType() == net.exmo.exworld.battle.skill.SkillDefinition.TargetType.SELF) return true;
        if (!card.reaches(actor.cell(), intent.cell())) return false;
        if (card.targetType() == net.exmo.exworld.battle.skill.SkillDefinition.TargetType.CELL) return true;
        BattleSnapshot.CombatantView target = current.combatants().get(intent.targetId());
        if (target == null || target.downed()) return false;
        boolean friendly = actor.factionId().equals(target.factionId());
        return card.targetType() == net.exmo.exworld.battle.skill.SkillDefinition.TargetType.ALLY ? friendly : !friendly;
    }

    private static void send(BattleIntentPayload.Kind kind, BattleCell cell, UUID card, boolean flag, UUID target) {
        BattleSnapshot current = snapshot; BattleSnapshot.CombatantView actor = localCombatant(); if (current == null || actor == null) return;
        BattleCell resolved = cell == null ? actor.cell() : cell;
        PacketDistributor.sendToServer(new BattleIntentPayload(current.battleId(), current.revision(), UUID.randomUUID(), actor.id(), kind,
                resolved.x(), resolved.z(), resolved.floorY(), card, target, flag, ""));
    }
    private static void sendValue(BattleIntentPayload.Kind kind, String value) {
        BattleSnapshot current = snapshot; BattleSnapshot.CombatantView actor = localCombatant(); if (current == null || actor == null) return;
        int weaponSlot = kind == BattleIntentPayload.Kind.SWITCH_WEAPON ? Integer.parseInt(value) : -1;
        PacketDistributor.sendToServer(new BattleIntentPayload(current.battleId(), current.revision(), UUID.randomUUID(), actor.id(), kind,
                actor.cell().x(), actor.cell().z(), actor.cell().floorY(), null, null, false, value, weaponSlot));
    }

    public static BattleCell hoveredCell() {
        CursorRay ray = cursorRay(); BattleSnapshot current = snapshot;
        if (ray == null || current == null || ray.direction().y >= -1.0E-4) return null;
        double t = (65.02 - ray.start().y) / ray.direction().y; if (t <= 0) return null;
        int x = (int) Math.floor(ray.start().x + ray.direction().x * t) - current.arenaOriginX();
        int z = (int) Math.floor(ray.start().z + ray.direction().z * t) - current.arenaOriginZ();
        return x >= 0 && z >= 0 && x < current.arenaSize() && z < current.arenaSize() ? new BattleCell(x, z, 64) : null;
    }

    public static BattleSnapshot.CombatantView hoveredCombatant() {
        return combatantAt(hoveredCell());
    }

    public static BattleSnapshot.CardView selectedCardView() {
        BattleSnapshot.CombatantView actor = localCombatant();
        return actor == null ? null : actor.hand().stream().filter(card -> card.instanceId().equals(selectedCard)).findFirst().orElse(null);
    }

    public static List<FloatingNumber> floatingNumbers() { return List.copyOf(floatingNumbers); }
    public static List<CardCastAnimation> cardCastAnimations() { return List.copyOf(cardCastAnimations); }
    public static List<SkillCaption> skillCaptions() { return List.copyOf(skillCaptions); }
    public static PileOverlay pileOverlay(){return pileOverlay;}
    public static void togglePile(PileOverlay pile){pileOverlay=pileOverlay==pile?PileOverlay.NONE:pile;}

    private static void present(BattleEvent event, BattleSnapshot.CardView selectedBefore) {
        if (event.type() == BattleEvent.Type.SKILL) triggerCastGlow(event);
        BattleSnapshot.CombatantView local = localCombatant();
        if (event.type() == BattleEvent.Type.SKILL && local != null && event.actorId().equals(local.id())
                && selectedBefore != null && selectedBefore.skillId().equals(event.skillId())) {
            cardCastAnimations.add(new CardCastAnimation(event.sequence(), selectedBefore, 0));
        }
        if (event.type() == BattleEvent.Type.SKILL && event.actorId() != null && !event.skillNameKey().isBlank())
            skillCaptions.add(new SkillCaption(event.sequence(), event.actorId(), event.skillNameKey(), 0));
        if (!StylizedDamagePresence.active() && (event.type() == BattleEvent.Type.DAMAGE || event.type() == BattleEvent.Type.HEAL)
                && event.targetId() != null && event.amount() > 0) {
            Entity target = entity(event.targetId());
            Vec3 position = target == null ? event.toCell() == null ? Vec3.ZERO : worldPosition(event.toCell())
                    : target.position().add(0, target.getBbHeight() + .35, 0);
            floatingNumbers.add(new FloatingNumber(event.sequence(), event.targetId(), position, event.amount(), event.type() == BattleEvent.Type.HEAL, false, 0));
            if (event.type() == BattleEvent.Type.DAMAGE && target instanceof LivingEntity living) { living.hurtTime = 10; living.hurtDuration = 10; }
        }
    }

    private static void tickPresentation() {
        floatingNumbers.replaceAll(number -> new FloatingNumber(number.sequence(), number.targetId(), number.position(), number.amount(), number.healing(), number.critical(), number.age() + 1));
        floatingNumbers.removeIf(number -> number.age() > 34);
        cardCastAnimations.replaceAll(animation -> new CardCastAnimation(animation.sequence(), animation.card(), animation.age() + 1));
        cardCastAnimations.removeIf(animation -> animation.age() > 18);
        skillCaptions.replaceAll(caption -> new SkillCaption(caption.sequence(),caption.actorId(),caption.nameKey(),caption.age()+1));
        skillCaptions.removeIf(caption -> caption.age() > 42);
    }

    private static void tickWalkAnimations() {
        BattleSnapshot current = snapshot;
        if (current == null) return;
        for (BattleSnapshot.CombatantView combatant : current.combatants().values()) {
            Entity entity = entity(combatant.id());
            if (!(entity instanceof LivingEntity living)) continue;
            BattleSnapshot.MotionView motion = current.motions().stream().filter(view -> view.actorId().equals(combatant.id())).findFirst().orElse(null);
            if (motion != null) {
                faceMovement(living, motion);
                living.walkAnimation.update(1.0F, .45F);
            } else {
                living.walkAnimation.setSpeed(0.0F);
                facePreferredTarget(living, combatant, current);
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

    private static void facePreferredTarget(LivingEntity entity, BattleSnapshot.CombatantView combatant, BattleSnapshot current) {
        BattleSnapshot.CombatantView target = BattleFacing.skillTargetIdsNewestFirst(current.events(), combatant.id()).stream()
                .map(current.combatants()::get)
                .filter(candidate -> hostile(combatant, candidate))
                .findFirst()
                .orElseGet(() -> current.combatants().values().stream()
                        .filter(candidate -> hostile(combatant, candidate))
                        .min(java.util.Comparator.comparingInt(candidate -> combatant.cell().distanceTo(candidate.cell())))
                        .orElse(null));
        if (target != null) {
            float yaw = BattleFacing.approachYaw(entity.getYRot(), combatant.cell(), target.cell(), 12.0F);
            entity.setYRot(yaw); entity.setYHeadRot(yaw); entity.yBodyRot = yaw;
            entity.setXRot(BattleFacing.approachPitch(entity.getXRot(), 0.0F, 12.0F));
        }
    }

    private static boolean hostile(BattleSnapshot.CombatantView actor, BattleSnapshot.CombatantView candidate) {
        return candidate != null && !candidate.downed() && !candidate.id().equals(actor.id())
                && !candidate.factionId().equals(actor.factionId());
    }

    private static Vec3 worldPosition(BattleCell cell) { return worldPosition(cell, java.util.Set.of()); }
    private static Vec3 worldPosition(BattleSnapshot.CombatantView combatant) { return worldPosition(combatant.cell(), combatant.attributes()); }
    private static Vec3 worldPosition(BattleCell cell, java.util.Set<net.exmo.exworld.battle.model.CombatantAttribute> attributes) {
        double elevation = attributes.stream().mapToDouble(net.exmo.exworld.battle.model.CombatantAttribute::elevation).max().orElse(0.0D);
        return new Vec3(snapshot.arenaOriginX() + cell.x() + .5, cell.floorY() + 1.0 + elevation, snapshot.arenaOriginZ() + cell.z() + .5);
    }

    public static Vec3 presentationPosition(UUID actorId, float partialTick) {
        BattleSnapshot current = snapshot;
        if (current == null) return null;
        BattleSnapshot.MotionView motion = current.motions().stream().filter(view -> view.actorId().equals(actorId)).findFirst().orElse(null);
        if (motion == null) {
            BattleSnapshot.CombatantView view = current.combatants().get(actorId);
            return view == null ? null : worldPosition(view);
        }
        BattleMotionClock clock=motionClocks.get(actorId);long clientTick=Minecraft.getInstance().level==null?0:Minecraft.getInstance().level.getGameTime();
        double elapsed=clock==null?motion.elapsedTicks()+partialTick:clock.sample(clientTick,partialTick,motion.durationTicks());
        double scaled=Math.min(1.0,elapsed/motion.durationTicks())*motion.path().size();int index=Math.min(motion.path().size()-1,(int)Math.floor(scaled));
        var segment=new net.exmo.exworld.battle.action.BattleActionTimeline.Segment(index==0?motion.start():motion.path().get(index-1),motion.path().get(index),Math.min(1.0,scaled-index));
        BattleSnapshot.CombatantView combatant = current.combatants().get(actorId);
        if (combatant == null) return null;
        return worldPosition(segment.from(), combatant.attributes())
                .lerp(worldPosition(segment.to(), combatant.attributes()),
                BattleMovementInterpolator.segmentProgress(segment.progress()));
    }

    private static CursorRay cursorRay() {
        Minecraft minecraft = Minecraft.getInstance(); BattleSnapshot current = snapshot;
        if (current == null || minecraft.player == null) return null;
        double width = minecraft.getWindow().getScreenWidth(), height = minecraft.getWindow().getScreenHeight(); if (width <= 0 || height <= 0) return null;
        double ndcX = minecraft.mouseHandler.xpos() / width * 2 - 1, ndcY = 1 - minecraft.mouseHandler.ypos() / height * 2;
        var camera = minecraft.gameRenderer.getMainCamera();
        float fov = net.exmo.exworld.client.perspective.CursorProjection.fovForCursorRay(minecraft.options.fov().get(),
                (float) ((GameRendererAccessor) minecraft.gameRenderer).exworld$renderedFov(camera, camera.getPartialTickTime(), true));
        var direction = net.exmo.exworld.client.perspective.CursorProjection.direction(new org.joml.Vector3f(camera.getLookVector()),
                new org.joml.Vector3f(camera.getLeftVector()), new org.joml.Vector3f(camera.getUpVector()), ndcX, ndcY, width / height, fov);
        return new CursorRay(camera.getPosition(), new Vec3(direction.x, direction.y, direction.z));
    }

    private static Entity entity(UUID id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;
        for (Entity entity : minecraft.level.entitiesForRendering()) if (entity.getUUID().equals(id)) return entity;
        return null;
    }

    private static void clearGlow() {
        if (glowingEntity != null) {
            Entity entity = entity(glowingEntity);
            if (entity != null && !castGlows.containsKey(glowingEntity)) entity.setGlowingTag(glowingEntityWasGlowing);
        }
        glowingEntity = null; glowingEntityWasGlowing = false;
    }

    private static void triggerCastGlow(BattleEvent event) {
        UUID actorId = event.actorId();
        if (actorId == null) return;
        BattleSnapshot.CombatantView actor = snapshot == null ? null : snapshot.combatants().get(actorId);
        if (actor == null || actor.downed()) return;
        Entity entity = entity(actorId);
        CastGlow previous = castGlows.get(actorId);
        boolean wasGlowing = previous != null ? previous.wasGlowing()
                : entity != null && (actorId.equals(glowingEntity) ? glowingEntityWasGlowing : entity.isCurrentlyGlowing());
        boolean ally = localCombatant() != null && localCombatant().factionId().equals(actor.factionId());
        castGlows.put(actorId, new CastGlow(ally ? ALLY_CAST_GLOW_COLOR : HOSTILE_CAST_GLOW_COLOR,
                clientGameTime() + CAST_GLOW_TICKS, wasGlowing));
        if (entity != null) entity.setGlowingTag(true);
    }

    private static void tickCastGlows() {
        long now = clientGameTime();
        var iterator = castGlows.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            CastGlow glow = entry.getValue();
            Entity entity = entity(entry.getKey());
            if (glow.expiresAt() > now) {
                if (entity != null) entity.setGlowingTag(true);
                continue;
            }
            if (entity != null && !entry.getKey().equals(glowingEntity)) entity.setGlowingTag(glow.wasGlowing());
            iterator.remove();
        }
    }

    private static void clearCastGlows() {
        for (var entry : castGlows.entrySet()) {
            Entity entity = entity(entry.getKey());
            if (entity != null) entity.setGlowingTag(entry.getValue().wasGlowing());
        }
        castGlows.clear();
    }

    private static boolean isCastGlowActive(UUID actorId) {
        CastGlow glow = castGlows.get(actorId);
        return glow != null && glow.expiresAt() > clientGameTime();
    }

    private static long clientGameTime() {
        return Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
    }

    private record CursorRay(Vec3 start, Vec3 direction) {}
    public enum PileOverlay{NONE,DRAW,DISCARD}
    public record FloatingNumber(long sequence, UUID targetId, Vec3 position, double amount, boolean healing, boolean critical, int age) {}
    public record CardCastAnimation(long sequence, BattleSnapshot.CardView card, int age) {}
    public record SkillCaption(long sequence, UUID actorId, String nameKey, int age) {}
    private record CastGlow(int color, long expiresAt, boolean wasGlowing) {}

    private static void hideVanillaHud(RenderGuiLayerEvent.Pre event) {
        if (!active() || !HIDDEN_LAYERS.contains(event.getName())) return;
        if (event.getName().equals(VanillaGuiLayers.CHAT) && Minecraft.getInstance().screen instanceof ChatScreen) return;
        event.setCanceled(true);
    }
    private static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!active()) return; event.setCanceled(true); event.setSwingHand(false);
    }
    private static void mouse(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!BattleInputPolicy.capturesMouse(active(), minecraft.screen != null)) return;
        double scale = minecraft.getWindow().getGuiScale();
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getAction() == InputConstants.RELEASE) {
            if (BattleHud.endItemPanelDrag()) event.setCanceled(true);
            return;
        }
        if (event.getAction() != InputConstants.PRESS) return;
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && BattleHud.beginItemPanelDrag(minecraft.mouseHandler.xpos() / scale, minecraft.mouseHandler.ypos() / scale)) {
            event.setCanceled(true);
            return;
        }
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            selectedCard = null;
            selectedInventorySlot = -1; selectedItemId = "";
            pileOverlay=PileOverlay.NONE;
            event.setCanceled(true);
            return;
        }
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            BattleSnapshot.CombatantView actor = localCombatant();
            BattleHud.Hit hit = BattleHud.hitTest(minecraft.mouseHandler.xpos() / scale, minecraft.mouseHandler.ypos() / scale);
            switch (hit.action()) {
                case CARD -> {
                    if (actor != null && hit.cardId() != null) actor.hand().stream()
                            .filter(card -> card.instanceId().equals(hit.cardId()) && card.playable()).findFirst()
                            .ifPresent(card -> selectCard(card.instanceId()));
                }
                case READY -> {
                    if (snapshot.state() == net.exmo.exworld.battle.model.BattleState.INTRO) skipIntro();
                    else if (actor != null && actor.factionId().equals(snapshot.activeFaction()))
                        setReady(actor.playerId() == null || !snapshot.readyPlayers().contains(actor.playerId()));
                }
                case AUTO -> { if (actor != null) setAuto(!actor.autoBattle()); }
                case ESCAPE -> escape();
                case BACKPACK -> toggleItems();
                case ITEM -> chooseItem(hit.index());
                case WEAPON_1 -> switchWeapon(1);
                case WEAPON_2 -> switchWeapon(2);
                case DRAW_PILE -> togglePile(PileOverlay.DRAW);
                case DISCARD_PILE -> togglePile(PileOverlay.DISCARD);
                case LOG -> BattleHud.toggleLog();
                case WORLD -> clickWorld();
                case BLOCKED -> { }
            }
            event.setCanceled(true);
        }
    }
    private static void scroll(InputEvent.MouseScrollingEvent event) {
        if (!active() || Minecraft.getInstance().screen != null) return;
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        double mouseX = event.getMouseX() / scale, mouseY = event.getMouseY() / scale;
        if (BattleHud.scrollLog(mouseX, mouseY, event.getScrollDeltaY())
                || BattleHud.scrollItems(mouseX, mouseY, event.getScrollDeltaY())) event.setCanceled(true);
    }
    private static void screenOpening(ScreenEvent.Opening event) {
        if (active() && event.getNewScreen() instanceof InventoryScreen) event.setCanceled(true);
    }

    public static void key(InputEvent.Key event) {
        if (!active() || event.getAction() != InputConstants.PRESS || Minecraft.getInstance().screen != null) return;
        BattleSnapshot.CombatantView actor = localCombatant(); if (actor == null) return;
        if (event.getKey() == GLFW.GLFW_KEY_R && snapshot.state() == net.exmo.exworld.battle.model.BattleState.INTRO) skipIntro();
        else if (event.getKey() == GLFW.GLFW_KEY_R) setReady(!snapshot.readyPlayers().contains(actor.playerId()));
        else if (event.getKey() == GLFW.GLFW_KEY_B) setAuto(!actor.autoBattle());
        else if (event.getKey() == GLFW.GLFW_KEY_X) escape();
        else if (event.getKey() == GLFW.GLFW_KEY_I) toggleItems();
        else if (event.getKey() == GLFW.GLFW_KEY_1 && Screen.hasControlDown()) switchWeapon(1);
        else if (event.getKey() == GLFW.GLFW_KEY_2 && Screen.hasControlDown()) switchWeapon(2);
        else if (event.getKey() >= GLFW.GLFW_KEY_1 && event.getKey() <= GLFW.GLFW_KEY_9) {
            int index = event.getKey() - GLFW.GLFW_KEY_1;
            if (index < actor.hand().size() && actor.hand().get(index).playable()) selectCard(actor.hand().get(index).instanceId());
        }
    }
}
