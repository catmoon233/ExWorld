package net.exmo.exphone;

import com.google.gson.*;
import com.sighs.apricityui.init.Element;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.concurrent.*;
import static net.exmo.exphone.PhoneApricity.*;

/** Local tools and game-backed utilities share the controller's lifecycle and drafts. */
final class PhoneUtilities {
    private static final PhoneCalculator calculator=new PhoneCalculator();
    private static final Set<String> albumSelection=new LinkedHashSet<>();
    private static final Set<String> favoritePhotos=new LinkedHashSet<>();
    private static final ScheduledExecutorService HOLD=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"phone-album-hold");t.setDaemon(true);return t;});
    private static boolean albumSelecting;
    private static String albumFilter="全部";
    private static boolean viewerUi=true;
    static void draw(Element page){
        switch(ui.route){
            case "pay"->wallet(page);case "bill"->bill(page);case "wifi"->wifi(page);case "settings"->settings(page);
            case "camera"->camera(page);case "album"->album(page);case "photo"->photo(page);case "map"->map(page);
            case "calc"->calculator(page);case "notes"->notes(page);case "note"->note(page);case "timer"->timer(page);
            case "calendar"->calendar(page);case "recorder"->recorder(page);
            case "convert"->converter(page);case "editor"->editor(page);default->empty(page,"应用暂不可用");
        }
    }
    private static void wallet(Element page){
        node(page,"div","muted","可用金币");id(node(page,"div","balance","0"),"wallet-balance");refreshers.add(()->set("wallet-balance",String.valueOf(num(state,"gold"))));
        node(page,"div","muted","金币可由指令、ATM 或其他模组接入。");
        Element to=input(page,"pay-to","玩家名、微信名或手机号",false),amount=input(page,"pay-amount","转账金币数量",false);
        button(page,"转账","primary",()->{
            Element form=sheet("确认转账");node(form,"div","title",value(amount)+" 金币");node(form,"div","muted","收款人："+value(to));
            button(form,"确认","primary",()->act("pay",data("to",value(to),"amount",number(value(amount))),PhoneApricity::hideSheet));
        });cell(page,"手机充值","话费与流量",()->go("bill"));
    }
    private static void bill(Element page){
        id(node(page,"div","card",""),"bill-info");refreshers.add(()->set("bill-info","号码："+str(state,"number")+"\n话费 "+num(state,"credit")+" · 流量 "+num(state,"data")));
        Element amount=input(page,"bill-amount","数量",false);
        button(page,"金币充值话费","primary",()->act("topup",data("amount",number(value(amount))),null));
        button(page,"话费兑换流量","card",()->act("data",data("amount",number(value(amount))),null));
    }
    private static void wifi(Element page){
        id(node(page,"div","card",""),"wifi-status");refreshers.add(()->set("wifi-status",str(state,"wifi").isBlank()?"未连接 WiFi":"已连接 "+str(state,"wifi")));
        Element list=list(page);refreshers.add(()->reconcile(list,array("wifis").toString(),()->{
            for(JsonObject wifi:objects("wifis"))cell(list,str(wifi,"name"),num(wifi,"dist")+" 格",()->{
                Element form=sheet("连接 "+str(wifi,"name")),pass=input(form,"wifi-pass","网络密码",false);attr(pass,"type","password");
                button(form,"连接","primary",()->act("wifi",data("name",str(wifi,"name"),"text",value(pass)),PhoneApricity::hideSheet));
            });if(list.childNodes.isEmpty())empty(list,"附近没有可用网络");
        }));button(page,"刷新附近网络","card",PhoneApricity::requestState);button(page,"断开 WiFi","danger",()->act("unwifi",new JsonObject(),null));
    }
    private static void settings(Element page){
        Element list=list(page);node(list,"div","title","外观与声音");Element row=node(list,"div","row","");
        button(row,"浅色","",()->act("setting",data("name","theme","text","light"),null));button(row,"深色","",()->act("setting",data("name","theme","text","dark"),null));
        Element sizes=node(list,"div","row","");for(String size:List.of("13","14","16"))button(sizes,size+" px","",()->act("setting",data("name","font","text",size),null));
        button(list,muted?"提示音：关闭":"提示音：开启","card",()->{muted=!muted;remember();render("");});
        button(list,reduced?"减少动态效果：开启":"减少动态效果：关闭","card",()->{reduced=!reduced;remember();render("");});
        node(list,"div","title","锁屏与密码");
        Element oldPin=PhoneLock.hasPin()?pin(list,"当前密码"):null;
        Element newPin=pin(list,"新密码（4–8 位数字）"),confirmPin=pin(list,"再次输入新密码");
        button(list,PhoneLock.hasPin()?"修改锁屏密码":"设置锁屏密码","primary",()->{
            String result=PhoneLock.change(value(oldPin),value(newPin),value(confirmPin));
            notice(result.isEmpty()?"锁屏密码已保存":result);
            if(result.isEmpty())render("");
        });
        if(PhoneLock.hasPin())button(list,"移除锁屏密码","danger",()->{
            String result=PhoneLock.remove(value(oldPin));notice(result.isEmpty()?"已移除锁屏密码":result);
            if(result.isEmpty())render("");
        });
        Element rename=input(list,"rename","新的微信名",false);button(list,"保存微信名","primary",()->act("rename",data("text",value(rename)),null));
        cell(list,"无线网络",str(state,"wifi"),()->go("wifi"));cell(list,"SIM 与充值",str(state,"number"),()->go("bill"));button(list,"锁定手机","card",()->go("lock"));
        cell(list,"微信手机号绑定",bool(state,"wechatBound")?"已绑定 "+str(state,"number"):"需要绑定当前手机号",()->go("wechat-bind"));
    }
    private static Element pin(Element parent,String placeholder){
        Element field=node(parent,"input","","");attr(field,"placeholder",placeholder);attr(field,"type","password");return field;
    }
    private static void camera(Element page){PhoneCameraUi.draw(page);}
    static void shutter(){
        if(!ui.route.equals("camera"))return;
        // Capture locally on the shutter edge. The server action is only the permission/audit event.
        PhoneCamera.shoot();PhoneCameraUi.flash();sound();set("camera-countdown","");
        act("camera",new JsonObject(),null);
    }
    private static void album(Element page){
        attr(page,"class","page album-page");
        Element top=node(page,"div","album-topbar","");
        if(albumSelecting)attr(top,"class","album-topbar selecting");
        if(albumSelecting){
            button(top,"取消","album-top-button",()->{albumSelecting=false;albumSelection.clear();render("");});
            node(top,"div","album-top-title","已选 "+albumSelection.size()+" 项");
            button(top,"全选","album-top-button",()->{albumSelection.clear();albumSelection.addAll(albumIds());render("");});
        }else{
            button(top,"←","album-top-button",PhoneApricity::back);
            node(top,"div","album-top-title","相册");
            Element filters=node(top,"div","album-filters","");
            for(String candidate:List.of("全部","照片","视频")){
                String selected=candidate;
                button(filters,candidate,candidate.equals(albumFilter)?"selected":"",()->{albumFilter=selected;render("");});
            }
            button(top,"选择","album-top-button",()->{albumSelecting=true;render("");});
        }

        Element list=list(page);attr(list,"class","album-scroll");
        refreshers.add(()->reconcile(list,albumIds().toString()+albumSelecting+albumSelection.toString()+favoritePhotos.toString(),()->{
            List<String> ids=albumIds();
            if(ids.isEmpty()){empty(list,albumFilter.equals("视频")?"暂无视频":"还没有照片\n用相机记录你的世界");return;}
            Map<LocalDate,List<String>> groups=new LinkedHashMap<>();
            ZoneId zone=ZoneId.systemDefault();
            for(String id:ids){
                long modified=PhonePhotoClient.modifiedTime(id);
                LocalDate date=modified==0?LocalDate.now(zone):Instant.ofEpochMilli(modified).atZone(zone).toLocalDate();
                groups.computeIfAbsent(date,k->new ArrayList<>()).add(id);
            }
            LocalDate today=LocalDate.now(zone);
            for(Map.Entry<LocalDate,List<String>> group:groups.entrySet()){
                LocalDate date=group.getKey();
                String label=date.equals(today)?"今天":date.equals(today.minusDays(1))?"昨天":date.toString();
                node(list,"div","album-group-title",label);
                Element grid=node(list,"div","album-grid","");
                for(String id:group.getValue())albumCard(grid,id);
            }
        }));

        if(albumSelecting){
            Element actions=node(page,"div","album-selection-actions","");
            button(actions,"☆  收藏","album-action",()->{favoritePhotos.addAll(albumSelection);render("");});
            button(actions,"⇧  导出","album-action",()->notice("已准备导出 "+albumSelection.size()+" 项"));
            button(actions,"删除","album-action delete",()->deleteSelected());
        }else{
            Element bottom=node(page,"div","album-bottom","");
            node(bottom,"span","album-storage",storageLabel());
            button(bottom,"返回相机","album-camera-button",()->go("camera"));
        }
    }

    private static List<String> albumIds(){
        List<String> ids=new ArrayList<>();
        for(String id:PhonePhotoClient.album())if(!albumFilter.equals("视频"))ids.add(id);
        return ids;
    }

    private static void albumCard(Element grid,String id){
        Element card=node(grid,"div","album-card"+(albumSelection.contains(id)?" selected":""),"");
        image(card,id,false,"album-thumbnail");
        if(favoritePhotos.contains(id))node(card,"span","album-star","★");
        if(albumSelecting)node(card,"span","album-check",albumSelection.contains(id)?"✓":"");
        final boolean[] longPressed={false};
        final ScheduledFuture<?>[] hold={null};
        Runnable cancelHold=()->{if(hold[0]!=null){hold[0].cancel(false);hold[0]=null;}};
        card.addEventListener("mousedown",event->{
            cancelHold.run();
            hold[0]=HOLD.schedule(()->net.minecraft.client.Minecraft.getInstance().execute(()->{
                if(!card.isConnected()||albumSelecting)return;
                longPressed[0]=true;albumSelecting=true;albumSelection.add(id);render("");
            }),520,TimeUnit.MILLISECONDS);
        });
        card.addEventListener("mouseup",event->cancelHold.run());
        card.addEventListener("mouseleave",event->cancelHold.run());
        click(card,()->{
            cancelHold.run();
            if(longPressed[0]){longPressed[0]=false;return;}
            if(albumSelecting){if(!albumSelection.add(id))albumSelection.remove(id);render("");}
            else{selectedPhoto=id;go("photo");}
        });
    }

    private static String storageLabel(){
        double gb=PhonePhotoClient.storageBytes()/1024.0/1024.0/1024.0;
        return String.format(Locale.ROOT,"%.1fGB / 5GB",gb);
    }

    private static void photo(Element page){
        attr(page,"class","page photo-view-page");
        viewerUi=true;
        Element chrome=node(page,"div","photo-view-chrome","");
        Element header=node(chrome,"div","photo-view-header","");
        button(header,"←","photo-view-button",PhoneApricity::back);
        node(header,"span","photo-view-title","照片预览");
        button(header,favoritePhotos.contains(selectedPhoto)?"★":"☆","photo-view-button favorite",()->{if(!favoritePhotos.remove(selectedPhoto))favoritePhotos.add(selectedPhoto);render("");});
        Element stage=node(page,"div","photo-stage","");
        Element image=image(stage,selectedPhoto,true,"photo-full");
        id(image,"photo-full");
        click(stage,()->{viewerUi=!viewerUi;attr(chrome,"class",viewerUi?"photo-view-chrome":"photo-view-chrome hidden");});
        fitOpenPhoto();
        Element footer=node(chrome,"div","photo-view-footer","");
        button(footer,"分享","photo-action",PhoneUtilities::sharePhoto);
        button(footer,"删除","photo-action delete",()->deletePhoto(selectedPhoto));
    }
    /** Texture draw ignores object-fit, so the element box itself must already be contained. */
    static final int PHOTO_STAGE_W = 322;
    static final int PHOTO_STAGE_H = 550;

    static int[] containedSize(int imageW, int imageH, int boxW, int boxH) {
        if (imageW <= 0 || imageH <= 0 || boxW <= 0 || boxH <= 0) return new int[]{0, 0};
        double scale = Math.min(boxW / (double) imageW, boxH / (double) imageH);
        int width = Math.max(1, (int) Math.floor(imageW * scale));
        int height = Math.max(1, (int) Math.floor(imageH * scale));
        return new int[]{Math.min(boxW, width), Math.min(boxH, height)};
    }

    static void fitOpenPhoto() {
        if (!ui.route.equals("photo")) return;
        Element image = el("photo-full");
        if (image == null) return;
        int[] pixels = PhonePhotoClient.pixelSize(selectedPhoto, true);
        if (pixels == null) return;
        int[] fitted = containedSize(pixels[0], pixels[1], PHOTO_STAGE_W, PHOTO_STAGE_H);
        if (fitted[0] <= 0 || fitted[1] <= 0) return;
        attr(image, "style", "position:absolute;left:50%;top:50%;width:" + fitted[0] + "px;height:" + fitted[1]
                + "px;margin-left:" + (-fitted[0] / 2) + "px;margin-top:" + (-fitted[1] / 2) + "px;opacity:1;");
    }

    static void photoSwipe(double dx,double dy){
        if(dy>70){back();return;}
        if(Math.abs(dx)<65)return;
        List<String> ids=PhonePhotoClient.album();int index=ids.indexOf(selectedPhoto);if(index<0||ids.isEmpty())return;
        int next=dx<0?index+1:index-1;if(next<0||next>=ids.size())return;
        selectedPhoto=ids.get(next);render("");
    }

    private static void deletePhoto(String id){
        if(!PhonePhotoClient.album().contains(id))return;
        Element form=sheet("删除照片");node(form,"div","muted","删除后本地原图和缩略图都会移除，已分享的消息仍会保留。");
        button(form,"确认删除","danger",()->{PhonePhotoClient.delete(id);favoritePhotos.remove(id);hideSheet();go("album");});
    }

    private static void deleteSelected(){
        if(albumSelection.isEmpty()){notice("请先选择照片");return;}
        Element form=sheet("删除所选照片");node(form,"div","muted","确认删除 "+albumSelection.size()+" 项？已分享的消息仍会保留。");
        button(form,"确认删除","danger",()->{for(String id:List.copyOf(albumSelection))PhonePhotoClient.delete(id);albumSelection.clear();albumSelecting=false;hideSheet();render("");});
    }

    private static void sharePhoto(){
        if(!PhonePhotoClient.album().contains(selectedPhoto))return;
        Element form=sheet("分享照片");
        button(form,"朋友圈","card",()->{momentPhoto=selectedPhoto;go("moments");PhonePages.composeMoment();});
        for(JsonObject friend:objects("book"))cell(form,str(friend,"name"),"发送照片",()->{String id=selectedPhoto;PhonePhotoClient.upload(id,()->act("image",data("to",str(friend,"id"),"photo",id),PhoneApricity::hideSheet));});
        for(JsonObject group:objects("groups"))cell(form,str(group,"name"),"群聊",()->{String id=selectedPhoto;PhonePhotoClient.upload(id,()->act("image",data("to","g:"+str(group,"id"),"photo",id),PhoneApricity::hideSheet));});
    }

    static void pickPhoto(Consumer<String> selected){
        Element form=sheet("选择照片");
        Element grid=node(form,"div","picker-grid","");
        for(String id:PhonePhotoClient.album()){Element card=node(grid,"div","picker-card","");image(card,id,false,"photo thumbnail");click(card,()->{hideSheet();selected.accept(id);});}
        if(PhonePhotoClient.album().isEmpty())empty(form,"还没有照片，先打开相机拍一张吧");
    }
    private static void map(Element page){
        node(page,"div","muted","当前位置 "+num(state,"px")+", "+num(state,"pz"));Element list=list(page);
        for(JsonObject place:objects("map")){
            if(!str(place,"dim").isEmpty()&&!str(place,"dim").equals(str(state,"dim")))continue;
            cell(list,str(place,"name"),str(place,"tag")+" · "+str(place,"info"),()->{
                Element form=sheet(str(place,"name"));node(form,"div","card",str(place,"info"));node(form,"div","muted","位置："+num(place,"x1")+", "+num(place,"z1"));
            });
        }if(list.childNodes.isEmpty())empty(list,"地图上还没有标记地点");
    }
    private static void calculator(Element page){
        id(node(page,"div","readout",calculator.display),"calc-value");Element keys=node(page,"div","keys","");
        for(String key:List.of("C","⌫","÷","×","7","8","9","−","4","5","6","+","1","2","3","=","0","."))button(keys,key,"key",()->{calculator.press(key);set("calc-value",calculator.display);});
    }
    private static void notes(Element page){
        button(page,"新建备忘录","primary",()->{noteId="";ui.drafts.remove("note/note-title");ui.drafts.remove("note/note-text");go("note");});Element list=list(page);
        refreshers.add(()->reconcile(list,array("notes").toString(),()->{
            for(JsonObject note:objects("notes"))cell(list,str(note,"title"),str(note,"text"),()->{noteId=str(note,"id");ui.drafts.put("note/note-title",str(note,"title"));ui.drafts.put("note/note-text",str(note,"text"));go("note");});
            if(list.childNodes.isEmpty())empty(list,"把重要的事情记在这里");
        }));
    }
    private static void note(Element page){
        Element title=input(page,"note-title","标题",false),text=input(page,"note-text","正文",true);attr(text,"style","height:280px;");
        button(page,"保存","primary",()->act("note",data("id",noteId,"name",value(title),"text",value(text)),()->go("notes")));
        if(!noteId.isBlank())button(page,"删除","danger",()->act("note_delete",data("id",noteId),()->go("notes")));
    }
    private static void timer(Element page){
        Element tabs=node(page,"div","row","");button(tabs,"计时器",alarmsView?"":"primary",()->{alarmsView=false;render("");});
        button(tabs,"闹钟",alarmsView?"primary":"",()->{alarmsView=true;render("");});
        if(alarmsView){
            Element row=node(page,"div","row","");Element hour=input(row,"alarm-hour","时 0–23",false),minute=input(row,"alarm-minute","分 0–59",false);
            Element label=input(page,"alarm-label","闹钟名称（可选）",false);
            button(page,"添加闹钟","primary",()->{
                String result=PhoneOrganizer.addAlarm(number(value(hour)),number(value(minute)),value(label));notice(result);
                if(result.equals("闹钟已设置"))render("");
            });
            Element list=list(page);
            for(JsonObject alarm:PhoneOrganizer.alarms()){
                Element card=node(list,"div","card","");
                node(card,"div","cell-title",String.format(Locale.ROOT,"%02d:%02d",num(alarm,"hour"),num(alarm,"minute")));
                node(card,"div","muted",str(alarm,"label")+" · 每个游戏日响铃");
                button(card,"删除","small danger",()->{PhoneOrganizer.removeAlarm(str(alarm,"id"));render("");});
            }
            if(list.childNodes.isEmpty())empty(list,"还没有闹钟");
            return;
        }
        id(node(page,"div","hero-time",duration(timerEnd>0?(timerEnd-System.currentTimeMillis())/1000:timerPaused/1000)),"timer-value");
        Element minutes=input(page,"timer-minutes","分钟",false),seconds=input(page,"timer-seconds","秒",false);
        button(page,"开始 / 继续","primary",()->{long ms=timerPaused>0?timerPaused:Math.max(0,Math.min(86400,number(value(minutes))*60L+number(value(seconds))))*1000;timerEnd=System.currentTimeMillis()+ms;timerPaused=0;});
        button(page,"暂停","card",()->{timerPaused=Math.max(0,timerEnd-System.currentTimeMillis());timerEnd=0;});
        button(page,"重置","card",()->{timerEnd=0;timerPaused=0;set("timer-value","00:00");});
    }
    private static void calendar(Element page){
        var level=net.minecraft.client.Minecraft.getInstance().level;
        long today=level==null?0:Math.max(0,Math.floorDiv(level.getDayTime(),24_000));
        long month=Math.floorDiv(today,30)+calendarOffset;
        long first=month*30;
        if(selectedDay<0)selectedDay=today;
        Element nav=node(page,"div","row","");button(nav,"‹","",()->{calendarOffset--;render("");});
        node(nav,"div","cell-title",(month/12+1)+" 年 " +(Math.floorMod(month,12)+1)+" 月");
        button(nav,"›","",()->{calendarOffset++;render("");});
        button(page,"回到今天","small",()->{calendarOffset=0;selectedDay=today;render("");});
        Element list=list(page);node(list,"div","muted","一　 二　 三　 四　 五　 六　 日");
        int start=(int)Math.floorMod(first,7);
        for(int week=0;week<6;week++){
            Element row=node(list,"div","calendar-week","");
            for(int weekday=0;weekday<7;weekday++){
                int date=week*7+weekday-start+1;
                if(date<1||date>30){node(row,"div","calendar-day","");continue;}
                long day=first+date-1;
                button(row,String.valueOf(date),"calendar-day"+(day==selectedDay?" selected-day":""),()->{selectedDay=day;render("");});
            }
        }
        node(list,"div","cell-title",PhoneClock.date(selectedDay));
        Element title=input(list,"calendar-title","添加日程",false);
        button(list,"保存日程","primary",()->{String result=PhoneOrganizer.addEvent(selectedDay,value(title));notice(result);if(result.equals("日程已保存")){clear(title);render("");}});
        for(JsonObject event:PhoneOrganizer.events(selectedDay)){
            Element row=node(list,"div","row","");node(row,"div","card",str(event,"title"));
            button(row,"删除","small danger",()->{PhoneOrganizer.removeEvent(str(event,"id"));render("");});
        }
    }
    private static void recorder(Element page){
        node(page,"div","title","录音机");
        node(page,"div","muted",PhoneCalls.installed()?"使用 Simple Voice Chat 已启用的麦克风；录音时仍遵循语音模组的说话方式。":"需要安装并连接 Simple Voice Chat 才能录音。");
        button(page,PhoneRecorder.recording()?"停止录音":"开始录音","primary",()->{
            String result=PhoneRecorder.recording()?PhoneRecorder.stop():PhoneRecorder.start();notice(result);render("");
        });
        Element list=list(page);
        Runnable draw=()->reconcile(list,PhoneRecorder.clips().toString(),()->{
            for(String clip:PhoneRecorder.clips()){
                Element row=node(list,"div","row","");button(row,"▶  "+clip.substring(0,8),"card",()->PhoneRecorder.play(clip));
                button(row,"删除","small danger",()->{PhoneRecorder.delete(clip);render("");});
            }
            if(list.childNodes.isEmpty())empty(list,"还没有录音");
        });refreshers.add(draw);
    }
    private static void converter(Element page){
        Element value=input(page,"convert-value","输入数值",false);id(node(page,"div","balance","0"),"convert-result");
        for(String kind:List.of("秒 → 游戏刻","游戏刻 → 秒","话费 → 流量","方块 → 米"))button(page,kind,"card",()->{
            try{double n=Double.parseDouble(value(value));double result=kind.startsWith("秒")?n*20:kind.startsWith("游戏刻")?n/20:kind.startsWith("话费")?n*10:n;set("convert-result",Double.toString(result));}
            catch(NumberFormatException error){notice("请输入有效数字");}
        });
    }
    private static void editor(Element page){
        if(!bool(state,"editor")){empty(page,"需要创造模式或管理员权限");return;}Element list=list(page);
        for(JsonObject npc:objects("npcs"))cell(list,str(npc,"name"),str(npc,"wechat"),()->{
            Element form=sheet("编辑名片"),number=input(form,"npc-number","手机号",false),name=input(form,"npc-name","微信名",false),moment=input(form,"npc-moment","代发朋友圈（可选）",true);
            setValue(number,str(npc,"number"));setValue(name,str(npc,"wechat"));
            button(form,"保存","primary",()->act("npc",data("id",str(npc,"id"),"number",value(number),"wechat",value(name),"text",value(moment)),PhoneApricity::hideSheet));
        });
    }
}
