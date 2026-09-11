package net.godlycow.org.essc.modules.kit.gui;

import net.godlycow.org.essc.EssentialsC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

public class KitGuiListener implements Listener {

    private final EssentialsC plugin;
    private final KitGuiManager guiManager;

    public KitGuiListener(EssentialsC plugin, KitGuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof KitGuiHolder holder)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        NamespacedKey actionKey = new NamespacedKey(plugin, "gui_action");
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);

        if (action == null) {
            NamespacedKey kitKey = new NamespacedKey(plugin, "kit_name");
            String kitName = meta.getPersistentDataContainer().get(kitKey, PersistentDataType.STRING);
            if (kitName != null) {
                guiManager.handleKitClick(player, kitName, holder.getPage());
            }
            return;
        }

        switch (action) {
            case "page_prev" -> guiManager.handlePageTurn(player, holder.getPage() - 1);
            case "page_next" -> guiManager.handlePageTurn(player, holder.getPage() + 1);
            case "kit_preview_back" -> guiManager.handlePreviewBack(player, holder.getReturnPage());
            case "kit_preview_claim" -> {
                String kitName = holder.getKitName();
                if (kitName != null) {
                    guiManager.handlePreviewClaim(player, kitName, holder.getReturnPage());
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getInventory().getHolder() instanceof KitGuiHolder) {
            guiManager.handleClose(player);
            guiManager.clearSession(player.getUniqueId());
        }
    }
}
