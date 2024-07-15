package io.github.gaming32.worldhostbedrock.util;

import net.minecraft.client.Minecraft;

import java.util.Locale;

public class LocaleUtil {
    public static String getCurrent() {
        return Minecraft.getInstance().options.languageCode;
    }

    public static String minecraftToXbl(String locale) {
        final String[] parts = locale.split("_", 2);
        if (parts.length != 2) {
            return locale;
        }
        return parts[0] + "-" + parts[1].toUpperCase(Locale.ROOT);
    }
}
