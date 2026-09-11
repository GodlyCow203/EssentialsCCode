package net.godlycow.org.essc.modules.shop;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.util.InventoryViewCompat;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

public class ShopManager {
    private final EssentialsC plugin;
    private final ShopDatabase database;
    private final Map<String, ShopCategory> categories;
    private File shopFolder;
    private YamlConfiguration mainConfig;
    private ShopMainConfig shopMainConfig;
    private ShopListener shopListener;
    private ShopGuiManager shopGuiManager;

    public ShopManager(EssentialsC plugin) {
        this.plugin = plugin;
        this.database = new ShopDatabase(plugin);
        this.categories = new HashMap<>();

        if (!plugin.getConfigManager().isShopEnabled()) {
            plugin.debug("Shop system is disabled");
            return;
        }

        try {
            database.connect();
            loadShop();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize shop: " + e.getMessage());
        }
    }

    public void setShopListener(ShopListener listener) {
        this.shopListener = listener;
    }

    public ShopListener getShopListener() {
        return shopListener;
    }

    public void setShopGuiManager(ShopGuiManager shopGuiManager) {
        this.shopGuiManager = shopGuiManager;
    }

    private void loadShop() {
        shopFolder = new File(plugin.getDataFolder(), "shop");
        if (!shopFolder.exists()) {
            shopFolder.mkdirs();
        }

        saveResourceFiles();

        File mainFile = new File(shopFolder, "main.yml");
        if (!mainFile.exists()) {
            createDefaultMainFile(mainFile);
        }
        mainConfig = YamlConfiguration.loadConfiguration(mainFile);
        shopMainConfig = new ShopMainConfig(mainConfig);

        ConfigurationSection catsSection = mainConfig.getConfigurationSection("categories");
        if (catsSection != null) {
            for (String key : catsSection.getKeys(false)) {
                loadCategory(key, catsSection.getConfigurationSection(key));
            }
        }

        plugin.debug("Loaded " + categories.size() + " shop categories");
    }

    private void saveResourceFiles() {
        File shopDir = new File(plugin.getDataFolder(), "shop");
        if (!shopDir.exists()) {
            shopDir.mkdirs();
        }

        String[] files = {"main.yml", "farming.yml", "mining.yml", "spawners.yml", "enchanted_books.yml", "tools.yml", "blocks.yml", "redstone.yml", "misc.yml", "combat.yml" };

        for (String fileName : files) {
            File file = new File(shopDir, fileName);
            if (!file.exists()) {
                plugin.saveResource("shop/" + fileName, false);
            }
        }
    }

    private void loadCategory(String id, ConfigurationSection section) {
        ShopCategory category = new ShopCategory(id);
        category.setDisplayName(section.getString("name", id));
        category.setIcon(Material.valueOf(section.getString("material", "CHEST")));
        category.setTextureUrl(section.getString("texture"));
        category.setLore(section.getStringList("lore"));
        category.setSlot(section.getInt("slot", 0));
        category.setFileName(section.getString("file", id + ".yml"));
        category.setPermission(section.getString("permission"));
        category.setEnabled(section.getBoolean("enabled", true));

        File itemFile = new File(shopFolder, category.getFileName());
        if (itemFile.exists()) {
            YamlConfiguration itemConfig = YamlConfiguration.loadConfiguration(itemFile);
            loadItems(category, itemConfig);
        }

        categories.put(id, category);
    }

    private void loadItems(ShopCategory category, YamlConfiguration config) {
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) return;

        List<ShopItem> collected = new ArrayList<>();
        boolean hasExplicitLayout = false;

        for (String itemId : itemsSection.getKeys(false)) {
            ConfigurationSection itemSec = itemsSection.getConfigurationSection(itemId);
            if (itemSec == null) continue;

            ShopItem item = new ShopItem(itemId);
            item.setCategory(category.getId());
            item.setMaterial(Material.valueOf(itemSec.getString("material", "STONE")));
            item.setAmount(itemSec.getInt("amount", 1));
            item.setDisplayName(itemSec.getString("name"));
            item.setLore(itemSec.getStringList("lore"));
            item.setBuyPrice(itemSec.getDouble("buy-price", 0));
            item.setSellPrice(itemSec.getDouble("sell-price", 0));
            item.setBuyable(itemSec.getBoolean("buyable", true));
            item.setSellable(itemSec.getBoolean("sellable", true));
            item.setStock(itemSec.getInt("stock", -1));
            item.setPermission(itemSec.getString("permission"));
            item.setGlow(itemSec.getBoolean("glow", false));
            item.setTextureUrl(itemSec.getString("texture-url"));
            item.setSkullOwner(itemSec.getString("skull-owner"));
            item.setMaxStack(itemSec.getInt("max-stack", 64));

            item.setSpawner(itemSec.getBoolean("spawner", false));
            String spawnerTypeFallback = itemSec.getString("spawner-type", itemSec.getString("spawnerType", "PIG"));
            item.setSpawnerType(spawnerTypeFallback);

            item.setEnchantedBook(itemSec.getBoolean("enchanted-book", false));
            ConfigurationSection storedEnchants = itemSec.getConfigurationSection("enchanted-book-enchants");
            if (storedEnchants != null) {
                for (String enchKey : storedEnchants.getKeys(false)) {
                    try {
                        Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(enchKey.toLowerCase()));
                        if (ench != null) {
                            item.addStoredEnchantment(ench, storedEnchants.getInt(enchKey));
                        }
                    } catch (Exception ignored) {}
                }
            }

            ConfigurationSection enchants = itemSec.getConfigurationSection("enchantments");
            if (enchants != null) {
                for (String enchKey : enchants.getKeys(false)) {
                    try {
                        Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(enchKey.toLowerCase()));
                        if (ench != null) {
                            item.addEnchantment(ench, enchants.getInt(enchKey));
                        }
                    } catch (Exception ignored) {}
                }
            }

            item.setCommands(itemSec.getStringList("commands"));

            int rawSlot = itemSec.getInt("slot", 0);
            int rawPage = itemSec.getInt("page", 1);
            item.setSlot(rawSlot);
            item.setPage(rawPage);
            if (rawSlot != 0 || rawPage != 1) {
                hasExplicitLayout = true;
            }

            collected.add(item);
        }

        if (!hasExplicitLayout && !collected.isEmpty()) {
            int itemsPerPage = shopMainConfig.getItemsPerPage();
            int[] availableSlots = buildAutoSlots(itemsPerPage);
            plugin.debug("Auto-assigning slots for category '" + category.getId()
                    + "' (" + collected.size() + " items, " + itemsPerPage + " per page)");

            for (int i = 0; i < collected.size(); i++) {
                int page      = i / itemsPerPage + 1;
                int slotIndex = i % itemsPerPage;
                int slot      = slotIndex < availableSlots.length ? availableSlots[slotIndex] : slotIndex;
                collected.get(i).setPage(page);
                collected.get(i).setSlot(slot);
            }
        }

        for (ShopItem item : collected) {
            category.addItem(item);
        }
    }

    private int[] buildAutoSlots(int itemsPerPage) {
        int capacity = Math.min(itemsPerPage, 45);
        int[] slots = new int[capacity];
        for (int i = 0; i < capacity; i++) {
            slots[i] = i;
        }
        return slots;
    }

    private void createDefaultMainFile(File file) {
        YamlConfiguration config = new YamlConfiguration();

        ConfigurationSection categories = config.createSection("categories");

        ConfigurationSection farming = categories.createSection("farming");
        farming.set("name", "<color:#06FFA5>Farming");
        farming.set("material", "WHEAT");
        farming.set("slot", 20);
        farming.set("file", "farming.yml");
        farming.set("lore", Arrays.asList(
                "<color:#AAAAAA>Buy and sell farming items",
                "",
                "<color:#FFE66D>Click to open"
        ));

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create default shop files");
        }
    }

    public void openMainShop(Player player) {
        if (!plugin.getConfigManager().isShopEnabled()) {
            player.sendMessage(plugin.getLanguageManager().get(player, "shop.disabled"));
            return;
        }

        if (shopGuiManager == null) {
            plugin.getLogger().warning("ShopGuiManager is not initialized!");
            return;
        }

        if (plugin.getEconomyManager() == null) {
            shopGuiManager.openMainShop(player, 0.0);
            if (shopListener != null) {
                shopListener.setSession(player, new ShopSession(null, 1));
            }
            return;
        }

        plugin.getEconomyManager().getBalance(player.getUniqueId()).thenAccept(balance -> {
            player.getScheduler().run(plugin, task -> {
                shopGuiManager.openMainShop(player, balance.doubleValue());
                if (shopListener != null) {
                    shopListener.setSession(player, new ShopSession(null, 1));
                }
            }, null);
        });
    }

    public void openCategory(Player player, String categoryId, int page) {
        ShopCategory category = categories.get(categoryId);
        if (category == null || !category.isEnabled()) {
            player.sendMessage(plugin.getLanguageManager().get(player, "shop.invalid-category"));
            return;
        }

        if (category.getPermission() != null && !player.hasPermission(category.getPermission())) {
            player.sendMessage(plugin.getLanguageManager().get(player, "error.no_permission"));
            return;
        }

        if (shopGuiManager == null) {
            plugin.getLogger().warning("ShopGuiManager is not initialized!");
            return;
        }

        if (plugin.getEconomyManager() == null) {
            shopGuiManager.openCategory(player, category, page, 0.0);
            if (shopListener != null) {
                shopListener.setSession(player, new ShopSession(categoryId, page));
            }
            return;
        }

        plugin.getEconomyManager().getBalance(player.getUniqueId()).thenAccept(balance -> {
            player.getScheduler().run(plugin, task -> {
                shopGuiManager.openCategory(player, category, page, balance.doubleValue());
                if (shopListener != null) {
                    shopListener.setSession(player, new ShopSession(categoryId, page));
                }
            }, null);
        });
    }

    public void processPurchase(Player player, ShopItem item, int amount) {
        if (!item.isBuyable()) {
            shopListener.getSounds().playError(player);
            player.sendMessage(plugin.getLanguageManager().get(player, "shop.not-buyable"));
            return;
        }

        if (item.getPermission() != null && !player.hasPermission(item.getPermission())) {
            shopListener.getSounds().playNoPermission(player);
            player.sendMessage(plugin.getLanguageManager().get(player, "error.no_permission"));
            return;
        }

        if (item.getStock() == 0) {
            shopListener.getSounds().playError(player);
            player.sendMessage(plugin.getLanguageManager().get(player, "shop.out-of-stock"));
            return;
        }

        if (item.getStock() > 0 && item.getStock() < amount) {
            shopListener.getSounds().playError(player);
            player.sendMessage(plugin.getLanguageManager().get(player, "shop.not-enough-stock"));
            return;
        }

        double totalPrice = item.getBuyPrice() * amount;

        ItemStack giveItem = item.createGiveItem(amount * item.getAmount());

        if (!canFitInInventory(player, giveItem)) {
            shopListener.getSounds().playInventoryFull(player);
            player.sendMessage(plugin.getLanguageManager().get(player, "shop.inventory-full"));
            return;
        }

        if (plugin.getEconomyManager() == null) {
            completePurchase(player, item, amount, totalPrice, giveItem, null);
            return;
        }

        BigDecimal price = BigDecimal.valueOf(totalPrice);

        plugin.getEconomyManager().has(player.getUniqueId(), price).thenAccept(hasEnough -> {
            if (!hasEnough) {
                player.getScheduler().run(plugin, task -> {
                    shopListener.getSounds().playInsufficientFunds(player);
                    player.sendMessage(plugin.getLanguageManager().get(player, "shop.not-enough-money"));
                }, null);
                return;
            }

            plugin.getEconomyManager().withdraw(player.getUniqueId(), price).thenAccept(success -> {
                player.getScheduler().run(plugin, task -> {
                    if (!success) {
                        shopListener.getSounds().playError(player);
                        player.sendMessage(plugin.getLanguageManager().get(player, "error.internal"));
                        return;
                    }
                    completePurchase(player, item, amount, totalPrice, giveItem, price);
                }, null);
            });
        });
    }

    private boolean canFitInInventory(Player player, ItemStack item) {
        int freeSlots = 0;
        for (ItemStack invItem : player.getInventory().getStorageContents()) {
            if (invItem == null || invItem.getType().isAir()) {
                freeSlots++;
            } else if (invItem.isSimilar(item) && invItem.getAmount() < invItem.getMaxStackSize()) {
                int space = invItem.getMaxStackSize() - invItem.getAmount();
                if (space >= item.getAmount()) return true;
            }
        }
        return freeSlots > 0;
    }

    private void completePurchase(Player player, ShopItem item, int amount, double totalPrice,
                                  ItemStack giveItem, BigDecimal price) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(giveItem);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(drop ->
                    player.getWorld().dropItemNaturally(player.getLocation(), drop));
        }

        if (item.getStock() > 0) {
            item.setStock(item.getStock() - amount);
        }

        if (plugin.getConfigManager().isShopLogTransactions()) {
            database.logPurchase(player.getUniqueId(), item.getId(), amount, totalPrice);
        }

        int totalItems = amount * item.getAmount();

        for (String cmd : item.getCommands()) {
            String formatted = cmd.replace("%player%", player.getName())
                    .replace("%amount%", String.valueOf(totalItems));
            plugin.getServer().getGlobalRegionScheduler().execute(plugin, () ->
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), formatted));
        }

        shopListener.getSounds().playPurchase(player);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("item", item.getDisplayName() != null ? item.getDisplayName() : item.getId());
        placeholders.put("amount", String.valueOf(totalItems));
        placeholders.put("price", String.valueOf(totalPrice));
        player.sendMessage(plugin.getLanguageManager().get(player, "shop.purchase-success", placeholders));

        refreshPlayerGUI(player);

        plugin.debug(player.getName() + " bought " + totalItems + "x " + item.getId() + " for " + totalPrice);
    }

    public void processSale(Player player, ShopItem item, int amount) {
        if (!item.isSellable()) {
            shopListener.getSounds().playError(player);
            player.sendMessage(plugin.getLanguageManager().get(player, "shop.not-sellable"));
            return;
        }

        ItemStack checkItem = item.createComparisonItem(1);
        int totalItemsRequired = amount * item.getAmount();

        if (!containsMatchingItem(player, checkItem, totalItemsRequired)) {
            shopListener.getSounds().playError(player);
            player.sendMessage(plugin.getLanguageManager().get(player, "shop.not-enough-items"));
            return;
        }

        double totalPrice = item.getSellPrice() * amount;

        if (plugin.getEconomyManager() != null) {
            BigDecimal price = BigDecimal.valueOf(totalPrice);

            plugin.getEconomyManager().deposit(player.getUniqueId(), price).thenAccept(deposited -> {
                player.getScheduler().run(plugin, task -> {
                    if (!deposited) {
                        shopListener.getSounds().playError(player);
                        player.sendMessage(plugin.getLanguageManager().get(player, "error.internal"));
                        return;
                    }
                    removeMatchingItems(player, checkItem, totalItemsRequired);
                    completeSale(player, item, amount, totalPrice);
                }, null);
            });
        } else {
            removeMatchingItems(player, checkItem, totalItemsRequired);
            completeSale(player, item, amount, totalPrice);
        }
    }

    private boolean containsMatchingItem(Player player, ItemStack checkItem, int amount) {
        int count = 0;
        for (ItemStack invItem : player.getInventory().getContents()) {
            if (invItem == null || invItem.getType().isAir()) continue;
            if (isItemMatching(invItem, checkItem)) {
                count += invItem.getAmount();
                if (count >= amount) return true;
            }
        }
        return false;
    }

    private void removeMatchingItems(Player player, ItemStack checkItem, int amount) {
        int toRemove = amount;
        for (ItemStack invItem : player.getInventory().getContents()) {
            if (invItem == null || invItem.getType().isAir()) continue;
            if (isItemMatching(invItem, checkItem)) {
                int remove = Math.min(invItem.getAmount(), toRemove);
                invItem.setAmount(invItem.getAmount() - remove);
                toRemove -= remove;
                if (toRemove <= 0) break;
            }
        }
    }

    private boolean isItemMatching(ItemStack playerItem, ItemStack checkItem) {
        if (playerItem.getType() != checkItem.getType()) return false;

        ItemMeta playerMeta = playerItem.getItemMeta();
        ItemMeta checkMeta = checkItem.getItemMeta();

        if (playerItem.getType() == Material.ENCHANTED_BOOK) {
            if (!(playerMeta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta playerBook) ||
                    !(checkMeta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta checkBook)) {
                return false;
            }
            return playerBook.getStoredEnchants().equals(checkBook.getStoredEnchants());
        }

        if (playerItem.getType() == Material.SPAWNER) {
            if (!(playerMeta instanceof BlockStateMeta playerBlock) ||
                    !(checkMeta instanceof BlockStateMeta checkBlock)) {
                return false;
            }
            if (!(playerBlock.getBlockState() instanceof CreatureSpawner playerSpawner) ||
                    !(checkBlock.getBlockState() instanceof CreatureSpawner checkSpawner)) {
                return false;
            }
            return playerSpawner.getSpawnedType() == checkSpawner.getSpawnedType();
        }

        if (playerMeta != null && checkMeta != null) {
            return playerMeta.getEnchants().equals(checkMeta.getEnchants());
        }
        return true;
    }

    private void completeSale(Player player, ShopItem item, int amount, double totalPrice) {
        if (plugin.getConfigManager().isShopLogTransactions()) {
            database.logSale(player.getUniqueId(), item.getId(), amount, totalPrice);
        }

        shopListener.getSounds().playSale(player);

        int totalItems = amount * item.getAmount();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("item", item.getDisplayName() != null ? item.getDisplayName() : item.getId());
        placeholders.put("amount", String.valueOf(totalItems));
        placeholders.put("price", String.valueOf(totalPrice));
        player.sendMessage(plugin.getLanguageManager().get(player, "shop.sale-success", placeholders));

        refreshPlayerGUI(player);
    }

    private void refreshPlayerGUI(Player player) {
        if (plugin.getEconomyManager() == null) {
            return;
        }

        plugin.getEconomyManager().getBalance(player.getUniqueId()).thenAccept(balance -> {
            player.getScheduler().run(plugin, task -> {
                org.bukkit.inventory.Inventory open = InventoryViewCompat.getTopInventory(player);
                if (open == null || !(open.getHolder() instanceof ShopHolder holder)) {
                    return;
                }

                String templateId = holder.isMain() ? "shop_main" : "shop_category";
                shopGuiManager.updateBalanceSlot(player, balance.doubleValue(), templateId);

                if (holder.isCategory()) {
                    ShopSession session = shopListener != null ? shopListener.getSession(player) : null;
                    if (session == null) {
                        return;
                    }
                    ShopCategory category = categories.get(session.getCategoryId());
                    if (category == null) {
                        return;
                    }
                    shopGuiManager.refreshCategoryItems(open, player, category, session.getPage(), balance.doubleValue());
                }
            }, null);
        });
    }

    public Map<String, ShopCategory> getCategories() {
        return categories;
    }

    public ShopCategory getCategory(String id) {
        return categories.get(id);
    }

    public ShopMainConfig getMainConfig() {
        return shopMainConfig;
    }

    public void reload() {
        categories.clear();

        File mainFile = new File(shopFolder, "main.yml");
        if (mainFile.exists()) {
            mainConfig = YamlConfiguration.loadConfiguration(mainFile);
            shopMainConfig = new ShopMainConfig(mainConfig);
        }

        ConfigurationSection catsSection = mainConfig.getConfigurationSection("categories");
        if (catsSection != null) {
            for (String key : catsSection.getKeys(false)) {
                loadCategory(key, catsSection.getConfigurationSection(key));
            }
        }

        if (shopGuiManager != null) {
            shopGuiManager.reload();
        }

        plugin.debug("Shop reloaded with " + categories.size() + " categories");
    }

    public void shutdown() {
        database.disconnect();
    }
}