package net.godlycow.org.essc.migration.model;

import java.math.BigDecimal;
import java.util.Map;

import java.util.UUID;

public record EssUserData(
        UUID uuid,
        String lastAccountName,

        BigDecimal money,
        boolean acceptingPay,
        boolean baltopExempt,

        boolean teleportEnabled,
        boolean teleportAuto,
        EssLocation lastLocation,
        EssLocation logoutLocation,
        Map<String, EssHome> homes,
        long lastTeleportTime,
        boolean godMode,
        boolean muted,
        String muteReason,
        boolean jailed,
        boolean afk,
        boolean socialSpy,
        boolean npc,
        long lastHealTime,
        long muteTimeout,
        long jailTimeout,
        long onlineJailTime,
        long logoutTime,
        long loginTime,

        String ipAddress,
        boolean powertoolsEnabled,
        String nickname
) {
}