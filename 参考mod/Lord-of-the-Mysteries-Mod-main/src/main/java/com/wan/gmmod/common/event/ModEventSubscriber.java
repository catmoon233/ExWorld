package com.wan.gmmod.common.event;

import com.wan.gmmod.GuimiMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(modid = GuimiMod.MODID)
public class ModEventSubscriber {

    // MOD 总线事件：通用设置
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        // 初始化网络、能力等，目前打印日志

        GuimiMod. LOGGER.info("诡秘之主模组 - 通用设置完成");
    }



    // 后续可加 @EventBusSubscriber 在 FORGE 总线上处理其他事件
}