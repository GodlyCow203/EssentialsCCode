package net.godlycow.org.essc.api.impl.warp;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.api.warp.Warp;
import net.godlycow.org.essc.api.warp.WarpManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class WarpManagerImpl implements WarpManager {
    private final EssentialsC plugin;

    public WarpManagerImpl(EssentialsC plugin) {
        this.plugin = plugin;
    }

    private net.godlycow.org.essc.modules.warp.WarpManager internal() {
        return plugin.getWarpManager();
    }

    private Warp wrap(net.godlycow.org.essc.modules.warp.Warp warp) {
        return warp == null ? null : new WarpImpl(warp);
    }

    @Override
    public boolean isWarpSystemEnabled() {
        return plugin.getConfigManager().isWarpEnabled();
    }

    @Override
    public Warp getWarp(String name) {
        net.godlycow.org.essc.modules.warp.WarpManager manager = internal();
        if (manager == null) return null;
        return wrap(manager.getWarp(name));
    }

    @Override
    public boolean warpExists(String name) {
        net.godlycow.org.essc.modules.warp.WarpManager manager = internal();
        return manager != null && manager.warpExists(name);
    }

    @Override
    public Collection<Warp> getAllWarps() {
        net.godlycow.org.essc.modules.warp.WarpManager manager = internal();
        if (manager == null) return Collections.emptyList();
        return manager.getAllWarps().stream().map(this::wrap).collect(Collectors.toList());
    }

    @Override
    public Collection<Warp> getVisibleWarps() {
        net.godlycow.org.essc.modules.warp.WarpManager manager = internal();
        if (manager == null) return Collections.emptyList();
        return manager.getVisibleWarps().stream().map(this::wrap).collect(Collectors.toList());
    }

    @Override
    public Collection<Warp> getWarpsByCategory(String category) {
        net.godlycow.org.essc.modules.warp.WarpManager manager = internal();
        if (manager == null) return Collections.emptyList();
        return manager.getWarpsByCategory(category).stream().map(this::wrap).collect(Collectors.toList());
    }

    @Override
    public Set<String> getCategories() {
        net.godlycow.org.essc.modules.warp.WarpManager manager = internal();
        if (manager == null) return Collections.emptySet();
        return manager.getCategories();
    }

    @Override
    public CompletableFuture<Boolean> createWarp(String name, Location location) {
        net.godlycow.org.essc.modules.warp.WarpManager manager = internal();
        if (manager == null) return CompletableFuture.completedFuture(false);
        return manager.createWarp(name, location);
    }

    @Override
    public CompletableFuture<Boolean> deleteWarp(String name) {
        net.godlycow.org.essc.modules.warp.WarpManager manager = internal();
        if (manager == null) return CompletableFuture.completedFuture(false);
        return manager.deleteWarp(name);
    }

    @Override
    public CompletableFuture<Boolean> updateWarp(Warp warp) {
        net.godlycow.org.essc.modules.warp.WarpManager manager = internal();
        if (manager == null || warp == null) return CompletableFuture.completedFuture(false);

        net.godlycow.org.essc.modules.warp.Warp internalWarp;
        if (warp instanceof WarpImpl impl) {
            internalWarp = impl.getInternalWarp();
        } else {
            internalWarp = new net.godlycow.org.essc.modules.warp.Warp(warp.getName(), warp.getLocation());
            internalWarp.setPermission(warp.getPermission());
            internalWarp.setCost(warp.getCost());
            internalWarp.setHidden(warp.isHidden());
            internalWarp.setDescription(warp.getDescription());
            internalWarp.setCategory(warp.getCategory());
        }
        return manager.updateWarp(internalWarp);
    }

    @Override
    public CompletableFuture<Integer> getWarpUsage(UUID playerId, String warpName) {
        net.godlycow.org.essc.modules.warp.WarpManager manager = internal();
        if (manager == null) return CompletableFuture.completedFuture(0);
        return manager.getWarpUsage(playerId, warpName);
    }

    @Override
    public boolean isOnCooldown(Player player) {
        net.godlycow.org.essc.modules.warp.WarpManager manager = internal();
        if (manager == null) return false;
        return manager.getRemainingCooldown(player.getUniqueId()) > 0;
    }

    @Override
    public long getRemainingCooldownSeconds(Player player) {
        net.godlycow.org.essc.modules.warp.WarpManager manager = internal();
        if (manager == null) return 0;
        return manager.getRemainingCooldown(player.getUniqueId());
    }

    @Override
    public boolean hasPendingWarp(Player player) {
        net.godlycow.org.essc.modules.warp.WarpManager manager = internal();
        return manager != null && manager.hasPendingWarp(player.getUniqueId());
    }

    @Override
    public void cancelWarp(Player player) {
        net.godlycow.org.essc.modules.warp.WarpManager manager = internal();
        if (manager == null) return;
        manager.cancelWarmupTask(player.getUniqueId());
        manager.removePendingWarp(player.getUniqueId());
        manager.clearMovementTrack(player.getUniqueId());
    }

    @Override
    public void reload() {
        net.godlycow.org.essc.modules.warp.WarpManager manager = internal();
        if (manager == null) return;
        manager.reload();
    }
}