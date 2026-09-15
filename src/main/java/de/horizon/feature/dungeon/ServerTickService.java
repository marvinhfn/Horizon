package de.horizon.feature.dungeon;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Monotonic count of real <b>server</b> ticks. Incremented once per Hypixel per-tick ping
 * packet (see {@code ClientCommonPacketListenerImplMixin}), so it advances at the server's
 * true tick rate and slows exactly when the server lags — unlike {@code level.getGameTime()},
 * which the client advances at 20/s regardless. Boss timers anchor on this so their countdown
 * lines up with the real server-side damage tick.
 *
 * <p>{@code onServerTick()} may be called off the client thread (netty), so the counter is an
 * {@link AtomicLong}; readers are on the client thread.
 */
public final class ServerTickService {
    private final AtomicLong serverTick = new AtomicLong(0L);

    /** Called once per received per-tick ping packet. */
    public void onServerTick() { serverTick.incrementAndGet(); }

    /** Current monotonic server-tick count. */
    public long getServerTick() { return serverTick.get(); }

    /** Reset on world change / disconnect. */
    public void reset() { serverTick.set(0L); }
}
