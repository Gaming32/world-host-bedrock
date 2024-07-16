package io.github.gaming32.worldhostbedrock;

import com.mojang.logging.LogUtils;
import de.florianmichael.viafabricplus.ViaFabricPlus;
import de.florianmichael.viafabricplus.save.impl.AccountsSave;
import io.github.gaming32.worldhost.LoadedWorldHostPlugin;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.plugin.FriendAdder;
import io.github.gaming32.worldhost.plugin.FriendListFriend;
import io.github.gaming32.worldhost.plugin.OnlineFriend;
import io.github.gaming32.worldhost.plugin.WorldHostPlugin;
import io.github.gaming32.worldhostbedrock.impl.BedrockFriendAdder;
import io.github.gaming32.worldhostbedrock.impl.BedrockFriendListFriend;
import io.github.gaming32.worldhostbedrock.impl.BedrockOnlineFriend;
import io.github.gaming32.worldhostbedrock.xbox.XboxRequests;
import io.github.gaming32.worldhostbedrock.xbox.models.Session;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

@WorldHostPlugin.Entrypoint
public class WorldHostBedrock implements WorldHostPlugin {
    public static final String MOD_ID = "world_host_bedrock";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final boolean VFP_INSTALLED = WHBPlatform.isModLoaded("viafabricplus");
    public static final boolean GEYSER_INSTALLED = WHBPlatform.isModLoaded(WHBPlatform.getGeyserModId());

    private static WorldHostBedrock instance;

    private final Path cacheDir;
    private final AuthenticationManager authenticationManager;
    private final XboxRequests xboxRequests;
    private final BedrockIconManager bedrockIconManager;
    private final BedrockFriendAdder friendAdder;
    private final BedrockPoller poller;

    public WorldHostBedrock() {
        final Minecraft minecraft = Minecraft.getInstance();
        cacheDir = minecraft.gameDirectory.toPath().resolve("world_host_bedrock");
        authenticationManager = new AuthenticationManager(cacheDir.resolve("auth.json"));
        xboxRequests = new XboxRequests(authenticationManager);
        bedrockIconManager = new BedrockIconManager(cacheDir.resolve("icons"));
        friendAdder = new BedrockFriendAdder(xboxRequests);
        poller = new BedrockPoller(xboxRequests);
    }

    public static WorldHostBedrock getInstance() {
        return instance;
    }

    public Path getCacheDir() {
        return cacheDir;
    }

    public AuthenticationManager getAuthenticationManager() {
        return authenticationManager;
    }

    public XboxRequests getXboxRequests() {
        return xboxRequests;
    }

    public BedrockIconManager getBedrockIconManager() {
        return bedrockIconManager;
    }

    @Override
    public void init() {
        instance = WorldHost.getPlugins()
            .stream()
            .map(LoadedWorldHostPlugin::plugin)
            .filter(WorldHostBedrock.class::isInstance)
            .map(WorldHostBedrock.class::cast)
            .findAny()
            .orElseThrow();

        initAuthentication();

        LOGGER.info("Logged into Bedrock as {}", authenticationManager.getXuid());
        LOGGER.info("Java can connect to Bedrock: {}", VFP_INSTALLED);
        LOGGER.info("Bedrock can connect to Java: {}", GEYSER_INSTALLED);

        poller.start();
    }

    private void initAuthentication() {
        authenticationManager.load();
        authenticationManager.save();

        if (VFP_INSTALLED) {
            final AccountsSave accountsSave = ViaFabricPlus.global().getSaveManager().getAccountsSave();
            if (authenticationManager.getFullSession() != null && accountsSave.getBedrockAccount() == null) {
                accountsSave.setBedrockAccount(authenticationManager.getFullSession());
            } else if (authenticationManager.getFullSession() == null && accountsSave.getBedrockAccount() != null) {
                try {
                    authenticationManager.setFullSessionAndXbl(accountsSave.getBedrockAccount());
                } catch (Exception e) {
                    LOGGER.error("Failed to init XBL from full session", e);
                }
                authenticationManager.save();
            }
        }
    }

    @Override
    public void pingFriends(Collection<OnlineFriend> friends) {
        friends.stream()
            .filter(BedrockOnlineFriend.class::isInstance)
            .map(BedrockOnlineFriend.class::cast)
            .map(BedrockOnlineFriend::session)
            .forEach(session -> WorldHost.ONLINE_FRIEND_PINGS.put(
                session.ownerXuid().toUuid(), session.customProperties().toServerStatus()
            ));
    }

    @Override
    public void refreshOnlineFriends() {
        xboxRequests.requestSessions()
            .thenAcceptAsync(sessions -> {
                for (final Session session : sessions) {
                    WorldHost.friendWentOnline(new BedrockOnlineFriend(session));
                }
            }, Minecraft.getInstance())
            .exceptionally(t -> {
                LOGGER.error("Failed to request online Bedrock friends", t);
                return null;
            });
    }

    @Override
    public void listFriends(Consumer<FriendListFriend> friendConsumer) {
        xboxRequests.requestSocial()
            .thenAccept(friends -> friends.stream()
                .map(BedrockFriendListFriend::new)
                .forEach(friendConsumer)
            )
            .exceptionally(t -> {
                LOGGER.error("Failed to request Bedrock friends list", t);
                return null;
            });
    }

    @Override
    public Optional<FriendAdder> friendAdder() {
        return Optional.of(friendAdder);
    }
}
