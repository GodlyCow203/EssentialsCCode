package net.godlycow.org.essc.modules.home;

import org.bukkit.Location;

import java.util.UUID;

public class Home {
    private final UUID owner;
    private final String name;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final long createdAt;

    public Home(UUID owner, String name, Location location, long createdAt) {
        this.owner = owner;
        this.name = name.toLowerCase();
        this.world = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.createdAt = createdAt;
    }

    public Home(UUID owner, String name, String world, double x, double y, double z, float yaw, float pitch, long createdAt) {
        this.owner = owner;
        this.name = name.toLowerCase();
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.createdAt = createdAt;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Location toLocation(org.bukkit.Server server) {
        org.bukkit.World w = server.getWorld(world);
        if (w == null) return null;
        return new Location(w, x, y, z, yaw, pitch);
    }
}