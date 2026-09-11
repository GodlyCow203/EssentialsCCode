package net.godlycow.org.essc.modules.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ShopHolder implements InventoryHolder {

    public enum Type {
        MAIN,
        CATEGORY
    }

    private final Type type;
    private final String categoryId;
    private final int page;

    public ShopHolder(Type type, String categoryId, int page) {
        this.type = type;
        this.categoryId = categoryId;
        this.page = page;
    }

    public ShopHolder(Type type) {
        this(type, null, 1);
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public Type getType() {
        return type;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public int getPage() {
        return page;
    }

    public boolean isMain() {
        return type == Type.MAIN;
    }

    public boolean isCategory() {
        return type == Type.CATEGORY;
    }
}