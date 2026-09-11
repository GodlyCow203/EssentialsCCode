package net.godlycow.org.essc.integration.bedrock;

import java.util.UUID;

public final class TeamNameUtil {

    private static final String PREFIX = "ec_";
    private static final int HEX_CHARS = 13;

    private TeamNameUtil() {
    }
    public static String fromUUID(UUID uuid) {
        String hex = uuid.toString().replace("-", "");
        return PREFIX + hex.substring(0, HEX_CHARS);
    }
}
