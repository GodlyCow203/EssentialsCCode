package net.godlycow.org.essc.command.admin;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmiteCommand extends Command {

    public SmiteCommand(EssentialsC plugin) {
        super(plugin, "smite", "essentialsc.smite", false, 1, "command.usage.smite");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player target = plugin.getBedrockUtil().resolvePlayer(args[0]);

        if (target == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", args[0]);
            sender.sendMessage(lang.get(sender, "error.player_not_found", placeholders));
            return true;
        }

        if (sender instanceof Player playerSender && target.equals(playerSender)) {
            sender.sendMessage(lang.get(sender, "smite.cannot_smite_self"));
            return true;
        }

        if (sender instanceof Player playerSender && target.hasPermission("essentialsc.smite.exempt")) {
            if (!playerSender.hasPermission("essentialsc.smite.exempt.bypass")) {
                sender.sendMessage(lang.get(sender, "smite.exempt"));
                plugin.debug("Denied: " + target.getName() + " is exempt from /smite");
                return true;
            }
        }

        target.getWorld().strikeLightning(target.getLocation());

        Map<String, String> senderPlaceholders = new HashMap<>();
        senderPlaceholders.put("target", target.getName());
        sender.sendMessage(lang.get(sender, "smite.success", senderPlaceholders));

        plugin.debug("Smite executed on " + target.getName() + " by " + sender.getName());

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream().filter(p -> !p.equals(sender)).map(Player::getName).filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }
}