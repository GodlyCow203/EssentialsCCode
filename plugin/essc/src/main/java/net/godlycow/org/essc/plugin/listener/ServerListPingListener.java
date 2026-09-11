package net.godlycow.org.essc.plugin.listener;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.VanishManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class ServerListPingListener implements Listener {

    private final EssentialsC plugin;

    public ServerListPingListener(EssentialsC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        VanishManager vanishManager = plugin.getVanishManager();
        if (vanishManager == null) {
            return;
        }

        Set<UUID> vanished = vanishManager.getVanishedPlayers();
        if (vanished.isEmpty()) {
            return;
        }

        @SuppressWarnings("removal") Iterator<Player> iterator = event.iterator(); // marked for removal since 1.20.6, works for now
        while (iterator.hasNext()) {
            Player player = iterator.next();
            if (vanished.contains(player.getUniqueId())) {
                iterator.remove();
            }
        }
    }
}