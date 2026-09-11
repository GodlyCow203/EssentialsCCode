package net.godlycow.org.essc.integration.placeholderapi;

import net.godlycow.org.essc.EssentialsC;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlaytimePlaceholders {

    private final EssentialsC plugin;

    public PlaytimePlaceholders(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public String onRequest(Player player, String identifier) {
        if (!identifier.startsWith("playtime_")) {
            return null;
        }

        long ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        long ms = ticks * 50L;

        return switch (identifier.toLowerCase()) {
            case "playtime_ticks" -> String.valueOf(ticks);
            case "playtime_seconds" -> String.valueOf(TimeUnit.MILLISECONDS.toSeconds(ms));
            case "playtime_minutes" -> String.valueOf(TimeUnit.MILLISECONDS.toMinutes(ms));
            case "playtime_hours" -> String.valueOf(TimeUnit.MILLISECONDS.toHours(ms));
            case "playtime_days" -> String.valueOf(TimeUnit.MILLISECONDS.toDays(ms));
            case "playtime_formatted" -> formatPlaytime(ms);
            default -> null;
        };
    }

    private String formatPlaytime(long ms) {
        long days = TimeUnit.MILLISECONDS.toDays(ms);
        long hours = TimeUnit.MILLISECONDS.toHours(ms) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;

        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        }
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    public static List<String> getPlaceholderList() {
        return List.of(
                "%essc_playtime_ticks%     — raw playtime in ticks",
                "%essc_playtime_seconds%   — total playtime in seconds",
                "%essc_playtime_minutes%   — total playtime in minutes",
                "%essc_playtime_hours%     — total playtime in hours",
                "%essc_playtime_days%      — total playtime in days",
                "%essc_playtime_formatted% — formatted playtime e.g. '3d 2h 15m 40s'"
        );
    }
}