package net.godlycow.org.essc.command.warp;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.modules.warp.Warp;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class SetWarpCommand extends Command {

    public SetWarpCommand(EssentialsC plugin) {
        super(plugin, "setwarp", "essentialsc.setwarp", true, 1, "command.usage.setwarp");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (!plugin.getConfigManager().isWarpEnabled()) {
            player.sendMessage(lang.get(player, "warp.disabled"));
            plugin.debug("Setwarp command blocked: warp system disabled in config");
            return true;
        }

        String warpName = args[0];

        int maxWarps = getMaxWarps(player);

        String finalWarpName = maxWarps != Integer.MAX_VALUE
                ? player.getName().toLowerCase() + "_" + warpName
                : warpName;

        int maxLength = plugin.getConfigManager().getWarpMaxNameLength();
        if (finalWarpName.length() > maxLength) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("max", String.valueOf(maxLength));
            player.sendMessage(lang.get(player, "warp.name_too_long", placeholders));
            return true;
        }

        if (!finalWarpName.matches("^[a-zA-Z0-9_-]+$")) {
            player.sendMessage(lang.get(player, "warp.invalid_name"));
            return true;
        }

        if (plugin.getWarpManager().warpExists(finalWarpName)) {
            if (!player.hasPermission("essentialsc.setwarp.overwrite")) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("warp", finalWarpName);
                player.sendMessage(lang.get(player, "warp.already_exists", placeholders));
                return true;
            }

            Warp existing = plugin.getWarpManager().getWarp(finalWarpName);
            existing.setLocation(player.getLocation());
            plugin.getWarpManager().updateWarp(existing).thenAccept(success -> {
                player.getScheduler().run(plugin, task -> {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("warp", finalWarpName);
                    if (success) {
                        player.sendMessage(lang.get(player, "warp.updated", placeholders));
                        plugin.debug(player.getName() + " updated warp: " + finalWarpName);
                    } else {
                        player.sendMessage(lang.get(player, "error.internal"));
                    }
                }, null);
            });
            return true;
        }

        if (maxWarps != Integer.MAX_VALUE) {
            int playerWarps = (int) plugin.getWarpManager().getAllWarps().stream()
                    .filter(w -> w.getName().startsWith(player.getName().toLowerCase() + "_"))
                    .count();

            if (playerWarps >= maxWarps) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("max", String.valueOf(maxWarps));
                player.sendMessage(lang.get(player, "warp.max_reached", placeholders));
                return true;
            }
        }

        Location loc = player.getLocation();
        plugin.getWarpManager().createWarp(finalWarpName, loc).thenAccept(success -> {
            player.getScheduler().run(plugin, task -> {
                if (success) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("warp", finalWarpName);
                    placeholders.put("world", loc.getWorld().getName());
                    placeholders.put("x", String.format("%.1f", loc.getX()));
                    placeholders.put("y", String.format("%.1f", loc.getY()));
                    placeholders.put("z", String.format("%.1f", loc.getZ()));
                    player.sendMessage(lang.get(player, "warp.created", placeholders));
                    plugin.debug(player.getName() + " created warp: " + finalWarpName);
                } else {
                    player.sendMessage(lang.get(player, "error.internal"));
                }
            }, null);
        });

        return true;
    }

    private int getMaxWarps(Player player) {
        if (player.hasPermission("essentialsc.setwarp.unlimited")) {
            return Integer.MAX_VALUE;
        }

        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("essentialsc.setwarp." + i)) {
                return i;
            }
        }

        if (plugin.getConfigManager().isWarpLimitEnabled()) {
            return plugin.getConfigManager().getWarpMaxPerPlayer();
        }

        return Integer.MAX_VALUE;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!plugin.getConfigManager().isWarpEnabled()) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Collections.singletonList("<name>");
        }
        return Collections.emptyList();
    }
}