package net.godlycow.org.essc.command.server;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BroadcastCommand extends Command {

    public BroadcastCommand(EssentialsC plugin) {
        super(plugin, "broadcast", "essentialsc.broadcast", false, 1, "command.usage.broadcast");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        World targetWorld = Bukkit.getWorld(args[0]);
        String message;
        boolean worldSpecific = false;

        if (targetWorld != null && args.length > 1) {
            worldSpecific = true;
            message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("world", targetWorld.getName());
            sender.sendMessage(lang.get(sender, "broadcast.world.selected", placeholders));
        } else {
            message = String.join(" ", args);
        }

        String prefix = plugin.getConfigManager().getBroadcastPrefix();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("message", prefix + message);

        if (worldSpecific) {
            for (Player player : targetWorld.getPlayers()) {
                player.sendMessage(lang.get(player, "broadcast.format", placeholders));
            }
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(lang.get(player, "broadcast.format", placeholders));
            }
        }

        sender.sendMessage(lang.get(sender, "broadcast.success"));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Bukkit.getWorlds().stream()
                    .map(World::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}