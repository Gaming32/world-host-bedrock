package io.github.gaming32.worldhostbedrock.xbox;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import io.github.gaming32.worldhostbedrock.xbox.models.ProfileUser;
import io.github.gaming32.worldhostbedrock.xbox.models.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RequestParsers {
    public static final Gson GSON = new GsonBuilder().create();

    public static List<Session> parseSessions(JsonReader reader) throws IOException {
        final List<Session> result = new ArrayList<>();
        reader.beginObject();
        while (reader.hasNext()) {
            if (!reader.nextName().equals("results")) {
                reader.skipValue();
                continue;
            }
            reader.beginArray();
            while (reader.hasNext()) {
                final Session session = GSON.fromJson(reader, Session.class);
                if (session.isMinecraft()) {
                    result.add(session);
                }
            }
            reader.endArray();
        }
        reader.endObject();
        return result;
    }

    public static List<ProfileUser> parseProfileUsers(JsonReader reader) throws IOException {
        final List<ProfileUser> result = new ArrayList<>();
        reader.beginObject();
        while (reader.hasNext()) {
            if (!reader.nextName().equals("profileUsers")) {
                reader.skipValue();
                continue;
            }
            reader.beginArray();
            while (reader.hasNext()) {
                result.add(GSON.fromJson(reader, ProfileUser.class));
            }
            reader.endArray();
        }
        reader.endObject();
        return result;
    }
}
