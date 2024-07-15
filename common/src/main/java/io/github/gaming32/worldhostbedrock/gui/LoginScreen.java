package io.github.gaming32.worldhostbedrock.gui;

import de.florianmichael.viafabricplus.ViaFabricPlus;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhostbedrock.AuthenticationManager;
import io.github.gaming32.worldhostbedrock.WorldHostBedrock;
import io.github.gaming32.worldhostbedrock.gui.screen.StatusScreen;
import io.github.gaming32.worldhostbedrock.xbox.XboxRequests;
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
import net.raphimc.minecraftauth.util.logging.ConsoleLogger;
import net.raphimc.minecraftauth.util.logging.ILogger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class LoginScreen {
    private static final Component TITLE = Component.translatable("world_host_bedrock.login.title");

    private LoginScreen() {
    }

    public static void startLoginProcess(Screen parentScreen, AuthenticationManager authenticationManager) {
        final Minecraft minecraft = Minecraft.getInstance();
        final StatusScreen statusScreen = new StatusScreen(TITLE);
        minecraft.setScreen(statusScreen);
        Thread.ofVirtual().name("BedrockLogin").start(() -> {
            final AtomicBoolean cancelled = new AtomicBoolean();
            final AtomicReference<StepXblSisuAuthentication.XblSisuTokens> xbl = new AtomicReference<>();
            final AtomicReference<StepFullBedrockSession.FullBedrockSession> fullSession = new AtomicReference<>();
            try {
                final ILogger logger = new ConsoleLogger() {
                    @Override
                    public void info(String message) {
                        super.info(message);
                        if (!message.equals("Waiting for MSA login via device code...")) {
                            minecraft.execute(() -> {
                                if (minecraft.screen != statusScreen) {
                                    minecraft.setScreen(statusScreen);
                                }
                                statusScreen.setMessage(Component.literal(message));
                            });
                        }
                    }
                };
                final var httpClient = MinecraftAuth.createHttpClient()
                    .setHeader("User-Agent", XboxRequests.USER_AGENT);
                xbl.set(MinecraftAuth.BEDROCK_XBL_DEVICE_CODE_LOGIN.getFromInput(
                    logger, httpClient,
                    new StepMsaDeviceCode.MsaDeviceCodeCallback(deviceCode -> {
                        Util.getPlatform().openUri(deviceCode.getDirectVerificationUri());
                        minecraft.execute(() -> minecraft.setScreen(new ConfirmScreen(
                            copyUrl -> {
                                if (copyUrl) {
                                    minecraft.keyboardHandler.setClipboard(deviceCode.getDirectVerificationUri());
                                } else {
                                    minecraft.setScreen(parentScreen);
                                    cancelled.set(true);
                                }
                            },
                            TITLE,
                            Component.translatable("world_host_bedrock.login.message"),
                            Component.translatable("world_host_bedrock.login.copy_link"),
                            CommonComponents.GUI_CANCEL
                        )));
                    })
                ));
                fullSession.set(MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN.getFromInput(
                    logger, httpClient,
                    new StepMsaToken.RefreshToken(xbl.get().getInitialXblSession().getMsaToken().getRefreshToken())
                ));
            } catch (Exception e) {
                WorldHostBedrock.LOGGER.error("Failed to login to Bedrock", e);
                if (cancelled.get()) return;
                minecraft.execute(() -> minecraft.setScreen(new DisconnectedScreen(
                    parentScreen,
                    TITLE,
                    Component.literal(e.toString()),
                    CommonComponents.GUI_BACK
                )));
                return;
            }
            if (cancelled.get()) return;
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
                minecraft.setScreen(parentScreen);
            });
        });
    }
}
