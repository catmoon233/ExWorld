package net.exmo.exworld.progress;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.network.QuestNetwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.exmo.exworld.world.WorldSystem;

/** NeoForge adapter that turns trusted vanilla events into the small QuestModule interface. */
public final class PlayerProgressSystem {
    private static final PlayerResourceVault VAULT = new PlayerResourceVault();
    private static final QuestModule QUESTS = new QuestModule(VAULT);
    private static final RewardDistributor REWARDS = new RewardDistributor(VAULT, QUESTS);
    static { QUESTS.rewards(REWARDS); }
    private PlayerProgressSystem() {}
    public static PlayerResourceVault vault() { return VAULT; } public static QuestModule quests() { return QUESTS; } public static RewardDistributor rewards() { return REWARDS; }
    public static void registerEvents() { NeoForge.EVENT_BUS.register(PlayerProgressSystem.class); NeoForge.EVENT_BUS.addListener(QuestDataReloadListener::register); PlayerProgressCommands.register(); }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { if (event.getEntity() instanceof ServerPlayer player) { net.exmo.exworld.battle.BattleSystem.playerCards().migrateGold(player.getServer(), player.getUUID()); QUESTS.validateContent(player); QuestNetwork.send(player); if (QUESTS.mailbox(player).stream().anyMatch(message -> !message.read())) player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("mail.exworld.arrived")); } }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) { if (event.getEntity() instanceof ServerPlayer player && !net.exmo.exworld.battle.BattleSystem.isParticipating(player.getUUID()) && player.tickCount % 20 == 0) { QUESTS.checkInventory(player); QUESTS.observe(player,new QuestEvent(QuestObjective.Type.LOCATION,ResourceLocation.fromNamespaceAndPath(Exworld.MODID,"position"),"",player.level().dimension().location().toString(),player.getX(),player.getY(),player.getZ(),1)); QUESTS.observe(player,new QuestEvent(QuestObjective.Type.WORLD_TILE,ResourceLocation.fromNamespaceAndPath(Exworld.MODID,WorldSystem.snapshot(player).currentTileId()),"",player.level().dimension().location().toString(),player.getX(),player.getY(),player.getZ(),1)); QuestNetwork.send(player); } }
    @SubscribeEvent public static void death(LivingDeathEvent event) { if (event.getSource().getEntity() instanceof ServerPlayer player) { Entity dead=event.getEntity(); QUESTS.observe(player,new QuestEvent(QuestObjective.Type.KILL,BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType()),"",player.level().dimension().location().toString(),dead.getX(),dead.getY(),dead.getZ(),1)); QuestNetwork.send(player); } }
    @SubscribeEvent public static void block(PlayerInteractEvent.RightClickBlock event) { if (event.getEntity() instanceof ServerPlayer player) { var pos=event.getPos(); QUESTS.observe(player,new QuestEvent(QuestObjective.Type.BLOCK_INTERACT,BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(pos).getBlock()),"",player.level().dimension().location().toString(),pos.getX(),pos.getY(),pos.getZ(),1)); QuestNetwork.send(player); } }
    @SubscribeEvent public static void entity(PlayerInteractEvent.EntityInteract event) { if (event.getEntity() instanceof ServerPlayer player) { Entity target=event.getTarget(); var tags=target.getTags(); if(tags.isEmpty()) QUESTS.observe(player,new QuestEvent(QuestObjective.Type.ENTITY_INTERACT,BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()),"",player.level().dimension().location().toString(),target.getX(),target.getY(),target.getZ(),1)); else for(String tag:tags) QUESTS.observe(player,new QuestEvent(QuestObjective.Type.ENTITY_INTERACT,BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()),tag,player.level().dimension().location().toString(),target.getX(),target.getY(),target.getZ(),1)); QuestNetwork.send(player); } }
}
