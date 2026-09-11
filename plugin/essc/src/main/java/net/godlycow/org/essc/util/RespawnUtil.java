package net.godlycow.org.essc.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class RespawnUtil {

    private static volatile Method respawnLocationMethod;

    private RespawnUtil() {
    }

    public static Location getBedSpawnLocation(Player player) {
        try {
            return player.getBedSpawnLocation();
        } catch (UnsupportedOperationException e) {
            return getStoredRespawnLocation(player);
        }
    }

    private static Location getStoredRespawnLocation(Player player) {
        try {
            Method method = respawnLocationMethod;
            if (method == null) {
                method = player.getClass().getMethod("getRespawnLocation", boolean.class);
                respawnLocationMethod = method;
            }
            return (Location) method.invoke(player, Boolean.FALSE);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
