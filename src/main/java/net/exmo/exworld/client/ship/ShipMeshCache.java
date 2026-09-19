package net.exmo.exworld.client.ship;

import net.exmo.exworld.ship.entity.ShipEntity;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;

/** Invalidates baked meshes when hull revision changes. Baking itself is done by the renderer per visible voxel. */
public final class ShipMeshCache {
    private int entityId = -1;
    private int revision = Integer.MIN_VALUE;

    public void ensure(ShipEntity entity, BlockRenderDispatcher dispatcher) {
        if (entityId != entity.getId() || revision != entity.hull().revision()) {
            entityId = entity.getId();
            revision = entity.hull().revision();
        }
    }

    public boolean stale(ShipEntity entity) {
        return entityId != entity.getId() || revision != entity.hull().revision();
    }
}
