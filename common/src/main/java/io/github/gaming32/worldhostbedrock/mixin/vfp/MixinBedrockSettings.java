package io.github.gaming32.worldhostbedrock.mixin.vfp;

import de.florianmichael.viafabricplus.settings.impl.BedrockSettings;
import io.github.gaming32.worldhostbedrock.WorldHostBedrock;
import io.github.gaming32.worldhostbedrock.gui.LoginScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = BedrockSettings.class, remap = false)
public class MixinBedrockSettings {
    /**
     * @author Gaming32i
     * @reason Unify the login screens
     */
    @Overwrite
    private void lambda$new$0() {
        LoginScreen.startLoginProcess(
            Minecraft.getInstance().screen,
            WorldHostBedrock.getInstance().getAuthenticationManager()
        );
    }
}
