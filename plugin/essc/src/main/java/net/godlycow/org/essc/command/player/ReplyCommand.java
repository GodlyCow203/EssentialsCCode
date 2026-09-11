package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class ReplyCommand extends Command {

    public ReplyCommand(EssentialsC plugin) {
        super(plugin, "reply", "essentialsc.reply", false, 1, "command.usage.reply");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player playerSender)) {
            sender.sendMessage(lang.get(sender, "reply.console_no_reply"));
            return true;
        }

        UUID replyTarget = plugin.getReplyManager().getReplyTarget(playerSender.getUniqueId());

        if (replyTarget == null) {
            sender.sendMessage(lang.get(sender, "reply.no_reply_target"));
            return true;
        }

        Player target = plugin.getServer().getPlayer(replyTarget);

        if (target == null) {
            sender.sendMessage(lang.get(sender, "reply.target_offline"));
            plugin.getReplyManager().removeReplyTarget(playerSender.getUniqueId());
            return true;
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (String arg : args) {
            messageBuilder.append(arg).append(" ");
        }
        String message = messageBuilder.toString().trim();

        if (message.isEmpty()) {
            sendUsage(sender);
            return true;
        }

        plugin.getUserManager().getIgnoredPlayers(target.getUniqueId())
                .thenCompose(targetIgnored -> {
                    if (targetIgnored.contains(playerSender.getUniqueId())) {
                        plugin.getServer().getGlobalRegionScheduler().run(plugin, task ->
                                sender.sendMessage(lang.get(sender, "msg.ignored_by_target"))
                        );
                        return null;
                    }
                    return plugin.getUserManager().getIgnoredPlayers(playerSender.getUniqueId());
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
                            deliverReply(playerSender, target, message)
                    );
                })
                .exceptionally(ex -> {
                    plugin.debug("Failed to check ignore status for reply: " + ex.getMessage());
                    plugin.getServer().getGlobalRegionScheduler().run(plugin, task ->
                            deliverReply(playerSender, target, message)
                    );
                    return null;
                });

        return true;
    }

    private void deliverReply(Player playerSender, Player target, String message) {
        plugin.getReplyManager().setReplyTarget(target.getUniqueId(), playerSender.getUniqueId());
        plugin.getReplyManager().setReplyTarget(playerSender.getUniqueId(), target.getUniqueId());

        plugin.debug("Reply from " + playerSender.getName() + " to " + target.getName() + ": " + message);

        Map<String, String> senderPlaceholders = new HashMap<>();
        senderPlaceholders.put("target", target.getName());
        senderPlaceholders.put("message", message);
        playerSender.sendMessage(lang.get(playerSender, "msg.outgoing", senderPlaceholders));

        Map<String, String> targetPlaceholders = new HashMap<>();
        targetPlaceholders.put("sender", playerSender.getName());
        targetPlaceholders.put("message", message);
        target.sendMessage(lang.get(target, "msg.incoming", targetPlaceholders));

        Map<String, String> spyPlaceholders = new HashMap<>();
        spyPlaceholders.put("sender", playerSender.getName());
        spyPlaceholders.put("target", target.getName());
        spyPlaceholders.put("message", message);

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.hasPermission("essentialsc.msg.spy") && !online.equals(playerSender) && !online.equals(target)) {
                online.sendMessage(lang.get(online, "msg.spy", spyPlaceholders));
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}