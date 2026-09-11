package net.godlycow.org.essc.api.impl.kit;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.api.kit.Kit;
import net.godlycow.org.essc.api.kit.KitClaimProfile;
import net.godlycow.org.essc.api.kit.KitManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class KitManagerImpl implements KitManager {
    private final EssentialsC plugin;

    public KitManagerImpl(EssentialsC plugin) {
        this.plugin = plugin;
    }

    @Override
    public Collection<Kit> getLoadedKits() {
        List<Kit> result = new ArrayList<>();
        for (var internalKit : plugin.getKitManager().getKits()) {
            Kit apiKit = new KitImpl(internalKit);
            result.add(apiKit);
        }
        return result;
    }

    @Override
    public Kit findKitByName(String name) {
        var internal = plugin.getKitManager().getKit(name);
        if (internal == null) {
            return null;
        }
        Kit apiKit = new KitImpl(internal);
        return apiKit;
    }

    @Override
    public Collection<Kit> getKitsAvailableTo(Player player) {
        List<Kit> result = new ArrayList<>();
        for (var internalKit : plugin.getKitManager().getKits()) {
            Kit apiKit = new KitImpl(internalKit);
            boolean allowed = isClaimAllowedFor(player, apiKit);
            if (allowed) {
                result.add(apiKit);
            }
        }
        return result;
    }

    @Override
    public boolean hasCooldownExpiredFor(Player player, Kit kit) {
        long remaining = getRemainingCooldownSeconds(player, kit);
        boolean expired = remaining <= 0;
        return expired;
    }

    @Override
    public CompletableFuture<Long> fetchCooldownRemainingAsync(Player player, Kit kit) {
        var impl = (KitImpl) kit;
        CompletableFuture<Long> future = plugin.getKitManager().getCooldownRemainingAsync(player, impl.getInternalKit());
        return future.thenApply(remaining -> remaining);
    }

    @Override
    public long getRemainingCooldownSeconds(Player player, Kit kit) {
        var impl = (KitImpl) kit;
        long remaining = plugin.getKitManager().getCooldownRemaining(player, impl.getInternalKit());
        return remaining;
    }

    @Override
    public boolean hasPlayerClaimed(Player player, Kit kit) {
        var impl = (KitImpl) kit;
        boolean claimed = plugin.getKitManager().hasClaimed(player, impl.getInternalKit());
        return claimed;
    }

    @Override
    public int getPlayerClaimCount(Player player, Kit kit) {
        var impl = (KitImpl) kit;
        int count = plugin.getKitManager().getClaimCount(player, impl.getInternalKit());
        return count;
    }

    @Override
    public KitClaimProfile fetchClaimProfile(Player player, Kit kit) {
        var impl = (KitImpl) kit;
        var internal = impl.getInternalKit();
        boolean claimed = plugin.getKitManager().hasClaimed(player, internal);
        int count = plugin.getKitManager().getClaimCount(player, internal);
        long lastClaimed = plugin.getKitManager().getLastClaimedTimestamp(player, internal);
        String kitName = internal.getName();
        java.util.UUID playerId = player.getUniqueId();

        return new KitClaimProfileImpl(playerId, kitName, lastClaimed, count, claimed);
    }

    @Override
    public boolean isClaimAllowedFor(Player player, Kit kit) {
        var impl = (KitImpl) kit;
        boolean result = plugin.getKitManager().canClaim(player, impl.getInternalKit());
        return result;
    }

    @Override
    public boolean isPermittedToUse(Player player, Kit kit) {
        var impl = (KitImpl) kit;
        boolean result = plugin.getKitManager().hasPermission(player, impl.getInternalKit());
        return result;
    }

    @Override
    public void reloadKitDefinitions() {
        plugin.getKitManager().reload();
    }

    @Override
    public CompletableFuture<Void> claimKitForPlayer(Player player, Kit kit) {
        var impl = (KitImpl) kit;
        CompletableFuture<Void> future = new CompletableFuture<>();

        player.getScheduler().run(plugin, task -> {
            try {
                plugin.getKitManager().giveKit(player, impl.getInternalKit());
                future.complete(null);
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        }, null);

        return future;
    }

    @Override
    public int getTotalLoadedKitCount() {
        int count = plugin.getKitManager().getKits().size();
        return count;
    }

    @Override
    public boolean isKitLoaded(String name) {
        var internal = plugin.getKitManager().getKit(name);
        boolean loaded = internal != null;
        return loaded;
    }
}