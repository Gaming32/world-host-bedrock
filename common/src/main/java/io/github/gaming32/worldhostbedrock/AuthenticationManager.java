package io.github.gaming32.worldhostbedrock;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.github.gaming32.worldhostbedrock.util.XUID;
import net.minecraft.Util;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.xbl.StepXblSisuAuthentication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class AuthenticationManager {
    private final Path authFile;
    private StepXblSisuAuthentication.XblSisuTokens session;

    public AuthenticationManager(Path authFile) {
        this.authFile = authFile;
    }

    public StepXblSisuAuthentication.XblSisuTokens getSession() {
        return session;
    }

    public void setSession(StepXblSisuAuthentication.XblSisuTokens session) {
        this.session = session;
    }

    public CompletableFuture<StepXblSisuAuthentication.XblSisuTokens> getRefreshedSession() {
        if (session == null || !session.isExpired()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                session = MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.refresh(MinecraftAuth.createHttpClient(), session);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to refresh Bedrock login", e);
            }
            save();
            return session;
        }, Util.ioPool());
    }

    public void load() {
        final JsonElement json;
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(authFile))) {
            json = Streams.parse(reader);
        } catch (NoSuchFileException ignored) {
            return;
        } catch (Exception e) {
            WorldHostBedrock.LOGGER.error("Failed to load auth", e);
            return;
        }
        session = !json.equals(JsonNull.INSTANCE)
            ? MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.fromJson(json.getAsJsonObject())
            : null;
    }

    public void save() {
        final JsonElement json = session != null
            ? MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.toJson(session)
            : JsonNull.INSTANCE;
        try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(authFile))) {
            writer.setIndent("  ");
            Streams.write(json, writer);
        } catch (IOException e) {
            WorldHostBedrock.LOGGER.error("Failed to save auth", e);
        }
    }

    public XUID getXuid() {
        if (session == null) {
            return null;
        }
        return XUID.parse(session.getDisplayClaims().get("xid"));
    }

    public CompletableFuture<String> getAuthHeader() {
        return getRefreshedSession().thenApply(session -> {
            if (session == null) {
                return null;
            }
            return "XBL3.0 x=" + session.getServiceToken();
        });
    }
}
