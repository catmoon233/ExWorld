package net.exmo.exworld.battle.data;

import java.util.List;
import java.util.Random;

public record RewardPoolDefinition(String id, int gold, List<Entry> entries) {
    public RewardPoolDefinition { gold=Math.max(0,gold);entries=List.copyOf(entries);if(entries.isEmpty())throw new IllegalArgumentException("Reward pool "+id+" is empty"); }
    public List<String> choose(long seed,int count){Random random=new Random(seed);java.util.ArrayList<String> result=new java.util.ArrayList<>();java.util.ArrayList<Entry> available=new java.util.ArrayList<>(entries);while(!available.isEmpty()&&result.size()<count){int total=available.stream().mapToInt(Entry::weight).sum(),roll=random.nextInt(Math.max(1,total));Entry selected=available.getFirst();for(Entry entry:available){roll-=entry.weight();if(roll<0){selected=entry;break;}}result.add(selected.cardId());available.remove(selected);}return List.copyOf(result);}
    public record Entry(String cardId,int weight,String rarity){public Entry{weight=Math.max(1,weight);}}
}
