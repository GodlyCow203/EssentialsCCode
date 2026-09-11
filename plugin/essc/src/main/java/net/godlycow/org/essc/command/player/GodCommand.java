package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GodCommand extends Command {

    public GodCommand(EssentialsC plugin) {
        super(plugin, "god", "essentialsc.god", true, 0, "command.usage.god");
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

            if (target != player && !player.hasPermission("essentialsc.god.others")) {
                player.sendMessage(lang.get(player, "error.no_permission"));
                plugin.debug("Denied: " + player.getName() + " lacks permission essentialsc.god.others");
                return true;
            }
        } else {
            target = player;
        }

        boolean godMode = !target.isInvulnerable();
        target.setInvulnerable(godMode);

        plugin.debug("God mode " + (godMode ? "enabled" : "disabled") + " for " + target.getName() + " by " + player.getName());

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("target", target.getName());
        placeholders.put("status", godMode ? "enabled" : "disabled");

        if (target == player) {
            player.sendMessage(lang.get(player, godMode ? "god.enabled" : "god.disabled"));
        } else {
            player.sendMessage(lang.get(player, godMode ? "god.enabled.other" : "god.disabled.other", placeholders));

            Map<String, String> targetPlaceholders = new HashMap<>();
            targetPlaceholders.put("player", player.getName());
            target.sendMessage(lang.get(target, godMode ? "god.enabled.by" : "god.disabled.by", targetPlaceholders));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("essentialsc.god.others")) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}