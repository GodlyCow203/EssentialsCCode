package net.godlycow.org.essc.command.inv;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class AnvilCommand extends Command {

    public AnvilCommand(EssentialsC plugin) {
        super(plugin, "anvil", "essentialsc.anvil", true, 0, "command.usage.anvil");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        player.openAnvil(null, true);
        player.sendMessage(lang.get(player, "anvil.opened"));

        plugin.debug(player.getName() + " opened anvil");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}