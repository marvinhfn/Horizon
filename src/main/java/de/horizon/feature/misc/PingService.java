package de.horizon.feature.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.util.debugchart.LocalSampleLogger;

public final class PingService {
    private volatile int currentPing = -1;
    private volatile int averagePing = -1;

    public void tick(Minecraft client) {
        if (client == null || client.getConnection() == null || client.player == null) {
            currentPing = -1;
            averagePing = -1;
            return;
        }

        LocalSampleLogger pingLog = client.getDebugOverlay().getPingLogger();
        int sampleSize = Math.min(pingLog.size(), 20);
        if (sampleSize <= 0) {
            currentPing = -1;
            averagePing = -1;
            return;
        }

        currentPing = (int) Math.max(0L, pingLog.get(0));

        long total = 0L;
        for (int index = 0; index < sampleSize; index++) {
            total += pingLog.get(index);
        }
        averagePing = (int) Math.max(0L, total / sampleSize);
    }

    public int getPing(Minecraft client) {
        return averagePing >= 0 ? averagePing : currentPing;
    }
}
