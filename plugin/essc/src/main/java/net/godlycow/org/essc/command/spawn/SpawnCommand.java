package net.godlycow.org.essc.command.spawn;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SpawnCommand extends Command {

    public SpawnCommand(EssentialsC plugin) {
        super(plugin, "spawn", "essentialsc.spawn", true, 0);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length > 0 && player.hasPermission("essentialsc.spawn.admin")) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(lang.get(player, "error.player_not_found"));
                return true;
            }

            plugin.debug("Admin " + player.getName() + " teleporting " + target.getName() + " to spawn");
            plugin.getSpawnManager().teleportToSpawn(target, true);
            player.sendMessage(lang.get(player, "spawn.admin.teleported_other",
                    java.util.Map.of("player", target.getName())));
            return true;
        }

        plugin.debug("Spawn command by " + player.getName());
        plugin.getSpawnManager().teleportToSpawn(player);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("essentialsc.spawn.admin")) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return super.tabComplete(sender, args);
    }
}