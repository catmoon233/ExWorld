package net.exmo.exphone;

import com.google.gson.*;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import com.sighs.apricityui.event.MouseEvent;
import com.sighs.apricityui.screen.ApricityScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.function.*;

/** Single owner for routes, drafts and commands. HTML never needs a script runtime. */
final class PhoneApricity {
    static final PhoneUiState ui = new PhoneUiState();
    static JsonObject state = new JsonObject();
    static Document document;
    static boolean muted, reduced;
    static long timerEnd, timerPaused, shotAt;
    static long selectedDay = -1;
    static int calendarOffset;
    static boolean alarmsView;
    static int delay;
    static String noteId = "", selectedPhoto = "", momentPhoto = "";
    static final List<Runnable> refreshers = new ArrayList<>();
    static final Set<String> groupMembers = new LinkedHashSet<>();
    static final Map<String, Pending> pending = new LinkedHashMap<>();
    private static final Map<String, Element> fields = new LinkedHashMap<>();
    private static final List<ImageBinding> images = new ArrayList<>();
    private static final Deque<String> notices = new ArrayDeque<>();
    private static long generation = -1, noticeUntil, second, closeAt, serverOffset, nextOrganizerCheck;
    private static String lastClockText = "", lastHeroTimeText = "", lastHeroDateText = "";
    private static boolean listening, dirty, gesture;
    static String lastCallPhase = "";
    static boolean pointerDragged;
    private static double downX, downY, lastX, lastY;
    private record ImageBinding(Element element, String id, boolean full) {}
    static final class Pending {
        final JsonObject body; final Runnable success; final String route;
        long sent = System.currentTimeMillis(); String failure = "";
        Pending(JsonObject body, Runnable success) { this.body=body;this.success=success;this.route=ui.route; }
    }
    static void initialize() {
        if (!listening) {
            NeoForge.EVENT_BUS.addListener(PhoneApricity::tick);
            NeoForge.EVENT_BUS.addListener(PhoneApricity::logout);
            listening=true;
        }
    }
    static void open() {
        initialize();
        closeAt=0;second=-1;resetClockCache();PhoneLock.open();ui.go("lock");PhonePhotoClient.open();Minecraft.getInstance().setScreen(new PhoneScreen());
    }
    static boolean isOpen(){return Minecraft.getInstance().screen instanceof PhoneScreen;}
    static void close(){if(isOpen()){closeAt=System.currentTimeMillis()+180;attr(el("screen"),"style","opacity:0;");}}
    private static void logout(ClientPlayerNetworkEvent.LoggingOut event){
        PhoneCamera.close();PhonePhotoClient.clear();PhoneRecorder.reset();state=new JsonObject();pending.clear();notices.clear();
        ui.drafts.clear();ui.scroll.clear();ui.read.clear();ui.recents.clear();ui.go("lock");PhoneLock.lock();
        PhoneOrganizer.reset();
        timerEnd=0;timerPaused=0;resetClockCache();document=null;lastCallPhase="";
    }
    static void apply(String json){
        JsonObject packet;try{packet=JsonParser.parseString(json).getAsJsonObject();}catch(RuntimeException error){return;}
        if(packet.has("result")){
            JsonObject result=packet.getAsJsonObject("result");Pending request=pending.get(str(result,"requestId"));
            if(request!=null){
                if(bool(result,"ok")){pending.remove(str(result,"requestId"));if(str(request.body,"action").equals("wechat_bind"))state.addProperty("wechatBound",true);if(request.success!=null)request.success.run();}
                else request.failure=str(result,"message");
            }
            notice(str(result,"message"));dirty=true;return;
        }
        String section=str(packet,"section");if(section.isEmpty())return;
        if(section.equals("photoTransfer")){PhonePhotoClient.uploaded(packet.getAsJsonObject("value"));return;}
        if(section.equals("audioTransfer")){PhoneRecorder.uploaded(packet.getAsJsonObject("value"));return;}
        state.add(section,packet.get("value"));dirty=true;
        if(section.equals("serverTime"))serverOffset=packet.get("value").getAsLong()-System.currentTimeMillis();
        if(section.equals("status")&&!str(state,"status").isBlank())notice(str(state,"status"));
        if(section.equals("callState")){
            String phase=str(obj(state,"callState"),"phase");
            boolean changed=!phase.equals(lastCallPhase);
            if(isOpen()&&changed&&(phase.equals("incoming")||phase.equals("active")))PhonePages.callSheet();
            if(isOpen()&&changed&&phase.equals("idle")&&!lastCallPhase.isEmpty()&&!lastCallPhase.equals("idle")){
                if(ui.overlay.equals("电话"))hideSheet();
                dirty=true;
            }
            lastCallPhase=phase;
        }
    }
    static void notice(String message){
        if(message==null||message.isBlank())return;
        notices.addFirst(message);while(notices.size()>30)notices.removeLast();
        set("toast",message);attr(el("toast"),"class","toast on");noticeUntil=System.currentTimeMillis()+3200;
    }
    static void albumChanged(){dirty=true;}
    static void imagesChanged(){
        for(ImageBinding b:List.copyOf(images)){if(!b.element.isConnected())continue;var texture=PhonePhotoClient.texture(b.id,b.full);if(texture!=null)attr(b.element,"src",texture.toString());}
        PhoneUtilities.fitOpenPhoto();
    }
    private static void tick(ClientTickEvent.Post event){
        long now=System.currentTimeMillis();
        var level=Minecraft.getInstance().level;
        if(level!=null&&now>=nextOrganizerCheck){nextOrganizerCheck=now+200;PhoneOrganizer.tick(level.getDayTime());}
        if(timerEnd>0&&now>=timerEnd){timerEnd=0;timerPaused=0;notice("计时结束");sound();}
        if(!isOpen())return;
        if(closeAt>0&&now>=closeAt){closeAt=0;Minecraft.getInstance().setScreen(null);return;}
        if(document==null)return;
        PhoneCameraUi.tick(now);
        if(generation!=document.getRefreshGeneration())bind(document);
        if(dirty){dirty=false;for(Runnable refresh:List.copyOf(refreshers))refresh.run();status();imagesChanged();}
        if(now/1000!=second){
            second=now/1000;clock();
            if(timerEnd>0)set("timer-value",duration((timerEnd-now+999)/1000));
            if(shotAt>0){set("camera-countdown",String.valueOf(Math.max(0,(shotAt-now+999)/1000)));if(now>=shotAt){shotAt=0;PhoneUtilities.shutter();}}
            for(Pending p:pending.values())if(p.failure.isEmpty()&&now-p.sent>15000){p.failure="等待服务器超时，点击重试";dirty=true;}
        }
        if(noticeUntil>0&&now>=noticeUntil){attr(el("toast"),"class","toast");noticeUntil=0;}
    }
    private static final class PhoneScreen extends ApricityScreen{
        PhoneScreen(){super("exphone/phone.html");setPauseGame(false);setShowDefaultBackground(false);}
        @Override protected void init(){super.init();bind(getLinkedDocument());}
        @Override public boolean keyPressed(int key,int scan,int mods){if(key==GLFW.GLFW_KEY_ESCAPE){back();return true;}return super.keyPressed(key,scan,mods);}
        @Override public void render(GuiGraphics graphics,int x,int y,float partial){super.render(graphics,x,y,partial);}
        @Override public void removed(){
            remember();PhoneCamera.close();shotAt=0;PhonePhotoClient.releaseTextures();
            fields.clear();refreshers.clear();images.clear();resetClockCache();document=null;generation=-1;super.removed();
        }
    }
    private static void bind(Document doc){
        if(doc==null)return;document=doc;generation=doc.getRefreshGeneration();
        click(el("back"),PhoneApricity::back);click(el("nav-back"),PhoneApricity::back);click(el("home"),()->go("home"));click(el("more"),PhoneApricity::context);
        click(el("island"),PhoneApricity::controls);click(el("clock"),PhoneApricity::notifications);click(el("recents"),PhoneApricity::recents);
        Element backdrop=el("overlay");backdrop.addEventListener("click",event->{if(event.target==backdrop)hideSheet();});
        el("screen").addEventListener("mousedown",e->{
            if(!(e instanceof MouseEvent m)||m.button!=0){gesture=false;return;}
            pointerDragged=false;downX=lastX=m.clientX;downY=lastY=m.clientY;
            gesture=!ui.route.equals("camera")||!PhoneCameraUi.controlTarget(m.target);
        });
        doc.addEventListener("mousemove",e->{
            if(!gesture||!(e instanceof MouseEvent m))return;
            if(ui.route.equals("camera"))PhoneCamera.drag(m.clientX-lastX,m.clientY-lastY);
            lastX=m.clientX;lastY=m.clientY;
        });
        doc.addEventListener("mouseup",e->{
            if(!(e instanceof MouseEvent m))return;
            double dx=m.clientX-downX,dy=m.clientY-downY;
            pointerDragged=Math.hypot(dx,dy)>8;
            if(!gesture)return;
            gesture=false;
            if(ui.route.equals("camera"))return;
            if(ui.route.equals("photo")){PhoneUtilities.photoSwipe(dx,dy);return;}
            if(dy < -55&&ui.route.equals("lock"))unlock(value(el("lock-pin")));
            else if(dy>70)controls();
            else if(dx>65)back();
        });
        render("launch");status();clock();
    }
    static void go(String route){
        if(!PhoneLock.unlocked()&&!route.equals("lock")&&!route.equals("camera")){notice("请先解锁手机");return;}
        if(wechatRoute(route)&&!bool(state,"wechatBound"))route="wechat-bind";
        if(route.equals("lock"))PhoneLock.lock();
        if(ui.route.equals(route)){hideSheet();return;}remember();PhoneCamera.close();shotAt=0;
        String previous=ui.route;ui.go(route);render(previous.equals("home")||previous.equals("lock")?"launch":"");sound();
    }
    static void back(){
        if(!ui.overlay.isEmpty()){hideSheet();return;}remember();PhoneCamera.close();shotAt=0;
        if(!ui.back()){close();return;}
        if(!PhoneLock.unlocked()&&!ui.route.equals("lock"))ui.go("lock");
        render("back");sound();
    }
    private static boolean wechatRoute(String route){return route.startsWith("chat:")||Set.of("wechat","contacts","moments","me","friend","group").contains(route);}
    static void unlock(String pin){String error=PhoneLock.unlock(pin);Element field=el("lock-pin");if(field!=null)setValue(field,"");if(error.isEmpty())go("home");else notice(error);}
    static void remember(){fields.forEach((id,f)->ui.draft(id,value(f)));Element list=el("main-list");if(list!=null)ui.scroll.put(ui.route,list.getScrollTop());}
    static void render(String motion){
        if(document==null)return;fields.clear();refreshers.clear();images.clear();hideSheet();
        resetClockCache();
        Element content=el("content");if(content==null)return;content.clearChildren();
        boolean wallpaper=ui.route.equals("home")||ui.route.equals("lock");
        boolean camera=ui.route.equals("camera");
        attr(el("shell"),"class",camera?"shell camera-shell":"shell");
        String screenClass=wallpaper?"screen wallpaper":camera?"screen camera-screen":ui.route.equals("album")?"screen album-screen":ui.route.equals("photo")?"screen photo-screen":"screen";
        attr(el("screen"),"class",screenClass);set("heading",wallpaper?"":title(ui.route));
        Element page=node(content,"div","page "+motion,"");PhonePages.draw(page);
        for(Runnable refresh:List.copyOf(refreshers))refresh.run();
        Element list=el("main-list");if(list!=null)list.setScrollTop(ui.scroll.getOrDefault(ui.route,0.0));
        status();clock();imagesChanged();
    }
    static void act(String action,JsonObject body,Runnable success){
        String request=(System.currentTimeMillis()+serverOffset)+":"+UUID.randomUUID();body.addProperty("version",2);body.addProperty("requestId",request);body.addProperty("action",action);
        pending.put(request,new Pending(body,success));PhoneClient.fromPage(body.toString());dirty=true;
    }
    static void retry(Pending p){if(p.failure.isEmpty())return;p.failure="";p.sent=System.currentTimeMillis();PhoneClient.fromPage(p.body.toString());dirty=true;}
    static void requestState(){PhoneClient.fromPage("{\"action\":\"state\",\"version\":2}");}
    private static void clock(){
        var level=Minecraft.getInstance().level;if(level==null)return;
        String time=PhoneClock.time(level.getDayTime());
        String date=PhoneClock.date(level.getDayTime())+(level.isRaining()?" · 雨":" · 晴");
        if(!time.equals(lastClockText)){paintDigits("clock-d",time);lastClockText=time;}
        if(!time.equals(lastHeroTimeText)){paintDigits("hero-d",time);lastHeroTimeText=time;}
        if(!date.equals(lastHeroDateText)){set("hero-date",date);lastHeroDateText=date;}
    }
    static void clockFace(Element parent,String clazz){
        Element face=id(node(parent,"div","time-face "+clazz,""),"hero-time");
        id(node(face,"span","digit","0"),"hero-d0");id(node(face,"span","digit","0"),"hero-d1");
        node(face,"span","sep",":");
        id(node(face,"span","digit","0"),"hero-d2");id(node(face,"span","digit","0"),"hero-d3");
        Element warm=node(face,"span","time-warm","");
        for(char glyph:"0123456789:".toCharArray())node(warm,"span","digit",String.valueOf(glyph));
    }
    private static void paintDigits(String prefix,String time){
        if(el(prefix+"0")==null){if("clock-d".equals(prefix))set("clock",time);return;}
        int[] index={0,1,3,4};
        for(int slot=0;slot<4;slot++){
            Element digit=el(prefix+slot);if(digit==null)return;
            String glyph=String.valueOf(time.charAt(index[slot]));
            if(!glyph.equals(digit.getTextContent()))digit.setInnerText(glyph);
        }
    }
    private static void resetClockCache(){lastClockText="";lastHeroTimeText="";lastHeroDateText="";}
    private static void status(){attr(el("screen"),"data-theme",str(state,"theme").equals("dark")?"dark":"light");attr(el("screen"),"data-reduced",String.valueOf(reduced));set("signal",(bool(state,"online")?"▴▴▴ ":"离线 ")+num(state,"battery")+"%");}
    static void sound(){if(!muted)Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK,.9f));}
    private static void context(){
        if(ui.route.startsWith("chat:")){PhonePages.chatTools(ui.route.substring(5));return;}
        if(Set.of("wechat","contacts","moments","me").contains(ui.route)){Element form=sheet("微信");button(form,"添加朋友","card",()->go("friend"));button(form,"发起群聊","card",()->go("group"));return;}controls();
    }
    private static void controls(){
        Element form=sheet("控制中心");
        JsonObject call=obj(state,"callState");String phase=str(call,"phase");
        if(!phase.isEmpty()&&!phase.equals("idle")){
            String peer=str(call,"peer");
            Element row=node(form,"div","row","");
            button(row,switch(phase){case "incoming"->"来电："+peer;case "outgoing"->"正在呼叫 "+peer+"…";default->"通话中："+peer;},"card",PhonePages::callSheet);
            if(phase.equals("incoming"))button(row,"接听","primary",()->act("call_accept",new JsonObject(),PhonePages::callSheet));
            button(row,"挂断","danger",()->act("hangup",new JsonObject(),PhoneApricity::hideSheet));
        }
        Element net=node(form,"div","row","");button(net,bool(state,"online")?"网络已连接":"网络未连接","card",()->go("wifi"));button(net,"话费 / 流量","card",()->go("bill"));
        button(form,"切换深浅外观","card",()->act("setting",data("name","theme","text",str(state,"theme").equals("dark")?"light":"dark"),null));
        button(form,muted?"开启提示音":"关闭提示音","card",()->{muted=!muted;controls();});button(form,"锁定手机","card",()->go("lock"));
    }
    private static void recents(){
        Element form=sheet("最近使用");List<String> apps=new ArrayList<>(ui.recents);Collections.reverse(apps);
        for(String app:apps){Element row=node(form,"div","row","");button(row,title(app),"card",()->go(app));button(row,"×","small",()->{ui.recents.remove(app);recents();});}if(apps.isEmpty())empty(form,"还没有最近使用的应用");
    }
    private static void notifications(){Element form=sheet("通知中心");for(String message:notices)node(form,"div","card",message);if(notices.isEmpty())empty(form,"暂无通知");button(form,"清空通知","small",()->{notices.clear();notifications();});}
    static Element sheet(String title){
        Element overlay=el("overlay");if(overlay==null)return new Element(document,"div");overlay.clearChildren();attr(overlay,"class","overlay on");ui.overlay=title;
        Element sheet=node(overlay,"div","sheet",""),head=node(sheet,"div","row","");node(head,"div","cell-title",title);button(head,"完成","small",PhoneApricity::hideSheet);return sheet;
    }
    static void hideSheet(){ui.overlay="";Element overlay=el("overlay");if(overlay!=null){overlay.clearChildren();attr(overlay,"class","overlay");}}
    static String peerName(String id){
        if(id.startsWith("g:"))for(JsonObject g:objects("groups"))if(id.equals("g:"+str(g,"id")))return str(g,"name");
        for(JsonObject f:objects("book"))if(id.equals(str(f,"id")))return str(f,"name");
        for(JsonObject m:objects("messages"))if(id.equals(str(m,"conversation")))return str(m,"fromId").equals(str(state,"selfId"))?str(m,"to"):str(m,"from");return id;
    }
    static String title(String route){
        if(route.startsWith("chat:"))return peerName(route.substring(5));if(route.startsWith("sms:"))return route.substring(4);
        return switch(route){case "wechat"->"微信";case "wechat-bind"->"绑定微信";case "contacts","phonebook"->"通讯录";case "moments"->"朋友圈";case "me"->"我";case "friend"->"添加朋友";case "group"->"新建群聊";case "camera"->"相机";case "album","photo"->"照片";case "dial"->"电话";case "sms"->"短信";case "pay"->"钱包";case "bill"->"话费与流量";case "wifi"->"无线网络";case "settings"->"设置";case "map"->"地图";case "notes","note"->"便签";case "calc"->"计算器";case "timer"->"时钟";case "calendar"->"日历";case "recorder"->"录音机";case "convert"->"换算";case "editor"->"NPC 名片";default->"手机";};
    }
    static Element list(Element parent){return id(node(parent,"div","scroll",""),"main-list");}
    static Element cell(Element parent,String title,String sub,Runnable action){Element row=node(parent,"div","cell","");node(row,"div","avatar",title.isBlank()?"·":title.substring(0,1));Element copy=node(row,"div","cell-copy","");node(copy,"div","cell-title",title);node(copy,"div","cell-sub",sub);click(row,action);return row;}
    static Element image(Element parent,String id,boolean full,String clazz){Element image=node(parent,"texture",clazz,"");images.add(new ImageBinding(image,id,full));var location=PhonePhotoClient.texture(id,full);if(location!=null)attr(image,"src",location.toString());return image;}
    static Element input(Element parent,String id,String placeholder,boolean multiline){Element input=id(node(parent,multiline?"textarea":"input","",""),id);attr(input,"placeholder",placeholder);setValue(input,ui.draft(id));fields.put(id,input);input.addEventListener("input",e->ui.draft(id,value(input)));return input;}
    static Element button(Element parent,String text,String clazz,Runnable action){Element button=node(parent,"button",clazz,text);button.setAttribute("type","button");click(button,action);return button;}
    static void click(Element element,Runnable action){if(element!=null)element.addEventListener("click",e->action.run());}
    static Element node(Element parent,String tag,String clazz,String text){
        Element element=Element.init(document.createElement(tag));
        if(!clazz.isEmpty())element.setClassName(clazz);
        if(!text.isEmpty())element.setInnerText(text);
        parent.appendChild(element);
        return element;
    }
    static Element id(Element element,String id){element.setAttribute("id",id);return element;}
    static Element el(String id){return document==null?null:document.getElementById(id);}
    static void attr(Element element,String key,String value){if(element!=null&&!Objects.equals(element.getAttribute(key),value))element.setAttribute(key,value);}
    static void set(String id,String text){Element element=el(id);if(element!=null&&!element.getTextContent().equals(text))element.setInnerText(text);}
    static void setValue(Element element,String value){element.value=value;element.setAttribute("value",value);}
    static String value(Element element){return element==null||element.value==null?"":element.value.trim();}
    static void clear(Element element){setValue(element,"");ui.draft(element.getAttribute("id"),"");}
    static void empty(Element parent,String text){node(parent,"div","empty",text);}
    static void reconcile(Element list,String key,Runnable draw){if(key.equals(list.getAttribute("data-key")))return;double scroll=list.getScrollTop();list.clearChildren();draw.run();list.setScrollTop(scroll);attr(list,"data-key",key);images.removeIf(b->!b.element.isConnected());}
    static JsonArray array(String key){return state.has(key)&&state.get(key).isJsonArray()?state.getAsJsonArray(key):new JsonArray();}
    static List<JsonObject> objects(String key){List<JsonObject> objects=new ArrayList<>();array(key).forEach(v->{if(v.isJsonObject())objects.add(v.getAsJsonObject());});return objects;}
    static JsonObject obj(JsonObject body,String key){return body.has(key)&&body.get(key).isJsonObject()?body.getAsJsonObject(key):new JsonObject();}
    static String str(JsonObject body,String key){try{return body.has(key)&&!body.get(key).isJsonNull()?body.get(key).getAsString():"";}catch(RuntimeException ignored){return "";}}
    static boolean bool(JsonObject body,String key){return "true".equals(str(body,key));}
    static long num(JsonObject body,String key){try{return body.has(key)?body.get(key).getAsLong():0;}catch(RuntimeException ignored){return 0;}}
    static int number(String raw){try{return Integer.parseInt(raw);}catch(NumberFormatException ignored){return 0;}}
    static JsonObject data(Object... values){JsonObject object=new JsonObject();for(int i=0;i<values.length;i+=2){String key=values[i].toString();Object value=values[i+1];if(value instanceof Number n)object.addProperty(key,n);else object.addProperty(key,value.toString());}return object;}
    static String duration(long seconds){return String.format(Locale.ROOT,"%02d:%02d",Math.max(0,seconds)/60,Math.max(0,seconds)%60);}
}
