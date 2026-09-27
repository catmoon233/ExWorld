package net.exmo.exworld.phone;

import net.exmo.exphone.api.PhoneCameraControl;
import net.exmo.exphone.api.PhoneMicrophone;
import net.exmo.exphone.api.PhoneUiHooks;
import net.exmo.exworld.client.battle.BattleClient;
import net.exmo.exworld.client.camera.AdvancedCameraDirector;
import net.exmo.exworld.client.perspective.CameraProfile;
import net.exmo.exworld.client.perspective.DungeonPerspective;
import net.exmo.exworld.client.perspective.FirstPersonToggle;
import net.exmo.exworld.client.webview.WebView2Window;
import net.exmo.exworld.mystery.client.InWorldMemory;
import net.minecraft.resources.ResourceLocation;

/** Client hooks the phone calls instead of importing ExWorld itself. */
public final class ExworldPhoneClientBridge {
    private static final ResourceLocation CAMERA = ResourceLocation.fromNamespaceAndPath("exworld", "phone_camera");

    private ExworldPhoneClientBridge() {}

    public static void connect() {
        PhoneCameraControl.lock(() -> BattleClient.active()
                || AdvancedCameraDirector.active()
                || DungeonPerspective.active());
        PhoneCameraControl.onHeld(held -> {
            FirstPersonToggle.setPhoneCameraHeld(held);
            if (held) DungeonPerspective.setOverride(CAMERA, CameraProfile.VANILLA);
            else DungeonPerspective.clearOverride(CAMERA);
        });
        PhoneUiHooks.beforeOpen(() -> {
            try {
                WebView2Window.close();
            } catch (RuntimeException ignored) {
            }
        });
        PhoneMicrophone.tap(InWorldMemory::voice);
    }
}
