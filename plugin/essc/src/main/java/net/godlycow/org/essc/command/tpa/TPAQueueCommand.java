package net.godlycow.org.essc.command.tpa;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.modules.teleport.TPARequest;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TPAQueueCommand extends Command {

    public TPAQueueCommand(EssentialsC plugin) {
        super(plugin, "tpaqueue", "essentialsc.tpaqueue", true, 0);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        var manager = plugin.getTPAManager();

        var incoming = manager.getIncomingRequests(player);
        var outgoing = manager.getOutgoingRequests(player);

        if (incoming.isEmpty() && outgoing.isEmpty()) {
            sender.sendMessage(lang.get(sender, "tpa.queue.empty"));
            return true;
        }

        if (!incoming.isEmpty()) {
            sender.sendMessage(lang.get(sender, "tpa.queue.header_incoming"));
            for (TPARequest req : incoming) {
                String name = Bukkit.getOfflinePlayer(req.getRequester()).getName();
                String type = req.getType() == TPARequest.Type.TPA ? "→" : "←";
                long remaining = (req.getTimestamp() + (plugin.getConfigManager().getTPATimeout() * 1000) - System.currentTimeMillis()) / 1000;

                sender.sendMessage(lang.get(sender, "tpa.queue.entry",
                        Map.of(
                                "player", name != null ? name : "Unknown",
                                "type", type,
                                "seconds", String.valueOf(Math.max(0, remaining))
                        )));
            }
        }

        if (!outgoing.isEmpty()) {
            sender.sendMessage(lang.get(sender, "tpa.queue.header_outgoing"));
            for (TPARequest req : outgoing) {
                String name = Bukkit.getOfflinePlayer(req.getTarget()).getName();
                String type = req.getType() == TPARequest.Type.TPA ? "→" : "←";
                long remaining = (req.getTimestamp() + (plugin.getConfigManager().getTPATimeout() * 1000) - System.currentTimeMillis()) / 1000;

                sender.sendMessage(lang.get(sender, "tpa.queue.entry",
                        Map.of(
                                "player", name != null ? name : "Unknown",
                                "type", type,
                                "seconds", String.valueOf(Math.max(0, remaining))
                        )));
            }
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}