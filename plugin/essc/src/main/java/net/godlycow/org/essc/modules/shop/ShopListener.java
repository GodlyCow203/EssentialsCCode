package net.godlycow.org.essc.modules.shop;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.util.InventoryViewCompat;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShopListener implements Listener {
    private final EssentialsC plugin;
    private final ShopManager shopManager;
    private final ShopSoundManager sounds;
    private final Map<UUID, ShopSession> sessions = new HashMap<>();

    private final NamespacedKey guiActionKey;
    private final NamespacedKey shopItemKey;
    private final NamespacedKey shopCategoryKey;
    private final NamespacedKey shopPageKey;
    private final NamespacedKey spawnerTypeKey;

    public ShopListener(EssentialsC plugin, ShopManager shopManager, ShopSoundManager sounds) {
        this.plugin = plugin;
        this.shopManager = shopManager;
        this.sounds = sounds;
        this.guiActionKey = new NamespacedKey(plugin, "gui_action");
        this.shopItemKey = new NamespacedKey(plugin, "shop_item");
        this.shopCategoryKey = new NamespacedKey(plugin, "shop_category");
        this.shopPageKey = new NamespacedKey(plugin, "shop_page");
        this.spawnerTypeKey = new NamespacedKey(plugin, "essc_spawner_type");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSpawnerPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        if (event.getBlock().getType() != Material.SPAWNER) return;

        ItemStack hand = event.getItemInHand();
        if (hand.getType() != Material.SPAWNER) return;

        ItemMeta handMeta = hand.getItemMeta();
        if (handMeta == null) return;

        String entityTypeName = handMeta.getPersistentDataContainer().get(spawnerTypeKey, PersistentDataType.STRING);
        if (entityTypeName == null) return;

        EntityType entityType;
        try {
            entityType = EntityType.valueOf(entityTypeName);
        } catch (IllegalArgumentException ignored) {
            return;
        }

        plugin.getServer().getRegionScheduler().run(plugin, event.getBlock().getLocation(), task -> {
            org.bukkit.block.BlockState state = event.getBlock().getState();
            if (state instanceof CreatureSpawner cs) {
                cs.setSpawnedType(entityType);
                cs.update(true, false);
            }
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof ShopHolder shopHolder)) return;

        if (event.getClickedInventory() != event.getInventory()) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        sounds.playClick(player);

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String guiAction = pdc.get(guiActionKey, PersistentDataType.STRING);
        if (guiAction != null) {
            handleGuiAction(player, guiAction, shopHolder);
            return;
        }

        Integer targetPage = pdc.get(shopPageKey, PersistentDataType.INTEGER);
        if (targetPage != null) {
            String categoryId = shopHolder.getCategoryId();
            if (categoryId != null) {
                sounds.playPageTurn(player);
                setSession(player, new ShopSession(categoryId, targetPage));
                shopManager.openCategory(player, categoryId, targetPage);
            }
            return;
        }

        String categoryId = pdc.get(shopCategoryKey, PersistentDataType.STRING);
        if (categoryId != null) {
            setSession(player, new ShopSession(categoryId, 1));
            shopManager.openCategory(player, categoryId, 1);
            return;
        }

        String itemId = pdc.get(shopItemKey, PersistentDataType.STRING);
        if (itemId != null && shopHolder.isCategory()) {
            handleShopItemClick(player, shopHolder, itemId, event);
        }
    }

    private void handleGuiAction(Player player, String action, ShopHolder holder) {
        switch (action) {
            case "close" -> {
                sounds.playClose(player);
                player.closeInventory();
                removeSession(player);
            }
            case "back" -> {
                sounds.playClose(player);
                removeSession(player);
                shopManager.openMainShop(player);
            }
            case "balance" -> {
                sounds.playClick(player);
                ShopSession session = getSession(player);
                if (session != null && session.getCategoryId() != null) {
                    shopManager.openCategory(player, session.getCategoryId(), holder.getPage());
                } else {
                    shopManager.openMainShop(player);
                }
            }
        }
    }

    private void handleShopItemClick(Player player, ShopHolder holder, String itemId, InventoryClickEvent event) {
        String categoryId = holder.getCategoryId();
        int page = holder.getPage();

        ShopCategory category = shopManager.getCategory(categoryId);
        if (category == null) {
            removeSession(player);
            shopManager.openMainShop(player);
            return;
        }

        ShopItem item = category.getPageItems(page).get(event.getSlot());
        if (item == null || !item.getId().equals(itemId)) return;

        if (item.getPermission() != null && !player.hasPermission(item.getPermission())) {
            sounds.playNoPermission(player);
            player.sendMessage(plugin.getLanguageManager().get(player, "error.no_permission"));
            return;
        }

        if (event.isLeftClick()) {
            int amount = event.isShiftClick() ? Math.min(64, item.getMaxStack()) : 1;
            shopManager.processPurchase(player, item, amount);
        } else if (event.isRightClick()) {
            int amount = event.isShiftClick() ? Math.min(64, item.getMaxStack()) : 1;
            shopManager.processSale(player, item, amount);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory topInv = InventoryViewCompat.getTopInventory(player);
        if (topInv == null || !(topInv.getHolder() instanceof ShopHolder)) return;

        for (int slot : event.getRawSlots()) {
            if (slot < topInv.getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Inventory inv = event.getInventory();
        if (!(inv.getHolder() instanceof ShopHolder)) return;

        player.getScheduler().run(plugin, task -> {
            Inventory current = InventoryViewCompat.getTopInventory(player);
            if (current == null || !(current.getHolder() instanceof ShopHolder)) {
                removeSession(player);
            }
        }, null);
    }

    public ShopSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public void setSession(Player player, ShopSession session) {
        sessions.put(player.getUniqueId(), session);
    }

    public void removeSession(Player player) {
        sessions.remove(player.getUniqueId());
    }

    public ShopSoundManager getSounds() {
        return sounds;
    }


}