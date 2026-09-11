package net.godlycow.org.essc.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

import java.lang.reflect.Method;

// avoids IncompatibleClassChangeError on paper 1.20.x where InventoryView is a class, not an interface :/ Thanks faststats
public final class InventoryViewCompat {

    private static Method getTopInventoryMethod;

    private InventoryViewCompat() {
    }

    // use reflection to call getTopInventory on the open inventoryView
    public static Inventory getTopInventory(Player player) {
        try {
            Object view = player.getOpenInventory();
            if (getTopInventoryMethod == null) {
                getTopInventoryMethod = view.getClass().getMethod("getTopInventory");
            }
            return (Inventory) getTopInventoryMethod.invoke(view);
        } catch (Exception e) {
            return null;
        }
    }
}
