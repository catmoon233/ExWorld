package net.exmo.exworld.client.party;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.client.battle.BattleClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

public final class PartyHud {
    private PartyHud() {}
    public static void registerLayer(RegisterGuiLayersEvent event){event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(Exworld.MODID,"party_hud"),(graphics,delta)->render(graphics));}
    private static void render(GuiGraphics g){Minecraft mc=Minecraft.getInstance();var snapshot=PartyClient.snapshot();if(BattleClient.active()||mc.options.hideGui||mc.player==null||snapshot.members().size()<=1)return;
        var others=snapshot.members().stream().filter(member->!member.id().equals(mc.player.getUUID())).toList();if(others.isEmpty())return;int x=12,y=42,w=154,h=20+others.size()*38;
        panel(g,x,y,w,h);g.drawString(mc.font,Component.translatable("hud.exworld.party"),x+10,y+8,0xFFF2CB72,true);int row=y+23;
        for(var member:others){int color=member.online()?0xFFF4F6F8:0xFF68727D;String mark=member.leader()?"◆ ":"";g.drawString(mc.font,mark+member.name(),x+9,row,color,false);String range=!member.online()?Component.translatable("hud.exworld.party_offline").getString():member.distance()<0?Component.translatable("hud.exworld.party_far").getString():Math.round(member.distance())+"m";g.drawString(mc.font,range,x+w-mc.font.width(range)-8,row,member.online()?0xFF9AA7B5:0xFF68727D,false);drawBar(g,x+9,row+12,w-18,6,member.health(),member.maxHealth(),0xFFE95E67);drawBar(g,x+9,row+21,w-18,5,member.mana(),member.maxMana(),0xFF5796F2);row+=38;}}
    private static void drawBar(GuiGraphics g,int x,int y,int w,int h,float value,float max,int color){g.fill(x,y,x+w,y+h,0xCC202832);int fill=max<=0?0:Math.round((w-2)*Math.max(0,Math.min(1,value/max)));g.fill(x+1,y+1,x+1+fill,y+h-1,color);}
    private static void panel(GuiGraphics g,int x,int y,int w,int h){g.fill(x+4,y,x+w-4,y+h,0xD9121821);g.fill(x,y+4,x+w,y+h-4,0xD9121821);g.fill(x+4,y,x+w-4,y+1,0xFFF2CB72);}
}
