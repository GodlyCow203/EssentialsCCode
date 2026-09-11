package net.godlycow.org.essc.command.home;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.modules.home.Home;
import net.godlycow.org.essc.util.RespawnUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomesCommand extends Command {

    public HomesCommand(EssentialsC plugin) {
        super(plugin, "homes", "essentialsc.homes", true, 0);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("notifications")) {
            handleNotifications(player);
            return true;
        }

        if (args.length > 0 && player.hasPermission("essentialsc.home.admin")) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                player.sendMessage(lang.get(player, "error.player_not_found"));
                return true;
            }

            plugin.debug("Listing homes for " + target.getName() + " (requested by " + player.getName() + ")");

            plugin.getHomeManager().getHomes(target.getUniqueId()).thenAccept(homes -> {
                player.getScheduler().run(plugin, task -> {
                    sendHomeList(player, homes, target.getName());
                }, null);
            });
            return true;
        }

        plugin.debug("Listing homes for " + player.getName());

        plugin.getHomeManager().getHomes(player.getUniqueId()).thenAccept(homes -> {
            player.getScheduler().run(plugin, task -> {
                Location bedLoc = player.hasPermission("essentialsc.home.bed")
                        ? RespawnUtil.getBedSpawnLocation(player)
                        : null;
                boolean hasBed = bedLoc != null;

                if (homes.isEmpty() && !hasBed) {
                    player.sendMessage(lang.get(player, "home.list.empty"));
                    return;
                }

                int max = plugin.getHomeManager().getMaxHomes(player);
                int usedCount = homes.size() + (hasBed && plugin.getConfigManager().isBedHomeCountsInLimit() ? 1 : 0);
                String limit = max == Integer.MAX_VALUE ? "∞" : String.valueOf(max);

                player.sendMessage(lang.get(player, "homes.list.header",
                        Map.of("used", String.valueOf(usedCount), "limit", limit)));
                player.sendMessage(lang.get(player, "homes.list.separator"));

                if (hasBed) {
                    player.sendMessage(lang.get(player, "homes.list.entry",
                            Map.of("name", "bed",
                                    "world", bedLoc.getWorld() != null ? bedLoc.getWorld().getName() : "?",
                                    "x", String.valueOf((int) bedLoc.getX()),
                                    "y", String.valueOf((int) bedLoc.getY()),
                                    "z", String.valueOf((int) bedLoc.getZ()))));
                }

                for (Home home : homes) {
                    player.sendMessage(lang.get(player, "homes.list.entry",
                            Map.of("name", home.getName(),
                                    "world", home.getWorld(),
                                    "x", String.valueOf((int) home.getX()),
                                    "y", String.valueOf((int) home.getY()),
                                    "z", String.valueOf((int) home.getZ()))));
                }

                player.sendMessage(lang.get(player, "homes.list.footer",
                        Map.of("count", String.valueOf(homes.size()))));
            }, null);
        });

        return true;
    }

    private void handleNotifications(Player player) {
        if (!player.hasPermission("essentialsc.home.notifications")) {
            player.sendMessage(lang.get(player, "error.no_permission"));
            return;
        }

        boolean current = plugin.getHomeNotificationManager().isNotificationsEnabled(player.getUniqueId());
        boolean newValue = !current;
        plugin.getHomeNotificationManager().setNotificationsEnabled(player.getUniqueId(), newValue);
        player.sendMessage(lang.get(player, newValue
                ? "home.notifications.enabled"
                : "home.notifications.disabled"));
    }

    private void sendHomeList(Player player, List<Home> homes, String targetName) {
        player.sendMessage(lang.get(player, "home.list.header_other", Map.of("player", targetName)));

        if (homes.isEmpty()) {
            player.sendMessage(lang.get(player, "home.list.empty_other", Map.of("player", targetName)));
            return;
        }

        for (Home home : homes) {
            player.sendMessage(lang.get(player, "home.list.entry",
                    Map.of("name", home.getName(), "world", home.getWorld())));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("essentialsc.home.notifications")
                    && "notifications".startsWith(args[0].toLowerCase())) {
                completions.add("notifications");
            }
            if (sender.hasPermission("essentialsc.home.admin")) {
                for (Player online : plugin.getServer().getOnlinePlayers()) {
                    if (online.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(online.getName());
                    }
                }
            }
        }
        return completions;
    }
}