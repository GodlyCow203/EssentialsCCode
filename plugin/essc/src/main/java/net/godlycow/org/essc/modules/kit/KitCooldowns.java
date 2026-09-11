package net.godlycow.org.essc.modules.kit;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.api.impl.kit.KitImpl;
import net.godlycow.org.essc.api.kit.event.KitCooldownExpireEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class KitCooldowns {
    private final EssentialsC plugin;
    private final KitData data;
    private final Map<String, Long> lastNotified = new ConcurrentHashMap<>();

    public KitCooldowns(EssentialsC plugin, KitData data) {
        this.plugin = plugin;
        this.data = data;
    }

    public long getRemainingSeconds(Player player, Kit kit) {
        PlayerKitData claimData = data.getKitData(player.getUniqueId(), kit.getName());
        if (claimData == null || claimData.lastClaimed == 0) {
            return 0;
        }

        long cooldownEnd = claimData.lastClaimed + (kit.getCooldown() * 1000L);
        long remaining = cooldownEnd - System.currentTimeMillis();
        long secondsRemaining = Math.max(0, remaining / 1000L);

        if (secondsRemaining == 0 && kit.getCooldown() > 0) {
            String notificationKey = player.getUniqueId().toString() + ":" + kit.getName();
            Long notifiedAt = lastNotified.get(notificationKey);

            if (notifiedAt == null || notifiedAt != claimData.lastClaimed) {
                lastNotified.put(notificationKey, claimData.lastClaimed);

                KitImpl apiKit = new KitImpl(kit);
                KitCooldownExpireEvent expireEvent = new KitCooldownExpireEvent(player, apiKit, claimData.lastClaimed);
                Bukkit.getPluginManager().callEvent(expireEvent);
            }
        }

        return secondsRemaining;
    }

    public void clearNotification(Player player, Kit kit) {
        String notificationKey = player.getUniqueId().toString() + ":" + kit.getName();
        lastNotified.remove(notificationKey);
    }

    public void clearAllNotifications() {
        lastNotified.clear();
    }

    public boolean isNotificationsEnabled(UUID uuid) {
        return data.isNotificationsEnabled(uuid);
    }

    public void setNotificationsEnabled(UUID uuid, boolean enabled) {
        data.setNotificationsEnabled(uuid, enabled);
    }

    public CompletableFuture<Void> loadNotificationsEnabled(UUID uuid) {
        return data.loadNotificationsEnabled(uuid);
    }
}