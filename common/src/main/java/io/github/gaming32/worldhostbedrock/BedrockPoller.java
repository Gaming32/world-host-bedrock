package io.github.gaming32.worldhostbedrock;

import io.github.gaming32.worldhost.FriendsListUpdate;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.plugin.OnlineFriend;
import io.github.gaming32.worldhostbedrock.impl.BedrockOnlineFriend;
import io.github.gaming32.worldhostbedrock.util.XUID;
import io.github.gaming32.worldhostbedrock.xbox.XboxRequests;
import io.github.gaming32.worldhostbedrock.xbox.models.Session;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class BedrockPoller implements AutoCloseable {
    private static final ThreadFactory THREAD_FACTORY = Thread.ofVirtual()
        .name("BedrockPoller-", 0)
        .factory();

    private final XboxRequests xboxRequests;
    private final ScheduledExecutorService executor;

    public BedrockPoller(XboxRequests xboxRequests) {
        this.xboxRequests = xboxRequests;
        final var executor = new ScheduledThreadPoolExecutor(1, THREAD_FACTORY);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        this.executor = executor;
    }

    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    public void start() {
        executor.scheduleAtFixedRate(this::run, 10L, 10L, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        executor.close();
    }

    private void run() {
        WorldHostBedrock.LOGGER.debug("Polling for Bedrock sessions...");
        xboxRequests.requestSessions()
            .thenAcceptAsync(this::updateSessions, Minecraft.getInstance())
            .exceptionally(t -> {
                WorldHostBedrock.LOGGER.error("Failed to poll friends list", t);
                return null;
            });
    }

    private void updateSessions(List<Session> sessions) {
        final Set<XUID> sessionXuids = sessions.stream().map(Session::ownerXuid).collect(Collectors.toSet());
        boolean hasUpdate = WorldHost.ONLINE_FRIENDS.values().removeIf(friend ->
            friend instanceof BedrockOnlineFriend bedrockFriend && !sessionXuids.contains(bedrockFriend.xuid())
        );

        for (final Session session : sessions) {
            final UUID sessionUuid = session.ownerXuid().toUuid();
            final OnlineFriend oldFriend = WorldHost.ONLINE_FRIENDS.get(sessionUuid);
            if (!(oldFriend instanceof BedrockOnlineFriend oldBedrock) || !session.equals(oldBedrock.session())) {
                hasUpdate = true;
                WorldHost.ONLINE_FRIENDS.put(sessionUuid, new BedrockOnlineFriend(session));
            }
        }

        if (hasUpdate) {
            WorldHostBedrock.LOGGER.info("Bedrock sessions updated");
            WorldHost.ONLINE_FRIEND_UPDATES.forEach(FriendsListUpdate::friendsListUpdate);
        }
    }
}
