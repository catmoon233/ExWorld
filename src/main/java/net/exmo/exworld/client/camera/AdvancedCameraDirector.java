/*
 * Adapted from StarRailExpress2, GPL-3.0-or-later. See THIRD_PARTY_NOTICES.md.
 */
package net.exmo.exworld.client.camera;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.battle.api.BattleSnapshot;
import net.exmo.exworld.battle.model.BattleState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

/** Keyframe director that owns only the cinematic camera override; battle remains the lower-priority owner. */
public final class AdvancedCameraDirector {
    public static final ResourceLocation OWNER = ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "battle_cinematic");
    private static AdvancedCameraSequence sequence;
    private static int tick;
    private static Pose previous, current;
    private static java.util.UUID battleId;
    private static java.util.List<String> factions = java.util.List.of();
    private static final int BATTLE_START_DELAY = 8;
    private static final int BATTLE_START_IN = 6;
    private static final int BATTLE_START_HOLD = 18;
    private static final int BATTLE_START_OUT = 10;
    private static final int BATTLE_START_TOTAL = BATTLE_START_IN + BATTLE_START_HOLD + BATTLE_START_OUT;
    private static final float BATTLE_START_SCALE = 2.4F;
    private static final int BATTLE_START_COLOR = 0xFF2E2E;
    private static boolean battleStartAnnounced;
    private AdvancedCameraDirector() {}

    public static void syncBattleIntro(BattleSnapshot snapshot) {
        if (snapshot.state() != BattleState.INTRO || snapshot.intro() == null) { if (sequence != null && snapshot.state() != BattleState.INTRO) clear(); return; }
        if (snapshot.battleId().value().equals(battleId)) { tick = snapshot.intro().elapsedTicks(); return; }
        battleId = snapshot.battleId().value(); factions = java.util.List.copyOf(snapshot.factionOrder()); sequence = buildIntro(snapshot); tick = snapshot.intro().elapsedTicks(); previous = current = null;
        battleStartAnnounced = false;
    }
    private static AdvancedCameraSequence buildIntro(BattleSnapshot snapshot) {
        var nodes = new ArrayList<AdvancedCameraNode>(); double centerX=snapshot.arenaOriginX()+snapshot.arenaSize()/2.0, centerZ=snapshot.arenaOriginZ()+snapshot.arenaSize()/2.0;
        nodes.add(new AdvancedCameraNode(8,0,new Vec3(centerX+10,76,centerZ+10),new Vec3(centerX,65,centerZ),55));
        for (String faction : snapshot.factionOrder()) {
            var members=snapshot.combatants().values().stream().filter(c->c.factionId().equals(faction)).toList(); if(members.isEmpty())continue;
            double x=members.stream().mapToDouble(c->snapshot.arenaOriginX()+c.cell().x()+.5).average().orElse(centerX);
            double z=members.stream().mapToDouble(c->snapshot.arenaOriginZ()+c.cell().z()+.5).average().orElse(centerZ);
            nodes.add(new AdvancedCameraNode(20,15,new Vec3(x+7,70,z+7),new Vec3(x,66,z),55));
        }
        nodes.add(new AdvancedCameraNode(25,0,new Vec3(centerX+18,82,centerZ+18),new Vec3(centerX,65,centerZ),70));
        return new AdvancedCameraSequence(nodes,true);
    }
    public static void tick() {
        if(sequence!=null && tick<sequence.totalTicks())tick++;
        if(!battleStartAnnounced && tick>=BATTLE_START_DELAY && active()){battleStartAnnounced=true;playBattleStartSound();}
    }
    public static boolean active(){return sequence!=null && tick<sequence.totalTicks();}
    public static Vec3 position(float partial){return pose(partial).position;}
    public static float yaw(float partial){return pose(partial).yaw;}
    public static float pitch(float partial){return pose(partial).pitch;}
    public static float fov(float partial){return pose(partial).fov;}
    private static Pose pose(float partial){
        if(sequence==null||sequence.nodes().isEmpty())return new Pose(Vec3.ZERO,0,0,70); int cursor=0; AdvancedCameraNode prior=sequence.nodes().getFirst();
        for(AdvancedCameraNode node:sequence.nodes()){int end=cursor+node.totalTicks();if(tick<=end){double p=node.durationTicks()==0?1:Mth.clamp((tick+partial-cursor)/(double)node.durationTicks(),0,1);Vec3 start=prior.position(),pos=start.lerp(node.position(),p);Pose out=lookAt(pos,node.lookAt(),node.fov());previous=current;current=out;return out;}cursor=end;prior=node;}
        AdvancedCameraNode last=sequence.nodes().getLast();return lookAt(last.position(),last.lookAt(),last.fov());
    }
    private static Pose lookAt(Vec3 pos,Vec3 target,float fov){Vec3 d=target.subtract(pos);float yaw=(float)(Mth.atan2(d.z,d.x)*180/Math.PI)-90;float pitch=(float)(-Mth.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))*180/Math.PI);return new Pose(pos,yaw,pitch,fov);}
    public static void renderOverlay(GuiGraphics graphics){
        if(!active()||sequence==null||!sequence.blackBars())return;
        int w=graphics.guiWidth(), h=graphics.guiHeight();
        int barH=Math.max(18,h/12);
        graphics.fill(0,0,w,barH,0xFF000000);
        graphics.fill(0,h-barH,w,h,0xFF000000);
        if(tick<BATTLE_START_DELAY){int alpha=(int)(255*(1-tick/(float)BATTLE_START_DELAY));graphics.fill(0,0,w,h,alpha<<24);}
        renderBattleStart(graphics,w,h);
        if(tick<BATTLE_START_DELAY)return;
        int factionIndex=(tick-BATTLE_START_DELAY)/35;
        if(factionIndex>=0&&factionIndex<factions.size()){
            String faction=factions.get(factionIndex);var font=Minecraft.getInstance().font;
            var title=Component.translatable("hud.exworld.faction_intro",faction);
            graphics.drawCenteredString(font,title,w/2,h-barH-26,0xFFFFE1A0);
        }
    }
    private static void renderBattleStart(GuiGraphics graphics,int width,int height){
        if(tick<BATTLE_START_DELAY||tick>=BATTLE_START_DELAY+BATTLE_START_TOTAL)return;
        int local=tick-BATTLE_START_DELAY;
        float alpha,scale;
        if(local<BATTLE_START_IN){float p=local/(float)BATTLE_START_IN;alpha=p;scale=BATTLE_START_SCALE+(1.0F-p)*1.4F;}
        else if(local<BATTLE_START_IN+BATTLE_START_HOLD){alpha=1.0F;scale=BATTLE_START_SCALE;}
        else{float p=(local-BATTLE_START_IN-BATTLE_START_HOLD)/(float)BATTLE_START_OUT;alpha=1.0F-p;scale=BATTLE_START_SCALE+p*0.4F;}
        if(alpha*255.0F<4.0F)return;
        var font=Minecraft.getInstance().font;
        var title=Component.translatable("hud.exworld.battle_start");
        int color=(Mth.clamp((int)(alpha*255.0F),4,255)<<24)|BATTLE_START_COLOR;
        var pose=graphics.pose();
        pose.pushPose();
        pose.translate(width/2.0F,height*0.40F,0.0F);
        pose.scale(scale,scale,1.0F);
        graphics.drawCenteredString(font,title,0,-font.lineHeight/2,color);
        pose.popPose();
    }
    private static void playBattleStartSound(){
        Minecraft minecraft=Minecraft.getInstance();
        if(minecraft.player!=null)minecraft.player.playSound(SoundEvents.LIGHTNING_BOLT_IMPACT,0.7F,1.0F);
    }
    public static void clear(){sequence=null;tick=0;battleId=null;factions=java.util.List.of();previous=current=null;battleStartAnnounced=false;}
    private record Pose(Vec3 position,float yaw,float pitch,float fov){}
}
