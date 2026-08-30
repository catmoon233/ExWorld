package net.exmo.exworld.client.voice;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.exmo.exworld.client.battle.BattleClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

/**
 * Optional client bridge for VoiceCastAddon. The bridge is deliberately reflection based so an
 * ExWorld client without VoiceCastAddon still loads normally. Voice results become battle UI
 * selection intents; this class never calls the addon's spell-casting payload.
 */
public final class VoiceCastAddonCompat {
    private static final String VOICE_MANAGER = "com.yelle233.voicecastaddon.client.VoiceRecognitionManager";
    private static final String TEMPLATE_MANAGER = "com.yelle233.voicecastaddon.client.audio.VoiceTemplateManager";
    private static final String VOICE_KEY_NAME = "key.exworld.voice_select";
    private static final String CATEGORY = "key.categories.exworld";
    private static final long MIN_PACKET_GAP_MS = 75L;

    private static final KeyMapping VOICE_KEY = new KeyMapping(VOICE_KEY_NAME, GLFW.GLFW_KEY_T, CATEGORY);
    private static boolean wasDown;
    private static boolean recognitionInProgress;
    private static boolean registered;
    private static long lastRecognitionAt;
    private static String recordingTemplate;

    private VoiceCastAddonCompat() {}

    public static void register(IEventBus modBus) {
        if (registered) return;
        registered = true;
        modBus.addListener(VoiceCastAddonCompat::registerKey);
        NeoForge.EVENT_BUS.addListener(VoiceCastAddonCompat::clientTick);
        NeoForge.EVENT_BUS.addListener(VoiceCastAddonCompat::registerClientCommands);
    }

    private static void registerKey(RegisterKeyMappingsEvent event) {
        event.register(VOICE_KEY);
        if (VoiceCastBridge.available()) VoiceCastBridge.initialize();
    }

    private static void registerClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("exworldvoice")
                .then(net.minecraft.commands.Commands.literal("record")
                        .then(net.minecraft.commands.Commands.argument("id", StringArgumentType.string())
                                .executes(context -> beginRecording(StringArgumentType.getString(context, "id")))))
                .then(net.minecraft.commands.Commands.literal("delete")
                        .then(net.minecraft.commands.Commands.argument("id", StringArgumentType.string())
                                .executes(context -> deleteTemplate(StringArgumentType.getString(context, "id")))))
                .then(net.minecraft.commands.Commands.literal("list")
                        .executes(context -> listTemplates())));
    }

    private static void clientTick(ClientTickEvent.Post ignored) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            if (wasDown && VoiceCastBridge.isListening()) VoiceCastBridge.stopAudio();
            wasDown = false;
            return;
        }

        // The optional mixin disables VoiceCastAddon's original direct-cast controller. This
        // guard also makes the bridge inert when the optional mod is not installed.
        if (!VoiceCastBridge.available()) return;

        boolean down = VOICE_KEY.isDown();
        if (down && !wasDown && !recognitionInProgress && canStartListening()) {
            if (!VoiceCastBridge.startListening()) {
                message(Component.translatable("voice.exworld.start_failed"));
            } else if (recordingTemplate != null) {
                message(Component.translatable("voice.exworld.recording", recordingTemplate));
            } else {
                message(Component.translatable("voice.exworld.listening"));
            }
        }

        if (!down && wasDown && VoiceCastBridge.isListening() && !recognitionInProgress) {
            recognitionInProgress = true;
            Thread thread = new Thread(VoiceCastAddonCompat::processRecognition, "exworld-voice-recognize");
            thread.setDaemon(true);
            thread.start();
        }
        wasDown = down;
    }

    private static boolean canStartListening() {
        return recordingTemplate != null || BattleClient.active();
    }

    private static void processRecognition() {
        try {
            byte[] audio = recordingTemplate != null ? VoiceCastBridge.stopAudio() : new byte[0];
            Object matched = recordingTemplate == null ? VoiceCastBridge.matchAudio() : null;
            String target = matched == null ? null : matched.toString();
            Minecraft.getInstance().execute(() -> finishRecognition(target, audio));
        } catch (Throwable error) {
            Minecraft.getInstance().execute(() -> {
                message(Component.translatable("voice.exworld.recognition_failed"));
                recognitionInProgress = false;
            });
        }
    }

    private static void finishRecognition(String matched, byte[] audio) {
        try {
            if (recordingTemplate != null) {
                String target = recordingTemplate;
                recordingTemplate = null;
                if (audio.length == 0 || !VoiceCastBridge.saveTemplate(target, audio)) {
                    message(Component.translatable("voice.exworld.record_failed", target));
                } else {
                    message(Component.translatable("voice.exworld.record_saved", target));
                }
                return;
            }

            if (matched == null || matched.isBlank()) {
                message(Component.translatable("voice.exworld.no_match"));
                return;
            }

            if (isEndTurnCommand(matched)) {
                message(BattleClient.voiceEndTurn());
            } else if (isIntroSkipCommand(matched)) {
                message(BattleClient.voiceSkipIntro());
            } else {
                message(BattleClient.selectCardFromVoice(matched));
            }
        } finally {
            recognitionInProgress = false;
            lastRecognitionAt = System.currentTimeMillis();
        }
    }

    private static boolean isEndTurnCommand(String value) {
        return matches(value, "skip_turn", "end_turn", "pass_turn", "finish_turn", "结束回合", "跳过回合", "结束行动");
    }

    private static boolean isIntroSkipCommand(String value) {
        return matches(value, "skip_intro", "skip_deployment", "跳过开场", "跳过介绍");
    }

    private static boolean matches(String value, String... names) {
        String normalized = normalize(value);
        for (String name : names) {
            if (normalized.equals(normalize(name)) || normalized.endsWith(":" + normalize(name))) return true;
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private static int beginRecording(String value) {
        if (!VoiceCastBridge.available()) {
            message(Component.translatable("voice.exworld.addon_missing"));
            return 0;
        }
        try {
            String id = ResourceLocation.parse(value).toString();
            recordingTemplate = id;
            message(Component.translatable("voice.exworld.record_ready", id));
            return 1;
        } catch (Exception error) {
            message(Component.translatable("voice.exworld.invalid_id", value));
            return 0;
        }
    }

    private static int deleteTemplate(String value) {
        try {
            String id = ResourceLocation.parse(value).toString();
            if (!VoiceCastBridge.deleteTemplate(id)) return 0;
            message(Component.translatable("voice.exworld.record_deleted", id));
            return 1;
        } catch (Exception error) {
            return 0;
        }
    }

    private static int listTemplates() {
        Collection<?> templates = VoiceCastBridge.templates();
        message(Component.translatable("voice.exworld.template_count", templates.size()));
        templates.stream().map(Objects::toString).sorted().forEach(value -> message(Component.literal("  " + value)));
        return templates.size();
    }

    private static void message(Component component) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && component != null) minecraft.player.displayClientMessage(component, true);
    }

    private static final class VoiceCastBridge {
        private VoiceCastBridge() {}

        static boolean available() {
            try {
                Class.forName(VOICE_MANAGER, false, VoiceCastAddonCompat.class.getClassLoader());
                Class.forName(TEMPLATE_MANAGER, false, VoiceCastAddonCompat.class.getClassLoader());
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }

        static void initialize() {
            invokeStatic(VOICE_MANAGER, "initialize");
        }

        static boolean startListening() {
            if (System.currentTimeMillis() - lastRecognitionAt < MIN_PACKET_GAP_MS) return false;
            Object result = invokeStatic(VOICE_MANAGER, "startListening");
            return result instanceof Boolean value && value;
        }

        static boolean isListening() {
            Object result = invokeStatic(VOICE_MANAGER, "isListening");
            return result instanceof Boolean value && value;
        }

        static byte[] stopAudio() {
            Object result = invokeStatic(VOICE_MANAGER, "stopListeningAndGetAudio");
            return result instanceof byte[] bytes ? bytes : new byte[0];
        }

        static Object matchAudio() {
            Object result = invokeStatic(VOICE_MANAGER, "stopListeningAndMatch");
            return result;
        }

        static boolean saveTemplate(String id, byte[] audio) {
            return invokeTemplate("saveTemplate", id, audio);
        }

        static boolean deleteTemplate(String id) {
            return invokeTemplate("deleteTemplates", id, null);
        }

        static Collection<?> templates() {
            Object result = invokeStatic(TEMPLATE_MANAGER, "getTemplatedSpells");
            return result instanceof Collection<?> values ? values : java.util.List.of();
        }

        private static boolean invokeTemplate(String methodName, String id, byte[] audio) {
            try {
                Class<?> manager = Class.forName(TEMPLATE_MANAGER);
                ResourceLocation resource = ResourceLocation.parse(id);
                Method method = audio == null
                        ? manager.getMethod(methodName, ResourceLocation.class)
                        : manager.getMethod(methodName, ResourceLocation.class, byte[].class);
                if (audio == null) method.invoke(null, resource);
                else method.invoke(null, resource, audio);
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }

        private static Object invokeStatic(String className, String methodName) {
            try {
                Class<?> owner = Class.forName(className);
                return owner.getMethod(methodName).invoke(null);
            } catch (Throwable ignored) {
                return null;
            }
        }
    }
}
