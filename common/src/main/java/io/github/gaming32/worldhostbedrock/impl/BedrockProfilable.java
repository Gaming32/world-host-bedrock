package io.github.gaming32.worldhostbedrock.impl;

import com.mojang.authlib.GameProfile;
import io.github.gaming32.worldhost.plugin.Profilable;
import io.github.gaming32.worldhost.plugin.ProfileInfo;
import io.github.gaming32.worldhost.plugin.vanilla.GameProfileProfileInfo;
import io.github.gaming32.worldhostbedrock.WorldHostBedrock;
import io.github.gaming32.worldhostbedrock.util.XUID;
import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;

public interface BedrockProfilable extends Profilable {
    XUID xuid();

    @Override
    default ProfileInfo fallbackProfileInfo() {
        return new GameProfileProfileInfo(new GameProfile(xuid().toUuid(), ""));
    }

    @Override
    default CompletableFuture<ProfileInfo> profileInfo() {
        return WorldHostBedrock.getInstance()
            .getXboxRequests()
            .requestProfileUser(xuid())
            .thenApplyAsync(BedrockProfileInfo::create, Minecraft.getInstance());
    }
}
