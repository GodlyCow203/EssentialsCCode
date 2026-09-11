package net.godlycow.org.essc.command.admin;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.modules.punishment.PunishmentManager;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UnbanIpCommand extends Command {

    private final PunishmentManager punishmentManager;

    public UnbanIpCommand(EssentialsC plugin, PunishmentManager punishmentManager) {
        super(plugin, "unban-ip", "essentialsc.unbanip", false, 1, "command.usage.unbanip");
        this.punishmentManager = punishmentManager;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String ip = args[0];

        if (!punishmentManager.isIpBanned(ip)) {
            sender.sendMessage(lang.get(sender, "unbanip.not_banned", Map.of("ip", ip)));
            return true;
        }

        punishmentManager.unbanIp(ip);

        plugin.getServer().broadcast(lang.get(sender, "unbanip.broadcast", Map.of("ip", ip, "unbanner", sender.getName())), "essentialsc.banip.notify");//broadcast

        sender.sendMessage(lang.get(sender, "unbanip.success", Map.of("ip", ip)));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return punishmentManager.getActiveIpBans().stream()
                    .map(PunishmentManager.IpBanEntry::ip)
                    .filter(ip -> ip.startsWith(args[0]))
                    .collect(Collectors.toList());
        }

        return super.tabComplete(sender, args);
    }
}