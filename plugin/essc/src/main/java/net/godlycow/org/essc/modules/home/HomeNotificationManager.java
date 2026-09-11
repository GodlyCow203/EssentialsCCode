package net.godlycow.org.essc.modules.home;

import net.godlycow.org.essc.EssentialsC;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HomeNotificationManager {

    public enum NotificationType {
        DELETED,
        RENAMED,
        RELOCATED
    }

    private final EssentialsC plugin;
    private final Map<UUID, Boolean> notificationsCache = new ConcurrentHashMap<>();

    public HomeNotificationManager(EssentialsC plugin) {
        this.plugin = plugin;
        initTables();
    }

    private void initTables() {
        plugin.getHomeManager().getDatabase().async(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS home_notifications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    target_uuid TEXT NOT NULL,
                    type TEXT NOT NULL,
                    home_name TEXT NOT NULL,
                    extra TEXT,
                    admin_name TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
            """)) {
                stmt.execute();
            }

            try (PreparedStatement stmt = conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS home_notification_settings (
                    uuid TEXT PRIMARY KEY,
                    enabled INTEGER NOT NULL DEFAULT 1
                )
            """)) {
                stmt.execute();
            }

            return null;
        });
    }

    public void notifyAdminAction(UUID targetUuid, NotificationType type,
                                  String homeName, String extra, String adminName) {
        boolean enabled = notificationsCache.getOrDefault(targetUuid, true);
        if (!enabled) return;

        Player target = Bukkit.getPlayer(targetUuid);
        if (target != null && target.isOnline()) {
            deliverLive(target, type, homeName, extra, adminName);
        } else {
            queueOffline(targetUuid, type, homeName, extra, adminName);
        }
    }

    private void deliverLive(Player target, NotificationType type,
                             String homeName, String extra, String adminName) {
        target.getScheduler().run(plugin, task -> {
            if (!target.isOnline()) return;
            target.sendMessage(buildMessage(target, type, homeName, extra, adminName));
        }, null);
    }

    private void queueOffline(UUID targetUuid, NotificationType type,
                              String homeName, String extra, String adminName) {
        plugin.getHomeManager().getDatabase().async(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO home_notifications (target_uuid, type, home_name, extra, admin_name, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
            """)) {
                stmt.setString(1, targetUuid.toString());
                stmt.setString(2, type.name());
                stmt.setString(3, homeName);
                stmt.setString(4, extra);
                stmt.setString(5, adminName);
                stmt.setLong(6, System.currentTimeMillis());
                stmt.executeUpdate();
            }
            return null;
        });
    }

    public void deliverPending(Player player) {
        plugin.getHomeManager().getDatabase().async(conn -> {
            List<PendingNotification> pending = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, type, home_name, extra, admin_name FROM home_notifications WHERE target_uuid = ? ORDER BY created_at ASC")) {
                stmt.setString(1, player.getUniqueId().toString());
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    pending.add(new PendingNotification(
                            rs.getInt("id"),
                            NotificationType.valueOf(rs.getString("type")),
                            rs.getString("home_name"),
                            rs.getString("extra"),
                            rs.getString("admin_name")
                    ));
                }
            }
            return pending;
        }).thenAccept(pending -> {
            if (pending.isEmpty()) return;
            player.getScheduler().run(plugin, task -> {
                if (!player.isOnline()) return;
                for (PendingNotification n : pending) {
                    player.sendMessage(buildMessage(player, n.type(), n.homeName(), n.extra(), n.adminName()));
                }
            }, null);
            clearPending(player.getUniqueId());
        });

        loadNotificationsEnabled(player.getUniqueId());
    }

    private void clearPending(UUID uuid) {
        plugin.getHomeManager().getDatabase().async(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM home_notifications WHERE target_uuid = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            }
            return null;
        });
    }

    private void loadNotificationsEnabled(UUID uuid) {
        plugin.getHomeManager().getDatabase().async(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT enabled FROM home_notification_settings WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) return rs.getInt("enabled") == 1;
            }
            return true;
        }).thenAccept(enabled -> notificationsCache.put(uuid, enabled));
    }

    public boolean isNotificationsEnabled(UUID uuid) {
        return notificationsCache.getOrDefault(uuid, true);
    }

    public void setNotificationsEnabled(UUID uuid, boolean enabled) {
        notificationsCache.put(uuid, enabled);
        plugin.getHomeManager().getDatabase().async(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO home_notification_settings (uuid, enabled) VALUES (?, ?)
                ON CONFLICT(uuid) DO UPDATE SET enabled = excluded.enabled
            """)) {
                stmt.setString(1, uuid.toString());
                stmt.setInt(2, enabled ? 1 : 0);
                stmt.executeUpdate();
            }
            return null;
        });
    }

    private Component buildMessage(Player player, NotificationType type,
                                   String homeName, String extra, String adminName) {
        return switch (type) {
            case DELETED -> plugin.getLanguageManager().get(player,
                    "home.notification.admin.deleted",
                    Map.of("name", homeName, "admin", adminName));
            case RENAMED -> plugin.getLanguageManager().get(player,
                    "home.notification.admin.renamed",
                    Map.of("old", homeName, "new", extra != null ? extra : "?", "admin", adminName));
            case RELOCATED -> plugin.getLanguageManager().get(player,
                    "home.notification.admin.relocated",
                    Map.of("name", homeName, "admin", adminName));
        };
    }

    private record PendingNotification(int id, NotificationType type, String homeName, String extra, String adminName) {

    }
}