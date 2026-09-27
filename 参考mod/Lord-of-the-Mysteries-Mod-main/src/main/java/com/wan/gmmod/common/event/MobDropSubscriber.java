package com.wan.gmmod.common.event;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.registry.ModItems;
import com.wan.gmmod.content.entities.AdultPegasusEntity;
import com.wan.gmmod.content.entities.AdultUnicornEntity;
import com.wan.gmmod.content.entities.DawnRoosterEntity;
import com.wan.gmmod.content.entities.DeathRavenEntity;
import com.wan.gmmod.content.entities.EvilPantherEntity;
import com.wan.gmmod.content.entities.FireSalamanderEntity;
import com.wan.gmmod.content.entities.FrogMeatPuppetEntity;
import com.wan.gmmod.content.entities.GrayBirdGrandmaEntity;
import com.wan.gmmod.content.entities.BlackSpottedFrogEntity;
import com.wan.gmmod.content.entities.BlackScaleSharkEntity;
import com.wan.gmmod.content.entities.HornachisGoatEntity;
import com.wan.gmmod.content.entities.HumanSkinShadowEntity;
import com.wan.gmmod.content.entities.LavaDemonEntity;
import com.wan.gmmod.content.entities.LivingCorpseEntity;
import com.wan.gmmod.content.entities.NightmareEyeEntity;
import com.wan.gmmod.content.entities.NightmareShadowEntity;
import com.wan.gmmod.content.entities.OneEyedBullEntity;
import com.wan.gmmod.content.entities.RainBirdEntity;
import com.wan.gmmod.content.entities.RottenShepherdEntity;
import com.wan.gmmod.content.entities.SilverWarBearEntity;
import com.wan.gmmod.content.entities.SkinlessBloodCatEntity;
import com.wan.gmmod.content.entities.SpiritEntity;
import com.wan.gmmod.content.entities.ThousandFacedHunterEntity;
import com.wan.gmmod.content.entities.VengefulShadowEntity;
import com.wan.gmmod.content.entities.WhiteFoxEntity;
import com.wan.gmmod.content.entities.WidowSpiderEntity;
import com.wan.gmmod.content.entities.WolfmanEntity;
import com.wan.gmmod.content.entities.WraithEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.function.Supplier;

/**
 * 生物掉落注入：为模组生物与原版章鱼追加配方材料掉落。
 * <p>
 * 每个生物掉落「主材料 ×1 + 副材料 ×1~N」（K先生与魔女教会-布朗丝·索伦不掉落专属材料）。
 * 覆盖：拉瓦章鱼（鱿鱼）、怨灵、灵体、狼人、人皮幽影、邪纹黑豹、千面狩猎者、白尾赤狐、
 * 寡妇巨蛛、霍纳奇斯山羊、岩浆之魔、告死乌鸦、告雨鸟、噩梦之影、复仇之影、活尸、火蝾螈、
 * 灰鸟祖母、独眼白牛、腐烂牧者、黑斑青蛙、蛙肉布套人、黑鳞鲨、银白战熊、无皮血猫、
 * 成年独角兽、成年飞马、黎明雄鸡、噩梦邪眼。
 */
@EventBusSubscriber(modid = GuimiMod.MODID)
public class MobDropSubscriber {

    /** 单份掉落定义：物品供应商 + 数量上限（1~max 随机）。 */
    private record Drop(Supplier<? extends net.minecraft.world.item.Item> item, int max) {
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }

        // 原版鱿鱼 → 拉瓦章鱼血液（占卜家魔药主料）
        if (entity instanceof Squid) {
            drop(event, entity, new Drop(ModItems.LAVA_OCTOPUS_BLOOD::get, 2));
            return;
        }

        // ===== 被缚者 / 愚者途径相关 =====

        // 怨灵 → 古老怨灵的粉尘 + 残余灵性
        if (entity instanceof WraithEntity) {
            drop(event, entity, new Drop(ModItems.ANCIENT_WRAITH_DUST::get, 2));
            drop(event, entity, new Drop(ModItems.ANCIENT_WRAITH_RESIDUAL_SPIRITUALITY::get, 1));
            return;
        }
        // 灵体 → 徘徊的幽灵
        if (entity instanceof SpiritEntity) {
            drop(event, entity, new Drop(ModItems.MAT_048::get, 1));
            return;
        }
        // 狼人 → 獠牙 + 血液 + 黑毛
        if (entity instanceof WolfmanEntity) {
            drop(event, entity, new Drop(ModItems.MAT_124::get, 1));
            drop(event, entity, new Drop(ModItems.MAT_122::get, 2));
            drop(event, entity, new Drop(ModItems.MAT_123::get, 3));
            return;
        }
        // 活尸 → 心脏 + 血肉 + 眼珠（50%）
        if (entity instanceof LivingCorpseEntity) {
            drop(event, entity, new Drop(ModItems.MAT_088::get, 1));
            drop(event, entity, new Drop(ModItems.MAT_089::get, 2));
            if (entity.level().random.nextBoolean()) {
                drop(event, entity, new Drop(ModItems.MAT_264::get, 1));
            }
            return;
        }

        // ===== 愚者 / 无面人途径相关 =====

        // 人皮幽影 → 人皮幽影特性（无面人魔药主料）
        if (entity instanceof HumanSkinShadowEntity) {
            drop(event, entity, new Drop(ModItems.HUMAN_SKIN_SHADOW_CHARACTERISTIC::get, 1));
            return;
        }
        // 千面狩猎者 → 血液 + 异变脑垂体
        if (entity instanceof ThousandFacedHunterEntity) {
            drop(event, entity, new Drop(ModItems.THOUSAND_FACED_HUNTER_BLOOD::get, 2));
            drop(event, entity, new Drop(ModItems.THOUSAND_FACED_HUNTER_PITUITARY::get, 1));
            return;
        }

        // ===== 隐者途径相关 =====

        // 噩梦之影 → 噩梦之影
        if (entity instanceof NightmareShadowEntity) {
            drop(event, entity, new Drop(ModItems.MAT_011::get, 1));
            return;
        }
        // 噩梦邪眼（双形态均掉落）→ 角膜 + 脓液
        if (entity instanceof NightmareEyeEntity) {
            drop(event, entity, new Drop(ModItems.MAT_013::get, 1));
            drop(event, entity, new Drop(ModItems.MAT_012::get, 3));
            return;
        }
        // 灰鸟祖母 → 眼珠 + 羽毛
        if (entity instanceof GrayBirdGrandmaEntity) {
            drop(event, entity, new Drop(ModItems.MAT_102::get, 1));
            drop(event, entity, new Drop(ModItems.MAT_103::get, 3));
            return;
        }
        // 鹿头兽人未实装实体，其材料（角/血液）由战利品表等其他途径获得

        // ===== 审判者途径相关 =====

        // 银白战熊 → 右掌 + 血液
        if (entity instanceof SilverWarBearEntity) {
            drop(event, entity, new Drop(ModItems.MAT_197::get, 1));
            drop(event, entity, new Drop(ModItems.MAT_198::get, 2));
            return;
        }

        // ===== 隐者 / 深渊途径相关 =====

        // 无皮血猫 → 心脏 + 血液
        if (entity instanceof SkinlessBloodCatEntity) {
            drop(event, entity, new Drop(ModItems.MAT_061::get, 1));
            drop(event, entity, new Drop(ModItems.MAT_062::get, 2));
            return;
        }

        // ===== 月亮途径相关 =====

        // 成年独角兽 → 结晶 + 血液
        if (entity instanceof AdultUnicornEntity) {
            drop(event, entity, new Drop(ModItems.MAT_041::get, 1));
            drop(event, entity, new Drop(ModItems.MAT_042::get, 2));
            return;
        }
        // 成年飞马 → 独角飞马的角 + 血液
        if (entity instanceof AdultPegasusEntity) {
            drop(event, entity, new Drop(ModItems.MAT_056::get, 1));
            drop(event, entity, new Drop(ModItems.MAT_055::get, 2));
            return;
        }
        // 渴血兽未实装实体，其材料（心脏/血液）由战利品表等其他途径获得

        // ===== 太阳途径相关 =====

        // 黎明雄鸡 → 红冠 + 血液（血液较稀有）
        if (entity instanceof DawnRoosterEntity) {
            drop(event, entity, new Drop(ModItems.MAT_238::get, 1));
            if (entity.level().random.nextInt(3) > 0) {
                drop(event, entity, new Drop(ModItems.MAT_239::get, 1));
            }
            return;
        }

        // ===== 其他途径材料生物 =====

        // 邪纹黑豹 → 脊髓液（魔术师魔药主料）
        if (entity instanceof EvilPantherEntity) {
            drop(event, entity, new Drop(ModItems.EVIL_PANTHER_SPINAL_FLUID::get, 1));
            return;
        }
        // 白尾赤狐 → 胃袋 + 血液（博学者魔药主料）
        if (entity instanceof WhiteFoxEntity) {
            drop(event, entity, new Drop(ModItems.MAT_125::get, 1));
            drop(event, entity, new Drop(ModItems.MAT_126::get, 2));
            return;
        }
        // 寡妇巨蛛 → 丝腺（欢愉魔女魔药主料）
        if (entity instanceof WidowSpiderEntity) {
            drop(event, entity, new Drop(ModItems.MAT_053::get, 1));
            return;
        }
        // 霍纳奇斯灰山羊 → 独角结晶（小丑魔药主料）
        if (entity instanceof HornachisGoatEntity) {
            drop(event, entity, new Drop(ModItems.HORNACIS_GOAT_HORN_CRYSTAL::get, 1));
            return;
        }
        // 岩浆之魔 → 独角 + 熔融之液（深渊恶魔魔药主料）
        if (entity instanceof LavaDemonEntity) {
            drop(event, entity, new Drop(ModItems.MAT_029::get, 1));
            drop(event, entity, new Drop(ModItems.MAT_028::get, 2));
            return;
        }
        // 告死乌鸦 → 眼睛 + 羽毛（掘墓人魔药主料）
        if (entity instanceof DeathRavenEntity) {
            drop(event, entity, new Drop(ModItems.MAT_006::get, 1));
            drop(event, entity, new Drop(ModItems.MAT_007::get, 3));
            return;
        }
        // 告雨鸟 → 眼珠 + 羽毛（耕种者魔药主料）
        if (entity instanceof RainBirdEntity) {
            drop(event, entity, new Drop(ModItems.MAT_008::get, 1));
            drop(event, entity, new Drop(ModItems.MAT_009::get, 3));
            return;
        }
        // 复仇之影 → 最大碎片 + 残留粉末（蔷薇主教魔药主料）
        if (entity instanceof VengefulShadowEntity) {
            drop(event, entity, new Drop(ModItems.MAT_018::get, 1));
            drop(event, entity, new Drop(ModItems.MAT_019::get, 2));
            return;
        }
        // 火蝾螈 → 腺体 + 血液（纵火家魔药主料）
        if (entity instanceof FireSalamanderEntity) {
            drop(event, entity, new Drop(ModItems.MAT_099::get, 1));
            drop(event, entity, new Drop(ModItems.MAT_100::get, 2));
            return;
        }
        // 独眼白牛 → 蛇尾 + 血液（灾祸教士魔药主料）
        if (entity instanceof OneEyedBullEntity) {
            drop(event, entity, new Drop(ModItems.MAT_119::get, 1));
            drop(event, entity, new Drop(ModItems.MAT_120::get, 2));
            return;
        }
        // 腐烂牧者 → 脓液 + 灵体结晶（安魂师魔药主料）
        if (entity instanceof RottenShepherdEntity) {
            drop(event, entity, new Drop(ModItems.MAT_276::get, 2));
            if (entity.level().random.nextInt(2) == 0) {
                drop(event, entity, new Drop(ModItems.MAT_109::get, 1));
            }
            return;
        }
        // 黑斑青蛙 → 体液（收尸人魔药辅料）
        if (entity instanceof BlackSpottedFrogEntity) {
            drop(event, entity, new Drop(ModItems.MAT_241::get, 2));
            return;
        }
        // 黑斑青蛙肉布套人 → 结晶（收尸人魔药主料）
        if (entity instanceof FrogMeatPuppetEntity) {
            drop(event, entity, new Drop(ModItems.MAT_242::get, 1));
            return;
        }
        // 黑鳞鲨 → 脑垂体 + 鳍翅（航海家魔药主料）
        if (entity instanceof BlackScaleSharkEntity) {
            drop(event, entity, new Drop(ModItems.MAT_250::get, 1));
            drop(event, entity, new Drop(ModItems.MAT_251::get, 2));
        }
    }

    /** 向掉落物列表追加 1~max 份物品。 */
    private static void drop(LivingDropsEvent event, LivingEntity entity, Drop drop) {
        int count = 1 + entity.level().random.nextInt(Math.max(1, drop.max()));
        ItemStack stack = new ItemStack(drop.item().get(), count);
        event.getDrops().add(new ItemEntity(entity.level(),
                entity.getX(), entity.getY(), entity.getZ(), stack));
    }
}
