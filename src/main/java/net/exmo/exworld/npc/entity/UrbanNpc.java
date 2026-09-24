package net.exmo.exworld.npc.entity;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.npc.NpcSystem;
import net.exmo.exworld.npc.data.NpcDocument;
import net.exmo.exworld.npc.data.NpcLoadout;
import net.exmo.exworld.npc.data.TradeSpec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** A document-backed urban NPC. The document id is shared; runtime progress stays on the entity. */
public class UrbanNpc extends PathfinderMob implements Merchant {
    private static final EntityDataAccessor<String> DOCUMENT = SynchedEntityData.defineId(UrbanNpc.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.defineId(UrbanNpc.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SPEECH = SynchedEntityData.defineId(UrbanNpc.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_KIND = SynchedEntityData.defineId(UrbanNpc.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SLIM = SynchedEntityData.defineId(UrbanNpc.class, EntityDataSerializers.BOOLEAN);

    private String nodeId = "";
    private int routeIndex;
    private int stuck;
    private String marginalId = "";
    private long marginalEndsAt;
    private String suspendedNodeId = "";
    private boolean dialogOpen;
    private int sequenceIndex;
    private int seenRevision = -1;
    private int speechTicks;
    private boolean trading;
    private String lastIdle = "";
    @Nullable private Player tradingPlayer;
    private MerchantOffers offers = new MerchantOffers();
    private final java.util.Map<String, Integer> stock = new java.util.LinkedHashMap<>();
    private long nextRestock;
    private String tradePlaceId = "";

    public UrbanNpc(EntityType<? extends UrbanNpc> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    public String documentId() { return entityData.get(DOCUMENT); }
    public void setDocumentId(String id) { entityData.set(DOCUMENT, id == null ? "" : id); }
    public String textureId() { return entityData.get(TEXTURE); }
    public String skinKind() { return entityData.get(SKIN_KIND); }
    public boolean slim() { return entityData.get(SLIM); }
    public String speech() { return entityData.get(SPEECH); }
    public String nodeId() { return nodeId; }
    public void setNodeId(String id) { nodeId = id == null ? "" : id; }
    public int routeIndex() { return routeIndex; }
    public void setRouteIndex(int index) { routeIndex = Math.max(0, index); }
    public int stuck() { return stuck; }
    public void setStuck(int value) { stuck = Math.max(0, value); }
    public String marginalId() { return marginalId; }
    public long marginalEndsAt() { return marginalEndsAt; }
    public String suspendedNodeId() { return suspendedNodeId; }
    public void setSuspendedNodeId(String id) { suspendedNodeId = id == null ? "" : id; }
    public boolean dialogOpen() { return dialogOpen; }
    public void setDialogOpen(boolean open) { dialogOpen = open; }
    public int sequenceIndex() { return sequenceIndex; }
    public void setSequenceIndex(int index) { sequenceIndex = Math.max(0, index); }
    public int seenRevision() { return seenRevision; }
    public void setSeenRevision(int revision) { seenRevision = revision; }
    public boolean tradingOpen() { return trading; }
    public void setTrading(boolean trading) { this.trading = trading; }
    public void setTextureId(String texture) { entityData.set(TEXTURE, texture == null ? "" : texture); }
    public MerchantOffers offers() { return offers; }
    public void setOffers(MerchantOffers offers) { this.offers = offers == null ? new MerchantOffers() : offers; }
    public String tradePlaceId() { return tradePlaceId == null ? "" : tradePlaceId; }
    public void setTradePlace(String id) { tradePlaceId = id == null ? "" : id; }

    public boolean arrivedAt(NpcDocument doc, String placeId) {
        if (doc == null || placeId == null || placeId.isBlank()) return false;
        net.exmo.exworld.npc.data.NpcPlace place = doc.place(placeId).orElse(null);
        if (place == null) return false;
        if (!place.dimension().equals(level().dimension().location().toString())) return false;
        return NpcMovement.arrived(getX(), getY(), getZ(), place.x(), place.y(), place.z(), place.radius());
    }

    public void setSpeech(String line, int ticks) {
        entityData.set(SPEECH, line == null ? "" : line);
        speechTicks = Math.max(0, ticks);
    }

    public void setMarginal(String id, long endsAt, String suspended) {
        if (id != null && !id.equals(marginalId)) suspendedNodeId = suspended == null ? nodeId : suspended;
        marginalId = id == null ? "" : id;
        marginalEndsAt = endsAt;
    }

    public void clearMarginal() {
        marginalId = "";
        marginalEndsAt = 0;
    }

    public void safeIdle(String reason) {
        if (reason != null && !reason.equals(lastIdle)) {
            Exworld.LOGGER.warn("Urban NPC {} idle: {}", documentId().isBlank() ? getStringUUID() : documentId(), reason);
            lastIdle = reason;
        }
        getNavigation().stop();
        setTrading(false);
        setTradePlace("");
    }

    public void clearIdle() { lastIdle = ""; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new GroundPathNavigation(this, level);
    }

    @Override
    protected void customServerAiStep() {
        if (speechTicks > 0 && --speechTicks == 0) entityData.set(SPEECH, "");
        try {
            NpcRuntime.tick(this);
        } catch (RuntimeException ex) {
            Exworld.LOGGER.warn("Urban NPC {} tick failed", getStringUUID(), ex);
            safeIdle("fault");
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).getItem() instanceof net.exmo.exworld.npc.item.NpcWandItem) return InteractionResult.PASS;
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer) NpcSystem.interact(serverPlayer, this);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt) setSpeech("……", 30);
        return hurt;
    }

    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean canBeLeashed() { return true; }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DOCUMENT, "");
        builder.define(TEXTURE, "");
        builder.define(SPEECH, "");
        builder.define(SKIN_KIND, "resource");
        builder.define(SLIM, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("document_id", documentId());
        tag.putString("node_id", nodeId);
        tag.putInt("route_index", routeIndex);
        tag.putInt("stuck", stuck);
        tag.putString("marginal_id", marginalId);
        tag.putLong("marginal_end", marginalEndsAt);
        tag.putString("suspended_node", suspendedNodeId);
        tag.putBoolean("dialog_open", dialogOpen);
        tag.putInt("sequence_index", sequenceIndex);
        tag.putLong("restock", nextRestock);
        ListTag lines = new ListTag();
        stock.forEach((item, count) -> { CompoundTag entry = new CompoundTag(); entry.putString("item", item); entry.putInt("count", count); lines.add(entry); });
        tag.put("stock", lines);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setDocumentId(tag.getString("document_id"));
        nodeId = tag.getString("node_id");
        routeIndex = tag.getInt("route_index");
        stuck = tag.getInt("stuck");
        marginalId = tag.getString("marginal_id");
        marginalEndsAt = tag.getLong("marginal_end");
        suspendedNodeId = tag.getString("suspended_node");
        dialogOpen = tag.getBoolean("dialog_open");
        sequenceIndex = tag.getInt("sequence_index");
        seenRevision = -1;
        nextRestock = tag.getLong("restock");
        stock.clear();
        ListTag lines = tag.getList("stock", Tag.TAG_COMPOUND);
        for (int i = 0; i < lines.size(); i++) stock.put(lines.getCompound(i).getString("item"), lines.getCompound(i).getInt("count"));
    }

    public ResourceLocation clientTexture() {
        String raw = textureId();
        if (raw == null || raw.isBlank()) return ResourceLocation.withDefaultNamespace("textures/entity/steve.png");
        try {
            ResourceLocation parsed = ResourceLocation.parse(raw);
            return BuiltInRegistries.ITEM.containsKey(parsed) ? parsed : parsed;
        } catch (RuntimeException ex) {
            return ResourceLocation.withDefaultNamespace("textures/entity/steve.png");
        }
    }

    @Override public void setTradingPlayer(@Nullable Player player) { tradingPlayer = player; }
    @Override public @Nullable Player getTradingPlayer() { return tradingPlayer; }
    @Override public MerchantOffers getOffers() { return offers; }
    @Override public void overrideOffers(MerchantOffers offers) { setOffers(offers); }
    public void applyLoadout(NpcDocument doc) {
        NpcLoadout loadout = doc.loadout();
        entityData.set(SKIN_KIND, loadout.skinKind());
        entityData.set(SLIM, loadout.slim());
        for (EquipmentSlot slot : GEAR) {
            setDropChance(slot, 1.0F);
            setItemSlot(slot, ItemStack.EMPTY);
        }
        for (NpcLoadout.Gear piece : loadout.equipment()) {
            EquipmentSlot slot = slotOf(piece.slot());
            Item item = itemOf(piece.item());
            if (slot != null && item != Items.AIR) setItemSlot(slot, new ItemStack(item));
        }
        if (stock.isEmpty()) {
            for (NpcLoadout.Stock line : loadout.inventory()) stock.put(line.item(), line.count());
        }
        setOffers(offersFor(doc));
    }

    public void restock(NpcDocument doc, long gameTime) {
        if (gameTime < nextRestock) return;
        nextRestock = gameTime + doc.loadout().restockMinutes() * 1200L;
        for (NpcLoadout.Stock line : doc.loadout().inventory()) stock.put(line.item(), Math.max(stock.getOrDefault(line.item(), 0), line.count()));
        setOffers(offersFor(doc));
    }

    public MerchantOffers offersFor(NpcDocument doc) {
        MerchantOffers next = new MerchantOffers();
        for (TradeSpec trade : doc.trades()) {
            if (stock.getOrDefault(trade.resultItem(), 0) < trade.resultCount()) continue;
            Item pay = itemOf(trade.payItem());
            Item result = itemOf(trade.resultItem());
            if (pay == Items.AIR || result == Items.AIR) continue;
            next.add(new MerchantOffer(new net.minecraft.world.item.trading.ItemCost(pay, trade.payCount()), new ItemStack(result, trade.resultCount()), trade.maxUses(), 0, 0.05f));
        }
        return next;
    }

    @Override public void notifyTrade(MerchantOffer offer) {
        ItemStack result = offer.getResult();
        ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
        stock.put(resultId.toString(), Math.max(0, stock.getOrDefault(resultId.toString(), 0) - result.getCount()));
        ItemStack pay = offer.getBaseCostA();
        ResourceLocation payId = BuiltInRegistries.ITEM.getKey(pay.getItem());
        stock.put(payId.toString(), stock.getOrDefault(payId.toString(), 0) + pay.getCount());
        if (level() instanceof net.minecraft.server.level.ServerLevel server) {
            NpcDocument doc = net.exmo.exworld.npc.data.NpcCatalog.get(server.getServer()).document(documentId()).orElse(null);
            if (doc != null) setOffers(offersFor(doc));
        }
    }

    @Override
    protected void dropEquipment() {
        boolean manual = true;
        for (EquipmentSlot slot : GEAR) {
            if (getEquipmentDropChance(slot) > 0.0F) manual = false;
        }
        if (manual) {
            for (EquipmentSlot slot : GEAR) {
                ItemStack stack = getItemBySlot(slot);
                if (!stack.isEmpty()) spawnAtLocation(stack.copy());
                setItemSlot(slot, ItemStack.EMPTY);
            }
        } else {
            super.dropEquipment();
        }
        stock.forEach((id, count) -> {
            Item item = itemOf(id);
            int left = count == null ? 0 : count;
            if (item == Items.AIR || left <= 0) return;
            int max = Math.max(1, item.getDefaultMaxStackSize());
            while (left > 0) {
                int amount = Math.min(left, max);
                spawnAtLocation(new ItemStack(item, amount));
                left -= amount;
            }
        });
        stock.clear();
    }

    private static final EquipmentSlot[] GEAR = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static EquipmentSlot slotOf(String slot) {
        return switch (slot) {
            case "mainhand", "main" -> EquipmentSlot.MAINHAND;
            case "offhand", "off" -> EquipmentSlot.OFFHAND;
            case "head" -> EquipmentSlot.HEAD;
            case "chest" -> EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS;
            case "feet" -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    private static Item itemOf(String id) {
        try { return id == null || id.isBlank() ? Items.AIR : BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)); }
        catch (RuntimeException ex) { return Items.AIR; }
    }
    @Override public void notifyTradeUpdated(ItemStack stack) {}
    @Override public int getVillagerXp() { return 0; }
    @Override public void overrideXp(int xp) {}
    @Override public boolean showProgressBar() { return false; }
    @Override public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_YES; }
    @Override public boolean isClientSide() { return level().isClientSide(); }
}
