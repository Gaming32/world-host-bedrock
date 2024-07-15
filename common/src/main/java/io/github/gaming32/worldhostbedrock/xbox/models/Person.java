package io.github.gaming32.worldhostbedrock.xbox.models;

import io.github.gaming32.worldhostbedrock.util.XUID;

import java.net.URI;

public record Person(
    XUID xuid,
    boolean isFollowedByCaller,
    String displayName,
    URI displayPicRaw
) {
}
