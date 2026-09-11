package net.godlycow.org.essc.plugin.economy;

import net.godlycow.org.essc.EssentialsC;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VaultHook {
    private final EconomyManager economyManager;
    private final VaultEconomy vaultEconomy;
    private boolean hooked = false;

    public VaultHook(EconomyManager economyManager) {
        this.economyManager = economyManager;
        this.vaultEconomy = new VaultEconomy();
    }

    public boolean hook() {
        economyManager.getPlugin().debug("Attempting Vault economy registration...");
        try {
            Class.forName("net.milkbowl.vault.economy.Economy");
        } catch (ClassNotFoundException ignored) {
            economyManager.getPlugin().debug("Vault class not found, skipping registration.");
            return false;
        }

        Bukkit.getServicesManager().register(
                Economy.class,
                vaultEconomy,
                JavaPlugin.getPlugin(EssentialsC.class),
                ServicePriority.High
        );
        hooked = true;
        economyManager.getPlugin().debug("Vault economy registered with ServicePriority.High.");
        return true;
    }

    public boolean isHooked() {
        return hooked;
    }

    private BigDecimal getBalanceSync(UUID uuid) {
        try (Connection conn = economyManager.getDatabase().openFreshConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT balance FROM economy WHERE uuid = ?"
            )) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return BigDecimal.valueOf(rs.getDouble("balance"));
                }
            }
        } catch (SQLException e) {
            economyManager.getPlugin().getLogger().severe("Vault sync balance error: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    private boolean hasAccountSync(UUID uuid) {
        try (Connection conn = economyManager.getDatabase().openFreshConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM economy WHERE uuid = ?"
            )) {
                ps.setString(1, uuid.toString());
                return ps.executeQuery().next();
            }
        } catch (SQLException e) {
            economyManager.getPlugin().getLogger().severe("Vault sync hasAccount error: " + e.getMessage());
        }
        return false;
    }

    private boolean withdrawSync(UUID uuid, BigDecimal amount) {
        economyManager.getPlugin().debug("Vault: withdrawSync " + amount + " from " + uuid);
        try (Connection conn = economyManager.getDatabase().openFreshConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE economy SET balance = balance - ?, last_updated = strftime('%s', 'now') WHERE uuid = ? AND balance >= ?"
            )) {
                ps.setDouble(1, amount.doubleValue());
                ps.setString(2, uuid.toString());
                ps.setDouble(3, amount.doubleValue());
                boolean success = ps.executeUpdate() > 0;
                if (success) {
                    economyManager.invalidateCache(uuid);
                }
                return success;
            }
        } catch (SQLException e) {
            economyManager.getPlugin().getLogger().severe("Vault sync withdraw error: " + e.getMessage());
        }
        return false;
    }

    private boolean depositSync(UUID uuid, BigDecimal amount) {
        economyManager.getPlugin().debug("Vault: depositSync " + amount + " to " + uuid);
        try (Connection conn = economyManager.getDatabase().openFreshConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE economy SET balance = balance + ?, last_updated = strftime('%s', 'now') WHERE uuid = ?"
            )) {
                ps.setDouble(1, amount.doubleValue());
                ps.setString(2, uuid.toString());
                if (ps.executeUpdate() > 0) {
                    economyManager.invalidateCache(uuid);
                    return true;
                }
            }

            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name == null) return false;

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO economy (uuid, username, balance) VALUES (?, ?, ?)"
            )) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setDouble(3, amount.doubleValue());
                boolean success = ps.executeUpdate() > 0;
                if (success) {
                    economyManager.invalidateCache(uuid);
                }
                return success;
            }
        } catch (SQLException e) {
            economyManager.getPlugin().getLogger().severe("Vault sync deposit error: " + e.getMessage());
        }
        return false;
    }

    public class VaultEconomy implements Economy {

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public String getName() {
            return "EssentialsCEconomy";
        }

        @Override
        public boolean hasBankSupport() {
            return false;
        }

        @Override
        public int fractionalDigits() {
            return 2;
        }

        @Override
        public String format(double amount) {
            return economyManager.format(BigDecimal.valueOf(amount));
        }

        @Override
        public String currencyNamePlural() {
            return economyManager.currencyNamePlural();
        }

        @Override
        public String currencyNameSingular() {
            return economyManager.currencyNameSingular();
        }

        @Override
        public boolean hasAccount(String playerName) {
            return hasAccount(Bukkit.getOfflinePlayer(playerName));
        }

        @Override
        public boolean hasAccount(OfflinePlayer player) {
            return hasAccountSync(player.getUniqueId());
        }

        @Override
        public boolean hasAccount(String playerName, String worldName) {
            return hasAccount(playerName);
        }

        @Override
        public boolean hasAccount(OfflinePlayer player, String worldName) {
            return hasAccount(player);
        }

        @Override
        public double getBalance(String playerName) {
            return getBalance(Bukkit.getOfflinePlayer(playerName));
        }

        @Override
        public double getBalance(OfflinePlayer player) {
            return getBalanceSync(player.getUniqueId()).doubleValue();
        }

        @Override
        public double getBalance(String playerName, String world) {
            return getBalance(playerName);
        }

        @Override
        public double getBalance(OfflinePlayer player, String world) {
            return getBalance(player);
        }

        @Override
        public boolean has(String playerName, double amount) {
            return has(Bukkit.getOfflinePlayer(playerName), amount);
        }

        @Override
        public boolean has(OfflinePlayer player, double amount) {
            return getBalanceSync(player.getUniqueId()).compareTo(BigDecimal.valueOf(amount)) >= 0;
        }

        @Override
        public boolean has(String playerName, String worldName, double amount) {
            return has(playerName, amount);
        }

        @Override
        public boolean has(OfflinePlayer player, String worldName, double amount) {
            return has(player, amount);
        }

        @Override
        public EconomyResponse withdrawPlayer(String playerName, double amount) {
            return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount);
        }

        @Override
        public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
            BigDecimal amt = BigDecimal.valueOf(amount);
            boolean success = withdrawSync(player.getUniqueId(), amt);
            double newBal = getBalanceSync(player.getUniqueId()).doubleValue();

            if (success) {
                return new EconomyResponse(amount, newBal, EconomyResponse.ResponseType.SUCCESS, null);
            } else {
                return new EconomyResponse(0, newBal, EconomyResponse.ResponseType.FAILURE, "Insufficient funds");
            }
        }

        @Override
        public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
            return withdrawPlayer(playerName, amount);
        }

        @Override
        public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
            return withdrawPlayer(player, amount);
        }

        @Override
        public EconomyResponse depositPlayer(String playerName, double amount) {
            return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount);
        }

        @Override
        public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
            BigDecimal amt = BigDecimal.valueOf(amount);
            boolean success = depositSync(player.getUniqueId(), amt);
            double newBal = getBalanceSync(player.getUniqueId()).doubleValue();

            return new EconomyResponse(amount, newBal,
                    success ? EconomyResponse.ResponseType.SUCCESS : EconomyResponse.ResponseType.FAILURE,
                    success ? null : "Deposit failed");
        }

        @Override
        public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
            return depositPlayer(playerName, amount);
        }

        @Override
        public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
            return depositPlayer(player, amount);
        }

        @Override
        public EconomyResponse createBank(String name, String player) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support");
        }

        @Override
        public EconomyResponse createBank(String name, OfflinePlayer player) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support");
        }

        @Override
        public EconomyResponse deleteBank(String name) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support");
        }

        @Override
        public EconomyResponse bankBalance(String name) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support");
        }

        @Override
        public EconomyResponse bankHas(String name, double amount) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support");
        }

        @Override
        public EconomyResponse bankWithdraw(String name, double amount) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support");
        }

        @Override
        public EconomyResponse bankDeposit(String name, double amount) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support");
        }

        @Override
        public EconomyResponse isBankOwner(String name, String playerName) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support");
        }

        @Override
        public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support");
        }

        @Override
        public EconomyResponse isBankMember(String name, String playerName) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support");
        }

        @Override
        public EconomyResponse isBankMember(String name, OfflinePlayer player) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support");
        }

        @Override
        public List<String> getBanks() {
            return new ArrayList<>();
        }

        @Override
        public boolean createPlayerAccount(String playerName) {
            return createPlayerAccount(Bukkit.getOfflinePlayer(playerName));
        }

        @Override
        public boolean createPlayerAccount(OfflinePlayer player) {
            if (hasAccountSync(player.getUniqueId())) {
                return false;
            }
            return depositSync(player.getUniqueId(), economyManager.getStartingBalance());
        }

        @Override
        public boolean createPlayerAccount(String playerName, String worldName) {
            return createPlayerAccount(playerName);
        }

        @Override
        public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
            return createPlayerAccount(player);
        }
    }
}