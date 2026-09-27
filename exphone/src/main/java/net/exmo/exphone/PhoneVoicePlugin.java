package net.exmo.exphone;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.LocationalSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.ServerEvent;
import de.maxhenkel.voicechat.api.events.SoundPacketEvent;
import de.maxhenkel.voicechat.api.events.StaticSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import de.maxhenkel.voicechat.api.packets.Packet;
import de.maxhenkel.voicechat.api.packets.SoundPacket;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 电话通话的 SVC 插件，服务端通话逻辑的语音通道。
 * 不使用 Simple Voice Chat 的群组功能：通话中把双方麦克风包点对点转发为静态音频
 * （{@link MicrophonePacketEvent} 拦截 + {@link VoicechatServerApi#sendStaticSoundPacketTo}），
 * 并把发往通话参与者的其他语音包拦截掉，互相只听得到对方。
 * 客户端复用本插件捕获麦克风 PCM 供录音机使用；所有 run_command 挂断链路在
 * {@link PhoneCalls} 内维护，插件只在语音层工作。
 */
@ForgeVoicechatPlugin
public final class PhoneVoicePlugin implements VoicechatPlugin {
    private static volatile VoicechatServerApi serverApi;

    @Override
    public String getPluginId() {
        return "exphone_call";
    }

    @Override
    public void initialize(VoicechatApi api) {
        if (api instanceof VoicechatServerApi server) serverApi = server;
        PhoneCalls.attach();
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            registration.registerEvent(ClientSoundEvent.class, event -> {
                PhoneRecorder.capture(event.getRawAudio());
                net.exmo.exphone.api.PhoneMicrophone.hear(event.getRawAudio());
            });
        }
        registration.registerEvent(MicrophonePacketEvent.class, PhoneVoicePlugin::routeMic);
        registration.registerEvent(EntitySoundPacketEvent.class, PhoneVoicePlugin::isolate);
        registration.registerEvent(LocationalSoundPacketEvent.class, PhoneVoicePlugin::isolate);
        registration.registerEvent(StaticSoundPacketEvent.class, PhoneVoicePlugin::isolate);
        registration.registerEvent(PlayerDisconnectedEvent.class, event -> PhoneCalls.onVoiceOffline(event.getPlayerUuid()));
        registration.registerEvent(VoicechatServerStartedEvent.class, event -> serverApi = event.getVoicechat());
        registration.registerEvent(VoicechatServerStoppedEvent.class, event -> {
            serverApi = null;
            PhoneCalls.onVoiceServerStopped();
        });
    }

    /** 通话双方是否都连着语音；语音服务尚未启动时放行（由后续对话流程兜底）。 */
    static boolean canConnect(UUID left, UUID right) {
        VoicechatServerApi api = serverApi;
        if (api == null) return true;
        VoicechatConnection a = api.getConnectionOf(left);
        VoicechatConnection b = api.getConnectionOf(right);
        return a != null && b != null && a.isConnected() && b.isConnected();
    }

    /** 把通话者的麦克风包改成点对点静态音频，替代周边的广播。 */
    private static void routeMic(MicrophonePacketEvent event) {
        refresh(event);
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null) return;
        UUID senderId = sender.getPlayer().getUuid();
        UUID peer = PhoneCalls.peerOf(senderId);
        if (peer == null) return;
        event.cancel();
        VoicechatServerApi api = serverApi;
        if (api == null) return;
        VoicechatConnection receiver = api.getConnectionOf(peer);
        if (receiver == null) return;
        StaticSoundPacket routed = event.getPacket().staticSoundPacketBuilder()
                .channelId(channel(senderId, peer))
                .build();
        api.sendStaticSoundPacketTo(receiver, routed);
    }

    /** 通话期间拦截一切进入参与者耳机的语音，只放行我们转发的对端通话频道。 */
    private static void isolate(SoundPacketEvent<?> event) {
        refresh(event);
        VoicechatConnection receiver = event.getReceiverConnection();
        Packet raw = event.getPacket();
        if (receiver == null || !(raw instanceof SoundPacket packet)) return;
        UUID receiverId = receiver.getPlayer().getUuid();
        UUID peer = PhoneCalls.peerOf(receiverId);
        if (peer == null) return;
        if (peer.equals(packet.getSender()) && channel(peer, receiverId).equals(packet.getChannelId())) return;
        event.cancel();
    }

    private static void refresh(ServerEvent event) {
        if (serverApi == null) serverApi = event.getVoicechat();
    }

    private static UUID channel(UUID sender, UUID receiver) {
        return UUID.nameUUIDFromBytes(("exphone:" + sender + "→" + receiver).getBytes(StandardCharsets.UTF_8));
    }
}
