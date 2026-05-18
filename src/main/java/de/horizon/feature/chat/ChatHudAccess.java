package de.horizon.feature.chat;

/**
 * Exposes a method to retrieve the plain text of a visible chat line
 * at a given screen position. Applied to ChatHud via ChatHudMixin.
 */
public interface ChatHudAccess {
    String horizon$getMessageTextAt(double mouseX, double mouseY, boolean fullEntry);
}
