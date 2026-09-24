package net.exmo.lotm.phone;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public final class WifiRouterBlockEntity extends BlockEntity {
    private WifiBlockModel spec = WifiBlockModel.normalize("Lotm-WiFi", "12345678", 16, "");

    public WifiRouterBlockEntity(BlockPos pos, BlockState state) {
        super(LotmBlocks.WIFI_ENTITY.get(), pos, state);
    }

    public String name() {
        return spec.hotspotName();
    }

    public String password() {
        return spec.accessSecret();
    }

    public int range() {
        return spec.coverageRadius();
    }

    public void setOwner(UUID id) {
        spec = WifiBlockModel.normalize(spec.hotspotName(), spec.accessSecret(), spec.coverageRadius(), id == null ? "" : id.toString());
        setChanged();
    }

    public boolean canEdit(ServerPlayer player) {
        return spec.placedBy().isBlank() || spec.placedBy().equals(player.getUUID().toString()) || player.hasPermissions(2);
    }

    public void configure(String nextName, String nextPassword, int nextRange) {
        spec = WifiBlockModel.normalize(nextName, nextPassword, nextRange, spec.placedBy());
        setChanged();
    }

    public boolean covers(ServerPlayer player) {
        if (level == null || !player.serverLevel().dimension().equals(level.dimension())) return false;
        return player.distanceToSqr(getBlockPos().getCenter()) <= (double) range() * range();
    }

    public String editorJson() {
        JsonObject root = new JsonObject();
        root.addProperty("openWifi", true);
        root.addProperty("x", getBlockPos().getX());
        root.addProperty("y", getBlockPos().getY());
        root.addProperty("z", getBlockPos().getZ());
        root.addProperty("name", name());
        root.addProperty("password", password());
        root.addProperty("range", range());
        return root.toString();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("hotspotName", spec.hotspotName());
        tag.putString("accessSecret", spec.accessSecret());
        tag.putInt("coverageRadius", spec.coverageRadius());
        tag.putString("placedBy", spec.placedBy());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        spec = WifiBlockModel.normalize(
                tag.contains("hotspotName") ? tag.getString("hotspotName") : (tag.contains("name") ? tag.getString("name") : "Lotm-WiFi"),
                tag.contains("accessSecret") ? tag.getString("accessSecret") : tag.getString("password"),
                tag.contains("coverageRadius") ? tag.getInt("coverageRadius") : (tag.contains("range") ? tag.getInt("range") : 16),
                tag.contains("placedBy") ? tag.getString("placedBy") : tag.getString("owner")
        );
    }

}
