package net.godlycow.org.essc.modules.home;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.storage.database.Database;

import org.bukkit.Location;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class HomeDatabase {

    private final EssentialsC plugin;
    private final Database database;

    public HomeDatabase(EssentialsC plugin) {
        this.plugin = plugin;
        this.database = new Database(plugin, "homes.db");
        try {
            database.connect();
            createTables();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize home database: " + e.getMessage());
        }
        plugin.debug("HomeRepository initialized with homes.db");
    }

    private void createTables() throws SQLException {
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS homes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT NOT NULL,
                    name TEXT NOT NULL,
                    world TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL,
                    yaw REAL NOT NULL,
                    pitch REAL NOT NULL,
                    created_at INTEGER DEFAULT (strftime('%s', 'now')),
                    UNIQUE(uuid, name)
                )
             """)) {
            stmt.execute();
        }

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "CREATE INDEX IF NOT EXISTS idx_homes_uuid ON homes(uuid)"
             )) {
            stmt.execute();
        }

        plugin.debug("Home database tables initialized.");
    }

    public CompletableFuture<Integer> getTotalHomeCount() {
        return database.async(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM homes")) {
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    public CompletableFuture<Integer> getHomeCount(UUID uuid) {
        return database.async(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM homes WHERE uuid = ?"
            )) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    public CompletableFuture<Boolean> homeExists(UUID uuid, String name) {
        return database.async(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT 1 FROM homes WHERE uuid = ? AND name = ?"
            )) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, name.toLowerCase());
                ResultSet rs = stmt.executeQuery();
                return rs.next();
            }
        });
    }

    public CompletableFuture<Boolean> save(UUID uuid, String name, Location location) {
        plugin.debug("Saving home '" + name + "' for " + uuid);

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
                stmt.setString(1, uuid.toString());
                stmt.setString(2, name.toLowerCase());
                stmt.setString(3, location.getWorld().getName());
                stmt.setDouble(4, location.getX());
                stmt.setDouble(5, location.getY());
                stmt.setDouble(6, location.getZ());
                stmt.setFloat(7, location.getYaw());
                stmt.setFloat(8, location.getPitch());
                stmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save home: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Boolean> delete(UUID uuid, String name) {
        plugin.debug("Deleting home '" + name + "' for " + uuid);

        return database.async(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM homes WHERE uuid = ? AND name = ?"
            )) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, name.toLowerCase());
                return stmt.executeUpdate() > 0;
            }
        });
    }

    public CompletableFuture<Home> findOne(UUID uuid, String name) {
        return database.async(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM homes WHERE uuid = ? AND name = ?"
            )) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, name.toLowerCase());
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? mapRow(rs) : null;
            }
        });
    }

    public CompletableFuture<List<Home>> findAll(UUID uuid) {
        return database.async(conn -> {
            List<Home> homes = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM homes WHERE uuid = ? ORDER BY name"
            )) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    homes.add(mapRow(rs));
                }
            }
            return homes;
        });
    }

    public CompletableFuture<Set<UUID>> findAllOwners() {
        return database.async(conn -> {
            Set<UUID> owners = new HashSet<>();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT DISTINCT uuid FROM homes"
            )) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    try {
                        owners.add(UUID.fromString(rs.getString("uuid")));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            return owners;
        });
    }

    private Home mapRow(ResultSet rs) throws SQLException {
        return new Home(
                UUID.fromString(rs.getString("uuid")),
                rs.getString("name"),
                rs.getString("world"),
                rs.getDouble("x"),
                rs.getDouble("y"),
                rs.getDouble("z"),
                rs.getFloat("yaw"),
                rs.getFloat("pitch"),
                rs.getLong("created_at")
        );
    }

    public Database getDatabase() {
        return database;
    }

    public void shutdown() {
        database.disconnect();
        plugin.debug("HomeRepository shutdown complete.");
    }
}