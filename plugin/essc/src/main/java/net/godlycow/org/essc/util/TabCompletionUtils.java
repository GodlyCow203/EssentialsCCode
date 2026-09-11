package net.godlycow.org.essc.util;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.VanishManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class TabCompletionUtils {

    private TabCompletionUtils() {
    }

    public static boolean canSeeVanished(EssentialsC plugin, CommandSender sender) {
        if (!(sender instanceof Player))
            return true;
        VanishManager vm = plugin.getVanishManager();

        return vm == null || sender.hasPermission("essentialsc.vanish.see");
    }

    public static Set<UUID> getVanishedUUIDs(EssentialsC plugin) {
        VanishManager vm = plugin.getVanishManager();

        return vm == null ? Set.of() : vm.getVanishedPlayers();
    }


    public static List<String> filterCompletions(EssentialsC plugin, CommandSender sender, List<String> completions) {
        if (canSeeVanished(plugin, sender) || completions == null || completions.isEmpty()) {
            return completions;
        }

        Set<UUID> vanished = getVanishedUUIDs(plugin);
        if (vanished.isEmpty())
            return completions;

        Set<String> vanishedNames = vanished.stream()
                .map(uuid -> plugin.getServer().getPlayer(uuid))
                .filter(java.util.Objects::nonNull)
                .map(Player::getName)
                .collect(Collectors.toSet());

        if (vanishedNames.isEmpty())
            return completions;

        return completions.stream()
                .filter(c -> c == null || !vanishedNames.contains(c))
                .collect(Collectors.toList());
    }

}
