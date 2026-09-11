package net.godlycow.org.essc.modules.kit;

public class PlayerKitData {
    final long lastClaimed;
    final int claimCount;

    public PlayerKitData(long lastClaimed, int claimCount) {
        this.lastClaimed = lastClaimed;
        this.claimCount = claimCount;
    }
}