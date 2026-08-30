package net.exmo.exworld.client.battle;

/** Client prediction clock that advances smoothly between authoritative movement snapshots. */
public final class BattleMotionClock {
    private int serverElapsed;
    private long predictedElapsed;
    private long receivedAtTick;
    private boolean initialized;
    public void sync(int elapsed,long clientTick){
        if(!initialized){serverElapsed=Math.max(0,elapsed);predictedElapsed=serverElapsed;receivedAtTick=clientTick;initialized=true;return;}
        predictedElapsed=Math.max(predictedElapsed+Math.max(0,clientTick-receivedAtTick),elapsed);
        serverElapsed=Math.max(serverElapsed,elapsed);receivedAtTick=clientTick;
    }
    public double sample(long clientTick,float partialTick,int duration){double predicted=Math.max(predictedElapsed,serverElapsed+Math.max(0,clientTick-receivedAtTick));return Math.min(Math.max(1,duration),predicted+Math.max(0,partialTick));}
}
