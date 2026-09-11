package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class BackCommand extends Command {

    public BackCommand(EssentialsC plugin) {
        super(plugin, "back", "essentialsc.back", true, 0);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        boolean confirm = args.length > 0 && args[0].equalsIgnoreCase("confirm");

        plugin.debug("Back command executed by " + player.getName() + (confirm ? " (confirm)" : ""));
        plugin.getBackManager().teleportBack(player, confirm);

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("confirm");
        }
        return Collections.emptyList();
    }
}