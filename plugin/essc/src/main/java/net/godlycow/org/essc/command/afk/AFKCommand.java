package net.godlycow.org.essc.command.afk;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class AFKCommand extends Command {

    public AFKCommand(EssentialsC plugin) {
        super(plugin, "afk", "essentialsc.afk", true, 0, "command.usage.afk");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (!plugin.getConfigManager().isAfkEnabled()) {
            player.sendMessage(lang.get(player, "afk.error.disabled"));
            return true;
        }

        plugin.getAfkManager().toggleAFK(player);

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}