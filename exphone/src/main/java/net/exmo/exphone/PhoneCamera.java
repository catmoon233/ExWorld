package net.exmo.exphone;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import net.exmo.exphone.api.PhoneCameraControl;

/** Live viewfinder. The world is copied before the phone UI is drawn, and pixels are read only on the shutter frame. */
final class PhoneCamera {
    static final ResourceLocation VIEW = ResourceLocation.fromNamespaceAndPath(ExPhone.MODID, "phone/viewfinder");

    private static TextureTarget preview;
    private static CameraType previous;
    private static boolean active, registered, capture, front, free;
    private static int captureWait;
    private static double zoom = 1;

    static boolean isOpen() { return active; }
    static boolean front() { return front; }
    static boolean free() { return free; }

    static boolean locked() {
        return PhoneCameraControl.locked();
    }

    static void open() {
        if (active) return;
        if (!registered) {
            NeoForge.EVENT_BUS.addListener(PhoneCamera::beforeTick);
            NeoForge.EVENT_BUS.addListener(PhoneCamera::onLevel);
            NeoForge.EVENT_BUS.addListener(PhoneCamera::hand);
            NeoForge.EVENT_BUS.addListener(PhoneCamera::fov);
            registered = true;
        }
        active = true;
        capture = false;
        captureWait = 0;
        front = false;
        free = false;
        zoom = 1;
        Minecraft mc = Minecraft.getInstance();
        previous = mc.options.getCameraType();
        PhoneCameraControl.held(true);
        applyType();
    }

    static void setFree(boolean value) { free = value; }

    static boolean flip() {
        if (!active) return false;
        if (locked()) {
            PhoneApricity.notice("当前场景正在控制镜头");
            return false;
        }
        front = !front;
        applyType();
        return true;
    }

    static void zoom(double value) { zoom = Math.clamp(value, 1, 3); }

    /** Left-drag deltas are in page pixels. Positive x looks the same way vanilla mouse-look does. */
    static void drag(double x, double y) {
        if (!active || locked()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        float scale = free ? 0.42f : 0.28f;
        player.turn(x * scale, y * scale);
        player.setXRot(Math.clamp(player.getXRot(), -90f, 90f));
        player.xRotO = player.getXRot();
        player.yRotO = player.getYRot();
        player.setYHeadRot(player.getYRot());
        if (!free) player.setYBodyRot(player.getYRot());
    }

    static void shoot() {
        if (!active) {
            PhoneApricity.notice("相机未打开");
            return;
        }
        if (!capture) {
            capture = true;
            captureWait = 0;
        }
    }

    static void close() {
        if (!active) return;
        PhoneCameraControl.held(false);
        active = false;
        capture = false;
        captureWait = 0;
        front = false;
        free = false;
        Minecraft mc = Minecraft.getInstance();
        if (previous != null) mc.options.setCameraType(previous);
        previous = null;
        mc.getTextureManager().release(VIEW);
        if (preview != null) preview.destroyBuffers();
        preview = null;
    }

    private static void applyType() {
        if (!active || locked()) return;
        Minecraft.getInstance().options.setCameraType(front ? CameraType.THIRD_PERSON_FRONT : CameraType.FIRST_PERSON);
    }

    private static void beforeTick(ClientTickEvent.Pre event) {
        if (!active) return;
        applyType();
        if (!capture) return;
        if (++captureWait > 40) {
            capture = false;
            captureWait = 0;
            PhoneApricity.notice("拍照失败，画面没有刷新");
        }
    }

    private static void hand(RenderHandEvent event) { if (active) event.setCanceled(true); }

    private static void fov(ViewportEvent.ComputeFov event) {
        if (!active || locked()) return;
        double factor = zoom / (free ? 1.25 : 1);
        event.setFOV(event.getFOV() / factor);
    }

    private static void onLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !PhoneApricity.isOpen()) {
            close();
            return;
        }
        RenderTarget source = mc.getMainRenderTarget();
        if (source.width <= 0 || source.height <= 0) return;
        ensurePreview(mc, source);
        copy(source, preview);
        if (!capture) {
            source.bindWrite(false);
            return;
        }
        capture = false;
        captureWait = 0;
        double scale = Math.min(1, Math.min(1280.0 / source.width, 720.0 / source.height));
        int width = Math.max(1, (int) Math.round(source.width * scale));
        int height = Math.max(1, (int) Math.round(source.height * scale));
        TextureTarget photo = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        try {
            copy(source, photo);
            PhonePhotoClient.save(Screenshot.takeScreenshot(photo));
        } catch (RuntimeException error) {
            PhoneApricity.notice("拍照失败");
        } finally {
            photo.destroyBuffers();
            source.bindWrite(false);
        }
    }

    private static void ensurePreview(Minecraft mc, RenderTarget source) {
        double previewScale = Math.min(1, Math.min(960.0 / source.width, 540.0 / source.height));
        int width = Math.max(1, (int) Math.round(source.width * previewScale));
        int height = Math.max(1, (int) Math.round(source.height * previewScale));
        if (preview != null && preview.width == width && preview.height == height) return;
        if (preview != null) preview.destroyBuffers();
        preview = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        linearize(preview.getColorTextureId());
        mc.getTextureManager().register(VIEW, new AbstractTexture() {
            @Override public int getId() { return preview == null ? 0 : preview.getColorTextureId(); }
            @Override public void load(ResourceManager manager) { }
            @Override public void releaseId() { }
        });
    }

    private static void linearize(int texture) {
        if (texture <= 0) return;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }

    private static void copy(RenderTarget source, RenderTarget destination) {
        if (source == null || destination == null) return;
        if (source.width <= 0 || source.height <= 0 || destination.width <= 0 || destination.height <= 0) return;
        int read = GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int draw = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destination.frameBufferId);
        GL30.glBlitFramebuffer(0, source.height, source.width, 0, 0, 0, destination.width, destination.height,
                GL30.GL_COLOR_BUFFER_BIT, GL30.GL_LINEAR);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
    }
}
