package io.github.gaming32.worldhostbedrock.impl;

import io.github.gaming32.worldhost.plugin.FriendAdder;
import io.github.gaming32.worldhost.plugin.FriendListFriend;
import io.github.gaming32.worldhostbedrock.xbox.XboxRequests;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class BedrockFriendAdder implements FriendAdder {
    public static final Pattern VALID_GAMERTAG = Pattern.compile("[a-zA-Z0-9, ]{1,15}");

    private final XboxRequests xboxRequests;

    public BedrockFriendAdder(XboxRequests xboxRequests) {
        this.xboxRequests = xboxRequests;
    }

    @Override
    public Component label() {
        return Component.literal("Bedrock");
    }

    @Override
    public CompletableFuture<Optional<FriendListFriend>> resolveFriend(String name) {
        if (!VALID_GAMERTAG.matcher(name).matches()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return xboxRequests.searchPerson(name)
            .thenApply(p -> p.map(BedrockFriendListFriend::new));
    }

    @Override
    public boolean rateLimit(String name) {
        return true;
    }
}
