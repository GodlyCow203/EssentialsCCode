package net.godlycow.org.essc.modules.shop.sell;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.util.InventoryViewCompat;
import net.godlycow.org.essc.modules.shop.ShopCategory;
import net.godlycow.org.essc.modules.shop.ShopItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class SellManager {
    private final EssentialsC plugin;
    private final SellListener sellListener;
    private final Map<Player, SellGUI> activeGUIs = new HashMap<>();

    public SellManager(EssentialsC plugin, SellListener sellListener) {
        this.plugin = plugin;
        this.sellListener = sellListener;
    }

    public void openSellGUI(Player player) {
        if (!plugin.getConfigManager().isSellEnabled()) {
            player.sendMessage(plugin.getLanguageManager().get(player, "sell.disabled"));
            return;
        }

        SellGUI gui = new SellGUI(plugin, this, player);
        activeGUIs.put(player, gui);
        sellListener.registerGUI(player, gui);
        gui.open();
    }


    public SellGUI getActiveGUI(Player player) {
        return activeGUIs.get(player);
    }


    public void reload() {
        for (Map.Entry<Player, SellGUI> entry : new HashMap<>(activeGUIs).entrySet()) {
            Player player = entry.getKey();
            SellGUI gui = entry.getValue();

            gui.onClose();

            if (InventoryViewCompat.getTopInventory(player)instanceof Inventory inv && inv.getHolder() instanceof SellHolder) {
                player.closeInventory();
            }

            player.sendMessage(plugin.getLanguageManager().get(player, "sell.reload-closed"));
        }

        activeGUIs.clear();

        plugin.debug("SellManager reloaded - closed all open sell GUIs");
    }

    public void unregisterGUI(Player player) {
        activeGUIs.remove(player);
    }

    public double getItemWorth(ItemStack item) {
        if (item == null || item.getType().isAir()) return 0.0;

        ShopItem shopItem = findMatchingShopItem(item);
        if (shopItem == null || !shopItem.isSellable()) return 0.0;

        return shopItem.getSellPrice() * item.getAmount();
    }

    public SellResult sellItem(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return new SellResult(0, 0, true);
        }

        ShopItem shopItem = findMatchingShopItem(item);
        if (shopItem == null || !shopItem.isSellable()) {
            return new SellResult(0, 0, false);
        }

        double totalPrice = shopItem.getSellPrice() * item.getAmount();

        if (plugin.getEconomyManager() != null) {
            plugin.getEconomyManager().deposit(player.getUniqueId(), BigDecimal.valueOf(totalPrice));
        }

        return new SellResult(item.getAmount(), totalPrice, true);
    }

    public SellResult sellHand(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            return new SellResult(0, 0, false);
        }

        SellResult result = sellItem(player, hand);
        if (result.isSuccess()) {
            player.getInventory().setItemInMainHand(null);
        }
        return result;
    }

    public SellResult sellInventory(Player player) {
        double totalWorth = 0.0;
        int totalItems = 0;

        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType().isAir()) continue;

            ShopItem shopItem = findMatchingShopItem(item);
            if (shopItem == null || !shopItem.isSellable()) continue;

            double price = shopItem.getSellPrice() * item.getAmount();
            totalWorth += price;
            totalItems += item.getAmount();
            player.getInventory().setItem(i, null);
        }

        if (totalWorth > 0 && plugin.getEconomyManager() != null) {
            plugin.getEconomyManager().deposit(player.getUniqueId(), BigDecimal.valueOf(totalWorth));
        }

        return new SellResult(totalItems, totalWorth, totalItems > 0);
    }

    public double calculateInventoryWorth(Player player) {
        double total = 0.0;
        for (ItemStack item : player.getInventory().getContents()) {
            total += getItemWorth(item);
        }
        return total;
    }

    public ShopItem findMatchingShopItem(ItemStack item) {
        if (plugin.getShopManager() == null) return null;

        Material type = item.getType();
        ItemMeta meta = item.getItemMeta();

        for (ShopCategory category : plugin.getShopManager().getCategories().values()) {
            for (int page = 1; page <= category.getMaxPage(); page++) {
                for (ShopItem shopItem : category.getPageItems(page).values()) {
                    if (shopItem.getMaterial() != type) continue;

                    if (shopItem.isEnchantedBook() && type == Material.ENCHANTED_BOOK) {
                        if (meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta bookMeta) {
                            if (bookMeta.getStoredEnchants().equals(shopItem.getStoredEnchantments())) {
                                return shopItem;
                            }
                        }
                        continue;
                    }

                    if (shopItem.isSpawner() && type == Material.SPAWNER) {
                        if (meta instanceof org.bukkit.inventory.meta.BlockStateMeta blockMeta) {
                            if (blockMeta.getBlockState() instanceof org.bukkit.block.CreatureSpawner spawner) {
                                if (spawner.getSpawnedType().name().equalsIgnoreCase(shopItem.getSpawnerType())) {
                                    return shopItem;
                                }
                            }
                        }
                        continue;
                    }

                    if (!shopItem.getEnchantments().isEmpty()) {
                        if (meta != null && meta.getEnchants().equals(shopItem.getEnchantments())) {
                            return shopItem;
                        }
                        continue;
                    }

                    if (shopItem.getEnchantments().isEmpty() && (meta == null || meta.getEnchants().isEmpty())) {
                        return shopItem;
                    }
                }
            }
        }
        return null;
    }

    public static class SellResult {
        private final int amount;
        private final double price;
        private final boolean success;

        public SellResult(int amount, double price, boolean success) {
            this.amount = amount;
            this.price = price;
            this.success = success;
        }

        public int getAmount() {
            return amount;
        }

        public double getPrice() {
            return price;
        }

        public boolean isSuccess() {
            return success;
        }
    }
}