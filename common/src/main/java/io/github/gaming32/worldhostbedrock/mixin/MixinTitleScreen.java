package io.github.gaming32.worldhostbedrock.mixin;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.gui.OnlineStatusLocation;
import io.github.gaming32.worldhostbedrock.AuthenticationManager;
import io.github.gaming32.worldhostbedrock.WorldHostBedrock;
import io.github.gaming32.worldhostbedrock.gui.LoginScreen;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen extends Screen {
    protected MixinTitleScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void bedrockAccountStatus(CallbackInfo ci) {
        int y = 10 + (1 + WorldHost.getMenuLines(false, OnlineStatusLocation.RIGHT)) * WorldHost.getMenuLineSpacing();
        if (WorldHost.CONFIG.getOnlineStatusLocation() == OnlineStatusLocation.RIGHT) {
            y += WorldHost.getMenuLineSpacing();
        }
        final AuthenticationManager authenticationManager = WorldHostBedrock.getInstance().getAuthenticationManager();
        final var session = authenticationManager.getSession();
        final Component text = session != null
            ? Component.translatable("world_host_bedrock.status.logged_in", session.getDisplayClaims().get("umg"))
            : Component.translatable("world_host_bedrock.status.log_in");
        final int textWidth = font.width(text);
        addRenderableWidget(new PlainTextButton(
            width - 2 - textWidth, height - y, textWidth, 10, text,
            button -> LoginScreen.startLoginProcess(this, authenticationManager),
            font
        ));
    }
}
