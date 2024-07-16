package io.github.gaming32.worldhostbedrock.xbox.models;

import com.google.gson.annotations.SerializedName;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhostbedrock.util.WHBConstants;
import io.github.gaming32.worldhostbedrock.util.XUID;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record Session(SessionRef sessionRef, XUID ownerXuid, CustomProperties customProperties) {
    public boolean isMinecraft() {
        return SessionRef.MINECRAFT.equals(sessionRef) && ownerXuid != null && customProperties != null;
    }

    public record SessionRef(UUID scid, String templateName) {
        public static final SessionRef MINECRAFT = new SessionRef(
            WHBConstants.MINECRAFT_SCID, WHBConstants.MINECRAFT_TEMPLATE_NAME
        );
    }

    public record CustomProperties(
        String hostName,
        String version,
        String worldName,
        int protocol,
        @SerializedName("MemberCount") int memberCount,
        @SerializedName("MaxMemberCount") int maxMemberCount,
        @SerializedName("SupportedConnections") List<SupportedConnection> supportedConnections
    ) {
        public ServerStatus toServerStatus() {
            final boolean isCompatibleBedrock =
                WorldHost.isModLoaded("viafabricplus") &&
                protocol == BedrockProtocolVersion.bedrockLatest.getVersion();
            final int protocolVersion = isCompatibleBedrock ? SharedConstants.getProtocolVersion() : protocol;
            return new ServerStatus(
                Component.literal(worldName),
                Optional.of(new ServerStatus.Players(maxMemberCount, memberCount, List.of())),
                Optional.of(new ServerStatus.Version("Bedrock" + version, protocolVersion)),
                Optional.empty(),
                false
            );
        }
    }

    public record SupportedConnection(
        @SerializedName("ConnectionType") int connectionType,
        @SerializedName("HostIpAddress") String hostIpAddress,
        @SerializedName("HostPort") int hostPort,
        @SerializedName("WebRTCNetworkId") String webRtcNetworkId
    ) {
    }
}
