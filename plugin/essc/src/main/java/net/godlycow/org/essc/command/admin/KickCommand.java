package net.godlycow.org.essc.command.admin;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KickCommand extends Command {

    public KickCommand(EssentialsC plugin) {
        super(plugin, "kick", "essentialsc.kick", false, 1, "command.usage.kick");
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

        if (target.equals(sender)) {
            sender.sendMessage(lang.get(sender, "kick.cannot_kick_self"));
            return true;
        }

        if (sender instanceof Player playerSender && target.hasPermission("essentialsc.kick.exempt")) {
            if (!playerSender.hasPermission("essentialsc.kick.exempt.bypass")) {
                sender.sendMessage(lang.get(sender, "kick.exempt"));
                plugin.debug("Denied: " + target.getName() + " is exempt from being kicked");
                return true;
            }
        }

        String reason;
        if (args.length > 1) {
            StringBuilder reasonBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                reasonBuilder.append(args[i]).append(" ");
            }
            reason = reasonBuilder.toString().trim();
        } else {
            reason = "No reason provided";
        }

        plugin.debug("Kicking " + target.getName() + " by " + sender.getName() + " for: " + reason);

        Map<String, String> kickPlaceholders = new HashMap<>();
        kickPlaceholders.put("reason", reason);
        kickPlaceholders.put("kicker", sender.getName());

        Component kickMessage = lang.get(target, "kick.screen_message", kickPlaceholders);

        target.kick(kickMessage);

        Map<String, String> broadcastPlaceholders = new HashMap<>();
        broadcastPlaceholders.put("target", target.getName());
        broadcastPlaceholders.put("kicker", sender.getName());
        broadcastPlaceholders.put("reason", reason);

        plugin.getServer().broadcast(lang.get(sender, "kick.broadcast", broadcastPlaceholders), "essentialsc.kick.notify");

        Map<String, String> senderPlaceholders = new HashMap<>();
        senderPlaceholders.put("target", target.getName());
        sender.sendMessage(lang.get(sender, "kick.success", senderPlaceholders));

        if (plugin.getConfigManager().isDiscordSRVEnabled()) {
            if (plugin.getDiscordSRVHook() != null && plugin.getDiscordSRVHook().isHooked()) {
                plugin.getDiscordSRVHook().sendKickEmbed(
                        target.getUniqueId(),
                        target.getName(),
                        reason,
                        sender.getName()
                );
            } else {
                sender.sendMessage(lang.get(sender, "error.missing_plugin.discordsrv"));
            }
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .filter(p -> !p.equals(sender))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
