package net.exmo.exworld.phone;

import java.util.UUID;
import net.exmo.exphone.api.PhoneEconomy;
import net.exmo.exphone.api.PhoneLedger;
import net.exmo.exphone.api.PhoneNpc;
import net.exmo.exphone.api.PhoneNpcs;
import net.exmo.exworld.npc.data.NpcCatalog;
import net.exmo.exworld.progress.PlayerProgressSystem;
import net.exmo.exworld.progress.PlayerResourceVault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/** Connects ExWorld gold and NPCs to the phone without the phone depending on ExWorld. */
public final class ExworldPhoneBridge {
    private ExworldPhoneBridge() {}

    public static void connect() {
        PhoneEconomy.connect(PhoneEconomy.GOLD, new VaultGold());
        PhoneNpcs.connect(new NpcCards());
        if (FMLEnvironment.dist == Dist.CLIENT) ExworldPhoneClientBridge.connect();
    }

    private static final class VaultGold implements PhoneLedger {
        @Override
        public long balance(MinecraftServer server, UUID player, ResourceLocation currency) {
            return PlayerProgressSystem.vault().balance(server, player, PlayerResourceVault.GOLD);
        }

        @Override
        public long add(MinecraftServer server, UUID player, ResourceLocation currency, long amount) {
            return PlayerProgressSystem.vault().add(server, player, PlayerResourceVault.GOLD, amount);
        }

        @Override
        public boolean take(MinecraftServer server, UUID player, ResourceLocation currency, long amount) {
            return PlayerProgressSystem.vault().take(server, player, PlayerResourceVault.GOLD, amount);
        }

        @Override
        public void set(MinecraftServer server, UUID player, ResourceLocation currency, long amount) {
            PlayerProgressSystem.vault().set(server, player, PlayerResourceVault.GOLD, amount);
        }
    }

    private static final class NpcCards implements net.exmo.exphone.api.PhoneNpcSource {
        @Override
        public java.util.List<PhoneNpc> list(MinecraftServer server) {
            return NpcCatalog.get(server).documents().stream()
                    .map(document -> new PhoneNpc(document.id(), document.displayName()))
                    .toList();
        }

        @Override
        public PhoneNpc find(MinecraftServer server, String id) {
            return NpcCatalog.get(server).document(id)
                    .map(document -> new PhoneNpc(document.id(), document.displayName()))
                    .orElse(null);
        }
    }
}
