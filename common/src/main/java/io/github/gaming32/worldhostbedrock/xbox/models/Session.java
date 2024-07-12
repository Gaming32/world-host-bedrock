package io.github.gaming32.worldhostbedrock.xbox.models;

import com.google.gson.annotations.SerializedName;
import io.github.gaming32.worldhostbedrock.util.WHBConstants;
import io.github.gaming32.worldhostbedrock.util.XUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;

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
        String version,
        String worldName,
        int protocol,
        @SerializedName("MemberCount") int memberCount,
        @SerializedName("MaxMemberCount") int maxMemberCount
    ) {
        public ServerStatus toServerStatus() {
            return new ServerStatus(
                Component.literal(worldName),
                Optional.of(new ServerStatus.Players(memberCount, maxMemberCount, List.of())),
                Optional.of(new ServerStatus.Version(version, protocol)),
                Optional.empty(),
                false
            );
        }
    }
}
