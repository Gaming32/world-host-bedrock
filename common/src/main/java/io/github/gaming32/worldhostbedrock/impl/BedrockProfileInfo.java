package io.github.gaming32.worldhostbedrock.impl;

import io.github.gaming32.worldhost.plugin.ProfileInfo;
import io.github.gaming32.worldhost.toast.IconRenderer;
import io.github.gaming32.worldhostbedrock.WorldHostBedrock;
import io.github.gaming32.worldhostbedrock.util.ProfileIconRenderer;
import io.github.gaming32.worldhostbedrock.util.XUID;
import io.github.gaming32.worldhostbedrock.xbox.models.ProfileUser;
import net.minecraft.client.resources.DefaultPlayerSkin;

import java.net.URI;
import java.util.Objects;

public final class BedrockProfileInfo {
    private BedrockProfileInfo() {
    }

    public static ProfileInfo create(ProfileUser user) {
        return create(user.id(), user.displayName(), user.profilePicture());
    }

    public static ProfileInfo create(XUID xuid, String displayName, URI profilePicture) {
        IconRenderer icon = IconRenderer.createSkinIconRenderer(DefaultPlayerSkin.get(xuid.toUuid()).texture());
        if (profilePicture != null) {
            icon = new ProfileIconRenderer(
                icon, WorldHostBedrock.getInstance().getBedrockIconManager().getOrLoad(profilePicture)
            );
        }

        return new ProfileInfo.Basic(Objects.requireNonNullElse(displayName, xuid.toString()), icon);
    }
}
