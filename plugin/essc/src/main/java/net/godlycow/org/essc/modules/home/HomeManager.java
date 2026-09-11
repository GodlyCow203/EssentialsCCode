package net.godlycow.org.essc.modules.home;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.api.home.event.HomeDeleteEvent;
import net.godlycow.org.essc.api.home.event.HomeSetEvent;
import net.godlycow.org.essc.storage.database.Database;
import net.godlycow.org.essc.util.RespawnUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class HomeManager {

    private final EssentialsC plugin;
    private final HomeDatabase repository;
    private final TeleportHandler teleportHandler;
    private final Map<UUID, Set<String>> homeNameCache = new ConcurrentHashMap<>();

    public HomeManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.repository = new HomeDatabase(plugin);
        this.teleportHandler = new TeleportHandler(plugin, repository);
        repository.getTotalHomeCount().thenAccept(count -> plugin.debug("HomeManager initialized (" + count + " homes)"));
    }

    public int getMaxHomes(Player player) {
        if (player.hasPermission("essentialsc.sethome.admin")
                || player.hasPermission("essentialsc.sethome.unlimited")) {
            return Integer.MAX_VALUE;
        }

        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("essentialsc.sethome." + i)) {
                return i;
            }
        }

        return plugin.getConfigManager().getMaxHomes();
    }

    public CompletableFuture<Integer> getHomeCount(UUID uuid) {
        return repository.getHomeCount(uuid);
    }

    public CompletableFuture<Integer> getEffectiveHomeCount(Player player) {
        return repository.getHomeCount(player.getUniqueId()).thenApply(count -> {
            if (player.hasPermission("essentialsc.home.bed")
                    && plugin.getConfigManager().isBedHomeCountsInLimit()
                    && RespawnUtil.getBedSpawnLocation(player) != null) {
                return count + 1;
            }
            return count;
        });
    }

    public CompletableFuture<Boolean> homeExists(UUID uuid, String name) {
        return repository.homeExists(uuid, name);
    }

    public CompletableFuture<Boolean> setHome(Player player, String name, Location location) {
        HomeSetEvent event = new HomeSetEvent(player, name, location);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return CompletableFuture.completedFuture(false);
        }
        return repository.save(player.getUniqueId(), name, location).whenComplete((result, err) -> {
            if (result != null && result) {
                homeNameCache.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet()).add(name.toLowerCase());
            }
        });
    }

    public CompletableFuture<Boolean> setHome(UUID uuid, String name, Location location) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            HomeSetEvent event = new HomeSetEvent(player, name, location);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return CompletableFuture.completedFuture(false);
            }
        }
        return repository.save(uuid, name, location).whenComplete((result, err) -> {
            if (result != null && result) {
                homeNameCache.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(name.toLowerCase());
            }
        });
    }

    public CompletableFuture<Boolean> deleteHome(UUID uuid, String name) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            HomeDeleteEvent event = new HomeDeleteEvent(player, name);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return CompletableFuture.completedFuture(false);
            }
        }
        return repository.delete(uuid, name).whenComplete((result, err) -> {
            Set<String> cached = homeNameCache.get(uuid);
            if (cached != null) {
                cached.remove(name.toLowerCase());
            }
        });
    }

    public CompletableFuture<Home> getHome(UUID uuid, String name) {
        return repository.findOne(uuid, name);
    }

    public CompletableFuture<List<Home>> getHomes(UUID uuid) {
        return repository.findAll(uuid).whenComplete((homes, err) -> {
            if (homes != null) {
                Set<String> names = ConcurrentHashMap.newKeySet();
                for (Home home : homes) {
                    names.add(home.getName().toLowerCase());
                }
                homeNameCache.put(uuid, names);
            }
        });
    }

    public Set<String> getCachedHomeNames(UUID uuid) {
        return homeNameCache.getOrDefault(uuid, Collections.emptySet());
    }

    public void clearCache(UUID uuid) {
        homeNameCache.remove(uuid);
    }

    public CompletableFuture<Set<UUID>> getAllHomeOwners() {
        return repository.findAllOwners();
    }

    public boolean isOnCooldown(Player player) {
        return teleportHandler.isOnCooldown(player);
    }

    public long getRemainingCooldown(Player player) {
        return teleportHandler.getRemainingCooldown(player);
    }

    public boolean hasPendingTeleport(Player player) {
        return teleportHandler.hasPendingTeleport(player);
    }

    public void cancelTeleport(Player player) {
        teleportHandler.cancelTeleport(player);
    }

    public void startTeleport(Player player, Home home) {
        teleportHandler.startTeleport(player, home);
    }

    public Database getDatabase() {
        return repository.getDatabase();
    }

    public void reload() {
        homeNameCache.clear();
        plugin.debug("HomeManager configuration reloaded.");
    }

    public void shutdown() {
        teleportHandler.shutdown();
        repository.shutdown();
        homeNameCache.clear();
        plugin.debug("HomeManager shutdown complete.");
    }
}