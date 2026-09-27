package com.wan.gmmod;

import com.mojang.blaze3d.platform.InputConstants;
import com.wan.gmmod.client.render.SpiritGlowLayer;
import com.wan.gmmod.client.render.SpiritRenderer;
import com.wan.gmmod.client.render.SpiritThreadOutlineLayer;
import com.wan.gmmod.client.render.ShadowCreatureRenderer;
import com.wan.gmmod.client.render.TransformVisualLayer;
import com.wan.gmmod.client.render.WraithRenderer;
import com.wan.gmmod.client.render.WolfmanRenderer;
import com.wan.gmmod.client.render.MermaidRenderer;
import com.wan.gmmod.client.render.NunRenderer;
import com.wan.gmmod.client.render.PriestRenderer;
import com.wan.gmmod.client.render.PaperFigurineRenderer;
import com.wan.gmmod.client.render.HumanSkinShadowRenderer;
import com.wan.gmmod.client.render.EvilPantherRenderer;
import com.wan.gmmod.client.render.ThousandFacedHunterRenderer;
import com.wan.gmmod.client.render.WhiteFoxRenderer;
import com.wan.gmmod.client.render.WidowSpiderRenderer;
import com.wan.gmmod.client.render.HornachisGoatRenderer;
import com.wan.gmmod.client.render.LavaDemonRenderer;
import com.wan.gmmod.client.render.MrKRenderer;
import com.wan.gmmod.client.render.BrownSilkSolenRenderer;
import com.wan.gmmod.client.render.AbyssDemonRenderer;
import com.wan.gmmod.client.render.EvilBlackCatRenderer;
import com.wan.gmmod.client.render.DeathRavenRenderer;
import com.wan.gmmod.client.render.RainBirdRenderer;
import com.wan.gmmod.client.render.NightmareShadowRenderer;
import com.wan.gmmod.client.render.VengefulShadowRenderer;
import com.wan.gmmod.client.render.LivingCorpseRenderer;
import com.wan.gmmod.client.render.FireSalamanderRenderer;
import com.wan.gmmod.client.render.GrayBirdGrandmaRenderer;
import com.wan.gmmod.client.render.OneEyedBullRenderer;
import com.wan.gmmod.client.render.RottenShepherdRenderer;
import com.wan.gmmod.client.render.BlackSpottedFrogRenderer;
import com.wan.gmmod.client.render.FrogMeatPuppetRenderer;
import com.wan.gmmod.client.render.BlackScaleSharkRenderer;
import com.wan.gmmod.client.PendulumClientState;
import com.wan.gmmod.client.network.ClientPayloadHandlers;
import com.wan.gmmod.client.BlockHighlightClientState;
import com.wan.gmmod.client.MeditationClientState;
import com.wan.gmmod.client.CocoonClientState;
import com.wan.gmmod.client.DistortionClientState;
import com.wan.gmmod.client.HudClientState;
import com.wan.gmmod.client.MarionetteControlClientState;
import com.wan.gmmod.client.SkillPageClientState;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.client.render.FlyingCardRenderer;
import com.wan.gmmod.client.render.DawnBuffVisualLayer;
import com.wan.gmmod.common.item.SilenceGunItem;
import com.wan.gmmod.common.item.FlameWeaponItem;
import com.wan.gmmod.common.registry.ModEffects;
import com.wan.gmmod.common.registry.ModEntities;
import com.wan.gmmod.common.registry.ModItems;
import com.wan.gmmod.client.KeyBindings;
import com.wan.gmmod.client.gui.DisguiseScreen;
import com.wan.gmmod.common.network.packet.ToggleSpiritVisionPacket;
import com.wan.gmmod.common.network.packet.SilenceGunFirePacket;
import com.wan.gmmod.common.network.packet.SpiritWallTogglePacket;
import com.wan.gmmod.common.network.packet.MeditationTogglePacket;
import com.wan.gmmod.common.network.packet.ToggleCardModePacket;
import com.wan.gmmod.common.network.packet.MarionetteActionPacket;
import com.wan.gmmod.common.network.packet.ConfigureSkillPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@Mod(value = GuimiMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = GuimiMod.MODID, value = Dist.CLIENT)
public class GMmodClient {
    public GMmodClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        ClientPayloadHandlers.register();
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        GuimiMod.LOGGER.info("HELLO FROM CLIENT SETUP");
        GuimiMod.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        event.enqueueWork(() -> {
            EntityRenderers.register(ModEntities.SPIRIT.get(), SpiritRenderer::new);
            EntityRenderers.register(ModEntities.WRAITH.get(), WraithRenderer::new);
            EntityRenderers.register(ModEntities.MERMAID.get(), MermaidRenderer::new);
            EntityRenderers.register(ModEntities.NUN.get(), NunRenderer::new);
            EntityRenderers.register(ModEntities.PRIEST.get(), PriestRenderer::new);
            EntityRenderers.register(ModEntities.SHADOW_CREATURE.get(), ShadowCreatureRenderer::new);
            EntityRenderers.register(ModEntities.WOLFMAN.get(), WolfmanRenderer::new);
            // 飞牌：平放旋转渲染；空气弹：投掷物渲染器（物品为空气 → 无形）
            EntityRenderers.register(ModEntities.FLYING_CARD.get(), FlyingCardRenderer::new);
            EntityRenderers.register(ModEntities.AIR_BULLET.get(), ThrownItemRenderer::new);
            // 子弹：投掷物渲染器，外观跟随装填的子弹物品
            EntityRenderers.register(ModEntities.BULLET.get(), ThrownItemRenderer::new);
            // 纸人：平面广告牌渲染
            EntityRenderers.register(ModEntities.PAPER_FIGURINE.get(), PaperFigurineRenderer::new);
            // 魔女途径弹射物：黑焰 / 冰晶长枪（投掷物渲染器，外观跟随物品）
            EntityRenderers.register(ModEntities.BLACK_FLAME.get(), ThrownItemRenderer::new);
            EntityRenderers.register(ModEntities.ICE_SPEAR.get(), ThrownItemRenderer::new);
            // 战争之红途径弹射物（均以默认物品外观渲染）
            EntityRenderers.register(ModEntities.FLAME_ORB.get(), ThrownItemRenderer::new);
            EntityRenderers.register(ModEntities.FIRE_RAVEN.get(), ThrownItemRenderer::new);
            EntityRenderers.register(ModEntities.FLAME_SPEAR.get(), ThrownItemRenderer::new);
            EntityRenderers.register(ModEntities.FLAME_TRAP.get(), ThrownItemRenderer::new);
            // 灵性符咒投射物：以符咒物品外观渲染
            EntityRenderers.register(ModEntities.TALISMAN.get(), ThrownItemRenderer::new);

            // 新增生物实体渲染器
            EntityRenderers.register(ModEntities.HUMAN_SKIN_SHADOW.get(), HumanSkinShadowRenderer::new);
            EntityRenderers.register(ModEntities.EVIL_PANTHER.get(), EvilPantherRenderer::new);
            EntityRenderers.register(ModEntities.THOUSAND_FACED_HUNTER.get(), ThousandFacedHunterRenderer::new);
            EntityRenderers.register(ModEntities.WHITE_FOX.get(), WhiteFoxRenderer::new);
            EntityRenderers.register(ModEntities.WIDOW_SPIDER.get(), WidowSpiderRenderer::new);
            EntityRenderers.register(ModEntities.HORNACHIS_GOAT.get(), HornachisGoatRenderer::new);
            EntityRenderers.register(ModEntities.LAVA_DEMON.get(), LavaDemonRenderer::new);
            EntityRenderers.register(ModEntities.MR_K.get(), MrKRenderer::new);
            EntityRenderers.register(ModEntities.BROWN_SILK_SOLEN.get(), BrownSilkSolenRenderer::new);
            EntityRenderers.register(ModEntities.ABYSS_DEMON.get(), AbyssDemonRenderer::new);
            EntityRenderers.register(ModEntities.EVIL_BLACK_CAT.get(), EvilBlackCatRenderer::new);
            EntityRenderers.register(ModEntities.DEATH_RAVEN.get(), DeathRavenRenderer::new);
            EntityRenderers.register(ModEntities.RAIN_BIRD.get(), RainBirdRenderer::new);
            EntityRenderers.register(ModEntities.NIGHTMARE_SHADOW.get(), NightmareShadowRenderer::new);
            EntityRenderers.register(ModEntities.VENGEFUL_SHADOW.get(), VengefulShadowRenderer::new);
            EntityRenderers.register(ModEntities.LIVING_CORPSE.get(), LivingCorpseRenderer::new);
            EntityRenderers.register(ModEntities.FIRE_SALAMANDER.get(), FireSalamanderRenderer::new);
            EntityRenderers.register(ModEntities.GRAY_BIRD_GRANDMA.get(), GrayBirdGrandmaRenderer::new);
            EntityRenderers.register(ModEntities.ONE_EYED_BULL.get(), OneEyedBullRenderer::new);
            EntityRenderers.register(ModEntities.ROTTEN_SHEPHERD.get(), RottenShepherdRenderer::new);
            EntityRenderers.register(ModEntities.BLACK_SPOTTED_FROG.get(), BlackSpottedFrogRenderer::new);
            EntityRenderers.register(ModEntities.FROG_MEAT_PUPPET.get(), FrogMeatPuppetRenderer::new);
            EntityRenderers.register(ModEntities.BLACK_SCALE_SHARK.get(), BlackScaleSharkRenderer::new);
            // 配方材料来源生物（新增）
            EntityRenderers.register(ModEntities.SILVER_WAR_BEAR.get(), com.wan.gmmod.client.render.SilverWarBearRenderer::new);
            EntityRenderers.register(ModEntities.SKINLESS_BLOOD_CAT.get(), com.wan.gmmod.client.render.SkinlessBloodCatRenderer::new);
            EntityRenderers.register(ModEntities.ADULT_UNICORN.get(), com.wan.gmmod.client.render.AdultUnicornRenderer::new);
            EntityRenderers.register(ModEntities.ADULT_PEGASUS.get(), com.wan.gmmod.client.render.AdultPegasusRenderer::new);
            EntityRenderers.register(ModEntities.DAWN_ROOSTER.get(), com.wan.gmmod.client.render.DawnRoosterRenderer::new);
            EntityRenderers.register(ModEntities.NIGHTMARE_EYE.get(), com.wan.gmmod.client.render.NightmareEyeRenderer::new);
            // 火焰塑形：模型谓词读取当前形态（0=剑、1=鞭、2=马刀），驱动三套纹理切换
            ItemProperties.register(ModItems.FLAME_WEAPON.get(), GuimiMod.id("flame_form"),
                    (stack, level, entity, seed) -> FlameWeaponItem.getForm(stack));
        });
    }

    /** 灵体之线视野：给所有活体渲染器（含玩家皮肤）追加灵体之线轮廓层与灵体光晕层。 */
    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (EntityType<?> type : event.getEntityTypes()) {
            if (event.getRenderer((EntityType) type) instanceof LivingEntityRenderer living) {
                // 灵体（怨灵 / 灵体实体）：附加灵视光晕层，开启灵视时灵体发光
                if (type == ModEntities.SPIRIT.get() || type == ModEntities.WRAITH.get()) {
                    living.addLayer(new SpiritGlowLayer<>(living));
                }
                living.addLayer(new SpiritThreadOutlineLayer<>(living));
            }
        }
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof LivingEntityRenderer living) {
                living.addLayer(new SpiritThreadOutlineLayer<>(living));
                // 黎明加持视觉层：黎明命甲 / 晨曦之剑 模型显示
                living.addLayer(new DawnBuffVisualLayer<>(living));
                // 变身视觉层：狼人化 / 恶魔化 模型显示
                living.addLayer(new TransformVisualLayer<>(living));
            }
        }
    }

    /** 美人鱼的歌声 / 反占卜：隐藏 Buff，不在 HUD 与背包效果列表中显示。 */
    @SubscribeEvent
    static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerMobEffect(new IClientMobEffectExtensions() {
            @Override
            public boolean isVisibleInInventory(MobEffectInstance instance) {
                return false;
            }

            @Override
            public boolean isVisibleInGui(MobEffectInstance instance) {
                return false;
            }
        }, ModEffects.MERMAID_SONG.get(), ModEffects.ANTI_DIVINATION.get(),
                ModEffects.DAWN_ARMOR_ACTIVE.get(), ModEffects.DAWN_SWORD_ACTIVE.get(),
                ModEffects.WEREWOLF_FORM.get(), ModEffects.DEMON_FORM.get());
    }

    @EventBusSubscriber(modid = GuimiMod.MODID, value = Dist.CLIENT)
    static class GameEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            // 递减灵摆手臂旋转 / 动画的客户端计时
            PendulumClientState.tick();
            // 递减方块高亮描边计时
            BlockHighlightClientState.tick();
            // 冥想姿态：固定视角
            MeditationClientState.tick();
            // 蛛丝蚕茧：递减各实体外壳剩余时长（驱动外壳 / 第一人称滤网消退）
            CocoonClientState.tick();
            // 扭曲区域：递减描边边界计时
            DistortionClientState.tick();
            // 扭曲模式：数字键 1~6 切换当前扭曲类型（移动反向 / 攻击转移 / 偏转 / 关门 / 隔绝 / 劫持）
            if (DistortionClientState.isModeActive()) {
                for (int i = 0; i < 6; i++) {
                    if (InputConstants.isKeyDown(mc.getWindow().getWindow(),
                            GLFW.GLFW_KEY_1 + i)) {
                        DistortionClientState.setSelectedType(i);
                    }
                }
            }
            // V 键：切换灵视（服务端处理夜视 / 灵体可见性）
            while (KeyBindings.SPIRIT_VISION_KEY.consumeClick()) {
                if (mc.player == null) continue;
                if (!com.wan.gmmod.client.SpiritVisionClient.allowed()) {
                    com.wan.gmmod.client.SpiritVisionClient.flash("仅序列九可使用灵视");
                } else {
                    boolean next = !Boolean.TRUE.equals(mc.player.getData(com.wan.gmmod.common.capability.ModAttachments.SPIRIT_VISION));
                    com.wan.gmmod.client.SpiritVisionClient.flash(next ? "灵视已开启" : "灵视已关闭");
                    PacketDistributor.sendToServer(new ToggleSpiritVisionPacket());
                }
            }
            // 技能页状态：页码指示渐隐 / 切页闪烁计时 / 中键长按检测
            SkillPageClientState.tick();
            // 技能槽快捷键：5 个键映射到当前技能页对应槽位（Shift+键 = 将页内选中槽的技能
            // 快速存入该槽位，无需打开配置界面）；扭曲模式下禁用，数字键留给扭曲选型
            if (!DistortionClientState.isModeActive()) {
                for (int i = 0; i < KeyBindings.SKILL_SLOTS.length; i++) {
                    while (KeyBindings.SKILL_SLOTS[i].consumeClick()) {
                    }
                }
            }
            // X 键：切换灵性之墙
            while (KeyBindings.SPIRIT_WALL_KEY.consumeClick()) {
                PacketDistributor.sendToServer(new SpiritWallTogglePacket());
            }
            // P 键：切换冥想
            while (KeyBindings.MEDITATION_KEY.consumeClick()) {
                PacketDistributor.sendToServer(new MeditationTogglePacket());
            }
            // G 键：打开变形选择界面（无面人 · 序列 6，仅愚者途径解锁）
            while (KeyBindings.OPEN_DISGUISE_KEY.consumeClick()) {
                if (hasFoolSequence(6,
                        "message.guimi_mod.fool_locked_disguise")) {
                    Minecraft.getInstance().setScreen(new DisguiseScreen());
                }
            }
            // H 键：隐藏 / 显示模组 HUD
            while (KeyBindings.TOGGLE_HUD_KEY.consumeClick()) {
                HudClientState.toggle();
            }
            // B 键：切换纸牌发射模式（精准单点 / 散射，小丑 · 序列 8，仅愚者途径解锁）
            while (KeyBindings.CARD_MODE_KEY.consumeClick()) {
                if (hasFoolSequence(8,
                        "message.guimi_mod.fool_locked_card")) {
                    PacketDistributor.sendToServer(new ToggleCardModePacket());
                }
            }
            // J 键：打开任务书
            while (KeyBindings.OPEN_QUEST_JOURNAL.consumeClick()) {
            }
            // 共享视野：摄像机绑定维护 + 操控输入上报
            MarionetteControlClientState.tick();
        }

        /** 冥想 / 秘偶操控期间清零移动输入，实现“无法移动”（操控时先捕获再清零）。 */
        @SubscribeEvent
        public static void onMovementInput(MovementInputUpdateEvent event) {
            boolean controlling = MarionetteControlClientState.isControlling();
            if (!MeditationClientState.isMeditating() && !controlling) return;
            var input = event.getInput();
            if (controlling) {
                // 捕获本刻输入用于驱动秘偶，随后清零使本体挂机
                MarionetteControlClientState.captureInput(
                        input.forwardImpulse, input.leftImpulse, input.jumping);
            }
            input.forwardImpulse = 0.0F;
            input.leftImpulse = 0.0F;
            input.up = false;
            input.down = false;
            input.left = false;
            input.right = false;
            input.jumping = false;
        }

        /** 共享视野期间不渲染本体第一人称手臂（摄像机已绑定秘偶）。 */
        @SubscribeEvent
        public static void onRenderHand(RenderHandEvent event) {
            if (MarionetteControlClientState.isControlling()) {
                event.setCanceled(true);
            }
        }

        /** 共享视野期间拦截左右键：转为秘偶近战攻击 / 触发原有能力。 */
        @SubscribeEvent
        public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
            // 扭曲模式：左键确认当前类型目标，右键不在此触发（拖拽在 MouseButton 处理）
            if (DistortionClientState.isModeActive()) {
                if (event.isAttack() && DistortionClientState.fireAttack()) {
                    event.setCanceled(true);
                    event.setSwingHand(false);
                }
                return;
            }
            if (MarionetteControlClientState.isControlling()) {
                if (event.isAttack()) {
                    MarionetteControlClientState.sendAction(MarionetteActionPacket.ATTACK);
                } else if (event.isUseItem()) {
                    MarionetteControlClientState.sendAction(MarionetteActionPacket.ABILITY);
                }
                event.setCanceled(true);
                event.setSwingHand(false);
                return;
            }
        }

        /** 旧技能栏不再拦截滚轮、中键和侧键。 */

        /** 扭曲模式仍用右键拖选。中键、侧键和滚轮不再被技能栏抢走。 */
        @SubscribeEvent
        public static void onMouseButton(InputEvent.MouseButton.Pre event) {
            if (!DistortionClientState.isModeActive()
                    || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                return;
            }
            if (event.getAction() == GLFW.GLFW_PRESS) {
                DistortionClientState.startDrag();
            } else if (event.getAction() == GLFW.GLFW_RELEASE) {
                DistortionClientState.endDrag();
            }
            event.setCanceled(true);
        }

        /** 是否为愚者途径且序列等级已达到要求（序列号越小越强，level ≤ required 即已晋升）。 */
        private static boolean hasFoolSequence(int requiredSequence, String lockMessageKey) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return false;
            }
            String pathway = mc.player.getData(ModAttachments.PATHWAY);
            int level = mc.player.getData(ModAttachments.SEQUENCE_LEVEL);
            if ("fool".equals(pathway) && level <= requiredSequence && level >= 0) {
                return true;
            }
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(lockMessageKey), true);
            return false;
        }

        /** Shift+技能键 1~5：将页内选中槽的技能存入当前页对应槽位（快速配置）。 */
        private static void quickAssign(int localSlot) {            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            int from = SkillPageClientState.getSelectedGlobalSlot();
            if (from < 0) {
                return;
            }
            ResourceLocation id = mc.player.getData(ModAttachments.SKILL_BAR).get(from);
            PacketDistributor.sendToServer(new ConfigureSkillPacket(
                    SkillPageClientState.globalSlot(localSlot), id == null ? "" : id.toString()));
        }

        /** 手持寂灭左键空挥：发送开火包到服务端。 */
        @SubscribeEvent
        public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
            if (event.getEntity().getMainHandItem().getItem() instanceof SilenceGunItem) {
                PacketDistributor.sendToServer(new SilenceGunFirePacket());
            }
        }
    }
}
