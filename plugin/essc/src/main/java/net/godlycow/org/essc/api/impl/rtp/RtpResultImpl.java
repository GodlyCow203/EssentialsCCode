package net.godlycow.org.essc.api.impl.rtp;

import net.godlycow.org.essc.api.rtp.RtpResult;
import org.bukkit.Location;
import org.bukkit.World;

public class RtpResultImpl implements RtpResult {
    private final boolean successful;
    private final Location destination;
    private final World world;
    private final String failureReason;
    private final long requestTimestamp;
    private final long completionTimestamp;
    private final int searchAttempts;

    public RtpResultImpl(boolean successful, Location destination, World world,
                         String failureReason, long requestTimestamp,
                         long completionTimestamp, int searchAttempts) {
        this.successful = successful;
        this.destination = destination != null ? destination.clone() : null;
        this.world = world;
        this.failureReason = failureReason;
        this.requestTimestamp = requestTimestamp;
        this.completionTimestamp = completionTimestamp;
        this.searchAttempts = searchAttempts;
    }

    @Override
    public boolean wasSuccessful() {
        return successful;
    }

    @Override
    public Location getDestination() {
        return destination != null ? destination.clone() : null;
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public String getFailureReason() {
        return failureReason;
    }

    @Override
    public long getRequestTimestamp() {
        return requestTimestamp;
    }

    @Override
    public long getCompletionTimestamp() {
        return completionTimestamp;
    }

    @Override
    public int getSearchAttempts() {
        return searchAttempts;
    }
}