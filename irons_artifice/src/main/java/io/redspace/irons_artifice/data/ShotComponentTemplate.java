package io.redspace.irons_artifice.data;

import io.redspace.irons_artifice.client.sounds.GunShotSoundSettings;
import io.redspace.irons_artifice.entity.Bullet;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Factory for a gun's innate {@link ShotComponentMap}.
 * <p>
 * Every call to {@link #get()} builds a new map. Callers may mutate the result freely
 */
@FunctionalInterface
public interface ShotComponentTemplate extends Supplier<ShotComponentMap> {

    /**
     * @return a freshly built map
     */
    @Override
    ShotComponentMap get();

    /**
     * @param damage            base damage per shot ({@link ShotComponents#DAMAGE})
     * @param spread            base spread in degrees ({@link ShotComponents#SPREAD})
     * @param characterBlowback push-back applied to the shooter ({@link ShotComponents#CHARACTER_BLOWBACK})
     * @param fireDelayTicks    ticks between shots ({@link ShotComponents#FIRE_DELAY})
     * @param cameraRecoil      camera recoil profile ({@link ShotComponents#CAMERA_RECOIL})
     */
    static Builder builder(double damage, double spread, double characterBlowback, int fireDelayTicks, RecoilProfile cameraRecoil) {
        return new Builder(damage, spread, cameraRecoil, characterBlowback, fireDelayTicks);
    }

    final class Builder {
        public static final int DEFAULT_PROJECTILE_COUNT = 1;
        public static final double DEFAULT_GRAVITY = 0.05;
        public static final double DEFAULT_KNOCKBACK = 0.3;

        // required
        private final double damage;
        private final double spread;
        private final RecoilProfile cameraRecoil;
        private final double characterBlowback;
        private final int fireDelayTicks;

        // defaulted
        private int projectileCount = DEFAULT_PROJECTILE_COUNT;
        private double bulletSpeed = Bullet.BASE_SPEED;
        private double gravity = DEFAULT_GRAVITY;
        private double knockback = DEFAULT_KNOCKBACK;

        // everything else, applied in order after the base components
        private final List<Consumer<ShotComponentMap>> steps = new ArrayList<>();

        private Builder(double damage, double spread, RecoilProfile cameraRecoil, double characterBlowback, int fireDelayTicks) {
            this.damage = damage;
            this.spread = spread;
            this.cameraRecoil = cameraRecoil;
            this.characterBlowback = characterBlowback;
            this.fireDelayTicks = fireDelayTicks;
        }

        /* ************
         * Defaulted base components
         * ************/

        public Builder projectileCount(int projectileCount) {
            this.projectileCount = projectileCount;
            return this;
        }

        public Builder bulletSpeed(double bulletSpeed) {
            this.bulletSpeed = bulletSpeed;
            return this;
        }

        /**
         * Helper for {@code bulletSpeed(Bullet.BASE_SPEED * multiplier)}
         */
        public Builder bulletSpeedMultiplier(double multiplier) {
            return bulletSpeed(Bullet.BASE_SPEED * multiplier);
        }

        public Builder gravity(double gravity) {
            this.gravity = gravity;
            return this;
        }

        public Builder knockback(double knockback) {
            this.knockback = knockback;
            return this;
        }

        /* ************
         * Common optional components
         * ************/

        public Builder gunshotSound(Supplier<GunShotSoundStack> factory) {
            return set(ShotComponents.GUNSHOT_SOUND, factory);
        }

        public Builder gunshotSound(GunShotSoundSettings shot, GunShotSoundSettings echo, PlayableSound dryFire) {
            return gunshotSound(() -> new GunShotSoundStack(shot, echo, dryFire));
        }

        /**
         * Gunshot sound with the standard dry-fire click.
         */
        public Builder gunshotSound(GunShotSoundSettings shot, GunShotSoundSettings echo, Holder<SoundEvent> dryFire) {
            return gunshotSound(() -> new GunShotSoundStack(shot, echo, PlayableSound.of(dryFire, 0.75f, 1.4f, 1.6f)));
        }

        public Builder muzzleFlash(MuzzleFlashType... types) {
            return set(ShotComponents.MUZZLE_FLASH, () -> MuzzleFlashSettings.of(types));
        }

        /* ************
         * Generic
         * ************/

        /**
         * Sets a {@link Value} component with the given base and no modifiers.
         */
        public Builder value(ComponentType<Value> type, double base) {
            return set(type, () -> Value.of(base));
        }

        /**
         * Sets a component from a factory. The factory runs once per {@link ShotComponentTemplate#get()}.
         */
        public <T> Builder set(ComponentType<T> type, Supplier<? extends T> factory) {
            steps.add(map -> map.set(type, factory.get()));
            return this;
        }

        /**
         * Arbitrary step run against the freshly built map, after all base components are set.
         * Same contract as {@code GunModifier.apply}.
         */
        public Builder modify(Consumer<ShotComponentMap> step) {
            steps.add(step);
            return this;
        }

        public ShotComponentTemplate build() {
            // snapshot everything so later mutation of this builder cannot leak into the template
            List<Consumer<ShotComponentMap>> frozen = List.copyOf(steps);
            int projectileCount = this.projectileCount;
            double bulletSpeed = this.bulletSpeed;
            double gravity = this.gravity;
            double knockback = this.knockback;
            return () -> {
                ShotComponentMap map = new ShotComponentMap();
                map.set(ShotComponents.PROJECTILE_COUNT, Value.of(projectileCount));
                map.set(ShotComponents.BULLET_SPEED, Value.of(bulletSpeed));
                map.set(ShotComponents.GRAVITY, Value.of(gravity));
                map.set(ShotComponents.KNOCKBACK, Value.of(knockback));
                map.set(ShotComponents.DAMAGE, Value.of(damage));
                map.set(ShotComponents.SPREAD, Value.of(spread));
                map.set(ShotComponents.CAMERA_RECOIL, cameraRecoil);
                map.set(ShotComponents.CHARACTER_BLOWBACK, Value.of(characterBlowback));
                map.set(ShotComponents.FIRE_DELAY, Value.of(fireDelayTicks));
                for (Consumer<ShotComponentMap> step : frozen) {
                    step.accept(map);
                }
                return map;
            };
        }
    }
}
