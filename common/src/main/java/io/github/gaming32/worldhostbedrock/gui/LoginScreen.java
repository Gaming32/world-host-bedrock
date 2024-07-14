package io.github.gaming32.worldhostbedrock.gui;

import de.florianmichael.viafabricplus.ViaFabricPlus;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhostbedrock.AuthenticationManager;
import io.github.gaming32.worldhostbedrock.WorldHostBedrock;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.bedrock.session.StepFullBedrockSession;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;
import net.raphimc.minecraftauth.step.msa.StepMsaToken;
import net.raphimc.minecraftauth.step.xbl.StepXblSisuAuthentication;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class LoginScreen {
    private LoginScreen() {
    }

    public static void startLoginProcess(Screen parentScreen, AuthenticationManager authenticationManager) {
        final Minecraft minecraft = Minecraft.getInstance();
        Thread.ofVirtual().name("BedrockLogin").start(() -> {
            final AtomicBoolean cancelled = new AtomicBoolean();
            final AtomicReference<StepXblSisuAuthentication.XblSisuTokens> xbl = new AtomicReference<>();
            final AtomicReference<StepFullBedrockSession.FullBedrockSession> fullSession = new AtomicReference<>();
            try {
                final var httpClient = MinecraftAuth.createHttpClient();
                xbl.set(MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.getFromInput(
                    httpClient,
                    new StepMsaDeviceCode.MsaDeviceCodeCallback(deviceCode -> {
                        minecraft.execute(() -> minecraft.setScreen(new ConfirmScreen(
                            copyUrl -> {
                                if (copyUrl) {
                                    minecraft.keyboardHandler.setClipboard(deviceCode.getDirectVerificationUri());
                                } else {
                                    minecraft.setScreen(parentScreen);
                                    cancelled.set(true);
                                }
                            },
                            Component.translatable("world_host_bedrock.login.title"),
                            Component.translatable("world_host_bedrock.login.message"),
                            Component.translatable("world_host_bedrock.login.copy_link"),
                            CommonComponents.GUI_CANCEL
                        )));
                        Util.getPlatform().openUri(deviceCode.getDirectVerificationUri());
                    })
                ));
                fullSession.set(MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN.getFromInput(
                    httpClient,
                    new StepMsaToken.RefreshToken(xbl.get().getInitialXblSession().getMsaToken().getRefreshToken())
                ));
            } catch (Exception e) {
                WorldHostBedrock.LOGGER.error("Failed to login to Bedrock", e);
                if (cancelled.get()) return;
                minecraft.execute(() -> minecraft.setScreen(new DisconnectedScreen(
                    parentScreen,
                    Component.translatable("world_host_bedrock.login.title"),
                    Component.literal(e.toString()),
                    CommonComponents.GUI_BACK
                )));
                return;
            }
            minecraft.execute(() -> {
                final var newXbl = xbl.get();
                final var newSession = fullSession.get();
                if (newXbl != null && newSession != null) {
                    authenticationManager.setXbl(newXbl);
                    authenticationManager.setFullSession(newSession);
                    authenticationManager.save();
                    WorldHost.refreshFriendsList();
                    if (WorldHostBedrock.VFP_INSTALLED) {
                        ViaFabricPlus.global().getSaveManager().getAccountsSave().setBedrockAccount(newSession);
                    }
                }
                if (!cancelled.get()) {
                    minecraft.setScreen(parentScreen);
                }
            });
        });
    }
}
