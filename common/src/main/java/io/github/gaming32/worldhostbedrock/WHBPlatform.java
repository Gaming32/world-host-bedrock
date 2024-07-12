package io.github.gaming32.worldhostbedrock;

import dev.architectury.injectables.annotations.ExpectPlatform;

public class WHBPlatform {
    @ExpectPlatform
    public static boolean isModLoaded(String modId) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static String getModVersion(String modId) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static String getGeyserModId() {
        throw new AssertionError();
    }
}
