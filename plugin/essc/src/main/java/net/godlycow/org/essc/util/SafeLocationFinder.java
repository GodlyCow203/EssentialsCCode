package net.godlycow.org.essc.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public final class SafeLocationFinder {

    private static final int HORIZONTAL_RADIUS = 4;
    private static final int VERTICAL_RADIUS = 8;

    private SafeLocationFinder() {}

    public static Location findSafe(Location origin) {
        if (origin == null || origin.getWorld() == null) {
            return null;
        }

        World world = origin.getWorld();
        int originX = origin.getBlockX();
        int originY = origin.getBlockY();
        int originZ = origin.getBlockZ();

        int minY = world.getMinHeight() + 1;
        int maxY = world.getMaxHeight() - 2;

        if (isSafeAt(world, originX, originY, originZ)) {
            return centeredLocation(world, originX, originY, originZ, origin);
        }

        for (int radius = 1; radius <= HORIZONTAL_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }

                    int x = originX + dx;
                    int z = originZ + dz;

                    for (int dy = 0; dy <= VERTICAL_RADIUS; dy++) {
                        int yUp = originY + dy;
                        int yDown = originY - dy;

                        if (yUp <= maxY && isSafeAt(world, x, yUp, z)) {
                            return centeredLocation(world, x, yUp, z, origin);
                        }

                        if (dy != 0 && yDown >= minY && isSafeAt(world, x, yDown, z)) {
                            return centeredLocation(world, x, yDown, z, origin);
                        }
                    }
                }
            }
        }

        for (int dy = 1; dy <= VERTICAL_RADIUS; dy++) {
            int yUp = originY + dy;
            int yDown = originY - dy;

            if (yUp <= maxY && isSafeAt(world, originX, yUp, originZ)) {
                return centeredLocation(world, originX, yUp, originZ, origin);
            }

            if (yDown >= minY && isSafeAt(world, originX, yDown, originZ)) {
                return centeredLocation(world, originX, yDown, originZ, origin);
            }
        }

        return null;
    }

    private static Location centeredLocation(World world, int x, int y, int z, Location origin) {
        return new Location(world, x + 0.5, y, z + 0.5, origin.getYaw(), origin.getPitch());
    }

    private static boolean isSafeAt(World world, int x, int y, int z) {
        Material ground = world.getBlockAt(x, y - 1, z).getType();
        Material feet = world.getBlockAt(x, y, z).getType();
        Material head = world.getBlockAt(x, y + 1, z).getType();

        return isSafeGround(ground) && isSafePassable(feet) && isSafePassable(head);
    }

    private static boolean isSafeGround(Material material) {
        if (!material.isSolid()) {
            return false;
        }

        return material != Material.LAVA
                && material != Material.CACTUS
                && material != Material.FIRE
                && material != Material.MAGMA_BLOCK
                && material != Material.WITHER_ROSE
                && material != Material.SWEET_BERRY_BUSH
                && material != Material.SOUL_SAND
                && material != Material.POWDER_SNOW;
    }

    private static boolean isSafePassable(Material material) {
        if (material.isSolid()) {
            return false;
        }

        return material != Material.LAVA
                && material != Material.FIRE
                && material != Material.WATER;
    }

    public static boolean isSafe(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        return isSafeAt(world, location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}