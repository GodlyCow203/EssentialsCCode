package net.godlycow.org.essc.integration.placeholderapi;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.afk.AFKManager;
import org.bukkit.entity.Player;

import java.util.List;

public class AFKPlaceholders {

    private final EssentialsC plugin;

    public AFKPlaceholders(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public String onRequest(Player player, String identifier) {
        if (!identifier.startsWith("afk_")) {
            return null;
        }

        AFKManager manager = plugin.getAfkManager();
        if (manager == null) {
            return "";
        }

        return switch (identifier.toLowerCase()) {
            case "afk_status" -> manager.isAFK(player) ? "AFK" : "Active";
            case "afk_boolean" -> String.valueOf(manager.isAFK(player));
            case "afk_duration" -> manager.isAFK(player)
                    ? String.valueOf(manager.getAFKDurationSeconds(player))
                    : "0";
            case "afk_duration_formatted" -> manager.isAFK(player)
                    ? manager.getAFKDurationFormatted(player)
                    : "0s";
            case "afk_count" -> String.valueOf(manager.getAFKCount());
            default -> null;
        };
    }

    public static List<String> getPlaceholderList() {
        return List.of(
                "%essc_afk_status%            — 'AFK' or 'Active'",
                "%essc_afk_boolean%           — 'true' or 'false'",
                "%essc_afk_duration%          — seconds the player has been AFK (0 if not AFK)",
                "%essc_afk_duration_formatted% — formatted AFK duration e.g. '5m 30s'",
                "%essc_afk_count%             — total number of AFK players online"
        );
    }
}