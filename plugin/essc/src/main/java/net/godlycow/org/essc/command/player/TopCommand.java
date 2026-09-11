package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopCommand extends Command {

    public TopCommand(EssentialsC plugin) {
        super(plugin, "top", "essentialsc.top", true, 0, null);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        Location current = player.getLocation();
        World world = current.getWorld();

        if (world == null) {
            player.sendMessage(lang.get(player, "top.error"));
            return true;
        }

        int x = current.getBlockX();
        int z = current.getBlockZ();
        int maxY = world.getMaxHeight();
        int minY = world.getMinHeight();

        for (int y = maxY - 1; y >= minY; y--) {
            Location check = new Location(world, x + 0.5, y, z + 0.5);
            Location above = check.clone().add(0, 1, 0);
            Location below = check.clone().subtract(0, 1, 0);

            if (isSafe(check) && isSafe(above) && isSolid(below)) {
                Location teleport = check.clone();
                teleport.setYaw(current.getYaw());
                teleport.setPitch(current.getPitch());

                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("y", String.valueOf(y));

                plugin.teleportHelper().teleportAsync(player, teleport).thenAccept(success -> {
                    if (!success) return;
                    player.sendMessage(lang.get(player, "top.success", placeholders));
                    plugin.debug("Teleported " + player.getName() + " to top at Y=" + teleport.getBlockY());
                });
                return true;
            }
        }

        player.sendMessage(lang.get(player, "top.no_safe_location"));
        return true;
    }

    private boolean isSafe(Location loc) {
        return loc.getBlock().isPassable() && !loc.getBlock().isLiquid();
    }

    private boolean isSolid(Location loc) {
        return loc.getBlock().getType().isSolid();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}