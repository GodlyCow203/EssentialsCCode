package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class VanishCommand extends Command {

    public VanishCommand(EssentialsC plugin) {
        super(plugin, "vanish", "essentialsc.vanish", true, 0, "command.usage.vanish");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (plugin.getVanishManager().isVanished(player)) {
            plugin.getVanishManager().unvanish(player);
            player.sendMessage(lang.get(player, "vanish.disabled"));
        } else {
            plugin.getVanishManager().vanish(player);
            player.sendMessage(lang.get(player, "vanish.enabled"));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}