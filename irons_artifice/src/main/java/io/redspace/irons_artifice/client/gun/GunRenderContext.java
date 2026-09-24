package io.redspace.irons_artifice.client.gun;

import io.redspace.irons_artifice.data.HandOccupancy;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.item.AttachmentMap;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.GunplayManager;
import io.redspace.irons_artifice.item.MagazineContents;
import io.redspace.irons_artifice.item.ReloadState;
import io.redspace.irons_artifice.item.animation_adjuster.AnimationAdjuster;
import io.redspace.irons_artifice.registry.DataComponentRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Render-time values that GeckoLib 5 stored on a render state. Bone adjusters keep the same
 * magazine, reload, and attachment inputs.
 */
public final class GunRenderContext {
    public final ItemStack stack;
    public final GunItem gun;
    public final ItemDisplayContext perspective;
    public final float partialTick;
    public final int packedLight;
    public final MagazineContents magazine;
    public final double reloadProgressSeconds;
    public final float reloadPercent;
    public final float muzzleOffset;
    public final List<AnimationAdjuster> adjusters;
    public final AttachmentMap attachments;
    public final HandOccupancy occupancy;
    public final Integer ownerId;

    private GunRenderContext(
            ItemStack stack,
            GunItem gun,
            ItemDisplayContext perspective,
            float partialTick,
            int packedLight,
            MagazineContents magazine,
            double reloadProgressSeconds,
            float reloadPercent,
            float muzzleOffset,
            List<AnimationAdjuster> adjusters,
            AttachmentMap attachments,
            HandOccupancy occupancy,
            Integer ownerId
    ) {
        this.stack = stack;
        this.gun = gun;
        this.perspective = perspective;
        this.partialTick = partialTick;
        this.packedLight = packedLight;
        this.magazine = magazine;
        this.reloadProgressSeconds = reloadProgressSeconds;
        this.reloadPercent = reloadPercent;
        this.muzzleOffset = muzzleOffset;
        this.adjusters = adjusters;
        this.attachments = attachments;
        this.occupancy = occupancy;
        this.ownerId = ownerId;
    }

    public static GunRenderContext capture(GunItem gun, ItemStack stack, ItemDisplayContext perspective, float partialTick, int packedLight) {
        LivingEntity owner = findOwner(stack);
        ReloadState reload = ReloadState.get(stack);
        HandOccupancy occupancy = owner == null ? gun.getGun().defaultOccupancy() : GunItem.currentOccupancy(owner, stack);
        if (occupancy == null) {
            occupancy = HandOccupancy.BOTH;
        }
        return new GunRenderContext(
                stack,
                gun,
                perspective,
                partialTick,
                packedLight,
                MagazineContents.has(stack) ? MagazineContents.get(stack) : null,
                GunItem.reloadAnimationSeconds(stack),
                reload != null ? reload.percent(partialTick) : 0f,
                (float) GunplayManager.compose(owner, gun.getGun(), stack).value(ShotComponents.MUZZLE_OFFSET),
                gun.getGun().animationAdjusters(),
                stack.getOrDefault(DataComponentRegistry.ATTACHMENT.get(), AttachmentMap.EMPTY),
                occupancy,
                owner == null ? null : owner.getId()
        );
    }

    private static LivingEntity findOwner(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return null;
        }
        if (minecraft.player != null && holds(minecraft.player, stack)) {
            return minecraft.player;
        }
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && holds(living, stack)) {
                return living;
            }
        }
        return null;
    }

    private static boolean holds(LivingEntity entity, ItemStack stack) {
        return entity.getMainHandItem() == stack || entity.getOffhandItem() == stack;
    }
}
