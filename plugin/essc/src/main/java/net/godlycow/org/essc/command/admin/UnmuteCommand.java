package net.godlycow.org.essc.command.admin;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.modules.punishment.PunishmentManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UnmuteCommand extends Command {

    private final PunishmentManager punishmentManager;

    public UnmuteCommand(EssentialsC plugin, PunishmentManager punishmentManager) {
        super(plugin, "unmute", "essentialsc.unmute", false, 1, "command.usage.unmute");
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

        if (!punishmentManager.isMuted(target.getUniqueId())) {
            sender.sendMessage(lang.get(sender, "unmute.not_muted", Map.of("target", target.getName())));
            return true;
        }

        punishmentManager.unmutePlayer(target.getUniqueId());

        plugin.getServer().broadcast(lang.get(sender, "unmute.broadcast", Map.of(
                "target", target.getName(),
                "unmuter", sender.getName()
        )), "essentialsc.mute.notify");

        sender.sendMessage(lang.get(sender, "unmute.success", Map.of("target", target.getName())));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return punishmentManager.getAllMutes().stream()
                    .map(PunishmentManager.MuteEntry::name)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}