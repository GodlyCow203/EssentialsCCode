package net.godlycow.org.essc.migration.listener;

import net.godlycow.org.essc.EssentialsC;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OfflineListener implements Listener {
    private final EssentialsC plugin;
    private final Map<UUID, org.bukkit.Location> pendingBackLocations = new ConcurrentHashMap<>();

    public OfflineListener(EssentialsC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        var uuid = player.getUniqueId();

        var backLoc = pendingBackLocations.remove(uuid);
        if (backLoc != null && plugin.getBackManager() != null) {
            plugin.getBackManager().setBackLocation(player, backLoc);
            plugin.debug("Applied pending back location to " + player.getName());
        }
    }
}