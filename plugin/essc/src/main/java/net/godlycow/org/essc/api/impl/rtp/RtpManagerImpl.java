package net.godlycow.org.essc.api.impl.rtp;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.api.rtp.*;
import net.godlycow.org.essc.api.rtp.event.*;
import net.godlycow.org.essc.modules.rtp.RTPManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class RtpManagerImpl implements RtpManager, Listener {
    private final EssentialsC plugin;
    private final Map<UUID, CompletableFuture<RtpResult>> pendingFutures = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastRtpTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> totalRtpCounts = new ConcurrentHashMap<>();
    private final Map<UUID, RtpRequest> activeRequests = new ConcurrentHashMap<>();

    public RtpManagerImpl(EssentialsC plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean isRtpSystemEnabled() {
        RTPManager manager = plugin.getRtpManager();
        if (manager == null) {
            return false;
        }
        return manager.isEnabled();
    }

    @Override
    public Collection<RtpWorldSettings> getConfiguredWorlds() {
        RTPManager manager = plugin.getRtpManager();
        if (manager == null) {
            return Collections.emptyList();
        }
        List<RtpWorldSettings> result = new ArrayList<>();
        for (String worldName : manager.getConfiguredWorldNames()) {
            RTPManager.WorldRTPSettings internal = manager.getWorldSettings(worldName);
            if (internal != null) {
                result.add(new RtpWorldSettingsImpl(worldName, internal));
            }
        }
        return result;
    }

    @Override
    public RtpWorldSettings getWorldSettings(String worldName) {
        RTPManager manager = plugin.getRtpManager();
        if (manager == null) {
            return null;
        }
        RTPManager.WorldRTPSettings internal = manager.getWorldSettings(worldName);
        if (internal == null) {
            return null;
        }
        return new RtpWorldSettingsImpl(worldName, internal);
    }

    @Override
    public boolean isWorldEnabled(String worldName) {
        RTPManager manager = plugin.getRtpManager();
        if (manager == null) {
            return false;
        }
        return manager.isWorldEnabled(worldName);
    }

    @Override
    public boolean isRtpInProgress(Player player) {
        RTPManager manager = plugin.getRtpManager();
        if (manager == null) {
            return false;
        }
        return manager.isRtpInProgress(player);
    }

    @Override
    public boolean isOnCooldown(Player player) {
        RTPManager manager = plugin.getRtpManager();
        if (manager == null) {
            return false;
        }
        return manager.isOnCooldown(player);
    }

    @Override
    public long getRemainingCooldownSeconds(Player player) {
        RTPManager manager = plugin.getRtpManager();
        if (manager == null) {
            return 0;
        }
        return manager.getRemainingCooldown(player);
    }

    @Override
    public boolean hasBypassPermission(Player player, String type) {
        RTPManager manager = plugin.getRtpManager();
        if (manager == null) {
            return false;
        }
        return manager.hasBypassPermission(player, type);
    }

    @Override
    public boolean hasWorldPermission(Player player, String worldName) {
        RTPManager manager = plugin.getRtpManager();
        if (manager == null) {
            return false;
        }
        return manager.hasWorldPermission(player, worldName);
    }

    @Override
    public RtpPlayerState getPlayerState(Player player) {
        RTPManager manager = plugin.getRtpManager();
        if (manager == null) {
            return new RtpPlayerStateImpl(
                    player.getUniqueId(),
                    false,
                    false,
                    0,
                    0,
                    0,
                    false
            );
        }
        boolean inProgress = manager.isRtpInProgress(player);
        boolean onCooldown = manager.isOnCooldown(player);
        long remaining = manager.getRemainingCooldown(player);
        long lastTime = lastRtpTimestamps.getOrDefault(player.getUniqueId(), 0L);
        int totalCount = totalRtpCounts.getOrDefault(player.getUniqueId(), 0);
        boolean pendingWarmup = inProgress;
        return new RtpPlayerStateImpl(
                player.getUniqueId(),
                inProgress,
                onCooldown,
                remaining,
                lastTime,
                totalCount,
                pendingWarmup
        );
    }

    @Override
    public RtpRequest getActiveRequest(Player player) {
        return activeRequests.get(player.getUniqueId());
    }

    @Override
    public CompletableFuture<RtpResult> requestRtp(Player player, World world) {
        CompletableFuture<RtpResult> future = new CompletableFuture<>();
        RTPManager manager = plugin.getRtpManager();
        if (manager == null || !manager.isEnabled()) {
            long now = System.currentTimeMillis();
            RtpResult result = new RtpResultImpl(
                    false,
                    null,
                    world,
                    "RTP system disabled",
                    now,
                    now,
                    0
            );
            future.complete(result);
            return future;
        }
        CompletableFuture<RtpResult> existing = pendingFutures.putIfAbsent(player.getUniqueId(), future);
        if (existing != null) {
            long now = System.currentTimeMillis();
            RtpResult result = new RtpResultImpl(
                    false,
                    null,
                    world,
                    "RTP request already pending",
                    now,
                    now,
                    0
            );
            future.complete(result);
            return future;
        }
        long warmupSeconds = manager.getWarmup();
        boolean bypassWarmup = manager.hasBypassPermission(player, "warmup");
        long actualWarmup = bypassWarmup ? 0 : warmupSeconds;
        RtpRequest request = new RtpRequestImpl(
                UUID.randomUUID(),
                player,
                world,
                System.currentTimeMillis(),
                actualWarmup > 0,
                actualWarmup
        );
        activeRequests.put(player.getUniqueId(), request);
        manager.startRTP(player, world);
        return future;
    }

    @Override
    public void cancelPendingRtp(Player player) {
        RTPManager manager = plugin.getRtpManager();
        if (manager == null) {
            return;
        }
        manager.cancelRtp(player);
        activeRequests.remove(player.getUniqueId());
        pendingFutures.remove(player.getUniqueId());
    }

    @Override
    public List<String> getAvailableWorldNamesFor(Player player) {
        RTPManager manager = plugin.getRtpManager();
        if (manager == null) {
            return Collections.emptyList();
        }
        List<String> available = new ArrayList<>();
        for (String worldName : manager.getConfiguredWorldNames()) {
            if (manager.isWorldEnabled(worldName) && manager.hasWorldPermission(player, worldName)) {
                available.add(worldName);
            }
        }
        return available;
    }

    @Override
    public int getPlayerCountInWorld(String worldName) {
        RTPManager manager = plugin.getRtpManager();
        if (manager == null) {
            return 0;
        }
        return manager.getPlayerCountInWorld(worldName);
    }

    @Override
    public boolean isWorldBorderGloballyEnabled() {
        RTPManager manager = plugin.getRtpManager();
        if (manager == null) {
            return false;
        }
        return manager.isUseBorder();
    }

    @Override
    public long getGlobalCooldownSeconds() {
        RTPManager manager = plugin.getRtpManager();
        if (manager == null) {
            return 0;
        }
        return manager.getCooldown();
    }

    @Override
    public long getGlobalWarmupSeconds() {
        RTPManager manager = plugin.getRtpManager();
        if (manager == null) {
            return 0;
        }
        return manager.getWarmup();
    }

    @Override
    public boolean isCancelOnMovementEnabled() {
        RTPManager manager = plugin.getRtpManager();
        if (manager == null) {
            return false;
        }
        return manager.isCancelOnMovement();
    }

    @Override
    public boolean areParticlesEnabled() {
        RTPManager manager = plugin.getRtpManager();
        if (manager == null) {
            return false;
        }
        return manager.isParticles();
    }

    @Override
    public int getMaxSearchAttempts() {
        RTPManager manager = plugin.getRtpManager();
        if (manager == null) {
            return 0;
        }
        return manager.getMaxAttempts();
    }

    @Override
    public int getGlobalMinY() {
        RTPManager manager = plugin.getRtpManager();
        if (manager == null) {
            return 0;
        }
        return manager.getMinY();
    }

    @Override
    public int getGlobalMaxY() {
        RTPManager manager = plugin.getRtpManager();
        if (manager == null) {
            return 0;
        }
        return manager.getMaxY();
    }

    @EventHandler
    public void onRtpPostTeleport(RtpPostTeleportEvent event) {
        Player player = event.getPlayer();
        activeRequests.remove(player.getUniqueId());
        CompletableFuture<RtpResult> future = pendingFutures.remove(player.getUniqueId());
        if (future != null) {
            long now = System.currentTimeMillis();
            lastRtpTimestamps.put(player.getUniqueId(), now);
            totalRtpCounts.merge(player.getUniqueId(), 1, Integer::sum);
            RtpResult result = new RtpResultImpl(
                    true,
                    event.getDestination(),
                    event.getWorld(),
                    "",
                    now,
                    now,
                    0
            );
            future.complete(result);
        }
    }

    @EventHandler
    public void onRtpFail(RtpFailEvent event) {
        Player player = event.getPlayer();
        activeRequests.remove(player.getUniqueId());
        CompletableFuture<RtpResult> future = pendingFutures.remove(player.getUniqueId());
        if (future != null) {
            long now = System.currentTimeMillis();
            RtpResult result = new RtpResultImpl(
                    false,
                    null,
                    event.getWorld(),
                    event.getReason().name(),
                    now,
                    now,
                    0
            );
            future.complete(result);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        activeRequests.remove(player.getUniqueId());
        pendingFutures.remove(player.getUniqueId());
    }
}