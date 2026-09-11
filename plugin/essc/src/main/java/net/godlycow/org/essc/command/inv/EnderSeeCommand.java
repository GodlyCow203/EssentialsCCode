package net.godlycow.org.essc.command.inv;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnderSeeCommand extends Command {

    public EnderSeeCommand(EssentialsC plugin) {
        super(plugin, "endersee", "essentialsc.endersee", true, 1, "command.usage.endersee");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", args[0]);
            player.sendMessage(lang.get(player, "error.player_not_found", placeholders));
            return true;
        }

        if (target == player) {
            player.sendMessage(lang.get(player, "endersee.self"));
            return true;
        }

        player.openInventory(target.getEnderChest());

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("target", target.getName());
        player.sendMessage(lang.get(player, "endersee.opened", placeholders));

        plugin.debug(player.getName() + " is viewing " + target.getName() + "'s enderchest via /endersee");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> !name.equals(sender.getName()))
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}