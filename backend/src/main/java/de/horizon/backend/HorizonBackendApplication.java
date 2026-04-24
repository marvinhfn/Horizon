package de.horizon.backend;

import com.google.gson.JsonObject;
import de.horizon.backend.auth.DevTokenService;
import de.horizon.backend.config.BackendConfig;
import de.horizon.backend.hypixel.HypixelProfileService;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

public final class HorizonBackendApplication {
    private HorizonBackendApplication() {
    }

    public static void main(String[] args) {
        BackendConfig config = BackendConfig.fromEnvironment();
        DevTokenService tokenService = new DevTokenService(config.devAuthSecret());
        HypixelProfileService hypixelProfileService = new HypixelProfileService(config);

        Javalin app = Javalin.create(javalinConfig -> javalinConfig.showJavalinBanner = false);

        app.get("/health", context -> {
            JsonObject response = new JsonObject();
            response.addProperty("status", "ok");
            response.addProperty("service", "horizon-backend");
            response.addProperty("appName", config.hypixelAppName());
            response.addProperty("hypixelConfigured", !config.hypixelApiKey().isBlank());
            context.json(response.toString());
        });

        app.post("/v1/auth/token", context -> {
            JsonObject request = context.bodyAsClass(JsonObject.class);
            String minecraftUuid = stringValue(request, "minecraftUuid");
            String minecraftUsername = stringValue(request, "minecraftUsername");
            String audience = stringValue(request, "audience");
            String proofValue = stringValue(request, "proofValue");

            JsonObject response = new JsonObject();
            response.addProperty("accessToken", tokenService.issueToken(minecraftUuid, minecraftUsername, audience, proofValue));
            response.addProperty("expiresAt", tokenService.expiresAtEpochSeconds());
            response.addProperty("tokenType", "Bearer");
            response.addProperty("mode", "development");
            context.json(response.toString());
        });

        app.get("/v1/skyblock/profile", context -> {
            try {
                String player = context.queryParam("player");
                context.json(hypixelProfileService.loadProfileSummary(player == null ? "" : player).toString());
            } catch (Exception exception) {
                context.status(HttpStatus.BAD_GATEWAY);
                JsonObject response = new JsonObject();
                response.addProperty("error", "Failed to load Hypixel profile.");
                response.addProperty("detail", exception.getMessage());
                context.json(response.toString());
            }
        });

        app.start(config.port());
    }

    private static String stringValue(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
            ? object.get(key).getAsString()
            : "";
    }
}
