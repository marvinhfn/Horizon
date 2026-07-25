package de.horizon.feature.dungeon.puzzle;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.List;

/** Shared world-space rendering helpers for dungeon solvers. */
public final class DungeonRenderUtil {

    public static final float DEFAULT_LINE_WIDTH = 2.5f;

    // A filled-box pipeline with the depth test disabled (CompareOp.ALWAYS_PASS)
    // so translucent fills are visible through walls, mirroring the through-wall
    // line rendering. The vanilla debugFilledBox pipeline is depth-tested.
    private static final RenderPipeline FILLED_NO_DEPTH_PIPELINE = RenderPipelines.register(RenderPipeline
        .builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("horizon", "pipeline/filled_no_depth"))
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .build());
    private static final RenderType FILLED_NO_DEPTH = RenderType.create(
        "horizon_filled_no_depth",
        RenderSetup.builder(FILLED_NO_DEPTH_PIPELINE).createRenderSetup());

    private DungeonRenderUtil() {}

    /** One box for {@link #drawBoxesBatched}: its bounds and fill/outline colours. */
    public record BoxSpec(AABB box, int fillArgb, int outlineArgb) {}

    /**
     * Draws many boxes in just two draw batches (one fill, one outline) instead of
     * two per box. Each immediate {@code endBatch} opens its own GPU render pass, so
     * batching is essential when rendering more than a handful of boxes per frame.
     */
    public static void drawBoxesBatched(LevelRenderContext ctx, List<BoxSpec> boxes, boolean noDepth, float lineWidth) {
        if (boxes.isEmpty()) return;
        if (ctx.levelState() == null || ctx.levelState().cameraRenderState == null || ctx.bufferSource() == null) return;
        PoseStack.Pose pose = ctx.poseStack().last();
        Vec3 cam = ctx.levelState().cameraRenderState.pos;
        MultiBufferSource.BufferSource buf = ctx.bufferSource();

        if (noDepth) GlStateManager._depthFunc(GL11.GL_ALWAYS);

        var fillType = noDepth ? FILLED_NO_DEPTH : RenderTypes.debugFilledBox();
        VertexConsumer fill = buf.getBuffer(fillType);
        for (BoxSpec spec : boxes) fillBox(pose, fill, spec.box(), cam, spec.fillArgb());
        buf.endBatch(fillType);

        var lineType = noDepth ? RenderTypes.lines() : RenderTypes.linesTranslucent();
        VertexConsumer line = buf.getBuffer(lineType);
        for (BoxSpec spec : boxes) outlineBox(pose, line, spec.box(), cam, spec.outlineArgb(), lineWidth);
        buf.endBatch(lineType);

        if (noDepth) GlStateManager._depthFunc(GL11.GL_LEQUAL);
    }

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

    /**
     * Builds the model matrix for a billboarded world-space label using a FRESH
     * {@link Matrix4f} (not the render-context pose stack). The context pose already carries the
     * camera/view transform, so reusing it double-transforms the text off-screen — the reason world
     * text was invisible while boxes rendered. Here the matrix is model-only and the text render
     * type applies the view/projection itself.
     */
    // Permanent, never-changing facings for static wall text. Derived from MC's camera math:
    // Camera.rotation() == rotationYXZ(π − camYaw·π/180, …), so the frozen euler-yaw for a camera
    // looking a given cardinal direction is π − thatYaw. The text sits "straight" for a viewer at
    // exactly that camera yaw and NEVER re-orients, regardless of where the player later stands/looks.
    public static float yawForCameraFacing(float cameraYawDeg) {
        return (float) (Math.PI - Math.toRadians(cameraYawDeg));
    }
    /** Straight for a viewer whose camera faces SOUTH (yaw 0) — the I4 emerald wall (viewed from −z). */
    public static final float FACE_SOUTH = (float) Math.PI;              // yawForCameraFacing(0)
    /** Straight for a viewer whose camera faces WEST (yaw 90) — the arrow-align wall (viewed from +x). */
    public static final float FACE_WEST = (float) (Math.PI / 2.0);       // yawForCameraFacing(90)

    private static Matrix4f worldTextMatrix(double x, double y, double z) {
        return worldTextMatrix(x, y, z, 1.0f, null);
    }

    /**
     * Builds the world-text model matrix. {@code sizeMul} scales the glyphs (1.0 = the old 0.025
     * baseline). {@code fixedYawRad}: {@code null} = billboard (faces the camera). Otherwise the text
     * is fully STATIC at that fixed yaw (no pitch/roll, no tracking) — it never changes orientation.
     */
    private static Matrix4f worldTextMatrix(double x, double y, double z, float sizeMul, Float fixedYawRad) {
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();
        Matrix4f m = new Matrix4f();
        m.translate((float)(x - camPos.x), (float)(y - camPos.y), (float)(z - camPos.z));
        if (fixedYawRad != null) {
            m.rotate(new org.joml.Quaternionf().rotationYXZ(fixedYawRad, 0f, 0f));
        } else {
            m.rotate(camera.rotation());
        }
        float s = 0.025f * sizeMul;
        m.scale(s, -s, s);
        return m;
    }

    /** Renders a string in 3D world space, billboard-facing the camera. */
    public static void drawString(LevelRenderContext ctx, String text, double x, double y, double z) {
        drawString(ctx, text, x, y, z, 0xFFFFFFFF, 1.0f, null);
    }

    /** Renders a string in 3D world space with explicit colour/size; {@code fixedYawRad} non-null = static wall text. */
    public static void drawString(LevelRenderContext ctx, String text, double x, double y, double z, int color, float sizeMul, Float fixedYawRad) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        Font font = mc.font;
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        font.drawInBatch(text, -font.width(text) / 2f, 0, color | 0xFF000000, true, worldTextMatrix(x, y, z, sizeMul, fixedYawRad), buf,
            Font.DisplayMode.SEE_THROUGH, 0x40000000, 0xF000F0);
        buf.endBatch();
    }

    /** One label for {@link #drawStringsBatched}. */
    public record StringSpec(String text, double x, double y, double z) {}

    /** One coloured label for {@link #drawColoredStringsBatched}. */
    public record ColoredStringSpec(String text, double x, double y, double z, int color) {}

    /** Like {@link #drawStringsBatched} but each label carries its own ARGB colour (billboard). */
    public static void drawColoredStringsBatched(LevelRenderContext ctx, List<ColoredStringSpec> specs) {
        drawColoredStringsBatched(ctx, specs, null);
    }

    /**
     * Coloured batched labels. {@code fixedYawRad} null = billboard; non-null = fully STATIC wall
     * text at that fixed yaw — used for the arrow-align numbers on the wall.
     */
    public static void drawColoredStringsBatched(LevelRenderContext ctx, List<ColoredStringSpec> specs, Float fixedYawRad) {
        if (specs.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        Font font = mc.font;
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        for (ColoredStringSpec s : specs) {
            font.drawInBatch(s.text(), -font.width(s.text()) / 2f, 0, s.color() | 0xFF000000, true,
                worldTextMatrix(s.x(), s.y(), s.z(), 1.0f, fixedYawRad), buf, Font.DisplayMode.SEE_THROUGH, 0x40000000, 0xF000F0);
        }
        buf.endBatch(); // single flush for all labels
    }

    /** Renders several billboarded labels with a single buffer flush (endBatch). */
    public static void drawStringsBatched(LevelRenderContext ctx, List<StringSpec> specs) {
        if (specs.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        Font font = mc.font;
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        for (StringSpec s : specs) {
            font.drawInBatch(s.text(), -font.width(s.text()) / 2f, 0, 0xFFFFFFFF, true,
                worldTextMatrix(s.x(), s.y(), s.z()), buf, Font.DisplayMode.SEE_THROUGH, 0x40000000, 0xF000F0);
        }
        buf.endBatch(); // single flush for all labels
    }
}
