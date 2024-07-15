package io.github.gaming32.worldhostbedrock.impl;

import io.github.gaming32.worldhost.plugin.FriendListFriend;
import io.github.gaming32.worldhost.plugin.ProfileInfo;
import io.github.gaming32.worldhostbedrock.WorldHostBedrock;
import io.github.gaming32.worldhostbedrock.xbox.models.Person;
import net.minecraft.network.chat.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class BedrockFriendListFriend implements FriendListFriend {
    private final Person person;
    private final ProfileInfo profile;

    public BedrockFriendListFriend(Person person) {
        this.person = person;
        profile = BedrockProfileInfo.create(person.xuid(), person.displayName(), person.displayPicRaw());
    }

    @Override
    public ProfileInfo fallbackProfileInfo() {
        return profile;
    }

    @Override
    public CompletableFuture<ProfileInfo> profileInfo() {
        return CompletableFuture.completedFuture(profile);
    }

    @Override
    public void removeFriend(Runnable refresher) {
        WorldHostBedrock.getInstance()
            .getXboxRequests()
            .removeFriend(person.xuid())
            .thenRun(refresher)
            .exceptionally(t -> {
                WorldHostBedrock.LOGGER.error("Failed to remove friend {}", person.gamertag(), t);
                return null;
            });
    }

    @Override
    public Optional<Component> tag() {
        return Optional.of(Component.literal("Bedrock"));
    }
}
