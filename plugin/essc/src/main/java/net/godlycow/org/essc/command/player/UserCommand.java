package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.storage.user.UserProfile;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class UserCommand extends Command {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "profile", "states", "punishments", "locations", "cooldowns", "all"
    );

    public UserCommand(EssentialsC plugin) {
        super(plugin, "user", "essentialsc.user", false, 1, "command.usage.user");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String targetName = args[0];
        String subcommand = args.length >= 2 ? args[1].toLowerCase() : "all";

        OfflinePlayer target = plugin.getBedrockUtil().resolveOfflinePlayer(targetName);

        if (target == null || !target.hasPlayedBefore()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", targetName);
            sender.sendMessage(lang.get(sender, "error.player_not_found", placeholders));
            return true;
        }

        UUID targetUuid = target.getUniqueId();
        UserProfile profile = plugin.getUserManager().getCachedProfile(targetUuid);

        if (profile == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", targetName);
            sender.sendMessage(lang.get(sender, "user.error.no_profile", placeholders));
            return true;
        }

        if (!SUBCOMMANDS.contains(subcommand)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("subcommand", subcommand);
            sender.sendMessage(lang.get(sender, "user.error.unknown_subcommand", placeholders));
            return true;
        }

        switch (subcommand) {
            case "profile" -> {
                sender.sendMessage(lang.get(sender, "user.header", Map.of("player", profile.getUsername())));
                sendProfileSection(sender, profile);
                sender.sendMessage(lang.get(sender, "user.footer"));
            }
            case "states" -> {
                sender.sendMessage(lang.get(sender, "user.header", Map.of("player", profile.getUsername())));
                sendStatesSection(sender, profile, targetUuid);
                sender.sendMessage(lang.get(sender, "user.footer"));
            }
            case "punishments" -> {
                sender.sendMessage(lang.get(sender, "user.header", Map.of("player", profile.getUsername())));
                sendPunishmentsSection(sender, profile, targetUuid);
                sender.sendMessage(lang.get(sender, "user.footer"));
            }
            case "locations" -> {
                sender.sendMessage(lang.get(sender, "user.header", Map.of("player", profile.getUsername())));
                sendLocationsSection(sender, profile);
                sender.sendMessage(lang.get(sender, "user.footer"));
            }
            case "cooldowns" -> {
                sender.sendMessage(lang.get(sender, "user.header", Map.of("player", profile.getUsername())));
                sendCooldownsSection(sender, targetUuid);
                sender.sendMessage(lang.get(sender, "user.footer"));
            }
            default -> {
                sender.sendMessage(lang.get(sender, "user.header", Map.of("player", profile.getUsername())));
                sendProfileSection(sender, profile);
                sendStatesSection(sender, profile, targetUuid);
                sendPunishmentsSection(sender, profile, targetUuid);
                sendLocationsSection(sender, profile);
                sendCooldownsSection(sender, targetUuid);
                sender.sendMessage(lang.get(sender, "user.footer"));
            }
        }

        return true;
    }

    private void sendProfileSection(CommandSender sender, UserProfile profile) {
        sender.sendMessage(lang.get(sender, "user.section.profile"));

        Map<String, String> p = new HashMap<>();
        p.put("uuid", profile.getUuid().toString());
        p.put("username", profile.getUsername());
        p.put("last_known_name", profile.getLastKnownName());
        p.put("language", profile.getLanguageCode());
        p.put("first_join", formatTimestamp(profile.getFirstJoinTime()));
        p.put("last_join", formatTimestamp(profile.getLastJoinTime()));
        p.put("created_at", formatTimestamp(profile.getCreatedAt()));
        p.put("updated_at", formatTimestamp(profile.getUpdatedAt()));

        sender.sendMessage(lang.get(sender, "user.profile.uuid", p));
        sender.sendMessage(lang.get(sender, "user.profile.username", p));
        sender.sendMessage(lang.get(sender, "user.profile.last_known_name", p));
        sender.sendMessage(lang.get(sender, "user.profile.language", p));
        sender.sendMessage(lang.get(sender, "user.profile.first_join", p));
        sender.sendMessage(lang.get(sender, "user.profile.last_join", p));
        sender.sendMessage(lang.get(sender, "user.profile.created_at", p));
        sender.sendMessage(lang.get(sender, "user.profile.updated_at", p));

        if (profile.getLastIp() != null && sender.hasPermission("essentialsc.user.ip")) {
            sender.sendMessage(lang.get(sender, "user.profile.last_ip", Map.of("ip", profile.getLastIp())));
        }
    }

    private void sendStatesSection(CommandSender sender, UserProfile profile, UUID targetUuid) {
        sender.sendMessage(lang.get(sender, "user.section.states"));

        sender.sendMessage(lang.get(sender, "user.states.summary",
                Map.of("summary", plugin.getUserManager().getStatesSummary(targetUuid))));

        sender.sendMessage(lang.get(sender, "user.states.fly",
                Map.of("value", booleanKey(plugin.getUserManager().isFlyEnabled(targetUuid)))));

        sender.sendMessage(lang.get(sender, "user.states.vanished",
                Map.of("value", booleanKey(plugin.getUserManager().isVanished(targetUuid)))));

        sender.sendMessage(lang.get(sender, "user.states.tpa_blocked",
                Map.of("value", booleanKey(plugin.getUserManager().isTpaBlocked(targetUuid)))));

        sender.sendMessage(lang.get(sender, "user.states.scoreboard_disabled",
                Map.of("value", booleanKey(plugin.getUserManager().isScoreboardDisabled(targetUuid)))));

        sender.sendMessage(lang.get(sender, "user.states.rules_accepted",
                Map.of("value", booleanKey(plugin.getUserManager().hasAcceptedRules(targetUuid)))));

        UUID lastReplyTarget = plugin.getUserManager().getLastReplyTarget(targetUuid);
        if (lastReplyTarget != null) {
            OfflinePlayer replyTarget = plugin.getServer().getOfflinePlayer(lastReplyTarget);
            String replyName = replyTarget.getName() != null ? replyTarget.getName() : lastReplyTarget.toString();
            sender.sendMessage(lang.get(sender, "user.states.last_reply_target",
                    Map.of("player", replyName)));
        } else {
            sender.sendMessage(lang.get(sender, "user.states.last_reply_target_none"));
        }
    }

    private void sendPunishmentsSection(CommandSender sender, UserProfile profile, UUID targetUuid) {
        sender.sendMessage(lang.get(sender, "user.section.punishments"));

        boolean banned = plugin.getUserManager().isBanned(targetUuid);
        boolean muted = plugin.getUserManager().isMuted(targetUuid);

        if (banned) {
            Map<String, String> p = new HashMap<>();
            p.put("reason", plugin.getUserManager().getBanReason(targetUuid) != null ? plugin.getUserManager().getBanReason(targetUuid) : "-");
            p.put("banner", profile.getBanBanner() != null ? profile.getBanBanner() : "-");
            p.put("time", profile.getBanTime() > 0 ? formatTimestamp(profile.getBanTime()) : "-");
            p.put("expires", profile.getBanExpires() == 0 ? "Never" : formatTimestamp(profile.getBanExpires()));
            sender.sendMessage(lang.get(sender, "user.punishments.banned", p));
        } else {
            sender.sendMessage(lang.get(sender, "user.punishments.not_banned"));
        }

        if (muted) {
            Map<String, String> p = new HashMap<>();
            p.put("reason", plugin.getUserManager().getMuteReason(targetUuid) != null ? plugin.getUserManager().getMuteReason(targetUuid) : "-");
            p.put("muter", profile.getMuteMuter() != null ? profile.getMuteMuter() : "-");
            p.put("time", profile.getMuteTime() > 0 ? formatTimestamp(profile.getMuteTime()) : "-");
            p.put("expires", profile.getMuteExpires() == 0 ? "Never" : formatTimestamp(profile.getMuteExpires()));
            p.put("offline_notification", booleanKey(plugin.getUserManager().isMuteOfflineNotification(targetUuid)));
            sender.sendMessage(lang.get(sender, "user.punishments.muted", p));
        } else {
            sender.sendMessage(lang.get(sender, "user.punishments.not_muted"));
        }
    }

    private void sendLocationsSection(CommandSender sender, UserProfile profile) {
        sender.sendMessage(lang.get(sender, "user.section.locations"));

        Location backLocation = profile.getBackLocation();
        if (backLocation != null) {
            sender.sendMessage(lang.get(sender, "user.locations.back", buildLocationPlaceholders(backLocation)));
        } else {
            sender.sendMessage(lang.get(sender, "user.locations.back_none"));
        }

        Location deathLocation = profile.getDeathLocation();
        if (deathLocation != null) {
            sender.sendMessage(lang.get(sender, "user.locations.death", buildLocationPlaceholders(deathLocation)));
        } else {
            sender.sendMessage(lang.get(sender, "user.locations.death_none"));
        }

        Location logoutLocation = profile.getLogoutLocation();
        if (logoutLocation != null) {
            Map<String, String> p = buildLocationPlaceholders(logoutLocation);
            p.put("logout_time", profile.getLogoutTime() > 0 ? formatTimestamp(profile.getLogoutTime()) : "-");
            sender.sendMessage(lang.get(sender, "user.locations.logout", p));
        } else {
            sender.sendMessage(lang.get(sender, "user.locations.logout_none"));
        }
    }

    private void sendCooldownsSection(CommandSender sender, UUID targetUuid) {
        sender.sendMessage(lang.get(sender, "user.section.cooldowns"));

        long rtpLastUsed = plugin.getUserManager().getRtpLastUsed(targetUuid);
        if (rtpLastUsed > 0) {
            sender.sendMessage(lang.get(sender, "user.cooldowns.rtp", Map.of("time", formatTimestamp(rtpLastUsed))));
        } else {
            sender.sendMessage(lang.get(sender, "user.cooldowns.rtp_never"));
        }

        long spawnLastTeleport = plugin.getUserManager().getSpawnLastTeleport(targetUuid);
        if (spawnLastTeleport > 0) {
            sender.sendMessage(lang.get(sender, "user.cooldowns.spawn", Map.of("time", formatTimestamp(spawnLastTeleport))));
        } else {
            sender.sendMessage(lang.get(sender, "user.cooldowns.spawn_never"));
        }
    }

    private Map<String, String> buildLocationPlaceholders(Location loc) {
        Map<String, String> p = new HashMap<>();
        p.put("world", loc.getWorld().getName());
        p.put("x", String.valueOf(loc.getBlockX()));
        p.put("y", String.valueOf(loc.getBlockY()));
        p.put("z", String.valueOf(loc.getBlockZ()));
        return p;
    }

    private String booleanKey(boolean value) {
        return value ? "yes" : "no";
    }

    private String formatTimestamp(long epochSeconds) {
        if (epochSeconds <= 0) {
            return "Never";
        }
        long millis = epochSeconds * 1000L;
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(millis));
        return date + " (" + formatTimeAgo(millis) + ")";
    }

    private String formatTimeAgo(long millis) {
        long diff = System.currentTimeMillis() - millis;
        if (diff < 0) return "in the future";
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
        if (days > 0) return days + "d " + hours + "h ago";
        if (hours > 0) return hours + "h " + minutes + "m ago";
        if (minutes > 0) return minutes + "m ago";
        return "just now";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
            if (sender.hasPermission("essentialsc.user.offline")) {
                for (OfflinePlayer offlinePlayer : plugin.getServer().getOfflinePlayers()) {
                    if (offlinePlayer.getName() != null
                            && offlinePlayer.getName().toLowerCase().startsWith(partial)
                            && !completions.contains(offlinePlayer.getName())) {
                        completions.add(offlinePlayer.getName());
                    }
                }
            }
        } else if (args.length == 2) {
            String partial = args[1].toLowerCase();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(partial)) {
                    completions.add(sub);
                }
            }
        }

        return completions;
    }
}
