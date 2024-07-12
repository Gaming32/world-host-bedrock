package io.github.gaming32.worldhostbedrock.xbox.models;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.github.gaming32.worldhostbedrock.util.XUID;
import net.minecraft.Optionull;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

public record ProfileUser(
    XUID id,
    @JsonAdapter(GsonSettingsAdapter.class) Map<String, String> settings
) {
    public static final String DISPLAY_NAME_SETTING = "GameDisplayName";
    public static final String PROFILE_PICTURE_SETTING = "GameDisplayPicRaw";

    public String displayName() {
        return settings.get(DISPLAY_NAME_SETTING);
    }

    public URI profilePicture() {
        return Optionull.map(settings.get(PROFILE_PICTURE_SETTING), URI::create);
    }

    public static class GsonSettingsAdapter extends TypeAdapter<Map<String, String>> {
        @Override
        public void write(JsonWriter out, Map<String, String> value) throws IOException {
            out.beginArray();
            for (final var entry : value.entrySet()) {
                out.beginObject();
                out.name("id").value(entry.getKey());
                out.name("value").value(entry.getValue());
                out.endObject();
            }
            out.endArray();
        }

        @Override
        public Map<String, String> read(JsonReader in) throws IOException {
            final Map<String, String> result = new LinkedHashMap<>();
            in.beginArray();
            while (in.hasNext()) {
                String id = null;
                String value = null;
                in.beginObject();
                while (in.hasNext()) {
                    switch (in.nextName()) {
                        case "id" -> id = in.nextString();
                        case "value" -> value = in.nextString();
                        default -> in.skipValue();
                    }
                }
                in.endObject();
                result.put(id, value);
            }
            in.endArray();
            return result;
        }
    }
}
