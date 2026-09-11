package net.godlycow.org.essc.modules;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.godlycow.org.essc.EssentialsC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnManager implements Listener {
    private final EssentialsC plugin;
    private final File spawnFile;
    private FileConfiguration spawnConfig;
    private Location spawnLocation;

    private final Map<UUID, Long> teleportCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> pendingTeleports = new ConcurrentHashMap<>();

    public SpawnManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.spawnFile = new File(plugin.getDataFolder(), "spawn.yml");
        loadSpawn();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.debug("SpawnManager initialized");
    }

    public void loadSpawn() {
        if (!spawnFile.exists()) {
            plugin.saveResource("spawn.yml", false);
        }
        spawnConfig = YamlConfiguration.loadConfiguration(spawnFile);

        if (spawnConfig.contains("spawn.world")) {
            String worldName = spawnConfig.getString("spawn.world");
            double x = spawnConfig.getDouble("spawn.x");
            double y = spawnConfig.getDouble("spawn.y");
            double z = spawnConfig.getDouble("spawn.z");
            float yaw = (float) spawnConfig.getDouble("spawn.yaw");
            float pitch = (float) spawnConfig.getDouble("spawn.pitch");

            if (Bukkit.getWorld(worldName) != null) {
                spawnLocation = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
                plugin.debug("Loaded spawn location: " + worldName + " (" + x + ", " + y + ", " + z + ")");
            } else {
                plugin.getLogger().warning("Spawn world '" + worldName + "' not found!");
            }
        } else {
            plugin.debug("No spawn location set yet");
        }
    }

    public void saveSpawn() {
        if (spawnLocation == null) return;

        spawnConfig.set("spawn.world", spawnLocation.getWorld().getName());
        spawnConfig.set("spawn.x", spawnLocation.getX());
        spawnConfig.set("spawn.y", spawnLocation.getY());
        spawnConfig.set("spawn.z", spawnLocation.getZ());
        spawnConfig.set("spawn.yaw", spawnLocation.getYaw());
        spawnConfig.set("spawn.pitch", spawnLocation.getPitch());

        try {
            spawnConfig.save(spawnFile);
            plugin.debug("Spawn location saved to spawn.yml");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save spawn.yml: " + e.getMessage());
        }
    }

    public void setSpawn(Location location) {
        this.spawnLocation = location.clone();
        saveSpawn();
        plugin.debug("Spawn set to: " + location.getWorld().getName() + " (" + location.getX() + ", " + location.getY() + ", " + location.getZ() + ")");
    }

    public Location getSpawn() {
        return spawnLocation == null ? null : spawnLocation.clone();
    }

    public boolean isSpawnSet() {
        return spawnLocation != null && spawnLocation.getWorld() != null;
    }

    public boolean isOnCooldown(Player player) {
        if (player.hasPermission("essentialsc.spawn.admin")) return false;

        long cooldown = plugin.getConfigManager().getSpawnCooldown();
        if (cooldown <= 0) return false;

        Long last = teleportCooldowns.get(player.getUniqueId());
        if (last == null) return false;

        return System.currentTimeMillis() - last < cooldown * 1000;
    }

    public long getRemainingCooldown(Player player) {
        long cooldown = plugin.getConfigManager().getSpawnCooldown();
        long last = teleportCooldowns.getOrDefault(player.getUniqueId(), 0L);
        return Math.max(0, (last + (cooldown * 1000) - System.currentTimeMillis()) / 1000);
    }

    public void teleportToSpawn(Player player) {
        teleportToSpawn(player, false);
    }

    public void teleportToSpawn(Player player, boolean bypassWarmup) {
        if (!isSpawnSet()) {
            player.sendMessage(plugin.getLanguageManager().get(player, "spawn.not_set"));
            plugin.debug("Teleport failed: spawn not set");
            return;
        }

        if (pendingTeleports.containsKey(player.getUniqueId())) {
            player.sendMessage(plugin.getLanguageManager().get(player, "spawn.already_pending"));
            return;
        }

        if (isOnCooldown(player)) {
            player.sendMessage(plugin.getLanguageManager().get(player, "spawn.cooldown",
                    Map.of("seconds", String.valueOf(getRemainingCooldown(player)))));
            return;
        }

        Location spawn = getSpawn();
        long warmup = bypassWarmup ? 0 : plugin.getConfigManager().getSpawnWarmup();

        if (warmup > 0 && !player.hasPermission("essentialsc.spawn.admin")) {
            player.sendMessage(plugin.getLanguageManager().get(player, "spawn.pending",
                    Map.of("seconds", String.valueOf(warmup))));
            plugin.debug("Starting spawn warmup for " + player.getName() + " (" + warmup + "s)");

            ScheduledTask task = player.getScheduler().runDelayed(plugin, task1 -> {
                completeTeleport(player, spawn);
            }, null, warmup * 20L);

            pendingTeleports.put(player.getUniqueId(), task);
        } else {
            completeTeleport(player, spawn);
        }
    }

    private void completeTeleport(Player player, Location location) {
        pendingTeleports.remove(player.getUniqueId());

        plugin.teleportHelper().teleportAsync(player, location).thenAccept(success -> {
            if (!success) return;
            teleportCooldowns.put(player.getUniqueId(), System.currentTimeMillis());

            if (plugin.getUserManager() != null) {
                plugin.getUserManager().setSpawnLastTeleport(player.getUniqueId(), System.currentTimeMillis() / 1000L);
            }

            player.sendMessage(plugin.getLanguageManager().get(player, "spawn.success"));

            if (plugin.getConfigManager().isSpawnParticles()) {
                location.getWorld().spawnParticle(Particle.PORTAL, location, 50, 0.5, 1, 0.5);
            }
            if (plugin.getConfigManager().isSpawnSounds()) {
                player.playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }

            plugin.debug("Teleported " + player.getName() + " to spawn");
        });
    }

    public void cancelTeleport(Player player) {
        ScheduledTask task = pendingTeleports.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            plugin.debug("Cancelled spawn teleport for " + player.getName());
        }
    }

    public boolean hasPendingTeleport(Player player) {
        return pendingTeleports.containsKey(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfigManager().isSpawnFirstJoin()) return;

        Player player = event.getPlayer();
        if (!player.hasPlayedBefore() && isSpawnSet()) {
            plugin.debug("First join teleport for " + player.getName());
            player.getScheduler().runDelayed(plugin, task -> {
                if (player.isOnline()) {
                    plugin.teleportHelper().teleportAsync(player, getSpawn());
                }
            }, null, 1L);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!plugin.getConfigManager().isSpawnTeleportOnRespawn()) {
            return;
        }

        Player player = event.getPlayer();

        if (plugin.getConfigManager().isSpawnAllowBedsToOverride()
                && (event.isBedSpawn() || event.isAnchorSpawn())) {
            plugin.debug("Player " + player.getName() + " respawning at bed/anchor (override enabled)");
            return;
        }

        if (isSpawnSet()) {
            event.setRespawnLocation(getSpawn());
            plugin.debug("Teleported " + player.getName() + " to spawn on respawn");
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getConfigManager().isSpawnCancelOnMovement()) return;

        Player player = event.getPlayer();
        if (!hasPendingTeleport(player)) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) return;
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        cancelTeleport(player);
        player.sendMessage(plugin.getLanguageManager().get(player, "spawn.cancelled"));
        plugin.debug("Cancelled spawn teleport for " + player.getName() + " due to movement");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cancelTeleport(event.getPlayer());
    }

    public void reload() {
        loadSpawn();
        plugin.debug("Spawn configuration reloaded");
    }

    public void shutdown() {
        pendingTeleports.values().forEach(ScheduledTask::cancel);
        pendingTeleports.clear();
        teleportCooldowns.clear();
        HandlerList.unregisterAll(this);
        plugin.debug("Shutting down the Spawn Manager");
    }
}