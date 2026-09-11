package net.godlycow.org.essc.command.admin;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SudoCommand extends Command {

    public SudoCommand(EssentialsC plugin) {
        super(plugin, "sudo", "essentialsc.sudo", false, 2, "command.usage.sudo");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", args[0]);
            sender.sendMessage(lang.get(sender, "error.player_not_found", placeholders));
            plugin.debug("Sudo failed: Target '" + args[0] + "' not found");
            return true;
        }

        if (target.hasPermission("essentialsc.sudo.exempt") && !sender.hasPermission("essentialsc.sudo.override")) {
            sender.sendMessage(lang.get(sender, "sudo.exempt"));
            plugin.debug("Sudo denied: " + target.getName() + " is exempt");
            return true;
        }

        String commandLine = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        boolean isChat = !commandLine.startsWith("/");

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        placeholders.put("command", commandLine);

        if (isChat) {
            target.chat(commandLine);
            sender.sendMessage(lang.get(sender, "sudo.success.chat", placeholders));
            plugin.debug(sender.getName() + " forced " + target.getName() + " to chat: " + commandLine);
        } else {
            String cmd = commandLine.substring(1);
            target.performCommand(cmd);
            sender.sendMessage(lang.get(sender, "sudo.success.command", placeholders));
            plugin.debug(sender.getName() + " forced " + target.getName() + " to run: " + cmd);
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return List.of("/", "say", "help", "me");
        }
        return Collections.emptyList();
    }
}