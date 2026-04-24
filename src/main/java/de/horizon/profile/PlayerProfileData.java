package de.horizon.profile;

public record PlayerProfileData(
    String username,
    String uuid,
    String skyCryptUrl,
    String nameMcUrl
) {
}
