import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.Random;

public class GenerateCherryBranch {
    static final int W = 128, H = 512;

    public static void main(String[] args) throws Exception {
        BufferedImage wood = img();
        BufferedImage petals = img();
        drawWood(gfx(wood));
        drawPetals(gfx(petals));
        File dir = new File("src/main/resources/assets/horizon/textures/gui");
        ImageIO.write(wood, "png", new File(dir, "cherry_branch_wood.png"));
        ImageIO.write(petals, "png", new File(dir, "cherry_branch_petals.png"));
        System.out.println("wrote cherry_branch_wood.png + cherry_branch_petals.png");
    }

    static BufferedImage img() { return new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB); }
    static Graphics2D gfx(BufferedImage b) {
        Graphics2D g = b.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g;
    }

    // Main branch path hugging the right edge, with a couple of offshoots.
    static Path2D main() {
        Path2D p = new Path2D.Float();
        p.moveTo(W - 6, H);
        p.curveTo(W - 20, H - 120, W - 40, H - 220, W - 30, H - 340);
        p.curveTo(W - 24, H - 420, W - 50, H - 470, W - 70, H - 505);
        return p;
    }
    static Path2D offshoot(float x, float y, float dx, float dy) {
        Path2D p = new Path2D.Float();
        p.moveTo(x, y);
        p.curveTo(x + dx * 0.4f, y - 10, x + dx * 0.8f, y + dy * 0.5f, x + dx, y + dy);
        return p;
    }

    static void drawWood(Graphics2D g) {
        g.setColor(new Color(99, 63, 43)); // warm brown
        g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(main());
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(offshoot(W - 34, H - 230, -40, -18));
        g.draw(offshoot(W - 30, H - 330, -46, 6));
        g.draw(offshoot(W - 50, H - 450, -34, -16));
        g.dispose();
    }

    static void drawPetals(Graphics2D g) {
        Random rnd = new Random(7);
        // blossom clusters near branch tips/offshoots
        float[][] spots = {
            {W - 70, H - 500}, {W - 84, H - 466}, {W - 40, H - 250}, {W - 74, H - 236},
            {W - 30, H - 330}, {W - 76, H - 324}, {W - 84, H - 460}, {W - 60, H - 150},
            {W - 44, H - 120}, {W - 90, H - 380},
        };
        for (float[] s : spots) blossom(g, s[0], s[1], 13f + rnd.nextFloat() * 4f, rnd);
        g.dispose();
    }

    // 5 rounded petals + center, all white (alpha = coverage -> tintable).
    static void blossom(Graphics2D g, float cx, float cy, float r, Random rnd) {
        g.setColor(new Color(255, 255, 255, 235));
        for (int i = 0; i < 5; i++) {
            double a = Math.toRadians(72 * i + rnd.nextInt(10));
            float px = cx + (float) Math.cos(a) * r * 0.6f;
            float py = cy + (float) Math.sin(a) * r * 0.6f;
            g.fill(new Ellipse2D.Float(px - r * 0.5f, py - r * 0.5f, r, r));
        }
        g.setColor(new Color(255, 255, 255, 255));
        g.fill(new Ellipse2D.Float(cx - r * 0.33f, cy - r * 0.33f, r * 0.66f, r * 0.66f));
    }
}
