package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PlaytimeCommand extends Command {

    public PlaytimeCommand(EssentialsC plugin) {
        super(plugin, "playtime", "essentialsc.playtime", false, 0, "command.usage.playtime");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player target;

        if (args.length > 0) {
            target = plugin.getServer().getPlayer(args[0]);

            if (target == null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", args[0]);
                sender.sendMessage(lang.get(sender, "error.player_not_found", placeholders));
                return true;
            }

            if (target != sender && !sender.hasPermission("essentialsc.playtime.others")) {
                sender.sendMessage(lang.get(sender, "error.no_permission"));
                plugin.debug("Denied: " + sender.getName() + " lacks permission essentialsc.playtime.others");
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(lang.get(sender, "error.player_only"));
                return true;
            }
            target = (Player) sender;
        }

        long ticks = target.getStatistic(Statistic.PLAY_ONE_MINUTE);
        long milliseconds = ticks * 50;

        long days = TimeUnit.MILLISECONDS.toDays(milliseconds);
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60;

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        placeholders.put("days", String.valueOf(days));
        placeholders.put("hours", String.valueOf(hours));
        placeholders.put("minutes", String.valueOf(minutes));
        placeholders.put("seconds", String.valueOf(seconds));

        if (target == sender) {
            sender.sendMessage(lang.get(sender, "playtime.self", placeholders));
        } else {
            sender.sendMessage(lang.get(sender, "playtime.other", placeholders));
        }

        plugin.debug("Playtime checked: " + target.getName() + " has played for " +
                days + "d " + hours + "h " + minutes + "m " + seconds + "s");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("essentialsc.playtime.others")) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}