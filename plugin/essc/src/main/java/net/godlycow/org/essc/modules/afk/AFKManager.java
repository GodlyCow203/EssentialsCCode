package net.godlycow.org.essc.modules.afk;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.plugin.config.EssConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AFKManager implements Listener {

    private final EssentialsC plugin;
    private final EssConfig config;
    private final MiniMessage miniMessage;

    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> afkStatus = new ConcurrentHashMap<>();
    private final Map<UUID, Long> afkStartTime = new ConcurrentHashMap<>();
    private final Map<UUID, Location> afkLocations = new ConcurrentHashMap<>();
    private final Set<UUID> knockbackImmunity = ConcurrentHashMap.newKeySet();

    private ScheduledTask checkTask;
    private ScheduledTask kickTask;

    public AFKManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.miniMessage = plugin.getMiniMessage();

        if (!config.isAfkEnabled()) {
            plugin.debug("AFK system is disabled in config");
            return;
        }

        startTasks();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.debug("AFK Manager initialized");
    }

    public void shutdown() {
        if (checkTask != null) {
            checkTask.cancel();
        }
        if (kickTask != null) {
            kickTask.cancel();
        }

        for (UUID uuid : new HashSet<>(afkStatus.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                setAFK(player, false, false);
            }
        }

        lastActivity.clear();
        afkStatus.clear();
        afkStartTime.clear();
        afkLocations.clear();
        knockbackImmunity.clear();
    }

    public void reload() {
        shutdown();
        if (config.isAfkEnabled()) {
            startTasks();
            plugin.debug("AFK Manager reloaded");
        }
    }

    private void startTasks() {
        checkTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task -> checkAFKStatus(), 100L, 100L);

        if (config.isAfkKickEnabled()) {
            kickTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task -> checkAFKKick(), 1200L, 1200L);
        }
    }

    private void checkAFKStatus() {
        long timeoutMs = config.getAfkTimeout() * 1000L;
        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("essentialsc.afk.bypass.auto")) continue;

            UUID uuid = player.getUniqueId();
            long lastActive = lastActivity.getOrDefault(uuid, now);
            long msInactive = now - lastActive;
            boolean currentlyAFK = afkStatus.getOrDefault(uuid, false);

            if (!currentlyAFK && msInactive >= timeoutMs) {
                setAFK(player, true, true);
            } else if (currentlyAFK && msInactive < timeoutMs) {
                setAFK(player, false, true);
            }
        }
    }

    private void checkAFKKick() {
        if (!config.isAfkKickEnabled()) return;

        long kickTimeoutMs = config.getAfkKickTimeout() * 1000L;
        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("essentialsc.afk.bypass.kick")) continue;

            UUID uuid = player.getUniqueId();
            if (!afkStatus.getOrDefault(uuid, false)) continue;

            Long afkStart = afkStartTime.get(uuid);
            if (afkStart == null) continue;

            long afkDurationMs = now - afkStart;

            if (afkDurationMs >= kickTimeoutMs) {
                Component kickMessage = plugin.getLanguageManager().get(player, "afk.kick.message");
                player.kick(kickMessage);

                if (config.isAfkBroadcastEnabled()) {
                    broadcastKick(player);
                }

                plugin.debug("Kicked " + player.getName() + " for being AFK too long (" + (afkDurationMs / 1000) + "s)");
            }
        }
    }

    public void setAFK(Player player, boolean afk, boolean broadcast) {
        UUID uuid = player.getUniqueId();
        boolean wasAFK = afkStatus.getOrDefault(uuid, false);

        if (wasAFK == afk) return;

        afkStatus.put(uuid, afk);

        if (afk) {
            afkStartTime.put(uuid, System.currentTimeMillis());

            if (config.isAfkFreezePlayer()) {
                afkLocations.put(uuid, player.getLocation().clone());
            }

            if (config.isAfkTitleEnabled()) {
                Title.Times times = Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofDays(1),
                        Duration.ofMillis(500)
                );

                Title title = Title.title(
                        miniMessage.deserialize(config.getAfkTitle()),
                        miniMessage.deserialize(config.getAfkSubtitle()),
                        times
                );
                player.showTitle(title);
            }

            if (broadcast && config.isAfkBroadcastEnabled()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", player.getName());

                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.getUniqueId().equals(uuid)) {
                        online.sendMessage(plugin.getLanguageManager().get(online, "afk.broadcast.enter", placeholders));// use the receivers language instead of the afks player language
                    }
                }
            }

            player.sendMessage(plugin.getLanguageManager().get(player, "afk.self.enter"));
            plugin.debug(player.getName() + " is now AFK");

        } else {
            afkStartTime.remove(uuid);
            afkLocations.remove(uuid);

            player.clearTitle();

            if (broadcast && config.isAfkBroadcastEnabled()) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", player.getName());

                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.getUniqueId().equals(uuid)) {
                        online.sendMessage(plugin.getLanguageManager().get(online, "afk.broadcast.leave", placeholders));// same thing as above
                    }
                }
            }

            player.sendMessage(plugin.getLanguageManager().get(player, "afk.self.leave"));
            plugin.debug(player.getName() + " is no longer AFK");
        }

        if (config.isAfkTabPlaceholderEnabled()) {
            updatePlayerListName(player);
        }
    }

    public void toggleAFK(Player player) {
        UUID uuid = player.getUniqueId();
        boolean currentlyAFK = afkStatus.getOrDefault(uuid, false);
        setAFK(player, !currentlyAFK, true);
        if (currentlyAFK) {
            lastActivity.put(uuid, System.currentTimeMillis());
        }
    }

    public long getAFKDurationSeconds(Player player) {
        Long start = afkStartTime.get(player.getUniqueId());
        if (start == null) return 0;
        return (System.currentTimeMillis() - start) / 1000L;
    }

    public String getAFKDurationFormatted(Player player) {
        long seconds = getAFKDurationSeconds(player);
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }

    public Set<Player> getAFKPlayers() {
        Set<Player> afkPlayers = new HashSet<>();
        for (UUID uuid : afkStatus.keySet()) {
            if (afkStatus.get(uuid)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    afkPlayers.add(player);
                }
            }
        }
        return afkPlayers;
    }

    public void updateActivity(Player player) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        lastActivity.put(uuid, System.currentTimeMillis());

        if (afkStatus.getOrDefault(uuid, false)) {
            setAFK(player, false, true);
        }
    }

    public void updatePlayerListName(Player player) {
        if (plugin.getTabManager() != null) {
            plugin.getTabManager().updatePlayerTab(player);
        }
    }

    public boolean isCommandBlocked(String command) {
        String lowerCmd = command.toLowerCase();
        for (String blocked : config.getAfkBlockedCommands()) {
            if (lowerCmd.startsWith(blocked.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private void broadcastKick(Player player) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("duration", String.valueOf(config.getAfkKickTimeout() / 60));

        Component message = plugin.getLanguageManager().get(player, "afk.kick.broadcast", placeholders);

        Bukkit.broadcast(message);
    }

    private boolean hasPositionChanged(Location from, Location to) {
        return Double.compare(from.getX(), to.getX()) != 0
                || Double.compare(from.getY(), to.getY()) != 0
                || Double.compare(from.getZ(), to.getZ()) != 0;
    }

    private boolean hasLookChanged(Location from, Location to) {
        return Double.compare(from.getYaw(), to.getYaw()) != 0
                || Double.compare(from.getPitch(), to.getPitch()) != 0;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.isInsideVehicle()) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) return;

        boolean positionChanged = hasPositionChanged(from, to);
        boolean lookChanged = hasLookChanged(from, to);

        if (!positionChanged && !lookChanged) return;

        UUID uuid = player.getUniqueId();

        if (isAFK(player) && config.isAfkFreezePlayer()) {
            Location afkLoc = afkLocations.get(uuid);
            if (afkLoc != null) {
                if (positionChanged && !knockbackImmunity.contains(uuid)) {
                    updateActivity(player);
                    return;
                }
                Location frozen = afkLoc.clone();
                frozen.setYaw(to.getYaw());
                frozen.setPitch(to.getPitch());
                event.setTo(frozen);
                return;
            }
        }

        if (!positionChanged && !lookChanged) return;

        if (positionChanged && !config.isAfkFreezePlayer() && knockbackImmunity.contains(uuid)) return;

        updateActivity(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!(event.getVehicle().getPassengers().stream().anyMatch(e -> e instanceof Player))) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        boolean positionChanged = hasPositionChanged(from, to);
        boolean lookChanged = hasLookChanged(from, to);

        if (!positionChanged && !lookChanged) return;

        for (var passenger : event.getVehicle().getPassengers()) {
            if (!(passenger instanceof Player player)) continue;
            updateActivity(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPaperChat(AsyncChatEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().split(" ")[0];

        if (isAFK(player) && isCommandBlocked(command)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getLanguageManager().get(player, "afk.error.blocked_command"));
            return;
        }

        updateActivity(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPickupItem(PlayerAttemptPickupItemEvent event) {
        Player player = event.getPlayer();

        if (isAFK(player) && config.isAfkPreventPickup()) {
            event.setCancelled(true);
            return;
        }

        updateActivity(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (isAFK(player) && config.isAfkPreventDamage()) {
            event.setCancelled(true);
            return;
        }

        UUID uuid = player.getUniqueId();
        knockbackImmunity.add(uuid);
        player.getScheduler().runDelayed(plugin, task -> knockbackImmunity.remove(uuid), null, 10L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            updateActivity(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player player)) return;

        if (isAFK(player) && config.isAfkPreventMobTarget()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
        afkStatus.put(player.getUniqueId(), false);

        player.getScheduler().runDelayed(plugin, task -> {
            if (player.isOnline()) {
                updatePlayerListName(player);
            }
        }, null, 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (afkStatus.getOrDefault(uuid, false)) {
            player.clearTitle();
        }

        lastActivity.remove(uuid);
        afkStatus.remove(uuid);
        afkStartTime.remove(uuid);
        afkLocations.remove(uuid);
        knockbackImmunity.remove(uuid);
    }

    public boolean isAFK(Player player) {
        return afkStatus.getOrDefault(player.getUniqueId(), false);
    }

    public boolean isAFK(UUID uuid) {
        return afkStatus.getOrDefault(uuid, false);
    }

    public int getAFKCount() {
        return getAFKPlayers().size();
    }
}