package net.godlycow.org.essc.modules.shop;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.storage.database.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ShopDatabase {
    private final EssentialsC plugin;
    private final Database database;

    public ShopDatabase(EssentialsC plugin) {
        this.plugin = plugin;
        this.database = new Database(plugin, "shop.db");
    }

    public void connect() throws SQLException {
        database.connect();
        createTables();
    }

    public void disconnect() {
        database.disconnect();
    }

    private void createTables() throws SQLException {
        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS purchase_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT NOT NULL,
                    item_id TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    price DECIMAL(19,2) NOT NULL,
                    timestamp INTEGER DEFAULT (strftime('%s', 'now'))
                )
            """)) {
            stmt.execute();
        }

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                CREATE TABLE IF NOT EXISTS sell_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT NOT NULL,
                    item_id TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    price DECIMAL(19,2) NOT NULL,
                    timestamp INTEGER DEFAULT (strftime('%s', 'now'))
                )
            """)) {
            stmt.execute();
        }

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "CREATE INDEX IF NOT EXISTS idx_purchases_uuid ON purchase_history(uuid)"
             )) {
            stmt.execute();
        }

        plugin.debug("Shop database tables initialized");
    }

    public CompletableFuture<Void> logPurchase(UUID uuid, String itemId, int amount, double price) {
        return database.async(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO purchase_history (uuid, item_id, amount, price)
                VALUES (?, ?, ?, ?)
            """)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, itemId);
                stmt.setInt(3, amount);
                stmt.setDouble(4, price);
                stmt.executeUpdate();
                return null;
            }
        });
    }

    public CompletableFuture<Void> logSale(UUID uuid, String itemId, int amount, double price) {
        return database.async(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO sell_history (uuid, item_id, amount, price)
                VALUES (?, ?, ?, ?)
            """)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, itemId);
                stmt.setInt(3, amount);
                stmt.setDouble(4, price);
                stmt.executeUpdate();
                return null;
            }
        });
    }
}