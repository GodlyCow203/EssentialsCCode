package net.godlycow.org.essc.integration.placeholderapi;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.plugin.economy.EconomyManager;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyPlaceholders {

    private static final long CACHE_DURATION_MS = 3_000L;

    private final EssentialsC plugin;
    private final Map<UUID, BigDecimal> balanceCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> fetching = ConcurrentHashMap.newKeySet();

    public EconomyPlaceholders(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public String onRequest(Player player, String identifier) {
        if (!identifier.startsWith("eco_")) {
            return null;
        }

        EconomyManager manager = plugin.getEconomyManager();
        if (manager == null) {
            return "";
        }

        refreshCacheIfNeeded(player, manager);

        BigDecimal balance = balanceCache.getOrDefault(player.getUniqueId(), BigDecimal.ZERO);

        return switch (identifier.toLowerCase()) {
            case "eco_balance" -> manager.formatPlain(balance);
            case "eco_balance_formatted" -> manager.formatAbbreviated(balance);
            case "eco_currency_singular" -> manager.currencyNameSingular();
            case "eco_currency_plural" -> manager.currencyNamePlural();
            case "eco_max_balance" -> manager.hasMaxBalance()
                    ? manager.formatPlain(manager.getMaxBalance())
                    : "∞";
            case "eco_starting_balance" -> manager.formatPlain(manager.getStartingBalance());
            default -> null;
        };
    }

    private void refreshCacheIfNeeded(Player player, EconomyManager manager) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cacheTimestamps.get(uuid);

        if (last != null && (now - last) < CACHE_DURATION_MS) {
            return;
        }

        if (!fetching.add(uuid)) {
            return;
        }

        manager.getBalance(uuid).thenAccept(balance -> {
            balanceCache.put(uuid, balance);
            cacheTimestamps.put(uuid, System.currentTimeMillis());
            fetching.remove(uuid);
        }).exceptionally(err -> {
            fetching.remove(uuid);
            return null;
        });
    }

    public void clearCache(UUID uuid) {
        balanceCache.remove(uuid);
        cacheTimestamps.remove(uuid);
        fetching.remove(uuid);
    }

    public static List<String> getPlaceholderList() {
        return List.of(
                "%essc_eco_balance%            — raw balance as plain number",
                "%essc_eco_balance_formatted%  — abbreviated balance (e.g. 1.1k, 2.5M)",
                "%essc_eco_currency_singular%  — singular currency name",
                "%essc_eco_currency_plural%    — plural currency name",
                "%essc_eco_max_balance%        — server max balance (∞ if unlimited)",
                "%essc_eco_starting_balance%   — starting balance for new players"
        );
    }
}