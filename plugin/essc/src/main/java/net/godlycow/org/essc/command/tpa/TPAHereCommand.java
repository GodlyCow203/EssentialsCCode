package net.godlycow.org.essc.command.tpa;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.modules.teleport.TPARequest;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TPAHereCommand extends Command {

    public TPAHereCommand(EssentialsC plugin) {
        super(plugin, "tpahere", "essentialsc.tpahere", true, 1, "command.usage.tpahere");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            sender.sendMessage(lang.get(sender, "error.player_not_found", Map.of("player", args[0])));
            return true;
        }

        if (target.equals(player)) {
            sender.sendMessage(lang.get(sender, "tpa.error.self"));
            return true;
        }

        plugin.getTPAManager().requestTeleport(player, target, TPARequest.Type.TPAHERE);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return null;
        }
        return Collections.emptyList();
    }
}