package de.horizon.config;

public final class HudPosition {
    private int x;
    private int y;
    private double scale = 1.0D;

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
}
