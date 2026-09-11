package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class SeenCommand extends Command {

    public SeenCommand(EssentialsC plugin) {
        super(plugin, "seen", "essentialsc.seen", false, 1, "command.usage.seen");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String targetName = args[0];

        Player onlineTarget = plugin.getBedrockUtil().resolvePlayer(targetName);
        if (onlineTarget != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", onlineTarget.getName());
            placeholders.put("world", onlineTarget.getWorld().getName());
            placeholders.put("x", String.valueOf(onlineTarget.getLocation().getBlockX()));
            placeholders.put("y", String.valueOf(onlineTarget.getLocation().getBlockY()));
            placeholders.put("z", String.valueOf(onlineTarget.getLocation().getBlockZ()));

            sender.sendMessage(lang.get(sender, "seen.online", placeholders));

            if (sender.hasPermission("essentialsc.seen.ip") && !onlineTarget.hasPermission("essentialsc.seen.exempt")) {
                Map<String, String> ipPlaceholders = new HashMap<>();
                ipPlaceholders.put("ip", onlineTarget.getAddress().getAddress().getHostAddress());
                sender.sendMessage(lang.get(sender, "seen.ip_info", ipPlaceholders));
            }

            return true;
        }

        if (!sender.hasPermission("essentialsc.seen.offline")) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", targetName);
            sender.sendMessage(lang.get(sender, "error.player_not_found", placeholders));
            return true;
        }

        OfflinePlayer offlineTarget = plugin.getBedrockUtil().resolveOfflinePlayer(targetName);

        if (offlineTarget == null || !offlineTarget.hasPlayedBefore()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", targetName);
            sender.sendMessage(lang.get(sender, "error.player_not_found", placeholders));
            return true;
        }

        long lastSeen = offlineTarget.getLastSeen();
        long firstPlayed = offlineTarget.getFirstPlayed();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", offlineTarget.getName());
        placeholders.put("last_seen", formatTimeAgo(lastSeen));
        placeholders.put("first_join", formatDate(firstPlayed));
        placeholders.put("playtime", formatPlayTime(offlineTarget.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) * 50L));

        sender.sendMessage(lang.get(sender, "seen.offline", placeholders));

        if (sender.hasPermission("essentialsc.seen.uuid")) {
            Map<String, String> uuidPlaceholders = new HashMap<>();
            uuidPlaceholders.put("uuid", offlineTarget.getUniqueId().toString());
            sender.sendMessage(lang.get(sender, "seen.uuid_info", uuidPlaceholders));
        }

        return true;
    }

    private String formatTimeAgo(long millis) {
        long diff = System.currentTimeMillis() - millis;
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;

        if (days > 0) return days + " day" + (days > 1 ? "s" : "") + " ago";
        if (hours > 0) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        if (minutes > 0) return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        return "Just now";
    }

    private String formatDate(long millis) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(millis));
    }

    private String formatPlayTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long days = hours / 24;
        hours = hours % 24;

        if (days > 0) return days + "d " + hours + "h";
        return hours + "h";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();

            for (Player p : plugin.getServer().getOnlinePlayers()) {
                names.add(p.getName());
            }

            if (sender.hasPermission("essentialsc.seen.offline")) {
                for (OfflinePlayer p : plugin.getServer().getOfflinePlayers()) {
                    if (p.getName() != null && !names.contains(p.getName())) {
                        names.add(p.getName());
                    }
                }
            }

            return names.stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
