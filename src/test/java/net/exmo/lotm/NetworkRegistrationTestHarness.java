package net.exmo.lotm;

import java.lang.reflect.Method;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Regression test for clientbound payloads being registered on the common path. */
public final class NetworkRegistrationTestHarness {
    public static void main(String[] args) throws Exception {
        RegisterPayloadHandlersEvent event = new RegisterPayloadHandlersEvent();
        invokePayloadRegistration("net.exmo.exphone.PhoneSystem", event);
        invokePayloadRegistration("net.exmo.lotm.story.StorySystem", event);

        check(NetworkRegistry.getCodec(
                ResourceLocation.fromNamespaceAndPath("exphone", "phone_state"),
                ConnectionProtocol.PLAY,
                PacketFlow.CLIENTBOUND) != null, "phone_state is registered clientbound");
        check(NetworkRegistry.getCodec(
                ResourceLocation.fromNamespaceAndPath("lotm", "open_letter"),
                ConnectionProtocol.PLAY,
                PacketFlow.CLIENTBOUND) != null, "open_letter is registered clientbound");
    }

    private static void invokePayloadRegistration(String className, RegisterPayloadHandlersEvent event) throws Exception {
        Class<?> type = Class.forName(className);
        Method method = type.getDeclaredMethod("payloads", RegisterPayloadHandlersEvent.class);
        method.setAccessible(true);
        method.invoke(null, event);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
