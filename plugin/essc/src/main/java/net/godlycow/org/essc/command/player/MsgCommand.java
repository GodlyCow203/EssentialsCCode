package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class MsgCommand extends Command {

    public MsgCommand(EssentialsC plugin) {
        super(plugin, "msg", "essentialsc.msg", false, 2, "command.usage.msg");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String targetName = args[0];
        Player target = plugin.getServer().getPlayer(targetName);

        if (target == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", targetName);
            sender.sendMessage(lang.get(sender, "error.player_not_found", placeholders));
            return true;
        }

        if (target.equals(sender)) {
            sender.sendMessage(lang.get(sender, "msg.cannot_message_self"));
            return true;
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            messageBuilder.append(args[i]).append(" ");
        }
        String message = messageBuilder.toString().trim();

        if (message.isEmpty()) {
            sendUsage(sender);
            return true;
        }

        Player senderPlayer = sender instanceof Player ? (Player) sender : null;

        if (senderPlayer == null) {
            deliverMessage(sender, null, target, message);
            return true;
        }

        Player finalSenderPlayer = senderPlayer;
        plugin.getUserManager().getIgnoredPlayers(target.getUniqueId())
                .thenCompose(targetIgnored -> {
                    if (targetIgnored.contains(finalSenderPlayer.getUniqueId())) {
                        plugin.getServer().getGlobalRegionScheduler().run(plugin, task ->
                                sender.sendMessage(lang.get(sender, "msg.ignored_by_target"))
                        );
                        return null;
                    }
                    return plugin.getUserManager().getIgnoredPlayers(finalSenderPlayer.getUniqueId());
                })
                .thenAccept(senderIgnored -> {
                    if (senderIgnored == null) {
                        return;
                    }
                    if (senderIgnored.contains(target.getUniqueId())) {
                        plugin.getServer().getGlobalRegionScheduler().run(plugin, task ->
                                sender.sendMessage(lang.get(sender, "msg.ignoring_target"))
                        );
                        return;
                    }
                    plugin.getServer().getGlobalRegionScheduler().run(plugin, task ->
                            deliverMessage(sender, finalSenderPlayer, target, message)
                    );
                })
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Failed to check ignore status for msg: " + ex.getMessage());
                    plugin.getServer().getGlobalRegionScheduler().run(plugin, task ->
                            deliverMessage(sender, finalSenderPlayer, target, message)
                    );
                    return null;
                });

        return true;
    }

    private void deliverMessage(CommandSender sender, Player senderPlayer, Player target, String message) {
        if (senderPlayer != null) {
            plugin.getReplyManager().setReplyTarget(senderPlayer.getUniqueId(), target.getUniqueId());
            plugin.getReplyManager().setReplyTarget(target.getUniqueId(), senderPlayer.getUniqueId());
        }

        plugin.debug("Message from " + sender.getName() + " to " + target.getName() + ": " + message);

        Map<String, String> senderPlaceholders = new HashMap<>();
        senderPlaceholders.put("target", target.getName());
        senderPlaceholders.put("message", message);
        sender.sendMessage(lang.get(sender, "msg.outgoing", senderPlaceholders));

        Map<String, String> targetPlaceholders = new HashMap<>();
        targetPlaceholders.put("sender", sender.getName());
        targetPlaceholders.put("message", message);
        target.sendMessage(lang.get(target, "msg.incoming", targetPlaceholders));

        Map<String, String> spyPlaceholders = new HashMap<>();
        spyPlaceholders.put("sender", sender.getName());
        spyPlaceholders.put("target", target.getName());
        spyPlaceholders.put("message", message);

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.hasPermission("essentialsc.msg.spy") && !online.equals(sender) && !online.equals(target)) {
                online.sendMessage(lang.get(online, "msg.spy", spyPlaceholders));
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return plugin.getServer().getOnlinePlayers().stream()
                    .filter(p -> !p.equals(sender))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .toList();
        }
        return Collections.emptyList();
    }
}