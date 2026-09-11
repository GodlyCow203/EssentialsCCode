package net.godlycow.org.essc.integration.placeholderapi;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.home.Home;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HomePlaceholders {

    private static final long CACHE_DURATION_MS = 5_000L;

    private final EssentialsC plugin;

    private final Map<UUID, List<Home>> homeCache      = new ConcurrentHashMap<>();
    private final Map<UUID, Long>       cacheTimestamps = new ConcurrentHashMap<>();
    private final Set<UUID> fetching = ConcurrentHashMap.newKeySet();

    public HomePlaceholders(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public String onRequest(Player player, String identifier) {
        if (!identifier.startsWith("home_")) return null;

        refreshCacheIfNeeded(player);

        List<Home> homes = homeCache.getOrDefault(player.getUniqueId(), Collections.emptyList());

        return switch (identifier.toLowerCase()) {
            case "home_count"     -> String.valueOf(homes.size());
            case "home_max"       -> formatMax(plugin.getHomeManager().getMaxHomes(player));
            case "home_remaining" -> getRemainingHomes(player, homes.size());
            case "home_list"      -> homes.isEmpty()
                    ? "None"
                    : String.join(", ", homes.stream().map(Home::getName).toList());
            default -> {
                if (identifier.toLowerCase().startsWith("home_name_")) {
                    yield getHomeNameAtIndex(homes, identifier.substring("home_name_".length()));
                }
                yield null;
            }
        };
    }

    private void refreshCacheIfNeeded(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cacheTimestamps.get(uuid);

        if (last != null && (now - last) < CACHE_DURATION_MS) return;
        if (!fetching.add(uuid)) return;

        plugin.getHomeManager().getHomes(uuid).thenAccept(homes -> {
            homeCache.put(uuid, homes);
            cacheTimestamps.put(uuid, System.currentTimeMillis());
            fetching.remove(uuid);
        }).exceptionally(err -> {
            fetching.remove(uuid);
            return null;
        });
    }

    private String getRemainingHomes(Player player, int currentCount) {
        int max = plugin.getHomeManager().getMaxHomes(player);
        return max == Integer.MAX_VALUE ? "∞" : String.valueOf(Math.max(0, max - currentCount));
    }

    private String formatMax(int max) {
        return max == Integer.MAX_VALUE ? "∞" : String.valueOf(max);
    }

    private String getHomeNameAtIndex(List<Home> homes, String indexStr) {
        try {
            int index = Integer.parseInt(indexStr);
            return (index >= 0 && index < homes.size()) ? homes.get(index).getName() : "";
        } catch (NumberFormatException e) {
            return "";
        }
    }

    public void clearCache(UUID uuid) {
        homeCache.remove(uuid);
        cacheTimestamps.remove(uuid);
        fetching.remove(uuid);
    }

    public static List<String> getPlaceholderList() {
        return List.of(
                "%essc_home_count%      — number of homes set",
                "%essc_home_max%        — maximum homes allowed",
                "%essc_home_remaining%  — remaining home slots",
                "%essc_home_list%       — comma-separated list of home names",
                "%essc_home_name_<n>%   — name of home at index n (0-based)"
        );
    }
}