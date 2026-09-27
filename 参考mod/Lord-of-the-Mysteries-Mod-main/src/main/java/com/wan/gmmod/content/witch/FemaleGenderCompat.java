package com.wan.gmmod.content.witch;

import com.wan.gmmod.GuimiMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Female Gender Mod（{@code wildfire_gender}）软兼容层。
 * <p>
 * 通过反射尝试同步玩家在 Wildfire 性别模组中的性别设置，使女巫「性别转换」
 * 能真正切换为女性模型。未安装该模组、或其 API 版本不匹配时静默降级——
 * 此时 {@code FEMALE_FORM} 附件仍作为权威标记保留，仅缺少第三方模型表现。
 * <p>
 * 反射失败一律吞掉异常，绝不影响主流程。
 */
public final class FemaleGenderCompat {
    private FemaleGenderCompat() {}

    /** Female Gender Mod 的 modid。 */
    private static final String MODID = "wildfire_gender";
    /** 该模组是否已加载（启动期一次性判定）。 */
    private static final boolean LOADED = ModList.get() != null && ModList.get().isLoaded(MODID);

    public static boolean isLoaded() {
        return LOADED;
    }

    /**
     * 同步玩家性别到 Female Gender Mod。
     *
     * @param player 目标玩家
     * @param female {@code true} 设为女性，{@code false} 设为男性
     */
    public static void setFemale(ServerPlayer player, boolean female) {
        if (!LOADED) {
            return;
        }
        try {
            Object config = resolveConfig(player.getUUID());
            if (config == null) {
                return;
            }
            Object gender = resolveGenderConstant(female);
            if (gender == null) {
                return;
            }
            // 尝试常见的性别写入方法名
            for (String name : new String[] {"updateGender", "setGender"}) {
                Method setter = findMethod(config.getClass(), name, gender.getClass());
                if (setter != null) {
                    setter.setAccessible(true);
                    setter.invoke(config, gender);
                    return;
                }
            }
        } catch (Throwable t) {
            GuimiMod.LOGGER.debug("Female Gender Mod 兼容层同步失败（已忽略）：{}", t.toString());
        }
    }

    /** 反射获取 Wildfire 的玩家配置对象。 */
    private static Object resolveConfig(UUID uuid) throws ReflectiveOperationException {
        Class<?> main = Class.forName("com.wildfire.main.WildfireGender");
        for (String name : new String[] {"getPlayerById", "getPlayer"}) {
            Method m = findMethod(main, name, UUID.class);
            if (m != null) {
                m.setAccessible(true);
                return m.invoke(null, uuid);
            }
        }
        return null;
    }

    /** 反射取 Wildfire 的 {@code Gender} 枚举常量（FEMALE / MALE）。 */
    private static Object resolveGenderConstant(boolean female) {
        String constant = female ? "FEMALE" : "MALE";
        for (String cls : new String[] {"com.wildfire.main.Gender", "com.wildfire.main.entitydata.Gender"}) {
            try {
                Class<?> genderClass = Class.forName(cls);
                if (genderClass.isEnum()) {
                    for (Object c : genderClass.getEnumConstants()) {
                        if (constant.equals(((Enum<?>) c).name())) {
                            return c;
                        }
                    }
                }
            } catch (ClassNotFoundException ignored) {
                // 尝试下一个候选类名
            }
        }
        return null;
    }

    /** 按名称与单参数类型查找方法（含参数类型可赋值的兜底匹配）。 */
    private static Method findMethod(Class<?> owner, String name, Class<?> paramType) {
        try {
            return owner.getMethod(name, paramType);
        } catch (NoSuchMethodException e) {
            for (Method m : owner.getMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].isAssignableFrom(paramType)) {
                    return m;
                }
            }
            return null;
        }
    }
}
