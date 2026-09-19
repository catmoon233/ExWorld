package net.exmo.exworld.ship.entity;

import net.exmo.exworld.content.ExWorldContent;
import net.exmo.exworld.network.ShipNetwork;
import net.exmo.exworld.ship.model.PartSelection;
import net.exmo.exworld.ship.model.ShipHull;
import net.exmo.exworld.ship.model.ShipOccupancy;
import net.exmo.exworld.ship.model.ShipSlot;
import net.exmo.exworld.ship.motion.AboardAttachment;
import net.exmo.exworld.ship.motion.KinematicMover;
import net.exmo.exworld.ship.storage.ShipNbtCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** One 飞船实例. Holds the hull, drives kinematically, and never runs per-block world ticks. */
public class ShipEntity extends Entity {
    private static final EntityDataAccessor<Integer> REVISION = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> TEMPLATE_ID = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.FLOAT);

    private ShipHull hull = ShipHull.empty();
    private PartSelection selection = PartSelection.empty();
    private double maxSpeed = 0.35;
    private static final double ACCELERATION = 0.05;
    private static final double FRICTION = 0.90;
    private Vec3 velocity = Vec3.ZERO;
    private UUID driverId;
    private int driveFlags;
    private int driveTicks;
    private final Map<Integer, Button> buttons = new HashMap<>();
    private Vec3 lastDelta = Vec3.ZERO;
    private Vec3 rideLocal = new Vec3(0.5, 1.0, 0.5);
    private float driveYaw;

    public ShipEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public static ShipEntity create(ServerLevel level, ShipHull hull, String templateId, PartSelection selection, double speed) {
        ShipEntity entity = ExWorldContent.SHIP.get().create(level);
        if (entity == null) throw new IllegalStateException("could not create ship entity");
        entity.install(hull, templateId, selection, speed);
        return entity;
    }

    public void install(ShipHull hull, String templateId, PartSelection selection, double speed) {
        this.hull = hull == null ? ShipHull.empty() : hull;
        this.selection = selection == null ? PartSelection.empty() : selection;
        this.maxSpeed = Math.max(0.05, speed);
        entityData.set(TEMPLATE_ID, templateId == null ? "" : templateId);
        entityData.set(REVISION, this.hull.revision());
        refreshBounds();
    }

    public ShipHull hull() { return hull; }
    public PartSelection selection() { return selection; }
    public String templateId() { return entityData.get(TEMPLATE_ID); }
    public double maxSpeed() { return maxSpeed; }
    public void selection(PartSelection next) { this.selection = next == null ? PartSelection.empty() : next; }

    public void clientInstall(ShipHull hull, String templateId, PartSelection selection, double speed) {
        install(hull, templateId, selection, speed);
    }

    public void setBlock(int x, int y, int z, String key) {
        hull = hull.setBlockKey(x, y, z, key);
        entityData.set(REVISION, hull.revision());
        if (!level().isClientSide() && level() instanceof ServerLevel server) {
            ShipNetwork.broadcastDelta(server, this, x, y, z, key);
        }
        refreshBounds();
    }

    public void replaceContainer(int packed, List<ShipSlot> slots) {
        hull = hull.withContainer(packed, slots);
    }

    public void replaceHull(ShipHull next, PartSelection nextSelection) {
        this.hull = next;
        this.selection = nextSelection;
        entityData.set(REVISION, hull.revision());
        refreshBounds();
        if (!level().isClientSide() && level() instanceof ServerLevel server) ShipNetwork.broadcastHull(server, this);
    }

    public void beginDriving(ServerPlayer player, int helmX, int helmY, int helmZ) {
        rideLocal = new Vec3(helmX + 0.5, helmY + 1.0, helmZ + 0.5);
        driverId = player.getUUID();
        player.startRiding(this);
    }

    public void drive(int flags, float yaw) {
        this.driveFlags = flags;
        this.driveTicks = 8;
        this.driveYaw = yaw;
        entityData.set(YAW, yaw);
        setYRot(yaw);
    }

    /** 客户端本地预测：只写本地状态，不碰同步数据。 */
    public void driveLocal(int flags, float yaw) {
        this.driveFlags = flags;
        this.driveTicks = 8;
        this.driveYaw = yaw;
        setYRot(yaw);
    }

    public void pressButton(int packed, String releasedKey) {
        buttons.put(packed, new Button(10, releasedKey));
    }

    public Vec3 toLocal(Vec3 world) { return world.subtract(position()); }
    public Vec3 toWorld(Vec3 local) { return position().add(local); }
    public Vec3 lastDelta() { return lastDelta; }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        Vec3 world = toWorld(rideLocal);
        callback.accept(passenger, world.x, world.y, world.z);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 before = position();
        if (!level().isClientSide()) {
            tickButtons();
            tickDrive();
            lastDelta = position().subtract(before);
            tickAboard();
        } else {
            // 本地预测：客户端用同样的速度模型驱动船体，不等服务器位置包。
            tickDrive();
            lastDelta = position().subtract(before);
        }
        refreshBounds();
    }

    private void tickButtons() {
        if (buttons.isEmpty()) return;
        buttons.entrySet().removeIf(entry -> {
            Button button = entry.getValue();
            button.ticks--;
            if (button.ticks > 0) return false;
            int packed = entry.getKey();
            setBlock(ShipOccupancy.x(packed), ShipOccupancy.y(packed), ShipOccupancy.z(packed), button.released);
            return true;
        });
    }

    private void tickDrive() {
        if (driveTicks > 0) driveTicks--;
        else driveFlags = 0;
        if (!level().isClientSide() && driverId != null) {
            Entity driver = ((ServerLevel) level()).getEntity(driverId);
            if (!(driver instanceof Player player) || player.getVehicle() != this) driverId = null;
        }
        // 推力方向（驾驶 yaw 决定前后左右，垂直轴独立）
        if (driveFlags != 0) {
            float yaw = driveYaw;
            double rad = yaw * Mth.DEG_TO_RAD;
            double forward = ((driveFlags & 1) != 0 ? 1 : 0) - ((driveFlags & 2) != 0 ? 1 : 0);
            double strafe = ((driveFlags & 4) != 0 ? 1 : 0) - ((driveFlags & 8) != 0 ? 1 : 0);
            double up = ((driveFlags & 16) != 0 ? 1 : 0) - ((driveFlags & 32) != 0 ? 1 : 0);
            double x = (-Math.sin(rad) * forward) + (Math.cos(rad) * strafe);
            double z = (Math.cos(rad) * forward) + (Math.sin(rad) * strafe);
            Vec3 thrust = new Vec3(x, up, z);
            double length = thrust.length();
            if (length > 1.0E-8) velocity = velocity.add(thrust.scale(ACCELERATION / length));
        } else {
            // 松开油门：摩擦减速，带一点滑行惯性
            velocity = velocity.scale(FRICTION);
        }
        double speed = velocity.length();
        if (speed > maxSpeed) velocity = velocity.scale(maxSpeed / speed);
        if (velocity.lengthSqr() < 1.0E-8) { velocity = Vec3.ZERO; return; }
        Vec3 from = position();
        KinematicMover.Vec next = KinematicMover.step(
                new KinematicMover.Vec(from.x, from.y, from.z),
                new KinematicMover.Vec(velocity.x, velocity.y, velocity.z),
                maxSpeed,
                hullBox(),
                this::blocked);
        Vec3 nextPos = new Vec3(next.x(), next.y(), next.z());
        // 撞墙的轴速度清零，未挡的轴保留，形成滑墙
        velocity = new Vec3(
                Math.abs(nextPos.x - from.x) < Math.abs(velocity.x) - 1.0E-4 ? 0 : velocity.x,
                Math.abs(nextPos.y - from.y) < Math.abs(velocity.y) - 1.0E-4 ? 0 : velocity.y,
                Math.abs(nextPos.z - from.z) < Math.abs(velocity.z) - 1.0E-4 ? 0 : velocity.z);
        Vec3 delta = nextPos.subtract(from);
        setPos(nextPos.x, nextPos.y, nextPos.z);
        if (!level().isClientSide()) broadcastMove(delta);
    }

    /** 每 tick 发相对移动包，客户端 lerp 插值，避免位置跳变；定期 teleport 校正漂移。 */
    private void broadcastMove(Vec3 delta) {
        if (delta.lengthSqr() < 1.0E-10) return;
        short sx = (short) Mth.clamp(delta.x * 4096.0, -32768, 32767);
        short sy = (short) Mth.clamp(delta.y * 4096.0, -32768, 32767);
        short sz = (short) Mth.clamp(delta.z * 4096.0, -32768, 32767);
        sendToTrackers(new ClientboundMoveEntityPacket.Pos(getId(), sx, sy, sz, onGround()));
        if (tickCount % 20 == 0) sendToTrackers(new ClientboundTeleportEntityPacket(this));
    }

    private void sendToTrackers(net.minecraft.network.protocol.Packet<?> packet) {
        for (ServerPlayer tracker : ((ServerLevel) level()).getPlayers(player -> player.distanceToSqr(this) < 64.0 * 64.0)) {
            tracker.connection.send(packet);
        }
    }

    private void tickAboard() {
        AABB search = localBounds().move(position()).inflate(1.5);
        for (Player player : level().getEntitiesOfClass(Player.class, search)) {
            if (player.getVehicle() == this) continue;
            Vec3 carried = player.position().add(lastDelta);
            Vec3 local = toLocal(carried);
            AboardAttachment.Result result = AboardAttachment.inspect(local.x, local.y, local.z, hull);
            if (!result.aboard()) continue;
            Vec3 snapped = toWorld(new Vec3(local.x, result.snapLocalY(), local.z));
            boolean rising = player.getDeltaMovement().y > 0.01;
            boolean steering = player.xxa != 0 || player.zza != 0;
            double targetX = steering ? player.getX() : snapped.x;
            double targetZ = steering ? player.getZ() : snapped.z;
            double targetY = rising ? carried.y : snapped.y;
            if (!rising && Math.abs(carried.y - snapped.y) >= 1.5) targetY = carried.y;
            if (player instanceof ServerPlayer server) server.teleportTo(targetX, targetY, targetZ);
            else player.setPos(targetX, targetY, targetZ);
            player.setOnGround(!rising);
        }
    }

    private boolean blocked(KinematicMover.Box box) {
        AABB aabb = new AABB(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
        int minX = Mth.floor(aabb.minX);
        int minY = Mth.floor(aabb.minY);
        int minZ = Mth.floor(aabb.minZ);
        int maxX = Mth.floor(aabb.maxX - 1.0E-7);
        int maxY = Mth.floor(aabb.maxY - 1.0E-7);
        int maxZ = Mth.floor(aabb.maxZ - 1.0E-7);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockBehaviour.BlockStateBase state = level().getBlockState(cursor.set(x, y, z));
                    if (!state.isAir() && !state.getCollisionShape(level(), cursor).isEmpty()) return true;
                }
            }
        }
        return false;
    }

    private KinematicMover.Box hullBox() {
        AABB aabb = localBounds();
        return new KinematicMover.Box(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
    }

    private AABB localBounds() {
        if (hull == null) return new AABB(0, 0, 0, 1, 1, 1);
        return new AABB(0, 0, 0, Math.max(1, hull.sizeX()), Math.max(1, hull.sizeY()), Math.max(1, hull.sizeZ()));
    }

    private void refreshBounds() {
        setBoundingBox(localBounds().move(position()));
    }

    @Override protected AABB makeBoundingBox() { return localBounds().move(position()); }
    @Override public boolean isPickable() { return true; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 256 * 256; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(REVISION, 0);
        builder.define(TEMPLATE_ID, "");
        builder.define(YAW, 0f);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Hull")) hull = ShipNbtCodec.decodeHull(tag.getByteArray("Hull"));
        entityData.set(TEMPLATE_ID, tag.getString("Template"));
        entityData.set(YAW, tag.getFloat("ShipYaw"));
        maxSpeed = tag.contains("Speed") ? tag.getDouble("Speed") : 0.35;
        Map<String, String> selected = new HashMap<>();
        ListTag list = tag.getList("Selection", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            selected.put(entry.getString("part"), entry.getString("variant"));
        }
        selection = new PartSelection(selected);
        entityData.set(REVISION, hull.revision());
        refreshBounds();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putByteArray("Hull", ShipNbtCodec.encodeHull(hull));
        tag.putString("Template", templateId());
        tag.putFloat("ShipYaw", entityData.get(YAW));
        tag.putDouble("Speed", maxSpeed);
        ListTag list = new ListTag();
        selection.variantByPart().forEach((part, variant) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("part", part);
            entry.putString("variant", variant);
            list.add(entry);
        });
        tag.put("Selection", list);
    }

    private static final class Button {
        int ticks;
        final String released;
        Button(int ticks, String released) { this.ticks = ticks; this.released = released; }
    }
}
