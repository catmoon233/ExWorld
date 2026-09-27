package com.wan.gmmod.common.event;

import com.wan.gmmod.Config;
import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.item.CharacteristicItem;
import com.wan.gmmod.common.registry.ModItems;
import com.wan.gmmod.content.characteristics.CharacteristicManager;
import com.wan.gmmod.content.characteristics.CharacteristicsPool;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * 非凡集会：流浪商人以小概率化身为「非凡集会商人」，
 * 出售从守恒池抽出的随机途径 / 等级特性，价格随池中剩余量（稀有度）浮动。
 */
@EventBusSubscriber(modid = GuimiMod.MODID)
public class BeyondersGatheringSubscriber {
    private static final String GATHERER_TAG = "guimi_beyonder_gatherer";

    @SubscribeEvent
    public static void onTraderJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof WanderingTrader trader)) {
            return;
        }
        if (!(trader.level() instanceof ServerLevel level)) {
            return;
        }
        if (trader.getPersistentData().getBoolean(GATHERER_TAG)) {
            return;
        }
        if (Config.GATHERING_CHANCE.get() <= 0) {
            return;
        }
        if (level.random.nextDouble() >= Config.GATHERING_CHANCE.get()) {
            return;
        }
        if (CharacteristicManager.pool(level).totalPending() <= 0) {
            return;
        }

        trader.getPersistentData().putBoolean(GATHERER_TAG, true);
        trader.setCustomName(Component.translatable("entity.guimi_mod.beyonder_gatherer"));
        trader.setCustomNameVisible(true);
        trader.setDespawnDelay(6000);

        MerchantOffers offers = trader.getOffers();
        int toAdd = 2 + level.random.nextInt(2);
        for (int i = 0; i < toAdd; i++) {
            addGatheringOffer(level, offers);
        }
    }

    private static void addGatheringOffer(ServerLevel level, MerchantOffers offers) {
        CharacteristicsPool pool = CharacteristicManager.pool(level);
        int total = pool.totalPending();
        if (total <= 0) {
            return;
        }
        Sequences.Pathway pathway = null;
        int lvl = -1;
        int roll = level.random.nextInt(total);
        outer:
        for (Sequences.Pathway p : Sequences.Pathway.values()) {
            for (int l = 0; l <= Sequences.MAX_LEVEL; l++) {
                int pend = pool.getPending(p, l);
                if (pend <= 0) {
                    continue;
                }
                roll -= pend;
                if (roll < 0) {
                    pathway = p;
                    lvl = l;
                    break outer;
                }
            }
        }
        if (pathway == null) {
            return;
        }
        int rarity = Math.max(1, pool.getPending(pathway, lvl));
        int price = Config.GATHERING_BASE_PRICE.get()
                + lvl * Config.GATHERING_LEVEL_STEP.get()
                + (int) Math.ceil(Config.GATHERING_SCARCITY_BONUS.get() / (double) rarity);
        pool.addPending(pathway, lvl, -1);
        CharacteristicManager.save(level, pool);
        ItemStack result = CharacteristicItem.create(pathway, lvl);
        offers.add(new MerchantOffer(
                new ItemCost(ModItems.SOYLE.get(), Math.min(64, price)),
                result, 1, 5, 0.15f));
    }
}