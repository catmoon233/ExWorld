package net.exmo.exworld.battle.data;

public record IntroProfile(String id,int fadeTicks,int factionTravelTicks,int factionHoldTicks,int overviewTicks,float fov,boolean blackBars){
    public IntroProfile{fadeTicks=Math.max(0,fadeTicks);factionTravelTicks=Math.max(0,factionTravelTicks);factionHoldTicks=Math.max(0,factionHoldTicks);overviewTicks=Math.max(0,overviewTicks);fov=Math.max(30,Math.min(110,fov));}
    public int totalTicks(int factions){return fadeTicks+Math.max(0,factions)*(factionTravelTicks+factionHoldTicks)+overviewTicks;}
    public static IntroProfile defaults(){return new IntroProfile("exworld:default",8,20,15,25,55,true);}
}
