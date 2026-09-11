package net.godlycow.org.essc.api.impl.kit;

import net.godlycow.org.essc.api.kit.KitClaimProfile;
import java.util.UUID;

public class KitClaimProfileImpl implements KitClaimProfile {
    private final UUID playerId;
    private final String kitName;
    private final long lastClaimedTimestamp;
    private final int totalClaimCount;
    private final boolean hasEverClaimed;

    public KitClaimProfileImpl(UUID playerId, String kitName, long lastClaimedTimestamp, int totalClaimCount, boolean hasEverClaimed) {
        this.playerId = playerId;
        this.kitName = kitName;
        this.lastClaimedTimestamp = lastClaimedTimestamp;
        this.totalClaimCount = totalClaimCount;
        this.hasEverClaimed = hasEverClaimed;
    }

    @Override
    public UUID getPlayerId() {
        return playerId;
    }

    @Override
    public String getKitName() {
        return kitName;
    }

    @Override
    public long getLastClaimedTimestamp() {
        return lastClaimedTimestamp;
    }

    @Override
    public int getTotalClaimCount() {
        return totalClaimCount;
    }

    @Override
    public boolean hasEverClaimed() {
        return hasEverClaimed;
    }
}