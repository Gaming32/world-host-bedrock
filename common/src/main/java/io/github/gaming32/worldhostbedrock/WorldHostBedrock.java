package io.github.gaming32.worldhostbedrock;

import com.mojang.logging.LogUtils;
import de.florianmichael.viafabricplus.ViaFabricPlus;
import de.florianmichael.viafabricplus.save.impl.AccountsSave;
import io.github.gaming32.worldhost.FriendsListUpdate;
import io.github.gaming32.worldhost.LoadedWorldHostPlugin;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.plugin.InfoTextsCategory;
import io.github.gaming32.worldhost.plugin.OnlineFriend;
import io.github.gaming32.worldhost.plugin.WorldHostPlugin;
import io.github.gaming32.worldhostbedrock.xbox.XboxRequests;
import io.github.gaming32.worldhostbedrock.xbox.models.Session;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

@WorldHostPlugin.Entrypoint
public class WorldHostBedrock implements WorldHostPlugin {
    public static final String MOD_ID = "world_host_bedrock";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final boolean VFP_INSTALLED = WHBPlatform.isModLoaded("viafabricplus");
    public static final boolean GEYSER_INSTALLED = WHBPlatform.isModLoaded(WHBPlatform.getGeyserModId());

    private static final List<Component> BEDROCK_FRIENDS_TEXT = List.of(Component.translatable(
        "world_host_bedrock.friends.bedrock_notice",
        Component.translatable("world_host_bedrock.friends.bedrock_notice.link").withStyle(s -> s
            .applyFormat(ChatFormatting.UNDERLINE)
            .withColor(ChatFormatting.BLUE)
            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://account.xbox.com/Profile"))
        )
    ));

    private static WorldHostBedrock instance;

    private final Path cacheDir;
    private final AuthenticationManager authenticationManager;
    private final XboxRequests xboxRequests;
    private final BedrockIconManager bedrockIconManager;

    public WorldHostBedrock() {
        final Minecraft minecraft = Minecraft.getInstance();
        cacheDir = minecraft.gameDirectory.toPath().resolve("world_host_bedrock");
        authenticationManager = new AuthenticationManager(cacheDir.resolve("auth.json"));
        xboxRequests = new XboxRequests(authenticationManager);
        bedrockIconManager = new BedrockIconManager(cacheDir.resolve("icons"));
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
                    WorldHost.ONLINE_FRIENDS.put(session.ownerXuid().toUuid(), new BedrockOnlineFriend(session));
                    WorldHost.ONLINE_FRIEND_UPDATES.forEach(FriendsListUpdate::friendsListUpdate);
                }
            })
            .exceptionally(t -> {
                LOGGER.error("Failed to request online Bedrock friends", t);
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
