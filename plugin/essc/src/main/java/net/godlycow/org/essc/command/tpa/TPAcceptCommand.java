package net.godlycow.org.essc.command.tpa;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TPAcceptCommand extends Command {

    public TPAcceptCommand(EssentialsC plugin) {
        super(plugin, "tpaccept", "essentialsc.tpaccept", true, 0, "command.usage.tpaccept");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        var manager = plugin.getTPAManager();

        if (!manager.hasIncomingRequests(player)) {
            sender.sendMessage(lang.get(sender, "tpa.error.no_requests"));
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
            var requests = manager.getIncomingRequests(player);
            target = Bukkit.getPlayer(requests.get(requests.size() - 1).getRequester());
            if (target == null) {
                sender.sendMessage(lang.get(sender, "tpa.error.player_offline"));
                return true;
            }
        }

        if (manager.acceptRequest(player, target)) {
            return true;
        } else {
            sender.sendMessage(lang.get(sender, "tpa.error.no_request_from", Map.of("player", target.getName())));
            return true;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            return plugin.getTPAManager().getIncomingRequests(player).stream()
                    .map(r -> Bukkit.getOfflinePlayer(r.getRequester()).getName())
                    .filter(name -> name != null)
                    .toList();
        }
        return Collections.emptyList();
    }
}