package io.github.gaming32.worldhostbedrock.xbox;

import com.google.gson.stream.JsonReader;
import io.github.gaming32.worldhostbedrock.AuthenticationManager;
import io.github.gaming32.worldhostbedrock.WHBPlatform;
import io.github.gaming32.worldhostbedrock.WorldHostBedrock;
import io.github.gaming32.worldhostbedrock.util.WHBConstants;
import io.github.gaming32.worldhostbedrock.util.XUID;
import io.github.gaming32.worldhostbedrock.xbox.models.ProfileUser;
import io.github.gaming32.worldhostbedrock.xbox.models.Session;
import net.minecraft.Util;
import org.apache.http.client.utils.URIBuilder;
import org.intellij.lang.annotations.Language;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class XboxRequests {
    public static final String USER_AGENT = "World Host Bedrock/" + WHBPlatform.getModVersion(WorldHostBedrock.MOD_ID);

    @Language("JSON")
    private static final String SESSIONS_REQUEST = """
        {
          "type": "activity",
          "scid": "%s",
          "owners": {
            "people": {
              "moniker": "people",
              "monikerXuid": "%s"
            }
          }
        }
        """;

    private final HttpClient http = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .executor(Util.ioPool())
        .build();
    private final AuthenticationManager authenticationManager;

    public XboxRequests(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public CompletableFuture<List<Session>> requestSessions() {
        return authenticationManager.getAuthHeader().thenCompose(authentication -> {
            if (authentication == null) {
                return CompletableFuture.completedFuture(List.of());
            }
            final URI uri = buildUri("https://sessiondirectory.xboxlive.com", builder -> builder
                .setPathSegments("handles", "query")
                .addParameter("include", "customProperties")
            );
            final HttpRequest request = HttpRequest.newBuilder(uri)
                .POST(HttpRequest.BodyPublishers.ofString(
                    SESSIONS_REQUEST.formatted(WHBConstants.MINECRAFT_SCID, authenticationManager.getXuid())
                ))
                .header("User-Agent", USER_AGENT)
                .header("Authorization", authentication)
                .header("x-xbl-contract-version", "107")
                .build();
            return http.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApplyAsync(response -> {
                    if (response.statusCode() / 100 != 2) {
                        throw new IllegalStateException("Failed to request " + uri + ": " + response.statusCode());
                    }
                    try {
                        return RequestParsers.parseSessions(new JsonReader(
                            new InputStreamReader(response.body(), StandardCharsets.UTF_8)
                        ));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }, Util.ioPool());
        });
    }

    public CompletableFuture<ProfileUser> requestProfileUser(XUID xuid) {
        return authenticationManager.getAuthHeader().thenCompose(authentication -> {
            if (authentication == null) {
                return CompletableFuture.failedFuture(new IllegalStateException("Not authenticated to Xbox API"));
            }
            final URI uri = buildUri("https://profile.xboxlive.com", builder -> builder
                .setPathSegments("users", "xuid(" + xuid + ")", "profile", "settings")
                .addParameter("settings", ProfileUser.DISPLAY_NAME_SETTING + "," + ProfileUser.PROFILE_PICTURE_SETTING)
            );
            final HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .header("User-Agent", USER_AGENT)
                .header("Authorization", authentication)
                .header("x-xbl-contract-version", "2")
                .build();
            return http.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApplyAsync(response -> {
                    if (response.statusCode() / 100 != 2) {
                        throw new IllegalStateException("Failed to request " + uri.getPath() + ": " + response.statusCode());
                    }
                    try {
                        return RequestParsers.parseProfileUsers(new JsonReader(
                            new InputStreamReader(response.body(), StandardCharsets.UTF_8)
                        ));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }, Util.ioPool())
                .thenApply(users -> users
                    .stream()
                    .filter(u -> u.id().equals(xuid))
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException("Didn't receive back profile info for " + xuid))
                );
        });
    }

    private static URI buildUri(String base, Consumer<URIBuilder> action) {
        try {
            final URIBuilder builder = new URIBuilder(base);
            action.accept(builder);
            return builder.build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }
}
