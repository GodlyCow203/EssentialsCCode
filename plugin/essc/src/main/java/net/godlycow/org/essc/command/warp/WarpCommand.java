package net.godlycow.org.essc.command.warp;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.api.impl.warp.WarpImpl;
import net.godlycow.org.essc.api.warp.event.WarpPostTeleportEvent;
import net.godlycow.org.essc.api.warp.event.WarpTeleportEvent;
import net.godlycow.org.essc.api.warp.event.WarpWarmupCancelEvent;
import net.godlycow.org.essc.api.warp.event.WarpWarmupStartEvent;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.modules.warp.Warp;
import net.godlycow.org.essc.modules.warp.WarpManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class WarpCommand extends Command {

    public WarpCommand(EssentialsC plugin) {
        super(plugin, "warp", "essentialsc.warp", true, 0, "command.usage.warp");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (!plugin.getConfigManager().isWarpEnabled()) {
            player.sendMessage(lang.get(player, "warp.disabled"));
            plugin.debug("Warp command blocked: warp system disabled in config");
            return true;
        }

        WarpManager warpManager = plugin.getWarpManager();

        if (args.length == 0) {
            player.performCommand("warps");
            return true;
        }

        String warpName = args[0];
        Warp warp = warpManager.getWarp(warpName);

        if (warp == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("warp", warpName);
            player.sendMessage(lang.get(player, "warp.not_found", placeholders));
            return true;
        }

        if (warp.getPermission() != null && !player.hasPermission(warp.getPermission())
                && !player.hasPermission("essentialsc.warp.bypass")) {
            player.sendMessage(lang.get(player, "warp.no_permission_specific"));
            plugin.debug("Denied: " + player.getName() + " lacks permission for warp " + warpName);
            return true;
        }

        List<String> blockedWorlds = plugin.getConfigManager().getWarpBlockedWorlds();
        if (!blockedWorlds.isEmpty() && blockedWorlds.contains(player.getWorld().getName())) {
            player.sendMessage(lang.get(player, "warp.blocked_world"));
            plugin.debug("Blocked: " + player.getName() + " tried to warp from blocked world " + player.getWorld().getName());
            return true;
        }

        long cooldownSeconds = plugin.getConfigManager().getWarpCooldown();
        if (cooldownSeconds > 0) {
            long remainingCooldown = warpManager.getRemainingCooldown(player.getUniqueId());
            if (remainingCooldown > 0 && !player.hasPermission("essentialsc.warp.bypass.cooldown")) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("time", String.valueOf(remainingCooldown));
                player.sendMessage(lang.get(player, "warp.cooldown", placeholders));
                return true;
            }
        }

        double cost = warp.getCost();
        if (cost > 0 && plugin.isVaultHooked() && !player.hasPermission("essentialsc.warp.free")) {
            plugin.getEconomyManager().getBalance(player.getUniqueId()).thenAccept(balance -> {
                player.getScheduler().run(plugin, task -> {
                    if (balance.doubleValue() < cost) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("cost", String.format("%.2f", cost));
                        player.sendMessage(lang.get(player, "warp.insufficient_funds", placeholders));
                        return;
                    }
                    long warmupSeconds = plugin.getConfigManager().getWarpWarmup();
                    if (warmupSeconds > 0 && !player.hasPermission("essentialsc.warp.bypass.warmup")) {
                        startWarmup(player, warp, warmupSeconds, cost);
                    } else {
                        executeWarp(player, warp, cost);
                    }
                }, null);
            });
            return true;
        }


        long warmupSeconds = plugin.getConfigManager().getWarpWarmup();
        if (warmupSeconds > 0 && !player.hasPermission("essentialsc.warp.bypass.warmup")) {
            startWarmup(player, warp, warmupSeconds, cost);
        } else {
            executeWarp(player, warp, cost);
        }

        return true;
    }

    private void startWarmup(Player player, Warp warp, long seconds, double cost) {
        WarpManager warpManager = plugin.getWarpManager();

        WarpWarmupStartEvent warmupStartEvent = new WarpWarmupStartEvent(player, new WarpImpl(warp), seconds);
        Bukkit.getPluginManager().callEvent(warmupStartEvent);
        if (warmupStartEvent.isCancelled()) {
            plugin.debug("Warp warmup cancelled by WarpWarmupStartEvent for " + player.getName());
            return;
        }
        seconds = warmupStartEvent.getWarmupSeconds();

        warpManager.cancelWarmupTask(player.getUniqueId());
        warpManager.removePendingWarp(player.getUniqueId());
        warpManager.clearMovementTrack(player.getUniqueId());

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("warp", warp.getName());
        placeholders.put("time", String.valueOf(seconds));
        player.sendMessage(lang.get(player, "warp.warmup", placeholders));

        warpManager.setPendingWarp(player.getUniqueId(), warp);

        if (plugin.getConfigManager().isWarpCancelOnMovement()) {
            warpManager.trackMovement(player.getUniqueId(), player.getLocation());
        }

        if (plugin.getConfigManager().isWarpSounds()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        }

        ScheduledTask[] taskHolder = new ScheduledTask[1];

        AtomicLong remaining = new AtomicLong(seconds);
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, task1 -> {
            if (!player.isOnline()) {
                Bukkit.getPluginManager().callEvent(new WarpWarmupCancelEvent(
                        player, new WarpImpl(warp), WarpWarmupCancelEvent.CancelReason.PLAYER_OFFLINE));
                warpManager.cancelWarmupTask(player.getUniqueId());
                warpManager.removePendingWarp(player.getUniqueId());
                warpManager.clearMovementTrack(player.getUniqueId());
                return;
            }

            if (!warpManager.hasPendingWarp(player.getUniqueId())) {
                warpManager.cancelWarmupTask(player.getUniqueId());
                return;
            }

            Warp pending = warpManager.getPendingWarp(player.getUniqueId());
            if (pending == null || !pending.getName().equalsIgnoreCase(warp.getName())) {
                warpManager.cancelWarmupTask(player.getUniqueId());
                return;
            }

            if (plugin.getConfigManager().isWarpCancelOnMovement()) {
                Location original = warpManager.getTrackedLocation(player.getUniqueId());
                if (original != null && player.getLocation().distanceSquared(original) > 0.1) {
                    Bukkit.getPluginManager().callEvent(new WarpWarmupCancelEvent(
                            player, new WarpImpl(warp), WarpWarmupCancelEvent.CancelReason.PLAYER_MOVED));
                    warpManager.cancelWarmupTask(player.getUniqueId());
                    warpManager.removePendingWarp(player.getUniqueId());
                    warpManager.clearMovementTrack(player.getUniqueId());
                    player.sendMessage(lang.get(player, "warp.cancelled_movement"));
                    return;
                }
            }

            var l = remaining.decrementAndGet();
            if (l <= 0) {
                warpManager.removePendingWarp(player.getUniqueId());
                warpManager.clearMovementTrack(player.getUniqueId());
                warpManager.cancelWarmupTask(player.getUniqueId());
                executeWarp(player, warp, cost);
            } else {
                if (plugin.getConfigManager().isWarpSounds() && l <= 3) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.8f + (0.1f * (3 - l)));
                }
            }
        }, null, 20L, 20L);

        warpManager.setWarmupTask(player.getUniqueId(), task);
    }

    private void executeWarp(Player player, Warp warp, double cost) {
        WarpManager warpManager = plugin.getWarpManager();

        WarpTeleportEvent teleportEvent = new WarpTeleportEvent(player, new WarpImpl(warp));
        Bukkit.getPluginManager().callEvent(teleportEvent);
        if (teleportEvent.isCancelled()) {
            plugin.debug("Warp teleport cancelled by WarpTeleportEvent for " + player.getName());
            return;
        }

        if (cost > 0 && plugin.getConfigManager().isEconomyEnabled() && plugin.isVaultHooked()) {
            plugin.getEconomyManager().withdraw(player.getUniqueId(), BigDecimal.valueOf(cost));

            Map<String, String> costPlaceholders = new HashMap<>();
            costPlaceholders.put("amount", String.format("%.2f", cost));
            player.sendMessage(lang.get(player, "warp.charged", costPlaceholders));
        }

        Location dest = warp.getLocation();
        plugin.teleportHelper().teleportAsync(player, dest).thenAccept(success -> {
            if (!success) return;

            Bukkit.getPluginManager().callEvent(new WarpPostTeleportEvent(player, new WarpImpl(warp), dest));

            if (plugin.getConfigManager().isWarpParticles()) {
                dest.getWorld().spawnParticle(Particle.PORTAL, dest, 100, 0.5, 1, 0.5);
                dest.getWorld().spawnParticle(Particle.END_ROD, dest, 50, 0.5, 1, 0.5, 0.1);
            }

            if (plugin.getConfigManager().isWarpSounds()) {
                player.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            }

            if (plugin.getConfigManager().getWarpCooldown() > 0) {
                warpManager.setCooldown(player.getUniqueId());
            }

            warpManager.recordWarpUsage(player.getUniqueId(), warp.getName());

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("warp", warp.getName());
            player.sendMessage(lang.get(player, "warp.success", placeholders));

            plugin.debug(player.getName() + " warped to " + warp.getName() +
                    " [particles:" + plugin.getConfigManager().isWarpParticles() +
                    ", sounds:" + plugin.getConfigManager().isWarpSounds() + "]");
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();

        if (!plugin.getConfigManager().isWarpEnabled()) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return plugin.getWarpManager().getVisibleWarps().stream()
                    .filter(w -> sender.hasPermission("essentialsc.warp") &&
                            (w.getPermission() == null || sender.hasPermission(w.getPermission())))
                    .map(Warp::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}