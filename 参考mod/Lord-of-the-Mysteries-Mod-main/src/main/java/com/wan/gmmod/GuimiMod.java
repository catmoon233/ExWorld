package com.wan.gmmod;

import com.mojang.logging.LogUtils;


import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.registry.ModArmorMaterials;
import com.wan.gmmod.common.registry.ModBlocks;
import com.wan.gmmod.common.registry.ModBlockEntities;
import com.wan.gmmod.common.registry.ModCreativeTabs;
import com.wan.gmmod.common.registry.ModDataComponents;
import com.wan.gmmod.common.registry.ModEntities;
import com.wan.gmmod.common.registry.ModItems;
import com.wan.gmmod.common.registry.ModLootModifiers;
import com.wan.gmmod.common.registry.ModSounds;
import com.wan.gmmod.content.brewing.BrewingRecipeManager;
import com.wan.gmmod.content.altar.AltarRecipeManager;
import com.wan.gmmod.content.quest.TaskRegistry;
import com.wan.gmmod.content.sequences.SequenceRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(GuimiMod.MODID)
public class GuimiMod {
    public static final String MODID = "guimi_mod";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
    public GuimiMod(IEventBus modEventBus, ModContainer modContainer) {
        // 注册数据组件（先于物品，保证物品 Properties.component 可解析）
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        // 注册物品
        ModItems.ITEMS.register(modEventBus);
        ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        // 原有事件注册
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModSounds.register(modEventBus);
        com.wan.gmmod.common.registry.ModFeatures.FEATURES.register(modEventBus);
        com.wan.gmmod.common.registry.ModEffects.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        com.wan.gmmod.content.sequences.SequenceRegistry.init();
        com.wan.gmmod.content.spell.GuimiSpells.register(modEventBus);
    }

    // 逻辑代码


    private void commonSetup(FMLCommonSetupEvent event) {
        SequenceRegistry.init();
        BrewingRecipeManager.init();
        AltarRecipeManager.init();
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // 将占卜家魔药添加到“工具与实用”标签页（也可以加进原版“酿造”）
        //if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
          //  event.accept(ModItems.SEER_POTION);
     //   }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        SequenceRegistry.init();
        LOGGER.info("诡秘序列注册表已初始化");
    }

    /** 数据包重载时（含世界加载 / 服务器启动）加载任务 JSON。 */
    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new net.minecraft.server.packs.resources.SimplePreparableReloadListener<>() {
            @Override
            protected Object prepare(net.minecraft.server.packs.resources.ResourceManager resourceManager,
                                     net.minecraft.util.profiling.ProfilerFiller profiler) {
                return net.minecraft.util.Unit.INSTANCE;
            }

            @Override
            protected void apply(Object object, net.minecraft.server.packs.resources.ResourceManager resourceManager,
                                 net.minecraft.util.profiling.ProfilerFiller profiler) {
// TaskRegistry.load(resourceManager);
            }
        });
    }
}