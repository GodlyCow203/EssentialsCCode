package net.godlycow.org.essc.modules.warp;

import org.bukkit.Location;

public class Warp {
    private final String name;
    private Location location;
    private String permission;
    private double cost;
    private boolean hidden;
    private String description;
    private String category;

    public Warp(String name, Location location) {
        this.name = name;
        this.location = location;
        this.permission = null;
        this.cost = 0.0;
        this.hidden = false;
        this.description = "";
        this.category = "default";
    }

    public String getName() {
        return name;
    }
    public Location getLocation() {
        return location;
    }
    public void setLocation(Location location) {
        this.location = location;
    }
    public String getPermission() {
        return permission;
    }
    public void setPermission(String permission) {
        this.permission = permission;
    }
    public double getCost() {
        return cost;
    }
    public void setCost(double cost) {
        this.cost = cost;
    }
    public boolean isHidden() {
        return hidden;
    }
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }
}