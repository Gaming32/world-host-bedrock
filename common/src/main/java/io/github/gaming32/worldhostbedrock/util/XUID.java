package io.github.gaming32.worldhostbedrock.util;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.UUID;

@JsonAdapter(XUID.GsonAdapter.class)
public record XUID(long id) {
    public static XUID parse(String s) {
        return new XUID(Long.parseUnsignedLong(s));
    }

    public static XUID parseHex(String s) {
        return new XUID(Long.parseUnsignedLong(s, 16));
    }

    public UUID toUuid() {
        return new UUID(0, id);
    }

    @Override
    public String toString() {
        return Long.toUnsignedString(id);
    }

    public static class GsonAdapter extends TypeAdapter<XUID> {
        @Override
        public void write(JsonWriter out, XUID value) throws IOException {
            out.value(value.toString());
        }

        @Override
        public XUID read(JsonReader in) throws IOException {
            try {
                return parse(in.nextString());
            } catch (NumberFormatException e) {
                throw new JsonParseException(e);
            }
        }
    }
}
