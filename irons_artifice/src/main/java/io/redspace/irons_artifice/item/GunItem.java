package io.redspace.irons_artifice.item;

import io.redspace.irons_artifice.IronsArtifice;
import io.redspace.irons_artifice.api.GunAnimations;
import io.redspace.irons_artifice.api.GunBones;
import io.redspace.irons_artifice.data.HandOccupancy;
import io.redspace.irons_artifice.data.ReloadResult;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.gun.GunProfile;
import io.redspace.irons_artifice.gun.GunState;
import io.redspace.irons_artifice.gun.ShotProfile;
import io.redspace.irons_artifice.item.animation_adjuster.AnimationAdjuster;
import io.redspace.irons_artifice.menu.GunContainer;
import io.redspace.irons_artifice.registry.DataComponentRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;

import java.util.List;
import java.util.function.Function;

public class GunItem extends BaseGeoItem {
    public static final String TRIGGERED_ANIMATION_CONTROLLER = GunAnimations.CONTROLLER_ACTIONS;
    public static final String IDLE_ANIMATION_CONTROLLER = GunAnimations.CONTROLLER_IDLE;

    private final GunProfile gunProfile;

    public GunItem(Properties properties, GunProfile gunProfile) {
        super(properties
                .stacksTo(1)
                .component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .component(DataComponentRegistry.MAGAZINE, new MagazineContents(gunProfile.magazineCapacity()))
        );
        this.gunProfile = gunProfile;
    }

    public static final int SCOPE_USE_DURATION = 1200;

    public static boolean hasGunSpyglass(ItemStack stack) {
        return stack.has(DataComponentRegistry.GUN_SPYGLASS);
    }

    public static boolean isScoping(LivingEntity entity) {
        return entity.isUsingItem() && hasGunSpyglass(entity.getUseItem());
    }

    public static boolean isChargingBayonet(Entity entity) {
        if (!(entity instanceof LivingEntity living) || !living.isUsingItem()) {
            return false;
        }
        AttachmentMap attachments = living.getUseItem().get(DataComponentRegistry.ATTACHMENT.get());
        return attachments != null && attachments.attachments().containsKey(GunBones.SOCKET_BAYONET);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (GunItem.isReloading(stack)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!hasGunSpyglass(stack)) {
            return super.use(level, player, hand);
        }
        player.playSound(SoundEvents.SPYGLASS_USE, 1.0F, 1.0F);
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity user) {
        return hasGunSpyglass(stack) ? SCOPE_USE_DURATION : super.getUseDuration(stack, user);
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity) {
        if (hasGunSpyglass(stack)) {
            entity.playSound(SoundEvents.SPYGLASS_STOP_USING, 1.0F, 1.0F);
            return stack;
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity, int remainingTime) {
        if (hasGunSpyglass(stack)) {
            entity.playSound(SoundEvents.SPYGLASS_STOP_USING, 1.0F, 1.0F);
            return;
        }
        super.releaseUsing(stack, level, entity, remainingTime);
    }

    public GunProfile getGun() {
        return gunProfile;
    }

    public int magazineCapacity() {
        return gunProfile.magazineCapacity();
    }

    public static @Nullable HandOccupancy currentOccupancy(LivingEntity entity, InteractionHand hand) {
        return currentOccupancy(entity, entity.getItemInHand(hand));
    }

    public static @Nullable HandOccupancy currentOccupancy(LivingEntity entity, ItemStack stack) {
        if (!(stack.getItem() instanceof GunItem gun)) {
            return null;
        }
        HandOccupancy occupancy;
        if (isReloading(stack)) {
            occupancy = gun.getGun().occupancyFor(GunState.RELOAD);
        } else if (FireDelayState.isActive(entity, stack)) {
            occupancy = gun.getGun().occupancyFor(GunState.FIRE);
        } else {
            occupancy = gun.getGun().defaultOccupancy();
        }
        if (occupancy == HandOccupancy.BOTH && stack == entity.getOffhandItem() && !entity.getMainHandItem().isEmpty()) {
            return HandOccupancy.MAINHAND;
        }
        return occupancy;
    }

    public static boolean isOffhandItemUseBlocked(LivingEntity entity) {
        return currentOccupancy(entity, InteractionHand.MAIN_HAND) == HandOccupancy.BOTH;
    }

    public static MagazineContents getMagazine(ItemStack stack) {
        MagazineContents magazine = MagazineContents.get(stack);
        return magazine != null ? magazine : MagazineContents.EMPTY;
    }

    public static void setMagazine(ItemStack stack, MagazineContents magazine) {
        MagazineContents.set(stack, magazine);
    }

    public static boolean isReloading(ItemStack stack) {
        return ReloadState.has(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemStack, @NotNull TooltipContext context, @NotNull List<Component> lines, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, lines, tooltipFlag);
        ShotProfile shotProfile = GunplayManager.compose(null, this.gunProfile, itemStack);
        String damage = ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(shotProfile.value(ShotComponents.DAMAGE));
        int bulletCount = (int) shotProfile.value(ShotComponents.PROJECTILE_COUNT);
        int bulletSpeedPercent = (int) (100 * shotProfile.value(ShotComponents.BULLET_SPEED) / Bullet.BASE_SPEED);
        String fireRate = ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(20.0 / shotProfile.fireDelayTicks());
        String reloadTime = ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(gunProfile.reloadTimeTicks() / 20f / shotProfile.value(ShotComponents.RELOAD_SPEED_MULTIPLIER));
        Function<String, Component> highlightText = s -> Component.literal(s).withStyle(ChatFormatting.GREEN);
        if (bulletCount > 1) {
            lines.add(stat(Component.translatable("irons_artifice.tooltip.damage_per_bullet", highlightText.apply(damage), Component.literal(String.valueOf(bulletCount)).withStyle(ChatFormatting.YELLOW))));
            lines.add(stat(Component.translatable("irons_artifice.tooltip.bullet_count", bulletCount).withStyle(ChatFormatting.YELLOW)));
        } else {
            lines.add(stat(Component.translatable("irons_artifice.tooltip.damage", highlightText.apply(damage))));
        }
        if (bulletSpeedPercent != 100 || Bullet.BASE_SPEED != shotProfile.peek(ShotComponents.BULLET_SPEED).base()) {
            lines.add(stat(Component.translatable("irons_artifice.tooltip.bullet_speed_percent", highlightText.apply(bulletSpeedPercent + "%"))));
        }
        if (gunProfile.magazineCapacity() > 1) {
            lines.add(stat(Component.translatable("irons_artifice.tooltip.fire_rate", highlightText.apply(fireRate))));
        }
        lines.add(stat(Component.translatable("irons_artifice.tooltip.reload_time", highlightText.apply(reloadTime + "s"))));
        lines.add(stat(Component.translatable("irons_artifice.tooltip.ammo_capacity", highlightText.apply("" + gunProfile.magazineCapacity()))));
        lines.add(Component.translatable("irons_artifice.tooltip.modifier_count",
                        gunProfile.modifierSlots()
                ).withStyle(ChatFormatting.GOLD)
                .append(" ").append(Component.translatable("irons_artifice.tooltip.keybind_hint",
                                Component.keybind("key.irons_artifice.open_modifier_menu")
                                        .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withItalic(false)))
                        .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withItalic(true))));
        GunContainer container = new GunContainer(itemStack);
        for (var item : container.getItems()) {
            if (!item.isEmpty()) {
                lines.add(Component.literal(" * ").withStyle(ChatFormatting.DARK_GRAY).append(item.getHoverName().copy().withStyle(ChatFormatting.GRAY)));
            }
        }
    }

    private static Component stat(Component component) {
        return Component.literal(" ").append(component).withStyle(ChatFormatting.DARK_GREEN);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    public static void playReloadFeedback(Level level, Player player, ReloadResult result) {
        switch (result) {
            case NO_AMMO -> level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6F, 1.0F);
            case ALREADY_FULL -> level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_DIDGERIDOO, SoundSource.PLAYERS, 0.6F, 1.0F);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        if (isReloading(stack)) {
            return (int) (ReloadState.get(stack).percent(0) * 13);
        } else {
            int count = getMagazine(stack).count();
            return Mth.clamp(Math.round(count * 13.0F / magazineCapacity()), 0, 13);
        }
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (isReloading(stack)) {
            return 0xAAAAAA;
        } else {
            return 0xFFAA00;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.@NotNull ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        controllers.add(new AnimationController<>(this, IDLE_ANIMATION_CONTROLLER, this::gunIdleHandler));
        controllers.add(new OffsetableAnimationController<>(this, GunAnimations.CONTROLLER_ACTIONS, test -> PlayState.STOP)
                .receiveTriggeredAnimations()
                .triggerableAnim(GunAnimations.FIRE, RawAnimation.begin().thenPlay(GunAnimations.FIRE))
                .triggerableAnim(GunAnimations.RELOAD, RawAnimation.begin().thenPlay(GunAnimations.RELOAD))
                .triggerableAnim(GunAnimations.EQUIP, RawAnimation.begin().thenPlay(GunAnimations.EQUIP))
        );
    }

    private PlayState gunIdleHandler(AnimationState<GunItem> animationTest) {
        animationTest.setAnimation(RawAnimation.begin().thenPlayAndHold(GunAnimations.IDLE));
        return PlayState.CONTINUE;
    }

    public void playTriggeredClientAnimation(long instanceId, String animName, double speed, double offsetSeconds, double skipAtSeconds, double skipToSeconds) {
        var manager = getAnimatableInstanceCache().getManagerForId(instanceId);
        manager.setData(DataTickets.ITEM_RENDER_PERSPECTIVE, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
        AnimationController<?> controller = manager.getAnimationControllers().get(TRIGGERED_ANIMATION_CONTROLLER);
        if (!(controller instanceof OffsetableAnimationController<?> offsetable)) {
            return;
        }
        manager.tryTriggerAnimation(TRIGGERED_ANIMATION_CONTROLLER, animName);
        offsetable.setAnimationSpeed(speed);
        offsetable.seekToSeconds(offsetSeconds);
        offsetable.setTimelineSkip(skipAtSeconds, skipToSeconds);
    }

    public void cancelTriggeredClientAnimation(long instanceId) {
        var manager = getAnimatableInstanceCache().getManagerForId(instanceId);
        manager.stopTriggeredAnimation(TRIGGERED_ANIMATION_CONTROLLER);
        AnimationController<?> controller = manager.getAnimationControllers().get(TRIGGERED_ANIMATION_CONTROLLER);
        if (controller instanceof OffsetableAnimationController<?> offsetable) {
            offsetable.seekToSeconds(0);
            offsetable.forceAnimationReset();
        }
    }

    public static double reloadAnimationSeconds(ItemStack stack) {
        if (!(stack.getItem() instanceof GunItem gun)) {
            return 0;
        }
        AnimationController<?> controller = gun.getAnimatableInstanceCache()
                .getManagerForId(GeoItem.getId(stack))
                .getAnimationControllers()
                .get(TRIGGERED_ANIMATION_CONTROLLER);
        if (controller instanceof OffsetableAnimationController<?> offsetable && offsetable.isPlayingNamed(GunAnimations.RELOAD)) {
            return offsetable.currentAnimationSeconds();
        }
        return 0;
    }

    /**
     * Client animation seek. GeckoLib 4 has no timelineTime; tickOffset is shifted so the
     * animation clock matches the requested seconds without changing server reload duration.
     */
    public static class OffsetableAnimationController<T extends GeoAnimatable> extends AnimationController<T> {
        private static final double TICKS_PER_SECOND = 20.0;
        private double skipAtSeconds;
        private double skipToSeconds;
        private boolean skipped;
        private double startOffsetSeconds;
        private double lastAdjustedTicks;

        public OffsetableAnimationController(T animatable, String name, AnimationStateHandler<T> stateHandler) {
            super(animatable, name, stateHandler);
        }

        public void setTimelineSkip(double skipAtSeconds, double skipToSeconds) {
            this.skipAtSeconds = skipAtSeconds;
            this.skipToSeconds = skipToSeconds;
            this.skipped = false;
        }

        public void seekToSeconds(double seconds) {
            this.startOffsetSeconds = Math.max(0, seconds);
        }

        public boolean isPlayingNamed(String name) {
            AnimationProcessor.QueuedAnimation current = getCurrentAnimation();
            return current != null && name.equals(current.animation().name());
        }

        public double currentAnimationSeconds() {
            return this.lastAdjustedTicks / TICKS_PER_SECOND;
        }

        @Override
        protected double adjustTick(double tick) {
            boolean resetting = this.shouldResetTick;
            double adjusted = super.adjustTick(tick);
            double speed = Math.max(getAnimationSpeed(), 1.0E-6);
            if (resetting && this.startOffsetSeconds > 0) {
                double startTicks = this.startOffsetSeconds * TICKS_PER_SECOND;
                this.tickOffset = tick - startTicks / speed;
                adjusted = startTicks;
            }
            if (!this.skipped && this.skipToSeconds > this.skipAtSeconds) {
                double skipAtTicks = this.skipAtSeconds * TICKS_PER_SECOND;
                double skipToTicks = this.skipToSeconds * TICKS_PER_SECOND;
                if (adjusted >= skipAtTicks && adjusted < skipToTicks) {
                    this.tickOffset = tick - skipToTicks / speed;
                    this.skipped = true;
                    adjusted = skipToTicks;
                }
            }
            this.lastAdjustedTicks = adjusted;
            return adjusted;
        }
    }
}
