package com.wan.gmmod.common.event;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.CauldronBrewingData;
import com.wan.gmmod.common.registry.ModItems;
import com.wan.gmmod.content.brewing.BrewingRecipe;
import com.wan.gmmod.content.brewing.BrewingRecipeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 炼药锅交互：以原版炼药锅为容器实现魔药合成。
 * <ul>
 *   <li>手持「净水」右键装水的炼药锅 → 该锅成为「纯水锅」，消耗一份净水。</li>
 *   <li>向纯水锅中投入配方材料后，手持木棍右键搅拌，累计三次触发合成。</li>
 *   <li>搅拌满三次时扫描锅内掉落物匹配配方：命中则消耗材料、产出魔药并清空锅（还原为空炼药锅）；
 *       未命中则重置搅拌计数并提示。</li>
 * </ul>
 */
@EventBusSubscriber(modid = GuimiMod.MODID)
public class BrewingEventSubscriber {

    /** 触发合成所需的搅拌次数。 */
    private static final int STIRS_REQUIRED = 3;

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // 仅处理主手，避免双手各触发一次
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();

        boolean isWaterCauldron = state.is(Blocks.WATER_CAULDRON);
        if (!isWaterCauldron) {
            return;
        }

        // 净水入锅：装水炼药锅 + 手持净水 → 纯水锅
        if (held.is(ModItems.PURIFIED_WATER.get())) {
            event.setCanceled(true);
            if (!level.isClientSide()) {
                CauldronBrewingData data = level.getData(ModAttachments.CAULDRON_BREWING.get());
                if (!data.isPurified(pos)) {
                    data.markPurified(pos);
                    level.setData(ModAttachments.CAULDRON_BREWING.get(), data);
                    if (!player.getAbilities().instabuild) {
                        held.shrink(1);
                    }
                    level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    player.displayClientMessage(
                            Component.translatable("message.guimi_mod.cauldron_purified"), true);
                }
            }
            return;
        }

        // 搅拌合成：纯水锅 + 手持木棍 → 搅拌
        if (held.is(Items.STICK)) {
            if (level.isClientSide()) {
                // 仅在服务端已是纯水锅时才拦截，客户端无附件数据，统一拦截交给服务端裁决
                event.setCanceled(true);
                return;
            }
            CauldronBrewingData data = level.getData(ModAttachments.CAULDRON_BREWING.get());
            if (!data.isPurified(pos)) {
                return;
            }
            event.setCanceled(true);
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6F, 1.4F);

            int stir = data.addStir(pos);
            if (stir < STIRS_REQUIRED) {
                level.setData(ModAttachments.CAULDRON_BREWING.get(), data);
                player.displayClientMessage(Component.translatable(
                        "message.guimi_mod.cauldron_stir", stir, STIRS_REQUIRED), true);
                return;
            }

            // 第三次搅拌：收集锅内掉落物并匹配配方
            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class,
                    new AABB(pos).expandTowards(0.0, 1.0, 0.0));
            Map<Item, Integer> available = new HashMap<>();
            for (ItemEntity ie : items) {
                ItemStack s = ie.getItem();
                available.merge(s.getItem(), s.getCount(), Integer::sum);
            }

            BrewingRecipe recipe = BrewingRecipeManager.findMatch(available);
            if (recipe == null) {
                // 配方不符：重置搅拌计数，允许玩家补料后重试
                data.markPurified(pos);
                level.setData(ModAttachments.CAULDRON_BREWING.get(), data);
                player.displayClientMessage(
                        Component.translatable("message.guimi_mod.cauldron_no_recipe"), true);
                return;
            }

            // 配方卷轴校验：必须先研读过该魔药的配方才可炼制
            if (!player.getData(ModAttachments.READ_RECIPES).contains(recipe.id())) {
                data.markPurified(pos);
                level.setData(ModAttachments.CAULDRON_BREWING.get(), data);
                player.displayClientMessage(Component.translatable(
                        "message.guimi_mod.cauldron_not_learned",
                        Component.translatable("item.guimi_mod." + recipe.id())), true);
                return;
            }

            consumeIngredients(items, recipe);
            ItemStack result = recipe.createResult();
            ItemEntity out = new ItemEntity(level,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, result);
            out.setDeltaMovement(0.0, 0.15, 0.0);
            level.addFreshEntity(out);

            // 合成完成：清空纯水锅状态并还原为空炼药锅
            data.clear(pos);
            level.setData(ModAttachments.CAULDRON_BREWING.get(), data);
            level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
            level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.displayClientMessage(
                    Component.translatable("message.guimi_mod.cauldron_brewed"), true);
        }
    }

    /** 按配方所需数量从锅内掉落物中扣除材料。 */
    private static void consumeIngredients(List<ItemEntity> items, BrewingRecipe recipe) {
        Map<Item, Integer> need = new HashMap<>(recipe.ingredients());
        for (ItemEntity ie : items) {
            ItemStack s = ie.getItem();
            Item item = s.getItem();
            int req = need.getOrDefault(item, 0);
            if (req <= 0) {
                continue;
            }
            int take = Math.min(req, s.getCount());
            s.shrink(take);
            need.put(item, req - take);
            if (s.isEmpty()) {
                ie.discard();
            } else {
                ie.setItem(s);
            }
        }
    }
}
