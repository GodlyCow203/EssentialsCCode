package net.godlycow.org.essc.migration.mapper;

import net.godlycow.org.essc.migration.model.EssWarp;
import org.bukkit.Location;

public class WarpMapper {

    public WarpTransfer transform(EssWarp warp) {
        if (warp == null || warp.location() == null) return null;

        Location loc = warp.location().toBukkitLocation();
        if (loc == null || loc.getWorld() == null) return null;

        return new WarpTransfer(
                warp.name() != null ? warp.name() : "unknown",
                loc,
                null,
                0.0,
                false,
                "",
                "default"
        );
    }

    public record WarpTransfer(
            String name,
            Location location,
            String permission,
            double cost,
            boolean hidden,
            String description,
            String category
    ) {}
}