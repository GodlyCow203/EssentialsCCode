package net.godlycow.org.essc.storage.user;

import net.godlycow.org.essc.EssentialsC;
import org.bukkit.Location;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {
    private final EssentialsC plugin;
    private final UserStore store;
    private final ConcurrentHashMap<UUID, UserProfile> cache = new ConcurrentHashMap<>();

    public UserManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.store = new UserStore(plugin);
    }

    private <T> CompletableFuture<T> async(StoreTask<T> task) {
        return store.getDatabase().async(conn -> {
            try { return task.run(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
    }


    public CompletableFuture<UserProfile> loadProfile(UUID uuid, String username) {
        //load profile async
        return async(() -> store.findByUuid(uuid)).thenCompose(profile -> {
            long now = System.currentTimeMillis() / 1000L;
            if (profile == null) {
                UserProfile created = UserProfile.createDefault(uuid, username, now);
                return async(() -> store.save(created)).thenApply(saved -> {
                    cache.put(uuid, created);
                    return created;
                });
            }
            UserProfile cached = cache.get(uuid);
            if (cached != null) {

                profile.setVanished(cached.isVanished());
                profile.setFlyEnabled(cached.isFlyEnabled());
                profile.setTpaBlocked(cached.isTpaBlocked());
                profile.setScoreboardDisabled(cached.isScoreboardDisabled());
                profile.setRulesAccepted(cached.isRulesAccepted());
            }
            
            if (!username.equals(profile.getUsername())) {
                profile.setLastKnownName(profile.getUsername());
                profile.setUsername(username);
            }
            if (profile.getFirstJoinTime() <= 0) profile.setFirstJoinTime(now);

            //refresh login
            profile.setLastJoinTime(now);
            profile.setUpdatedAt(now);

            //refresh cache
            return async(() -> store.save(profile)).thenApply(saved -> {
                cache.put(uuid, profile);
                return profile;
            });
        });
    }

    public CompletableFuture<Boolean> saveAsync(UserProfile profile) {
        if (profile == null)
            return CompletableFuture.completedFuture(false);
        cache.put(profile.getUuid(), profile);

        return async(() -> store.save(profile));
    }

    public UserProfile getCachedProfile(UUID uuid) {
        return cache.get(uuid);
    }

    public void clearCache(UUID uuid) {
        cache.remove(uuid);
    }



    public CompletableFuture<UserProfile> findProfile(UUID uuid) {
        return async(() -> store.findByUuid(uuid));
    }

    public void shutdown() {
        cache.clear();
        store.getDatabase().disconnect();
        plugin.debug("UserManager shutdown complete.");
    }

    public void setFlyEnabled(UUID uuid, boolean enabled) {
        UserProfile profile = cache.get(uuid);
        if (profile != null) {
            profile.setFlyEnabled(enabled);
            saveAsync(profile);
        }
    }

    public boolean isFlyEnabled(UUID uuid) {
        UserProfile profile = cache.get(uuid);
        return profile != null && profile.isFlyEnabled();
    }

    public void setVanished(UUID uuid, boolean vanished) {
        UserProfile profile = cache.get(uuid);
        if (profile != null) {
            profile.setVanished(vanished);
            saveAsync(profile);
        }
    }

    public boolean isVanished(UUID uuid) {
        UserProfile profile = cache.get(uuid);

        return profile != null && profile.isVanished();
    }

    public boolean isTpaBlocked(UUID uuid) {
        UserProfile profile = cache.get(uuid);
        return profile != null && profile.isTpaBlocked();
    }

    public void setScoreboardDisabled(UUID uuid, boolean disabled) {
        UserProfile profile = cache.get(uuid);
        if (profile != null) {
            profile.setScoreboardDisabled(disabled);
            saveAsync(profile);
        }
    }

    public boolean isScoreboardDisabled(UUID uuid) {
        UserProfile profile = cache.get(uuid);
        return profile != null && profile.isScoreboardDisabled();
    }

    public boolean hasAcceptedRules(UUID uuid) {
        UserProfile profile = cache.get(uuid);
        return profile != null && profile.isRulesAccepted();
    }

    public UUID getLastReplyTarget(UUID uuid) {
        UserProfile profile = cache.get(uuid);
        return profile != null ?
                profile.getLastReplyTarget() : null;
    }



    public String getStatesSummary(UUID uuid) {
        UserProfile profile = cache.get(uuid);

        return profile != null ? profile.getStatesSummary() : "Unknown";
    }


    public CompletableFuture<Boolean> banPlayer(UUID uuid, String reason, String banner, long expires) {

        UserProfile profile = cache.get(uuid);
        if (profile == null)

            return CompletableFuture.completedFuture(false);
        profile.setBanReason(reason);
        profile.setBanBanner(banner);
        profile.setBanTime(System.currentTimeMillis() / 1000L);
        profile.setBanExpires(expires);
        profile.setUpdatedAt(System.currentTimeMillis() / 1000L);
        return saveAsync(profile);
    }

    public CompletableFuture<Boolean> unbanPlayer(UUID uuid) {
        UserProfile profile = cache.get(uuid);
        if (profile == null)
            return CompletableFuture.completedFuture(false);
        profile.setBanReason(null);
        profile.setBanBanner(null);
        profile.setBanTime(0);
        profile.setBanExpires(0);
        profile.setUpdatedAt(System.currentTimeMillis() / 1000L);

        return saveAsync(profile);
    }

    public boolean isBanned(UUID uuid) {

        UserProfile profile = cache.get(uuid);
        if (profile == null)
            return false;
        if (profile.getBanExpires() == 0)
            return true;
        return profile.getBanExpires() > System.currentTimeMillis() / 1000L;
    }

    public String getBanReason(UUID uuid) {

        UserProfile profile = cache.get(uuid);
        return profile != null ? profile.getBanReason() : null;
    }

    public CompletableFuture<Boolean> mutePlayer(UUID uuid, String reason, String muter, long expires) {
        UserProfile profile = cache.get(uuid);
        if (profile == null)
            return CompletableFuture.completedFuture(false);
        profile.setMuteReason(reason);
        profile.setMuteMuter(muter);
        profile.setMuteTime(System.currentTimeMillis() / 1000L);
        profile.setMuteExpires(expires);
        profile.setUpdatedAt(System.currentTimeMillis() / 1000L);

        return saveAsync(profile);
    }

    public CompletableFuture<Boolean> unmutePlayer(UUID uuid) {
        UserProfile profile = cache.get(uuid);
        if (profile == null)
            return CompletableFuture.completedFuture(false);
        profile.setMuteReason(null);
        profile.setMuteMuter(null);
        profile.setMuteTime(0);
        profile.setMuteExpires(0);
        profile.setUpdatedAt(System.currentTimeMillis() / 1000L);

        return saveAsync(profile);
    }

    public boolean isMuted(UUID uuid) {
        UserProfile profile = cache.get(uuid);
        if (profile == null)
            return false;
        if (profile.getMuteExpires() == 0)
            return true;
        return profile.getMuteExpires() > System.currentTimeMillis() / 1000L;
    }

    public String getMuteReason(UUID uuid) {
        UserProfile profile = cache.get(uuid);
        return profile != null ? profile.getMuteReason() :
                null;
    }

    public void setMuteOfflineNotification(UUID uuid, boolean enabled) {
        UserProfile profile = cache.get(uuid);
        if (profile != null) {
            profile.setMuteOfflineNotification(enabled);
            profile.setUpdatedAt(System.currentTimeMillis() / 1000L);
            saveAsync(profile);
        }
    }

    public boolean isMuteOfflineNotification(UUID uuid) {
        UserProfile profile = cache.get(uuid);
        return profile != null && profile.isMuteOfflineNotification();
    }

    public void setBackLocation(UUID uuid, Location location) {
        UserProfile profile = cache.get(uuid);
        if (profile != null) {
            profile.setBackLocation(location);
            profile.setUpdatedAt(System.currentTimeMillis() / 1000L);
            saveAsync(profile);
        }
    }

    public void setDeathLocation(UUID uuid, Location location) {
        UserProfile profile = cache.get(uuid);
        if (profile != null) {
            profile.setDeathLocation(location);
            profile.setUpdatedAt(System.currentTimeMillis() / 1000L);
            saveAsync(profile);
        }
    }

    public void setLogoutLocation(UUID uuid, Location location) {
        UserProfile profile = cache.get(uuid);
        if (profile != null) {
            profile.setLogoutLocation(location);
            profile.setLogoutTime(System.currentTimeMillis() / 1000L);
            profile.setUpdatedAt(System.currentTimeMillis() / 1000L);
            saveAsync(profile);
        }
    }


    public void setRtpLastUsed(UUID uuid, long timestamp) {
        UserProfile profile = cache.get(uuid);
        if (profile != null) {
            profile.setRtpLastUsed(timestamp);
            profile.setUpdatedAt(System.currentTimeMillis() / 1000L);
            saveAsync(profile);
        }
    }

    public void setSpawnLastTeleport(UUID uuid, long timestamp) {

        UserProfile profile = cache.get(uuid);

        if (profile != null) {
            profile.setSpawnLastTeleport(timestamp);
            profile.setUpdatedAt(System.currentTimeMillis() / 1000L);
            saveAsync(profile);
        }
    }

    public long getRtpLastUsed(UUID uuid) {
        UserProfile profile = cache.get(uuid);
        return profile != null ? profile.getRtpLastUsed() : 0;
    }

    public long getSpawnLastTeleport(UUID uuid) {
        UserProfile profile = cache.get(uuid);
        return profile != null ? profile.getSpawnLastTeleport() : 0;
    }

    public CompletableFuture<Boolean> recordIp(UUID uuid, String ip) {

        UserProfile cached = cache.get(uuid);
        if (cached != null)
            cached.setLastIp(ip);
        return async(() -> store.recordIp(uuid, ip));
    }

    public String getLastIp(UUID uuid) {
        UserProfile cached = cache.get(uuid);
        return cached != null ? cached.getLastIp() : null;
    }

    public CompletableFuture<Set<UUID>> getIgnoredPlayers(UUID uuid) {
        return async(() -> store.getIgnoredPlayers(uuid));
    }

    public CompletableFuture<Map<UUID, String>> getIgnoredPlayersWithNames(UUID uuid) {
        return async(() -> store.getIgnoredPlayersWithNames(uuid));
    }

    public CompletableFuture<Boolean> addIgnoredPlayer(UUID uuid, UUID ignoredUuid, String ignoredName) {
        return async(() -> store.addIgnoredPlayer(uuid, ignoredUuid, ignoredName));
    }


    public CompletableFuture<Boolean> removeIgnoredPlayer(UUID uuid, UUID ignoredUuid) {
        return async(() -> store.removeIgnoredPlayer(uuid, ignoredUuid));
    }

    public CompletableFuture<Boolean> saveInventory(UUID uuid, String base64) {
        return async(() -> store.saveInventory(uuid, base64));
    }

    public CompletableFuture<String> loadInventory(UUID uuid) {
        return async(() -> store.loadInventory(uuid));
    }

    public CompletableFuture<Boolean> deleteInventory(UUID uuid) {
        return async(() -> store.deleteInventory(uuid));
    }


    @FunctionalInterface
    private interface StoreTask<T> {
        T run() throws Exception;
    }
}
