package net.godlycow.org.essc.modules.warp;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.api.impl.warp.WarpImpl;
import net.godlycow.org.essc.api.warp.event.WarpDeleteEvent;
import net.godlycow.org.essc.api.warp.event.WarpSetEvent;
import net.godlycow.org.essc.api.warp.event.WarpUpdateEvent;
import net.godlycow.org.essc.storage.database.Database;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class WarpManager {
    private final EssentialsC plugin;
    private final Database database;
    private final Map<String, Warp> warpCache = new ConcurrentHashMap<>();
    private final Map<UUID, Warp> pendingWarps = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Location> movementTracker = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> warmupTasks = new ConcurrentHashMap<>();

    public WarpManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.database = new Database(plugin, "warps.db");
        initializeDatabase();
    }

    private void initializeDatabase() {
        database.async(conn -> {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS warps (
                        name TEXT PRIMARY KEY,
                        world TEXT NOT NULL,
                        x REAL NOT NULL,
                        y REAL NOT NULL,
                        z REAL NOT NULL,
                        yaw REAL NOT NULL,
                        pitch REAL NOT NULL,
                        permission TEXT,
                        cost REAL DEFAULT 0.0,
                        hidden INTEGER DEFAULT 0,
                        description TEXT DEFAULT '',
                        category TEXT DEFAULT 'default'
                    )
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_warp_usage (
                        uuid TEXT NOT NULL,
                        warp_name TEXT NOT NULL,
                        uses INTEGER DEFAULT 0,
                        last_used INTEGER DEFAULT 0,
                        PRIMARY KEY (uuid, warp_name)
                    )
                """);
            }
            plugin.debug("Warp database initialized successfully");
            return null;
        }).thenRun(this::loadWarpsAsync).exceptionally(e -> {
            plugin.getLogger().severe("Failed to initialize warp database: " + e.getMessage());
            return null;
        });
    }

    public void setWarmupTask(UUID uuid, ScheduledTask task) {
        ScheduledTask existing = warmupTasks.put(uuid, task);
        if (existing != null && !existing.isCancelled()) {
            existing.cancel();
        }
    }

    public void cancelWarmupTask(UUID uuid) {
        ScheduledTask task = warmupTasks.remove(uuid);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    public void reload() {
        plugin.debug("Reloading warp system...");

        for (ScheduledTask task : warmupTasks.values()) {
            if (!task.isCancelled()) {
                task.cancel();
            }
        }
        warmupTasks.clear();
        pendingWarps.clear();
        movementTracker.clear();

        loadWarpsAsync();

        plugin.debug("Config - Enabled: " + plugin.getConfigManager().isWarpEnabled() +
                ", Cooldown: " + plugin.getConfigManager().getWarpCooldown() +
                "s, Warmup: " + plugin.getConfigManager().getWarpWarmup() +
                "s, CancelOnMove: " + plugin.getConfigManager().isWarpCancelOnMovement() +
                ", Particles: " + plugin.getConfigManager().isWarpParticles() +
                ", Sounds: " + plugin.getConfigManager().isWarpSounds());
    }

    private void loadWarpsAsync() {
        database.async(conn -> {
            Map<String, Warp> loaded = new HashMap<>();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM warps")) {

                while (rs.next()) {
                    String name = rs.getString("name");
                    String worldName = rs.getString("world");
                    World world = plugin.getServer().getWorld(worldName);

                    if (world == null) {
                        plugin.debug("World '" + worldName + "' not found for warp '" + name + "'");
                        continue;
                    }

                    Location loc = new Location(world,
                            rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                            rs.getFloat("yaw"), rs.getFloat("pitch"));

                    Warp warp = new Warp(name, loc);
                    warp.setPermission(rs.getString("permission"));
                    warp.setCost(rs.getDouble("cost"));
                    warp.setHidden(rs.getInt("hidden") == 1);
                    warp.setDescription(rs.getString("description"));
                    warp.setCategory(rs.getString("category"));

                    loaded.put(name.toLowerCase(), warp);
                }
            }
            return loaded;
        }).thenAccept(loaded -> {
            warpCache.clear();
            warpCache.putAll(loaded);
            plugin.debug("Loaded " + warpCache.size() + " warps from database");
        }).exceptionally(e -> {
            plugin.getLogger().severe("Failed to load warps: " + e.getMessage());
            return null;
        });
    }

    public boolean isSystemEnabled() {
        return plugin.getConfigManager().isWarpEnabled();
    }

    public CompletableFuture<Boolean> createWarp(String name, Location location) {
        if (warpCache.containsKey(name.toLowerCase())) {
            return CompletableFuture.completedFuture(false);
        }

        WarpSetEvent event = new WarpSetEvent(null, name, location);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            plugin.debug("Warp creation cancelled by WarpSetEvent: " + name);
            return CompletableFuture.completedFuture(false);
        }

        return database.async(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO warps (name, world, x, y, z, yaw, pitch, permission, cost, hidden, description, category) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

                stmt.setString(1, name);
                stmt.setString(2, location.getWorld().getName());
                stmt.setDouble(3, location.getX());
                stmt.setDouble(4, location.getY());
                stmt.setDouble(5, location.getZ());
                stmt.setFloat(6, location.getYaw());
                stmt.setFloat(7, location.getPitch());
                stmt.setNull(8, java.sql.Types.VARCHAR);
                stmt.setDouble(9, 0.0);
                stmt.setInt(10, 0);
                stmt.setString(11, "");
                stmt.setString(12, "default");
                stmt.executeUpdate();
            }

            Warp warp = new Warp(name, location);
            warpCache.put(name.toLowerCase(), warp);
            plugin.debug("Created warp: " + name);
            return true;
        }).exceptionally(e -> {
            plugin.getLogger().severe("Failed to create warp: " + e.getMessage());
            return false;
        });
    }

    public CompletableFuture<Boolean> deleteWarp(String name) {
        if (!warpCache.containsKey(name.toLowerCase())) {
            return CompletableFuture.completedFuture(false);
        }

        WarpDeleteEvent event = new WarpDeleteEvent(null, name);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            plugin.debug("Warp deletion cancelled by WarpDeleteEvent: " + name);
            return CompletableFuture.completedFuture(false);
        }

        return database.async(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM warps WHERE name = ?")) {
                stmt.setString(1, name);
                stmt.executeUpdate();
            }

            warpCache.remove(name.toLowerCase());
            plugin.debug("Deleted warp: " + name);
            return true;
        }).exceptionally(e -> {
            plugin.getLogger().severe("Failed to delete warp: " + e.getMessage());
            return false;
        });
    }

    public CompletableFuture<Boolean> updateWarp(Warp warp) {
        WarpUpdateEvent event = new WarpUpdateEvent(null, new WarpImpl(warp));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            plugin.debug("Warp update cancelled by WarpUpdateEvent: " + warp.getName());
            return CompletableFuture.completedFuture(false);
        }

        return database.async(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE warps SET world=?, x=?, y=?, z=?, yaw=?, pitch=?, permission=?, cost=?, hidden=?, description=?, category=? " +
                            "WHERE name=?")) {

                stmt.setString(1, warp.getLocation().getWorld().getName());
                stmt.setDouble(2, warp.getLocation().getX());
                stmt.setDouble(3, warp.getLocation().getY());
                stmt.setDouble(4, warp.getLocation().getZ());
                stmt.setFloat(5, warp.getLocation().getYaw());
                stmt.setFloat(6, warp.getLocation().getPitch());
                stmt.setString(7, warp.getPermission());
                stmt.setDouble(8, warp.getCost());
                stmt.setInt(9, warp.isHidden() ? 1 : 0);
                stmt.setString(10, warp.getDescription());
                stmt.setString(11, warp.getCategory());
                stmt.setString(12, warp.getName());
                stmt.executeUpdate();
            }

            warpCache.put(warp.getName().toLowerCase(), warp);
            plugin.debug("Updated warp: " + warp.getName());
            return true;
        }).exceptionally(e -> {
            plugin.getLogger().severe("Failed to update warp: " + e.getMessage());
            return false;
        });
    }

    public Warp getWarp(String name) {
        return warpCache.get(name.toLowerCase());
    }

    public Collection<Warp> getAllWarps() {
        return Collections.unmodifiableCollection(warpCache.values());
    }

    public List<Warp> getVisibleWarps() {
        return warpCache.values().stream()
                .filter(w -> !w.isHidden())
                .sorted(Comparator.comparing(Warp::getName))
                .collect(Collectors.toList());
    }

    public List<Warp> getWarpsByCategory(String category) {
        return warpCache.values().stream()
                .filter(w -> w.getCategory().equalsIgnoreCase(category))
                .sorted(Comparator.comparing(Warp::getName))
                .collect(Collectors.toList());
    }

    public Set<String> getCategories() {
        return warpCache.values().stream()
                .map(Warp::getCategory)
                .collect(Collectors.toSet());
    }

    public boolean warpExists(String name) {
        return warpCache.containsKey(name.toLowerCase());
    }

    public void setPendingWarp(UUID uuid, Warp warp) {
        pendingWarps.put(uuid, warp);
    }

    public Warp getPendingWarp(UUID uuid) {
        return pendingWarps.get(uuid);
    }

    public void removePendingWarp(UUID uuid) {
        pendingWarps.remove(uuid);
    }

    public boolean hasPendingWarp(UUID uuid) {
        return pendingWarps.containsKey(uuid);
    }

    public boolean hasAnyPendingWarp() {
        return !pendingWarps.isEmpty();
    }

    public void setCooldown(UUID uuid) {
        cooldowns.put(uuid, System.currentTimeMillis());
    }

    public long getRemainingCooldown(UUID uuid) {
        long cooldownMs = plugin.getConfigManager().getWarpCooldown() * 1000;
        Long lastUse = cooldowns.get(uuid);

        if (lastUse == null) return 0;

        long remaining = (lastUse + cooldownMs) - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }

    public void trackMovement(UUID uuid, Location location) {
        movementTracker.put(uuid, location);
    }

    public Location getTrackedLocation(UUID uuid) {
        return movementTracker.get(uuid);
    }

    public void clearMovementTrack(UUID uuid) {
        movementTracker.remove(uuid);
    }

    public void recordWarpUsage(UUID uuid, String warpName) {
        database.async(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO player_warp_usage (uuid, warp_name, uses, last_used) " +
                            "VALUES (?, ?, 1, ?) " +
                            "ON CONFLICT(uuid, warp_name) DO UPDATE SET " +
                            "uses = uses + 1, last_used = excluded.last_used")) {

                stmt.setString(1, uuid.toString());
                stmt.setString(2, warpName);
                stmt.setLong(3, System.currentTimeMillis());
                stmt.executeUpdate();
            }
            return null;
        }).exceptionally(e -> {
            plugin.debug("Failed to record warp usage: " + e.getMessage());
            return null;
        });
    }

    public CompletableFuture<Integer> getWarpUsage(UUID uuid, String warpName) {
        return database.async(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT uses FROM player_warp_usage WHERE uuid = ? AND warp_name = ?")) {

                stmt.setString(1, uuid.toString());
                stmt.setString(2, warpName);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return rs.getInt("uses");
                }
            }
            return 0;
        }).exceptionally(e -> {
            plugin.debug("Failed to get warp usage: " + e.getMessage());
            return 0;
        });
    }

    public void shutdown() {
        for (ScheduledTask task : warmupTasks.values()) {
            if (!task.isCancelled()) {
                task.cancel();
            }
        }
        warmupTasks.clear();

        plugin.debug("Shutting down the Warp Manager");
        database.disconnect();

    }
}