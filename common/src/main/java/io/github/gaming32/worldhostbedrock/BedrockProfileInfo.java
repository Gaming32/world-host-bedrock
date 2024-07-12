package io.github.gaming32.worldhostbedrock;

import io.github.gaming32.worldhost.plugin.ProfileInfo;
import io.github.gaming32.worldhost.toast.IconRenderer;
import io.github.gaming32.worldhostbedrock.util.ProfileIconRenderer;
import io.github.gaming32.worldhostbedrock.xbox.models.ProfileUser;
import net.minecraft.client.resources.DefaultPlayerSkin;

import java.net.URI;
import java.util.Objects;

public final class BedrockProfileInfo {
    private BedrockProfileInfo() {
    }

    public static ProfileInfo create(String fallbackName, ProfileUser user) {
        IconRenderer icon = IconRenderer.createSkinIconRenderer(DefaultPlayerSkin.get(user.id().toUuid()).texture());
        final URI iconUri = user.profilePicture();
        if (iconUri != null) {
            icon = new ProfileIconRenderer(
                icon, WorldHostBedrock.getInstance().getBedrockIconManager().getOrLoad(iconUri)
            );
        }

        return new ProfileInfo.Basic(Objects.requireNonNullElse(user.displayName(), fallbackName), icon);
    }
}
