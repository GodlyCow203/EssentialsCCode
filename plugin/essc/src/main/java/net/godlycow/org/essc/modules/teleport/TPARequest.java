package net.godlycow.org.essc.modules.teleport;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TPARequest {
    public enum Type { TPA, TPAHERE }

    private final UUID requester;
    private final UUID target;
    private final Type type;
    private final long timestamp;
    private final Location requesterLocation;
    private boolean expired = false;

    public TPARequest(Player requester, Player target, Type type) {
        this.requester = requester.getUniqueId();
        this.target = target.getUniqueId();
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.requesterLocation = requester.getLocation().clone();
    }

    public UUID getRequester() {
        return requester;
    }
    public UUID getTarget() {
        return target;
    }
    public Type getType() {
        return type;
    }
    public long getTimestamp() {
        return timestamp;
    }
    public boolean isExpired() {
        return expired;
    }
    public void setExpired(boolean expired) {
        this.expired = expired;
    }
    public boolean isExpired(long timeoutMillis) {
        return System.currentTimeMillis() - timestamp > timeoutMillis;
    }
}