package de.horizon.config;

public final class HudPosition {
    private int x;
    private int y;
    private double scale = 1.0D;
    private double fx = -1;
    private double fy = -1;

    public HudPosition() {
    }

    public HudPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public HudPosition(int x, int y, double scale) {
        this.x = x;
        this.y = y;
        this.scale = scale;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public int resolveX(int guiW) { return fx < 0 ? x : (int) Math.round(fx * guiW); }
    public int resolveY(int guiH) { return fy < 0 ? y : (int) Math.round(fy * guiH); }

    public void setFromPixels(int px, int py, int guiW, int guiH) {
        this.x = px; this.y = py;
        this.fx = guiW > 0 ? (double) px / guiW : 0;
        this.fy = guiH > 0 ? (double) py / guiH : 0;
    }

    public void migrateIfNeeded(int guiW, int guiH) {
        if (fx < 0 && guiW > 0 && guiH > 0) { fx = (double) x / guiW; fy = (double) y / guiH; }
    }
}
