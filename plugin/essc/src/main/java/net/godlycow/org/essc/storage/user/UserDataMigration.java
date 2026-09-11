package net.godlycow.org.essc.storage.user;

import net.godlycow.org.essc.EssentialsC;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UserDataMigration {

    private final EssentialsC plugin;
    private final UserStore store;

    public UserDataMigration(EssentialsC plugin, UserStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public boolean backupDatabase() {
        File dbFile = new File(store.getDatabase().getDbPath());
        if (!dbFile.exists()) {
            plugin.getLogger().info("[UserMigration] No database file found, skipping backup.");
            return true;
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File backupDir = new File(plugin.getDataFolder(), "backups");
        backupDir.mkdirs();
        File backup = new File(backupDir, "users-" + timestamp + ".db");
        try {
            Files.copy(dbFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("[UserMigration] Database backed up to " + backup.getName());
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("[UserMigration] Backup failed: " + e.getMessage());
            return false;
        }
    }

    public MigrationStatus checkStatus() {
        try (Connection conn = store.getDatabase().openFreshConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(version), 0) FROM _schema_version");
            int version = rs.next() ? rs.getInt(1) : 0;

            rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            int userCount = rs.next() ? rs.getInt(1) : 0;

            rs = stmt.executeQuery("SELECT COUNT(*) FROM user_ignored_players");
            int ignoredCount = rs.next() ? rs.getInt(1) : 0;

            rs = stmt.executeQuery("SELECT COUNT(*) FROM user_ip_history");
            int ipCount = rs.next() ? rs.getInt(1) : 0;

            return new MigrationStatus(version, userCount, ignoredCount, ipCount);
        } catch (SQLException e) {
            plugin.getLogger().severe("[UserMigration] Status check failed: " + e.getMessage());
            return new MigrationStatus(-1, 0, 0, 0);
        }
    }

    public record MigrationStatus(int schemaVersion, int userCount, int ignoredCount, int ipCount) {
        public boolean needsMigration() {
            return schemaVersion < 1;
        }

        public boolean hasData() {
            return userCount > 0 || ignoredCount > 0 || ipCount > 0;
        }
    }
}
