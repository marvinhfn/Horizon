package de.horizon.feature.dungeon.puzzle;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.List;

/** Shared world-space rendering helpers for dungeon solvers. */
public final class DungeonRenderUtil {

    public static final float DEFAULT_LINE_WIDTH = 2.5f;

    private DungeonRenderUtil() {}

    public static void drawBox(LevelRenderContext ctx, AABB box, int argbColor, int style, boolean noDepth) {
        drawBox(ctx, box, argbColor, style, noDepth, DEFAULT_LINE_WIDTH);
    }

    public static void drawBox(LevelRenderContext ctx, AABB box, int argbColor, int style, boolean noDepth, float lineWidth) {
        if (ctx.levelState() == null || ctx.levelState().cameraRenderState == null || ctx.bufferSource() == null) return;
        PoseStack.Pose pose = ctx.poseStack().last();
        Vec3 cam = ctx.levelState().cameraRenderState.pos;
        MultiBufferSource.BufferSource buf = ctx.bufferSource();

        if (noDepth) GlStateManager._depthFunc(GL11.GL_ALWAYS);

        if (style == 0 || style == 2) {
            var fillType = RenderTypes.debugFilledBox();
            fillBox(pose, buf.getBuffer(fillType), box, cam, argbColor);
            buf.endBatch(fillType);
        }
        if (style == 1 || style == 2) {
            // linesTranslucent() is depth-tested (hidden behind blocks)
            // lines() renders without depth test (visible through walls)
            var lineType = noDepth ? RenderTypes.lines() : RenderTypes.linesTranslucent();
            outlineBox(pose, buf.getBuffer(lineType), box, cam, argbColor, lineWidth);
            buf.endBatch(lineType);
        }

        if (noDepth) GlStateManager._depthFunc(GL11.GL_LEQUAL);
    }

    public static void drawLine(LevelRenderContext ctx, List<Vec3> points, int argbColor, boolean noDepth) {
        drawLine(ctx, points, argbColor, noDepth, DEFAULT_LINE_WIDTH);
    }

    public static void drawLine(LevelRenderContext ctx, List<Vec3> points, int argbColor, boolean noDepth, float lineWidth) {
        if (points.size() < 2) return;
        if (ctx.levelState() == null || ctx.levelState().cameraRenderState == null || ctx.bufferSource() == null) return;
        PoseStack.Pose pose = ctx.poseStack().last();
        Vec3 cam = ctx.levelState().cameraRenderState.pos;
        MultiBufferSource.BufferSource buf = ctx.bufferSource();

        int a = (argbColor >> 24) & 0xFF, r = (argbColor >> 16) & 0xFF, g = (argbColor >> 8) & 0xFF, b = argbColor & 0xFF;

        var lineType = noDepth ? RenderTypes.lines() : RenderTypes.linesTranslucent();
        VertexConsumer vc = buf.getBuffer(lineType);
        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 p1 = points.get(i), p2 = points.get(i + 1);
            float x1 = (float)(p1.x - cam.x), y1 = (float)(p1.y - cam.y), z1 = (float)(p1.z - cam.z);
            float x2 = (float)(p2.x - cam.x), y2 = (float)(p2.y - cam.y), z2 = (float)(p2.z - cam.z);
            float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
            float len = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (len < 1e-6f) continue;
            vc.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setNormal(pose, dx/len, dy/len, dz/len).setLineWidth(lineWidth);
            vc.addVertex(pose, x2, y2, z2).setColor(r, g, b, a).setNormal(pose, dx/len, dy/len, dz/len).setLineWidth(lineWidth);
        }
        buf.endBatch(lineType);
    }

    private static void fillBox(PoseStack.Pose pose, VertexConsumer vc, AABB box, Vec3 cam, int argb) {
        int a = (argb >> 24) & 0xFF, r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        float x0 = (float)(box.minX - cam.x), y0 = (float)(box.minY - cam.y), z0 = (float)(box.minZ - cam.z);
        float x1 = (float)(box.maxX - cam.x), y1 = (float)(box.maxY - cam.y), z1 = (float)(box.maxZ - cam.z);
        vc.addVertex(pose,x0,y0,z0).setColor(r,g,b,a); vc.addVertex(pose,x1,y0,z0).setColor(r,g,b,a);
        vc.addVertex(pose,x1,y0,z1).setColor(r,g,b,a); vc.addVertex(pose,x0,y0,z1).setColor(r,g,b,a);
        vc.addVertex(pose,x0,y1,z1).setColor(r,g,b,a); vc.addVertex(pose,x1,y1,z1).setColor(r,g,b,a);
        vc.addVertex(pose,x1,y1,z0).setColor(r,g,b,a); vc.addVertex(pose,x0,y1,z0).setColor(r,g,b,a);
        vc.addVertex(pose,x0,y1,z0).setColor(r,g,b,a); vc.addVertex(pose,x1,y1,z0).setColor(r,g,b,a);
        vc.addVertex(pose,x1,y0,z0).setColor(r,g,b,a); vc.addVertex(pose,x0,y0,z0).setColor(r,g,b,a);
        vc.addVertex(pose,x0,y0,z1).setColor(r,g,b,a); vc.addVertex(pose,x1,y0,z1).setColor(r,g,b,a);
        vc.addVertex(pose,x1,y1,z1).setColor(r,g,b,a); vc.addVertex(pose,x0,y1,z1).setColor(r,g,b,a);
        vc.addVertex(pose,x0,y0,z0).setColor(r,g,b,a); vc.addVertex(pose,x0,y0,z1).setColor(r,g,b,a);
        vc.addVertex(pose,x0,y1,z1).setColor(r,g,b,a); vc.addVertex(pose,x0,y1,z0).setColor(r,g,b,a);
        vc.addVertex(pose,x1,y1,z0).setColor(r,g,b,a); vc.addVertex(pose,x1,y1,z1).setColor(r,g,b,a);
        vc.addVertex(pose,x1,y0,z1).setColor(r,g,b,a); vc.addVertex(pose,x1,y0,z0).setColor(r,g,b,a);
    }

    private static void outlineBox(PoseStack.Pose pose, VertexConsumer vc, AABB box, Vec3 cam, int argb, float lineWidth) {
        int a = (argb >> 24) & 0xFF, r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        float x0 = (float)(box.minX - cam.x), y0 = (float)(box.minY - cam.y), z0 = (float)(box.minZ - cam.z);
        float x1 = (float)(box.maxX - cam.x), y1 = (float)(box.maxY - cam.y), z1 = (float)(box.maxZ - cam.z);
        addLine(pose,vc,r,g,b,a,lineWidth, x0,y0,z0, x1,y0,z0); addLine(pose,vc,r,g,b,a,lineWidth, x1,y0,z0, x1,y0,z1);
        addLine(pose,vc,r,g,b,a,lineWidth, x1,y0,z1, x0,y0,z1); addLine(pose,vc,r,g,b,a,lineWidth, x0,y0,z1, x0,y0,z0);
        addLine(pose,vc,r,g,b,a,lineWidth, x0,y1,z0, x1,y1,z0); addLine(pose,vc,r,g,b,a,lineWidth, x1,y1,z0, x1,y1,z1);
        addLine(pose,vc,r,g,b,a,lineWidth, x1,y1,z1, x0,y1,z1); addLine(pose,vc,r,g,b,a,lineWidth, x0,y1,z1, x0,y1,z0);
        addLine(pose,vc,r,g,b,a,lineWidth, x0,y0,z0, x0,y1,z0); addLine(pose,vc,r,g,b,a,lineWidth, x1,y0,z0, x1,y1,z0);
        addLine(pose,vc,r,g,b,a,lineWidth, x1,y0,z1, x1,y1,z1); addLine(pose,vc,r,g,b,a,lineWidth, x0,y0,z1, x0,y1,z1);
    }

    private static void addLine(PoseStack.Pose pose, VertexConsumer vc, int r, int g, int b, int a, float lineWidth,
                                 float x0, float y0, float z0, float x1, float y1, float z1) {
        float dx = x1-x0, dy = y1-y0, dz = z1-z0;
        float len = (float)Math.sqrt(dx*dx+dy*dy+dz*dz); if (len<1e-6f) len=1f;
        vc.addVertex(pose,x0,y0,z0).setColor(r,g,b,a).setNormal(pose,dx/len,dy/len,dz/len).setLineWidth(lineWidth);
        vc.addVertex(pose,x1,y1,z1).setColor(r,g,b,a).setNormal(pose,dx/len,dy/len,dz/len).setLineWidth(lineWidth);
    }

    /** Renders a string in 3D world space, billboard-facing the camera. */
    public static void drawString(LevelRenderContext ctx, String text, double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        Font font = mc.font;
        Vec3 cam = ctx.levelState().cameraRenderState.pos;
        // Use the global render buffers — the level context's bufferSource doesn't support text render types
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();

        var pose = ctx.poseStack();
        pose.pushPose();
        pose.translate((float)(x - cam.x), (float)(y - cam.y), (float)(z - cam.z));
        pose.mulPose(ctx.levelState().cameraRenderState.orientation);
        pose.scale(-0.025f, -0.025f, 0.025f);

        float halfWidth = font.width(text) / 2f;
        font.drawInBatch(text, -halfWidth, 0, 0xFFFFFFFF, true, pose.last().pose(), buf, Font.DisplayMode.SEE_THROUGH, 0x40000000, 0xF000F0);
        buf.endBatch();

        pose.popPose();
    }
}
