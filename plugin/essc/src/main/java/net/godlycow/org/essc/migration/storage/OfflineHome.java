package net.godlycow.org.essc.migration.storage;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.storage.database.Database;
import org.bukkit.Location;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class OfflineHome {
    private final EssentialsC plugin;
    private final Database database;

    public OfflineHome(EssentialsC plugin) {
        this.plugin = plugin;
        this.database = plugin.getHomeManager().getDatabase();
    }

    public CompletableFuture<Boolean> setHomeOffline(UUID owner, String name, Location location, String ownerName) {
        return database.async(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO homes (uuid, name, world, x, y, z, yaw, pitch)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid, name) DO UPDATE SET
                    world = excluded.world,
                    x = excluded.x,
                    y = excluded.y,
                    z = excluded.z,
                    yaw = excluded.yaw,
                    pitch = excluded.pitch,
                    created_at = strftime('%s', 'now')
            """)) {
                stmt.setString(1, owner.toString());
                stmt.setString(2, name.toLowerCase());
                stmt.setString(3, location.getWorld().getName());
                stmt.setDouble(4, location.getX());
                stmt.setDouble(5, location.getY());
                stmt.setDouble(6, location.getZ());
                stmt.setFloat(7, location.getYaw());
                stmt.setFloat(8, location.getPitch());
                stmt.executeUpdate();

                plugin.debug("Set home '" + name + "' for offline player " + owner);
                return true;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to set offline home: " + e.getMessage());
                return false;
            }
        });
    }

    public boolean isAvailable() {
        return plugin.getHomeManager() != null;
    }

    public void shutdown() {
    }
}