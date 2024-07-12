package io.github.gaming32.worldhostbedrock;

import com.google.common.hash.Hashing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class BedrockIconManager {
    private static final ResourceLocation DEFAULT_TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/dirt.png");

    private final Path cacheRoot;

    private final Map<String, CompletableFuture<ResourceLocation>> textures = new HashMap<>();

    public BedrockIconManager(Path cacheRoot) {
        this.cacheRoot = cacheRoot;
    }

    public CompletableFuture<ResourceLocation> getOrLoad(URI iconUri) {
        return textures.computeIfAbsent(getIconId(iconUri), iconId -> registerTexture(iconId, iconUri));
    }

    private CompletableFuture<ResourceLocation> registerTexture(String iconId, URI iconUri) {
        final String hashedId = Hashing.murmur3_128().hashUnencodedChars(iconId).toString();
        final ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(
            WorldHostBedrock.MOD_ID, "icons/" + hashedId
        );
        final Path cachePath = cacheRoot.resolve(hashedId + getIconExtension(iconUri));
        final CompletableFuture<ResourceLocation> future = new CompletableFuture<>();
        final HttpTexture result = new HttpTexture(
            cachePath.toFile(),
            iconUri.toString(),
            DEFAULT_TEXTURE,
            false,
            () -> future.complete(textureLocation)
        );
        Minecraft.getInstance().getTextureManager().register(textureLocation, result);
        return future;
    }

    private static String getIconId(URI iconUri) {
        for (final NameValuePair pair : URLEncodedUtils.parse(iconUri, StandardCharsets.UTF_8)) {
            if (pair.getName().equals("url")) {
                return pair.getValue();
            }
        }
        return iconUri.toString();
    }

    private static String getIconExtension(URI iconUri) {
        for (final NameValuePair pair : URLEncodedUtils.parse(iconUri, StandardCharsets.UTF_8)) {
            if (pair.getName().equals("format")) {
                return '.' + pair.getValue();
            }
        }
        return "";
    }
}
