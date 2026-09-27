package com.wan.gmmod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.DisguiseData;
import com.wan.gmmod.content.disguise.HumanoidDisguises;
import com.wan.gmmod.content.sequences.Sequences;
import com.wan.gmmod.mixin.WalkAnimationStateAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * 变形渲染切换（无面人 · 序列 6）。
 * <p>
 * 在 {@link RenderPlayerEvent.Pre} 判断玩家是否处于 {@code MOB} 变形状态：
 * <ul>
 *     <li>是：取消默认玩家渲染，改用对应怪物的模型 + 纹理渲染（护甲与手持物品随怪物模型
 *     自然隐藏，更利于伪装），并按预设缩放适配视觉高度。碰撞箱、战斗数值不受影响；</li>
 *     <li>观众（空想家途径）玩家看别人时看穿变形，仍渲染真实玩家；</li>
 *     <li>名称标签：为伪装的其他玩家附上其真实名字（不改名，需配合「欺诈」类能力隐藏）。</li>
 * </ul>
 * 通过临时怪物实例承载渲染，并把玩家的朝向 / 行走动画 / 挥手 / 受击等状态拷贝过去，
 * 使伪装外观随玩家动作实时联动。
 */
@EventBusSubscriber(modid = GuimiMod.MODID, value = Dist.CLIENT)
public class DisguiseRenderHandler {

    /** 每种怪物类型缓存一个渲染用临时实例，避免每帧新建。 */
    private static final Map<EntityType<?>, Mob> RENDER_MOBS = new HashMap<>();

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        DisguiseData data = player.getData(ModAttachments.DISGUISE_STATE);
        if (!data.isMob()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        // 观众（空想家途径）克制：观察他人时看穿变形，渲染真实玩家
        Player viewer = mc.player;
        if (viewer != null && viewer != player
                && Sequences.fromKey(viewer.getData(ModAttachments.PATHWAY)) == Sequences.Pathway.VISIONARY) {
            return;
        }

        HumanoidDisguises.Entry entry = HumanoidDisguises.get(data.mobId());
        if (entry == null) {
            return;
        }
        Mob mob = getOrCreate(entry.type(), level);
        if (mob == null) {
            return;
        }

        float partialTick = event.getPartialTick();
        copyState(player, mob, mc);

        // 取消默认玩家渲染，改画怪物
        event.setCanceled(true);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        float s = entry.scale();
        pose.scale(s, s, s);

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        @SuppressWarnings("unchecked")
        EntityRenderer<Mob> renderer = (EntityRenderer<Mob>) (EntityRenderer<?>) dispatcher.getRenderer(mob);
        renderer.render(mob, mob.getYRot(), partialTick, pose, event.getMultiBufferSource(), event.getPackedLight());

        pose.popPose();
    }

    /** 取出（或懒创建）指定类型的渲染临时怪物；维度变化时重建。变形界面的预览也复用此缓存。 */
    public static Mob getOrCreate(EntityType<?> type, ClientLevel level) {
        Mob mob = RENDER_MOBS.get(type);
        if (mob != null && mob.level() == level) {
            return mob;
        }
        Entity created = type.create(level);
        if (!(created instanceof Mob newMob)) {
            return null;
        }
        newMob.setNoAi(true);
        newMob.setSilent(true);
        RENDER_MOBS.put(type, newMob);
        return newMob;
    }

    /** 把玩家的朝向 / 姿态 / 动画状态拷贝到临时怪物，使伪装随玩家动作联动。 */
    private static void copyState(Player player, Mob mob, Minecraft mc) {
        mob.setPos(player.getX(), player.getY(), player.getZ());
        mob.setYRot(player.getYRot());
        mob.yRotO = player.yRotO;
        mob.setXRot(player.getXRot());
        mob.xRotO = player.xRotO;
        mob.yBodyRot = player.yBodyRot;
        mob.yBodyRotO = player.yBodyRotO;
        mob.yHeadRot = player.yHeadRot;
        mob.yHeadRotO = player.yHeadRotO;
        mob.tickCount = player.tickCount;

        mob.setPose(player.getPose());
        mob.setShiftKeyDown(player.isShiftKeyDown());
        mob.setSprinting(player.isSprinting());
        mob.setSwimming(player.isSwimming());
        mob.setOnGround(player.onGround());

        // 挥手 / 攻击动画
        mob.attackAnim = player.attackAnim;
        mob.oAttackAnim = player.oAttackAnim;
        mob.swinging = player.swinging;
        mob.swingingArm = player.swingingArm;
        mob.swingTime = player.swingTime;
        // 受击红闪
        mob.hurtTime = player.hurtTime;
        mob.hurtDuration = player.hurtDuration;

        // 行走动画：精确拷贝玩家的四肢摆动状态
        WalkAnimationStateAccessor acc = (WalkAnimationStateAccessor) (Object) mob.walkAnimation;
        acc.gmmod$setSpeed(player.walkAnimation.speed());
        acc.gmmod$setSpeedOld(player.walkAnimation.speed(0.0F));
        acc.gmmod$setPosition(player.walkAnimation.position());

        // 名称标签：为其他玩家保留真实名字（不改名）；本地玩家不显示自身名字
        if (player != mc.player) {
            mob.setCustomName(player.getName());
            mob.setCustomNameVisible(true);
        } else {
            mob.setCustomName(null);
            mob.setCustomNameVisible(false);
        }
    }
}
