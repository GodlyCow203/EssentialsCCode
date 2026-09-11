package net.godlycow.org.essc.modules.fly;

import net.godlycow.org.essc.EssentialsC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class FlyManager implements Listener {
    private final EssentialsC plugin;
    private boolean persistent;
    private boolean restoreOnJoin;
    private boolean disableOnJoin;

    public FlyManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.persistent = plugin.getConfigManager().isFlyPersistent();
        this.restoreOnJoin = plugin.getConfigManager().isFlyRestoreOnJoin();
        this.disableOnJoin = plugin.getConfigManager().isFlyDisableOnJoin();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.debug("Fly Manager initialized");
    }

    public void reload() {
        this.persistent = plugin.getConfigManager().isFlyPersistent();
        this.restoreOnJoin = plugin.getConfigManager().isFlyRestoreOnJoin();
        this.disableOnJoin = plugin.getConfigManager().isFlyDisableOnJoin();
        plugin.debug("Fly configuration reloaded");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!persistent) return;

        Player player = event.getPlayer();
        boolean flying = player.isFlying() || player.getAllowFlight();
        if (plugin.getUserManager() != null) {
            plugin.getUserManager().setFlyEnabled(player.getUniqueId(), flying);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        player.getScheduler().runDelayed(plugin, task -> {
            if (!player.isOnline()) return;

            if (disableOnJoin || player.hasPermission("essentialsc.fly.disable-on-join")) {
                player.setFlying(false);
                player.setAllowFlight(false);
                if (persistent && plugin.getUserManager() != null) {
                    plugin.getUserManager().setFlyEnabled(player.getUniqueId(), false);
                }
                plugin.debug("Disabled fly for " + player.getName() + " (disable-on-join)");
            } else if (persistent && restoreOnJoin && hasPersistentFly(player.getUniqueId())) {
                player.setAllowFlight(true);
                player.setFlying(true);
                plugin.debug("Restored fly mode for " + player.getName());
            }
        }, null, 2L);
    }

    public boolean isFlying(Player player) {
        return player.getAllowFlight() && player.isFlying();
    }

    public void setFlying(Player player, boolean flying) {
        player.setAllowFlight(flying);
        player.setFlying(flying);

        if (plugin.getUserManager() != null) {
            plugin.getUserManager().setFlyEnabled(player.getUniqueId(), flying);
        }
    }

    public boolean hasPersistentFly(UUID uuid) {
        if (!persistent || plugin.getUserManager() == null) return false;
        return plugin.getUserManager().isFlyEnabled(uuid);
    }

    public void setPersistentFly(UUID uuid, boolean enabled) {
        if (!this.persistent || plugin.getUserManager() == null) return;
        plugin.getUserManager().setFlyEnabled(uuid, enabled);
    }

    public void shutdown() {
        HandlerList.unregisterAll(this);
        plugin.debug("Shutting down the FlyManager");
    }
}
