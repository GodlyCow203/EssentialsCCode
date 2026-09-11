package net.godlycow.org.essc.command.warp;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.modules.warp.Warp;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WarpAdminCommand extends Command {

    public WarpAdminCommand(EssentialsC plugin) {
        super(plugin, "warpadmin", "essentialsc.warpadmin", true, 1, "command.usage.warpadmin");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (!plugin.getConfigManager().isWarpEnabled()) {
            player.sendMessage(lang.get(player, "warp.disabled"));
            plugin.debug("Warpadmin command blocked: warp system disabled in config");
            return true;
        }

        if (args.length < 1) {
            sendUsage(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "setperm", "permission" -> handleSetPerm(player, args);
            case "setcost", "cost" -> handleSetCost(player, args);
            case "setdesc", "description" -> handleSetDesc(player, args);
            case "setcategory", "category" -> handleSetCategory(player, args);
            case "hide", "hidden" -> handleHide(player, args);
            case "unhide", "visible" -> handleUnhide(player, args);
            case "move", "relocate" -> handleMove(player, args);
            case "info" -> handleInfo(player, args);
            case "reload" -> handleReload(player);
            default -> sendUsage(player);
        }

        return true;
    }

    private void handleSetPerm(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(lang.get(player, "warpadmin.usage.setperm"));
            return;
        }

        Warp warp = getWarpOrError(player, args[1]);
        if (warp == null) return;

        String perm = args[2].equalsIgnoreCase("none") ? null : args[2];
        warp.setPermission(perm);

        String langKey = perm != null ? "warpadmin.perm_set" : "warpadmin.perm_removed";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("warp", warp.getName());
        placeholders.put("perm", perm != null ? perm : "none");

        plugin.getWarpManager().updateWarp(warp).thenAccept(success ->
        player.getScheduler().run(plugin, task ->
                        player.sendMessage(lang.get(player, langKey, placeholders)), null)
        );
    }

    private void handleSetCost(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(lang.get(player, "warpadmin.usage.setcost"));
            return;
        }

        if (!plugin.getConfigManager().isEconomyEnabled()) {
            player.sendMessage(lang.get(player, "warpadmin.economy_disabled"));
            return;
        }

        Warp warp = getWarpOrError(player, args[1]);
        if (warp == null) return;

        try {
            double cost = Double.parseDouble(args[2]);
            if (cost < 0) {
                player.sendMessage(lang.get(player, "warpadmin.invalid_cost"));
                return;
            }

            warp.setCost(cost);

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("warp", warp.getName());
            placeholders.put("cost", String.format("%.2f", cost));

            plugin.getWarpManager().updateWarp(warp).thenAccept(success ->
            player.getScheduler().run(plugin, task ->
                            player.sendMessage(lang.get(player, "warpadmin.cost_set", placeholders)), null)
            );
        } catch (NumberFormatException e) {
            player.sendMessage(lang.get(player, "warpadmin.invalid_number"));
        }
    }

    private void handleSetDesc(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(lang.get(player, "warpadmin.usage.setdesc"));
            return;
        }

        Warp warp = getWarpOrError(player, args[1]);
        if (warp == null) return;

        String desc = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        warp.setDescription(desc);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("warp", warp.getName());

        plugin.getWarpManager().updateWarp(warp).thenAccept(success ->
        player.getScheduler().run(plugin, task ->
                        player.sendMessage(lang.get(player, "warpadmin.desc_set", placeholders)), null)
        );
    }

    private void handleSetCategory(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(lang.get(player, "warpadmin.usage.setcategory"));
            return;
        }

        Warp warp = getWarpOrError(player, args[1]);
        if (warp == null) return;

        warp.setCategory(args[2]);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("warp", warp.getName());
        placeholders.put("category", args[2]);

        plugin.getWarpManager().updateWarp(warp).thenAccept(success ->
        player.getScheduler().run(plugin, task ->
                        player.sendMessage(lang.get(player, "warpadmin.category_set", placeholders)), null)
        );
    }

    private void handleHide(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(lang.get(player, "warpadmin.usage.hide"));
            return;
        }

        Warp warp = getWarpOrError(player, args[1]);
        if (warp == null) return;

        warp.setHidden(true);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("warp", warp.getName());

        plugin.getWarpManager().updateWarp(warp).thenAccept(success ->
        player.getScheduler().run(plugin, task ->
                        player.sendMessage(lang.get(player, "warpadmin.hidden", placeholders)), null)
        );
    }

    private void handleUnhide(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(lang.get(player, "warpadmin.usage.unhide"));
            return;
        }

        Warp warp = getWarpOrError(player, args[1]);
        if (warp == null) return;

        warp.setHidden(false);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("warp", warp.getName());

        plugin.getWarpManager().updateWarp(warp).thenAccept(success ->
        player.getScheduler().run(plugin, task ->
                        player.sendMessage(lang.get(player, "warpadmin.unhidden", placeholders)), null)
        );
    }

    private void handleMove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(lang.get(player, "warpadmin.usage.move"));
            return;
        }

        Warp warp = getWarpOrError(player, args[1]);
        if (warp == null) return;

        warp.setLocation(player.getLocation());

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("warp", warp.getName());

        plugin.getWarpManager().updateWarp(warp).thenAccept(success ->
        player.getScheduler().run(plugin, task ->
                        player.sendMessage(lang.get(player, "warpadmin.moved", placeholders)), null)
        );
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(lang.get(player, "warpadmin.usage.info"));
            return;
        }

        Warp warp = getWarpOrError(player, args[1]);
        if (warp == null) return;

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("warp", warp.getName());
        placeholders.put("world", warp.getLocation().getWorld().getName());
        placeholders.put("x", String.format("%.2f", warp.getLocation().getX()));
        placeholders.put("y", String.format("%.2f", warp.getLocation().getY()));
        placeholders.put("z", String.format("%.2f", warp.getLocation().getZ()));
        placeholders.put("category", warp.getCategory());
        placeholders.put("cost", String.format("%.2f", warp.getCost()));
        placeholders.put("permission", warp.getPermission() != null ? warp.getPermission() : "none");
        placeholders.put("hidden", warp.isHidden() ? "yes" : "no");
        placeholders.put("description", warp.getDescription().isEmpty() ? "none" : warp.getDescription());

        player.sendMessage(lang.get(player, "warpadmin.info", placeholders));
    }

    private void handleReload(Player player) {
        plugin.getWarpManager().reload();
        player.sendMessage(lang.get(player, "warpadmin.reloaded"));

        player.sendMessage(lang.get(player, "warpadmin.status", Map.of(
                "enabled", String.valueOf(plugin.getConfigManager().isWarpEnabled()),
                "cooldown", String.valueOf(plugin.getConfigManager().getWarpCooldown()),
                "warmup", String.valueOf(plugin.getConfigManager().getWarpWarmup()),
                "particles", String.valueOf(plugin.getConfigManager().isWarpParticles()),
                "sounds", String.valueOf(plugin.getConfigManager().isWarpSounds())
        )));
    }

    private Warp getWarpOrError(Player player, String name) {
        Warp warp = plugin.getWarpManager().getWarp(name);
        if (warp == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("warp", name);
            player.sendMessage(lang.get(player, "warp.not_found", placeholders));
        }
        return warp;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!plugin.getConfigManager().isWarpEnabled()) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Stream.of("setperm", "setcost", "setdesc", "setcategory", "hide", "unhide", "move", "info", "reload")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (Stream.of("setperm", "setcost", "setdesc", "setcategory", "hide", "unhide", "move", "info")
                    .anyMatch(sub::equals)) {
                String partial = args[1].toLowerCase();
                return plugin.getWarpManager().getAllWarps().stream()
                        .map(Warp::getName)
                        .filter(name -> name.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}
