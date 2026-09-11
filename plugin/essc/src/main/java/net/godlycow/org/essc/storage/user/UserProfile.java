package net.godlycow.org.essc.storage.user;

import org.bukkit.Location;

import java.util.Objects;
import java.util.UUID;

public class UserProfile {
    private final UUID uuid;
    private String username;
    private String lastKnownName;
    private long firstJoinTime;
    private long lastJoinTime;
    private String lastIp;
    private String logoutLocation;
    private long logoutTime;
    private String languageCode;
    private String backLocation;
    private String deathLocation;
    private boolean flyEnabled;
    private boolean vanished;
    private boolean tpaBlocked;
    private String lastReplyTarget;
    private long rtpLastUsed;
    private long spawnLastTeleport;
    private String banReason;
    private String banBanner;
    private long banTime;
    private long banExpires;
    private String muteReason;
    private String muteMuter;
    private long muteTime;
    private long muteExpires;
    private boolean muteOfflineNotification;
    private boolean scoreboardDisabled;
    private boolean rulesAccepted;
    private long createdAt;
    private long updatedAt;

    public UserProfile(UUID uuid, String username) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.username = Objects.requireNonNull(username, "username");
        this.lastKnownName = username;
        this.languageCode = null;
    }

    public static UserProfile createDefault(UUID uuid, String username, long timestamp) {
        UserProfile profile = new UserProfile(uuid, username);
        profile.firstJoinTime = timestamp;
        profile.lastJoinTime = timestamp;
        profile.createdAt = timestamp;
        profile.updatedAt = timestamp;
        return profile;
    }

    //ALOT of getters
    public UUID getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }


    public void setUsername(String username) {
        this.username = username;
    }

    public String getLastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName = lastKnownName;
    }

    public long getFirstJoinTime() {
        return firstJoinTime;
    }

    public void setFirstJoinTime(long firstJoinTime) {
        this.firstJoinTime = firstJoinTime;
    }

    public long getLastJoinTime() {
        return lastJoinTime;
    }

    public void setLastJoinTime(long lastJoinTime) {
        this.lastJoinTime = lastJoinTime;
    }

    public String getLastIp() {
        return lastIp;
    }

    public void setLastIp(String lastIp) {
        this.lastIp = lastIp;
    }

    public Location getLogoutLocation() {
        return UserUtils.parseLocation(logoutLocation);
    }

    public void setLogoutLocation(Location location) {
        this.logoutLocation = UserUtils.serializeLocation(location);
    }

    String getRawLogoutLocation() {
        return logoutLocation;
    }

    void setRawLogoutLocation(String  raw) {
        this.logoutLocation = raw;
    }

    public long getLogoutTime() {
        return logoutTime;
    }
    public void setLogoutTime(long logoutTime) {
        this.logoutTime = logoutTime;
    }

    public String getLanguageCode() {
        return languageCode;

    }

    public void setLanguageCode(String languageCode) {
         this.languageCode = languageCode;
    }

    public Location getBackLocation() {
        return UserUtils.parseLocation(backLocation);
    }

    public void setBackLocation(Location location) {
        this.backLocation = UserUtils.serializeLocation(location);
    }

    String getRawBackLocation() {
        return  backLocation;
    }

    void setRawBackLocation(String raw) {
         this.backLocation = raw;
    }

    public Location getDeathLocation() {
        return  UserUtils.parseLocation( deathLocation );
    }

    public void setDeathLocation(Location location) {
        this.deathLocation = UserUtils.serializeLocation(location);
    }

    String getRawDeathLocation() {
          return deathLocation;
    }


    void setRawDeathLocation( String raw) {
        this.deathLocation = raw;
    }

    public boolean isFlyEnabled() {
        return flyEnabled;
    }
    public void setFlyEnabled(boolean    flyEnabled) {
        this.flyEnabled = flyEnabled;
    }

    public boolean isVanished() {
        return vanished;
    }

    public void setVanished(boolean vanished) {
        this.vanished = vanished;
    }

    public boolean isTpaBlocked() {
        return tpaBlocked;
    }
    public void setTpaBlocked(boolean tpaBlocked) { this.tpaBlocked = tpaBlocked; }

    public UUID getLastReplyTarget() {
        if (lastReplyTarget == null || lastReplyTarget.isBlank())
            return null;

        try {
            return UUID.fromString(lastReplyTarget);
        }

        catch (IllegalArgumentException ex) {
            return null;
        }
    }
    String getRawLastReplyTarget() {
        return lastReplyTarget;
    }

    void setRawLastReplyTarget(String  raw) {
        this.lastReplyTarget = raw;
    }

    public long getRtpLastUsed() {
        return rtpLastUsed;
    }

    public void setRtpLastUsed(long rtpLastUsed) {
        this.rtpLastUsed = rtpLastUsed;
    }

    public long getSpawnLastTeleport() {
        return spawnLastTeleport;
    }

    public void setSpawnLastTeleport(long spawnLastTeleport) {
        this.spawnLastTeleport = spawnLastTeleport;
    }

    public String getBanReason() {
        return banReason;
    }

    public void setBanReason(String banReason) {
        this.banReason = banReason;
    }

    public String getBanBanner() {
        return banBanner;
    }

    public void setBanBanner(String banBanner) {
        this.banBanner = banBanner;
    }

    public long getBanTime() {
        return banTime;
    }

    public void setBanTime(long banTime) {
        this.banTime = banTime;
    }

    public long getBanExpires() {
        return banExpires;
    }


    public void setBanExpires(long banExpires) {
        this.banExpires = banExpires;

    }

    public String getMuteReason() {
        return muteReason;
    }

    public void setMuteReason(String muteReason) {
        this.muteReason = muteReason;
    }

    public String getMuteMuter() {
        return muteMuter;
    }

    public void setMuteMuter(String muteMuter) {
        this.muteMuter = muteMuter;
    }

    public long getMuteTime(){
        return muteTime;
    }

    public void setMuteTime( long muteTime) {
        this.muteTime = muteTime;
    }

    public long getMuteExpires() {
        return muteExpires;
    }

    public void setMuteExpires(long muteExpires) {
        this.muteExpires = muteExpires;
    }

    public boolean isMuteOfflineNotification() {
        return muteOfflineNotification;
    }

    public void setMuteOfflineNotification(boolean muteOfflineNotification) {
        this.muteOfflineNotification = muteOfflineNotification;
    }

    public boolean isScoreboardDisabled() {
        return scoreboardDisabled;
    }

    public void setScoreboardDisabled(boolean scoreboardDisabled) {
        this.scoreboardDisabled = scoreboardDisabled;
    }

    public boolean isRulesAccepted() {
        return rulesAccepted;
    }

    public void setRulesAccepted(boolean rulesAccepted) {
        this.rulesAccepted = rulesAccepted;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt =  updatedAt;
    }

    public String getStatesSummary() {
        StringBuilder sb = new StringBuilder();

        if (flyEnabled)
            sb.append("Fly ");
        if (vanished)
            sb.append("Vanished ");
        if (tpaBlocked)
            sb.append("TPA-Blocked ");
        if (scoreboardDisabled)
            sb.append("NoScoreboard ");
        if (!rulesAccepted)
            sb.append("UnreadyRules ");


        String s = sb.toString().trim();


        return s.isEmpty() ? "Normal" : s;
    }
}
