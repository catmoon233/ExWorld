package com.wan.gmmod.client;

import com.wan.gmmod.common.network.packet.MarionetteActionPacket;
import com.wan.gmmod.common.network.packet.MarionetteControlInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 「共享视野」客户端状态：摄像机绑定 / 输入捕获与上报 / 目标拾取。
 * <p>
 * 由 {@link com.wan.gmmod.common.network.packet.MarionetteViewPacket} 开关；
 * 期间玩家本体的移动输入被 {@code MovementInputUpdateEvent} 清零（挂机），
 * 捕获值随视角每刻打包发往服务端驱动秘偶；左右键被
 * {@code InteractionKeyMappingTriggered} 拦截转为秘偶动作；潜行键退出。
 */
public final class MarionetteControlClientState {
    /** 近战攻击拾取距离（米） */
    private static final double MELEE_PICK_RANGE = 4.0;
    /** 能力（远程）拾取距离（米） */
    private static final double ABILITY_PICK_RANGE = 32.0;

    private static boolean controlling;
    private static int entityId = -1;

    /** MovementInputUpdateEvent 捕获的本刻移动输入（清零前的原始值） */
    private static float capturedForward;
    private static float capturedStrafe;
    private static boolean capturedJump;

    private MarionetteControlClientState() {}

    public static boolean isControlling() {
        return controlling;
    }

    /** 服务端开关共享视野：绑定 / 恢复摄像机实体。 */
    public static void setControlling(boolean active, int id) {
        controlling = active;
        entityId = active ? id : -1;
        Minecraft mc = Minecraft.getInstance();
        if (active) {
            Entity mob = mc.level != null ? mc.level.getEntity(id) : null;
            if (mob != null) {
                mc.setCameraEntity(mob);
            }
            // 实体尚未同步到客户端时，由 tick() 重试绑定
        } else if (mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
    }

    /** 当前被操控的秘偶（客户端实体），不存在返回 null。 */
    public static Mob getControlled() {
        Minecraft mc = Minecraft.getInstance();
        if (!controlling || mc.level == null) {
            return null;
        }
        return mc.level.getEntity(entityId) instanceof Mob mob && mob.isAlive() ? mob : null;
    }

    /** 由 MovementInputUpdateEvent 调用：记录清零前的移动输入。 */
    public static void captureInput(float forward, float strafe, boolean jump) {
        capturedForward = forward;
        capturedStrafe = strafe;
        capturedJump = jump;
    }

    /** 每客户端刻：维护摄像机绑定、视角跟手同步与输入上报。 */
    public static void tick() {
        if (!controlling) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        Mob mob = getControlled();
        if (mob == null) {
            // 秘偶消失 / 尚未同步：暂回本体视角，服务端稍后会下发关闭包
            if (mc.getCameraEntity() != player) {
                mc.setCameraEntity(player);
            }
            return;
        }
        if (mc.getCameraEntity() != mob) {
            mc.setCameraEntity(mob);
        }
        // 视角跟手：把玩家鼠标旋转实时拷贝给秘偶（本地渲染平滑），服务端另行权威同步
        mob.yRotO = player.yRotO;
        mob.setYRot(player.getYRot());
        mob.yBodyRotO = player.yRotO;
        mob.yBodyRot = player.getYRot();
        mob.yHeadRotO = player.yRotO;
        mob.yHeadRot = player.getYRot();
        mob.xRotO = player.xRotO;
        mob.setXRot(player.getXRot());
        // 潜行键：退出操控
        if (mc.options.keyShift.isDown()) {
            PacketDistributor.sendToServer(
                    new MarionetteActionPacket(MarionetteActionPacket.EXIT, -1));
            return;
        }
        // 上报本刻输入（WASD / 跳跃 / 视角），驱动服务端移动
        PacketDistributor.sendToServer(new MarionetteControlInputPacket(
                capturedForward, capturedStrafe, capturedJump,
                player.getYRot(), player.getXRot()));
    }

    /** 左键 / 右键动作：从秘偶眼位置沿视线拾取目标后发包。 */
    public static void sendAction(int action) {
        Minecraft mc = Minecraft.getInstance();
        Mob mob = getControlled();
        int targetId = -1;
        if (mob != null) {
            double range = action == MarionetteActionPacket.ATTACK ? MELEE_PICK_RANGE : ABILITY_PICK_RANGE;
            Vec3 start = mob.getEyePosition();
            Vec3 look = mob.getViewVector(1.0F);
            Vec3 end = start.add(look.scale(range));
            AABB box = mob.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
            EntityHitResult hit = ProjectileUtil.getEntityHitResult(mob, start, end, box,
                    e -> e != mc.player && e instanceof LivingEntity && e.isPickable(),
                    range * range);
            if (hit != null) {
                targetId = hit.getEntity().getId();
            }
        }
        PacketDistributor.sendToServer(new MarionetteActionPacket(action, targetId));
    }
}
