package net.godlycow.org.essc.modules.kit.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class KitGuiHolder implements InventoryHolder {

    private final int page;
    private final int returnPage;
    private final String kitName;
    private Inventory inventory;

    public KitGuiHolder(int page) {
        this.page = page;
        this.returnPage = page;
        this.kitName = null;
    }

    public KitGuiHolder(int page, int returnPage, String kitName) {
        this.page = page;
        this.returnPage = returnPage;
        this.kitName = kitName;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public int getPage() {
        return page;
    }

    public int getReturnPage() {
        return returnPage;
    }

    public String getKitName() {
        return kitName;
    }
}