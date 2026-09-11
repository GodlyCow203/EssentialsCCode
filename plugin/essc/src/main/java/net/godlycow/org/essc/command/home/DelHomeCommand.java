package net.godlycow.org.essc.command.home;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DelHomeCommand extends Command {

    private final Map<UUID, String> pendingDeletions = new HashMap<>();
    private final Map<UUID, ScheduledTask> pendingTasks = new HashMap<>();

    public DelHomeCommand(EssentialsC plugin) {
        super(plugin, "delhome", "essentialsc.delhome", true, 0, "command.usage.delhome");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(lang.get(player, "home.delete.no_name_provided"));
            return true;
        }

        if (args.length >= 2 && player.hasPermission("essentialsc.delhome.admin")) {
            String targetName = args[0];
            String name = args[1].toLowerCase();

            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (!target.hasPlayedBefore()  && !target.isOnline()) {
                player.sendMessage(lang.get(player, "home.admin.player_not_found", Map.of("player", targetName)));

                return true;
            }

            deleteHome(player, target.getUniqueId(),  name, targetName);
            return true;
        }

        String name = args[0].toLowerCase();

        String pending = pendingDeletions.get(player.getUniqueId());

        if (pending != null && pending.equals(name)) {
            cancelPending(player.getUniqueId());
            deleteHome(player, player.getUniqueId(), name, null);
        } else {
            setPendingDeletion(player.getUniqueId(), name);
            player.sendMessage(lang.get(player, "home.delete.confirm", Map.of("name", name)));
        }

        return true;
    }

    private void setPendingDeletion(UUID uuid, String name) {
        cancelPending(uuid);

        pendingDeletions.put(uuid, name);

        Player p = plugin.getServer().getPlayer(uuid);
        if (p == null) return;

        ScheduledTask task = p.getScheduler().runDelayed(plugin, task1 -> {
            pendingDeletions.remove(uuid);
            pendingTasks.remove(uuid);
        }, null, 15 * 20L);

        pendingTasks.put(uuid, task);
    }

    private void cancelPending(UUID uuid) {
        ScheduledTask task = pendingTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        pendingDeletions.remove(uuid);
    }

    private void deleteHome(Player player, UUID targetUuid, String name, String targetName) {
        plugin.getHomeManager().deleteHome(targetUuid, name).whenComplete((success, err) -> {
            player.getScheduler().run(plugin, task -> {
                if (success) {
                    if (targetName != null) {
                        player.sendMessage(lang.get(player, "home.admin.delete.success",
                                Map.of("player", targetName, "name", name)));
                    } else {
                        player.sendMessage(lang.get(player, "home.delete.success", Map.of("name", name)));
                    }
                } else {
                    if (targetName != null) {
                        player.sendMessage(lang.get(player, "home.admin.delete.not_found",
                                Map.of("player", targetName, "name", name)));
                    } else {
                        player.sendMessage(lang.get(player, "home.delete.not_found", Map.of("name", name)));
                    }
                }
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

            Set<String> homeNames = plugin.getHomeManager().getCachedHomeNames(player.getUniqueId());
            for (String homeName : homeNames) {
                if (homeName.startsWith(partial)) {
                    completions.add(homeName);
                }
            }

            return completions;
        } else if (args.length == 2 && player.hasPermission("essentialsc.delhome.admin")) {
            String partial = args[1].toLowerCase();
            List<String> completions = new ArrayList<>();

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target.hasPlayedBefore() || target.isOnline()) {
                Set<String> homeNames = plugin.getHomeManager().getCachedHomeNames(target.getUniqueId());
                for (String homeName : homeNames) {
                    if (homeName.startsWith(partial)) {
                        completions.add(homeName);
                    }
                }
            }

            return completions;
        }

        return Collections.emptyList();
    }
}