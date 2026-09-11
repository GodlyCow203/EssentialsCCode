package net.godlycow.org.essc.migration.saver;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.migration.mapper.UserDataMapper.EconomyTransfer;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public class Economy {
    private final EssentialsC plugin;

    public Economy(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<EconomyResult> write(EconomyTransfer data, boolean dryRun) {
        if (plugin.getEconomyManager() == null) {
            return CompletableFuture.completedFuture(new EconomyResult(false, "Economy manager not available", BigDecimal.ZERO));
        }

        return ensureAccountExists(data).thenCompose(exists -> {
            if (!exists) {
                return CompletableFuture.completedFuture(
                        new EconomyResult(false, "Failed to create account", BigDecimal.ZERO)
                );
            }
            return performTransfer(data, dryRun);
        });
    }

    private CompletableFuture<Boolean> ensureAccountExists(EconomyTransfer data) {
        return plugin.getEconomyManager().hasAccount(data.uuid()).thenCompose(hasAccount -> {
            if (hasAccount) {
                return CompletableFuture.completedFuture(true);
            }
            return plugin.getEconomyManager().createAccount(data.uuid(), data.username());
        });
    }

    private CompletableFuture<EconomyResult> performTransfer(EconomyTransfer data, boolean dryRun) {
        if (dryRun) {
            return plugin.getEconomyManager().getBalance(data.uuid()).thenApply(currentBalance ->
                    new EconomyResult(true, "DRY RUN - Would set balance to " + data.balance(), currentBalance)
            );
        }

        return plugin.getEconomyManager().getBalance(data.uuid()).thenCompose(oldBalance ->
                plugin.getEconomyManager().setBalance(data.uuid(), data.balance()).thenApply(success -> {
                    if (success) {
                        plugin.debug("Migrated economy for " + data.username() + " (" + data.uuid() + "): " + oldBalance + " -> " + data.balance());
                        return new EconomyResult(true, null, oldBalance);
                    } else {
                        return new EconomyResult(false, "setBalance returned false", oldBalance);
                    }
                })
        );
    }

    public record EconomyResult(boolean success, String error, BigDecimal previousBalance) {}
}