package net.godlycow.org.essc.util;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.back.BackManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public final class TeleportHelper {
    private final EssentialsC plugin;

    public TeleportHelper(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Boolean> teleportAsync(Player player, Location location) {
        return teleportAsync(player, location, true);
    }

    public CompletableFuture<Boolean> teleportAsync(Player player, Location location, boolean saveBack) {
        BackManager backManager = plugin.getBackManager();

        if (saveBack && backManager != null && player.isOnline()) {
            backManager.setBackLocation(player, player.getLocation());
        }

        return player.teleportAsync(location);
    }
}
