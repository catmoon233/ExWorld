package com.wan.gmmod.common.item;

import com.wan.gmmod.client.render.SilenceGunRenderer;
import com.wan.gmmod.common.registry.ModDataComponents;
import com.wan.gmmod.common.registry.ModItems;
import com.wan.gmmod.common.registry.ModSounds;
import com.wan.gmmod.content.entities.BulletEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 丧钟（原名寂灭）：填装弹药式火器武器。
 * <p>
 * 使用机制：
 * - 右键：从背包中装填子弹（弹巢容量 6 发），播放装弹动画。
 * - 左键：开枪（消耗弹巢中 1 发），播放开枪动画。
 * - 弹药来源：背包中各类子弹物品。
 * <p>
 * 子弹优先级：普通子弹 → 特殊子弹按背包顺序。
 * 左键触发方式：空挥由 {@link com.wan.gmmod.common.network.packet.SilenceGunFirePacket} 上行，
 * 点击实体 / 方块由 {@link com.wan.gmmod.common.event.SilenceGunEventSubscriber} 拦截。
 */
public class SilenceGunItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 弹巢容量 */
    private static final int MAGAZINE_SIZE = 6;
    /** 换弹冷却（tick），与装弹动画时长 0.75s 匹配 */
    private static final int RELOAD_TICKS = 15;
    /** 射击间隔（tick），与开枪动画时长 0.4s 匹配 */
    private static final int FIRE_COOLDOWN = 8;

    /** 开枪动画（对应动画文件中的 animation.animation.silence_gun.fire） */
    private static final RawAnimation FIRE_ANIM =
            RawAnimation.begin().thenPlay("animation.animation.silence_gun.fire");
    /** 装弹动画（对应动画文件中的 animation.model.new） */
    private static final RawAnimation RELOAD_ANIM =
            RawAnimation.begin().thenPlay("animation.model.new");

    public SilenceGunItem(Properties properties) {
        super(properties);
        // 注册可同步动画，允许服务端 triggerAnim 触发开枪 / 装弹动画
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> PlayState.STOP)
                .triggerableAnim("fire", FIRE_ANIM)
                .triggerableAnim("reload", RELOAD_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private SilenceGunRenderer renderer;

            @Override
            public GeoItemRenderer<SilenceGunItem> getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new SilenceGunRenderer();
                return this.renderer;
            }
        });
    }

    /**
     * 右键：装填弹药。从背包中取子弹填满弹巢，播放装弹动画。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }

        int ammo = stack.getOrDefault(ModDataComponents.GUN_AMMO.get(), 0);
        if (ammo >= MAGAZINE_SIZE) {
            player.displayClientMessage(
                    Component.translatable("message.guimi_mod.gun_full"), true);
            return InteractionResultHolder.fail(stack);
        }

        // 从背包装填子弹，并记录弹巢内的子弹类型队列（先装先发）
        List<String> magazine = new ArrayList<>(stack.getOrDefault(ModDataComponents.GUN_MAGAZINE.get(), List.of()));
        int loaded = 0;
        if (player.isCreative()) {
            loaded = MAGAZINE_SIZE - ammo;
            for (int i = 0; i < loaded; i++) {
                magazine.add(BuiltInRegistries.ITEM.getKey(ModItems.BULLET.get()).toString());
            }
        } else {
            while (ammo + loaded < MAGAZINE_SIZE) {
                ItemStack bullet = findBullet(player);
                if (bullet.isEmpty()) break;
                magazine.add(BuiltInRegistries.ITEM.getKey(bullet.getItem()).toString());
                bullet.shrink(1);
                loaded++;
            }
            if (loaded == 0) {
                // 无子弹可装填
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.GUN_EMPTY.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                player.displayClientMessage(
                        Component.translatable("message.guimi_mod.gun_no_bullets"), true);
                return InteractionResultHolder.fail(stack);
            }
        }
        stack.set(ModDataComponents.GUN_AMMO.get(), ammo + loaded);
        stack.set(ModDataComponents.GUN_MAGAZINE.get(), List.copyOf(magazine));

        // 装弹动画 + 音效 + 冷却
        if (level instanceof ServerLevel serverLevel) {
            triggerAnim(player, GeoItem.getOrAssignId(stack, serverLevel), "controller", "reload");
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.GUN_RELOAD.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.getCooldowns().addCooldown(this, RELOAD_TICKS);
        player.displayClientMessage(
                Component.translatable("message.guimi_mod.gun_loaded", ammo + loaded, MAGAZINE_SIZE), true);
        return InteractionResultHolder.consume(stack);
    }

    /**
     * 左键：开枪。消耗弹巢中 1 发子弹并播放开枪动画。
     */
    public void fire(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof SilenceGunItem)) return;
        if (player.getCooldowns().isOnCooldown(this)) return;

        int ammo = stack.getOrDefault(ModDataComponents.GUN_AMMO.get(), 0);
        if (ammo <= 0) {
            // 空仓：击锤空响，提示装填
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.GUN_EMPTY.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.displayClientMessage(
                    Component.translatable("message.guimi_mod.gun_need_reload"), true);
            return;
        }
        stack.set(ModDataComponents.GUN_AMMO.get(), ammo - 1);

        // 取出弹巢队列中的下一发子弹（先装先发），队列缺失时回退为普通子弹
        List<String> magazine = new ArrayList<>(stack.getOrDefault(ModDataComponents.GUN_MAGAZINE.get(), List.of()));
        ItemStack bulletStack = new ItemStack(ModItems.BULLET.get());
        if (!magazine.isEmpty()) {
            ResourceLocation id = ResourceLocation.tryParse(magazine.remove(0));
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                bulletStack = new ItemStack(BuiltInRegistries.ITEM.get(id));
            }
            stack.set(ModDataComponents.GUN_MAGAZINE.get(), List.copyOf(magazine));
        }

        ServerLevel level = player.serverLevel();

        // 射击音效
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.GUN_FIRE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        // 发射子弹实体（外观与命中效果由子弹物品决定）
        BulletEntity bullet = new BulletEntity(level, player, bulletStack);
        bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 0.0F);
        level.addFreshEntity(bullet);

        // 开枪动画
        triggerAnim(player, GeoItem.getOrAssignId(stack, level), "controller", "fire");

        // 耐久消耗
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(InteractionHand.MAIN_HAND));

        // 射击冷却
        player.getCooldowns().addCooldown(this, FIRE_COOLDOWN);
    }

    /**
     * 取消左键挥打动作：开枪只播放枪械自身的开枪动画，不要手臂挥动。
     */
    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        return true;
    }

    /**
     * 取消武器下沉重新装备动作：开枪 / 装弹会修改弹药数据组件与耐久，
     * 原版会误判为「换了新物品」而播放收起再抬起的下沉动画。
     * 这里仅在真正切换手持槽位时才播放重新装备动画，保证左右键只呈现
     * 动画文件中的开枪 / 装弹两个动画。
     */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    /**
     * 在玩家背包中查找可用子弹。优先普通子弹，然后按背包顺序查找特殊子弹。
     */
    private ItemStack findBullet(Player player) {
        // 优先检查普通子弹
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(ModItems.BULLET.get())) {
                return s;
            }
        }
        // 然后检查各类特殊子弹
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (isBulletItem(s)) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    /** 判断是否是任意类型的子弹。 */
    public static boolean isBulletItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == ModItems.BULLET.get()
                || item == ModItems.DEPRIVATION_BULLET.get()
                || item == ModItems.PARASITIC_BULLET.get()
                || item == ModItems.SPIRIT_CONTROL_BULLET.get()
                || item == ModItems.DECEPTION_BULLET.get()
                || item == ModItems.EXORCISM_BULLET.get()
                || item == ModItems.PURIFICATION_BULLET.get();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        int ammo = stack.getOrDefault(ModDataComponents.GUN_AMMO.get(), 0);
        tooltipComponents.add(Component.translatable("item.guimi_mod.silence_gun.ammo", ammo, MAGAZINE_SIZE));
        tooltipComponents.add(Component.translatable("item.guimi_mod.silence_gun.tooltip"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
