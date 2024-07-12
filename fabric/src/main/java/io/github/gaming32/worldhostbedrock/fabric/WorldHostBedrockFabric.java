package io.github.gaming32.worldhostbedrock.fabric;

import io.github.gaming32.worldhostbedrock.WorldHostBedrock;
import net.fabricmc.api.ClientModInitializer;

public class WorldHostBedrockFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WorldHostBedrock.init();
    }
}
