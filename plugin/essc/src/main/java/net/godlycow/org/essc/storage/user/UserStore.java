package net.godlycow.org.essc.storage.user;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.storage.database.Database;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class UserStore {
    private final Database database;
    private final EssentialsC plugin;

    private static final int SCHEMA_VERSION = 2;


    // create all tables for the user db
    private static final String CREATE_USERS_TABLE = """
            CREATE TABLE IF NOT EXISTS users (
                uuid TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                last_known_name TEXT,
                first_join_time INTEGER DEFAULT 0,
                last_join_time INTEGER DEFAULT 0,
                last_ip TEXT,
                logout_location TEXT,
                logout_time INTEGER DEFAULT 0,
                language_code TEXT,
                back_location TEXT,
                death_location TEXT,
                fly_enabled BOOLEAN DEFAULT FALSE,
                vanished BOOLEAN DEFAULT FALSE,
                tpa_blocked BOOLEAN DEFAULT FALSE,
                last_reply_target TEXT,
                rtp_last_used INTEGER DEFAULT 0,
                spawn_last_teleport INTEGER DEFAULT 0,
                ban_reason TEXT,
                ban_banner TEXT,
                ban_time INTEGER DEFAULT 0,
                ban_expires INTEGER DEFAULT 0,
                mute_reason TEXT,
                mute_muter TEXT,
                mute_time INTEGER DEFAULT 0,
                mute_expires INTEGER DEFAULT 0,
                mute_offline_notification BOOLEAN DEFAULT FALSE,
                scoreboard_disabled BOOLEAN DEFAULT FALSE,
                rules_accepted BOOLEAN DEFAULT FALSE,
                created_at INTEGER DEFAULT (strftime('%s','now')),
                updated_at INTEGER DEFAULT (strftime('%s','now'))
            );
            """;

    private static final String CREATE_IGNORED_TABLE = """
            CREATE TABLE IF NOT EXISTS user_ignored_players (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL,
                ignored_uuid TEXT NOT NULL,
                ignored_name TEXT,
                UNIQUE(uuid, ignored_uuid)
            );
            """;

    private static final String CREATE_IP_HISTORY_TABLE = """
            CREATE TABLE IF NOT EXISTS user_ip_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL,
                ip TEXT NOT NULL,
                recorded_at INTEGER DEFAULT (strftime('%s','now'))
            );
            """;

    private static final String CREATE_INVENTORY_TABLE = """
            CREATE TABLE IF NOT EXISTS user_inventories (
                uuid TEXT PRIMARY KEY,
                inventory_data TEXT NOT NULL,
                saved_at INTEGER DEFAULT (strftime('%s','now'))
            );
            """;

    private static final String CREATE_PUNISHMENT_BANS_TABLE = """
            CREATE TABLE IF NOT EXISTS punishment_bans (
                uuid TEXT PRIMARY KEY,
                name TEXT,
                reason TEXT,
                banner TEXT,
                time INTEGER DEFAULT 0,
                expires INTEGER DEFAULT 0
            );
            """;

    private static final String CREATE_PUNISHMENT_IP_BANS_TABLE = """
            CREATE TABLE IF NOT EXISTS punishment_ip_bans (
                ip TEXT PRIMARY KEY,
                reason TEXT,
                banner TEXT,
                time INTEGER DEFAULT 0,
                expires INTEGER DEFAULT 0
            );
            """;

    private static final String CREATE_PUNISHMENT_MUTES_TABLE = """
            CREATE TABLE IF NOT EXISTS punishment_mutes (
                uuid TEXT PRIMARY KEY,
                name TEXT,
                reason TEXT,
                muter TEXT,
                time INTEGER DEFAULT 0,
                expires INTEGER DEFAULT 0,
                offline_notification BOOLEAN DEFAULT 0
            );
            """;

    private static final String CREATE_SCHEMA_VERSION_TABLE = """
            CREATE TABLE IF NOT EXISTS _schema_version (
                version INTEGER PRIMARY KEY
            );
            """;

    public UserStore(EssentialsC plugin) {
        this.plugin = plugin;
        this.database = new Database(plugin, "users.db");
        initialize();
        UserDataMigration migration = new UserDataMigration(plugin, this);
        UserDataMigration.MigrationStatus status = migration.checkStatus();
        if (status.needsMigration() && status.hasData()) {
            migration.backupDatabase();
        }
        runMigration();
        status = migration.checkStatus();
        plugin.debug("[UserStore] DB v" + status.schemaVersion() + ", " + status.userCount() + " users, "
                + status.ignoredCount() + " ignored, " + status.ipCount() + " ip history entries.");
    }

    public Database getDatabase() {
        return database;
    }

    private void initialize() {
        try (Connection conn = database.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_SCHEMA_VERSION_TABLE);
            stmt.execute(CREATE_USERS_TABLE);
            stmt.execute(CREATE_IGNORED_TABLE);
            stmt.execute(CREATE_IP_HISTORY_TABLE);
            stmt.execute(CREATE_INVENTORY_TABLE);
            stmt.execute(CREATE_PUNISHMENT_BANS_TABLE);
            stmt.execute(CREATE_PUNISHMENT_IP_BANS_TABLE);
            stmt.execute(CREATE_PUNISHMENT_MUTES_TABLE);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize user database", e);
        }
    }

    private void runMigration() {
        try (Connection conn = database.getConnection()) {
            int version = getSchemaVersion(conn);
            if (version < SCHEMA_VERSION) {
                plugin.getLogger().info("[UserStore] Migrating user database from v" + version + " to v" + SCHEMA_VERSION + " ...");
                migrate(conn, version);
                setSchemaVersion(conn, SCHEMA_VERSION);
                plugin.getLogger().info("[UserStore] Migration completed");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[UserStore] Migration failed!!! : " + e.getMessage());
        }
    }

    private void migrate(Connection conn, int fromVersion) throws SQLException {
        if (fromVersion < 1) {

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(CREATE_USERS_TABLE);
                stmt.execute(CREATE_IGNORED_TABLE);
                stmt.execute(CREATE_IP_HISTORY_TABLE);
                stmt.execute(CREATE_INVENTORY_TABLE);
                stmt.execute(CREATE_PUNISHMENT_BANS_TABLE);
                stmt.execute(CREATE_PUNISHMENT_IP_BANS_TABLE);
                stmt.execute(CREATE_PUNISHMENT_MUTES_TABLE);
            }

            migrateYamlPunishments(conn);
        }
        if (fromVersion < 2) {
            clearLegacyDefaultLanguageCodes(conn);
        }
    }

    private void clearLegacyDefaultLanguageCodes(Connection conn) throws SQLException {
        boolean hasLanguageColumn = false;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(users)")) {
            while (rs.next()) {
                if ("language_code".equals(rs.getString("name"))) {
                    hasLanguageColumn = true;
                    break;
                }
            }
        }
        if (!hasLanguageColumn) {
            plugin.getLogger().warning("[UserStore] users table has no language_code column, skipping legacy language cleanup");
            return;
        }
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE users SET language_code = NULL WHERE language_code = ?")) {
            stmt.setString(1, "en_US");
            int cleared = stmt.executeUpdate();
            if (cleared > 0) {
                plugin.getLogger().info("[UserStore] Cleared legacy default language codes for " + cleared + " player(s)");
            }
        }
    }

    private void migrateYamlPunishments(Connection conn) throws SQLException {
        File banFile = new File(plugin.getDataFolder(), "bans.yml");
        File muteFile = new File(plugin.getDataFolder(), "mutes.yml");

        int banCount = 0, ipBanCount = 0, muteCount = 0;
        try {
            if (banFile.exists()) {
                FileConfiguration banConfig = YamlConfiguration.loadConfiguration(banFile);
                if (banConfig.contains("players")) {
                    for (String key : banConfig.getConfigurationSection("players").getKeys(false)) {
                        try {

                            UUID uuid = UUID.fromString(key);
                            String path = "players." + key; //get bans
                            try (PreparedStatement stmt = conn.prepareStatement(
                                    "INSERT OR IGNORE INTO punishment_bans (uuid, name, reason, banner, time, expires) VALUES (?, ?, ?, ?, ?, ?)")) {
                                stmt.setString(1, uuid.toString());
                                stmt.setString(2, banConfig.getString(path + ".name"));
                                stmt.setString(3, banConfig.getString(path + ".reason"));
                                stmt.setString(4, banConfig.getString(path + ".banner"));
                                stmt.setLong(5, banConfig.getLong(path + ".time"));
                                stmt.setLong(6, banConfig.getLong(path + ".expires"));
                                banCount += stmt.executeUpdate();
                            }

                        } catch (IllegalArgumentException ignored) {

                        }
                    }
                }
                if (banConfig.contains("ips")) {

                    for (String key : banConfig.getConfigurationSection("ips").getKeys(false)) {
                        String path = "ips." + key; // ips
                        String originalIp = banConfig.getString(path + ".ip", key.replace('_', '.'));
                        try (PreparedStatement stmt = conn.prepareStatement(
                                "INSERT OR IGNORE INTO punishment_ip_bans (ip, reason, banner, time, expires) VALUES (?, ?, ?, ?, ?)")) {
                            stmt.setString(1, originalIp);
                            stmt.setString(2, banConfig.getString(path + ".reason"));
                            stmt.setString(3, banConfig.getString(path + ".banner"));
                            stmt.setLong(4, banConfig.getLong(path + ".time"));
                            stmt.setLong(5, banConfig.getLong(path + ".expires"));

                            ipBanCount += stmt.executeUpdate(); //update bd
                        }
                    }
                }
            }
            if (muteFile.exists()) {
                FileConfiguration muteConfig = YamlConfiguration.loadConfiguration(muteFile);

                for (String key : muteConfig.getKeys(false )) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        try (PreparedStatement stmt = conn.prepareStatement(
                                "INSERT OR IGNORE INTO punishment_mutes (uuid, name, reason, muter, time, expires, offline_notification) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                            stmt.setString(1, uuid.toString());
                            stmt.setString(2, muteConfig.getString(key + ".name"));
                            stmt.setString(3, muteConfig.getString(key + ".reason"));
                            stmt.setString(4, muteConfig.getString(key + ".muter"));
                            stmt.setLong(5, muteConfig.getLong(key + ".time"));
                            stmt.setLong(6, muteConfig.getLong(key + ".expires"));
                            stmt.setBoolean(7, muteConfig.getBoolean(key + ".offline_notification", false));
                            muteCount += stmt.executeUpdate();
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            //, add ".backup" and move to backups folder
            if (banFile.exists() || muteFile.exists()) {

                String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
                File backupDir = new File(plugin.getDataFolder(), "backups");
                backupDir.mkdirs();

                if (banFile.exists()) {
                    File backup = new File(backupDir, "bans-" + timestamp + ".yml.backup");
                    Files.copy(banFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    banFile.delete();
                }
                if (muteFile.exists()) {
                    File backup = new File(backupDir, "mutes-" + timestamp + ".yml.backup");
                    Files.copy(muteFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    muteFile.delete();
                }
            }

        } catch (IOException e) {
            throw new SQLException("Failed to migrate YAML punishments", e);
        }
        if (banCount > 0 || ipBanCount > 0 || muteCount > 0) {
            plugin.getLogger().info("[UserStore] Migrated " + banCount + " bans, " + ipBanCount + " IP bans, " + muteCount + " mutes from YAML to database");
        }
    }


    // and a lof of
    private int getSchemaVersion(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(version), 0) FROM _schema_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void setSchemaVersion(Connection conn, int version) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("INSERT OR REPLACE INTO _schema_version (version) VALUES (?)")) {
            stmt.setInt(1, version);
            stmt.executeUpdate();
        }
    }

    UserProfile findByUuid(UUID uuid) { //find an user by the uuid
        try (Connection conn = database.openFreshConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error finding user " + uuid + ": " + e.getMessage());
            return null;
        }
    }


    // and we cant forget to save everything ofc
    boolean save(UserProfile profile) {
        long now = Instant.now().getEpochSecond();
        profile.setUpdatedAt(now);
        if (profile.getCreatedAt() <= 0) {
            profile.setCreatedAt(now);
        }
        try (Connection conn = database.openFreshConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO users (uuid, username, last_known_name, first_join_time, last_join_time, last_ip, logout_location, logout_time, language_code, back_location, death_location, fly_enabled, vanished, tpa_blocked, last_reply_target, rtp_last_used, spawn_last_teleport, ban_reason, ban_banner, ban_time, ban_expires, mute_reason, mute_muter, mute_time, mute_expires, mute_offline_notification, scoreboard_disabled, rules_accepted, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT(uuid) DO UPDATE SET " +
                     "username = excluded.username, " +
                     "last_known_name = excluded.last_known_name, " +
                     "first_join_time = excluded.first_join_time, " +
                     "last_join_time = excluded.last_join_time, " +
                     "last_ip = excluded.last_ip, " +
                     "logout_location = excluded.logout_location, " +
                     "logout_time = excluded.logout_time, " +
                     "language_code = excluded.language_code, " +
                     "back_location = excluded.back_location, " +
                     "death_location = excluded.death_location, " +
                     "fly_enabled = excluded.fly_enabled, " +
                     "vanished = excluded.vanished, " +
                     "tpa_blocked = excluded.tpa_blocked, " +
                     "last_reply_target = excluded.last_reply_target, " +
                     "rtp_last_used = excluded.rtp_last_used, " +
                     "spawn_last_teleport = excluded.spawn_last_teleport, " +
                     "ban_reason = excluded.ban_reason, " +
                     "ban_banner = excluded.ban_banner, " +
                     "ban_time = excluded.ban_time, " +
                     "ban_expires = excluded.ban_expires, " +
                     "mute_reason = excluded.mute_reason, " +
                     "mute_muter = excluded.mute_muter, " +
                     "mute_time = excluded.mute_time, " +
                     "mute_expires = excluded.mute_expires, " +
                     "mute_offline_notification = excluded.mute_offline_notification, " +
                     "scoreboard_disabled = excluded.scoreboard_disabled, " +
                     "rules_accepted = excluded.rules_accepted, " +
                     "created_at = excluded.created_at, " +
                     "updated_at = excluded.updated_at")) {
            int i = 1;
            stmt.setString(i++, profile.getUuid().toString());
            stmt.setString(i++,  profile.getUsername());
            stmt.setString(i++, profile.getLastKnownName());
            stmt.setLong(i++,  profile.getFirstJoinTime());
            stmt.setLong(i++, profile.getLastJoinTime());
            stmt.setString(i++, profile.getLastIp());
            stmt.setString(i++, profile.getRawLogoutLocation());
            stmt.setLong(i++, profile.getLogoutTime());
            stmt.setString( i++, profile.getLanguageCode());
            stmt.setString(i++,  profile.getRawBackLocation());
            stmt.setString(i++, profile.getRawDeathLocation());
            stmt.setBoolean(i ++, profile.isFlyEnabled());
            stmt.setBoolean(i++, profile.isVanished());
            stmt.setBoolean(i++,  profile.isTpaBlocked());
            stmt.setString(i++, profile.getRawLastReplyTarget());
            stmt.setLong(i++, profile.getRtpLastUsed());
            stmt.setLong(i++, profile.getSpawnLastTeleport());
            stmt.setString(i++ , profile.getBanReason());
            stmt.setString(i++, profile.getBanBanner());
            stmt.setLong(i ++,  profile.getBanTime());
            stmt.setLong(i++, profile.getBanExpires());
            stmt.setString(i++, profile.getMuteReason());
            stmt.setString(i++, profile.getMuteMuter());
            stmt.setLong(i++, profile.getMuteTime());
            stmt.setLong(i++,  profile.getMuteExpires());
            stmt.setBoolean(i++, profile.isMuteOfflineNotification());
            stmt.setBoolean (i++, profile.isScoreboardDisabled());
            stmt.setBoolean(i++, profile.isRulesAccepted());
            stmt.setLong(i++,  profile.getCreatedAt());
            stmt.setLong(i, profile.getUpdatedAt());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { //catch sql exception and log
            plugin.getLogger().severe("Error saving user " + profile.getUuid() + ": " + e.getMessage());

            return false;
        }
    }

    boolean recordIp(UUID uuid, String ip) {
        long now = Instant.now().getEpochSecond();
        try (Connection conn = database.openFreshConnection()) {
            try (PreparedStatement insert = conn.prepareStatement("INSERT INTO user_ip_history (uuid, ip) VALUES (?, ?)");
                 PreparedStatement update = conn.prepareStatement("UPDATE users SET last_ip = ?, updated_at = ? WHERE uuid = ?")) {
                insert.setString(1, uuid.toString());
                insert.setString(2, ip);
                insert.executeUpdate();
                update.setString(1, ip);
                update.setLong(2, now);
                update.setString(3, uuid.toString());
                update.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error recording IP for " + uuid + ": " + e.getMessage());
            return false;
        }
    }

    Set<UUID> getIgnoredPlayers(UUID uuid) {
        Set<UUID> ignored = new HashSet<>();
        try (Connection conn = database.openFreshConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT ignored_uuid FROM user_ignored_players WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try { ignored.add(UUID.fromString(rs.getString("ignored_uuid"))); }
                    catch (IllegalArgumentException ignored_) {}
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting ignored players for " + uuid + ": " + e.getMessage());
        }
        return ignored;
    }

    Map<UUID, String> getIgnoredPlayersWithNames(UUID uuid) {
        Map<UUID, String> result = new java.util.LinkedHashMap<>();
        try (Connection conn = database.openFreshConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT ignored_uuid, ignored_name FROM user_ignored_players WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        UUID ignoredUuid = UUID.fromString(rs.getString("ignored_uuid"));
                        String name = rs.getString("ignored_name");
                        result.put(ignoredUuid, name != null ? name : "Unknown");
                    } catch (IllegalArgumentException ignored_) {}
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting ignored players for " + uuid + ": " + e.getMessage());
        }
        return result;
    }

    boolean addIgnoredPlayer(UUID uuid, UUID ignoredUuid, String ignoredName) {
        try (Connection conn = database.openFreshConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT OR IGNORE INTO user_ignored_players (uuid, ignored_uuid, ignored_name) VALUES (?, ?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, ignoredUuid.toString());
            stmt.setString(3, ignoredName);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error adding ignored player: " + e.getMessage());
            return false;
        }
    }

    boolean removeIgnoredPlayer(UUID uuid, UUID ignoredUuid) {
        try (Connection conn = database.openFreshConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM user_ignored_players WHERE uuid = ? AND ignored_uuid = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, ignoredUuid.toString());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error removing ignored player: " + e.getMessage());
            return false;
        }
    }

    List<String> getIpHistory(UUID uuid) {
        List<String> entries = new ArrayList<>();
        try (Connection conn = database.openFreshConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT ip FROM user_ip_history WHERE uuid = ? ORDER BY recorded_at DESC")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(rs.getString("ip"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting IP history for " + uuid + ": " + e.getMessage());
        }
        return entries;
    }

    boolean saveInventory(UUID uuid, String base64) {
        try (Connection conn = database.openFreshConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO user_inventories (uuid, inventory_data, saved_at) VALUES (?, ?, strftime('%s','now')) " +
                     "ON CONFLICT(uuid) DO UPDATE SET inventory_data = excluded.inventory_data, saved_at = excluded.saved_at")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, base64);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving inventory for " + uuid + ": " + e.getMessage());
            return false;
        }
    }

    String loadInventory(UUID uuid) {
        try (Connection conn = database.openFreshConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT inventory_data FROM user_inventories WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("inventory_data") : null;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading inventory for " + uuid + ": " + e.getMessage());
            return null;
        }
    }

    boolean deleteInventory(UUID uuid) {
        try (Connection conn = database.openFreshConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM user_inventories WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error deleting inventory for " + uuid + ": " + e.getMessage());
            return false;
        }
    }

    private UserProfile mapRow(ResultSet row) throws SQLException {

        UUID uuid = UUID.fromString(row.getString("uuid"));
        
        UserProfile profile = new UserProfile(uuid, row.getString("username"));

        profile.setLastKnownName(row.getString("last_known_name"));
        profile.setFirstJoinTime(row.getLong("first_join_time"));
        profile.setLastJoinTime(row.getLong("last_join_time"));
        profile.setLastIp(row.getString("last_ip"));
        profile.setRawLogoutLocation(row.getString("logout_location"));
        profile.setLogoutTime(row.getLong("logout_time"));
        profile.setLanguageCode(row.getString("language_code"));
        profile.setRawBackLocation(row.getString("back_location"));
        profile.setRawDeathLocation(row.getString("death_location"));
        profile.setFlyEnabled(row.getBoolean("fly_enabled"));
        profile.setVanished(row.getBoolean("vanished"));
        profile.setTpaBlocked(row.getBoolean("tpa_blocked"));
        profile.setRawLastReplyTarget(row.getString("last_reply_target"));
        profile.setRtpLastUsed(row.getLong("rtp_last_used"));
        profile.setSpawnLastTeleport(row.getLong("spawn_last_teleport"));
        profile.setBanReason(row.getString("ban_reason"));
        profile.setBanBanner(row.getString("ban_banner"));
        profile.setBanTime(row.getLong("ban_time"));
        profile.setBanExpires(row.getLong("ban_expires"));
        profile.setMuteReason(row.getString("mute_reason"));
        profile.setMuteMuter(row.getString("mute_muter"));
        profile.setMuteTime(row.getLong("mute_time"));
        profile.setMuteExpires(row.getLong("mute_expires"));
        profile.setMuteOfflineNotification(row.getBoolean("mute_offline_notification"));
        profile.setScoreboardDisabled(row.getBoolean("scoreboard_disabled"));
        profile.setRulesAccepted(row.getBoolean("rules_accepted"));
        profile.setCreatedAt(row.getLong("created_at"));
        profile.setUpdatedAt(row.getLong("updated_at"));

        return profile;
    }
}
