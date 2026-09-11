package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class SuicideCommand extends Command {

    public SuicideCommand(EssentialsC plugin) {
        super(plugin, "suicide", "essentialsc.suicide", true, 0, "command.usage.suicide");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        plugin.debug("Suicide triggered by " + player.getName());

        player.setHealth(0.0);

        player.sendMessage(lang.get(player, "suicide.success"));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}