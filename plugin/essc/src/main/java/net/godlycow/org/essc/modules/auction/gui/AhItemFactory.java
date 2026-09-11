package net.godlycow.org.essc.modules.auction.gui;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.auction.Auction;
import net.godlycow.org.essc.modules.auction.BuyHistoryEntry;
import net.godlycow.org.essc.modules.auction.SellHistoryEntry;
import net.godlycow.org.essc.plugin.gui.GuiButton;
import net.godlycow.org.essc.plugin.gui.GuiItemBuilder;
import net.godlycow.org.essc.util.ComponentHelper;
import net.godlycow.org.essc.util.SkullTextureUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AhItemFactory {
    private final EssentialsC plugin;
    private final GuiItemBuilder guiItemBuilder;
    private final MiniMessage mm;

    private final NamespacedKey auctionKey;
    private final NamespacedKey ownKey;
    private final NamespacedKey pageKey;
    private final NamespacedKey navKey;
    private final NamespacedKey claimKey;

    public AhItemFactory(EssentialsC plugin, GuiItemBuilder guiItemBuilder) {
        this.plugin = plugin;
        this.guiItemBuilder = guiItemBuilder;
        this.mm = plugin.getMiniMessage();
        this.auctionKey = new NamespacedKey(plugin, "ah_id");
        this.ownKey = new NamespacedKey(plugin, "ah_own");
        this.pageKey = new NamespacedKey(plugin, "ah_page");
        this.navKey = new NamespacedKey(plugin, "ah_nav");
        this.claimKey = new NamespacedKey(plugin, "ah_claim");
    }

    public ItemStack createAuctionItem(Auction auction, Player viewer) {
        ItemStack display = auction.getItem().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        List<Component> lore = createBaseLore(viewer);

        String priceStr = formatAmount(auction.getPrice());
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.lore.price", Map.of("price", priceStr))));
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.lore.seller", Map.of("seller", auction.getSellerName()))));
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.lore.time_left", Map.of("time", formatTime(auction.getTimeRemaining())))));
        lore.add(Component.empty());
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.separator")));

        boolean isOwn = auction.getSellerUuid().equals(viewer.getUniqueId());
        if (isOwn) {
            lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.lore.your_auction")));
            lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.lore.right_click_cancel")));
        } else {
            lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.lore.click_purchase")));
        }

        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.lore.id", Map.of("id", String.valueOf(auction.getId())))));

        meta.lore(lore);
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(auctionKey, PersistentDataType.INTEGER, auction.getId());
        if (isOwn) container.set(ownKey, PersistentDataType.BYTE, (byte) 1);

        display.setItemMeta(meta);
        return display;
    }

    public ItemStack createOwnAuctionItem(Auction auction, Player viewer) {
        ItemStack display = auction.getItem().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        List<Component> lore = createBaseLore(viewer);

        String priceStr = formatAmount(auction.getPrice());
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.lore.price", Map.of("price", priceStr))));
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.lore.time_left", Map.of("time", formatTime(auction.getTimeRemaining())))));
        lore.add(Component.empty());
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.separator")));
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.lore.right_click_cancel")));
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.lore.id", Map.of("id", String.valueOf(auction.getId())))));

        meta.lore(lore);
        meta.getPersistentDataContainer().set(auctionKey, PersistentDataType.INTEGER, auction.getId());
        meta.getPersistentDataContainer().set(ownKey, PersistentDataType.BYTE, (byte) 1);
        display.setItemMeta(meta);
        return display;
    }


    public ItemStack createSellHistoryItem(SellHistoryEntry entry, Player viewer) {
        ItemStack display = entry.getItem().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        List<Component> lore = createBaseLore(viewer);
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.lore.sold_for", Map.of("price", formatAmount(entry.getPrice())))));
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.lore.buyer", Map.of("buyer", entry.getBuyerName()))));
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.lore.when", Map.of("time", formatTimeAgo(entry.getTimestamp())))));
        lore.add(Component.empty());
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.separator")));

        meta.lore(lore);
        display.setItemMeta(meta);
        return display;
    }

    public ItemStack createBuyHistoryItem(BuyHistoryEntry entry, Player viewer) {
        ItemStack display = entry.getItem().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        List<Component> lore = createBaseLore(viewer);
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.lore.bought_for", Map.of("price", formatAmount(entry.getPrice())))));
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.lore.seller_name", Map.of("seller", entry.getSellerName()))));
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.lore.when", Map.of("time", formatTimeAgo(entry.getTimestamp())))));
        lore.add(Component.empty());
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.separator")));

        meta.lore(lore);
        display.setItemMeta(meta);
        return display;
    }


    public ItemStack createClaimableItem(ItemStack original, int slot, Player viewer) {
        ItemStack item = original.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.separator")));
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.lore.click_claim")));
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.lore.slot", Map.of("slot", String.valueOf(slot)))));

        meta.lore(lore);
        meta.getPersistentDataContainer().set(claimKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createExpiredButtonItem(Player player, GuiButton config) {
        ItemStack item = guiItemBuilder.build(config, player);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        boolean hasExpired = plugin.getAuctionManager().hasExpiredItems(player.getUniqueId());
        String loreKey = hasExpired ? "ah.gui.item.expired.lore_waiting" : "ah.gui.item.expired.lore_empty";

        List<Component> lore = new ArrayList<>();
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(player, "ah.gui.item.expired.lore1")));
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(player, loreKey)));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createNavItem(GuiButton config, int targetPage, String navType, Player viewer) {
        ItemStack item = config != null
                ? guiItemBuilder.build(config, viewer)
                : new ItemStack(Material.ARROW);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (config == null) {
            meta.displayName(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.item.nav.name")));
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(pageKey, PersistentDataType.INTEGER, targetPage);
        container.set(navKey, PersistentDataType.STRING, navType);

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createFiller(GuiButton config, Player viewer) {
        if (config != null) return guiItemBuilder.build(config, viewer);
        return guiItemBuilder.buildSimple(Material.GRAY_STAINED_GLASS_PANE, Component.space(), List.of(), true);
    }

    public ItemStack createInfoItem(Player player, GuiButton config) {
        Material material = (config != null) ? config.getMaterial() : Material.PLAYER_HEAD;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (material == Material.PLAYER_HEAD && meta instanceof SkullMeta skullMeta) {
            if (config != null && config.getSkullTexture() != null) {
                SkullTextureUtil.applyTexture(skullMeta, config.getSkullTexture(), plugin.getLogger());
            } else {
                skullMeta.setOwningPlayer(player);
            }
        }

        if (config != null && config.getName() != null && !config.getName().isEmpty()) {
            meta.displayName(ComponentHelper.noItalic(guiItemBuilder.build(config, player).getItemMeta().displayName()));
        } else {
            meta.displayName(ComponentHelper.noItalic(plugin.getLanguageManager().get(player, "ah.gui.item.info.name")));
        }

        int active = plugin.getAuctionManager().getPlayerAuctions(player.getUniqueId()).size();
        int max = plugin.getConfigManager().getAHMaxAuctions();
        boolean bypass = player.hasPermission("essentialsc.ah.bypass.limit");
        boolean hasExpired = plugin.getAuctionManager().hasExpiredItems(player.getUniqueId());

        List<Component> lore = new ArrayList<>();
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(player, "ah.gui.separator_top")));
        lore.add(Component.empty());

        String maxStr = bypass ? "∞" : String.valueOf(max);
        String color = (active >= max && !bypass) ? "<color:#EF4444>" : "<color:#10B981>";
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(player, "ah.gui.item.info.lore.active",
                Map.of("count", String.valueOf(active), "max", maxStr, "color", color))));
        if (hasExpired) {
            lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(player, "ah.gui.item.info.lore.expired")));
        }
        lore.add(Component.empty());
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(player, "ah.gui.item.info.lore.help_sell")));
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(player, "ah.gui.item.info.lore.help_cancel")));
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(player, "ah.gui.item.info.lore.help_expired")));
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(player, "ah.gui.item.info.lore.help_listings")));
        lore.add(Component.empty());
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(player, "ah.gui.separator")));

        meta.lore(lore);

        if (config != null) {
            if (config.isGlow()) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            if (config.isHideAttributes()) meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            if (config.getCustomModelData() != null) meta.setCustomModelData(config.getCustomModelData());
        }

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createStatsItem(int count, Player viewer, GuiButton config) {
        ItemStack item;
        if (config != null) {
            item = guiItemBuilder.build(config, viewer);
        } else {
            item = new ItemStack(Material.CHEST);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (config == null) {
            meta.displayName(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.item.stats.name")));
        }

        List<Component> lore = new ArrayList<>();
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.item.stats.lore.items_waiting", Map.of("count", String.valueOf(count)))));
        lore.add(Component.empty());
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.item.stats.lore.click_individual")));
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.item.stats.lore.claim_all")));
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.separator")));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createSellHistoryStatsItem(List<SellHistoryEntry> history, Player viewer, GuiButton config) {
        ItemStack item = config != null ? guiItemBuilder.build(config, viewer) : new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (config == null) {
            meta.displayName(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.item.sell_stats.name")));
        }

        BigDecimal totalEarnings = history.stream()
                .map(SellHistoryEntry::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Component> lore = new ArrayList<>();
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.item.sell_stats.lore.total_sales", Map.of("count", String.valueOf(history.size())))));
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.item.sell_stats.lore.total_earnings", Map.of("amount", formatAmount(totalEarnings)))));
        lore.add(Component.empty());
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.separator")));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createBuyHistoryStatsItem(List<BuyHistoryEntry> history, Player viewer, GuiButton config) {
        ItemStack item = config != null ? guiItemBuilder.build(config, viewer) : new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (config == null) {
            meta.displayName(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.item.buy_stats.name")));
        }

        BigDecimal totalSpent = history.stream()
                .map(BuyHistoryEntry::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Component> lore = new ArrayList<>();
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.item.buy_stats.lore.total_purchases", Map.of("count", String.valueOf(history.size())))));
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.item.buy_stats.lore.total_spent", Map.of("amount", formatAmount(totalSpent)))));
        lore.add(Component.empty());
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.separator")));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createListingsInfoItem(int totalItems, Player player, GuiButton config) {
        Material material = (config != null) ? config.getMaterial() : Material.PAPER;
        ItemStack item = new ItemStack(material);

        if (material == Material.PLAYER_HEAD && config != null && config.getSkullTexture() != null) {
            SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
            SkullTextureUtil.applyTexture(skullMeta, config.getSkullTexture(), plugin.getLogger());
            item.setItemMeta(skullMeta);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (config != null && config.getName() != null && !config.getName().isEmpty()) {
            meta.displayName(ComponentHelper.noItalic(guiItemBuilder.build(config, player).getItemMeta().displayName()));
        } else {
            meta.displayName(ComponentHelper.noItalic(plugin.getLanguageManager().get(player, "ah.gui.item.listings_info.name")));
        }

        List<Component> lore = new ArrayList<>();
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(player, "ah.gui.separator")));
        lore.add(Component.empty());
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(player, "ah.gui.item.listings_info.lore.total", Map.of("count", String.valueOf(totalItems)))));
        lore.add(Component.empty());
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(player, "ah.gui.item.listings_info.lore.cancel_tip")));
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(player, "ah.gui.separator")));

        meta.lore(lore);

        if (config != null) {
            if (config.isGlow()) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            if (config.isHideAttributes()) meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            if (config.getCustomModelData() != null) meta.setCustomModelData(config.getCustomModelData());
        }

        item.setItemMeta(meta);
        return item;
    }

    private List<Component> createBaseLore(Player viewer) {
        List<Component> lore = new ArrayList<>();
        lore.add(ComponentHelper.noItalic(plugin.getLanguageManager().get(viewer, "ah.gui.separator_top")));
        lore.add(Component.empty());
        return lore;
    }

    private String formatAmount(BigDecimal amount) {
        if (plugin.getEconomyManager() != null) {
            return plugin.getEconomyManager().format(amount);
        }
        Economy vaultEconomy = Bukkit.getServicesManager().load(Economy.class);
        if (vaultEconomy != null) {
            return vaultEconomy.format(amount.doubleValue());
        }
        return amount.toPlainString();
    }

    private String formatTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long mins = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        String time = hours > 0 ? hours + "h " + mins + "m" : mins + "m";
        String color = hours < 1 ? "<color:#EF4444>" : "<color:#D1D5DB>";
        return color + time;
    }

    private String formatTimeAgo(long timestamp) {
        long days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - timestamp);
        long hours = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - timestamp) % 24;
        return days > 0 ? days + "d " + hours + "h ago" : hours + "h ago";
    }

    public NamespacedKey getAuctionKey() {
        return auctionKey;
    }

    public NamespacedKey getOwnKey() {
        return ownKey;
    }

    public NamespacedKey getPageKey() {
        return pageKey;
    }

    public NamespacedKey getNavKey() {
        return navKey;
    }

    public NamespacedKey getClaimKey() {
        return claimKey;
    }
}