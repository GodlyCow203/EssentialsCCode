package net.godlycow.org.essc.plugin.listener;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.VanishManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.UUID;

public class VanishTabCompleteListener implements Listener {

    private final EssentialsC plugin;

    public VanishTabCompleteListener(EssentialsC plugin) {
        this.plugin = plugin;
    }

    //do not show vanished players for tab completion
    @EventHandler
    public void onTabComplete(AsyncTabCompleteEvent event) {
        if (!(event.getSender() instanceof Player sender))
            return;

        if (sender.hasPermission("essentialsc.vanish.see"))
            return;

        VanishManager vanishManager = plugin.getVanishManager();
        if (vanishManager == null)

            return;

        List<UUID> vanishedUUIDs = vanishManager.getVanishedPlayers().stream().toList();
        if (vanishedUUIDs.isEmpty())
            return;

        event.completions().removeIf(completion -> {
            String suggest = completion.suggestion();

            return vanishedUUIDs.stream().anyMatch(uuid -> {

                Player vanished = plugin.getServer().getPlayer(uuid);
                return vanished != null && vanished.getName().equalsIgnoreCase(suggest);
            });
        });
    }
}