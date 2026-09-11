package net.godlycow.org.essc.migration.mapper;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.migration.model.EssHome;
import net.godlycow.org.essc.migration.model.EssLocation;
import net.godlycow.org.essc.migration.model.EssUserData;
import net.godlycow.org.essc.util.LegacyColorConverter;
import org.bukkit.Location;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserDataMapper {

    private final EssentialsC plugin;

    public UserDataMapper(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public EconomyTransfer transformEconomy(EssUserData data) {
        BigDecimal money = data.money() != null ? data.money() : BigDecimal.ZERO;
        return new EconomyTransfer(
                data.uuid(),
                data.lastAccountName() != null ? data.lastAccountName() : "Unknown",
                money
        );
    }

    public List<HomeTransfer> transformHomes(EssUserData data) {
        List<HomeTransfer> homes = new ArrayList<>();
        if (data.homes() == null || data.homes().isEmpty()) return homes;

        for (EssHome essHome : data.homes().values()) {
            if (essHome == null || essHome.location() == null) continue;

            Location loc = essHome.location().toBukkitLocation();
            if (loc == null || loc.getWorld() == null) {
                plugin.debug("Skipping home '" + essHome.name() + "' for " + data.uuid() + " - invalid world");
                continue;
            }

            homes.add(new HomeTransfer(
                    data.uuid(),
                    essHome.name() != null ? essHome.name() : "home",
                    loc
            ));
        }

        return homes;
    }

    public NicknameTransfer transformNickname(EssUserData data) {
        if (data.nickname() == null || data.nickname().isEmpty()) return null;

        String miniMessageNick = LegacyColorConverter.toMiniMessage(data.nickname());
        if (miniMessageNick == null || miniMessageNick.isEmpty()) return null;

        return new NicknameTransfer(data.uuid(), miniMessageNick);
    }

    public BackTransfer transformBackLocation(EssUserData data) {
        EssLocation backLoc = data.lastLocation() != null
                ? data.lastLocation()
                : data.logoutLocation();

        if (backLoc == null) return null;

        Location loc = backLoc.toBukkitLocation();
        if (loc == null || loc.getWorld() == null) {
            plugin.debug("Invalid back location for " + data.uuid() + " - world not found");
            return null;
        }

        return new BackTransfer(data.uuid(), loc);
    }

    public MuteTransfer transformMute(EssUserData data) {
        if (!data.muted()) return null;

        String reason = data.muteReason();
        if (reason == null || reason.isEmpty()) reason = "Migrated from EssentialsX";

        long expires = data.muteTimeout();
        if (expires > 0 && expires < System.currentTimeMillis()) {
            plugin.debug("Skipping expired mute for " + data.lastAccountName());
            return null;
        }

        return new MuteTransfer(
                data.uuid(),
                data.lastAccountName() != null ? data.lastAccountName() : "Unknown",
                reason,
                "EssentialsX",
                expires
        );
    }

    public record EconomyTransfer(UUID uuid, String username, BigDecimal balance) {}
    public record HomeTransfer(UUID owner, String name, Location location) {}
    public record NicknameTransfer(UUID uuid, String nickname) {}
    public record BackTransfer(UUID uuid, Location location) {}
    public record MuteTransfer(UUID uuid, String name, String reason, String muter, long expires) {}
    public record BanTransfer(UUID uuid, String name, String reason, String banner, long expires) {}
}