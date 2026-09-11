package net.godlycow.org.essc.integration.placeholderapi;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.warp.Warp;
import net.godlycow.org.essc.modules.warp.WarpManager;
import org.bukkit.entity.Player;

import java.util.List;

public class WarpPlaceholders {

    private final EssentialsC plugin;

    public WarpPlaceholders(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public String onRequest(Player player, String identifier) {
        if (!identifier.startsWith("warp_")) {
            return null;
        }

        WarpManager manager = plugin.getWarpManager();
        if (manager == null) {
            return "";
        }

        if (identifier.toLowerCase().startsWith("warp_cooldown_")) {
            String warpName = identifier.substring("warp_cooldown_".length());
            long remaining = manager.getRemainingCooldown(player.getUniqueId());
            if (warpName.equals("formatted")) {
                return formatSeconds(remaining);
            }
            return String.valueOf(remaining);
        }

        if (identifier.toLowerCase().startsWith("warp_uses_")) {
            String warpName = identifier.substring("warp_uses_".length());
            return String.valueOf(manager.getWarpUsage(player.getUniqueId(), warpName));
        }

        if (identifier.toLowerCase().startsWith("warp_exists_")) {
            String warpName = identifier.substring("warp_exists_".length());
            return String.valueOf(manager.warpExists(warpName));
        }

        return switch (identifier.toLowerCase()) {
            case "warp_count" -> String.valueOf(manager.getAllWarps().size());
            case "warp_count_visible" -> String.valueOf(manager.getVisibleWarps().size());
            case "warp_list" -> {
                List<Warp> warps = manager.getVisibleWarps();
                yield warps.isEmpty() ? "None" : String.join(", ", warps.stream().map(Warp::getName).toList());
            }
            case "warp_categories" -> String.valueOf(manager.getCategories().size());
            case "warp_cooldown" -> String.valueOf(manager.getRemainingCooldown(player.getUniqueId()));
            case "warp_cooldown_formatted" -> formatSeconds(manager.getRemainingCooldown(player.getUniqueId()));
            default -> null;
        };
    }

    private String formatSeconds(long seconds) {
        if (seconds <= 0) {
            return "0s";
        }
        long mins = seconds / 60;
        long secs = seconds % 60;
        return mins > 0 ? mins + "m " + secs + "s" : secs + "s";
    }

    public static List<String> getPlaceholderList() {
        return List.of(
                "%essc_warp_count%              — total number of warps",
                "%essc_warp_count_visible%      — number of non-hidden warps",
                "%essc_warp_list%               — comma-separated visible warp names",
                "%essc_warp_categories%         — number of warp categories",
                "%essc_warp_cooldown%           — player's remaining warp cooldown in seconds",
                "%essc_warp_cooldown_formatted% — formatted cooldown e.g. '1m 30s'",
                "%essc_warp_uses_<name>%        — how many times the player used a specific warp",
                "%essc_warp_exists_<name>%      — 'true' if the warp exists"
        );
    }
}