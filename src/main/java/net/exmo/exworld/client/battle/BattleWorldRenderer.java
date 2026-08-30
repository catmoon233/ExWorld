package net.exmo.exworld.client.battle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.exmo.exworld.battle.api.BattleSnapshot;
import net.exmo.exworld.battle.model.BattleCell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

/** World-space tactical indicators driven by the same cursor projection used for click intents. */
public final class BattleWorldRenderer {
    private static final float FLOATING_NUMBER_SCALE_MULTIPLIER = 3.0F;

    private BattleWorldRenderer() {}

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !BattleClient.active()) return;
        Minecraft minecraft = Minecraft.getInstance();
        BattleSnapshot snapshot = BattleClient.snapshot();
        BattleSnapshot.CombatantView actor = BattleClient.localCombatant();
        if (minecraft.level == null || snapshot == null || actor == null) return;

        PoseStack pose = event.getPoseStack(); Vec3 camera = event.getCamera().getPosition();
        pose.pushPose(); pose.translate(-camera.x, -camera.y, -camera.z);
        VertexConsumer filled = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.debugFilledBox());

        BattleSnapshot.CardView selected = BattleClient.selectedCardView();
        int radius = selected == null ? actor.movementRemaining() : selected.range();
        int minX = Math.max(0, actor.cell().x() - radius), maxX = Math.min(snapshot.arenaSize() - 1, actor.cell().x() + radius);
        int minZ = Math.max(0, actor.cell().z() - radius), maxZ = Math.min(snapshot.arenaSize() - 1, actor.cell().z() + radius);
        float red = selected == null ? .20F : .76F, green = selected == null ? .72F : .47F, blue = selected == null ? .95F : .18F;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            BattleCell cell = new BattleCell(x, z, actor.cell().floorY());
            if (!inSkillRange(actor.cell(), cell, selected, radius)) continue;
            addCell(pose, filled, snapshot, cell, red, green, blue, selected == null ? .20F : .28F, .024);
        }
        renderCombatantCells(pose, filled, snapshot, actor);
        minecraft.renderBuffers().bufferSource().endBatch(RenderType.debugFilledBox());

        BattleCell hovered = BattleClient.hoveredCell();
        if (hovered != null) {
            VertexConsumer lines = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());
            boolean inRange = inSkillRange(actor.cell(), hovered, selected, radius);
            double x = snapshot.arenaOriginX() + hovered.x(), y = hovered.floorY() + 1.025, z = snapshot.arenaOriginZ() + hovered.z();
            LevelRenderer.renderLineBox(pose, lines, x + .02, y, z + .02, x + .98, y + .05, z + .98,
                    inRange ? .3F : 1F, inRange ? 1F : .25F, inRange ? .72F : .25F, 1F);
            minecraft.renderBuffers().bufferSource().endBatch(RenderType.lines());
        }
        renderMovementIntent(pose, minecraft, snapshot, actor, hovered, selected == null && !BattleClient.itemTargeting());
        pose.popPose();
        renderFloatingNumbers(event, minecraft);
        renderSkillCaptions(event, minecraft);
    }

    private static void renderSkillCaptions(RenderLevelStageEvent event, Minecraft minecraft) {
        if (BattleClient.skillCaptions().isEmpty()) return;
        Vec3 camera=event.getCamera().getPosition();
        for(BattleClient.SkillCaption caption:BattleClient.skillCaptions()){
            Vec3 tracked=BattleClient.presentationPosition(caption.actorId(),event.getPartialTick().getGameTimeDeltaPartialTick(true));
            if(tracked==null)continue;float fade=Mth.clamp((42-caption.age())/12F,0,1);
            PoseStack pose=event.getPoseStack();pose.pushPose();pose.translate(tracked.x-camera.x+.65,tracked.y-camera.y+1.45+caption.age()*.012,tracked.z-camera.z);
            pose.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());float scale=.024F;pose.scale(-scale,-scale,scale);
            Component text=Component.translatable(caption.nameKey());float x=-minecraft.font.width(text)/2F;Matrix4f matrix=pose.last().pose();
            minecraft.font.drawInBatch(text,x,0,0xFFF4D27A,false,matrix,minecraft.renderBuffers().bufferSource(),Font.DisplayMode.SEE_THROUGH,((int)(fade*150)<<24),0xF000F0);
            minecraft.font.drawInBatch(text,x,0,0xFFF7E5B0,false,matrix,minecraft.renderBuffers().bufferSource(),Font.DisplayMode.NORMAL,0,0xF000F0);
            minecraft.renderBuffers().bufferSource().endBatch();pose.popPose();
        }
    }

    private static void renderMovementIntent(PoseStack pose, Minecraft minecraft, BattleSnapshot snapshot,
                                             BattleSnapshot.CombatantView actor, BattleCell hovered, boolean moveMode) {
        VertexConsumer lines = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());
        boolean rendered = false;
        for (BattleSnapshot.MotionView motion : snapshot.motions()) {
            BattleSnapshot.CombatantView moving = snapshot.combatants().get(motion.actorId());
            if (moving == null || moving.downed()) continue;
            if (motion.path().isEmpty()) continue;
            addPath(pose, lines, snapshot, motion.start(), motion.path(), .30F, .95F, 1F, .9F);
            rendered = true;
        }
        if (moveMode && hovered != null) {
            var preview = BattleClient.movementPreviewPath();
            if (preview.isPresent()) {
                List<BattleCell> path = preview.get();
                if (!path.isEmpty()) addPath(pose, lines, snapshot, actor.cell(), path, .38F, 1F, .68F, 1F);
                addDestinationBox(pose, lines, snapshot, hovered, .38F, 1F, .68F);
            } else {
                addDestinationBox(pose, lines, snapshot, hovered, 1F, .25F, .25F);
            }
            rendered = true;
        }
        if (rendered) minecraft.renderBuffers().bufferSource().endBatch(RenderType.lines());
    }

    private static void renderCombatantCells(PoseStack pose, VertexConsumer consumer, BattleSnapshot snapshot,
                                             BattleSnapshot.CombatantView localActor) {
        for (BattleSnapshot.CombatantView combatant : snapshot.combatants().values()) {
            if (combatant.downed()) continue;
            boolean ally = localActor.factionId().equals(combatant.factionId());
            float red = ally ? .19F : .96F, green = ally ? .50F : .20F, blue = ally ? 1F : .28F;
            addCell(pose, consumer, snapshot, combatant.cell(), red, green, blue, .48F, .055);
        }
    }

    private static void addPath(PoseStack pose, VertexConsumer lines, BattleSnapshot snapshot, BattleCell start,
                                List<BattleCell> path, float red, float green, float blue, float alpha) {
        Vec3 previous = cellCenter(snapshot, start).add(0, .08, 0);
        for (BattleCell cell : path) {
            Vec3 next = cellCenter(snapshot, cell).add(0, .08, 0);
            addLine(pose, lines, previous, next, red, green, blue, alpha);
            addDestinationBox(pose, lines, snapshot, cell, red, green, blue);
            previous = next;
        }
    }

    private static void addDestinationBox(PoseStack pose, VertexConsumer lines, BattleSnapshot snapshot, BattleCell cell,
                                          float red, float green, float blue) {
        double x=snapshot.arenaOriginX()+cell.x(),y=cell.floorY()+1.035,z=snapshot.arenaOriginZ()+cell.z();
        LevelRenderer.renderLineBox(pose,lines,x+.06,y,z+.06,x+.94,y+.18,z+.94,red,green,blue,1F);
    }
    private static Vec3 cellCenter(BattleSnapshot snapshot, BattleCell cell) {
        return new Vec3(snapshot.arenaOriginX()+cell.x()+.5,cell.floorY()+1.04,snapshot.arenaOriginZ()+cell.z()+.5);
    }
    private static void addLine(PoseStack pose, VertexConsumer consumer, Vec3 from, Vec3 to,
                                float red, float green, float blue, float alpha) {
        Vec3 normal=to.subtract(from).normalize();
        consumer.addVertex(pose.last().pose(),(float)from.x,(float)from.y,(float)from.z).setColor(red,green,blue,alpha)
                .setNormal(pose.last(),(float)normal.x,(float)normal.y,(float)normal.z);
        consumer.addVertex(pose.last().pose(),(float)to.x,(float)to.y,(float)to.z).setColor(red,green,blue,alpha)
                .setNormal(pose.last(),(float)normal.x,(float)normal.y,(float)normal.z);
    }

    private static void renderFloatingNumbers(RenderLevelStageEvent event, Minecraft minecraft) {
        if (BattleClient.floatingNumbers().isEmpty()) return;
        Vec3 camera = event.getCamera().getPosition();
        for (BattleClient.FloatingNumber number : BattleClient.floatingNumbers()) {
            float life = 1.0F - number.age() / 35.0F;
            double rise = number.age() * .025;
            PoseStack pose = event.getPoseStack(); pose.pushPose();
            Vec3 tracked = BattleClient.presentationPosition(number.targetId(), event.getPartialTick().getGameTimeDeltaPartialTick(true));
            Vec3 position = tracked == null ? number.position() : tracked.add(0, 2.2, 0);
            pose.translate(position.x - camera.x, position.y + rise - camera.y, position.z - camera.z);
            pose.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
            float scale = (.026F + (1.0F - Math.min(1.0F, number.age() / 6.0F)) * .012F)
                    * FLOATING_NUMBER_SCALE_MULTIPLIER;
            pose.scale(-scale, -scale, scale);
            Component text = Component.literal((number.healing() ? "+" : "-") + Math.max(1, Mth.ceil(number.amount())));
            float x = -minecraft.font.width(text) / 2.0F;
            Matrix4f matrix = pose.last().pose();
            minecraft.font.drawInBatch(text, x, 0, 0xFFFFFF, false, matrix, minecraft.renderBuffers().bufferSource(),
                    Font.DisplayMode.SEE_THROUGH, ((int) (life * 170) << 24), 0xF000F0);
            minecraft.font.drawInBatch(text, x, 0, number.healing() ? 0xFF55FF77 : 0xFFFF5555, false, matrix, minecraft.renderBuffers().bufferSource(),
                    Font.DisplayMode.NORMAL, 0, 0xF000F0);
            minecraft.renderBuffers().bufferSource().endBatch();
            pose.popPose();
        }
    }

    private static boolean inSkillRange(BattleCell center, BattleCell cell, BattleSnapshot.CardView card, int radius) {
        if (card == null) return center.distanceTo(cell) <= radius;
        return card.reaches(center, cell);
    }

    private static void addCell(PoseStack pose, VertexConsumer consumer, BattleSnapshot snapshot, BattleCell cell,
                                float red, float green, float blue, float alpha, double height) {
        double x = snapshot.arenaOriginX() + cell.x(), y = cell.floorY() + 1.006, z = snapshot.arenaOriginZ() + cell.z();
        LevelRenderer.addChainedFilledBoxVertices(pose, consumer, x + .04, y, z + .04, x + .96, y + height, z + .96,
                red, green, blue, alpha);
    }
}
