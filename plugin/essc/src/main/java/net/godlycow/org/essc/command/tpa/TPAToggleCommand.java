package net.godlycow.org.essc.command.tpa;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class TPAToggleCommand extends Command {

    public TPAToggleCommand(EssentialsC plugin) {
        super(plugin, "tpatoggle", "essentialsc.tpatoggle", true, 0);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        plugin.getTPAManager().toggleTPA(player);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}