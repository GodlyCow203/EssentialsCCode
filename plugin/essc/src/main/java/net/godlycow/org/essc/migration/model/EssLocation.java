package net.godlycow.org.essc.migration.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import java.util.UUID;

public record EssLocation(
        UUID world,
        String worldName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {
    public Location toBukkitLocation() {
        World bukkitWorld = null;
        if (world != null) {
            bukkitWorld = Bukkit.getWorld(world);
        }

        if (bukkitWorld == null && worldName != null && !worldName.isEmpty()) {
            bukkitWorld = Bukkit.getWorld(worldName);
        }

        if (bukkitWorld == null && worldName != null) {
            for (World w : Bukkit.getWorlds()) {
                if (w.getName().equalsIgnoreCase(worldName)) {
                    bukkitWorld = w;
                    break;
                }
            }
        }

        if (bukkitWorld == null) return null;

        return new Location(bukkitWorld, x, y, z, yaw, pitch);
    }
}