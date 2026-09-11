package net.godlycow.org.essc.command.inv;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class StonecutterCommand extends Command {

    public StonecutterCommand(EssentialsC plugin) {
        super(plugin, "stonecutter", "essentialsc.stonecutter", true, 0, "command.usage.stonecutter");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        player.openStonecutter(null, true);
        player.sendMessage(lang.get(player, "stonecutter.opened"));

        plugin.debug(player.getName() + " opened stonecutter");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}