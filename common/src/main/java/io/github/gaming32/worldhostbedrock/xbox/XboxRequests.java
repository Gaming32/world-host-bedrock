package io.github.gaming32.worldhostbedrock.xbox;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import io.github.gaming32.worldhostbedrock.AuthenticationManager;
import io.github.gaming32.worldhostbedrock.WHBPlatform;
import io.github.gaming32.worldhostbedrock.WorldHostBedrock;
import io.github.gaming32.worldhostbedrock.util.LocaleUtil;
import io.github.gaming32.worldhostbedrock.util.WHBConstants;
import io.github.gaming32.worldhostbedrock.util.XUID;
import io.github.gaming32.worldhostbedrock.xbox.models.Person;
import io.github.gaming32.worldhostbedrock.xbox.models.ProfileUser;
import io.github.gaming32.worldhostbedrock.xbox.models.Session;
import net.minecraft.Util;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.intellij.lang.annotations.Language;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public final class XboxRequests {
    public static final String USER_AGENT = "World Host Bedrock/" + WHBPlatform.getModVersion(WorldHostBedrock.MOD_ID);
    private static final Gson GSON = new GsonBuilder().create();

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

    private static final URI SOCIAL_URI = URI.create("https://peoplehub.xboxlive.com/users/me/people/social");

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
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("User-Agent", USER_AGENT)
                .header("x-xbl-contract-version", "107")
                .header("Authorization", authentication)
                .POST(HttpRequest.BodyPublishers.ofString(
                    SESSIONS_REQUEST.formatted(WHBConstants.MINECRAFT_SCID, authenticationManager.getXuid())
                ))
                .build();
            return requestObjectWithArray(request, "results", Session.class);
        });
    }

    public CompletableFuture<ProfileUser> requestProfileUser(XUID xuid) {
        return authenticationManager.getAuthHeader().thenCompose(authentication -> {
            if (authentication == null) {
                return notAuthenticatedException();
            }
            final URI uri = buildUri("https://profile.xboxlive.com", builder -> builder
                .setPathSegments("users", "xuid(" + xuid + ")", "profile", "settings")
                .addParameter("settings", ProfileUser.DISPLAY_NAME_SETTING + "," + ProfileUser.PROFILE_PICTURE_SETTING)
            );
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("User-Agent", USER_AGENT)
                .header("x-xbl-contract-version", "2")
                .header("Authorization", authentication)
                .GET()
                .build();
            return requestObjectWithArray(request, "profileUsers", ProfileUser.class)
                .thenApply(users -> users
                    .stream()
                    .filter(u -> u.id().equals(xuid))
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException("Didn't receive back profile info for " + xuid))
                );
        });
    }

    public CompletableFuture<List<Person>> requestSocial() {
        return authenticationManager.getAuthHeader().thenCompose(authentication -> {
            if (authentication == null) {
                return CompletableFuture.completedFuture(List.of());
            }
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(SOCIAL_URI)
                .header("User-Agent", USER_AGENT)
                .header("x-xbl-contract-version", "6")
                .header("Authorization", authentication)
                .header("Accept-Language", LocaleUtil.minecraftToXbl(LocaleUtil.getCurrent()))
                .GET()
                .build();
            return requestObjectWithArray(request, "people", Person.class);
        });
    }

    public CompletableFuture<Void> addFriend(XUID xuid) {
        return authenticationManager.getAuthHeader().thenCompose(authentication -> {
            if (authentication == null) {
                return notAuthenticatedException();
            }
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(createPeopleUri(xuid))
                .header("Authorization", authentication)
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
            return requestNoBody(request);
        });
    }

    public CompletableFuture<Void> removeFriend(XUID friend) {
        return authenticationManager.getAuthHeader().thenCompose(authentication -> {
            if (authentication == null) {
                return notAuthenticatedException();
            }
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(createPeopleUri(friend))
                .header("User-Agent", USER_AGENT)
                .header("Authorization", authentication)
                .DELETE()
                .build();
            return requestNoBody(request);
        });
    }

    public CompletableFuture<Optional<Person>> searchPerson(String gamertag) {
        return authenticationManager.getAuthHeader().thenCompose(authentication -> {
            if (authentication == null) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
            final URI uri = buildUri("https://peoplehub.xboxlive.com", builder -> builder
                .setPathSegments("users", "me", "people", "search")
                .addParameter("q", gamertag)
                .addParameter("maxItems", "1")
            );
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("User-Agent", USER_AGENT)
                .header("x-xbl-contract-version", "6")
                .header("Authorization", authentication)
                .header("Accept-Language", LocaleUtil.minecraftToXbl(LocaleUtil.getCurrent()))
                .GET()
                .build();
            return requestObjectWithArray(request, "people", Person.class)
                .thenApply(people -> people.stream()
                    .filter(p -> p.gamertag().equals(gamertag))
                    .findFirst()
                );
        });
    }

    private static <T> CompletableFuture<T> notAuthenticatedException() {
        return CompletableFuture.failedFuture(new IllegalStateException("Not authenticated to Xbox API"));
    }

    private static URI createPeopleUri(XUID xuid) {
        return URI.create("https://social.xboxlive.com/users/me/people/xuid(" + xuid + ")");
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

    private CompletableFuture<Void> requestNoBody(HttpRequest request) {
        return http.sendAsync(request, HttpResponse.BodyHandlers.discarding())
            .thenAccept(response -> checkHttpResponse(request.uri(), response));
    }

    private <T> CompletableFuture<List<T>> requestObjectWithArray(
        HttpRequest request, String arrayKey, Class<T> elementType
    ) {
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
            .thenApplyAsync(handleByParsingObjectWithArray(request.uri(), arrayKey, elementType), Util.ioPool());
    }

    private static <T> Function<HttpResponse<InputStream>, List<T>> handleByParsingObjectWithArray(
        URI sourceUri, String arrayKey, Class<T> elementType
    ) {
        return response -> {
            checkHttpResponse(sourceUri, response);
            Charset encoding;
            try {
                encoding = response.headers()
                    .firstValue("Content-Encoding")
                    .map(Charset::forName)
                    .orElse(StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ignored) {
                encoding = StandardCharsets.UTF_8;
            }
            try {
                return parseObjectWithArray(
                    new JsonReader(new InputStreamReader(response.body(), encoding)),
                    arrayKey, elementType
                );
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    private static void checkHttpResponse(URI sourceUri, HttpResponse<?> response) {
        if (response.statusCode() / 100 != 2) {
            final String reason = EnglishReasonPhraseCatalog.INSTANCE.getReason(response.statusCode(), null);
            throw new IllegalStateException(
                "Failed to " + response.request().method() +
                " " + sourceUri +
                ": " + response.statusCode() +
                " " + reason
            );
        }
    }

    private static <T> List<T> parseObjectWithArray(
        JsonReader reader, String arrayKey, Class<T> elementType
    ) throws IOException {
        final List<T> result = new ArrayList<>();
        reader.beginObject();
        while (reader.hasNext()) {
            if (!reader.nextName().equals(arrayKey)) {
                reader.skipValue();
                continue;
            }
            reader.beginArray();
            while (reader.hasNext()) {
                result.add(GSON.fromJson(reader, elementType));
            }
            reader.endArray();
        }
        reader.endObject();
        return result;
    }
}
