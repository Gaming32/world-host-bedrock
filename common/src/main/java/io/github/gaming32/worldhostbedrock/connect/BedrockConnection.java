package io.github.gaming32.worldhostbedrock.connect;

import de.florianmichael.viafabricplus.injection.access.IServerInfo;
import io.github.gaming32.worldhostbedrock.xbox.models.Session;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public sealed interface BedrockConnection {
    NetworkConnectionType connectionType();

    int priority();

    void joinWorld(Screen parentScreen, Session session);

    @Nullable
    static BedrockConnection chooseBestConnection(List<Session.SupportedConnection> connections) {
        return connections.stream()
            .map(BedrockConnection::fromSupportedConnection)
            .max(Comparator.comparingInt(BedrockConnection::priority))
            .orElse(null);
    }

    static BedrockConnection fromSupportedConnection(Session.SupportedConnection connection) {
        final NetworkConnectionType type = NetworkConnectionType.byId(connection.connectionType());
        if (type == null) {
            return new Unsupported(NetworkConnectionType.Undefined, connection.connectionType());
        }
        return switch (type) {
            // case WebRTCSignalingUsingWebSockets -> new NetherNet(connection.webRtcNetworkId());
            case UPNP -> new UPnP(connection.hostIpAddress(), connection.hostPort());
            default -> new Unsupported(type, connection.connectionType());
        };
    }

    record Unsupported(NetworkConnectionType connectionType, int connectionTypeId) implements BedrockConnection {
        public String reason() {
            if (connectionType == NetworkConnectionType.Undefined) {
                return "Unknown connection type ID " + connectionTypeId;
            }
            return "Unsupported connection type " + connectionType;
        }

        @Override
        public String toString() {
            return "Unsupported[reason=" + reason() + "]";
        }

        @Override
        public int priority() {
            return Integer.MIN_VALUE;
        }

        @Override
        public void joinWorld(Screen parentScreen, Session session) {
            throw new UnsupportedOperationException(reason());
        }
    }

    record NetherNet(String networkId) implements BedrockConnection {
        @Override
        public NetworkConnectionType connectionType() {
            return NetworkConnectionType.WebRTCSignalingUsingWebSockets;
        }

        @Override
        public int priority() {
            return 10;
        }

        @Override
        public void joinWorld(Screen parentScreen, Session session) {
            throw new UnsupportedOperationException("NetherNet not implemented yet");
        }
    }

    record UPnP(String host, int port) implements BedrockConnection {
        @Override
        public NetworkConnectionType connectionType() {
            return NetworkConnectionType.UPNP;
        }

        @Override
        public int priority() {
            return 20;
        }

        @Override
        public void joinWorld(Screen parentScreen, Session session) {
            ConnectScreen.startConnecting(
                parentScreen,
                Minecraft.getInstance(),
                new ServerAddress(host, port),
                createServerData(session),
                false,
                null
            );
        }

        private ServerData createServerData(Session session) {
            final var properties = session.customProperties();
            final ServerData serverData = new ServerData(properties.hostName(), host, ServerData.Type.OTHER);
            serverData.motd = Component.literal(properties.worldName());
            serverData.players = new ServerStatus.Players(properties.maxMemberCount(), properties.memberCount(), List.of());
            serverData.version = Component.literal("Bedrock " + session.customProperties().version());
            //noinspection DataFlowIssue
            ((IServerInfo)serverData).viaFabricPlus$forceVersion(BedrockProtocolVersion.bedrockLatest);
            return serverData;
        }
    }
}
