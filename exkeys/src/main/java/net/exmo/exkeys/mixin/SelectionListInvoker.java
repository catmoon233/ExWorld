package net.exmo.exkeys.mixin;

import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Collection;

/** replaceEntries is protected on the list base class, not declared on KeyBindsList. */
@Mixin(AbstractSelectionList.class)
public interface SelectionListInvoker {
    @Invoker("replaceEntries")
    void exkeys$replaceEntries(Collection<?> entries);
}
