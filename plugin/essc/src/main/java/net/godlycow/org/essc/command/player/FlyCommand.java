package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class FlyCommand extends Command {

    public FlyCommand(EssentialsC plugin) {
        super(plugin, "fly", "essentialsc.fly", true, 0, "command.usage.fly");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (plugin.getFlyManager() != null) {
            plugin.getFlyManager().setFlying(player, !player.getAllowFlight());
            if (player.getAllowFlight()) {
                player.sendMessage(lang.get(player, "fly.enabled"));
                plugin.debug("Enabled fly mode for " + player.getName());
            } else {
                player.sendMessage(lang.get(player, "fly.disabled"));
                plugin.debug("Disabled fly mode for " + player.getName());
            }
        } else {
            if (player.getAllowFlight()) {
                player.setAllowFlight(false);
                player.setFlying(false);
                player.sendMessage(lang.get(player, "fly.disabled"));
                plugin.debug("Disabled fly mode for " + player.getName());
            } else {
                player.setAllowFlight(true);
                player.sendMessage(lang.get(player, "fly.enabled"));
                plugin.debug("Enabled fly mode for " + player.getName());
            }
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}