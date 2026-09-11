package net.godlycow.org.essc.command.admin;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.modules.punishment.PunishmentManager;
import net.godlycow.org.essc.util.DurationParser;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class CheckpunishCommand extends Command {

    private final PunishmentManager punishmentManager;

    public CheckpunishCommand(EssentialsC plugin, PunishmentManager punishmentManager) {
        super(plugin, "checkpunish", "essentialsc.checkpunish", false, 1, "command.usage.checkpunish");
        this.punishmentManager = punishmentManager;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String targetName = args[0];
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(lang.get(sender, "error.player_not_found", Map.of("player", targetName)));
            return true;
        }

        sender.sendMessage(lang.get(sender, "checkpunish.header", Map.of("player", target.getName())));

        if (punishmentManager.isBanned(target.getUniqueId())) {
            var entry = punishmentManager.getBanEntry(target.getUniqueId());
            sender.sendMessage(lang.get(sender, "checkpunish.ban.active", Map.of(
                    "reason", entry.reason(),
                    "banner", entry.banner(),
                    "time", DurationParser.formatAgo(entry.time()),
                    "expires", entry.expires() > 0 ? DurationParser.formatRemaining(entry.expires()) : "Permanent"
            )));
        } else {
            sender.sendMessage(lang.get(sender, "checkpunish.ban.none"));
        }

        if (punishmentManager.isMuted(target.getUniqueId())) {
            var entry = punishmentManager.getMuteEntry(target.getUniqueId());
            sender.sendMessage(lang.get(sender, "checkpunish.mute.active", Map.of(
                    "reason", entry.reason(),
                    "muter", entry.muter(),
                    "time", DurationParser.formatAgo(entry.time()),
                    "expires", entry.expires() > 0 ? DurationParser.formatRemaining(entry.expires()) : "Permanent"
            )));
        } else {
            sender.sendMessage(lang.get(sender, "checkpunish.mute.none"));
        }

        String lastIp = plugin.getUserManager() != null
                ? plugin.getUserManager().getLastIp(target.getUniqueId())
                : null;
        if (lastIp != null && punishmentManager.isIpBanned(lastIp)) {
            var entry = punishmentManager.getIpBanEntry(lastIp);
            sender.sendMessage(lang.get(sender, "checkpunish.ipban.active", Map.of(
                    "ip", lastIp,
                    "reason", entry.reason(),
                    "banner", entry.banner()
            )));
        } else {
            sender.sendMessage(lang.get(sender, "checkpunish.ipban.none"));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return super.tabComplete(sender, args);
    }
}