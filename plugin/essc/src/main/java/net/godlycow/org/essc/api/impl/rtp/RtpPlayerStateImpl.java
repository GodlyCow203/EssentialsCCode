package net.godlycow.org.essc.api.impl.rtp;

import net.godlycow.org.essc.api.rtp.RtpPlayerState;

import java.util.UUID;

public class RtpPlayerStateImpl implements RtpPlayerState {
    private final UUID playerId;
    private final boolean rtpInProgress;
    private final boolean onCooldown;
    private final long remainingCooldownSeconds;
    private final long lastRtpTimestamp;
    private final int totalRtpCount;
    private final boolean pendingWarmup;

    public RtpPlayerStateImpl(UUID playerId, boolean rtpInProgress, boolean onCooldown,
                              long remainingCooldownSeconds, long lastRtpTimestamp,
                              int totalRtpCount, boolean pendingWarmup) {
        this.playerId = playerId;
        this.rtpInProgress = rtpInProgress;
        this.onCooldown = onCooldown;
        this.remainingCooldownSeconds = remainingCooldownSeconds;
        this.lastRtpTimestamp = lastRtpTimestamp;
        this.totalRtpCount = totalRtpCount;
        this.pendingWarmup = pendingWarmup;
    }

    @Override
    public UUID getPlayerId() {
        return playerId;
    }

    @Override
    public boolean isRtpInProgress() {
        return rtpInProgress;
    }

    @Override
    public boolean isOnCooldown() {
        return onCooldown;
    }

    @Override
    public long getRemainingCooldownSeconds() {
        return remainingCooldownSeconds;
    }

    @Override
    public long getLastRtpTimestamp() {
        return lastRtpTimestamp;
    }

    @Override
    public int getTotalRtpCount() {
        return totalRtpCount;
    }

    @Override
    public boolean hasPendingWarmup() {
        return pendingWarmup;
    }
}