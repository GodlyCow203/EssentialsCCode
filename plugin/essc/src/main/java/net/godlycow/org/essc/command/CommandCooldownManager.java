package net.godlycow.org.essc.command;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CommandCooldownManager {

    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    public long getRemainingSeconds(UUID uuid, String commandName) {
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) {
            return 0;
        }

        Long expiresAt = playerCooldowns.get(commandName);
        if (expiresAt == null) {
            return 0;
        }

        long remaining = expiresAt - System.currentTimeMillis();
        return Math.max(0, (remaining + 999) / 1000);
    }

    public boolean isOnCooldown(UUID uuid, String commandName) {
        return getRemainingSeconds(uuid, commandName) > 0;
    }

    public void setCooldown(UUID uuid, String commandName, long durationSeconds) {
        cooldowns
                .computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(commandName, System.currentTimeMillis() + (durationSeconds * 1000L));
    }

    public void clearCooldown(UUID uuid, String commandName) {
        Map<String, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns != null) {
            playerCooldowns.remove(commandName);
        }
    }

    public void clearAllCooldowns(UUID uuid) {
        cooldowns.remove(uuid);
    }
}