package net.godlycow.org.essc.command.kit;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.modules.kit.Kit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KitCommand extends Command {

    public KitCommand(EssentialsC plugin) {
        super(plugin, "kit", null, true, 0);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length == 0) {
            if (plugin.getConfigManager().isKitGuiMode() && plugin.getKitGuiManager() != null) {
                plugin.getKitGuiManager().openKitList(player, 1);
            } else {
                showKitsList(player);
            }
            return true;
        }

        String subCmd = args[0].toLowerCase();

        switch (subCmd) {
            case "claim" -> {
                if (args.length < 2) {
                    sendUsage(player);
                    return true;
                }
                handleClaim(player, args[1]);
            }
            case "cooldown" -> {
                if (args.length < 2) {
                    sendUsage(player);
                    return true;
                }
                handleCooldown(player, args[1]);
            }
            case "notifications" -> handleNotifications(player);
            case "debug" -> {
                if (!player.hasPermission("essentialsc.kits.admin")) {
                    player.sendMessage(lang.get(player, "error.no_permission"));
                    return true;
                }
                if (args.length < 2) {
                    sendUsage(player);
                    return true;
                }
                handleDebug(player, args[1]);
            }
            default -> {
                Kit kit = plugin.getKitManager().getKit(subCmd);
                if (kit != null) {
                    handleClaim(player, subCmd);
                } else {
                    showKitsList(player);
                }
            }
        }

        return true;
    }

    private void handleClaim(Player player, String kitName) {
        Kit kit = plugin.getKitManager().getKit(kitName);

        if (kit == null) {
            player.sendMessage(lang.get(player, "kit.not_found", Map.of("kit", kitName)));
            return;
        }

        if (!plugin.getKitManager().hasPermission(player, kit)) {
            player.sendMessage(lang.get(player, "kit.no_permission", Map.of("kit", kit.getDisplayName())));
            return;
        }

        if (kit.isOneTime() && plugin.getKitManager().hasClaimed(player, kit)) {
            player.sendMessage(lang.get(player, "kit.one_time_used", Map.of("kit", kit.getDisplayName())));
            return;
        }

        if (kit.getMaxClaims() > 0) {
            int count = plugin.getKitManager().getClaimCount(player, kit);
            if (count >= kit.getMaxClaims()) {
                player.sendMessage(lang.get(player, "kit.max_claims_reached",
                        Map.of("kit", kit.getDisplayName(), "max", String.valueOf(kit.getMaxClaims()))));
                return;
            }
        }

        plugin.getKitManager().getCooldownRemainingAsync(player, kit).thenAccept(cooldown -> {
            if (cooldown > 0 && !plugin.getKitManager().hasCooldownBypass(player, kit)) {
                player.sendMessage(lang.get(player, "kit.cooldown_active",
                        Map.of("kit", kit.getDisplayName(), "time", formatTime(cooldown))));
                return;
            }

            player.getScheduler().run(plugin, task ->
                    plugin.getKitManager().giveKit(player, kit), null);
        });
    }

    private void handleCooldown(Player player, String kitName) {
        Kit kit = plugin.getKitManager().getKit(kitName);

        if (kit == null) {
            player.sendMessage(lang.get(player, "kit.not_found", Map.of("kit", kitName)));
            return;
        }

        if (kit.getCooldown() == 0) {
            player.sendMessage(lang.get(player, "kit.no_cooldown", Map.of("kit", kit.getDisplayName())));
            return;
        }

        plugin.getKitManager().getCooldownRemainingAsync(player, kit).thenAccept(remaining -> {
            if (remaining == 0) {
                player.sendMessage(lang.get(player, "kit.cooldown.ready", Map.of("kit", kit.getDisplayName())));
            } else {
                player.sendMessage(lang.get(player, "kit.cooldown.status",
                        Map.of("kit", kit.getDisplayName(), "time", formatTime(remaining))));
            }
        });
    }

    private void handleNotifications(Player player) {
        if (!player.hasPermission("essentialsc.kit.notifications")) {
            player.sendMessage(lang.get(player, "error.no_permission"));
            return;
        }
        boolean current = plugin.getKitManager().isNotificationsEnabled(player.getUniqueId());
        boolean newValue = !current;
        plugin.getKitManager().setNotificationsEnabled(player.getUniqueId(), newValue);
        player.sendMessage(lang.get(player, newValue ? "kit.notifications.enabled" : "kit.notifications.disabled"));
    }

    private void handleDebug(Player player, String kitName) {
        Kit kit = plugin.getKitManager().getKit(kitName);

        if (kit == null) {
            player.sendMessage(lang.get(player, "kit.not_found", Map.of("kit", kitName)));
            return;
        }

        player.sendMessage(lang.get(player, "kit.debug.header", Map.of("kit", kit.getName())));
        player.sendMessage(lang.get(player, "kit.debug.name", Map.of("name", kit.getDisplayName())));
        player.sendMessage(lang.get(player, "kit.debug.permission", Map.of("perm", kit.getPermission())));
        player.sendMessage(lang.get(player, "kit.debug.items", Map.of("count", String.valueOf(kit.getItems().size()))));
        player.sendMessage(lang.get(player, "kit.debug.cooldown", Map.of("secs", String.valueOf(kit.getCooldown()))));
        player.sendMessage(lang.get(player, "kit.debug.onetime", Map.of("val", String.valueOf(kit.isOneTime()))));
        player.sendMessage(lang.get(player, "kit.debug.firstjoin", Map.of("val", String.valueOf(kit.isFirstJoin()))));
        player.sendMessage(lang.get(player, "kit.debug.networksync", Map.of("val", String.valueOf(kit.isNetworkSync()))));
        player.sendMessage(lang.get(player, "kit.debug.page", Map.of("page", String.valueOf(kit.getGuiPage()), "slot", String.valueOf(kit.getGuiSlot()))));

        if (kit.getMaxClaims() > 0) {
            player.sendMessage(lang.get(player, "kit.debug.maxclaims", Map.of("max", String.valueOf(kit.getMaxClaims()))));
        }

        if (!kit.getItems().isEmpty()) {
            player.sendMessage(lang.get(player, "kit.debug.itemlist"));
            for (var item : kit.getItems()) {
                player.sendMessage(lang.get(player, "kit.debug.item_entry", Map.of("type", item.getType().name(), "amount", String.valueOf(item.getAmount()))));
            }
        }
    }

    private void showKitsList(Player player) {
        var available = plugin.getKitManager().getKits().stream()
                .filter(k -> plugin.getKitManager().hasPermission(player, k))
                .toList();

        if (available.isEmpty()) {
            player.sendMessage(lang.get(player, "kit.list.empty"));
            return;
        }

        player.sendMessage(lang.get(player, "kit.list.header"));
        for (Kit kit : available) {
            boolean canClaim = plugin.getKitManager().canClaim(player, kit);
            String status = canClaim ? "<green>✔</green>" : "<red>✖</red>";

            player.sendMessage(lang.get(player, "kit.list.entry",
                    Map.of(
                            "status", status,
                            "kit", kit.getDisplayName(),
                            "name", kit.getName()
                    )));
        }
    }

    private String formatTime(long seconds) {
        Duration dur = Duration.ofSeconds(seconds);
        long hours = dur.toHours();
        long mins = dur.toMinutesPart();
        long secs = dur.toSecondsPart();

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, mins, secs);
        }
        if (mins > 0) {
            return String.format("%dm %ds", mins, secs);
        }
        return secs + "s";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> subs = new java.util.ArrayList<>(List.of("claim", "cooldown"));
            if (sender.hasPermission("essentialsc.kit.notifications")) subs.add("notifications");
            if (sender.hasPermission("essentialsc.kits.admin")) {
                subs.add("debug");
            }

            List<String> kits = plugin.getKitManager().getKits().stream()
                    .filter(k -> sender.hasPermission(k.getPermission()))
                    .map(Kit::getName)
                    .collect(Collectors.toList());

            List<String> all = new java.util.ArrayList<>(subs);
            all.addAll(kits);
            return all.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("claim") ||
                args[0].equalsIgnoreCase("cooldown") ||
                args[0].equalsIgnoreCase("debug"))) {
            return plugin.getKitManager().getKits().stream()
                    .map(Kit::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return super.tabComplete(sender, args);
    }

    private void sendUsage(Player player) {
        player.sendMessage(lang.get(player, "kit.usage"));
    }
}