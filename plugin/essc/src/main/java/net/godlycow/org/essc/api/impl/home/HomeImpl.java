package net.godlycow.org.essc.api.impl.home;

import net.godlycow.org.essc.api.home.Home;
import org.bukkit.Location;
import org.bukkit.Server;

import java.util.UUID;

public class HomeImpl implements Home {
    private final net.godlycow.org.essc.modules.home.Home internalHome;

    public HomeImpl(net.godlycow.org.essc.modules.home.Home internalHome) {
        this.internalHome = internalHome;
    }

    @Override
    public UUID getOwner() {
        return internalHome.getOwner();
    }

    @Override
    public String getName() {
        return internalHome.getName();
    }

    @Override
    public String getWorldName() {
        return internalHome.getWorld();
    }

    @Override
    public double getX() {
        return internalHome.getX();
    }

    @Override
    public double getY() {
        return internalHome.getY();
    }

    @Override
    public double getZ() {
        return internalHome.getZ();
    }

    @Override
    public float getYaw() {
        return internalHome.getYaw();
    }

    @Override
    public float getPitch() {
        return internalHome.getPitch();
    }

    @Override
    public long getCreatedAt() {
        return internalHome.getCreatedAt();
    }

    @Override
    public Location toLocation(Server server) {
        return internalHome.toLocation(server);
    }

    public net.godlycow.org.essc.modules.home.Home getInternalHome() {
        return internalHome;
    }
}
