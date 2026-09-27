package net.exmo.exphone;

import com.sighs.apricityui.event.MouseEvent;
import com.sighs.apricityui.init.Element;

import static net.exmo.exphone.PhoneApricity.*;

/** Camera controls. Only photo and free-look are exposed; every control is a real button. */
final class PhoneCameraUi {
    private static int zoom = 1;
    private static String mode = "照片";
    private static long flashUntil;

    static void draw(Element page) {
        boolean opening = !PhoneCamera.isOpen();
        if (opening) {
            mode = "照片";
            zoom = 1;
        }
        PhoneCamera.open();
        if (opening) PhoneCamera.setFree(false);

        attr(page, "class", "page camera-page");
        Element view = node(page, "div", "camera-view", "");
        Element texture = node(view, "texture", "camera-texture", "");
        attr(texture, "src", PhoneCamera.VIEW.toString());
        attr(texture, "blur", "true");
        id(node(page, "div", "camera-flash", ""), "camera-flash");
        Element chrome = id(node(page, "div", "camera-chrome", ""), "camera-chrome");
        Element top = node(chrome, "div", "camera-top", "");
        control(top, "返回", "camera-back", PhoneApricity::back);
        Element modes = node(top, "div", "camera-modes", "");
        id(control(modes, "照片", "camera-mode" + ("照片".equals(mode) ? " selected" : ""), () -> selectMode("照片")), "camera-mode-photo");
        id(control(modes, "自由", "camera-mode" + ("自由".equals(mode) ? " selected" : ""), () -> selectMode("自由")), "camera-mode-free");

        Element bottom = node(chrome, "div", "camera-bottom", "");
        Element gallery = control(bottom, "相册", "camera-thumbnail", () -> go("album"));
        refreshers.add(() -> reconcile(gallery, PhonePhotoClient.album().toString(), () -> {
            gallery.clearChildren();
            if (PhonePhotoClient.album().isEmpty()) node(gallery, "span", "thumbnail-placeholder", "相册");
            else image(gallery, PhonePhotoClient.album().getFirst(), false, "camera-thumbnail-image");
        }));

        Element shutter = node(bottom, "button", "camera-control camera-shutter", "");
        shutter.setAttribute("type", "button");
        attr(shutter, "title", "拍照");
        node(shutter, "span", "camera-shutter-core", "");
        shutter.addEventListener("mousedown", event -> {
            if (!(event instanceof MouseEvent mouse) || mouse.button != 0) return;
            if (shotAt > 0) return;
            if (delay == 0) PhoneUtilities.shutter();
            else shotAt = System.currentTimeMillis() + delay * 1000L;
        });
        Element flip = control(bottom, PhoneCamera.front() ? "前置" : "后置", "camera-flip", PhoneCameraUi::flip);
        id(flip, "camera-flip");
        attr(flip, "title", "切换前置 / 后置镜头");

        Element zoomBar = node(chrome, "div", "camera-zoom", "");
        for (int i = 1; i <= 3; i++) {
            int value = i;
            Element zoomButton = control(zoomBar, i + "×", zoom == i ? "selected" : "", () -> {
                zoom = value;
                PhoneCamera.zoom(value);
                updateZoom();
            });
            id(zoomButton, "camera-zoom-" + i);
        }
    }

    static boolean controlTarget(Object target) {
        if (!(target instanceof Element element)) return false;
        if (element.getClassName().contains("camera-control")) return true;
        for (Object node : element.getRoute()) {
            if (node instanceof Element part && part.getClassName().contains("camera-control")) return true;
        }
        return false;
    }

    static void tick(long now) {
        if (!ui.route.equals("camera") || flashUntil == 0 || now < flashUntil) return;
        flashUntil = 0;
        attr(el("camera-flash"), "class", "camera-flash");
    }

    static void flash() {
        flashUntil = System.currentTimeMillis() + 140;
        attr(el("camera-flash"), "class", "camera-flash active");
    }

    private static void flip() {
        if (!PhoneCamera.flip()) return;
        Element button = el("camera-flip");
        if (button != null) button.setInnerText(PhoneCamera.front() ? "前置" : "后置");
    }

    private static void selectMode(String next) {
        if (next.equals(mode)) return;
        mode = next;
        PhoneCamera.setFree("自由".equals(mode));
        mark("camera-mode-photo", "照片".equals(mode));
        mark("camera-mode-free", "自由".equals(mode));
    }

    private static void mark(String elementId, boolean selected) {
        Element button = el(elementId);
        if (button != null) attr(button, "class", "camera-control camera-mode" + (selected ? " selected" : ""));
    }

    private static void updateZoom() {
        for (int i = 1; i <= 3; i++) {
            Element button = el("camera-zoom-" + i);
            if (button != null) attr(button, "class", "camera-control" + (i == zoom ? " selected" : ""));
        }
    }

    private static Element control(Element parent, String text, String clazz, Runnable action) {
        Element button = node(parent, "button", "camera-control " + clazz, text);
        button.setAttribute("type", "button");
        button.addEventListener("mousedown", event -> {
            if (event instanceof MouseEvent mouse && mouse.button == 0) action.run();
        });
        return button;
    }
}
