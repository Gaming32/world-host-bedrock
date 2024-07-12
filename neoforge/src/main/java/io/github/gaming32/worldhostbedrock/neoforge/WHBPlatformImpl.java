package io.github.gaming32.worldhostbedrock.neoforge;

import net.neoforged.fml.ModList;

public class WHBPlatformImpl {
    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static String getModVersion(String modId) {
        return ModList.get()
            .getModContainerById(modId)
            .orElseThrow()
            .getModInfo()
            .getVersion()
            .toString();
    }

    public static String getGeyserModId() {
        return "geyser_neoforge";
    }
}
