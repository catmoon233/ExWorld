package net.exmo.exkeys.mixin;

import net.exmo.exkeys.KeyListVisibility;
import net.exmo.exkeys.client.ExKeysClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(KeyBindsList.class)
public abstract class KeyBindsListMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void exkeys$hide(KeyBindsScreen screen, Minecraft minecraft, CallbackInfo ci) {
        List<KeyBindsList.Entry> entries = List.copyOf(((KeyBindsList) (Object) this).children());
        boolean[] category = new boolean[entries.size()];
        boolean[] hidden = new boolean[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            KeyBindsList.Entry entry = entries.get(i);
            if (entry instanceof KeyBindsList.KeyEntry keyEntry) {
                hidden[i] = ExKeysClient.hides(((KeyEntryAccess) keyEntry).exkeys$key());
            } else {
                category[i] = true;
            }
        }
        boolean[] keep = KeyListVisibility.keep(category, hidden);
        List<KeyBindsList.Entry> next = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            if (keep[i]) next.add(entries.get(i));
        }
        ((SelectionListInvoker) (Object) this).exkeys$replaceEntries(next);
    }
}
