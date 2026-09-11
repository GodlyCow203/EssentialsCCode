package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PingCommand extends Command {

    public PingCommand(EssentialsC plugin) {
        super(plugin, "ping", "essentialsc.ping", true, 0, "command.usage.ping");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        Player target;

        if (args.length > 0) {
            target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", args[0]);
                player.sendMessage(lang.get(player, "error.player_not_found", placeholders));
                return true;
            }

            if (target != player && !player.hasPermission("essentialsc.ping.others")) {
                player.sendMessage(lang.get(player, "error.no_permission"));
                plugin.debug("Denied: " + player.getName() + " lacks permission essentialsc.ping.others");
                return true;
            }
        } else {
            target = player;
        }

        int ping = target.getPing();
        plugin.debug("Ping check for " + target.getName() + ": " + ping + "ms by " + player.getName());

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("ping", String.valueOf(ping));
        placeholders.put("target", target.getName());

        if (target == player) {
            player.sendMessage(lang.get(player, "ping.self", placeholders));
        } else {
            player.sendMessage(lang.get(player, "ping.other", placeholders));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("essentialsc.ping.others")) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}