package de.horizon.screen.render;

/**
 * Marker interface: screens that implement this are rendered at the fine Hi-DPI GUI scale managed by
 * {@link HiDpi}. {@code HiDpi.sync()} reads {@code mc.screen instanceof HiDpiScreen} every tick and
 * on every screen's {@code init()} to keep the scale in sync without fragile enter/exit pairing.
 */
public interface HiDpiScreen {}
