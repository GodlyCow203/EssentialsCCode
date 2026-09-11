package net.godlycow.org.essc.modules.kit;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface KitSyncHook {

    void onKitClaimed(UUID uuid, String kitName, long claimedAt, String serverId);

    CompletableFuture<Long> getNetworkLastClaimed(UUID uuid, String kitName);
}