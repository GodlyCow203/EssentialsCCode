package net.godlycow.org.essc.command.warp;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.modules.warp.Warp;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class DelWarpCommand extends Command {

    public DelWarpCommand(EssentialsC plugin) {
        super(plugin, "delwarp", "essentialsc.delwarp", true, 1, "command.usage.delwarp");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (!plugin.getConfigManager().isWarpEnabled()) {
            player.sendMessage(lang.get(player, "warp.disabled"));
            plugin.debug("Delwarp command blocked: warp system disabled in config");
            return true;
        }

        String warpName = args[0];

        Warp warp = plugin.getWarpManager().getWarp(warpName);

        if (warp == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("warp", warpName);
            player.sendMessage(lang.get(player, "warp.not_found", placeholders));
            return true;
        }

        boolean canDelete = player.hasPermission("essentialsc.delwarp.others") ||
                (player.hasPermission("essentialsc.delwarp.own") &&
                        warp.getName().startsWith(player.getName().toLowerCase() + "_"));

        if (!canDelete && !player.hasPermission("essentialsc.delwarp.all")) {
            player.sendMessage(lang.get(player, "warp.cannot_delete"));
            return true;
        }

        plugin.getWarpManager().deleteWarp(warpName).thenAccept(success -> {
            player.getScheduler().run(plugin, task -> {
                if (success) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("warp", warpName);
                    player.sendMessage(lang.get(player, "warp.deleted", placeholders));
                    plugin.debug(player.getName() + " deleted warp: " + warpName);
                } else {
                    player.sendMessage(lang.get(player, "error.internal"));
                }
            }, null);
        });

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!plugin.getConfigManager().isWarpEnabled()) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return plugin.getWarpManager().getAllWarps().stream()
                    .map(Warp::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}