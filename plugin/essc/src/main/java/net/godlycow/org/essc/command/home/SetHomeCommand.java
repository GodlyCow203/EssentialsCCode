package net.godlycow.org.essc.command.home;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SetHomeCommand extends Command {

    public SetHomeCommand(EssentialsC plugin) {
        super(plugin, "sethome", "essentialsc.sethome", true, 0, "command.usage.sethome");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length == 0) {
            setHome(player, player.getUniqueId(), plugin.getConfigManager().getDefaultHomeName(), null);
            return true;
        }

        if (args.length >= 2 && player.hasPermission("essentialsc.sethome.admin")) {
            String targetName = args[0];
            String homeName = args[1];

            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                player.sendMessage(lang.get(player, "home.admin.player_not_found", Map.of("player", targetName)));
                return true;
            }

            setHome(player, target.getUniqueId(), homeName, targetName);
            return true;
        }

        setHome(player, player.getUniqueId(), args[0], null);
        return true;
    }

    private void setHome(Player executor, UUID targetUuid, String name, String targetName) {
        if (!name.matches("^[a-zA-Z0-9_-]+$")) {
            executor.sendMessage(lang.get(executor, "home.set.invalid_name"));
            return;
        }

        if (name.length() > 16) {
            executor.sendMessage(lang.get(executor, "home.set.name_too_long"));
            return;
        }

        String worldName = executor.getWorld().getName();
        if (plugin.getConfigManager().getHomeBlockedWorlds().contains(worldName)) {
            executor.sendMessage(lang.get(executor, "home.set.blocked_world",
                    Map.of("world", worldName)));
            return;
        }

        boolean isAdmin = targetName != null;

        plugin.getHomeManager().homeExists(targetUuid, name).whenComplete((alreadyExists, err1) -> {
            if (err1 != null) {
                sendSetFailure(executor, name);
                return;
            }

            if (!isAdmin) {
                plugin.getHomeManager().getEffectiveHomeCount(executor).whenComplete((count, err2) -> {
                    if (err2 != null) {
                        sendSetFailure(executor, name);
                        return;
                    }

                    int max = plugin.getHomeManager().getMaxHomes(executor);

                    if (!alreadyExists && count >= max) {
                        executor.getScheduler().run(plugin, task ->
                                executor.sendMessage(lang.get(executor, "home.set.limit_reached",
                                        Map.of("limit", String.valueOf(max)))), null);
                        return;
                    }

                    saveHome(executor, targetUuid, name, alreadyExists, false, null);
                });
            } else {
                saveHome(executor, targetUuid, name, alreadyExists, true, targetName);
            }
        });
    }

    private void saveHome(Player executor, UUID targetUuid, String name, boolean alreadyExists, boolean isAdmin, String targetName) {
        executor.getScheduler().run(plugin, task -> {
            plugin.getHomeManager().setHome(targetUuid, name, executor.getLocation()).whenComplete((success, err3) -> {
                executor.getScheduler().run(plugin, task2 -> {
                    if (success != null && success) {
                        if (isAdmin) {
                            String key = alreadyExists ? "home.admin.set.updated" : "home.admin.set.success";
                            executor.sendMessage(lang.get(executor, key,
                                    Map.of("player", targetName, "name", name)));
                        } else {
                            String key = alreadyExists ? "home.set.updated" : "home.set.success";
                            executor.sendMessage(lang.get(executor, key, Map.of("name", name)));
                        }
                    } else {
                        executor.sendMessage(lang.get(executor, "home.set.failed", Map.of("name", name)));
                    }
                }, null);
            });
        }, null);
    }

    private void sendSetFailure(Player executor, String name) {
        executor.getScheduler().run(plugin, task ->
                executor.sendMessage(lang.get(executor, "home.set.failed", Map.of("name", name))), null);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player) || !player.hasPermission("essentialsc.sethome.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> completions =  new ArrayList<>();

            for (OfflinePlayer offline : plugin.getServer().getOfflinePlayers()) {
                String name = offline.getName();
                if (name != null && name.toLowerCase().startsWith(partial)) {
                    completions.add(name);
                }
            }

            return completions;
        }

        return Collections.emptyList();
    }
}