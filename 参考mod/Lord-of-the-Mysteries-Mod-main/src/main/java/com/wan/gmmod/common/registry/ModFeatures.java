package com.wan.gmmod.common.registry;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.world.GuimiLuckyGardenFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 世界生成特征注册表。
 * <p>
 * {@link #LUCKY_GARDEN 好运圃}：好运之花 + 四叶草环形群落，经
 * {@code configured_feature/lucky_garden.json} 与 {@code placed_feature/lucky_garden.json}
 * 配置后由生物群系修改器 {@code add_lucky_garden.json} 注入主世界植被装饰阶段。
 */
public class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, GuimiMod.MODID);

    public static final Supplier<GuimiLuckyGardenFeature> LUCKY_GARDEN =
            FEATURES.register("lucky_garden",
                    () -> new GuimiLuckyGardenFeature(NoneFeatureConfiguration.CODEC));
}
