package net.godlycow.org.essc.plugin.economy;

import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EconomyService {

    CompletableFuture<Boolean> hasAccount(UUID uuid);

    CompletableFuture<Boolean> createAccount(UUID uuid, String name);

    CompletableFuture<BigDecimal> getBalance(UUID uuid);

    CompletableFuture<Boolean> has(UUID uuid, BigDecimal amount);

    CompletableFuture<Boolean> withdraw(UUID uuid, BigDecimal amount);

    CompletableFuture<Boolean> deposit(UUID uuid, BigDecimal amount);

    CompletableFuture<Boolean> setBalance(UUID uuid, BigDecimal amount);

    CompletableFuture<Map<UUID, BigDecimal>> getTopBalances(int limit);

    String format(BigDecimal amount);

    String currencyNameSingular();

    String currencyNamePlural();

    boolean isVault();
}