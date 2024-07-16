package io.github.gaming32.worldhostbedrock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.florianmichael.viafabricplus.ViaFabricPlus;
import io.github.gaming32.worldhostbedrock.util.XUID;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.bedrock.session.StepFullBedrockSession;
import net.raphimc.minecraftauth.step.msa.StepMsaToken;
import net.raphimc.minecraftauth.step.xbl.StepXblSisuAuthentication;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class AuthenticationManager {
    private static final Gson GSON = new GsonBuilder().create();

    private final Path authFile;
    private StepXblSisuAuthentication.XblSisuTokens xbl;
    private StepFullBedrockSession.FullBedrockSession fullSession;

    public AuthenticationManager(Path authFile) {
        this.authFile = authFile;
    }

    public StepXblSisuAuthentication.XblSisuTokens getXbl() {
        return xbl;
    }

    public void setXbl(StepXblSisuAuthentication.XblSisuTokens xbl) {
        this.xbl = xbl;
    }

    public void setXblAndFullSession(StepXblSisuAuthentication.XblSisuTokens xbl) throws Exception {
        fullSession = MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN.getFromInput(
            MinecraftAuth.createHttpClient(),
            new StepMsaToken.RefreshToken(xbl.getInitialXblSession().getMsaToken().getRefreshToken())
        );
        this.xbl = xbl;
    }

    public StepFullBedrockSession.FullBedrockSession getFullSession() {
        return fullSession;
    }

    public void setFullSession(StepFullBedrockSession.FullBedrockSession fullSession) {
        this.fullSession = fullSession;
    }

    public void setFullSessionAndXbl(StepFullBedrockSession.FullBedrockSession fullSession) throws Exception {
        xbl = MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.getFromInput(
            MinecraftAuth.createHttpClient(),
            new StepMsaToken.RefreshToken(
                fullSession.getMcChain().getXblXsts().getInitialXblSession().getMsaToken().getRefreshToken()
            )
        );
        this.fullSession = fullSession;
    }

    public CompletableFuture<StepXblSisuAuthentication.XblSisuTokens> getRefreshedXbl() {
        return getRefreshed(
            MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN,
            AuthenticationManager::getXbl,
            AuthenticationManager::setXbl
        );
    }

    public CompletableFuture<StepFullBedrockSession.FullBedrockSession> getRefreshedFullSession() {
        return getRefreshed(
            MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN,
            AuthenticationManager::getFullSession,
            AuthenticationManager::setFullSession
        ).thenApplyAsync(session -> {
            if (WorldHostBedrock.VFP_INSTALLED) {
                ViaFabricPlus.global().getSaveManager().getAccountsSave().setBedrockAccount(session);
            }
            return session;
        }, Minecraft.getInstance());
    }

    private <T extends AbstractStep.StepResult<?>> CompletableFuture<T> getRefreshed(
        AbstractStep<?, T> step,
        Function<AuthenticationManager, T> getter,
        BiConsumer<AuthenticationManager, T> setter
    ) {
        final T currentValue = getter.apply(this);
        if (currentValue == null || !currentValue.isExpired()) {
            return CompletableFuture.completedFuture(currentValue);
        }
        return CompletableFuture.supplyAsync(() -> {
            final T newValue;
            try {
                newValue = step.refresh(MinecraftAuth.createHttpClient(), currentValue);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to refresh XBL login", e);
            }
            setter.accept(this, newValue);
            save();
            return newValue;
        }, Util.ioPool());
    }

    public void load() {
        xbl = null;
        fullSession = null;
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(authFile))) {
            reader.beginObject();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "xbl" -> xbl = MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.fromJson(
                        GSON.fromJson(reader, JsonObject.class)
                    );
                    case "fullSession" -> fullSession = MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN.fromJson(
                        GSON.fromJson(reader, JsonObject.class)
                    );
                    default -> reader.skipValue();
                }
            }
            reader.endObject();
        } catch (NoSuchFileException ignored) {
        } catch (Exception e) {
            WorldHostBedrock.LOGGER.error("Failed to load auth", e);
        }
    }

    public void save() {
        try (JsonWriter writer = new JsonWriter(Files.newBufferedWriter(authFile))) {
            writer.beginObject();
            if (xbl != null) {
                writer.name("xbl");
                GSON.toJson(MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.toJson(xbl), writer);
            }
            if (fullSession != null) {
                writer.name("fullSession");
                GSON.toJson(MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN.toJson(fullSession), writer);
            }
            writer.endObject();
        } catch (Exception e) {
            WorldHostBedrock.LOGGER.error("Failed to save auth", e);
        }
    }

    public XUID getXuid() {
        if (xbl == null) {
            return null;
        }
        return XUID.parse(fullSession.getMcChain().getXuid());
    }

    public CompletableFuture<String> getAuthHeader() {
        return getRefreshedXbl().thenApply(session -> {
            if (session == null) {
                return null;
            }
            return "XBL3.0 x=" + session.getServiceToken();
        });
    }
}
