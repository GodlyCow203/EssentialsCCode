package net.godlycow.org.essc.modules.punishment;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.storage.user.UserProfile;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class IpHistoryMigration {
    private final EssentialsC plugin;
    private final File dataFile;

    public IpHistoryMigration(EssentialsC plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "ip-history.yml");
    }

    public void runIfNeeded() {
        if (!dataFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

        if (!config.contains("players")) {
            markMigrated();
            return;
        }

        List<IpEntry> entries = new ArrayList<>();
        for (String key : config.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String ip = config.getString("players." + key + ".ip");
                if (ip != null && !ip.isBlank()) {
                    entries.add(new IpEntry(uuid, ip));
                }
            } catch (IllegalArgumentException ignored) {}
        }

        if (entries.isEmpty()) {
            markMigrated();
            return;
        }

        plugin.getLogger().info("[IpHistoryMigration] Migrating " + entries.size() + " IP record(s) to the user database...");

        CompletableFuture<?>[] futures = entries.stream()
                .map(entry -> migrateEntry(entry))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).thenRun(() -> {
            markMigrated();
            plugin.getLogger().info("[IpHistoryMigration] Migration complete.");
        }).exceptionally(ex -> {
            plugin.getLogger().warning("[IpHistoryMigration] Migration encountered errors: " + ex.getMessage());
            return null;
        });
    }

    private CompletableFuture<Void> migrateEntry(IpEntry entry) {
        UserProfile cached = plugin.getUserManager().getCachedProfile(entry.uuid());
        if (cached != null) {
            if (cached.getLastIp() == null) {
                cached.setLastIp(entry.ip());
            }
            return plugin.getUserManager().saveAsync(cached)
                    .thenApply(r -> null);
        }

        return plugin.getUserManager().recordIp(entry.uuid(), entry.ip()).thenApply(r -> null);
    }

    private void markMigrated() {
        File migrated = new File(plugin.getDataFolder(), "ip-history.yml.migrated");
        boolean renamed = dataFile.renameTo(migrated);
        if (!renamed) {
            plugin.getLogger().warning("[IpHistoryMigration] Could not rename ip-history.yml — please delete it manually.");
        } else {
            plugin.debug("[IpHistoryMigration] Renamed ip-history.yml to ip-history.yml.migrated");
        }
    }

    private record IpEntry(UUID uuid, String ip) {}
}
