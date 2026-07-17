package de.horizon.render;

/**
 * Shared state for depth-tested entity glow rendering.
 * When {@code forceOutlineDepthTest} is true, the outline pipeline's
 * depth test disable is overridden to enable depth testing instead,
 * making entity glow respect the main scene's depth buffer.
 */
public final class HorizonGlowState {
    public static volatile boolean forceOutlineDepthTest;

    private HorizonGlowState() {}
}
