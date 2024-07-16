package io.github.gaming32.worldhostbedrock.impl;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.plugin.Joinability;
import io.github.gaming32.worldhost.plugin.OnlineFriend;
import io.github.gaming32.worldhostbedrock.connect.BedrockConnection;
import io.github.gaming32.worldhostbedrock.util.XUID;
import io.github.gaming32.worldhostbedrock.xbox.models.Session;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public record BedrockOnlineFriend(
    Session session, BedrockConnection connection
) implements OnlineFriend, BedrockProfilable {
    public BedrockOnlineFriend(Session session) {
        this(session, BedrockConnection.chooseBestConnection(session.customProperties().supportedConnections()));
    }

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
        if (connection == null) {
            throw new UnsupportedOperationException("Cannot join world with no join method!");
        }
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSingleplayerServer() != null) {
            minecraft.getSingleplayerServer().halt(false);
        }
        connection.joinWorld(parentScreen, session);
    }

    @Override
    public Joinability joinability() {
        if (!WorldHost.isModLoaded("viafabricplus")) {
            return new Joinability.Unjoinable(Component.translatable("world_host_bedrock.join.no_vfp"));
        }
        if (connection == null) {
            return new Joinability.Unjoinable(Component.translatable("world_host_bedrock.join.no_methods"));
        }
        if (connection instanceof BedrockConnection.Unsupported unsupported) {
            return new Joinability.Unjoinable(Component.literal(unsupported.reason()));
        }
        return new Joinability.JoinableWithWarning(Component.translatable("world_host_bedrock.join.vb_in_alpha"));
    }
}
