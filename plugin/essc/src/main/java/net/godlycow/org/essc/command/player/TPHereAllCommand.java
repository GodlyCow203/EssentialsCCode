package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TPHereAllCommand extends Command {

    public TPHereAllCommand(EssentialsC plugin) {
        super(plugin, "tphereall", "essentialsc.tphereall", true, 0, "command.usage.tphereall");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        Location targetLocation = player.getLocation();

        int skippedCount = 0;

        Set<UUID> blockedPlayers = plugin.getTPAManager() != null
                ? plugin.getTPAManager().getBlockedPlayers()
                : Collections.emptySet();

        List<java.util.concurrent.CompletableFuture<Boolean>> futures = new java.util.ArrayList<>();
        List<Player> targets = new java.util.ArrayList<>();

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target == player) continue;

            if (plugin.getVanishManager() != null && plugin.getVanishManager().isVanished(target)) {
                if (!player.hasPermission("essentialsc.vanish.bypass")) {
                    skippedCount++;
                    continue;
                }
            }

            if (blockedPlayers.contains(target.getUniqueId())) {
                if (!player.hasPermission("essentialsc.tphereall.bypass")) {
                    skippedCount++;
                    continue;
                }
            }

            targets.add(target);
            futures.add(plugin.teleportHelper().teleportAsync(target, targetLocation));
        }

        final int skipped = skippedCount;
        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                .thenRun(() -> {
                    int teleportedCount = 0;
                    for (int i = 0; i < futures.size(); i++) {
                        try {
                            if (futures.get(i).get()) {
                                teleportedCount++;
                                Player target = targets.get(i);
                                Map<String, String> targetPlaceholders = new HashMap<>();
                                targetPlaceholders.put("player", player.getName());
                                target.sendMessage(lang.get(target, "tphereall.teleported", targetPlaceholders));
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    Map<String, String> senderPlaceholders = new HashMap<>();
                    senderPlaceholders.put("count", String.valueOf(teleportedCount));
                    senderPlaceholders.put("skipped", String.valueOf(skipped));
                    player.sendMessage(lang.get(player, "tphereall.success", senderPlaceholders));

                    plugin.debug("TPHereAll: " + player.getName() + " teleported " + teleportedCount + " players, skipped " + skipped);
                });

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}