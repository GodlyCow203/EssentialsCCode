package net.godlycow.org.essc.command.afk;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class AFKListCommand extends Command {

    private final MiniMessage miniMessage;

    public AFKListCommand(EssentialsC plugin) {
        super(plugin, "afklist", "essentialsc.afklist", false, 0, "command.usage.afklist");
        this.miniMessage = plugin.getMiniMessage();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!plugin.getConfigManager().isAfkEnabled()) {
            sender.sendMessage(lang.get(sender, "afk.error.disabled"));
            return true;
        }

        Set<Player> afkPlayers = plugin.getAfkManager().getAFKPlayers();

        if (afkPlayers.isEmpty()) {
            sender.sendMessage(lang.get(sender, "afk.list.empty"));
            return true;
        }

        List<Player> sortedPlayers = sortPlayers(afkPlayers, sender);

        Map<String, String> headerPlaceholders = new HashMap<>();
        headerPlaceholders.put("count", String.valueOf(afkPlayers.size()));
        sender.sendMessage(lang.get(sender, "afk.list.header", headerPlaceholders));

        boolean showLocation = plugin.getConfigManager().isAfkListShowLocation();
        boolean showWorld = plugin.getConfigManager().isAfkListShowWorld();

        for (Player afkPlayer : sortedPlayers) {
            Map<String, String> entryPlaceholders = new HashMap<>();
            entryPlaceholders.put("player", afkPlayer.getName());
            entryPlaceholders.put("duration", plugin.getAfkManager().getAFKDurationFormatted(afkPlayer));

            String entryKey = "afk.list.entry";

            if (showLocation) {
                Location loc = afkPlayer.getLocation();
                entryPlaceholders.put("x", String.format("%.1f", loc.getX()));
                entryPlaceholders.put("y", String.format("%.1f", loc.getY()));
                entryPlaceholders.put("z", String.format("%.1f", loc.getZ()));
                entryKey = "afk.list.entry.location";

                if (showWorld) {
                    entryPlaceholders.put("world", loc.getWorld() != null ? loc.getWorld().getName() : "Unknown");
                    entryKey = "afk.list.entry.location.world";
                }
            }

            sender.sendMessage(lang.get(sender, entryKey, entryPlaceholders));
        }

        sender.sendMessage(lang.get(sender, "afk.list.footer"));

        return true;
    }

    private List<Player> sortPlayers(Set<Player> players, CommandSender sender) {
        String sortBy = plugin.getConfigManager().getAfkListSortBy();
        List<Player> sorted = new ArrayList<>(players);

        switch (sortBy.toLowerCase()) {
            case "time":
            case "duration":
                sorted.sort((p1, p2) -> {
                    long d1 = plugin.getAfkManager().getAFKDurationSeconds(p1);
                    long d2 = plugin.getAfkManager().getAFKDurationSeconds(p2);
                    return Long.compare(d2, d1);
                });
                break;
            case "world":
                sorted.sort(Comparator.comparing(p -> {
                    World world = p.getWorld();
                    return world != null ? world.getName() : "";
                }));
                break;
            case "name":
            default:
                sorted.sort(Comparator.comparing(Player::getName));
                break;
        }

        return sorted;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}