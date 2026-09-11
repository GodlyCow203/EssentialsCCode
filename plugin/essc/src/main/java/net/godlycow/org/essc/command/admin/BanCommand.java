package net.godlycow.org.essc.command.admin;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.modules.punishment.PunishmentManager;
import net.godlycow.org.essc.util.DurationParser;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BanCommand extends Command {

    private final PunishmentManager punishmentManager;

    public BanCommand(EssentialsC plugin, PunishmentManager punishmentManager) {
        super(plugin, "ban", "essentialsc.ban", false, 1, "command.usage.ban");
        this.punishmentManager = punishmentManager;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String targetName = args[0];
        OfflinePlayer target;
        Player onlineResolved = plugin.getBedrockUtil().resolvePlayer(targetName);
        if (onlineResolved != null) {
            target = onlineResolved;
        } else {
            target = plugin.getServer().getOfflinePlayer(targetName);
        }

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(lang.get(sender, "error.player_not_found", Map.of("player", targetName)));
            return true;
        }

        if (sender instanceof Player playerSender && target.getUniqueId().equals(playerSender.getUniqueId())) {
            sender.sendMessage(lang.get(sender, "ban.cannot_ban_self"));
            return true;
        }

        if (target.isOnline()) {
            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null && onlineTarget.hasPermission("essentialsc.ban.exempt")) {
                if (!(sender instanceof Player playerSender) || !playerSender.hasPermission("essentialsc.ban.exempt.bypass")) {
                    sender.sendMessage(lang.get(sender, "ban.exempt"));
                    plugin.debug("Denied: " + target.getName() + " is exempt from being banned");
                    return true;
                }
            }
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
        plugin.debug("Banning " + target.getName() + " by " + sender.getName() + " for: " + reason + " expires: " + expires);

        PunishmentManager.BanEntry banEntry = punishmentManager.banPlayer(target.getUniqueId(), target.getName(), reason, sender.getName(), expires);

        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().kick(lang.get(target.getPlayer(), "ban.screen_message", Map.of(
                    "reason",   reason,
                    "banner",   sender.getName(),
                    "duration", DurationParser.format(duration),
                    "player",   target.getName(),
                    "date",     LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    "id",       banEntry.id()
            )));
        }

        String durationStr = DurationParser.format(duration);
        plugin.getServer().broadcast(lang.get(sender, duration > 0 ? "ban.broadcast_temp" : "ban.broadcast", Map.of(
                "target",   target.getName(),
                "banner",   sender.getName(),
                "reason",   reason,
                "duration", durationStr
        )), "essentialsc.ban.notify");

        sender.sendMessage(lang.get(sender, "ban.success", Map.of(
                "target",   target.getName(),
                "duration", durationStr
        )));

        if (plugin.getConfigManager().isDiscordSRVEnabled()) {
            if (plugin.getDiscordSRVHook() != null && plugin.getDiscordSRVHook().isHooked()) {
                plugin.getDiscordSRVHook().sendBanEmbed(
                        target.getUniqueId(), target.getName(), reason, sender.getName(),
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

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .filter(p -> !p.equals(sender))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && args[1].isEmpty()) {
            return Arrays.asList("-t:1h", "-t:1d", "-t:7d", "-t:30d");
        }
        return Collections.emptyList();
    }
}