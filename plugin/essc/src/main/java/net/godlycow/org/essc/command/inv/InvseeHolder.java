package net.godlycow.org.essc.command.inv;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class InvseeHolder implements InventoryHolder {

    private final UUID targetUuid;
    private final String targetName;
    private final boolean offline;
    private Player liveTarget;
    private Inventory inventory;

    public InvseeHolder(UUID targetUuid, String targetName, boolean offline) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.offline = offline;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public boolean isOffline() {
        return offline;
    }

    public Player getLiveTarget() {
        return liveTarget;
    }

    public void setLiveTarget(Player liveTarget) {
        this.liveTarget = liveTarget;
    }
}