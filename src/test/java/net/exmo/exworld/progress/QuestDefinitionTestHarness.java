package net.exmo.exworld.progress;

import java.util.*;
import net.minecraft.resources.ResourceLocation;

/** Pure model checks for graph validation and persistent branch state. */
public final class QuestDefinitionTestHarness {
    public static void main(String[] args) {
        ResourceLocation id=ResourceLocation.fromNamespaceAndPath("test","quest");
        QuestObjective objective=new QuestObjective(QuestObjective.Type.KILL,ResourceLocation.fromNamespaceAndPath("minecraft","zombie"),"",1,"",0,0,0,4);
        QuestDefinition valid=new QuestDefinition(id,QuestKind.MAIN,"title","desc","start",List.of(new QuestNode("start","","",List.of(objective),List.of("end"),List.of()),new QuestNode("end","","",List.of(),List.of(),List.of())));
        check(valid.node("start").next().equals(List.of("end")),"validated next link remains available");
        boolean cycle=false;try{new QuestDefinition(id,QuestKind.SIDE,"","","a",List.of(new QuestNode("a","","",List.of(),List.of("b"),List.of()),new QuestNode("b","","",List.of(),List.of("a"),List.of())));}catch(IllegalArgumentException expected){cycle=true;}check(cycle,"cycles are rejected at content load");
        QuestInstance instance=new QuestInstance(id,"start",QuestStatus.ACTIVE,java.time.Instant.EPOCH);instance.progress().put(0,1);instance.selectedBranches().add("start->end");instance.nodeId("end");check(instance.progress().isEmpty()&&instance.selectedBranches().contains("start->end"),"advancing clears node progress but preserves permanent branch history");
        System.out.println("QUEST_DEFINITION_TEST_OK");
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
