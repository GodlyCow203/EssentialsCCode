package net.godlycow.org.essc.command.admin;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.modules.punishment.PunishmentManager;
import net.godlycow.org.essc.util.DurationParser;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

public class BanIpCommand extends Command {

    private final PunishmentManager punishmentManager;

    public BanIpCommand(EssentialsC plugin, PunishmentManager punishmentManager) {
        super(plugin, "ban-ip", "essentialsc.banip", false, 1, "command.usage.banip");
        this.punishmentManager = punishmentManager;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String target = args[0];
        String ip;
        String targetName = target;

        if (isValidIp(target)) {
            ip = target;
        } else {
            Player onlineTarget = plugin.getBedrockUtil().resolvePlayer(target);
            if (onlineTarget == null) onlineTarget = plugin.getServer().getPlayerExact(target);

            if (onlineTarget != null && onlineTarget.getAddress() != null) {
                ip = onlineTarget.getAddress().getAddress().getHostAddress();
                targetName = onlineTarget.getName();
            } else {
                OfflinePlayer offlineTarget = plugin.getServer().getOfflinePlayer(target);
                if (offlineTarget.hasPlayedBefore()) {
                    String storedIp = plugin.getUserManager() != null
                            ? plugin.getUserManager().getLastIp(offlineTarget.getUniqueId())
                            : null;
                    if (storedIp != null) {
                        ip = storedIp;
                        targetName = offlineTarget.getName();
                        sender.sendMessage(lang.get(sender, "banip.stored_ip_used", Map.of("ip", ip, "player", targetName)));
                    } else {
                        sender.sendMessage(lang.get(sender, "banip.no_stored_ip", Map.of("player", target)));
                        return true;
                    }
                } else {
                    sender.sendMessage(lang.get(sender, "error.player_not_found", Map.of("player", target)));
                    return true;
                }
            }
        }

        Player onlineWithIp = getOnlinePlayerByIp(ip);
        if (onlineWithIp != null && onlineWithIp.hasPermission("essentialsc.ban.exempt")) {
            if (!(sender instanceof Player playerSender) || !playerSender.hasPermission("essentialsc.ban.exempt.bypass")) {
                sender.sendMessage(lang.get(sender, "banip.exempt"));
                plugin.debug("Denied: IP " + ip + " belongs to exempt player " + onlineWithIp.getName());
                return true;
            }
        }

        if (sender instanceof Player playerSender && playerSender.getAddress() != null
                && playerSender.getAddress().getAddress().getHostAddress().equals(ip)) {
            sender.sendMessage(lang.get(sender, "banip.cannot_ban_self"));
            return true;
        }

        long duration = 0;
        int reasonStart = 1;

        if (args.length > 1 && args[1].startsWith("-t:")) {
            duration = DurationParser.parse(args[1].substring(3));
            if (duration == DurationParser.INVALID) {
                sender.sendMessage(lang.get(sender, "error.invalid_duration"));
                return true;
            }
            if (duration == DurationParser.PERMANENT) duration = 0;
            reasonStart = 2;
        }

        String reason = buildReason(args, reasonStart, "Breaking server rules");
        long expires = duration > 0 ? System.currentTimeMillis() + duration : -1;

        plugin.debug("IP Banning " + ip + " (resolved from: " + targetName + ") by " + sender.getName()
                + " for: " + reason + " expires: " + expires);

        punishmentManager.banIp(ip, reason, sender.getName(), expires);

        int kickedCount = kickPlayersWithIp(ip, sender, reason, duration);
        String durationStr = DurationParser.format(duration);

        plugin.getServer().broadcast(lang.get(sender, duration > 0 ? "banip.broadcast_temp" : "banip.broadcast", Map.of(
                "ip",       ip,
                "target",   targetName,
                "banner",   sender.getName(),
                "reason",   reason,
                "duration", durationStr,
                "count",    String.valueOf(kickedCount)
        )), "essentialsc.banip.notify");

        sender.sendMessage(lang.get(sender, "banip.success", Map.of(
                "ip",       ip,
                "target",   targetName,
                "duration", durationStr,
                "count",    String.valueOf(kickedCount)
        )));

        if (plugin.getConfigManager().isDiscordSRVEnabled()) {
            if (plugin.getDiscordSRVHook() != null && plugin.getDiscordSRVHook().isHooked()) {
                plugin.getDiscordSRVHook().sendBanIpEmbed(ip, targetName, reason, sender.getName(),
                        expires > 0 ? expires : -1);
            } else {
                sender.sendMessage(lang.get(sender, "error.missing_plugin.discordsrv"));
            }
        }

        return true;
    }

    private String buildReason(String[] args, int start, String defaultReason) {
        if (args.length <= start) return defaultReason;
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) sb.append(" ");
            sb.append(args[i]);
        }
        String reason = sb.toString().trim();
        return reason.isEmpty() ? defaultReason : reason;
    }

    private boolean isValidIp(String input) {
        try { InetAddress.getByName(input); return true; }
        catch (UnknownHostException e) { return false; }
    }

    private Player getOnlinePlayerByIp(String ip) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getAddress() != null
                    && player.getAddress().getAddress().getHostAddress().equals(ip)) {
                return player;
            }
        }
        return null;
    }

    private int kickPlayersWithIp(String ip, CommandSender banner, String reason, long duration) {
        int count = 0;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getAddress() != null
                    && player.getAddress().getAddress().getHostAddress().equals(ip)) {
                player.kick(lang.get(player, "banip.screen_message", Map.of(
                        "reason",   reason,
                        "banner",   banner.getName(),
                        "duration", DurationParser.format(duration),
                        "ip",       ip
                )));
                count++;
            }
        }
        return count;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> completions = plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
            if (args[0].matches("\\d+\\.?\\d*")) completions.add("192.168.1.1");
            return completions;
        }
        if (args.length == 2 && args[1].isEmpty()) {
            return Arrays.asList("-t:1h", "-t:1d", "-t:7d", "-t:30d", "-t:perm");
        }
        return Collections.emptyList();
    }
}