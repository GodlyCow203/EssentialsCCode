package net.godlycow.org.essc.migration.saver;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.migration.resolver.ConflictResolver;
import net.godlycow.org.essc.migration.storage.OfflineHome;
import net.godlycow.org.essc.migration.mapper.UserDataMapper.HomeTransfer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Home {
    private final EssentialsC plugin;
    private final ConflictResolver conflictResolver;
    private final OfflineHome offlineStorage;
    private final Map<UUID, String> usernameCache = new HashMap<>();

    public Home(EssentialsC plugin, ConflictResolver conflictResolver) {
        this.plugin = plugin;
        this.conflictResolver = conflictResolver;
        this.offlineStorage = new OfflineHome(plugin);
    }

    public CompletableFuture<HomeBatchResult> writeAll(UUID owner, List<HomeTransfer> homes, boolean dryRun) {

        if (plugin.getHomeManager() == null) {
            return CompletableFuture.completedFuture(new HomeBatchResult(0, 0, List.of("Home manager not available")));
        }

        String ownerName = getUsername(owner);

        List<String> warnings = new ArrayList<>();
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for (HomeTransfer home : homes) {
            CompletableFuture<Boolean> future = conflictResolver.resolveHomeConflict(owner, home.name())
                    .thenCompose(resolution -> {
                        String targetName = resolution.newName() != null ? resolution.newName() : home.name();

                        return switch (resolution.action()) {
                            case SKIP -> {
                                warnings.add("Skipped home '" + home.name() + "' - already exists");
                                yield CompletableFuture.completedFuture(false);
                            }
                            case ABORT -> throw new RuntimeException("Conflict abort requested for home: " + home.name());
                            case OVERWRITE, CREATE -> writeHome(owner, home, targetName, ownerName, dryRun);
                        };
                    });
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    long successCount = futures.stream().filter(f -> {
                        try { return f.get(); } catch (Exception e) { return false; }
                    }).count();
                    return new HomeBatchResult((int) successCount, homes.size() - (int) successCount, warnings);
                });
    }

    private CompletableFuture<Boolean> writeHome(UUID owner, HomeTransfer home, String targetName, String ownerName, boolean dryRun) {
        if (dryRun) {
            plugin.debug("DRY RUN: Would create home '" + targetName + "' for " + owner + " (offline: " + !isPlayerOnline(owner) + ")");
            return CompletableFuture.completedFuture(true);
        }
        if (offlineStorage.isAvailable()) {
            return offlineStorage.setHomeOffline(owner, targetName, home.location(), ownerName);
        }

        org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(owner);
        if (offlinePlayer.isOnline() && offlinePlayer.getPlayer() != null) {
            return plugin.getHomeManager().setHome(offlinePlayer.getPlayer(), targetName, home.location());
        }

        plugin.debug("CRITICALL: Cannot set home for " + owner + " - no storage method available");
        return CompletableFuture.completedFuture(false);
    }

    private boolean isPlayerOnline(UUID uuid) {
        return plugin.getServer().getPlayer(uuid) != null;
    }

    private String getUsername(UUID uuid) {
        if (usernameCache.containsKey(uuid)) {
            return usernameCache.get(uuid);
        }

        org.bukkit.OfflinePlayer player = plugin.getServer().getOfflinePlayer(uuid);
        String name = player.getName();
        if (name != null) {
            usernameCache.put(uuid, name);
        }
        return name != null ? name : uuid.toString().substring(0, 8);
    }

    public void shutdown() {
        offlineStorage.shutdown();
    }

    public record HomeBatchResult(int migrated, int skipped, List<String> warnings) {}
}