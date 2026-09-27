package net.exmo.exworld.mystery;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.battle.BattleSystem;
import net.exmo.exworld.mystery.MysterySave.Participant;
import net.exmo.exworld.npc.entity.UrbanNpc;
import net.exmo.exworld.npc.data.NpcCatalog;
import net.exmo.exworld.npc.data.NpcDocument;
import net.exmo.exworld.npc.data.NpcPlace;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Server authority for one mystery session. No client payload may directly set progress. */
public final class MysteryGame {
    public static final long INTRO_TICKS = 3 * 60 * 20;
    private static final int REWIND_GRACE_TICKS = 60 * 20;
    private static final int CHASE_TICKS = 2 * 60 * 20;

    private MysteryGame() {}

    public static void register() {
        NeoForge.EVENT_BUS.register(MysteryGame.class);
        NeoForge.EVENT_BUS.addListener(MysteryCommands::register);
        NeoForge.EVENT_BUS.addListener(net.exmo.exworld.mystery.blueprint.BlueprintLibrary::registerReload);
        net.exmo.exworld.mystery.blueprint.BlueprintRuntime.register();
    }

    public static boolean assign(ServerPlayer player, Era era, String characterId, String displayName) {
        if (era == Era.NONE || !characterId.matches("[a-z0-9_.-]{1,64}")) return false;
        MysterySave data = MysterySave.get(player.server);
        if (data.phase != GamePhase.LOBBY) return false;
        Participant p = data.players.computeIfAbsent(player.getUUID(), id -> new Participant(id, characterId, displayName, era));
        p.characterId = characterId;
        p.displayName = displayName.isBlank() ? characterId : displayName;
        p.era = era;
        p.role = era == Era.FUTURE ? "illegal" : "commoner";
        p.state = PlayerState.READY;
        captureSpawn(player, p);
        data.changed();
        sync(player);
        return true;
    }

    public static boolean start(MinecraftServer server) {
        MysterySave data = MysterySave.get(server);
        if (data.phase != GamePhase.LOBBY || data.players.values().stream().noneMatch(p ->
                p.era == Era.PAST && server.getPlayerList().getPlayer(p.uuid) != null)) return false;
        try { net.exmo.exworld.mystery.blueprint.BlueprintLibrary.beginGame(server); }
        catch (RuntimeException error) {
            Exworld.LOGGER.error("Mystery blueprint validation prevented game start", error);
            return false;
        }
        data.runId = UUID.randomUUID();
        data.activeCues.clear();
        data.globalCue = null;
        data.tick = 0;
        data.rewindAt = -1;
        data.endingAt = -1;
        data.timelineDayTime = server.overworld().getDayTime();
        data.sealCount = 0;
        data.ritualPerformed = false;
        data.phase = GamePhase.INTRO;
        for (Participant p : data.participants()) {
            p.state = p.era == Era.PAST ? PlayerState.PAST_ACTIVE : PlayerState.FUTURE_ACTIVE;
            p.suspicion = 0;
            p.cycleContacts.clear();
            p.lastClueTick = 0;
            p.hintStage = 0;
            p.lastWorkTick = -1000;
            ServerPlayer player = server.getPlayerList().getPlayer(p.uuid);
            if (player == null) {
                p.needsRestore = true;
                data.activeCues.put(p.uuid, new MysterySave.ActiveCue(data.runId, "identity_reveal", data.tick, 80));
                continue;
            }
            p.needsRestore = false;
            captureSpawn(player, p);
            restore(player, p);
            sync(player);
            cue(player, "identity_reveal", 80);
        }
        data.changed();
        MysteryStoryPresets.ensure(server);
        emit(server, "on_phase_enter", null, "INTRO");
        syncAll(server);
        return true;
    }

    private static void captureSpawn(ServerPlayer player, Participant p) {
        p.dimension = player.serverLevel().dimension().location().toString();
        p.x = player.getX(); p.y = player.getY(); p.z = player.getZ();
        p.yaw = player.getYRot(); p.pitch = player.getXRot();
    }

    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || BattleSystem.isParticipating(player.getUUID())) return;
        MysterySave data = MysterySave.get(player.server);
        if (data.phase != GamePhase.INTRO && data.phase != GamePhase.ACTIVE
                && data.phase != GamePhase.REWIND && data.phase != GamePhase.ENDING) return;
        Participant p = data.participant(player.getUUID());
        if (p == null || (p.state != PlayerState.PAST_ACTIVE && p.state != PlayerState.FUTURE_ACTIVE
                && p.state != PlayerState.FUTURE_DOOMED && p.state != PlayerState.ENDING_CHASE)) return;
        event.setCanceled(true);
        player.setHealth(1.0F);
        player.removeEffect(MobEffects.WITHER);
        player.setGameMode(GameType.SPECTATOR);
        p.state = p.era == Era.PAST ? PlayerState.PAST_DEAD : PlayerState.FUTURE_DEAD;
        cue(player, "memory_wait", 80);
        UUID deathRun = data.runId;
        emit(player.server, "on_player_death", player, p.characterId);
        if (!deathRun.equals(data.runId) || data.phase == GamePhase.FINISHED) return;
        if (p.era == Era.PAST) {
            for (Participant linked : MysteryRules.futureVictims(p, data.participants())) {
                linked.state = PlayerState.FUTURE_DOOMED;
                ServerPlayer future = player.server.getPlayerList().getPlayer(linked.uuid);
                if (future != null) {
                    future.addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * 60 * 20, 1, false, true));
                    cue(future, "linked_death", 90);
                }
            }
            if (data.phase != GamePhase.ENDING && data.participants().stream().filter(value -> value.era == Era.PAST
                    && player.server.getPlayerList().getPlayer(value.uuid) != null)
                    .allMatch(value -> value.state == PlayerState.PAST_DEAD)) {
                data.phase = GamePhase.REWIND;
                data.rewindAt = data.tick + REWIND_GRACE_TICKS;
                emit(player.server, "on_phase_enter", null, "REWIND");
                cueAll(player.server, "rewind", 100);
            }
        }
        if (data.phase == GamePhase.ENDING && data.participants().stream().noneMatch(value -> value.alive()
                && player.server.getPlayerList().getPlayer(value.uuid) != null)) {
            data.sealCount = 0;
            resolveEnding(player.server);
            return;
        }
        data.changed();
        syncAll(player.server);
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        MysterySave data = MysterySave.get(server);
        if (data.phase == GamePhase.LOBBY || data.phase == GamePhase.FINISHED) return;
        data.tick++;
        if (data.phase == GamePhase.INTRO && data.tick >= INTRO_TICKS) {
            data.phase = GamePhase.ACTIVE;
            emit(server, "on_phase_enter", null, "ACTIVE");
            cueAll(server, "hunt_start", 80);
            spawnHunters(server, data);
            syncAll(server);
        }
        if (data.phase == GamePhase.ACTIVE && data.tick % (90 * 20) == 0) spawnHunters(server, data);
        if (data.phase == GamePhase.ENDING && data.tick % 20 == 0) MysteryStoryPresets.ensureConductor(server);
        if (data.phase == GamePhase.ENDING && data.tick % (30 * 20) == 0) spawnEndingThreat(server, data);
        if (data.phase == GamePhase.ENDING && data.endingAt >= 0 && data.tick >= data.endingAt)
            resolveEnding(server);
        if (data.phase == GamePhase.REWIND) {
            boolean doomedAlive = data.participants().stream().anyMatch(p -> p.state == PlayerState.FUTURE_DOOMED);
            if (!doomedAlive && data.tick >= data.rewindAt - REWIND_GRACE_TICKS + 5 * 20) rewind(server);
            else if (data.tick >= data.rewindAt) {
                for (Participant p : data.participants()) if (p.state == PlayerState.FUTURE_DOOMED) {
                    p.state = PlayerState.FUTURE_DEAD;
                    ServerPlayer player = server.getPlayerList().getPlayer(p.uuid);
                    if (player != null) { player.setGameMode(GameType.SPECTATOR); cue(player, "memory_wait", 80); }
                }
                rewind(server);
            }
        }
        for (Participant p : data.participants()) if (p.state == PlayerState.FUTURE_DOOMED) {
            ServerPlayer player = server.getPlayerList().getPlayer(p.uuid);
            if (player != null) {
                MobEffectInstance effect = player.getEffect(MobEffects.WITHER);
                if (effect == null || effect.getAmplifier() < 1 || effect.getDuration() < 40)
                    player.addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * 60 * 20, 1, false, true));
            }
        }
        if (data.tick % 20 == 0) {
            for (Participant p : data.participants()) {
                ServerPlayer player = server.getPlayerList().getPlayer(p.uuid);
                if (player == null) continue;
                if (p.era == Era.FUTURE && p.suspicion > 0 && data.tick % (60 * 20) == 0)
                    p.suspicion--;
                if (p.state == PlayerState.PAST_ACTIVE || p.state == PlayerState.FUTURE_ACTIVE) {
                    int stage = (int)Math.min(3, Math.max(0, (data.tick - p.lastClueTick) / (3 * 60 * 20)));
                    if (stage > p.hintStage) {
                        p.hintStage = stage;
                        hint(player, p);
                    }
                }
            }
            syncAll(server);
        }
        data.changed();
    }

    private static void hint(ServerPlayer player, Participant p) {
        String key = "mystery.exworld.hint." + p.era.name().toLowerCase(Locale.ROOT) + "." + p.hintStage;
        player.displayClientMessage(Component.translatable(key), true);
    }

    public static void rewind(MinecraftServer server) {
        MysterySave data = MysterySave.get(server);
        if (data.phase == GamePhase.LOBBY) return;
        UUID oldRun = data.runId;
        data.cycle++;
        data.runId = UUID.randomUUID();
        data.activeCues.clear();
        data.globalCue = null;
        data.tick = 0;
        data.rewindAt = -1;
        data.endingAt = -1;
        data.sealCount = 0;
        data.ritualPerformed = false;
        data.phase = GamePhase.INTRO;
        for (ServerLevel level : server.getAllLevels()) level.setDayTime(data.timelineDayTime);
        resetRunEntities(server, oldRun);
        resetNpcs(server);
        MysteryStoryPresets.ensure(server);
        for (Participant p : data.participants()) {
            p.state = p.era == Era.PAST ? PlayerState.PAST_ACTIVE : PlayerState.FUTURE_ACTIVE;
            p.suspicion = 0;
            p.cycleContacts.clear();
            p.lastClueTick = 0;
            p.hintStage = 0;
            p.lastWorkTick = -1000;
            ServerPlayer player = server.getPlayerList().getPlayer(p.uuid);
            if (player == null) { p.needsRestore = true; continue; }
            restore(player, p);
            sync(player);
            cue(player, "cycle_recap", 100);
        }
        data.changed();
        emit(server, "on_phase_enter", null, "INTRO");
        syncAll(server);
    }

    private static void restore(ServerPlayer player, Participant p) {
        ResourceLocation dimension = ResourceLocation.tryParse(p.dimension);
        ServerLevel level = dimension == null ? player.server.overworld() :
                player.server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (level == null) level = player.server.overworld();
        player.getInventory().clearContent();
        player.removeAllEffects();
        player.setGameMode(GameType.ADVENTURE);
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.teleportTo(level, p.x, p.y, p.z, Set.of(), p.yaw, p.pitch);
        give(player, "lotm:message_token", 1);
        if (p.era == Era.PAST) give(player, "lotm:knife", 1);
        else give(player, "lotm:coin", 4);
    }

    private static void resetNpcs(MinecraftServer server) {
        NpcCatalog catalog = NpcCatalog.get(server);
        for (ServerLevel level : server.getAllLevels()) {
            List<UrbanNpc> npcs = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) if (entity instanceof UrbanNpc npc) npcs.add(npc);
            for (UrbanNpc npc : npcs) {
                npc.setNodeId(""); npc.setRouteIndex(0); npc.clearMarginal();
                npc.setDialogOpen(false); npc.setTrading(false); npc.setHealth(npc.getMaxHealth());
                NpcDocument document = catalog.document(npc.documentId()).orElse(null);
                NpcPlace home = document == null ? null : document.place(document.homePlaceId()).orElse(null);
                if (home != null && home.dimension().equals(level.dimension().location().toString()))
                    npc.moveTo(home.x(), home.y(), home.z(), (float)home.yaw(), 0);
            }
        }
    }

    private static void resetRunEntities(MinecraftServer server, UUID oldRun) {
        for (ServerLevel level : server.getAllLevels()) {
            List<Entity> removed = new ArrayList<>();
            for (Entity entity : level.getAllEntities())
                if (entity.getPersistentData().hasUUID("exworld_mystery_run")
                        && entity.getPersistentData().getUUID("exworld_mystery_run").equals(oldRun)) removed.add(entity);
            for (Entity entity : removed) entity.discard();
        }
    }

    private static void spawnHunters(MinecraftServer server, MysterySave data) {
        for (Participant p : data.participants()) {
            if (p.state != PlayerState.PAST_ACTIVE) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(p.uuid);
            if (player == null) continue;
            long nearby = player.serverLevel().getEntitiesOfClass(Husk.class, player.getBoundingBox().inflate(32),
                    mob -> mob.getPersistentData().hasUUID("exworld_mystery_run")
                            && mob.getPersistentData().getUUID("exworld_mystery_run").equals(data.runId)).size();
            if (nearby >= 2) continue;
            Husk hunter = EntityType.HUSK.create(player.serverLevel());
            if (hunter == null) continue;
            hunter.moveTo(player.getX() + 12, player.getY(), player.getZ() + 12, 0, 0);
            hunter.getPersistentData().putUUID("exworld_mystery_run", data.runId);
            hunter.setCustomName(Component.literal("追猎者"));
            hunter.setTarget(player);
            player.serverLevel().addFreshEntity(hunter);
        }
    }

    public static void spawnEndingThreat(MinecraftServer server, MysterySave data) {
        for (Participant p : data.participants()) {
            if (p.state != PlayerState.ENDING_CHASE) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(p.uuid);
            if (player == null) continue;
            if (!player.serverLevel().getEntitiesOfClass(Vex.class, player.getBoundingBox().inflate(32),
                    vex -> vex.getPersistentData().hasUUID("exworld_mystery_run")
                            && vex.getPersistentData().getUUID("exworld_mystery_run").equals(data.runId)).isEmpty()) continue;
            Vex threat = EntityType.VEX.create(player.serverLevel());
            if (threat == null) continue;
            threat.moveTo(player.getX() - 8, player.getY() + 1, player.getZ() - 8, 0, 0);
            threat.getPersistentData().putUUID("exworld_mystery_run", data.runId);
            threat.setCustomName(Component.literal("时隙追影"));
            threat.setTarget(player);
            player.serverLevel().addFreshEntity(threat);
        }
    }

    @SubscribeEvent public static void itemToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        MysterySave data = MysterySave.get(player.server);
        if (data.phase == GamePhase.LOBBY || data.phase == GamePhase.FINISHED || data.participant(player.getUUID()) == null) return;
        event.getEntity().getPersistentData().putUUID("exworld_mystery_run", data.runId);
    }

    @SubscribeEvent public static void hunterDrops(LivingDropsEvent event) {
        if (event.getEntity().getPersistentData().hasUUID("exworld_mystery_run")) event.getDrops().clear();
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Participant p = MysterySave.get(player.server).participant(player.getUUID());
        if (p != null && (p.state == PlayerState.PAST_DEAD || p.state == PlayerState.FUTURE_DEAD))
            player.setGameMode(GameType.SPECTATOR);
        if (p != null && p.needsRestore) { restore(player, p); p.needsRestore = false; MysterySave.get(player.server).changed(); }
        if (p != null && MysterySave.get(player.server).phase != GamePhase.LOBBY)
            MysteryStoryPresets.ensure(player.server);
        sync(player);
        MysterySave data = MysterySave.get(player.server);
        MysterySave.ActiveCue active = data.activeCues.get(player.getUUID());
        if (data.globalCue != null && (active == null || data.globalCue.start() > active.start()))
            active = data.globalCue;
        if (active != null && active.run().equals(data.runId) && data.tick < active.start() + active.duration()) {
            CompoundTag cue = new CompoundTag(); cue.putUUID("run", active.run());
            cue.putString("id", active.id()); cue.putLong("start", active.start()); cue.putInt("duration", active.duration());
            MysteryNetwork.send(player, "cue", cue);
        }
    }

    public static boolean discover(ServerPlayer player, String clue) {
        if (!clue.matches("[a-z0-9_.:-]{1,96}")) return false;
        MysterySave data = MysterySave.get(player.server);
        Participant p = data.participant(player.getUUID());
        if (p == null || !p.alive() || !p.knowledge.add(clue)) return false;
        p.lastClueTick = data.tick;
        p.hintStage = 0;
        if (clue.startsWith("priest:")) data.priestMemory.add(clue);
        data.changed();
        cue(player, "clue_found", 55);
        emit(player.server, "on_clue_found", player, clue);
        sync(player);
        return true;
    }

    public static void npcContact(ServerPlayer player, String npcId) {
        MysterySave data = MysterySave.get(player.server);
        Participant p = data.participant(player.getUUID());
        if (p == null || !p.alive()) return;
        String id = npcId.contains(":") ? npcId.substring(npcId.indexOf(':') + 1) : npcId;
        if (p.era == Era.FUTURE && id.equals("bartender")) discover(player, "future:recipe_source");
        if (p.era == Era.FUTURE && id.equals("foreman")) discover(player, "future:work_route");
        if (p.era == Era.FUTURE && id.equals("church_staff")) discover(player, "future:church_route");
        if (p.era == Era.FUTURE && id.equals("black_market")) discover(player, "future:black_market");
        if (p.era == Era.PAST && id.equals("priest_hand") && data.cycle > 0 && data.priestMemory.contains("priest:reported"))
            discover(player, "priest:death_place_requested");
        if (p.era == Era.FUTURE && (id.equals("bartender") || id.equals("foreman") || id.equals("church_staff"))
                && p.cycleContacts.add(id)) {
            p.suspicion = Math.min(3, p.suspicion + 1);
            data.changed(); sync(player);
        }
    }

    public static void npcChoice(ServerPlayer player, String npcId, String choiceId) {
        MysterySave data = MysterySave.get(player.server);
        Participant p = data.participant(player.getUUID());
        if (p == null || !p.alive()) return;
        String id = npcId.contains(":") ? npcId.substring(npcId.indexOf(':') + 1) : npcId;
        UUID choiceRun = data.runId;
        emit(player.server, "on_npc_choice", player, id + ":" + choiceId);
        if (!choiceRun.equals(data.runId) || data.phase == GamePhase.FINISHED) return;
        if (p.era == Era.PAST && id.equals("cultist") && choiceId.equals("notes")) {
            discover(player, "past:ritual_seen");
            if (p.cycleContacts.add("cultist_notes")) { give(player, "lotm:cultist_echo", 1); data.changed(); }
        }
        if (p.era == Era.PAST && id.equals("priest_hand") && choiceId.equals("report")
                && p.knowledge.contains("past:ritual_seen")) discover(player, "priest:reported");
        if (p.era == Era.FUTURE && id.equals("passerby") && choiceId.equals("reveal_all")) rewind(player.server);
        if (p.era == Era.PAST && id.equals("priest_hand") && choiceId.equals("death_place")
                && p.knowledge.contains("priest:death_place_requested")) {
            discover(player, "priest:seal_crafted");
            if (p.cycleContacts.add("priest_cache")) {
                data.sealCount += 4;
                give(player, "lotm:ritual_book", 1);
                data.changed(); syncAll(player.server);
            }
        }
        if (p.era == Era.FUTURE && id.equals("foreman") && choiceId.equals("take_work")) {
            if (data.tick - p.lastWorkTick >= 20 * 20) {
                p.lastWorkTick = data.tick;
                give(player, "lotm:coin", 8);
                data.changed();
            } else player.displayClientMessage(Component.translatable("mystery.exworld.work_cooldown"), true);
        }
        if (p.era == Era.FUTURE && id.equals("bartender") && choiceId.equals("buy_recipe")
                && !p.knowledge.contains("future:recipe_known") && takeCoins(player, 16))
            discover(player, "future:recipe_known");
        if (p.era == Era.FUTURE && id.equals("foreman") && choiceId.equals("four_workers")
                && data.participants().stream().filter(member -> member.era == Era.FUTURE && member.role.equals("illegal")).count() >= 4)
            discover(player, "future:recipe_known");
        if (p.era == Era.FUTURE && id.equals("church_staff") && choiceId.equals("join_church")) {
            if (!p.knowledge.contains("future:recipe_known") || !data.priestMemory.contains("priest:seal_crafted"))
                player.displayClientMessage(Component.translatable("mystery.exworld.church_requirements"), true);
            else if (p.suspicion >= 3)
                player.displayClientMessage(Component.translatable("mystery.exworld.church_suspicious"), true);
            else if (p.cycleContacts.add("church_loan")) {
                discover(player, "future:seal_route");
                give(player, "lotm:sealed_key", 1);
                data.changed();
            }
        }
        if (p.era == Era.FUTURE && id.equals("black_market") && choiceId.equals("buy_seal")
                && p.knowledge.contains("future:recipe_known") && takeCoins(player, 12)) {
            data.sealCount++;
            data.changed(); syncAll(player.server);
        }
        if (id.equals("conductor") && choiceId.equals("perform_ritual") && data.phase == GamePhase.ENDING
                && p.state == PlayerState.ENDING_CHASE && !data.ritualPerformed) {
            Item book = BuiltInRegistries.ITEM.get(ResourceLocation.parse("lotm:ritual_book"));
            if (player.getInventory().countItem(book) <= 0) {
                player.displayClientMessage(Component.translatable("mystery.exworld.need_ritual_book"), true);
            } else {
                data.ritualPerformed = true;
                data.endingAt = Math.min(data.endingAt, data.tick + 3 * 20);
                data.changed();
                cueAll(player.server, "ritual_complete", 60);
                syncAll(player.server);
            }
        }
    }

    private static boolean takeCoins(ServerPlayer player, int cost) {
        Item coin = BuiltInRegistries.ITEM.get(ResourceLocation.parse("lotm:coin"));
        if (coin == Items.AIR || player.getInventory().countItem(coin) < cost) {
            player.displayClientMessage(Component.translatable("mystery.exworld.not_enough_coins", cost), true);
            return false;
        }
        for (int i = 0; i < player.getInventory().getContainerSize() && cost > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(coin)) continue;
            int amount = Math.min(cost, stack.getCount()); stack.shrink(amount); cost -= amount;
        }
        return true;
    }

    private static void give(ServerPlayer player, String id, int count) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        if (item == Items.AIR) return;
        ItemStack stack = new ItemStack(item, count);
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    public static void setSeals(MinecraftServer server, int count) {
        MysterySave data = MysterySave.get(server);
        data.sealCount = Math.max(0, count);
        data.changed();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) sync(player);
    }

    /** Called only by the lotm sealed-key adapter after it has checked its train anchor. */
    public static List<ServerPlayer> board(ServerPlayer holder) {
        MysterySave data = MysterySave.get(holder.server);
        Participant p = data.participant(holder.getUUID());
        if (data.phase != GamePhase.ACTIVE || p == null || p.era != Era.FUTURE || p.state != PlayerState.FUTURE_ACTIVE) return List.of();
        List<ServerPlayer> group = new ArrayList<>();
        p.state = PlayerState.BOARDED;
        group.add(holder);
        for (Participant linked : data.participants()) {
            if (linked.era != Era.PAST || !linked.characterId.equals(p.characterId) || linked.state != PlayerState.PAST_ACTIVE) continue;
            ServerPlayer player = holder.server.getPlayerList().getPlayer(linked.uuid);
            if (player != null) { linked.state = PlayerState.BOARDED; group.add(player); }
        }
        data.sealCount++;
        data.changed();
        for (ServerPlayer player : group) { cue(player, "board_train", 90); sync(player); }
        UUID boardRun = data.runId;
        emit(holder.server, "on_seal_use", holder, p.characterId);
        if (!boardRun.equals(data.runId)) return List.copyOf(group);
        boolean ready = data.participants().stream().filter(value -> value.alive() && value.state != PlayerState.BOARDED
                && holder.server.getPlayerList().getPlayer(value.uuid) != null).findAny().isEmpty();
        if (ready) beginEnding(holder.server);
        return boardRun.equals(data.runId) ? List.copyOf(group) : List.of();
    }

    private static void beginEnding(MinecraftServer server) {
        MysterySave data = MysterySave.get(server);
        data.phase = GamePhase.ENDING;
        data.endingAt = data.tick + CHASE_TICKS;
        for (Participant p : data.participants()) if (p.state == PlayerState.BOARDED) p.state = PlayerState.ENDING_CHASE;
        data.changed();
        UUID endingRun = data.runId;
        emit(server, "on_phase_enter", null, "ENDING");
        if (!endingRun.equals(data.runId)) return;
        cueAll(server, "ending_chase", 120);
        syncAll(server);
    }

    public static String resolveEnding(MinecraftServer server) {
        MysterySave data = MysterySave.get(server);
        if (data.phase != GamePhase.ENDING) return "";
        data.phase = GamePhase.FINISHED;
        data.endingAt = -1;
        Item ritualBook = BuiltInRegistries.ITEM.get(ResourceLocation.parse("lotm:ritual_book"));
        boolean bookOnTrain = data.participants().stream().filter(p -> p.state == PlayerState.ENDING_CHASE)
                .map(p -> server.getPlayerList().getPlayer(p.uuid)).anyMatch(player -> player != null
                        && player.getInventory().countItem(ritualBook) > 0);
        String outcome = data.ritualPerformed || bookOnTrain ? MysteryRules.outcome(data.sealCount) : "bad";
        for (ServerLevel level : server.getAllLevels()) {
            List<UrbanNpc> echoes = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) if (entity instanceof UrbanNpc npc
                    && npc.documentId().equals("exworld:conductor")) echoes.add(npc);
            for (UrbanNpc npc : echoes) {
                level.sendParticles(ParticleTypes.END_ROD, npc.getX(), npc.getY() + 1, npc.getZ(),
                        72, .7, 1, .7, .04);
                npc.discard();
            }
        }
        resetRunEntities(server, data.runId);
        data.changed();
        UUID finishedRun = data.runId;
        emit(server, "on_phase_enter", null, "FINISHED");
        if (!finishedRun.equals(data.runId)) return outcome;
        cueAll(server, "ending_" + outcome, 140);
        syncAll(server);
        return outcome;
    }

    public static List<ServerPlayer> counterparts(ServerPlayer player) {
        return counterpartIds(player).stream().map(value -> player.server.getPlayerList().getPlayer(value))
                .filter(value -> value != null).toList();
    }

    public static List<UUID> counterpartIds(ServerPlayer player) {
        MysterySave data = MysterySave.get(player.server);
        Participant p = data.participant(player.getUUID());
        if (p == null || data.phase == GamePhase.LOBBY) return List.of();
        return data.participants().stream().filter(value -> value != p && value.era != p.era
                && value.characterId.equals(p.characterId)).map(value -> value.uuid).toList();
    }

    public static boolean active(ServerPlayer player) {
        MysterySave data = MysterySave.get(player.server);
        return data.phase != GamePhase.LOBBY && data.participant(player.getUUID()) != null;
    }

    public static UUID currentRun(ServerPlayer player) { return MysterySave.get(player.server).runId; }

    public static void cue(ServerPlayer player, String id, int duration) {
        MysterySave data = MysterySave.get(player.server);
        data.activeCues.put(player.getUUID(), new MysterySave.ActiveCue(data.runId, id, data.tick, duration));
        data.changed();
        CompoundTag tag = new CompoundTag();
        tag.putUUID("run", data.runId); tag.putString("id", id);
        tag.putLong("start", data.tick); tag.putInt("duration", duration);
        MysteryNetwork.send(player, "cue", tag);
    }

    public static void cueAll(MinecraftServer server, String id, int duration) {
        MysterySave data = MysterySave.get(server);
        data.globalCue = new MysterySave.ActiveCue(data.runId, id, data.tick, duration);
        data.changed();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) cue(player, id, duration);
    }

    /** A finished session returns to assignment so published blueprints can start the next game. */
    public static boolean prepareLobby(MinecraftServer server) {
        MysterySave data = MysterySave.get(server);
        if (data.phase != GamePhase.FINISHED) return false;
        UUID oldRun = data.runId;
        data.phase = GamePhase.LOBBY;
        data.runId = UUID.randomUUID();
        data.tick = 0; data.rewindAt = -1; data.endingAt = -1;
        data.cycle = 0; data.sealCount = 0; data.ritualPerformed = false;
        data.priestMemory.clear(); data.activeCues.clear(); data.globalCue = null;
        for (Participant p : data.participants()) {
            p.state = PlayerState.READY; p.knowledge.clear(); p.cycleContacts.clear();
            p.suspicion = 0; p.hintStage = 0; p.needsRestore = false;
            ServerPlayer player = server.getPlayerList().getPlayer(p.uuid);
            if (player != null) player.setGameMode(GameType.ADVENTURE);
        }
        resetRunEntities(server, oldRun);
        resetNpcs(server);
        data.changed(); syncAll(server);
        return true;
    }

    public static void sync(ServerPlayer player) {
        MysterySave data = MysterySave.get(player.server);
        Participant p = data.participant(player.getUUID());
        CompoundTag tag = new CompoundTag();
        tag.putUUID("run", data.runId);
        tag.putString("phase", data.phase.name()); tag.putLong("tick", data.tick);
        tag.putInt("cycle", data.cycle); tag.putInt("seals", data.sealCount);
        tag.putLong("ending_at", data.endingAt);
        if (p != null) {
            tag.putString("character", p.characterId); tag.putString("display", p.displayName);
            tag.putString("era", p.era.name()); tag.putString("state", p.state.name());
            tag.putInt("suspicion", p.suspicion);
            tag.putInt("hint_stage", p.hintStage);
            tag.putString("knowledge", String.join("\n", p.knowledge));
        }
        MysteryNetwork.send(player, "state", tag);
    }

    private static void syncAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) sync(player);
    }

    private static void emit(MinecraftServer server, String type, ServerPlayer player, String detail) {
        net.exmo.exworld.mystery.blueprint.BlueprintRuntime.emit(server, type, player, detail);
    }

    public static void handle(ServerPlayer player, MysteryPayloads.Server payload) {
        if (payload == null || payload.tag() == null) return;
        if ("editor".equals(payload.kind())) {
            net.exmo.exworld.mystery.blueprint.BlueprintEditorActions.handle(player, payload.tag());
        }
    }
}
