package net.godlycow.org.essc.modules.punishment;

import java.util.UUID;

public interface NetworkPunishmentHook {

    void onBan(UUID uuid, String name, String reason, String banner, long expires);

    void onUnban(UUID uuid);

    void onIpBan(String ip, String reason, String banner, long expires);

    void onIpUnban(String ip);

    void onMute(UUID uuid, String name, String reason, String muter, long expires);

    void onUnmute(UUID uuid);
}