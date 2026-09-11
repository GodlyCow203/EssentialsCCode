package net.godlycow.org.essc.api.impl.rtp;

import net.godlycow.org.essc.api.rtp.RtpRequest;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RtpRequestImpl implements RtpRequest {
    private final UUID requestId;
    private final Player player;
    private final World targetWorld;
    private final long requestTimestamp;
    private final boolean warmupRequired;
    private final long warmupSeconds;

    public RtpRequestImpl(UUID requestId, Player player, World targetWorld,
                          long requestTimestamp, boolean warmupRequired, long warmupSeconds) {
        this.requestId = requestId;
        this.player = player;
        this.targetWorld = targetWorld;
        this.requestTimestamp = requestTimestamp;
        this.warmupRequired = warmupRequired;
        this.warmupSeconds = warmupSeconds;
    }

    @Override
    public UUID getRequestId() {
        return requestId;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public World getTargetWorld() {
        return targetWorld;
    }

    @Override
    public long getRequestTimestamp() {
        return requestTimestamp;
    }

    @Override
    public boolean wasWarmupRequired() {
        return warmupRequired;
    }

    @Override
    public long getWarmupSeconds() {
        return warmupSeconds;
    }
}