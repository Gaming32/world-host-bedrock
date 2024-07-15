package io.github.gaming32.worldhostbedrock.impl;

import io.github.gaming32.worldhost.plugin.OnlineFriend;
import io.github.gaming32.worldhostbedrock.util.XUID;
import io.github.gaming32.worldhostbedrock.xbox.models.Session;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.UUID;

public record BedrockOnlineFriend(Session session) implements OnlineFriend, BedrockProfilable {
    @Override
    public XUID xuid() {
        return session.ownerXuid();
    }

    @Override
    public UUID uuid() {
        return xuid().toUuid();
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
