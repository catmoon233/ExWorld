package net.exmo.exworld.client.monster;

import net.exmo.exworld.network.MonsterPackageActionPayload;
import net.exmo.exworld.network.MonsterPackageListPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/** Administrator package manager: ordering is shown exactly as the server resolves it. */
public final class MonsterPackageScreen extends Screen {
    private static MonsterPackageListPayload latest;
    private String selected="";
    private EditBox fileName;
    public MonsterPackageScreen(){super(Component.literal("怪物数据包管理"));}
    public static void install(MonsterPackageListPayload payload){latest=payload;Minecraft.getInstance().setScreen(new MonsterPackageScreen());}
    public static void open(){PacketDistributor.sendToServer(new MonsterPackageActionPayload(MonsterPackageActionPayload.Action.REQUEST_LIST,"",0,"","",""));}
    @Override protected void init(){clearWidgets();if(latest==null)return;int y=48;for(var entry:latest.packages()){String id=entry.id();addRenderableWidget(Button.builder(Component.literal((latest.enabled().contains(id)?"[已启用] ":"[已停用] ")+entry.displayName()),b->{selected=id;}).bounds(24,y,210,20).build());addRenderableWidget(Button.builder(Component.literal("编辑"),b->send(MonsterPackageActionPayload.Action.OPEN_EDITOR,id)).bounds(240,y,50,20).build());addRenderableWidget(Button.builder(Component.literal(latest.enabled().contains(id)?"停用":"启用"),b->send(latest.enabled().contains(id)?MonsterPackageActionPayload.Action.DISABLE:MonsterPackageActionPayload.Action.ENABLE,id)).bounds(296,y,70,20).build());y+=24;}
        fileName=new EditBox(font,24,height-58,180,20,Component.literal("导入文件名"));fileName.setHint(Component.literal("输入 import 目录中的 ZIP 文件名"));addRenderableWidget(fileName);addRenderableWidget(Button.builder(Component.literal("导入"),b->send(MonsterPackageActionPayload.Action.IMPORT,"",0,fileName.getValue())).bounds(210,height-58,60,20).build());addRenderableWidget(Button.builder(Component.literal("导出"),b->{if(!selected.isBlank())send(MonsterPackageActionPayload.Action.EXPORT,selected);}).bounds(276,height-58,60,20).build());addRenderableWidget(Button.builder(Component.literal("热重载"),b->send(MonsterPackageActionPayload.Action.RELOAD,"" )).bounds(24,height-32,70,20).build());addRenderableWidget(Button.builder(Component.literal("上移"),b->{if(!selected.isBlank())send(MonsterPackageActionPayload.Action.MOVE,selected,Math.max(0,latest.enabled().indexOf(selected)-1));}).bounds(100,height-32,50,20).build());addRenderableWidget(Button.builder(Component.literal("下移"),b->{if(!selected.isBlank())send(MonsterPackageActionPayload.Action.MOVE,selected,latest.enabled().indexOf(selected)+1);}).bounds(156,height-32,50,20).build());addRenderableWidget(Button.builder(Component.literal("关闭"),b->onClose()).bounds(width-84,height-32,60,20).build());}
    private void send(MonsterPackageActionPayload.Action action,String id){send(action,id,0);}
    private void send(MonsterPackageActionPayload.Action action,String id,int index){PacketDistributor.sendToServer(new MonsterPackageActionPayload(action,id,index,"","",""));}
    private void send(MonsterPackageActionPayload.Action action,String id,int index,String file){PacketDistributor.sendToServer(new MonsterPackageActionPayload(action,id,index,file,"",""));}
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){renderBackground(graphics,mouseX,mouseY,partialTick);graphics.drawString(font,title,24,20,0xFFFFFFFF);if(latest!=null){graphics.drawString(font,"列表从上到下启用，越靠下优先级越高",24,34,0xFFB8C3D1);int y=48;for(var e:latest.packages()){graphics.drawString(font,e.id()+"  v"+e.version()+(e.valid()?"":"  错误："+e.error()),372,y+6, e.valid()?0xFFE6EAF0:0xFFFF7070);y+=24;}}super.render(graphics,mouseX,mouseY,partialTick);}
}
