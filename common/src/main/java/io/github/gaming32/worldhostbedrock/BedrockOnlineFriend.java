package io.github.gaming32.worldhostbedrock;

import com.mojang.authlib.GameProfile;
import io.github.gaming32.worldhost.plugin.OnlineFriend;
import io.github.gaming32.worldhost.plugin.ProfileInfo;
import io.github.gaming32.worldhost.plugin.vanilla.GameProfileProfileInfo;
import io.github.gaming32.worldhostbedrock.util.XUID;
import io.github.gaming32.worldhostbedrock.xbox.models.Session;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record BedrockOnlineFriend(Session session) implements OnlineFriend {
    public XUID xuid() {
        return session.ownerXuid();
    }

    @Override
    public UUID uuid() {
        return xuid().toUuid();
    }

    @Override
    public ProfileInfo fallbackProfileInfo() {
        return new GameProfileProfileInfo(new GameProfile(xuid().toUuid(), ""));
    }

    @Override
    public CompletableFuture<ProfileInfo> profileInfo() {
        return WorldHostBedrock.getInstance()
            .getXboxRequests()
            .requestProfileUser(session.ownerXuid())
            .thenApplyAsync(user -> BedrockProfileInfo.create(xuid().toString(), user), Minecraft.getInstance());
    }

    @Override
    public void joinWorld(Screen parentScreen) {
        throw new UnsupportedOperationException(unjoinableReason().map(Component::getString).orElse(null));
    }

    @Override
    public Optional<Component> unjoinableReason() {
        return Optional.of(Component.literal("Unable to join Bedrock worlds yet"));
    }
}
