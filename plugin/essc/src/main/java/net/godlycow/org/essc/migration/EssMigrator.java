package net.godlycow.org.essc.migration;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.migration.model.EssUserData;
import net.godlycow.org.essc.migration.model.EssWarp;
import net.godlycow.org.essc.migration.loader.UserDataLoader;
import net.godlycow.org.essc.migration.loader.WarpLoader;
import net.godlycow.org.essc.migration.resolver.ConflictResolver;
import net.godlycow.org.essc.migration.resolver.MigrationValidator;
import net.godlycow.org.essc.migration.resolver.MigrationValidator.ValidationReport;
import net.godlycow.org.essc.migration.mapper.UserDataMapper;
import net.godlycow.org.essc.migration.mapper.WarpMapper;
import net.godlycow.org.essc.migration.saver.*;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.entity.Player;

public class EssMigrator {
    private final EssentialsC plugin;
    private final File essentialsDataFolder;
    private final File serverRootFolder;
    private final UserDataLoader userReader;
    private final WarpLoader warpReader;
    private final UserDataMapper userTransformer;
    private final WarpMapper warpTransformer;
    private final MigrationValidator validator;
    private final MigrationProgress MigrationProgress;
    private final Economy economyWriter;
    private final Home homeWriter;
    private final Nick nicknameWriter;
    private final Warp warpWriter;
    private final Punishment punishmentWriter;

    private final MigrationResult stats;

    public EssMigrator(EssentialsC plugin, File essentialsDataFolder) {
        this.plugin = plugin;
        this.essentialsDataFolder = essentialsDataFolder;
        this.serverRootFolder = plugin.getDataFolder().getParentFile().getParentFile();
        this.userReader = new UserDataLoader();
        this.warpReader = new WarpLoader();
        this.userTransformer = new UserDataMapper(plugin);
        this.warpTransformer = new WarpMapper();
        this.validator = new MigrationValidator(plugin, essentialsDataFolder);
        this.MigrationProgress = new MigrationProgress();
        this.stats = new MigrationResult();



        ConflictResolver conflictResolver = new ConflictResolver(plugin, Options.ConflictStrategy.SKIP);
        this.economyWriter = new Economy(plugin);
        this.homeWriter = new Home(plugin, conflictResolver);
        this.nicknameWriter = new Nick(plugin, conflictResolver);
        this.warpWriter = new Warp(plugin, conflictResolver);
        this.punishmentWriter = new Punishment(plugin);
    }

    public CompletableFuture<MigrationResult> migrate(Options options) {
        return CompletableFuture.supplyAsync(() -> {
            plugin.debug("Starting migration with options: " + options);
            MigrationProgress.setStage("Validating");


            ValidationReport validation = validator.validate(options).join();
            if (!validation.isValid()) {
                throw new MigrationException("Validation failed: " + String.join(", ", validation.getErrors()));
            }

            MigrationProgress.setTotalUsers(validation.getUserCount());
            MigrationProgress.setTotalWarps(validation.getWarpCount());



            validation.getWarnings().forEach(warning -> {
                plugin.getLogger().warning("[Migration] " + warning);
                stats.addWarning(warning);
            });

            if (options.dryRun()) {
                plugin.getLogger().info("=== DRY RUN MODE - No changes will be made ===");
            }

            MigrationProgress.setStage("Migrating Warps");
            if (options.importWarps()) {
                migrateWarps(options);
            }

            MigrationProgress.setStage("Migrating Users");
            if (options.importUsers()) {
                migrateUsers(options);
            }

            MigrationProgress.setStage("Migrating Bans");
            if (options.importBans()) {
                migrateBans(options);
            }

            MigrationProgress.setStage("Complete");

            plugin.debug("Migration complete: " + stats);
            return stats;
        });
    }

    public MigrationProgress getProgress() {
        return MigrationProgress;
    }

    private void migrateWarps(Options options) {
        File warpsDir = new File(essentialsDataFolder, "warps");
        if (!warpsDir.exists() || !warpsDir.isDirectory()) {
            plugin.getLogger().warning("Warps directory not found: " + warpsDir.getPath());
            return;
        }

        File[] warpFiles = warpsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (warpFiles == null) return;

        plugin.debug("Found " + warpFiles.length + " warps to migrate");

        for (File file : warpFiles) {
            try {
                EssWarp warp = warpReader.read(file);
                if (warp == null) {
                    plugin.debug("Failed to read warp file: " + file.getName());
                    stats.warpsFailed.incrementAndGet();
                    continue;
                }

                var transfer = warpTransformer.transform(warp);
                if (transfer == null) {
                    plugin.debug("Failed to transform warp: " + warp.name());
                    stats.warpsFailed.incrementAndGet();
                    continue;
                }

                var result = warpWriter.write(transfer, options.dryRun()).join();
                MigrationProgress.incrementWarps();

                if (result.success()) {
                    stats.warpsMigrated.incrementAndGet();
                    plugin.debug("Migrated warp: " + warp.name());
                } else {
                    stats.warpsFailed.incrementAndGet();
                    if (result.error() != null && !result.error().contains("Skipped")) {
                        plugin.debug("Failed to migrate warp " + warp.name() + ": " + result.error());
                    }
                }

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to migrate warp " + file.getName() + ": " + e.getMessage());
                stats.warpsFailed.incrementAndGet();
            }
        }
    }

    private void migrateUsers(Options options) {
        File usersDir = new File(essentialsDataFolder, "userdata");
        if (!usersDir.exists() || !usersDir.isDirectory()) {
            plugin.getLogger().warning("Userdata directory not found: " + usersDir.getPath());
            return;
        }

        File[] userFiles = usersDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (userFiles == null) return;

        plugin.debug("Found " + userFiles.length + " users to migrate");

        for (File file : userFiles) {
            try {
                EssUserData userData = userReader.read(file);
                if (userData == null) {
                    plugin.debug("Failed to read user file: " + file.getName());
                    stats.usersFailed.incrementAndGet();
                    continue;
                }

                migrateSingleUser(userData, options);
                MigrationProgress.incrementUsers();
                stats.usersMigrated.incrementAndGet();

            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                plugin.getLogger().warning("Failed to migrate user " + file.getName() + ": " + cause);
                plugin.debug("Stack trace for " + file.getName() + ":");
                for (StackTraceElement frame : cause.getStackTrace()) {
                    plugin.debug("  at " + frame);
                }
                stats.usersFailed.incrementAndGet();
            }
        }
    }

    private void migrateSingleUser(EssUserData data, Options options) {

        if (options.importEconomy() && plugin.getEconomyManager() != null) {
            var econData = userTransformer.transformEconomy(data);
            var result = economyWriter.write(econData, options.dryRun()).join();

            if (result.success()) {
                stats.economyRecords.incrementAndGet();
                plugin.debug("Migrated economy for " + data.lastAccountName() + ": " + data.money());
            } else if (result.error() != null) {
                stats.addWarning("Economy failed for " + data.lastAccountName() + ": " + result.error());

            }
        }

        if (options.importHomes() && plugin.getHomeManager() != null) {
            var homes = userTransformer.transformHomes(data);

            var result = homeWriter.writeAll(data.uuid(), homes, options.dryRun()).join();
            stats.homesMigrated.addAndGet(result.migrated());
            stats.homesSkipped.addAndGet(result.skipped());

            if (!result.warnings().isEmpty()) {
                result.warnings().forEach(stats::addWarning);
            }

            if (result.migrated() > 0) {
                plugin.debug("Migrated " + result.migrated() + " homes for " + data.lastAccountName());
            }
        }


        if (options.importNicks() && plugin.getNickManager() != null) {
            var nickData = userTransformer.transformNickname(data);
            if (nickData != null) {
                var result = nicknameWriter.write(nickData, options.dryRun()).join();
                if (result.success()) {
                    stats.nicknamesMigrated.incrementAndGet();
                    plugin.debug("Migrated nickname for " + data.lastAccountName() + ": " + nickData.nickname());
                } else if (result.error() != null && !result.error().contains("Skipped")) {
                    stats.addWarning("Nickname failed for " + data.lastAccountName() + ": " + result.error());
                }
            }
        }

        if (options.importMutes() && plugin.getPunishmentManager() != null) {
            var muteData = userTransformer.transformMute(data);
            if (muteData != null) {

                var result = punishmentWriter.writeMute(muteData, options.dryRun()).join();
                if (result.success()) {
                    stats.mutesMigrated.incrementAndGet();
                    plugin.debug("Migrated mute for " + data.lastAccountName());
                }
            }
        }

        if (!options.dryRun()) {
            var backData = userTransformer.transformBackLocation(data);
            if (backData != null && plugin.getBackManager() != null) {
                Player onlinePlayer = plugin.getServer().getPlayer(data.uuid());

                if (onlinePlayer != null) {
                    plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
                        plugin.getBackManager().setBackLocation(onlinePlayer, backData.location());
                    });
                    plugin.debug("Migrated back location for online player: " + data.lastAccountName());
                } else {
                    plugin.debug("Skipped back location for offline player: " + data.lastAccountName());
                }
            }
        }
    }

    private void migrateBans(Options options) {
        File bannedPlayersFile = new File(serverRootFolder, "banned-players.json");
        if (!bannedPlayersFile.exists()) {
            plugin.getLogger().warning("banned-players.json not found at: " + bannedPlayersFile.getPath());

            return;
        }

        try (FileReader reader = new FileReader(bannedPlayersFile)) {

            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(reader);
            if (!element.isJsonArray()) {
                plugin.getLogger().warning("Invalid banned-players.json format");
                return;
            }

            JsonArray banArray = element.getAsJsonArray();
            MigrationProgress.setTotalBans(banArray.size());
            plugin.debug("Found " + banArray.size() + " bans to migrate");

            for (JsonElement banElement : banArray) {
                if (!banElement.isJsonObject()) continue;
                MigrationProgress.incrementBans();


                JsonObject banObj = banElement.getAsJsonObject();

                String uuidStr = banObj.has("uuid") ? banObj.get("uuid").getAsString() : null;
                String name = banObj.has("name") ? banObj.get("name").getAsString() : null;
                String reason = banObj.has("reason") ? banObj.get("reason").getAsString() : "Migrated from server";
                String source = banObj.has("source") ? banObj.get("source").getAsString() : "Server";
                String expiresStr = banObj.has("expires") ? banObj.get("expires").getAsString() : "forever";

                if (uuidStr == null || name == null) {
                    plugin.getLogger().warning("Skipping ban entry with missing UUID or name");
                    stats.bansFailed.incrementAndGet();
                    continue;
                }

                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    long expires = parseExpires(expiresStr);

                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                    if (!offlinePlayer.isBanned()) {
                        plugin.debug("Skipping expired/unbanned player: " + name);
                        continue;
                    }

                    var banData = new UserDataMapper.BanTransfer(uuid, name, reason, source, expires);
                    var result = punishmentWriter.writeBan(banData, options.dryRun()).join();


                    if (result.success()) {
                        stats.bansMigrated.incrementAndGet();
                        plugin.debug("Migrated ban for " + name);
                    } else {
                        stats.bansFailed.incrementAndGet();
                        if (result.message() != null && !result.message().contains("already banned")) {
                            plugin.debug("Failed to migrate ban for " + name + ": " + result.message());
                        }
                    }

                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in ban entry: " + uuidStr);
                    stats.bansFailed.incrementAndGet();
                }
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to read banned-players.json: " + e.getMessage());
        }
    }


    private long parseExpires(String expiresStr) {
        if (expiresStr == null || expiresStr.equalsIgnoreCase("forever")) {

            return 0;
        }

        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
            java.util.Date date = sdf.parse(expiresStr);
            return date.getTime();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse ban expiration: " + expiresStr);
            return 0;
        }
    }

    public static class MigrationException extends RuntimeException {
        public MigrationException(String message) {

            super(message);
        }
    }

    public static class MigrationResult {
        final AtomicInteger usersMigrated = new AtomicInteger(0);
        final AtomicInteger usersFailed = new AtomicInteger(0);
        final AtomicInteger warpsMigrated = new AtomicInteger(0);
        final AtomicInteger warpsFailed = new AtomicInteger(0);
        final AtomicInteger homesMigrated = new AtomicInteger(0);
        final AtomicInteger homesSkipped = new AtomicInteger
                (0);
        final AtomicInteger economyRecords = new AtomicInteger(0);
        final AtomicInteger nicknamesMigrated = new AtomicInteger(0);
        final AtomicInteger mutesMigrated = new AtomicInteger(0);
        final AtomicInteger bansMigrated = new AtomicInteger(0);
        final AtomicInteger bansFailed = new AtomicInteger(0);
        final List<String> warnings = new ArrayList<>();
        void addWarning(String warning) {
            warnings.add(warning);
        }
        public int usersMigrated() {
            return usersMigrated.get();
        }
        public int usersFailed() {
            return usersFailed.get();
        }
        public int warpsMigrated() {
            return warpsMigrated.get();
        }
        public int warpsFailed() {
            return warpsFailed.get();
        }
        public int homesMigrated() {

            return homesMigrated.get();
        }
        public int homesSkipped() {
            return homesSkipped.get();
        }
        public int economyRecords() {
            return economyRecords.get();
        }
        public int nicknamesMigrated() {
            return nicknamesMigrated.get();
        }
        public int mutesMigrated() {
            return mutesMigrated.get();
        }
        public int bansMigrated() {
            return bansMigrated.get();
        }
        public int bansFailed() {
            return bansFailed.get();
        }
        public List<String> warnings() {
            return warnings;
        }

        @Override
        public String toString() {
            return String.format(

                    "users=%d/%d, warps=%d/%d, homes=%d/%d skipped, economy=%d, nicks=%d, mutes=%d, bans=%d/%d, warnings=%d",
                    usersMigrated.get(), usersFailed.get(),
                    warpsMigrated.get(), warpsFailed.get(),
                    homesMigrated.get(), homesSkipped.get(),
                    economyRecords.get(),

                    nicknamesMigrated.get(), mutesMigrated.get(),
                    bansMigrated.get(), bansFailed.get(),
                    warnings.size()
            );
        }
    }

    public void shutdown() {
        homeWriter.shutdown();
    }
}