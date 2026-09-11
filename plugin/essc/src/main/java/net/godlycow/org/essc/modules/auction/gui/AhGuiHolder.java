package net.godlycow.org.essc.modules.auction.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class AhGuiHolder implements InventoryHolder {

    private final String guiId;
    private final int page;

    public AhGuiHolder(String guiId, int page) {
        this.guiId = guiId;
        this.page = page;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public String getGuiId() {
        return guiId;
    }

    public int getPage() {
        return page;
    }
}
