package de.horizon.backend.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record BackendConfig(
    int port,
    String baseUrl,
    String devAuthSecret,
    String hypixelApiKey,
    String hypixelAppName
) {
    public static BackendConfig fromEnvironment() {
        Map<String, String> envFile = readDotEnv();
        return new BackendConfig(
            intEnv(envFile, "HORIZON_PORT", 8787),
            stringEnv(envFile, "HORIZON_BASE_URL", "http://localhost:8787"),
            stringEnv(envFile, "HORIZON_DEV_AUTH_SECRET", "change-me"),
            stringEnv(envFile, "HYPIXEL_API_KEY", ""),
            stringEnv(envFile, "HYPIXEL_APP_NAME", "Horizon")
        );
    }

    private static String stringEnv(Map<String, String> envFile, String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = envFile.get(key);
        }
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static int intEnv(Map<String, String> envFile, String key, int fallback) {
        try {
            String value = System.getenv(key);
            if (value == null || value.isBlank()) {
                value = envFile.get(key);
            }
            return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static Map<String, String> readDotEnv() {
        Path path = Path.of(".env");
        if (!Files.exists(path)) {
            return Map.of();
        }

        Map<String, String> values = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                String trimmed = line == null ? "" : line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separatorIndex = trimmed.indexOf('=');
                if (separatorIndex <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, separatorIndex).trim();
                String value = trimmed.substring(separatorIndex + 1).trim();
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(key, value);
            }
        } catch (IOException ignored) {
            return Map.of();
        }
        return Map.copyOf(values);
    }
}
