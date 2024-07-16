package io.github.gaming32.worldhostbedrock.connect;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.Util;
import org.jetbrains.annotations.Nullable;

public enum NetworkConnectionType {
    Undefined(-1),
    Local(0),
    IPv4(1),
    IPv6(2),
    WebRTCSignalingUsingWebSockets(3),
    NAT(5),
    UPNP(6),
    UnknownIP(7);

    private static final Int2ObjectMap<NetworkConnectionType> BY_ID = Util.make(new Int2ObjectOpenHashMap<>(), map -> {
        for (final NetworkConnectionType type : values()) {
            map.put(type.id, type);
        }
        map.trim();
    });

    private final int id;

    NetworkConnectionType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Nullable
    public static NetworkConnectionType byId(int id) {
        return BY_ID.get(id);
    }
}
