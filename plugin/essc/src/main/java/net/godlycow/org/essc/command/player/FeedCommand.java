package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedCommand extends Command {

    public FeedCommand(EssentialsC plugin) {
        super(plugin, "feed", "essentialsc.feed", true, 0, "command.usage.feed");
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

            if (target != player && !player.hasPermission("essentialsc.feed.others")) {
                player.sendMessage(lang.get(player, "error.no_permission"));
                plugin.debug("Denied: " + player.getName() + " lacks permission essentialsc.feed.others");
                return true;
            }
        } else {
            target = player;
        }

        plugin.debug("Feeding initiated for " + target.getName() + " by " + player.getName());

        target.setFoodLevel(20);
        target.setSaturation(20);

        plugin.debug("Fed " + target.getName() + " to full food and saturation");

        if (target == player) {
            player.sendMessage(lang.get(player, "feed.success"));
        } else {
            Map<String, String> senderPlaceholders = new HashMap<>();
            senderPlaceholders.put("target", target.getName());
            player.sendMessage(lang.get(player, "feed.success.other", senderPlaceholders));

            Map<String, String> targetPlaceholders = new HashMap<>();
            targetPlaceholders.put("feeder", player.getName());
            target.sendMessage(lang.get(target, "feed.success.by", targetPlaceholders));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("essentialsc.feed.others")) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}