package net.exmo.exworld.battle.data;

import java.util.Map;

/** Six presentation tracks; blank tracks deliberately fall back to ExWorld generic particles. */
public record VfxDefinition(String id,Map<Track,String> particles,Map<Track,String> sounds){
    public VfxDefinition{particles=Map.copyOf(particles);sounds=Map.copyOf(sounds);}
    public enum Track{CAST,FLIGHT,IMPACT,AREA,PERSISTENT,STATUS}
}
