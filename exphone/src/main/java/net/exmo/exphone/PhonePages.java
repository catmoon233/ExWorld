package net.exmo.exphone;

import com.google.gson.*;
import com.sighs.apricityui.init.Element;
import java.util.*;
import static net.exmo.exphone.PhoneApricity.*;

/** Social applications rendered from structured server data. */
final class PhonePages {
    static void draw(Element page){
        String route=ui.route;
        if(route.startsWith("chat:")){chat(page,route.substring(5));return;}
        if(route.startsWith("sms:")){smsThread(page,route.substring(4));return;}
        switch(route){
            case "lock"->lock(page);case "home"->desktop(page);case "wechat"->conversations(page);case "wechat-bind"->wechatBind(page);
            case "contacts"->contacts(page);case "friend"->friend(page);case "group"->group(page);
            case "moments"->moments(page);case "me"->me(page);case "sms"->sms(page);case "dial"->dial(page);case "phonebook"->phonebook(page);
            default->PhoneUtilities.draw(page);
        }
    }
    private static void wechatBind(Element page){
        node(page,"div","title","绑定手机号");String number=str(state,"number");
        node(page,"div","card",number.isBlank()?"当前手机没有安装 SIM 卡。先给手机安装 SIM，再绑定微信。":"当前手机号："+number);
        if(!number.isBlank())button(page,"绑定当前手机号","primary",()->act("wechat_bind",new JsonObject(),()->go("wechat")));
        button(page,"刷新号码","card",PhoneApricity::requestState);
    }
    private static void lock(Element page){
        clockFace(page,"hero-time");id(node(page,"div","hero-date",""),"hero-date");
        node(page,"div","hero-weather",PhoneLock.hasPin()?"输入密码或向上轻扫解锁":"向上轻扫，开始新的一天");node(page,"div","spacer","");
        Element pin=null;
        if(PhoneLock.hasPin()){pin=id(node(page,"input","",""),"lock-pin");attr(pin,"placeholder","4–8 位数字密码");attr(pin,"type","password");}
        Element entry=pin;
        button(page,"解锁","unlock",()->unlock(value(entry)));
        button(page,"相机","small",()->go("camera"));
    }
    private static void desktop(Element page){
        Element widget=node(page,"div","widget","");clockFace(widget,"widget-time");id(node(widget,"div","muted",""),"hero-date");
        String[][] apps={{"wechat","微信","#12b867"},{"camera","相机","#637086"},{"album","照片","#b778e8"},{"map","地图","#42aab8"},
                {"settings","设置","#74839b"},{"bill","话费","#eb943c"},{"notes","便签","#d6a13d"},{"calc","计算器","#344357"},
                {"timer","时钟","#364d6c"},{"calendar","日历","#d55362"},{"recorder","录音机","#c46a89"},{"wifi","无线网络","#4b96d5"},
                {"convert","换算","#319caf"},{"editor","NPC 名片","#8671c8"}};
        Element grid=node(page,"div","apps",""),row=null;int shown=0;
        for(String[] entry:apps){if(entry[0].equals("editor")&&!bool(state,"editor"))continue;if(shown++%4==0)row=node(grid,"div","app-row","");app(row,entry);}
        node(page,"div","spacer","");
        Element dock=node(page,"div","dock","");app(dock,new String[]{"dial","电话","#25b368"});app(dock,new String[]{"sms","短信","#3689e6"});app(dock,new String[]{"pay","钱包","#bf9854"});
    }
    private static void app(Element parent,String[] app){
        Element button=button(parent,"","app",()->go(app[0]));Element icon=node(button,"div","icon","");
        attr(icon,"style","background-color:"+app[2]);
        if(app[0].equals("wechat"))wechatMark(icon);else icon(icon,app[0]);
        node(button,"span","",app[1]);
    }
    private static void conversations(Element page){
        Element search=input(page,"search","搜索会话",false),list=list(page);
        Runnable draw=()->{
            LinkedHashMap<String,JsonObject> latest=new LinkedHashMap<>();
            for(JsonObject message:objects("messages"))latest.putIfAbsent(str(message,"conversation"),message);
            for(JsonObject group:objects("groups"))latest.putIfAbsent("g:"+str(group,"id"),new JsonObject());
            reconcile(list,latest.toString()+value(search)+ui.read,()->{
                for(var entry:latest.entrySet()){
                    String peer=entry.getKey();if(peer.isBlank())continue;String name=peerName(peer);
                    if(!name.toLowerCase(Locale.ROOT).contains(value(search).toLowerCase(Locale.ROOT)))continue;
                    JsonObject message=entry.getValue();Element row=cell(list,name,str(message,"text").isBlank()?"开始聊天":str(message,"text"),()->go("chat:"+peer));
                    if(!str(message,"fromId").equals(str(state,"selfId"))&&!str(message,"id").equals(ui.read.get(peer))&&message.has("id"))node(row,"span","badge","●");
                }
                if(list.childNodes.isEmpty())empty(list,"还没有会话\n从通讯录选择好友开始聊天");
            });
        };refreshers.add(draw);search.addEventListener("input",e->draw.run());tabs(page,"wechat");
    }
    private static void contacts(Element page){
        Element row=node(page,"div","row","");button(row,"新的朋友","",()->go("friend"));button(row,"发起群聊","",()->go("group"));
        Element search=input(page,"contact-search","搜索联系人",false),list=list(page);
        Runnable draw=()->reconcile(list,array("book").toString()+array("requests")+value(search),()->{
            for(JsonElement request:array("requests")){
                String name=request.getAsString();Element card=node(list,"div","card","");node(card,"div","cell-title",name+" 请求加好友");
                Element actions=node(card,"div","row","");button(actions,"接受","primary",()->act("accept",data("to",name),null));button(actions,"拒绝","",()->act("reject",data("to",name),null));
            }
            for(JsonObject friend:objects("book"))if(str(friend,"name").contains(value(search)))cell(list,str(friend,"name"),str(friend,"number"),()->go("chat:"+str(friend,"id")));
            if(list.childNodes.isEmpty())empty(list,"还没有联系人");
        });refreshers.add(draw);search.addEventListener("input",e->draw.run());tabs(page,"contacts");
    }
    private static void friend(Element page){
        node(page,"div","title","添加朋友");Element name=input(page,"friend-name","微信名或手机号",false);
        button(page,"发送好友申请","primary",()->act("friend",data("to",value(name)),()->clear(name)));node(page,"div","muted","对方通过申请后，即可聊天、分享照片和转账。");
    }
    private static void group(Element page){
        Element name=input(page,"group-name","群聊名称（可选）",false),list=list(page);
        for(JsonObject friend:objects("book")){
            String who=str(friend,"name");Element choose=node(list,"button","card",(groupMembers.contains(who)?"✓  ":"○  ")+who);
            click(choose,()->{if(!groupMembers.remove(who))groupMembers.add(who);choose.setInnerText((groupMembers.contains(who)?"✓  ":"○  ")+who);});
        }
        button(page,"创建群聊","primary",()->act("group",data("text",value(name),"to",String.join(",",groupMembers)),()->{groupMembers.clear();go("wechat");}));
    }
    private static void chat(Element page,String peer){
        Element messages=list(page);
        Runnable draw=()->{
            List<JsonObject> thread=objects("messages").stream().filter(m->str(m,"conversation").equals(peer)).toList();
            String key=thread.toString()+pending.values().stream().map(p->p.body.toString()+p.failure).toList();
            boolean bottom=messages.getScrollTop()>=messages.scrollHeight-600;
            reconcile(messages,key,()->{
                List<JsonObject> ordered=new ArrayList<>(thread);Collections.reverse(ordered);for(JsonObject message:ordered)bubble(messages,message);
                for(Pending p:pending.values()){
                    if(!p.route.equals(ui.route))continue;Element row=node(messages,"div","bubble-row mine","");node(row,"div","bubble",str(p.body,"text"));
                    button(row,p.failure.isEmpty()?"发送中…":p.failure+" · 重试","small",()->retry(p));
                }
                if(messages.childNodes.isEmpty())empty(messages,"发送第一条消息吧");
            });
            if(!thread.isEmpty()){ui.read.put(peer,str(thread.getFirst(),"id"));if(bottom)messages.setScrollTop(messages.scrollHeight);}
        };refreshers.add(draw);
        Element composer=node(page,"div","composer chat-composer","");
        button(composer,"图片","chat-photo-button",()->PhoneUtilities.pickPhoto(id->PhonePhotoClient.upload(id,()->act("image",data("to",peer,"photo",id),PhoneApricity::hideSheet))));
        button(composer,"＋","",()->chatTools(peer));
        Element text=input(composer,"chat-text","发送消息",false);
        Runnable send=()->{String message=value(text);if(!message.isBlank())act("chat",data("to",peer,"text",message),()->{if(value(text).equals(message))clear(text);});};
        button(composer,"发送","primary",send);
        text.addEventListener("keydown",e->{if(e instanceof com.sighs.apricityui.event.KeyEvent k&&"Enter".equals(k.key))send.run();});
    }
    private static void bubble(Element list,JsonObject message){
        boolean mine=str(message,"fromId").equals(str(state,"selfId"));Element row=node(list,"div",mine?"bubble-row mine":"bubble-row","");
        node(row,"div","bubble-name",str(message,"from"));String kind=str(message,"kind");
        if(kind.equals("image")){
            Element image=image(row,str(message,"extra"),false,"photo");click(image,()->{selectedPhoto=str(message,"extra");go("photo");});
        }else if(kind.equals("audio")){
            button(row,"▶  播放语音","bubble",()->PhoneRecorder.play(str(message,"extra")));
        }else if(kind.equals("transfer")||kind.equals("redpack")){
            String packet=str(message,"extra");JsonObject money=objects("packets").stream().filter(p->str(p,"id").equals(packet)).findFirst().orElse(new JsonObject());
            button(row,str(message,"text")+(bool(money,"claimed")?"\n已领取":"\n查看并领取"),"bubble money",()->{
                Element form=sheet("领取");node(form,"div","title",str(message,"text"));button(form,"确认领取","primary",()->act("claim",data("id",packet),PhoneApricity::hideSheet));
            });
        }else node(row,"div","bubble",str(message,"text"));
    }
    static void chatTools(String peer){
        Element form=sheet("会话工具");
        button(form,"照片图库","card",()->PhoneUtilities.pickPhoto(id->PhonePhotoClient.upload(id,()->act("image",data("to",peer,"photo",id),PhoneApricity::hideSheet))));
        button(form,"发送语音","card",()->voiceCompose(peer));
        button(form,"拍摄照片","card",()->go("camera"));
        button(form,peer.startsWith("g:")?"发红包":"转账","card",()->{
            Element money=sheet(peer.startsWith("g:")?"发红包":"转账"),amount=input(money,"money","金币数量",false),shares=peer.startsWith("g:")?input(money,"shares","红包份数",false):null;
            button(money,"确认发送","primary",()->act(peer.startsWith("g:")?"redpack":"transfer",data("to",peer,"amount",number(value(amount)),"shares",shares==null?1:number(value(shares))),PhoneApricity::hideSheet));
        });
        if(peer.startsWith("g:"))button(form,"返回群聊","card",()->{hideSheet();go("chat:"+peer);});
        else button(form,"语音电话","card",()->act("call",data("to",peer),PhonePages::callSheet));
    }
    private static void moments(Element page){
        button(page,"分享这一刻","primary",PhonePages::composeMoment);Element list=list(page);
        refreshers.add(()->reconcile(list,array("moments").toString(),()->{
            for(JsonObject moment:objects("moments")){
                Element card=node(list,"div","card","");node(card,"div","cell-title",str(moment,"author"));node(card,"div","bubble",str(moment,"text"));
                if(!str(moment,"photo").isBlank())image(card,str(moment,"photo"),false,"photo");
                Element row=node(card,"div","row","");button(row,(bool(moment,"liked")?"♥ ":"♡ ")+num(moment,"likes"),"small",()->act("like",data("id",str(moment,"id")),null));
                button(row,"评论","small",()->{Element form=sheet("评论"),text=input(form,"comment","写下评论",false);button(form,"发布","primary",()->act("comment",data("id",str(moment,"id"),"text",value(text)),PhoneApricity::hideSheet));});
                if(moment.has("comments"))for(JsonElement comment:moment.getAsJsonArray("comments")){JsonObject c=comment.getAsJsonObject();node(card,"div","muted",str(c,"from")+"："+str(c,"text"));}
            }
            if(list.childNodes.isEmpty())empty(list,"朋友们分享的生活会出现在这里");
        }));tabs(page,"moments");
    }
    static void composeMoment(){
        Element form=sheet("分享这一刻"),text=input(form,"moment-text","这一刻的想法…",true);
        button(form,momentPhoto.isBlank()?"添加照片":"已选照片 · 更换","card",()->{ui.draft("moment-text",value(text));PhoneUtilities.pickPhoto(id->{momentPhoto=id;composeMoment();});});
        button(form,"发布","primary",()->{
            String content=value(text),photo=momentPhoto;
            Runnable publish=()->act("moment",data("text",content,"photo",photo),()->{momentPhoto="";ui.drafts.remove(ui.route+"/moment-text");hideSheet();});
            if(photo.isBlank())publish.run();else PhonePhotoClient.upload(photo,publish);
        });
    }
    private static void me(Element page){
        node(page,"div","title",str(state,"wechat"));node(page,"div","muted","已绑定手机号："+str(state,"number"));
        cell(page,"钱包",num(state,"gold")+" 金币",()->go("pay"));cell(page,"个人设置","外观与微信名",()->go("settings"));node(page,"div","spacer","");tabs(page,"me");
    }
    private static void tabs(Element page,String selected){
        Element tabs=node(page,"div","tabs","");String[][] options={{"wechat","微信"},{"contacts","通讯录"},{"moments","发现"},{"me","我"}};
        for(String[] tab:options){
            Element button=button(tabs,"",tab[0].equals(selected)?"selected":"",()->go(tab[0]));
            attr(button,"title",tab[1]);
            if(tab[0].equals("wechat"))wechatMark(button);
            else {
                Element mark=node(button,"div","tab-mark "+tab[0],"");
                node(mark,"div","mark-head","");node(mark,"div","mark-body","");
            }
        }
    }
    private static void voiceCompose(String peer){
        Element form=sheet("发送语音");
        node(form,"div","muted","录音使用 Simple Voice Chat 的麦克风；请启用语音输入。最长 15 秒。");
        button(form,PhoneRecorder.recording()?"停止并保存":"开始录音","primary",()->{
            notice(PhoneRecorder.recording()?PhoneRecorder.stop():PhoneRecorder.start());voiceCompose(peer);
        });
        button(form,"刷新录音列表","small",()->voiceCompose(peer));
        for(String id:PhoneRecorder.clips()){
            Element row=node(form,"div","row","");button(row,"▶ 试听","",()->PhoneRecorder.play(id));
            button(row,"发送这段语音","primary",()->PhoneRecorder.upload(id,()->act("audio",data("to",peer,"audio",id),PhoneApricity::hideSheet)));
        }
        if(PhoneRecorder.clips().isEmpty())empty(form,"还没有录音");
    }
    private static void wechatMark(Element parent){
        Element mark=node(parent,"div","wx-mark","");
        node(mark,"div","wx-bubble one","");node(mark,"div","wx-bubble two","");
        node(mark,"div","wx-eye a","");node(mark,"div","wx-eye b","");
    }
    private static void sms(Element page){
        Element target=input(page,"sms-to","手机号码",false);button(page,"新建短信","primary",()->{if(!value(target).isBlank())go("sms:"+value(target));});Element list=list(page);
        refreshers.add(()->reconcile(list,array("sms").toString(),()->{
            Set<String> peers=new LinkedHashSet<>();for(JsonObject sms:objects("sms")){String peer=str(sms,"from").equals(str(state,"number"))?str(sms,"to"):str(sms,"from");if(peers.add(peer))cell(list,peer,str(sms,"text"),()->go("sms:"+peer));}
            if(list.childNodes.isEmpty())empty(list,"暂无短信");
        }));
    }
    private static void smsThread(Element page,String peer){
        Element list=list(page);
        refreshers.add(()->reconcile(list,array("sms").toString(),()->{
            List<JsonObject> lines=new ArrayList<>(objects("sms"));Collections.reverse(lines);
            for(JsonObject line:lines){if(!str(line,"from").equals(peer)&&!str(line,"to").equals(peer))continue;Element row=node(list,"div",str(line,"from").equals(str(state,"number"))?"bubble-row mine":"bubble-row","");node(row,"div","bubble",str(line,"text"));}
        }));
        Element text=input(page,"sms-text","短信内容",false);button(page,"发送短信","primary",()->act("sms",data("to",peer,"text",value(text)),()->clear(text)));
    }
    private static void dial(Element page){
        Element tabs=node(page,"div","row","");button(tabs,"拨号","primary",()->{});button(tabs,"通讯录","",()->go("phonebook"));
        Element number=input(page,"dial-number","手机号或玩家名",false),keys=node(page,"div","keys","");
        for(String key:List.of("1","2","3","4","5","6","7","8","9","*","0","⌫"))button(keys,key,"dial-key",()->{String value=value(number);setValue(number,key.equals("⌫")?value.substring(0,Math.max(0,value.length()-1)):value+key);});
        button(page,"呼叫","primary",()->act("call",data("to",value(number)),PhonePages::callSheet));button(page,"当前通话","card",PhonePages::callSheet);
        Element list=list(page);refreshers.add(()->reconcile(list,array("calls").toString(),()->{
            for(JsonObject call:objects("calls"))cell(list,str(call,"peer"),str(call,"kind").equals("miss")?"未接来电":"通话记录",()->setValue(number,str(call,"peer")));
            if(list.childNodes.isEmpty())empty(list,"暂无通话记录");
        }));
    }
    private static void phonebook(Element page){
        Element tabs=node(page,"div","row","");button(tabs,"拨号","",()->go("dial"));button(tabs,"通讯录","primary",()->{});
        Element name=input(page,"phone-name","联系人名称",false),number=input(page,"phone-number","手机号",false);
        button(page,"保存联系人","primary",()->act("contact_save",data("name",value(name),"number",value(number)),()->{clear(name);clear(number);}));
        Element search=input(page,"phone-search","搜索姓名或号码",false),list=list(page);
        Runnable draw=()->reconcile(list,array("phoneContacts").toString()+array("book")+value(search),()->{
            LinkedHashMap<String,String> entries=new LinkedHashMap<>();
            for(JsonObject contact:objects("phoneContacts"))entries.put(str(contact,"number"),str(contact,"name"));
            for(JsonObject friend:objects("book"))if(!str(friend,"number").isBlank())entries.putIfAbsent(str(friend,"number"),str(friend,"name"));
            entries.forEach((digits,label)->{
                if(!label.contains(value(search))&&!digits.contains(value(search)))return;
                Element card=node(list,"div","card","");node(card,"div","cell-title",label);node(card,"div","muted",digits);
                Element row=node(card,"div","row","");button(row,"呼叫","primary",()->act("call",data("to",digits),PhonePages::callSheet));
                button(row,"短信","",()->go("sms:"+digits));
                if(objects("phoneContacts").stream().anyMatch(c->str(c,"number").equals(digits)))button(row,"删除","danger",()->act("contact_delete",data("number",digits),null));
            });
            if(list.childNodes.isEmpty())empty(list,"通讯录还没有联系人");
        });refreshers.add(draw);search.addEventListener("input",e->draw.run());
    }
    static void callSheet(){
        Element form=sheet("电话");JsonObject call=obj(state,"callState");String peer=str(call,"peer"),phase=str(call,"phase");
        node(form,"div","title",switch(phase){case "incoming"->(peer.isBlank()?"来电":peer+" 的来电");case "outgoing"->(peer.isBlank()?"正在呼叫…":"正在呼叫 "+peer+"…");case "active"->(peer.isBlank()?"通话中":"与 "+peer+" 通话中");default->"当前没有通话";});
        node(form,"div","muted",switch(phase){
            case "incoming"->"接听后开始双向语音通话";
            case "outgoing"->"等待对方接听…可以随时取消";
            case "active"->"已通话 "+duration(activeSeconds())+" · 1 话费/分钟 · 每分钟手机耗电 1%";
            default->"从聊天工具的「语音电话」或拨号页发起通话";});
        if(phase.equals("incoming"))button(form,"接听","primary",()->act("call_accept",new JsonObject(),PhonePages::callSheet));
        if(phase.equals("incoming")||phase.equals("outgoing")||phase.equals("active")){
            button(form,"挂断","danger",()->act("hangup",new JsonObject(),PhoneApricity::hideSheet));
        }else button(form,"关闭","card",PhoneApricity::hideSheet);
    }
    private static long activeSeconds(){
        long started=num(obj(state,"callState"),"startedAt");
        return started<=0?0:Math.max(0,(System.currentTimeMillis()-started)/1000);
    }
    private static void icon(Element parent,String app){
        Element svg=node(parent,"svg","","");attr(svg,"viewBox","0 0 32 32");attr(svg,"width","30");attr(svg,"height","30");
        String path=switch(app){
            case "wechat","sms"->"M5 5H25V22H15L8 27V22H5ZM10 12H22M10 17H19";
            case "camera"->"M4 9H10L12 5H20L22 9H28V26H4ZM22 17A6 6 0 1 1 10 17A6 6 0 1 1 22 17";
            case "album","map"->"M5 4H27V28H5ZM5 23L12 15L18 21L23 13L27 19M10 9H13";
            case "dial"->"M7 4L12 11L9 14Q13 22 19 24L22 20L28 24L26 29Q10 29 3 10L4 5Z";
            case "settings"->"M16 3V8M16 24V29M3 16H8M24 16H29M7 7L10 10M22 22L25 25M7 25L10 22M22 10L25 7M24 16A8 8 0 1 1 8 16A8 8 0 1 1 24 16M19 16A3 3 0 1 1 13 16A3 3 0 1 1 19 16";
            case "wifi"->"M3 10Q16 0 29 10M7 16Q16 8 25 16M12 22Q16 18 20 22M16 27H16.1";
            case "timer"->"M27 16A11 11 0 1 1 5 16A11 11 0 1 1 27 16M16 8V16L22 20";
            case "calendar"->"M5 7H27V28H5ZM5 12H27M10 3V10M22 3V10M10 17H14M18 17H22M10 22H14";
            case "recorder"->"M12 5V18A4 4 0 0 0 20 18V5A4 4 0 0 0 12 5ZM7 15V19A9 9 0 0 0 25 19V15M16 28V23M11 28H21";
            case "pay","bill"->"M4 7H26V27H4ZM4 12H28V23H20V12M23 17H25";
            default->"M7 3H25V29H7ZM11 10H21M11 16H21M11 22H18";
        };
        Element shape=node(svg,"path","","");attr(shape,"d",path);attr(shape,"fill","none");attr(shape,"stroke","#ffffff");attr(shape,"stroke-width","2.2");attr(shape,"stroke-linecap","round");attr(shape,"stroke-linejoin","round");
    }
}
