package net.godlycow.org.essc.command.tpa;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TPHereCommand extends Command {

    public TPHereCommand(EssentialsC plugin) {
        super(plugin, "tphere", "essentialsc.tphere", true, 1, "command.usage.tphere");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", args[0]);
            player.sendMessage(lang.get(player, "error.player_not_found", placeholders));
            return true;
        }

        if (target == player) {
            player.sendMessage(lang.get(player, "error.cannot_teleport_self"));
            return true;
        }

        if (!target.isOnline()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", target.getName());
            player.sendMessage(lang.get(player, "error.player_not_online", placeholders));
            return true;
        }

        if (target.hasPermission("essentialsc.tphere.bypass") && !player.hasPermission("essentialsc.tphere.bypass.override")) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", target.getName());
            player.sendMessage(lang.get(player, "tphere.bypass", placeholders));
            return true;
        }

        plugin.getBackManager().setBackLocation(target, target.getLocation());

        Location dest = player.getLocation();
        plugin.teleportHelper().teleportAsync(target, dest).thenAccept(success -> {
            if (!success) return;

            Map<String, String> senderPlaceholders = new HashMap<>();
            senderPlaceholders.put("target", target.getName());
            player.sendMessage(lang.get(player, "tphere.success", senderPlaceholders));

            Map<String, String> targetPlaceholders = new HashMap<>();
            targetPlaceholders.put("player", player.getName());
            target.sendMessage(lang.get(target, "tphere.teleported", targetPlaceholders));
        });

        plugin.debug("TPHere: " + player.getName() + " teleported " + target.getName() + " to their location");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .filter(name -> !name.equalsIgnoreCase(sender.getName()))
                    .toList();
        }
        return Collections.emptyList();
    }
}