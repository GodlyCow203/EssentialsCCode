package net.godlycow.org.essc.command.tpa;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TPACancelCommand extends Command {

    public TPACancelCommand(EssentialsC plugin) {
        super(plugin, "tpcancel", "essentialsc.tpcancel", true, 0, "command.usage.tpcancel");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        var manager = plugin.getTPAManager();

        if (!manager.hasOutgoingRequests(player)) {
            sender.sendMessage(lang.get(sender, "tpa.error.no_outgoing"));
            return true;
        }

        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(lang.get(sender, "error.player_not_found", Map.of("player", args[0])));
                return true;
            }
        } else {
            var requests = manager.getOutgoingRequests(player);
            target = Bukkit.getPlayer(requests.get(requests.size() - 1).getTarget());
            if (target == null) {
                sender.sendMessage(lang.get(sender, "tpa.error.player_offline"));
                return true;
            }
        }

        if (manager.cancelRequest(player, target)) {
            return true;
        } else {
            sender.sendMessage(lang.get(sender, "tpa.error.no_request_to", Map.of("player", target.getName())));
            return true;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            return plugin.getTPAManager().getOutgoingRequests(player).stream()
                    .map(r -> Bukkit.getOfflinePlayer(r.getTarget()).getName())
                    .filter(name -> name != null)
                    .toList();
        }
        return Collections.emptyList();
    }
}