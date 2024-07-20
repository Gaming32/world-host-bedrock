package io.github.gaming32.worldhostbedrock.impl;

import io.github.gaming32.worldhost.plugin.FriendAdder;
import io.github.gaming32.worldhost.plugin.FriendListFriend;
import io.github.gaming32.worldhostbedrock.xbox.XboxRequests;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class BedrockFriendAdder implements FriendAdder {
    private final XboxRequests xboxRequests;

    public BedrockFriendAdder(XboxRequests xboxRequests) {
        this.xboxRequests = xboxRequests;
    }

    @Override
    public Component label() {
        return Component.literal("Bedrock");
    }

    @Override
    public void searchFriends(String name, int maxResults, Consumer<FriendListFriend> friendConsumer) {
        if (name.isBlank()) return;
        xboxRequests.searchPeople(name, maxResults)
            .thenAccept(p -> p.stream().map(BedrockFriendListFriend::new).forEach(friendConsumer));
    }

    @Override
    public boolean delayLookup(String name) {
        return !name.isBlank();
    }
}
