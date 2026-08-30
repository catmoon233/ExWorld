package net.exmo.exworld.world;

import net.exmo.exworld.content.ExWorldContent;
import net.exmo.exworld.network.WorldNetwork;
import net.exmo.exworld.network.SaveWorldGroupEditPayload;
import net.exmo.exworld.world.generation.ChunkPreGenerator;
import net.exmo.exworld.world.generation.WorldLayoutGenerator;
import net.exmo.exworld.world.generation.WorldTerrainAdapter;
import net.exmo.exworld.world.model.AnchorSnapshot;
import net.exmo.exworld.world.model.TravelAnchor;
import net.exmo.exworld.world.model.MapAnchor;
import net.exmo.exworld.world.model.WorldSnapshot;
import net.exmo.exworld.world.model.WorldTile;
import net.exmo.exworld.world.model.MapTile;
import net.exmo.exworld.world.model.WorldBiome;
import net.exmo.exworld.world.model.ChunkGroupShape;
import net.exmo.exworld.world.model.ChunkGroupTransition;
import net.exmo.exworld.world.model.WorldDimensions;
import net.exmo.exworld.world.storage.NeoForgeWorldStateStore;
import net.exmo.exworld.world.storage.WorldStateData;
import net.exmo.exworld.world.storage.WorldStateStore;
import net.exmo.exworld.subtitle.SubtitlePayload;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.exmo.exworld.Config;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.exmo.exworld.battle.BattleSystem;

import java.util.Comparator;
import java.util.Set;
import java.util.List;
import java.util.Optional;

public final class WorldSystem {
    public static final long ANCHOR_COOLDOWN_MS = 60_000L;
    private static final WorldStateStore STORE = new NeoForgeWorldStateStore();
    private static final ChunkPreGenerator PRE_GENERATOR = new ChunkPreGenerator();

    private WorldSystem() {}

    public static WorldSnapshot snapshot(ServerPlayer player) {
        WorldStateData state = state(player.getServer());
        WorldTile current = state.tile(state.playerTile(player.getUUID())).orElseGet(() ->
                tileAt(state, player.getX(), player.getZ()).orElseGet(() -> state.tiles().getFirst()));
        int minimumX = current.mapX() - WorldDimensions.MAP_SIZE / 2;
        int minimumZ = current.mapZ() - WorldDimensions.MAP_SIZE / 2;
        int maximumX = minimumX + WorldDimensions.MAP_SIZE;
        int maximumZ = minimumZ + WorldDimensions.MAP_SIZE;
        List<WorldTile> viewTiles = state.tiles().stream().filter(tile -> tile.mapX() >= minimumX && tile.mapX() < maximumX
                && tile.mapZ() >= minimumZ && tile.mapZ() < maximumZ).toList();
        Set<String> visibleRegions = viewTiles.stream().map(WorldTile::regionId).collect(java.util.stream.Collectors.toSet());
        return new WorldSnapshot(viewTiles.stream().map(MapTile::from).toList(),
                current.id(), state.generatedChunks(), state.totalChunks(),
                minimumX, minimumZ, WorldDimensions.MAP_SIZE, WorldDimensions.MAP_SIZE, state.groupChunks(),
                state.pregenerationEnabled(), state.manualGroups(), state.regionInfos(visibleRegions), state.playerAnchors(player.getUUID()).stream()
                        .map(state::anchor).flatMap(Optional::stream)
                        .map(anchor -> new MapAnchor(anchor.id(), anchor.name(), anchor.pos().getX(),
                                anchor.pos().getY(), anchor.pos().getZ(), anchor.tileId())).toList(), state.groupRevision());
    }

    /** Returns the strategic-map biome at the player's current overworld tile. */
    public static String currentBiomeId(ServerPlayer player) {
        if (player == null || player.getServer() == null || player.level().dimension() != Level.OVERWORLD) {
            return WorldBiome.PRAIRIE.id();
        }
        WorldStateData state = state(player.getServer());
        return state.tile(state.playerTile(player.getUUID())).map(WorldTile::biomeId).orElse(WorldBiome.PRAIRIE.id());
    }

    /** Administrative travel; ordinary player travel happens through anchors. */
    public static boolean travel(ServerPlayer player, String tileId) {
        Optional<WorldTile> target = state(player.getServer()).tile(tileId);
        target.ifPresent(tile -> teleportToTile(player, tile));
        return target.isPresent();
    }

    /** Opens the server-authoritative collaborative editor from any connected client when the server enables it. */
    public static void openGroupEditor(ServerPlayer player) {
        if (!Config.allowClientGroupEditing && !player.getServer().getPlayerList().isOp(player.getGameProfile())) return;
        WorldNetwork.sendGroupEditor(player, snapshot(player), "");
    }

    /** Validates one complete GUI draft, persists it, and refreshes the live group shape of every overworld player. */
    public static void saveGroupEdit(ServerPlayer player, SaveWorldGroupEditPayload payload) {
        if (!Config.allowClientGroupEditing && !player.getServer().getPlayerList().isOp(player.getGameProfile())) return;
        try {
            WorldStateData state = state(player.getServer());
            if (payload.baseRevision() != state.groupRevision()) {
                WorldNetwork.sendGroupEditor(player, snapshot(player), "区域组已被其他玩家更新；请基于最新内容重新编辑。");
                return;
            }
            state.applyGroupEdit(player.getServer().overworld().getSeed(), payload.manualGroups(), payload.groups());
            refreshActiveChunkGroups(player.getServer());
            WorldNetwork.sendGroupEditor(player, snapshot(player), "");
        } catch (IllegalArgumentException error) {
            WorldNetwork.sendGroupEditor(player, snapshot(player), error.getMessage());
        }
    }

    public static void useAnchor(ServerPlayer player, BlockPos pos) {
        WorldStateData state = state(player.getServer());
        String id = anchorId(pos);
        TravelAnchor anchor = state.anchor(id).orElseGet(() -> {
            WorldTile tile = tileAt(state, pos.getX(), pos.getZ()).orElseGet(() -> state.tiles().getFirst());
            TravelAnchor created = new TravelAnchor(id, tile.name() + "锚点", pos.immutable(), tile.id());
            state.registerAnchor(created);
            return created;
        });
        if (state.bindAnchor(player.getUUID(), anchor.id())) {
            player.displayClientMessage(Component.translatable("message.exworld.anchor_activated", anchor.name()), false);
        }
        sendAnchors(player, anchor.id(), false);
    }

    public static void teleportToAnchor(ServerPlayer player, String anchorId) {
        WorldStateData state = state(player.getServer());
        if (!state.playerAnchors(player.getUUID()).contains(anchorId)) return;
        Optional<TravelAnchor> target = validAnchor(player.serverLevel(), state, anchorId);
        if (target.isEmpty()) return;
        boolean operator = player.getServer().getPlayerList().isOp(player.getGameProfile());
        long now = System.currentTimeMillis();
        if (!operator && state.anchorCooldown(player.getUUID()) > now) {
            long seconds = Math.max(1L, (state.anchorCooldown(player.getUUID()) - now + 999L) / 1000L);
            player.displayClientMessage(Component.translatable("message.exworld.anchor_cooldown", seconds), false);
            return;
        }
        teleport(player, target.get().pos().above(), target.get().tileId());
        if (!operator) state.setAnchorCooldown(player.getUUID(), now + ANCHOR_COOLDOWN_MS);
    }

    public static void selectRespawn(ServerPlayer player, String anchorId) {
        WorldStateData state = state(player.getServer());
        if (anchorId.isEmpty()) state.setPendingRespawn(player.getUUID(), "");
        else if (state.playerAnchors(player.getUUID()).contains(anchorId)) state.setPendingRespawn(player.getUUID(), anchorId);
    }

    public static void sendRespawnChoices(ServerPlayer player) { sendAnchors(player, "", true); }

    private static void sendAnchors(ServerPlayer player, String currentAnchorId, boolean respawnMode) {
        WorldStateData state = state(player.getServer());
        List<TravelAnchor> bound = state.playerAnchors(player.getUUID()).stream().map(state::anchor).flatMap(Optional::stream).toList();
        long remaining = Math.max(0L, state.anchorCooldown(player.getUUID()) - System.currentTimeMillis());
        WorldNetwork.sendAnchorSnapshot(player, new AnchorSnapshot(bound, currentAnchorId, remaining, respawnMode));
    }

    private static void teleportToTile(ServerPlayer player, WorldTile tile) {
        ServerLevel level = player.getServer().overworld();
        BlockPos destination = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(tile.worldX(), 0, tile.worldZ())).above();
        teleport(player, destination, tile.id());
    }

    private static void teleport(ServerPlayer player, BlockPos destination, String tileId) {
        ServerLevel level = player.getServer().overworld();
        player.teleportTo(level, destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5, player.getYRot(), player.getXRot());
        WorldStateData state = state(player.getServer());
        state.setPlayerTile(player.getUUID(), tileId);
        state.tile(tileId).ifPresent(tile -> WorldNetwork.sendActiveChunkGroup(player, chunkGroupShape(state, tile)));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        WorldStateData state = state(event.getServer());
        if (event.hasTime() && state.pregenerationEnabled()) PRE_GENERATOR.tick(event.getServer().overworld(), state);
    }

    /** The strategic map grows on demand, so do not keep a smaller vanilla world border as a second hidden limit. */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        event.getServer().overworld().getWorldBorder().setSize(WorldDimensions.MAX_WORLD_BORDER_DIAMETER);
    }

    /**
     * Enforces the configured RPG floor on newly generated chunks. The dimension min-y itself remains a datapack
     * concern; this guarantees the requested Y=minimum bedrock invariant for both vanilla and custom presets.
     */
    @SubscribeEvent
    public static void onChunkGenerated(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != ServerLevel.OVERWORLD) return;
        WorldStateData state = state(level.getServer());
        // Existing chunks count as explored once a player or the optional queue loads them; terrain adaptation only
        // mutates genuinely new chunks so installing ExWorld never rewrites an established world behind the player.
        state.markChunkGenerated(event.getChunk().getPos().x, event.getChunk().getPos().z);
        if (!event.isNewChunk()) return;
        int floorY = WorldDimensions.MIN_BUILD_HEIGHT;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int minX = event.getChunk().getPos().getMinBlockX();
        int minZ = event.getChunk().getPos().getMinBlockZ();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                event.getChunk().setBlockState(cursor.set(minX + x, floorY, minZ + z), Blocks.BEDROCK.defaultBlockState(), false);
            }
        }
        Optional<WorldTile> tile = tileAt(state, minX + 8.0, minZ + 8.0);
        tile.ifPresent(worldTile -> {
            event.getChunk().fillBiomesFromNoise(
                    (quartX, quartY, quartZ, sampler) -> level.registryAccess().registryOrThrow(Registries.BIOME)
                            .getHolderOrThrow(ResourceKey.create(Registries.BIOME,
                                    ResourceLocation.parse(worldTile.biome().vanillaBiomeId()))),
                    level.getChunkSource().randomState().sampler());
            WorldTerrainAdapter.apply(event.getChunk(), worldTile, level.getSeed(), floorY);
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 5 != 0 || player.isCreative()
                || player.isSpectator() || BattleSystem.isParticipating(player.getUUID())
                || player.level().dimension() != Level.OVERWORLD) return;
        WorldStateData state = state(player.getServer());
        Optional<WorldTile> current = tileAt(state, player.getX(), player.getZ());
        if (current.isPresent()) {
            String previous = state.playerTile(player.getUUID());
            WorldTile entered = current.get();
            String previousGroup = state.tile(previous).map(WorldTile::regionId).orElse("");
            String enteredGroup = entered.regionId();
            state.setPlayerTile(player.getUUID(), current.get().id());
            if (ChunkGroupTransition.shouldSynchronize(player.tickCount <= 5, previousGroup, enteredGroup)) {
                WorldNetwork.sendActiveChunkGroup(player, chunkGroupShape(state, entered));
            }
            if (ChunkGroupTransition.shouldShowSubtitle(previousGroup, enteredGroup)) {
                String groupName = state.region(enteredGroup).map(net.exmo.exworld.world.model.Region::name).orElse(entered.name());
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                        new SubtitlePayload(Component.literal(groupName), Component.literal(entered.description()), 70, 0xFFD6B56A));
            }
            return;
        }
        state.tile(state.playerTile(player.getUUID())).ifPresent(tile -> {
            double half = WorldDimensions.groupBlocks(state.groupChunks()) / 2.0 - 0.8;
            double x = Math.max(tile.worldX() - half, Math.min(tile.worldX() + half, player.getX()));
            double z = Math.max(tile.worldZ() - half, Math.min(tile.worldZ() + half, player.getZ()));
            player.teleportTo(x, player.getY(), z);
            player.displayClientMessage(Component.translatable("message.exworld.world_boundary"), true);
        });
    }

    /** A dimension transfer replaces the client's level renderer, so the group snapshot must be sent again. */
    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().dimension() != Level.OVERWORLD) return;
        syncActiveChunkGroup(player);
    }

    @SubscribeEvent
    public static void onRespawnPosition(PlayerRespawnPositionEvent event) {
        if (event.isFromEndFight()) return;
        ServerPlayer player = (ServerPlayer) event.getEntity();
        WorldStateData state = state(player.getServer());
        Optional<TravelAnchor> selected = state.pendingRespawn(player.getUUID()).flatMap(id -> validAnchor(player.getServer().overworld(), state, id));
        Optional<TravelAnchor> fallback = nearestValidAnchor(player.getServer().overworld(), state, player.blockPosition(), player.getUUID());
        Optional<TravelAnchor> target = selected.or(() -> fallback);
        state.setPendingRespawn(player.getUUID(), "");
        target.ifPresent(anchor -> event.setDimensionTransition(new DimensionTransition(player.getServer().overworld(),
                anchor.pos().above().getBottomCenter(), Vec3.ZERO, player.getYRot(), player.getXRot(), false, DimensionTransition.DO_NOTHING)));
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("exworld")
                .then(Commands.literal("travel")
                        .then(Commands.argument("tile", StringArgumentType.word())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String id = StringArgumentType.getString(context, "tile");
                                    if (!travel(player, id)) {
                                        context.getSource().sendFailure(Component.translatable("command.exworld.unknown_tile", id));
                                        return 0;
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("anchor").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("place").executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            BlockPos pos = player.blockPosition().relative(player.getDirection(), 2);
                            player.serverLevel().setBlockAndUpdate(pos, ExWorldContent.TRAVEL_ANCHOR.get().defaultBlockState());
                            useAnchor(player, pos);
                            return 1;
                        })))
                .then(Commands.literal("world").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("pregeneration")
                                .executes(context -> {
                                    boolean enabled = state(context.getSource().getServer()).pregenerationEnabled();
                                    context.getSource().sendSuccess(() -> Component.translatable(
                                            enabled ? "command.exworld.pregeneration.on" : "command.exworld.pregeneration.off"), false);
                                    return enabled ? 1 : 0;
                                })
                                .then(Commands.literal("on").executes(context -> setPregeneration(context.getSource(), true)))
                                .then(Commands.literal("off").executes(context -> setPregeneration(context.getSource(), false))))
                        .then(Commands.literal("group_size")
                                .executes(context -> {
                                    WorldStateData state = state(context.getSource().getServer());
                                    context.getSource().sendSuccess(() -> Component.translatable(
                                            "command.exworld.group_size.current", state.groupChunks(),
                                            WorldDimensions.groupBlocks(state.groupChunks())), false);
                                    return state.groupChunks();
                                })
                                .then(Commands.literal("set")
                                        .then(Commands.argument("chunks", IntegerArgumentType.integer(
                                                        WorldDimensions.MIN_GROUP_CHUNKS, WorldDimensions.MAX_GROUP_CHUNKS))
                                                .executes(context -> resizeChunkGroups(context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "chunks"))))))
                        .then(Commands.literal("groups").executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            openGroupEditor(player);
                            return 1;
                        }))));
    }

    private static int setPregeneration(net.minecraft.commands.CommandSourceStack source, boolean enabled) {
        state(source.getServer()).setPregenerationEnabled(enabled);
        source.sendSuccess(() -> Component.translatable(enabled
                ? "command.exworld.pregeneration.changed_on" : "command.exworld.pregeneration.changed_off"), true);
        return enabled ? 1 : 0;
    }

    private static int resizeChunkGroups(net.minecraft.commands.CommandSourceStack source, int groupChunks) {
        WorldStateData state = state(source.getServer());
        if (state.groupChunks() == groupChunks) {
            source.sendSuccess(() -> Component.translatable("command.exworld.group_size.unchanged", groupChunks), false);
            return 0;
        }
        state.resizeGroups(source.getServer().overworld().getSeed(), groupChunks);
        refreshActiveChunkGroups(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.exworld.group_size.changed", groupChunks,
                WorldDimensions.groupBlocks(groupChunks)), true);
        return groupChunks;
    }

    private static Optional<TravelAnchor> nearestValidAnchor(ServerLevel level, WorldStateData state, BlockPos origin, java.util.UUID playerId) {
        return state.playerAnchors(playerId).stream().map(state::anchor).flatMap(Optional::stream)
                .filter(anchor -> validAnchor(level, state, anchor.id()).isPresent())
                .min(Comparator.comparingDouble(anchor -> anchor.pos().distSqr(origin)));
    }

    private static Optional<TravelAnchor> validAnchor(ServerLevel level, WorldStateData state, String id) {
        return state.anchor(id).filter(anchor -> level.getBlockState(anchor.pos()).is(ExWorldContent.TRAVEL_ANCHOR.get()));
    }

    private static Optional<WorldTile> tileAt(WorldStateData state, double x, double z) {
        int mapX = WorldDimensions.groupCoordinate(x, state.groupChunks());
        int mapZ = WorldDimensions.groupCoordinate(z, state.groupChunks());
        return state.ensureTile(mapX, mapZ);
    }

    private static ChunkGroupShape chunkGroupShape(WorldStateData state, WorldTile tile) {
        List<ChunkGroupShape.Cell> cells = state.region(tile.regionId()).stream()
                .flatMap(region -> region.tileIds().stream())
                .map(state::tile).flatMap(Optional::stream)
                .map(member -> new ChunkGroupShape.Cell(member.mapX(), member.mapZ())).toList();
        return new ChunkGroupShape(tile.regionId(), state.groupChunks(), cells.isEmpty()
                ? List.of(new ChunkGroupShape.Cell(tile.mapX(), tile.mapZ())) : cells);
    }

    private static void syncActiveChunkGroup(ServerPlayer player) {
        WorldStateData state = state(player.getServer());
        tileAt(state, player.getX(), player.getZ()).ifPresent(tile -> {
            state.setPlayerTile(player.getUUID(), tile.id());
            WorldNetwork.sendActiveChunkGroup(player, chunkGroupShape(state, tile));
        });
    }

    private static void refreshActiveChunkGroups(MinecraftServer server) {
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            if (online.level().dimension() == Level.OVERWORLD) syncActiveChunkGroup(online);
        }
    }

    private static String tileId(int x, int z) {
        return "tile_" + (x < 0 ? "n" + -x : "p" + x) + "_" + (z < 0 ? "n" + -z : "p" + z);
    }

    private static String anchorId(BlockPos pos) { return "anchor_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ(); }

    private static WorldStateData state(MinecraftServer server) {
        WorldStateData state = STORE.get(server);
        state.initialize(server.overworld().getSeed());
        return state;
    }
}
