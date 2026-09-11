package net.godlycow.org.essc.command.admin;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.modules.punishment.PunishmentManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UnbanCommand extends Command {

    private final PunishmentManager punishmentManager;

    public UnbanCommand(EssentialsC plugin, PunishmentManager punishmentManager) {
        super(plugin, "unban", "essentialsc.unban", false, 1, "command.usage.unban");
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

        if (!punishmentManager.isBanned(target.getUniqueId())) {
            sender.sendMessage(lang.get(sender, "unban.not_banned", Map.of("target", target.getName())));
            return true;
        }

        punishmentManager.unbanPlayer(target.getUniqueId());

        plugin.getServer().broadcast(lang.get(sender, "unban.broadcast", Map.of(
                "target", target.getName(),
                "unbanner", sender.getName()
        )), "essentialsc.ban.notify");

        sender.sendMessage(lang.get(sender, "unban.success", Map.of("target", target.getName())));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return punishmentManager.getActiveBans().stream()
                    .map(PunishmentManager.BanEntry::name)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}