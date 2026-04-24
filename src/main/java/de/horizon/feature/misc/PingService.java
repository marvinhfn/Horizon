package de.horizon.feature.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.profiler.MultiValueDebugSampleLogImpl;

public final class PingService {
    private volatile int currentPing = -1;
    private volatile int averagePing = -1;

    public void tick(MinecraftClient client) {
        if (client == null || client.getNetworkHandler() == null || client.player == null) {
            currentPing = -1;
            averagePing = -1;
            return;
        }

        MultiValueDebugSampleLogImpl pingLog = client.getDebugHud().getPingLog();
        int sampleSize = Math.min(pingLog.getLength(), 20);
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

    public int getPing(MinecraftClient client) {
        return averagePing >= 0 ? averagePing : currentPing;
    }
}
