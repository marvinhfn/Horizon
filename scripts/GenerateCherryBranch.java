import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.Random;

/**
 * Generates the two-layer cherry-blossom branch assets (plus horizontally-flipped variants for the
 * mirrored left side). The petals layer is a grayscale tint-mask (soft radial shading baked in so the
 * theme tint gets depth); the wood layer is a fixed warm brown with light bark shading.
 *
 * Run: nix-shell -p temurin-bin-25 --run "java scripts/GenerateCherryBranch.java"
 */
public class GenerateCherryBranch {
    static final int W = 256, H = 1024; // hi-res; drawn downscaled in-game

    public static void main(String[] args) throws Exception {
        BufferedImage wood = img(), petals = img();
        drawWood(gfx(wood));
        drawPetals(gfx(petals));
        File dir = new File("src/main/resources/assets/horizon/textures/gui");
        write(wood, dir, "cherry_branch_wood.png");
        write(petals, dir, "cherry_branch_petals.png");
        write(flip(wood), dir, "cherry_branch_wood_l.png");
        write(flip(petals), dir, "cherry_branch_petals_l.png");
        System.out.println("wrote cherry_branch_{wood,petals}{,_l}.png");
    }

    static void write(BufferedImage b, File dir, String name) throws Exception {
        ImageIO.write(b, "png", new File(dir, name));
    }

    static BufferedImage img() { return new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB); }

    static Graphics2D gfx(BufferedImage b) {
        Graphics2D g = b.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g;
    }

    static BufferedImage flip(BufferedImage src) {
        BufferedImage out = img();
        Graphics2D g = out.createGraphics();
        g.drawImage(src, W, 0, -W, H, null); // mirror horizontally
        g.dispose();
        return out;
    }

    // ── Branch ────────────────────────────────────────────────────────────────────
    static final Color BARK = new Color(104, 67, 46);
    static final Color BARK_DARK = new Color(72, 45, 30);
    static final Color BARK_LIGHT = new Color(132, 92, 64);

    static Path2D mainBranch() {
        Path2D p = new Path2D.Float();
        p.moveTo(W - 14, H);
        p.curveTo(W - 46, H - 240, W - 92, H - 440, W - 64, H - 680);
        p.curveTo(W - 48, H - 840, W - 104, H - 940, W - 150, H - 1010);
        return p;
    }
    static Path2D twig(float x, float y, float dx, float dy) {
        Path2D p = new Path2D.Float();
        p.moveTo(x, y);
        p.curveTo(x + dx * 0.35f, y - 18, x + dx * 0.8f, y + dy * 0.5f, x + dx, y + dy);
        return p;
    }

    static final float[][] TWIGS = {
        {W - 72, H - 470, -84, -40}, {W - 64, H - 660, -96, 14},
        {W - 104, H - 900, -72, -36}, {W - 90, H - 300, -70, -46}, {W - 120, H - 980, -56, 10},
    };

    static void drawWood(Graphics2D g) {
        Path2D main = mainBranch();
        // layered strokes: dark base → mid → light core highlight (fake round bark)
        strokePath(g, main, 20f, BARK_DARK);
        strokePath(g, main, 15f, BARK);
        strokePath(g, main, 6f, BARK_LIGHT);
        for (float[] t : TWIGS) {
            Path2D tw = twig(t[0], t[1], t[2], t[3]);
            strokePath(g, tw, 10f, BARK_DARK);
            strokePath(g, tw, 7f, BARK);
            strokePath(g, tw, 2.5f, BARK_LIGHT);
        }
        g.dispose();
    }

    static void strokePath(Graphics2D g, Shape s, float w, Color c) {
        g.setColor(c);
        g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
    }

    // ── Blossoms (grayscale tint-mask with soft shading) ───────────────────────────
    static void drawPetals(Graphics2D g) {
        Random r = new Random(11);
        // clusters anchored to twig tips + along the main branch
        float[][] spots = {
            {W - 150, H - 1005, 30}, {W - 176, H - 975, 24}, {W - 120, H - 980, 22},
            {W - 104, H - 905, 28}, {W - 176, H - 940, 20},
            {W - 64, H - 660, 30}, {W - 160, H - 648, 26}, {W - 110, H - 656, 20},
            {W - 72, H - 470, 28}, {W - 156, H - 508, 26}, {W - 120, H - 486, 20},
            {W - 90, H - 300, 28}, {W - 160, H - 338, 24},
            {W - 40, H - 150, 30}, {W - 70, H - 120, 22},
        };
        for (float[] s : spots) cluster(g, s[0], s[1], s[2], r);
        // a few drifting petals for life
        for (int i = 0; i < 8; i++) {
            float x = 20 + r.nextFloat() * (W - 60), y = 60 + r.nextFloat() * (H - 120);
            singlePetal(g, x, y, 9 + r.nextFloat() * 4, r.nextFloat() * 6.28f, 0.75f);
        }
        g.dispose();
    }

    // a cluster = 2-3 blossoms + a bud
    static void cluster(Graphics2D g, float cx, float cy, float rad, Random r) {
        int n = 2 + r.nextInt(2);
        for (int i = 0; i < n; i++) {
            float a = r.nextFloat() * 6.28f, d = r.nextFloat() * rad * 0.5f;
            blossom(g, cx + (float) Math.cos(a) * d, cy + (float) Math.sin(a) * d, rad * (0.7f + r.nextFloat() * 0.4f), r);
        }
        bud(g, cx + rad * 0.7f, cy + rad * 0.3f, rad * 0.4f);
    }

    // 5 shaded, notched petals + stamen dots
    static void blossom(Graphics2D g, float cx, float cy, float r, Random rnd) {
        float rot = rnd.nextFloat() * 6.28f;
        for (int i = 0; i < 5; i++) singlePetal(g, cx, cy, r, rot + (float) (i * Math.PI * 2 / 5), 1f);
        // center stamens: a soft disc + tiny dots (bright = highlight after tint)
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), Math.max(1.5f, r * 0.34f),
            new float[]{0f, 1f}, new Color[]{new Color(255, 255, 255, 255), new Color(210, 210, 210, 140)}));
        g.fill(new Ellipse2D.Float(cx - r * 0.34f, cy - r * 0.34f, r * 0.68f, r * 0.68f));
        g.setColor(new Color(255, 255, 255, 230));
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3 + rot;
            float px = cx + (float) Math.cos(a) * r * 0.26f, py = cy + (float) Math.sin(a) * r * 0.26f;
            g.fill(new Ellipse2D.Float(px - 1.3f, py - 1.3f, 2.6f, 2.6f));
        }
    }

    // one petal: teardrop with a small outer notch, radially shaded (bright base → soft tip)
    static void singlePetal(Graphics2D g, float cx, float cy, float r, float ang, float alphaMul) {
        AffineTransform old = g.getTransform();
        g.translate(cx, cy);
        g.rotate(ang);
        float w = r * 0.86f, len = r * 1.18f;
        Path2D petal = new Path2D.Float();
        petal.moveTo(0, 0);
        petal.curveTo(-w * 0.55f, -len * 0.30f, -w * 0.52f, -len * 0.78f, -w * 0.16f, -len * 0.96f);
        petal.curveTo(-w * 0.07f, -len * 1.02f, 0, -len * 0.92f, 0, -len * 0.86f); // left half + inner notch dip
        petal.curveTo(0, -len * 0.92f, w * 0.07f, -len * 1.02f, w * 0.16f, -len * 0.96f);
        petal.curveTo(w * 0.52f, -len * 0.78f, w * 0.55f, -len * 0.30f, 0, 0);
        petal.closePath();
        int a = (int) (235 * alphaMul);
        g.setPaint(new RadialGradientPaint(new Point2D.Float(0, -len * 0.5f), len * 0.85f,
            new float[]{0f, 0.65f, 1f},
            new Color[]{new Color(255, 255, 255, a), new Color(244, 244, 244, a), new Color(214, 214, 214, (int) (a * 0.82f))}));
        g.fill(petal);
        g.setTransform(old);
    }

    // a closed bud: small oval + tiny calyx notch
    static void bud(Graphics2D g, float cx, float cy, float r) {
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx - r * 0.2f, cy - r * 0.2f), r * 1.3f,
            new float[]{0f, 1f}, new Color[]{new Color(255, 255, 255, 230), new Color(205, 205, 205, 170)}));
        g.fill(new Ellipse2D.Float(cx - r, cy - r * 1.2f, r * 2f, r * 2.4f));
    }
}
