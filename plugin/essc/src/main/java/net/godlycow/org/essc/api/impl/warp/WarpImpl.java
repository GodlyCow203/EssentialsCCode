package net.godlycow.org.essc.api.impl.warp;

import net.godlycow.org.essc.api.warp.Warp;
import org.bukkit.Location;

public class WarpImpl implements Warp {
    private final net.godlycow.org.essc.modules.warp.Warp internalWarp;

    public WarpImpl(net.godlycow.org.essc.modules.warp.Warp internalWarp) {
        this.internalWarp = internalWarp;
    }

    @Override
    public String getName() {
        return internalWarp.getName();
    }

    @Override
    public Location getLocation() {
        return internalWarp.getLocation();
    }

    @Override
    public void setLocation(Location location) {
        internalWarp.setLocation(location);
    }

    @Override
    public String getPermission() {
        return internalWarp.getPermission();
    }

    @Override
    public void setPermission(String permission) {
        internalWarp.setPermission(permission);
    }

    @Override
    public double getCost() {
        return internalWarp.getCost();
    }

    @Override
    public void setCost(double cost) {
        internalWarp.setCost(cost);
    }

    @Override
    public boolean isHidden() {
        return internalWarp.isHidden();
    }

    @Override
    public void setHidden(boolean hidden) {
        internalWarp.setHidden(hidden);
    }

    @Override
    public String getDescription() {
        return internalWarp.getDescription();
    }

    @Override
    public void setDescription(String description) {
        internalWarp.setDescription(description);
    }

    @Override
    public String getCategory() {
        return internalWarp.getCategory();
    }

    @Override
    public void setCategory(String category) {
        internalWarp.setCategory(category);
    }

    public net.godlycow.org.essc.modules.warp.Warp getInternalWarp() {
        return internalWarp;
    }
}