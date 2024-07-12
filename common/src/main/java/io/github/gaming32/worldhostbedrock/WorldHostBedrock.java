package io.github.gaming32.worldhostbedrock;

import com.mojang.logging.LogUtils;
import io.github.gaming32.worldhost.FriendsListUpdate;
import io.github.gaming32.worldhost.LoadedWorldHostPlugin;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.plugin.InfoTextsCategory;
import io.github.gaming32.worldhost.plugin.OnlineFriend;
import io.github.gaming32.worldhost.plugin.WorldHostPlugin;
import io.github.gaming32.worldhostbedrock.util.XUID;
import io.github.gaming32.worldhostbedrock.xbox.XboxRequests;
import io.github.gaming32.worldhostbedrock.xbox.models.Session;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

@WorldHostPlugin.Entrypoint
public class WorldHostBedrock implements WorldHostPlugin {
    public static final String MOD_ID = "world_host_bedrock";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final boolean JAVA_CONNECTS_TO_BEDROCK = WHBPlatform.isModLoaded("viafabricplus");
    public static final boolean BEDROCK_CONNECTS_TO_JAVA = WHBPlatform.isModLoaded(WHBPlatform.getGeyserModId());

    private static final List<Component> BEDROCK_FRIENDS_TEXT = List.of(Component.translatable(
        "world_host_bedrock.friends.bedrock_notice",
        Component.translatable("world_host_bedrock.friends.bedrock_notice.link").withStyle(s -> s
            .applyFormat(ChatFormatting.UNDERLINE)
            .withColor(ChatFormatting.BLUE)
            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://account.xbox.com/Profile"))
        )
    ));

    private static final XUID GAMING32I_XUID = XUID.parse("2535416896536191");

    private static WorldHostBedrock instance;

    private final Path cacheDir;
    private final BedrockIconManager bedrockIconManager;
    private final XboxRequests xboxRequests;

    public WorldHostBedrock() {
        final Minecraft minecraft = Minecraft.getInstance();
        cacheDir = minecraft.gameDirectory.toPath().resolve("world_host_bedrock");
        bedrockIconManager = new BedrockIconManager(cacheDir.resolve("icons"));
        try {
            xboxRequests = new XboxRequests(Files.readString(Path.of("../../authentication.txt")));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static WorldHostBedrock getInstance() {
        return instance;
    }

    public BedrockIconManager getBedrockIconManager() {
        return bedrockIconManager;
    }

    public XboxRequests getXboxRequests() {
        return xboxRequests;
    }

    public Path getCacheDir() {
        return cacheDir;
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

        LOGGER.info("Java can connect to Bedrock: {}", JAVA_CONNECTS_TO_BEDROCK);
        LOGGER.info("Bedrock can connect to Java: {}", BEDROCK_CONNECTS_TO_JAVA);
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
    public void refreshFriendsList() {
        xboxRequests.requestSessions(GAMING32I_XUID)
            .thenAcceptAsync(sessions -> {
                for (final Session session : sessions) {
                    WorldHost.ONLINE_FRIENDS.put(session.ownerXuid().toUuid(), new BedrockOnlineFriend(session));
                    WorldHost.ONLINE_FRIEND_UPDATES.forEach(FriendsListUpdate::friendsListUpdate);
                }
            })
            .exceptionally(t -> {
                LOGGER.error("Failed to request online friends", t);
                return null;
            });
    }

    @Override
    public List<Component> getInfoTexts(InfoTextsCategory category) {
        return switch (category) {
            case FRIENDS_SCREEN -> BEDROCK_FRIENDS_TEXT;
            default -> List.of();
        };
    }
}
