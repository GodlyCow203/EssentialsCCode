package net.godlycow.org.essc.plugin.economy;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.integration.metrics.bstats.EconomyCharts;
import net.godlycow.org.essc.storage.database.Database;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyManager implements EconomyService, Listener {
    private final EssentialsC plugin;
    private final Database database;
    private final Map<UUID, BigDecimal> cache = new ConcurrentHashMap<>();
    private final MiniMessage mm = MiniMessage.miniMessage();

    private String currencySingular;
    private String currencyPlural;
    private BigDecimal startingBalance;
    private String formatPattern;
    private BigDecimal minTransaction;
    private BigDecimal maxBalance;
    private DecimalFormat decimalFormat;

    public EconomyManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.database = new Database(plugin, "economy.db");
        loadConfig();

        try {
            database.connect();
            createTables();
            database.async(conn -> {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM economy")) {
                    ResultSet rs = stmt.executeQuery();
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }).thenAccept(count -> plugin.debug("EconomyManager initialized (" + count + " accounts)"));
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize economy database: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        Connection conn = database.getConnection();
        if (conn == null) {
            throw new SQLException("Could not get database connection");
        }

        try (PreparedStatement stmt = conn.prepareStatement("""
            CREATE TABLE IF NOT EXISTS economy (
                uuid TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                balance REAL DEFAULT 0.0,
                last_updated INTEGER DEFAULT (strftime('%s', 'now'))
            )
        """)) {
            stmt.execute();
        }

        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE INDEX IF NOT EXISTS idx_balance ON economy(balance DESC)"
        )) {
            stmt.execute();
        }

        plugin.debug("Economy database tables initialized/verified");
    }

    public void reload() {
        plugin.debug("Reloading economy configuration...");
        loadConfig();
        cache.clear();
    }

    private void loadConfig() {
        var cm = plugin.getConfigManager();
        this.currencySingular = cm.getCurrencySingular();
        this.currencyPlural = cm.getCurrencyPlural();
        this.startingBalance = cm.getStartingBalance();
        this.formatPattern = cm.getEconomyFormat();
        this.minTransaction = cm.getMinTransaction();
        this.maxBalance = cm.getMaxBalance();

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        this.decimalFormat = new DecimalFormat(formatPattern, symbols);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        createAccount(player.getUniqueId(), player.getName()).thenAccept(success -> {
            if (success) {
                plugin.debug("Created/Verified economy account for " + player.getName());

            }
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Failed to create account for " + player.getName() + ": " + ex.getMessage());
            return null;
        });
    }

    @Override
    public CompletableFuture<Boolean> hasAccount(UUID uuid) {
        return database.async(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM economy WHERE uuid = ?"
            )) {
                ps.setString(1, uuid.toString());
                return ps.executeQuery().next();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> createAccount(UUID uuid, String name) {
        return database.async(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT OR IGNORE INTO economy (uuid, username, balance) VALUES (?, ?, ?)"
            )) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setDouble(3, startingBalance.doubleValue());
                return ps.executeUpdate() > 0;
            }
        });
    }

    @Override
    public CompletableFuture<BigDecimal> getBalance(UUID uuid) {
        if (cache.containsKey(uuid)) {
            return CompletableFuture.completedFuture(cache.get(uuid));
        }

        return database.async(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT balance FROM economy WHERE uuid = ?"
            )) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    BigDecimal balance = BigDecimal.valueOf(rs.getDouble("balance"));
                    cache.put(uuid, balance);
                    return balance;
                }
                return BigDecimal.ZERO;
            }
        });
    }

    public BigDecimal getCachedBalance(UUID uuid) {
        BigDecimal cached = cache.get(uuid);

        if (cached != null) {
            return cached;
        }
        if (Bukkit.isPrimaryThread()) {
            return BigDecimal.ZERO;
        }

        try {
            return getBalance(uuid).join();
            
        } catch (Exception ex) {


            BigDecimal refreshed = cache.get(uuid);
            return refreshed != null ? refreshed : BigDecimal.ZERO;
        }
    }

    @Override
    public CompletableFuture<Boolean> has(UUID uuid, BigDecimal amount) {
        return getBalance(uuid).thenApply(balance ->
                balance.compareTo(amount) >= 0
        );
    }

    @Override
    public CompletableFuture<Boolean> withdraw(UUID uuid, BigDecimal amount) {
        if (amount.compareTo(minTransaction) < 0) {
            return CompletableFuture.completedFuture(false);
        }

        return database.async(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE economy SET balance = balance - ?, last_updated = strftime('%s', 'now') WHERE uuid = ? AND balance >= ?"
            )) {
                ps.setDouble(1, amount.doubleValue());
                ps.setString(2, uuid.toString());
                ps.setDouble(3, amount.doubleValue());
                boolean success = ps.executeUpdate() > 0;
                if (success) {
                    cache.remove(uuid);
                    EconomyCharts.trackWithdraw();
                }
                return success;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> deposit(UUID uuid, BigDecimal amount) {
        if (amount.compareTo(minTransaction) < 0) {
            return CompletableFuture.completedFuture(false);
        }

        if (maxBalance != null) {
            return getBalance(uuid).thenCompose(current -> {
                if (current.add(amount).compareTo(maxBalance) > 0) {
                    return CompletableFuture.completedFuture(false);
                }
                return performDeposit(uuid, amount);
            });
        }

        return performDeposit(uuid, amount);
    }

    private CompletableFuture<Boolean> performDeposit(UUID uuid, BigDecimal amount) {
        return database.async(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE economy SET balance = balance + ?, last_updated = strftime('%s', 'now') WHERE uuid = ?"
            )) {
                ps.setDouble(1, amount.doubleValue());
                ps.setString(2, uuid.toString());
                int updated = ps.executeUpdate();

                if (updated > 0) {
                    cache.remove(uuid);
                    EconomyCharts.trackDeposit();
                    return true;
                }
            }

            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name == null) {
                plugin.getLogger().warning("Cannot deposit to " + uuid + " - player name not found");
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO economy (uuid, username, balance) VALUES (?, ?, ?)"
            )) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setDouble(3, amount.doubleValue());
                cache.remove(uuid);
                EconomyCharts.trackDeposit();
                return ps.executeUpdate() > 0;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> setBalance(UUID uuid, BigDecimal amount) {
        if (maxBalance != null && amount.compareTo(maxBalance) > 0) {
            return CompletableFuture.completedFuture(false);
        }

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return CompletableFuture.completedFuture(false);
        }

        return database.async(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE economy SET balance = ?, last_updated = strftime('%s', 'now') WHERE uuid = ?"
            )) {
                ps.setDouble(1, amount.doubleValue());
                ps.setString(2, uuid.toString());
                cache.remove(uuid);
                return ps.executeUpdate() > 0;
            }
        });
    }

    @Override
    public CompletableFuture<Map<UUID, BigDecimal>> getTopBalances(int limit) {
        return database.async(conn -> {
            Map<UUID, BigDecimal> top = new LinkedHashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT uuid, balance FROM economy ORDER BY balance DESC LIMIT ?"
            )) {
                ps.setInt(1, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    top.put(
                            UUID.fromString(rs.getString("uuid")),
                            BigDecimal.valueOf(rs.getDouble("balance"))
                    );
                }
            }
            return top;
        });
    }

    @Override
    public String format(BigDecimal amount) {
        String formattedAmount = decimalFormat.format(amount);
        String currency = amount.compareTo(BigDecimal.ONE) == 0 ? currencySingular : currencyPlural;

        if (plugin.getConfigManager().isCurrencyBeforeAmount()) {
            return currency + " " + formattedAmount;
        }

        return formattedAmount + " " + currency;
    }

    public String formatPlain(BigDecimal amount) {
        return decimalFormat.format(amount);
    }

    public String formatAbbreviated(BigDecimal amount) {
        double value = amount.doubleValue();
        String suffix;
        double divisor;

        if (Math.abs(value) >= 1_000_000_000) {
            suffix = "B";
            divisor = 1_000_000_000;
        } else if (Math.abs(value) >= 1_000_000) {
            suffix = "M";
            divisor = 1_000_000;
        } else if (Math.abs(value) >= 1_000) {
            suffix = "k";
            divisor = 1_000;
        } else {
            String formattedAmount = decimalFormat.format(amount);
            String currency = amount.compareTo(BigDecimal.ONE) == 0 ? currencySingular : currencyPlural;
            return plugin.getConfigManager().isCurrencyBeforeAmount()
                    ? currency + " " + formattedAmount
                    : formattedAmount + " " + currency;
        }

        String abbreviated = new java.text.DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US))
                .format(value / divisor);
        return abbreviated + suffix;
    }

    @Override
    public String currencyNameSingular() {
        return currencySingular;
    }

    @Override
    public String currencyNamePlural() {
        return currencyPlural;
    }

    @Override
    public boolean isVault() {
        return false;
    }

    public BigDecimal getMinTransaction() {
        return minTransaction;
    }

    public BigDecimal getMaxBalance() {
        return maxBalance;
    }

    public boolean hasMaxBalance() {
        return maxBalance != null;
    }

    public void invalidateCache(UUID uuid) {
        cache.remove(uuid);
    }

    public void shutdown() {
        database.disconnect();
    }

    public Database getDatabase() {
        return database;
    }

    public BigDecimal getStartingBalance() {
        return startingBalance;
    }

    public EssentialsC getPlugin() {
        return plugin;
    }
}