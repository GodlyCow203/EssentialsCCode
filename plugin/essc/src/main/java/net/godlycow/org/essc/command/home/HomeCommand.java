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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HomeCommand extends Command {

    public HomeCommand(EssentialsC plugin) {
        super(plugin, "home", "essentialsc.home", true, 0, "command.usage.home");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length == 0) {
            String defaultHome = plugin.getConfigManager().getDefaultTeleportHomeName();
            if (!defaultHome.isEmpty()) {
                teleportToHome(player, player.getUniqueId(), defaultHome.toLowerCase(), null);
                return true;
            }
            showHomeList(player);
            return true;
        }

        if (args.length > 1 && player.hasPermission("essentialsc.home.admin")) {
            String targetName = args[0];
            String homeName = args[1];

            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                player.sendMessage(lang.get(player, "home.admin.player_not_found", Map.of("player", targetName)));
                return true;
            }

            teleportToHome(player, target.getUniqueId(), homeName, targetName);
            return true;
        }

        String name = args[0].toLowerCase();

        if (name.equals("bed") && player.hasPermission("essentialsc.home.bed")) {
            Location bedLoc = RespawnUtil.getBedSpawnLocation(player);
            if (bedLoc == null) {
                player.sendMessage(lang.get(player, "home.bed.no_bed"));
                return true;
            }
            plugin.teleportHelper().teleportAsync(player, bedLoc).thenAccept(success -> {
                if (success) {
                    player.getScheduler().run(plugin, task ->
                            player.sendMessage(lang.get(player, "home.bed.teleported")), null);
                }
            });
            return true;
        }

        teleportToHome(player, player.getUniqueId(), name, null);
        return true;
    }

    private void teleportToHome(Player player, UUID targetUuid, String name, String targetName) {
        plugin.getHomeManager().getHome(targetUuid, name).whenComplete((home, err) -> {
            player.getScheduler().run(plugin, task -> {
                if (home == null) {
                    String defaultHome = plugin.getConfigManager().getDefaultTeleportHomeName();
                    if (targetName == null && name.equalsIgnoreCase(defaultHome) && !defaultHome.isEmpty()) {
                        player.sendMessage(lang.get(player, "home.teleport.default_not_found", Map.of("name", name)));
                    } else {
                        player.sendMessage(lang.get(player, "home.teleport.not_found", Map.of("name", name)));
                    }
                    return;
                }

                if (targetName != null) {
                    Location loc = home.toLocation(plugin.getServer());
                    if (loc != null) {
                        plugin.teleportHelper().teleportAsync(player, loc).thenAccept(success -> {
                            if (success) player.sendMessage(lang.get(player, "home.admin.teleported_to_other",
                                    Map.of("player", targetName, "name", name)));
                        });
                    }
                } else {
                    plugin.getHomeManager().startTeleport(player, home);
                }
            }, null);
        });
    }

    private void showHomeList(Player player) {
        plugin.getHomeManager().getHomes(player.getUniqueId()).whenComplete((homes, err) -> {
            player.getScheduler().run(plugin, task -> {
                boolean hasBed = player.hasPermission("essentialsc.home.bed")
                        && RespawnUtil.getBedSpawnLocation(player) != null;

                if ((homes == null || homes.isEmpty()) && !hasBed) {
                    player.sendMessage(lang.get(player, "home.list.empty"));
                    return;
                }

                int max = plugin.getHomeManager().getMaxHomes(player);
                int used = (homes == null ? 0 : homes.size()) + (hasBed && plugin.getConfigManager().isBedHomeCountsInLimit() ? 1 : 0);
                String maxStr = max == Integer.MAX_VALUE ? "∞" : String.valueOf(max);

                player.sendMessage(lang.get(player, "home.list.header",
                        Map.of("used", String.valueOf(used), "limit", maxStr)));

                StringBuilder sb = new StringBuilder();

                if (hasBed) {
                    sb.append("<click:run_command:/home bed><color:#FFF200>bed</color></click>");
                    if (homes != null && !homes.isEmpty()) {
                        sb.append("<color:#666666>, </color>");
                    }
                }

                if (homes != null) {
                    for (int i = 0; i < homes.size(); i++) {
                        Home home = homes.get(i);
                        sb.append("<click:run_command:/home ").append(home.getName()).append(">")
                                .append("<color:#FFF200>").append(home.getName()).append("</color>")
                                .append("</click>");

                        if (i < homes.size() - 1) {
                            sb.append("<color:#666666>, </color>");
                        }
                    }
                }

                player.sendMessage(lang.get(player, "home.list.entries",
                        Map.of("homes", sb.toString())));
            }, null);
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }
        Player player = (Player) sender;

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();

            if ("bed".startsWith(partial) && player.hasPermission("essentialsc.home.bed")
                    && RespawnUtil.getBedSpawnLocation(player) != null) {
                completions.add("bed");
            }

            Set<String> homeNames = plugin.getHomeManager().getCachedHomeNames(player.getUniqueId());
            for (String name : homeNames) {
                if (name.startsWith(partial)) {
                    completions.add(name);
                }
            }

            if (player.hasPermission("essentialsc.home.admin")) {
                for (Player online : plugin.getServer().getOnlinePlayers()) {
                    if (online.getName().toLowerCase().startsWith(partial)) {
                        completions.add(online.getName());
                    }
                }
            }

            return completions;
        } else if (args.length == 2 && player.hasPermission("essentialsc.home.admin")) {
            String targetName = args[0];
            String partial = args[1].toLowerCase();
            Player target = plugin.getServer().getPlayer(targetName);

            if (target != null) {
                Set<String> homeNames = plugin.getHomeManager().getCachedHomeNames(target.getUniqueId());
                List<String> completions = new ArrayList<>();
                for (String name : homeNames) {
                    if (name.startsWith(partial)) {
                        completions.add(name);
                    }
                }
                return completions;
            }
        }

        return Collections.emptyList();
    }
}