package com.wan.gmmod.common.event;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.item.CharacteristicItem;
import com.wan.gmmod.common.registry.ModItems;
import com.wan.gmmod.content.characteristics.CharacteristicData;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.EnumMap;

/**
 * 非凡特性被动效果监听（服务端）。
 * <p>
 * 玩家手持任意途径的非凡特性时，持续获得该途径对应的状态效果。
 * 效果以「隐藏」形式施加：{@code visible = false}（不产生粒子）且
 * {@code showIcon = false}（HUD 不显示图标），但增益实际生效。
 * <p>
 * 每条途径绑定一个核心正向效果（见 {@link #PATHWAY_EFFECTS}），
 * 效果等级随序列等级提升：序列号越小越强，序列 9 入门为 0 级，
 * 序列 0 为途径「真神」时达最高级（见 {@link #amplifierForLevel(int)}）。
 */
@EventBusSubscriber(modid = GuimiMod.MODID)
public class CharacteristicEffectEventSubscriber {

    /** 各途径核心正向效果。 */
    private static final EnumMap<Sequences.Pathway, Holder<MobEffect>> PATHWAY_EFFECTS =
            new EnumMap<>(Sequences.Pathway.class);

    static {
        PATHWAY_EFFECTS.put(Sequences.Pathway.FOOL, MobEffects.NIGHT_VISION);       // 占卜家 · 洞察灵视
        PATHWAY_EFFECTS.put(Sequences.Pathway.ERROR, MobEffects.LUCK);              // 偷盗者 · 命运眷顾
        PATHWAY_EFFECTS.put(Sequences.Pathway.DOOR, MobEffects.MOVEMENT_SPEED);     // 学徒 · 空间漫游
        PATHWAY_EFFECTS.put(Sequences.Pathway.PARAGON, MobEffects.DIG_SPEED);       // 通识者 · 工匠效率
        PATHWAY_EFFECTS.put(Sequences.Pathway.HANGED_MAN, MobEffects.ABSORPTION);   // 秘祈人 · 圣堂庇护
        PATHWAY_EFFECTS.put(Sequences.Pathway.SUN, MobEffects.REGENERATION);        // 歌颂者 · 神光治愈
        PATHWAY_EFFECTS.put(Sequences.Pathway.TYRANT, MobEffects.DAMAGE_BOOST);     // 水手 · 巨力
        PATHWAY_EFFECTS.put(Sequences.Pathway.WHITE_TOWER, MobEffects.DAMAGE_RESISTANCE); // 阅读者 · 坚韧体魄
        PATHWAY_EFFECTS.put(Sequences.Pathway.VISIONARY, MobEffects.SLOW_FALLING);  // 观众 · 梦境轻盈
        PATHWAY_EFFECTS.put(Sequences.Pathway.DEATH, MobEffects.FIRE_RESISTANCE);   // 收尸人 · 冥火不焚
        PATHWAY_EFFECTS.put(Sequences.Pathway.DARKNESS, MobEffects.NIGHT_VISION);   // 不眠者 · 黑夜灵视
        PATHWAY_EFFECTS.put(Sequences.Pathway.GIANT, MobEffects.DAMAGE_RESISTANCE); // 战士 · 坚韧体魄
        PATHWAY_EFFECTS.put(Sequences.Pathway.WAR, MobEffects.DAMAGE_BOOST);        // 猎人 · 巨力
        PATHWAY_EFFECTS.put(Sequences.Pathway.HERMIT, MobEffects.LUCK);             // 窥秘人 · 命运眷顾
        PATHWAY_EFFECTS.put(Sequences.Pathway.MOON, MobEffects.REGENERATION);       // 药师 · 月华治愈
        PATHWAY_EFFECTS.put(Sequences.Pathway.MOTHER, MobEffects.SATURATION);       // 耕种者 · 丰收饱食
        PATHWAY_EFFECTS.put(Sequences.Pathway.ABYSS, MobEffects.DAMAGE_BOOST);      // 罪犯 · 巨力
        PATHWAY_EFFECTS.put(Sequences.Pathway.CHAINED, MobEffects.DAMAGE_RESISTANCE); // 囚犯 · 坚韧体魄
        PATHWAY_EFFECTS.put(Sequences.Pathway.WITCH, MobEffects.MOVEMENT_SPEED);    // 刺客 · 迅捷
        PATHWAY_EFFECTS.put(Sequences.Pathway.JUSTICE, MobEffects.ABSORPTION);      // 仲裁人 · 圣堂庇护
        PATHWAY_EFFECTS.put(Sequences.Pathway.BLACK_EMPEROR, MobEffects.LUCK);      // 律师 · 命运眷顾
        PATHWAY_EFFECTS.put(Sequences.Pathway.WHEEL, MobEffects.LUCK);              // 怪物 · 命运眷顾
    }

    @SubscribeEvent
    public static void onPlayerTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player player) || !(entity.level() instanceof ServerLevel)) {
            return;
        }
        if (player.isDeadOrDying()) {
            return;
        }
        CharacteristicData data = heldCharacteristic(player);
        if (data == null) {
            return;
        }
        Sequences.Pathway pathway = Sequences.fromKey(data.pathway());
        if (pathway == null) {
            return;
        }
        Holder<MobEffect> effect = PATHWAY_EFFECTS.get(pathway);
        if (effect == null) {
            return;
        }
        int level = Math.max(0, Math.min(Sequences.MAX_LEVEL, data.level()));
        // 隐藏图标 + 隐藏粒子，但效果真实生效（持续 2 秒，每 tick 刷新维持）
        player.addEffect(new MobEffectInstance(
                effect, 40, amplifierForLevel(level), false, false, false));
    }

    /** 序列号越小越强：9 → 0 级，0（真神）→ 3 级。 */
    private static int amplifierForLevel(int level) {
        return Math.max(0, (Sequences.MAX_LEVEL - level) / 3);
    }

    /** 读取主手 / 副手上的非凡特性数据，无特性返回 {@code null}。 */
    private static CharacteristicData heldCharacteristic(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(ModItems.CHARACTERISTIC.get())) {
                CharacteristicData data = CharacteristicItem.getData(stack);
                if (data != null) {
                    return data;
                }
            }
        }
        return null;
    }
}