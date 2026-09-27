package net.exmo.exphone;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ExPhone.MODID)
public final class ExPhone {
    public static final String MODID = "exphone";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ExPhone(IEventBus modBus) {
        PhoneSystem.register(modBus);
    }
}
