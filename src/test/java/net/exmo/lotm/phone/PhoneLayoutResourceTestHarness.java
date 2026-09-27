package net.exmo.lotm.phone;

import java.nio.file.Files;
import java.nio.file.Path;

/** Guards the fixed-layout rules that the in-game HTML renderer also uses for hit testing. */
public final class PhoneLayoutResourceTestHarness {
    public static void main(String[] args) throws Exception {
        String html = Files.readString(Path.of("exphone/src/main/resources/assets/apricityui/apricity/exphone/phone.html"));
        String interactiveCameraRules = html.substring(html.indexOf(".camera-chrome, .camera-chrome.quiet"));

        check(!interactiveCameraRules.contains("transform:translateX(-50%)"),
                "camera controls must be centered with layout coordinates, not a visual-only transform");
        check(interactiveCameraRules.contains(".camera-screen .camera-bottom { left:38px; right:38px;"),
                "shutter row reserves symmetric physical hit bounds");
        check(interactiveCameraRules.contains(".camera-screen .camera-modes { position:absolute; left:50%; top:0; width:148px; max-width:148px; height:40px; margin-left:-79px;"),
                "photo and free mode buttons stay a compact centered group");
        check(interactiveCameraRules.contains(".camera-screen .camera-modes .camera-mode { display:flex; align-items:center; justify-content:center; flex:0 0 72px; width:72px;"),
                "photo and free mode buttons keep separate fixed hit bounds");
        check(interactiveCameraRules.contains(".camera-screen .camera-zoom { position:absolute; left:50%; right:auto; bottom:118px; width:160px; max-width:160px; height:36px; margin-left:-85px;"),
                "zoom buttons stay a compact centered group");
        check(interactiveCameraRules.contains(".camera-screen .camera-zoom .camera-control { display:flex; align-items:center; justify-content:center; flex:0 0 50px; width:50px;"),
                "zoom buttons keep separate fixed hit bounds");
        check(!interactiveCameraRules.contains("left:80px; right:80px") && !interactiveCameraRules.contains("left:72px; right:72px"),
                "mode and zoom bars must not stretch across the screen");
        check(html.contains(".screen.photo-screen .photo-stage { top:56px; right:0; bottom:72px; left:0;"),
                "photo stage leaves the header and the action row inside the phone");
        check(html.contains(".photo-full { display:block; position:absolute; left:50%; top:50%; width:1px; height:1px;"),
                "photo preview is not stretched to the stage before its real aspect size is applied");
        check(!html.contains(".photo-full { display:block; max-width:100%; max-height:100%; width:100%; height:100%;"),
                "photo preview must not use a stretched 100% box");
        check(html.contains(".screen.photo-screen .photo-view-footer { position:absolute; left:16px; right:16px; bottom:18px; height:44px;"),
                "photo actions stay above the phone bezel");
        check(html.contains(".screen.photo-screen .photo-action { display:flex; align-items:center; justify-content:center; flex:0 0 108px; width:108px;"),
                "share and delete stay separate compact buttons");
        int[] landscape = contained(1920, 1080, 322, 550);
        check(landscape[0] == 322 && landscape[1] == 181, "landscape photos fit the width and keep 16:9");
        int[] portrait = contained(1080, 1920, 322, 550);
        check(portrait[0] == 309 && portrait[1] == 550, "portrait photos fit the height without stretching");

        System.out.println("phone layout resource rules ok");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static int[] contained(int imageW, int imageH, int boxW, int boxH) {
        double scale = Math.min(boxW / (double) imageW, boxH / (double) imageH);
        return new int[]{Math.min(boxW, (int) Math.floor(imageW * scale)), Math.min(boxH, (int) Math.floor(imageH * scale))};
    }
}
