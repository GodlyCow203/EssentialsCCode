package net.godlycow.org.essc.modules;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReplyManager {
    private final Map<UUID, UUID> replyTargets = new HashMap<>();

    public void setReplyTarget(UUID player, UUID target) {
        if (target == null) {
            replyTargets.remove(player);
        } else {
            replyTargets.put(player, target);
        }
    }

    public UUID getReplyTarget(UUID player) {
        return replyTargets.get(player);
    }

    public void removeReplyTarget(UUID player) {
        replyTargets.remove(player);
    }
}