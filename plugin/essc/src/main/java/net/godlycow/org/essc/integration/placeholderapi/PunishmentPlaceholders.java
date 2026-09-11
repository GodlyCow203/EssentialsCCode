package net.godlycow.org.essc.integration.placeholderapi;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.punishment.PunishmentManager;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PunishmentPlaceholders {

    private final EssentialsC plugin;

    public PunishmentPlaceholders(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public String onRequest(Player player, String identifier) {
        if (!identifier.startsWith("punish_")) {
            return null;
        }

        PunishmentManager manager = plugin.getPunishmentManager();
        if (manager == null) {
            return "";
        }

        return switch (identifier.toLowerCase()) {
            case "punish_is_banned" -> String.valueOf(manager.isBanned(player.getUniqueId()));
            case "punish_is_muted" -> String.valueOf(manager.isMuted(player.getUniqueId()));
            case "punish_ban_reason" -> {
                PunishmentManager.BanEntry entry = manager.getBanEntry(player.getUniqueId());
                yield entry != null ? entry.reason() : "";
            }
            case "punish_ban_expires" -> {
                PunishmentManager.BanEntry entry = manager.getBanEntry(player.getUniqueId());
                if (entry == null) {
                    yield "";
                }
                yield entry.expires() == -1 ? "Permanent" : formatExpiry(entry.expires());
            }
            case "punish_mute_reason" -> {
                PunishmentManager.MuteEntry entry = manager.getMuteEntry(player.getUniqueId());
                yield entry != null ? entry.reason() : "";
            }
            case "punish_mute_expires" -> {
                PunishmentManager.MuteEntry entry = manager.getMuteEntry(player.getUniqueId());
                if (entry == null) {
                    yield "";
                }
                yield entry.expires() == -1 ? "Permanent" : formatExpiry(entry.expires());
            }
            case "punish_active_bans" -> String.valueOf(manager.getActiveBans().size());
            case "punish_active_mutes" -> String.valueOf(manager.getAllMutes().stream()
                    .filter(m -> m.expires() == -1 || m.expires() > System.currentTimeMillis())
                    .count());
            default -> null;
        };
    }

    private String formatExpiry(long expiresMs) {
        long remaining = expiresMs - System.currentTimeMillis();
        if (remaining <= 0) {
            return "Expired";
        }
        long days = TimeUnit.MILLISECONDS.toDays(remaining);
        long hours = TimeUnit.MILLISECONDS.toHours(remaining) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60;
        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    public static List<String> getPlaceholderList() {
        return List.of(
                "%essc_punish_is_banned%      — 'true' if player is currently banned",
                "%essc_punish_is_muted%       — 'true' if player is currently muted",
                "%essc_punish_ban_reason%     — reason for the player's active ban",
                "%essc_punish_ban_expires%    — ban expiry as formatted duration or 'Permanent'",
                "%essc_punish_mute_reason%    — reason for the player's active mute",
                "%essc_punish_mute_expires%   — mute expiry as formatted duration or 'Permanent'",
                "%essc_punish_active_bans%    — total number of active bans on the server",
                "%essc_punish_active_mutes%   — total number of active mutes on the server"
        );
    }
}