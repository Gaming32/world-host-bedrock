package io.github.gaming32.worldhostbedrock.fabric;

import net.fabricmc.loader.api.FabricLoader;

public class WHBPlatformImpl {
    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static String getModVersion(String modId) {
        return FabricLoader.getInstance()
            .getModContainer(modId)
            .orElseThrow()
            .getMetadata()
            .getVersion()
            .getFriendlyString();
    }

    public static String getGeyserModId() {
        return "geyser-fabric";
    }
}
