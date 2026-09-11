package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class IgnoreCommand extends Command {

    public IgnoreCommand(EssentialsC plugin) {
        super(plugin, "ignore", "essentialsc.ignore", true, 0, "command.usage.ignore");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        UUID playerUuid = player.getUniqueId();

        if (args.length == 0) {
            plugin.getUserManager().getIgnoredPlayersWithNames(playerUuid).thenAccept(ignored -> {
                if (ignored.isEmpty()) {
                    player.sendMessage(lang.get(player, "ignore.list_empty"));
                    return;
                }

                player.sendMessage(lang.get(player, "ignore.list_header"));
                for (Map.Entry<UUID, String> entry : ignored.entrySet()) {
                    String name = entry.getValue();
                    Player online = plugin.getServer().getPlayer(entry.getKey());
                    if (online != null) name = online.getName();

                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("player", name);
                    player.sendMessage(lang.get(player, "ignore.list_entry", placeholders));
                }
            });
            return true;
        }

        String targetName = args[0];
        Player target = plugin.getServer().getPlayer(targetName);

        if (target == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", targetName);
            player.sendMessage(lang.get(player, "error.player_not_found", placeholders));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(lang.get(player, "ignore.cannot_ignore_self"));
            return true;
        }

        if (target.hasPermission("essentialsc.ignore.exempt")) {
            player.sendMessage(lang.get(player, "ignore.exempt"));
            plugin.debug("Denied: " + player.getName() + " cannot ignore " + target.getName());
            return true;
        }

        UUID targetUuid = target.getUniqueId();
        plugin.getUserManager().getIgnoredPlayers(playerUuid).thenAccept(ignored -> {
            boolean isIgnoring = ignored.contains(targetUuid);

            if (isIgnoring) {
                plugin.getUserManager().removeIgnoredPlayer(playerUuid, targetUuid)
                        .thenRun(() -> {
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("player", target.getName());
                            player.sendMessage(lang.get(player, "ignore.unignored", placeholders));
                            plugin.debug(player.getName() + " unignored " + target.getName());
                        });
            } else {
                plugin.getUserManager().addIgnoredPlayer(playerUuid, targetUuid, target.getName())
                        .thenRun(() -> {
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("player", target.getName());
                            player.sendMessage(lang.get(player, "ignore.ignored", placeholders));
                            plugin.debug(player.getName() + " ignored " + target.getName());
                        });
            }
        });

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            return plugin.getServer().getOnlinePlayers().stream().filter(p -> !p.equals(sender)).map(Player::getName).filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase())).toList();
        }
        return Collections.emptyList();
    }
}