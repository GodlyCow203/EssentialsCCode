package net.godlycow.org.essc.integration.placeholderapi;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.shop.ShopCategory;
import net.godlycow.org.essc.modules.shop.ShopItem;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.*;

public class ShopPlaceholders {

    private final EssentialsC plugin;

    private final Map<UUID, BigDecimal> balanceCache = new HashMap<>();
    private final Map<UUID, Long> balanceCacheTimestamps = new HashMap<>();
    private static final long CACHE_DURATION_MS = 5000;

    public ShopPlaceholders(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public String onRequest(Player player, String identifier) {
        if (!identifier.startsWith("shop_")) {
            return null;
        }

        return switch (identifier.toLowerCase()) {
            case "shop_balance" -> getBalance(player);
            case "shop_balance_formatted" -> getBalanceFormatted(player);
            case "shop_currency_singular" -> getCurrencySingular();
            case "shop_currency_plural" -> getCurrencyPlural();
            case "shop_categories" -> getCategoryCount();
            case "shop_enabled" -> isShopEnabled();
            default -> {
                if (identifier.toLowerCase().startsWith("shop_category_") && identifier.toLowerCase().endsWith("_items")) {
                    String categoryId = identifier.substring("shop_category_".length(), identifier.length() - "_items".length());
                    yield getCategoryItemCount(categoryId);
                }
                if (identifier.toLowerCase().startsWith("shop_item_") && identifier.toLowerCase().endsWith("_buyprice")) {
                    String itemId = identifier.substring("shop_item_".length(), identifier.length() - "_buyprice".length());
                    yield getItemBuyPrice(itemId);
                }
                if (identifier.toLowerCase().startsWith("shop_item_") && identifier.toLowerCase().endsWith("_sellprice")) {
                    String itemId = identifier.substring("shop_item_".length(), identifier.length() - "_sellprice".length());
                    yield getItemSellPrice(itemId);
                }
                yield null;
            }
        };
    }

    private String getBalance(Player player) {
        refreshBalanceCacheIfNeeded(player);
        BigDecimal balance = balanceCache.getOrDefault(player.getUniqueId(), BigDecimal.ZERO);
        return balance.toPlainString();
    }

    private String getBalanceFormatted(Player player) {
        refreshBalanceCacheIfNeeded(player);
        BigDecimal balance = balanceCache.getOrDefault(player.getUniqueId(), BigDecimal.ZERO);
        if (plugin.getEconomyManager() != null) {
            return plugin.getEconomyManager().format(balance);
        }
        return balance.toPlainString();
    }

    private void refreshBalanceCacheIfNeeded(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastUpdate = balanceCacheTimestamps.get(uuid);

        if (lastUpdate == null || (now - lastUpdate) > CACHE_DURATION_MS) {
            balanceCacheTimestamps.put(uuid, now);

            if (plugin.getEconomyManager() != null) {
                plugin.getEconomyManager().getBalance(uuid).thenAccept(balance -> {
                    balanceCache.put(uuid, balance);
                    balanceCacheTimestamps.put(uuid, System.currentTimeMillis());
                });
            } else {
                balanceCache.put(uuid, BigDecimal.ZERO);
            }
        }
    }
    private String getCurrencySingular() {
        return plugin.getConfigManager().getShopCurrencySingular();
    }

    private String getCurrencyPlural() {
        return plugin.getConfigManager().getShopCurrencyPlural();
    }

    private String getCategoryCount() {
        if (plugin.getShopManager() != null) {
            return String.valueOf(plugin.getShopManager().getCategories().size());
        }
        return "0";
    }

    private String getCategoryItemCount(String categoryId) {
        if (plugin.getShopManager() == null) return "0";

        ShopCategory category = plugin.getShopManager().getCategory(categoryId);
        if (category == null) return "0";

        int count = 0;
        for (int page = 1; page <= category.getMaxPage(); page++) {
            count += category.getPageItems(page).size();
        }
        return String.valueOf(count);
    }

    private String getItemBuyPrice(String itemId) {
        if (plugin.getShopManager() == null) return "0";

        for (ShopCategory category : plugin.getShopManager().getCategories().values()) {
            for (int page = 1; page <= category.getMaxPage(); page++) {
                for (ShopItem item : category.getPageItems(page).values()) {
                    if (item.getId().equalsIgnoreCase(itemId)) {
                        return String.valueOf(item.getBuyPrice());
                    }
                }
            }
        }
        return "0";
    }

    private String getItemSellPrice(String itemId) {
        if (plugin.getShopManager() == null) return "0";

        for (ShopCategory category : plugin.getShopManager().getCategories().values()) {
            for (int page = 1; page <= category.getMaxPage(); page++) {
                for (ShopItem item : category.getPageItems(page).values()) {
                    if (item.getId().equalsIgnoreCase(itemId)) {
                        return String.valueOf(item.getSellPrice());
                    }
                }
            }
        }
        return "0";
    }

    private String isShopEnabled() {
        return String.valueOf(plugin.getConfigManager().isShopEnabled());
    }

    public void clearCache(UUID uuid) {
        balanceCache.remove(uuid);
        balanceCacheTimestamps.remove(uuid);
    }

    public static List<String> getPlaceholderList() {
        List<String> list = new ArrayList<>();
        list.add("%essc_shop_balance% - Returns player's raw balance");
        list.add("%essc_shop_balance_formatted% - Returns player's formatted balance with currency");
        list.add("%essc_shop_currency_singular% - Returns the singular currency name (from shop.currency.singular)");
        list.add("%essc_shop_currency_plural% - Returns the plural currency name (from shop.currency.plural)");
        list.add("%essc_shop_categories% - Returns the number of shop categories");
        list.add("%essc_shop_enabled% - Returns 'true' if shop is enabled, 'false' otherwise");
        list.add("%essc_shop_category_<id>_items% - Returns item count in specified category");
        list.add("%essc_shop_item_<id>_buyprice% - Returns buy price of specified item");
        list.add("%essc_shop_item_<id>_sellprice% - Returns sell price of specified item");
        return list;
    }
}