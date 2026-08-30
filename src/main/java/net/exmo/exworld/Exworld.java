package net.exmo.exworld;

import com.mojang.logging.LogUtils;
import net.exmo.exworld.client.WorldMapClient;
import net.exmo.exworld.content.ExWorldContent;
import net.exmo.exworld.network.WorldNetwork;
import net.exmo.exworld.world.WorldSystem;
import net.exmo.exworld.battle.BattleSystem;
import net.exmo.exworld.battle.attribute.BattleAttributes;
import net.exmo.exworld.dungeon.DungeonSystem;
import net.exmo.exworld.progress.PlayerProgressSystem;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(Exworld.MODID)
public final class Exworld {
    public static final String MODID = "exworld";
    public static final Logger LOGGER = LogUtils.getLogger();


    public Exworld(IEventBus modBus, ModContainer container) {
        ExWorldContent.register(modBus);
        BattleAttributes.register(modBus);
        modBus.addListener(WorldNetwork::register);
        NeoForge.EVENT_BUS.register(WorldSystem.class);
        BattleSystem.registerEvents();
        DungeonSystem.registerEvents();
        PlayerProgressSystem.registerEvents();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            WorldMapClient.register(modBus);
        }
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
