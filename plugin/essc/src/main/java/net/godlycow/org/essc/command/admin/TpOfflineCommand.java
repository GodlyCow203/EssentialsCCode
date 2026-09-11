package net.godlycow.org.essc.command.admin;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.storage.user.UserManager;
import net.godlycow.org.essc.storage.user.UserProfile;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TpOfflineCommand extends Command {

    private final UserManager userManager;

    public TpOfflineCommand(EssentialsC plugin, UserManager userManager) {
        super(plugin, "tpoffline", "essentialsc.tpoffline", true, 1, "command.usage.tpoffline");
        this.userManager = userManager;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        String targetName = args[0];

        OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", targetName);
            player.sendMessage(lang.get(player, "error.player_not_found", placeholders));
            return true;
        }

        UserProfile cached = userManager.getCachedProfile(target.getUniqueId());
        if (cached != null) {
            teleportToProfile(player, cached);
            return true;
        }

        userManager.findProfile(target.getUniqueId()).thenAccept(profile -> {
            if (profile == null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", targetName);
                plugin.getServer().getGlobalRegionScheduler().run(plugin, task ->
                        player.sendMessage(lang.get(player, "tpoffline.no_location", placeholders)));
                return;
            }
            plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> teleportToProfile(player, profile));
        });

        return true;
    }

    private void teleportToProfile(Player player, UserProfile profile) {
        Location loc = profile.getLogoutLocation();

        if (loc == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", profile.getUsername());
            player.sendMessage(lang.get(player, "tpoffline.no_location", placeholders));
            return;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", profile.getUsername());
        placeholders.put("world", loc.getWorld().getName());
        placeholders.put("x", String.format("%.1f", loc.getX()));
        placeholders.put("y", String.format("%.1f", loc.getY()));
        placeholders.put("z", String.format("%.1f", loc.getZ()));

        plugin.teleportHelper().teleportAsync(player, loc).thenAccept(success -> {
            if (!success) return;
            player.sendMessage(lang.get(player, "tpoffline.success", placeholders));
            plugin.debug(player.getName() + " teleported to " + profile.getUsername() + "'s logout location");
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();

            Set<String> suggestions = new HashSet<>();

            plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .forEach(suggestions::add);

            for (OfflinePlayer offline : plugin.getServer().getOfflinePlayers()) {
                String name = offline.getName();
                if (name != null && name.toLowerCase().startsWith(input)) {
                    suggestions.add(name);
                }
            }

            return suggestions.stream()
                    .sorted()
                    .limit(50)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}