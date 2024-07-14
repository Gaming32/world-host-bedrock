package io.github.gaming32.worldhostbedrock.mixin.vfp;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.florianmichael.viafabricplus.save.impl.AccountsSave;
import io.github.gaming32.worldhostbedrock.WorldHostBedrock;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.bedrock.session.StepFullBedrockSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AccountsSave.class, remap = false)
public class MixinAccountsSave {
    @Shadow private StepFullBedrockSession.FullBedrockSession bedrockAccount;

    @ModifyExpressionValue(
        method = "refreshAndGetBedrockAccount",
        at = @At(
            value = "INVOKE",
            target = "Lnet/raphimc/minecraftauth/step/AbstractStep;refresh(Lnet/lenni0451/commons/httpclient/HttpClient;Lnet/raphimc/minecraftauth/step/AbstractStep$StepResult;)Lnet/raphimc/minecraftauth/step/AbstractStep$StepResult;"
        )
    )
    private AbstractStep.StepResult<?> copySave(AbstractStep.StepResult<?> stepResult) {
        final var session = (StepFullBedrockSession.FullBedrockSession)stepResult;
        if (session != bedrockAccount) {
            final var authenticationManager = WorldHostBedrock.getInstance().getAuthenticationManager();
            try {
                authenticationManager.setFullSessionAndXbl(session);
            } catch (Exception e) {
                WorldHostBedrock.LOGGER.error("Failed to init XBL from full session", e);
            }
            authenticationManager.save();
        }
        return stepResult;
    }
}
