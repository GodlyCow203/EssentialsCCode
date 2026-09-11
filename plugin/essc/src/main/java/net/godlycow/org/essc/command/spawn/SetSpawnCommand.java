package net.godlycow.org.essc.command.spawn;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SetSpawnCommand extends Command {

    public SetSpawnCommand(EssentialsC plugin) {
        super(plugin, "setspawn", "essentialsc.setspawn", true, 0);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        plugin.debug("Setspawn command executed by " + player.getName());

        plugin.getSpawnManager().setSpawn(player.getLocation());
        player.sendMessage(lang.get(player, "spawn.set.success"));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return super.tabComplete(sender, args);
    }
}