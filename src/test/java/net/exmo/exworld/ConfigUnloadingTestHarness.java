package net.exmo.exworld;

import java.lang.reflect.Constructor;
import java.util.concurrent.locks.ReentrantLock;

import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

/** Regression test for config callbacks receiving an unloading event. */
public final class ConfigUnloadingTestHarness {
    public static void main(String[] args) throws Exception {
        Constructor<ModConfig> constructor = ModConfig.class.getDeclaredConstructor(
                ModConfig.Type.class,
                net.neoforged.fml.config.IConfigSpec.class,
                net.neoforged.fml.ModContainer.class,
                String.class,
                ReentrantLock.class);
        constructor.setAccessible(true);
        ModConfig config = constructor.newInstance(
                ModConfig.Type.SERVER,
                Config.SERVER_SPEC,
                null,
                "exworld-server.toml",
                new ReentrantLock());

        Config.onLoad(new ModConfigEvent.Unloading(config));
    }
}
