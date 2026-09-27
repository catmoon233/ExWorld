package net.exmo.exphone.client;

import net.exmo.exphone.PhoneItems;
import net.exmo.exphone.PhoneItem;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

public final class PhoneItemColors {
    private PhoneItemColors() {}

    public static void items(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint == 0 ? PhoneItem.tint(stack) : -1, PhoneItems.PHONE.get());
    }
}
