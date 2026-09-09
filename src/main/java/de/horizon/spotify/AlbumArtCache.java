package de.horizon.spotify;

import com.mojang.blaze3d.platform.NativeImage;
import de.horizon.HorizonMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Loads and caches the current Spotify track's album cover as a GUI texture. Downloading + decoding
 * happens off-thread; the GPU upload happens on the render thread (via {@link #ensure(String)}), which
 * must be called every frame from a render pass. A single shared instance is used by both the music HUD
 * and the inventory overlay so the cover is only fetched once.
 */
public final class AlbumArtCache {
    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath("horizon", "spotify_album_art");

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .build();

    private volatile NativeImage pendingImage;
    private volatile String pendingUrl = "";
    private volatile String requestingUrl = "";
    private String loadedUrl = "";
    private boolean registered;
    private int width;
    private int height;

    public Identifier textureId() {
        return TEXTURE_ID;
    }

    public int width() {
        return width > 0 ? width : 1;
    }

    public int height() {
        return height > 0 ? height : 1;
    }

    public boolean isReady(String url) {
        return registered && !loadedUrl.isBlank() && loadedUrl.equals(url == null ? "" : url);
    }

    /**
     * Ensures the album art for {@code url} is loaded. Uploads a freshly decoded image to the GPU
     * (render thread only), and kicks off an async download when the URL is new. A blank or unchanged
     * URL is a no-op.
     */
    public void ensure(String url) {
        if (url == null) {
            url = "";
        }

        NativeImage ready = pendingImage;
        if (ready != null && pendingUrl.equals(url)) {
            pendingImage = null;
            try {
                width = ready.getWidth();
                height = ready.getHeight();
                Minecraft mc = Minecraft.getInstance();
                mc.getTextureManager().release(TEXTURE_ID);
                mc.getTextureManager().register(TEXTURE_ID, new DynamicTexture(() -> "horizon_album_art", ready));
                loadedUrl = url;
                registered = true;
            } catch (Exception exception) {
                ready.close();
                HorizonMod.LOGGER.debug("Album art upload failed", exception);
            }
            return;
        }

        if (url.isBlank() || url.equals(loadedUrl) || url.equals(requestingUrl)) {
            return;
        }

        requestingUrl = url;
        final String target = url;
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(target))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "HorizonMod/1.0")
                    .GET()
                    .build();
                HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() >= 400 || response.body() == null || response.body().length == 0) {
                    return;
                }
                // Spotify serves album covers as JPEG, but NativeImage.read only accepts PNG. Decode the
                // JPEG via ImageIO and re-encode it as PNG so NativeImage can load it.
                byte[] png = toPng(response.body());
                if (png == null) {
                    return;
                }
                pendingImage = NativeImage.read(png);
                pendingUrl = target;
            } catch (Exception exception) {
                HorizonMod.LOGGER.debug("Album art download failed", exception);
            }
        });
    }

    private static byte[] toPng(byte[] source) {
        try {
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(new ByteArrayInputStream(source));
            if (image == null) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (Exception exception) {
            HorizonMod.LOGGER.debug("Album art decode failed", exception);
            return null;
        }
    }
}
