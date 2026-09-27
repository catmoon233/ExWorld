package com.wan.gmmod.common.item;

import com.wan.gmmod.client.render.VestRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * 马甲盔甲物品类
 * <p>
 * 继承 {@link ArmorItem}，实现 {@link GeoItem} 接口，
 * 使用 GeckoLib 提供 3D 动画盔甲模型渲染。
 * <p>
 * 装填于胸甲槽（{@link ArmorItem.Type#CHESTPLATE}），
 * 穿戴时通过 {@link GeoArmorRenderer} 将模型骨骼绑定到玩家身体，
 * 实现「适配我的世界模型」的效果。
 *
 * @see GeoArmorRenderer
 */
public class VestItem extends ArmorItem implements GeoItem {
    /** GeckoLib 动画实例缓存，用于管理动画状态 */
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /**
     * 构造马甲盔甲物品
     *
     * @param material   盔甲材料
     * @param type       盔甲类型（应为 {@link ArmorItem.Type#CHESTPLATE}）
     * @param properties 品属性
     */
    public VestItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    /**
     * 注册动画控制器
     * <p>
     * 马甲暂无复杂动画需求，可在此处后续扩展动画控制器
     *
     * @param controllers 动画控制器注册器
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 马甲目前不需要动画，可按需在此添加 AnimationController
    }

    /**
     * 获取动画实例缓存
     *
     * @return GeckoLib 动画实例缓存
     */
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /**
     * 创建 GeckoLib 渲染器提供者
     * <p>
     * 通过 {@link GeoRenderProvider#getGeoArmorRenderer} 返回
     * 自定义的 {@link VestRenderer}（继承 {@link GeoArmorRenderer}），
     * 由 GeckoLib 的 {@code HumanoidArmorLayerMixin} 自动接管盔甲渲染。
     * <p>
     * {@code getGeoItemRenderer} 保持默认返回 null，
     * 物品栏 / 手持渲染使用原版物品模型 JSON（{@code models/item/vest.json}）。
     *
     * @param consumer 渲染器提供者消费者
     */
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private VestRenderer renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(
                    @Nullable T livingEntity, ItemStack itemStack,
                    @Nullable EquipmentSlot equipmentSlot,
                    @Nullable HumanoidModel<T> original) {
                if (this.renderer == null)
                    this.renderer = new VestRenderer();
                return this.renderer;
            }
        });
    }
}
