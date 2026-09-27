package net.exmo.lotm;

import net.exmo.lotm.effect.LotmEffects;
import net.exmo.lotm.graffiti.GraffitiTraps;
import net.exmo.lotm.guimi.GuimiIntegration;
import net.exmo.lotm.spell.LotmSpells;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

/** Upstream content registration, then the guimi adapter. The mod entry stays a one-line call. */
public final class LotmBootstrap {
    private LotmBootstrap() {}

    public static void register(IEventBus modBus) {
        net.exmo.lotm.sequence.SequenceSystem.register(modBus);
        net.exmo.lotm.sequence.SequenceSystem.registerEvents();
        LotmEffects.register(modBus);
        LotmSpells.register(modBus);
        WarriorPassives.register();
        PainterPassives.register();
        MotherPassives.register();
        WarriorPathway.register();
        PainterPathway.register();
        MotherPathway.register();
        RedPriestPassives.register();
        HangedManPassives.register();
        RedPriestPathway.register();
        HunterPassives.register();
        WitchPassives.register();
        WitchPathway.register();
        LotmRuntime.register();
        HangedManPathway.register();
        SpectatorPassives.register();
        SunPassives.register();
        SpectatorPathway.register();
        SunPathway.register();
        FateCirclePassives.register();
        HungerPassives.register();
        FateCirclePathway.register();
        HungerPathway.register();
        ThiefPassives.register();
        ApprenticePassives.register();
        ThiefPathway.register();
        ApprenticePathway.register();
        SailorPassives.register();
        WizardPassives.register();
        SailorPathway.register();
        WizardPathway.register();
        GuimiIntegration.install();
        NeoForge.EVENT_BUS.register(GraffitiTraps.class);
        NeoForge.EVENT_BUS.register(MotherPassives.class);
        NeoForge.EVENT_BUS.register(net.exmo.lotm.spell.LotmSpellRuntime.class);
        NeoForge.EVENT_BUS.register(RedPriestPassives.class);
        NeoForge.EVENT_BUS.register(HangedManPassives.class);
        NeoForge.EVENT_BUS.register(SpectatorPassives.class);
        NeoForge.EVENT_BUS.register(SunPassives.class);
        NeoForge.EVENT_BUS.register(net.exmo.lotm.effect.InfluenceEvents.class);
        NeoForge.EVENT_BUS.register(FateCirclePassives.class);
        NeoForge.EVENT_BUS.register(HungerPassives.class);
        NeoForge.EVENT_BUS.register(ThiefPassives.class);
        NeoForge.EVENT_BUS.register(ApprenticePassives.class);
        NeoForge.EVENT_BUS.register(SailorPassives.class);
        NeoForge.EVENT_BUS.register(WizardPassives.class);
        net.exmo.lotm.story.StorySystem.register(modBus);
        net.exmo.lotm.curios.LotmCurios.register(modBus);
        NeoForge.EVENT_BUS.register(net.exmo.lotm.curios.CurioEvents.class);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            net.exmo.lotm.client.NightSkyLight.register();
            net.exmo.lotm.client.LotmClient.bootstrap();
        }
    }
}
