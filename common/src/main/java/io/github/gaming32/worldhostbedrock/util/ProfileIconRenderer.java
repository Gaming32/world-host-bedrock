package io.github.gaming32.worldhostbedrock.util;

import io.github.gaming32.worldhost.toast.IconRenderer;
import io.github.gaming32.worldhostbedrock.WorldHostBedrock;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ProfileIconRenderer implements IconRenderer {
    private final IconRenderer fallback;
    private final CompletableFuture<ResourceLocation> future;

    public ProfileIconRenderer(IconRenderer fallback, CompletableFuture<ResourceLocation> future) {
        this.fallback = fallback;
        this.future = future.exceptionally(t -> {
            WorldHostBedrock.LOGGER.error("Failed to get profile icon. Falling back to {}.", fallback, t);
            return null;
        });
    }

    @Override
    public void draw(@NotNull GuiGraphics graphics, int x, int y, int width, int height) {
        final ResourceLocation texture = future.getNow(null);
        if (texture != null) {
            graphics.blit(texture, x, y, width, height, 0, 0, 1080, 1080, 1080, 1080);
        } else {
            fallback.draw(graphics, x, y, width, height);
        }
    }
}
