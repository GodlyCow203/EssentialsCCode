package net.godlycow.org.essc.plugin.listener;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.auction.AhCommand;
import net.godlycow.org.essc.modules.auction.AhSoundManager;
import net.godlycow.org.essc.modules.auction.Auction;
import net.godlycow.org.essc.modules.auction.gui.AhGuiHolder;
import net.godlycow.org.essc.modules.auction.gui.AhItemFactory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.Optional;

public class AhListener implements Listener {
    private final EssentialsC plugin;
    private final AhCommand ahCommand;
    private final AhSoundManager soundManager;
    private final AhItemFactory itemFactory;

    public AhListener(EssentialsC plugin, AhCommand ahCommand) {
        this.plugin = plugin;
        this.ahCommand = ahCommand;
        this.soundManager = ahCommand.getSoundManager();
        this.itemFactory = ahCommand.getItemFactory();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!plugin.getConfigManager().isAHEnabled()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!(event.getInventory().getHolder() instanceof AhGuiHolder)) return;

        event.setCancelled(true);

        if (event.getClickedInventory() != event.getInventory()) {
            if (event.isShiftClick()) event.setCancelled(true);
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        Material type = clicked.getType();
        if (type.name().endsWith("_STAINED_GLASS_PANE") || type.name().endsWith("_GLASS_PANE")) {
            soundManager.playError(player);
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (container.has(new NamespacedKey(plugin, "gui_close"), PersistentDataType.BYTE)) {
            soundManager.playClose(player);
            player.closeInventory();
            return;
        }

        if (container.has(itemFactory.getPageKey(), PersistentDataType.INTEGER)) {
            int page = container.get(itemFactory.getPageKey(), PersistentDataType.INTEGER);
            String navType = container.getOrDefault(itemFactory.getNavKey(), PersistentDataType.STRING, "main");

            soundManager.playPageTurn(player);

            switch (navType) {
                case "main" -> ahCommand.openMainGui(player, page);
                case "listings" -> ahCommand.openListingsGui(player, page);
                case "sell_history" -> ahCommand.openSellHistoryGui(player, page);
                case "buy_history" -> ahCommand.openBuyHistoryGui(player, page);
                case "expired" -> ahCommand.openExpiredGui(player, page);
            }
            return;
        }

        if (container.has(new NamespacedKey(plugin, "gui_action"), PersistentDataType.STRING)) {
            String action = container.get(new NamespacedKey(plugin, "gui_action"), PersistentDataType.STRING);
            handleAction(player, action);
            return;
        }

        if (container.has(itemFactory.getClaimKey(), PersistentDataType.BYTE)) {
            handleClaim(player);
            return;
        }

        if (container.has(itemFactory.getAuctionKey(), PersistentDataType.INTEGER)) {
            int id = container.get(itemFactory.getAuctionKey(), PersistentDataType.INTEGER);
            boolean isOwn = container.has(itemFactory.getOwnKey(), PersistentDataType.BYTE);
            handleAuctionClick(player, id, isOwn, event.getClick());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getInventory().getHolder() instanceof AhGuiHolder) {
            event.setCancelled(true);
        }
    }

    private void handleAction(Player player, String action) {
        switch (action) {
            case "sell" -> {
                if (!player.hasPermission("essentialsc.ah.sell")) {
                    player.sendMessage(plugin.getLanguageManager().get(player, "error.no_permission"));
                    soundManager.playError(player);
                    return;
                }
                soundManager.playClick(player);
                player.closeInventory();
                player.sendMessage(plugin.getLanguageManager().get(player, "ah.sell_prompt"));
            }
            case "expired" -> {
                soundManager.playClick(player);
                ahCommand.openExpiredGui(player, 1);
            }
            case "listings" -> {
                soundManager.playClick(player);
                ahCommand.openListingsGui(player, 1);
            }
            case "claim_all" -> handleClaimAll(player);
            case "refresh", "back_main" -> {
                soundManager.playClick(player);
                ahCommand.openMainGui(player, 1);
            }
            case "history", "history_type", "back_history" -> {
                soundManager.playClick(player);
                ahCommand.openHistoryTypeGui(player);
            }
            case "sell_history" -> {
                soundManager.playClick(player);
                ahCommand.openSellHistoryGui(player, 1);
            }
            case "buy_history" -> {
                soundManager.playClick(player);
                ahCommand.openBuyHistoryGui(player, 1);
            }
            case "close" -> {
                soundManager.playClose(player);
                player.closeInventory();
            }
        }
    }

    private void handleClaimAll(Player player) {
        var items = plugin.getAuctionManager().getExpiredItems(player.getUniqueId());
        if (items.isEmpty()) {
            soundManager.playError(player);
            return;
        }

        boolean success = plugin.getAuctionManager().claimExpiredItems(player);
        if (success) {
            soundManager.playSuccess(player);
            player.sendMessage(plugin.getLanguageManager().get(player, "ah.claimed_all",
                    Map.of("count", String.valueOf(items.size()))));
            player.closeInventory();
        } else {
            soundManager.playError(player);
        }
    }

    private void handleClaim(Player player) {
        if (!plugin.getAuctionManager().claimExpiredItems(player)) {
            player.sendMessage(plugin.getLanguageManager().get(player, "ah.claim_failed"));
            soundManager.playError(player);
            return;
        }

        soundManager.playSuccess(player);
        player.sendMessage(plugin.getLanguageManager().get(player, "ah.claimed"));

        if (!plugin.getAuctionManager().getExpiredItems(player.getUniqueId()).isEmpty()) {
            ahCommand.openExpiredGui(player, 1);
        } else {
            player.closeInventory();
            player.sendMessage(plugin.getLanguageManager().get(player, "ah.all_claimed"));
        }
    }

    private void handleAuctionClick(Player player, int id, boolean isOwn, ClickType click) {
        Optional<Auction> opt = plugin.getAuctionManager().getAuction(id);
        if (opt.isEmpty()) {
            player.sendMessage(plugin.getLanguageManager().get(player, "ah.not_found"));
            soundManager.playError(player);
            player.closeInventory();
            return;
        }

        Auction auction = opt.get();

        if (isOwn) {
            handleCancel(player, auction, click);
        } else {
            handleBuy(player, auction);
        }
    }

    private void handleCancel(Player player, Auction auction, ClickType click) {
        if (click != ClickType.RIGHT) {
            player.sendMessage(plugin.getLanguageManager().get(player, "ah.right_click_cancel"));
            soundManager.playClick(player);
            return;
        }

        if (!player.hasPermission("essentialsc.ah.cancel")) {
            player.sendMessage(plugin.getLanguageManager().get(player, "error.no_permission"));
            soundManager.playError(player);
            return;
        }

        soundManager.playClick(player);
        player.closeInventory();

        plugin.getAuctionManager().cancelAuction(player, auction.getId()).thenAccept(success -> {
            player.getScheduler().run(plugin, scheduledTask -> {
                if (success) {
                    player.sendMessage(plugin.getLanguageManager().get(player, "ah.cancelled"));
                    soundManager.playCancel(player);
                } else {
                    player.sendMessage(plugin.getLanguageManager().get(player, "ah.cancel_failed"));
                    soundManager.playError(player);
                }
            }, null);
        });
    }

    private void handleBuy(Player player, Auction auction) {
        if (!player.hasPermission("essentialsc.ah.buy")) {
            player.sendMessage(plugin.getLanguageManager().get(player, "error.no_permission"));
            soundManager.playError(player);
            return;
        }

        if (auction.getSellerUuid().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getLanguageManager().get(player, "ah.cannot_buy_own"));
            soundManager.playError(player);
            return;
        }

        soundManager.playClick(player);
        player.closeInventory();

        plugin.getAuctionManager().buyAuction(player, auction.getId()).thenAccept(success -> {
            player.getScheduler().run(plugin, scheduledTask -> {
                if (success) {
                    player.sendMessage(plugin.getLanguageManager().get(player, "ah.purchased", Map.of(
                            "item", auction.getItem().getType().toString(),
                            "price", plugin.getEconomyManager().format(auction.getPrice())
                    )));
                    soundManager.playPurchase(player);
                } else {
                    player.sendMessage(plugin.getLanguageManager().get(player, "ah.purchase_failed"));
                    soundManager.playError(player);
                }
            }, null);
        });
    }
}