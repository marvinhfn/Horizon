package de.horizon.feature.misc;

public final class TpsTracker {
    private volatile float lastKnownTps = 20.0F;
    private volatile long lastWorldTimePacketMillis = 0L;

    public float getLastKnownTps() {
        return lastKnownTps;
    }

    public void update(float tickRate) {
        lastKnownTps = tickRate;
    }

    public void onWorldTimePacket() {
        long now = System.currentTimeMillis();
        if (lastWorldTimePacketMillis > 0L) {
            long delta = now - lastWorldTimePacketMillis;
            if (delta > 0L) {
                lastKnownTps = Math.max(0.0F, Math.min(20.0F, (float) (20000.0D / delta)));
            }
        }
        lastWorldTimePacketMillis = now;
    }
}
