package net.exmo.exworld.dungeon;

import net.exmo.exworld.dungeon.model.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** Builds only dungeon-owned slots. Battle presentation never calls this builder. */
public final class DungeonSceneBuilder {
    private DungeonSceneBuilder() {}
    public static void build(ServerLevel level, DungeonRun run, DungeonDefinition definition) {
        for(int index=0;index<definition.rooms().size();index++) buildRoom(level,run,definition,definition.rooms().get(index),index);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int index = 0; index + 1 < definition.rooms().size(); index++) {
            DungeonRoomDefinition room = definition.rooms().get(index);
            int start = originX(run, index) + room.width();
            int end = originX(run, index + 1);
            for (int x = start; x < end; x++) level.setBlock(cursor.set(x, room.floorY(), originZ(run) + room.depth() / 2), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        }
    }
    public static void buildRoom(ServerLevel level,DungeonRun run,DungeonDefinition definition,DungeonRoomDefinition room,int index){
        int ox=run.slotX()+index*64,oz=run.slotZ();BlockPos.MutableBlockPos cursor=new BlockPos.MutableBlockPos();
        for(int x=0;x<room.width();x++)for(int z=0;z<room.depth();z++){
            level.setBlock(cursor.set(ox+x,room.floorY(),oz+z),Blocks.STONE_BRICKS.defaultBlockState(),3);
            if(x==0||z==0||x==room.width()-1||z==room.depth()-1)level.setBlock(cursor.set(ox+x,room.floorY()+1,oz+z),Blocks.DEEPSLATE_BRICKS.defaultBlockState(),3);
        }
        room.blocked().forEach(point->level.setBlock(cursor.set(ox+point.x(),room.floorY()+1,oz+point.z()),Blocks.COBBLESTONE.defaultBlockState(),3));
        level.setBlock(cursor.set(ox+room.entrance().x(),room.floorY()+1,oz+room.entrance().z()),Blocks.AIR.defaultBlockState(),3);
        level.setBlock(cursor.set(ox+room.exit().x(),room.floorY()+1,oz+room.exit().z()),Blocks.AIR.defaultBlockState(),3);
        level.setBlock(cursor.set(ox,room.floorY()+1,oz+room.depth()/2),Blocks.AIR.defaultBlockState(),3);
        level.setBlock(cursor.set(ox+room.width()-1,room.floorY()+1,oz+room.depth()/2),Blocks.AIR.defaultBlockState(),3);
    }
    public static int originX(DungeonRun run,int roomIndex){return run.slotX()+roomIndex*64;}
    public static int originZ(DungeonRun run){return run.slotZ();}
    public static void clear(ServerLevel level, DungeonRun run, DungeonDefinition definition) {
        if (level == null) return;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int index = 0; index < definition.rooms().size(); index++) {
            DungeonRoomDefinition room = definition.rooms().get(index);
            for (int x = 0; x < room.width(); x++) for (int z = 0; z < room.depth(); z++)
                for (int y = room.floorY(); y <= room.floorY() + 4; y++)
                    level.setBlock(cursor.set(originX(run, index) + x, y, originZ(run) + z), Blocks.AIR.defaultBlockState(), 3);
        }
        for (int index = 0; index + 1 < definition.rooms().size(); index++) {
            DungeonRoomDefinition room = definition.rooms().get(index);
            int start = originX(run, index) + room.width(), end = originX(run, index + 1);
            for (int x = start; x < end; x++) for (int y = room.floorY(); y <= room.floorY() + 4; y++)
                level.setBlock(cursor.set(x, y, originZ(run) + room.depth() / 2), Blocks.AIR.defaultBlockState(), 3);
        }
    }
}
