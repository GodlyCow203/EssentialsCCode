package net.godlycow.org.essc.modules.fly;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.storage.user.UserProfile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FlyMigration {
    private final EssentialsC plugin;
    private final File dataFile;

    public FlyMigration(EssentialsC plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "flying_players.json");
    }

    public void runIfNeeded() {
        if (!dataFile.exists()) return;

        Set<UUID> uuids = readFile();
        if (uuids.isEmpty()) {
            markMigrated();
            return;
        }

        plugin.getLogger().info("[FlyMigration] Migrating " + uuids.size() + " flying player(s) to the user database...");

        CompletableFuture<?>[] futures = uuids.stream()
                .map(uuid -> migratePlayer(uuid))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).thenRun(() -> {
            markMigrated();
            plugin.getLogger().info("[FlyMigration] Migration complete.");
        }).exceptionally(ex -> {
            plugin.getLogger().warning("[FlyMigration] Migration encountered errors: " + ex.getMessage());
            return null;
        });
    }

    private CompletableFuture<Void> migratePlayer(UUID uuid) {
        UserProfile cached = plugin.getUserManager().getCachedProfile(uuid);
        if (cached != null) {
            cached.setFlyEnabled(true);
            return plugin.getUserManager().saveAsync(cached).thenApply(r -> null);
        }

        return plugin.getUserManager().findProfile(uuid).thenCompose(profile -> {
            if (profile != null) {
                profile.setFlyEnabled(true);
                profile.setUpdatedAt(System.currentTimeMillis() / 1000L);
                return plugin.getUserManager().saveAsync(profile).thenApply(r -> null);
            }
            plugin.debug("[FlyMigration] No user row found for " + uuid + ", skipping.");
            return CompletableFuture.completedFuture(null);
        });
    }

    private Set<UUID> readFile() {
        Set<UUID> uuids = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                try {
                    uuids.add(UUID.fromString(trimmed));
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (IOException e) {
            plugin.getLogger().warning("[FlyMigration] Could not read flying_players.json: " + e.getMessage());
        }
        return uuids;
    }

    private void markMigrated() {
        File migrated = new File(plugin.getDataFolder(), "flying_players.json.migrated");
        boolean renamed = dataFile.renameTo(migrated);
        if (!renamed) {
            plugin.getLogger().warning("[FlyMigration] Could not rename flying_players.json — please delete it manually.");
        } else {
            plugin.debug("[FlyMigration] Renamed flying_players.json to flying_players.json.migrated");
        }
    }
}
