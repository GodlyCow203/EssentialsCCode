package net.godlycow.org.essc.modules.home;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.api.home.event.HomeCooldownExpireEvent;
import net.godlycow.org.essc.api.home.event.HomePostTeleportEvent;
import net.godlycow.org.essc.api.home.event.HomeTeleportEvent;
import net.godlycow.org.essc.api.home.event.HomeWarmupCancelEvent;
import net.godlycow.org.essc.api.home.event.HomeWarmupStartEvent;
import net.godlycow.org.essc.api.impl.home.HomeImpl;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Strider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class TeleportHandler implements Listener {

    private final EssentialsC plugin;
    private final HomeDatabase repository;

    private final Map<UUID, Long> teleportCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> pendingTeleports = new ConcurrentHashMap<>();
    private final Map<UUID, Home> pendingDestination = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastCooldownExpireNotified = new ConcurrentHashMap<>();

    public TeleportHandler(EssentialsC plugin, HomeDatabase repository) {
        this.plugin = plugin;
        this.repository = repository;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public boolean isOnCooldown(Player player) {
        if (player.hasPermission("essentialsc.home.admin")) return false;

        long cooldown = plugin.getConfigManager().getHomeCooldown();
        if (cooldown <= 0) return false;

        Long last = teleportCooldowns.get(player.getUniqueId());
        if (last == null) return false;

        return System.currentTimeMillis() - last < cooldown * 1000L;
    }

    public long getRemainingCooldown(Player player) {
        long cooldown = plugin.getConfigManager().getHomeCooldown();
        long last = teleportCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long remaining = Math.max(0, (last + (cooldown * 1000L) - System.currentTimeMillis()) / 1000L);
        if (remaining == 0 && cooldown > 0 && last > 0) {
            Long notifiedAt = lastCooldownExpireNotified.get(player.getUniqueId());
            if (notifiedAt == null || notifiedAt != last) {
                lastCooldownExpireNotified.put(player.getUniqueId(), last);
                Bukkit.getPluginManager().callEvent(new HomeCooldownExpireEvent(player, last));
            }
        }
        return remaining;
    }

    public boolean hasPendingTeleport(Player player) {
        return pendingTeleports.containsKey(player.getUniqueId());
    }

    public void cancelTeleport(Player player) {
        ScheduledTask task = pendingTeleports.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            Home home = pendingDestination.remove(player.getUniqueId());
            if (home != null) {
                Bukkit.getPluginManager().callEvent(new HomeWarmupCancelEvent(
                        player, new HomeImpl(home), HomeWarmupCancelEvent.CancelReason.EVENT_CANCELLED));
            }
            plugin.debug("Cancelled pending teleport for " + player.getName());
        }
    }

    public void startTeleport(Player player, Home home) {
        cancelTeleport(player);

        List<String> blocked = plugin.getConfigManager().getHomeBlockedWorlds();
        if (blocked.contains(home.getWorld())) {
            player.sendMessage(plugin.getLanguageManager().get(player, "home.teleport.blocked_world",
                    Map.of("world", home.getWorld())));
            return;
        }

        if (isOnCooldown(player)) {
            player.sendMessage(plugin.getLanguageManager().get(player, "home.teleport.cooldown",
                    Map.of("seconds", String.valueOf(getRemainingCooldown(player)))));
            return;
        }

        HomeTeleportEvent teleportEvent = new HomeTeleportEvent(player, new HomeImpl(home));
        Bukkit.getPluginManager().callEvent(teleportEvent);
        if (teleportEvent.isCancelled()) {
            return;
        }

        Location loc = home.toLocation(plugin.getServer());
        if (loc == null || loc.getWorld() == null) {
            player.sendMessage(plugin.getLanguageManager().get(player, "home.teleport.invalid_world"));
            return;
        }

        long warmup = plugin.getConfigManager().getHomeWarmup();

        if (warmup > 0 && !player.hasPermission("essentialsc.home.admin")) {
            HomeWarmupStartEvent warmupEvent = new HomeWarmupStartEvent(player, new HomeImpl(home), warmup);
            Bukkit.getPluginManager().callEvent(warmupEvent);
            if (warmupEvent.isCancelled()) {
                return;
            }
            warmup = warmupEvent.getWarmupSeconds();

            pendingDestination.put(player.getUniqueId(), home);

            player.sendMessage(plugin.getLanguageManager().get(player, "home.teleport.pending",
                    Map.of("seconds", String.valueOf(warmup), "name", home.getName())));

            plugin.debug("Starting warmup for " + player.getName() + " to home '" + home.getName() + "' (" + warmup + "s)");

            ScheduledTask task = player.getScheduler().runDelayed(plugin, task1 ->
                    completeTeleport(player, home), null, warmup * 20L);

            pendingTeleports.put(player.getUniqueId(), task);
        } else {
            completeTeleport(player, home);
        }
    }

    private boolean isTeleportableMount(Entity entity) {
        return entity instanceof AbstractHorse
                || entity instanceof ChestedHorse
                || entity instanceof Pig
                || entity instanceof Strider;
    }

    private void completeTeleport(Player player, Home home) {
        pendingTeleports.remove(player.getUniqueId());
        pendingDestination.remove(player.getUniqueId());

        Location loc = home.toLocation(plugin.getServer());
        if (loc == null) return;

        boolean hasMount = player.hasPermission("essentialsc.home.mount")
                && player.getVehicle() != null
                && isTeleportableMount(player.getVehicle());

        Entity mount = hasMount ? player.getVehicle() : null;

        if (mount != null) {
            mount.eject();
        }

        plugin.teleportHelper().teleportAsync(player, loc).thenAccept(success -> {
            if (!success) return;
            teleportCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            lastCooldownExpireNotified.remove(player.getUniqueId());

            Bukkit.getPluginManager().callEvent(new HomePostTeleportEvent(player, new HomeImpl(home), loc));

            player.sendMessage(plugin.getLanguageManager().get(player, "home.teleport.success",
                    Map.of("name", home.getName())));

            if (mount != null) {
                Entity finalMount = mount;
                finalMount.getScheduler().run(plugin, task -> {
                    finalMount.teleport(loc);
                    finalMount.addPassenger(player);
                    plugin.debug("Teleported mount " + finalMount.getType() + " for " + player.getName());
                }, null);
            }

            if (plugin.getConfigManager().isHomeParticles()) {
                loc.getWorld().spawnParticle(Particle.PORTAL, loc, 50, 0.5, 1, 0.5);
            }
            if (plugin.getConfigManager().isHomeSounds()) {
                player.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }

            plugin.debug("Teleported " + player.getName() + " to home '" + home.getName() + "'");
        });
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getConfigManager().isHomeCancelOnMovement()) return;

        Player player = event.getPlayer();
        if (!hasPendingTeleport(player)) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) return;
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        ScheduledTask task = pendingTeleports.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            Home home = pendingDestination.remove(player.getUniqueId());
            if (home != null) {
                Bukkit.getPluginManager().callEvent(new HomeWarmupCancelEvent(
                        player, new HomeImpl(home), HomeWarmupCancelEvent.CancelReason.PLAYER_MOVED));
            }
        }
        player.sendMessage(plugin.getLanguageManager().get(player, "home.teleport.cancelled"));
        plugin.debug("Cancelled teleport for " + player.getName() + " due to movement");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ScheduledTask task = pendingTeleports.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            Home home = pendingDestination.remove(player.getUniqueId());
            if (home != null) {
                Bukkit.getPluginManager().callEvent(new HomeWarmupCancelEvent(
                        player, new HomeImpl(home), HomeWarmupCancelEvent.CancelReason.PLAYER_OFFLINE));
            }
        }
        lastCooldownExpireNotified.remove(player.getUniqueId());
    }

    public void shutdown() {
        pendingTeleports.values().forEach(ScheduledTask::cancel);
        pendingTeleports.clear();
        plugin.debug("HomeTeleportHandler shutdown complete.");
    }
}