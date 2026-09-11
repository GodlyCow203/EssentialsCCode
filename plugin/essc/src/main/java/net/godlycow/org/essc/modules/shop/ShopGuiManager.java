package net.godlycow.org.essc.modules.shop;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.util.InventoryViewCompat;
import net.godlycow.org.essc.plugin.gui.GuiButton;
import net.godlycow.org.essc.plugin.gui.GuiFramework;
import net.godlycow.org.essc.plugin.gui.GuiTemplate;
import net.godlycow.org.essc.util.SkullTextureUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShopGuiManager {

    private final EssentialsC plugin;
    private final GuiFramework guiFramework;
    private final ShopManager shopManager;
    private final ShopSoundManager sounds;
    private final MiniMessage mm;

    private final NamespacedKey shopItemKey;
    private final NamespacedKey shopCategoryKey;
    private final NamespacedKey shopPageKey;

    public ShopGuiManager(EssentialsC plugin, GuiFramework guiFramework, ShopManager shopManager, ShopSoundManager sounds) {
        this.plugin = plugin;
        this.guiFramework = guiFramework;
        this.shopManager = shopManager;
        this.sounds = sounds;
        this.mm = plugin.getMiniMessage();
        this.shopItemKey = new NamespacedKey(plugin, "shop_item");
        this.shopCategoryKey = new NamespacedKey(plugin, "shop_category");
        this.shopPageKey = new NamespacedKey(plugin, "shop_page");
    }

    public void openMainShop(Player player, double balance) {
        GuiTemplate template = guiFramework.getTemplate("shop_main");
        if (template == null) {
            plugin.getLogger().warning("[Shop] Missing GUI template: shop_main.yml");
            return;
        }

        sounds.playOpen(player);

        Component title = template.resolveTitle(player, plugin);
        Inventory inv = Bukkit.createInventory(new ShopHolder(ShopHolder.Type.MAIN), template.getSize(), title);

        guiFramework.fillStaticItems(inv, "shop_main", player);

        for (ShopCategory category : shopManager.getCategories().values()) {
            if (!category.isEnabled()) continue;
            if (category.getPermission() != null && !player.hasPermission(category.getPermission())) continue;
            int slot = category.getSlot();
            if (slot >= 0 && slot < inv.getSize()) {
                inv.setItem(slot, createCategoryIcon(category, player));
            }
        }

        GuiButton balanceBtn = template.getItem("balance");
        if (balanceBtn != null) {
            for (int slot : balanceBtn.getSlots()) {
                if (slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, createBalanceItem(player, balanceBtn, balance));
                }
            }
        }

        player.openInventory(inv);

        ShopListener listener = shopManager.getShopListener();
        if (listener != null) {
            listener.setSession(player, new ShopSession(null, 1));
        }
    }

    public void openCategory(Player player, ShopCategory category, int page, double balance) {
        GuiTemplate template = guiFramework.getTemplate("shop_category");
        if (template == null) {
            plugin.getLogger().warning("[Shop] Missing GUI template: shop_category.yml");
            return;
        }

        sounds.playOpen(player);

        Map<String, String> placeholders = Map.of("category", category.getDisplayName());
        Component title = template.resolveTitle(player, plugin, placeholders);
        Inventory inv = Bukkit.createInventory(
                new ShopHolder(ShopHolder.Type.CATEGORY, category.getId(), page),
                template.getSize(), title);

        guiFramework.fillStaticItems(inv, "shop_category", player);

        String currencySingular = plugin.getConfigManager().getShopCurrencySingular();
        String currencyPlural = plugin.getConfigManager().getShopCurrencyPlural();

        for (Map.Entry<Integer, ShopItem> entry : category.getPageItems(page).entrySet()) {
            ShopItem item = entry.getValue();
            if (item.getPermission() != null && !player.hasPermission(item.getPermission())) continue;
            int slot = entry.getKey();
            if (slot >= 0 && slot < inv.getSize()) {
                inv.setItem(slot, createShopItemDisplay(item, player, balance, currencySingular, currencyPlural));
            }
        }

        int maxPage = category.getMaxPage();

        GuiButton prevBtn = template.getItem("prev-page");
        if (prevBtn != null && page > 1) {
            for (int slot : prevBtn.getSlots()) {
                if (slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, createNavItem(prevBtn, page - 1, player));
                }
            }
        }

        GuiButton nextBtn = template.getItem("next-page");
        if (nextBtn != null && page < maxPage) {
            for (int slot : nextBtn.getSlots()) {
                if (slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, createNavItem(nextBtn, page + 1, player));
                }
            }
        }

        GuiButton pageBtn = template.getItem("page-indicator");
        if (pageBtn != null) {
            for (int slot : pageBtn.getSlots()) {
                if (slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, createPageIndicator(pageBtn, page, maxPage, player));
                }
            }
        }

        GuiButton backBtn = template.getItem("back");
        if (backBtn != null) {
            for (int slot : backBtn.getSlots()) {
                if (slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, buildButton(backBtn, player));
                }
            }
        }

        GuiButton balanceBtn = template.getItem("balance");
        if (balanceBtn != null) {
            for (int slot : balanceBtn.getSlots()) {
                if (slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, createBalanceItem(player, balanceBtn, balance));
                }
            }
        }

        player.openInventory(inv);

        ShopListener listener = shopManager.getShopListener();
        if (listener != null) {
            listener.setSession(player, new ShopSession(category.getId(), page));
        }
    }

    private ItemStack createCategoryIcon(ShopCategory category, Player player) {
        ItemStack item = new ItemStack(category.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(mm.deserialize(category.getDisplayName()).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        for (String line : category.getLore()) {
            lore.add(mm.deserialize(line).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(mm.deserialize("").decoration(TextDecoration.ITALIC, false));
        lore.add(plugin.getLanguageManager().get(player, "shop.gui.main.category-lore-click").decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);

        if (category.getIcon() == Material.PLAYER_HEAD && category.getTextureUrl() != null) {
            if (meta instanceof SkullMeta skullMeta) {
                SkullTextureUtil.applyTexture(skullMeta, category.getTextureUrl(), plugin.getLogger());
                skullMeta.getPersistentDataContainer().set(shopCategoryKey, PersistentDataType.STRING, category.getId());
                item.setItemMeta(skullMeta);
                return item;
            }
        }

        meta.getPersistentDataContainer().set(shopCategoryKey, PersistentDataType.STRING, category.getId());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createShopItemDisplay(ShopItem item, Player player, double balance,
                                            String currencySingular, String currencyPlural) {
        String buyCurrency = item.getBuyPrice() == 1.0 ? currencySingular : currencyPlural;
        String sellCurrency = item.getSellPrice() == 1.0 ? currencySingular : currencyPlural;

        Component buyLine = plugin.getLanguageManager().get(player, "shop.gui.item.buy-line", Map.of(
                "price", String.valueOf(item.getBuyPrice()),
                "currency", buyCurrency
        )).decoration(TextDecoration.ITALIC, false);

        Component sellLine = plugin.getLanguageManager().get(player, "shop.gui.item.sell-line", Map.of(
                "price", String.valueOf(item.getSellPrice()),
                "currency", sellCurrency
        )).decoration(TextDecoration.ITALIC, false);

        Component stockLine = plugin.getLanguageManager().get(player, "shop.gui.item.stock-line", Map.of(
                "stock", String.valueOf(item.getStock())
        )).decoration(TextDecoration.ITALIC, false);

        Component leftClick = plugin.getLanguageManager().get(player, "shop.gui.item.left-click").decoration(TextDecoration.ITALIC, false);
        Component rightClick = plugin.getLanguageManager().get(player, "shop.gui.item.right-click").decoration(TextDecoration.ITALIC, false);
        Component shiftClick = plugin.getLanguageManager().get(player, "shop.gui.item.shift-click").decoration(TextDecoration.ITALIC, false);

        ItemStack display = item.createDisplayItem(balance, buyLine, sellLine, stockLine, leftClick, rightClick, shiftClick);
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(shopItemKey, PersistentDataType.STRING, item.getId());
            display.setItemMeta(meta);
        }
        return display;
    }

    private ItemStack createBalanceItem(Player player, GuiButton config, double balance) {
        ItemStack item = new ItemStack(config.getMaterial(), config.getAmount());
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        if (config.getMaterial() == Material.PLAYER_HEAD && meta instanceof SkullMeta skullMeta) {
            if (config.getSkullTexture() != null) {
                SkullTextureUtil.applyTexture(skullMeta, config.getSkullTexture(), plugin.getLogger());
            } else {
                skullMeta.setOwnerProfile(player.getPlayerProfile());
            }
            item.setItemMeta(skullMeta);
            meta = item.getItemMeta();
        }

        String formattedBalance = String.format("%.2f", balance);
        String currency = balance == 1.0 ?
                plugin.getConfigManager().getShopCurrencySingular() :
                plugin.getConfigManager().getShopCurrencyPlural();

        meta.displayName(mm.deserialize(config.getName()).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();

        Map<String, String> balancePlaceholders = Map.of(
                "balance", formattedBalance,
                "currency", currency
        );
        lore.add(plugin.getLanguageManager().get(player, "shop.gui.main.balance-line", balancePlaceholders).decoration(TextDecoration.ITALIC, false));
        lore.add(mm.deserialize("").decoration(TextDecoration.ITALIC, false));
        lore.add(plugin.getLanguageManager().get(player, "shop.gui.main.balance-refresh-hint").decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);

        String action = config.getAction();
        if (action != null && !action.isEmpty()) {
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "gui_action"),
                    PersistentDataType.STRING,
                    action
            );
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNavItem(GuiButton config, int targetPage, Player player) {
        ItemStack item = buildButton(config, player);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(shopPageKey, PersistentDataType.INTEGER, targetPage);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPageIndicator(GuiButton config, int currentPage, int maxPage, Player player) {
        String name = config.getName()
                .replace("<current>", String.valueOf(currentPage))
                .replace("<max>", String.valueOf(maxPage));

        Material material = config.getMaterial();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(mm.deserialize(name).decoration(TextDecoration.ITALIC, false));

        if (!config.getLore().isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : config.getLore()) {
                lore.add(mm.deserialize(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
        }

        if (config.isHideAttributes()) {
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        }
        if (config.isHideEnchants()) {
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }
        if (config.isGlow()) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }
        if (config.getCustomModelData() != null) {
            meta.setCustomModelData(config.getCustomModelData());
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildButton(GuiButton config, Player player) {
        if (config.getMaterial() == Material.PLAYER_HEAD && config.getSkullTexture() != null) {
            ItemStack item = new ItemStack(Material.PLAYER_HEAD, config.getAmount());
            SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
            if (skullMeta == null) return item;
            SkullTextureUtil.applyTexture(skullMeta, config.getSkullTexture(), plugin.getLogger());

            Component name = resolveText(config.getName(), player);
            skullMeta.displayName(net.godlycow.org.essc.util.ComponentHelper.noItalic(name));

            if (!config.getLore().isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String line : config.getLore()) {
                    lore.add(net.godlycow.org.essc.util.ComponentHelper.noItalic(resolveText(line, player)));
                }
                skullMeta.lore(lore);
            }

            if (config.isHideAttributes()) {
                skullMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            }
            if (config.isHideEnchants()) {
                skullMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            if (config.isGlow()) {
                skullMeta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
                skullMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            if (config.getCustomModelData() != null) {
                skullMeta.setCustomModelData(config.getCustomModelData());
            }

            String action = config.getAction();
            if (action != null && !action.isEmpty()) {
                skullMeta.getPersistentDataContainer().set(
                        new NamespacedKey(plugin, "gui_action"),
                        PersistentDataType.STRING,
                        action
                );
            }

            item.setItemMeta(skullMeta);
            return item;
        }

        return guiFramework.getItemBuilder().build(config, player);
    }

    private Component resolveText(String text, Player player) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        if (text.startsWith("lang:")) {
            return plugin.getLanguageManager().get(player, text.substring(5));
        }
        return mm.deserialize(text);
    }

    public void updateBalanceSlot(Player player, double balance, String templateId) {
        org.bukkit.inventory.Inventory open = InventoryViewCompat.getTopInventory(player);
        if (open == null || !(open.getHolder() instanceof ShopHolder)) {
            return;
        }

        GuiTemplate template = guiFramework.getTemplate(templateId);
        if (template == null) {
            return;
        }

        GuiButton balanceBtn = template.getItem("balance");
        if (balanceBtn == null) {
            return;
        }

        ItemStack balanceItem = createBalanceItem(player, balanceBtn, balance);
        for (int slot : balanceBtn.getSlots()) {
            if (slot >= 0 && slot < open.getSize()) {
                open.setItem(slot, balanceItem);
            }
        }
    }

    public void refreshCategoryItems(org.bukkit.inventory.Inventory inv, Player player,
                                     ShopCategory category, int page, double balance) {
        String currencySingular = plugin.getConfigManager().getShopCurrencySingular();
        String currencyPlural = plugin.getConfigManager().getShopCurrencyPlural();

        for (Map.Entry<Integer, ShopItem> entry : category.getPageItems(page).entrySet()) {
            ShopItem item = entry.getValue();
            if (item.getPermission() != null && !player.hasPermission(item.getPermission())) {
                continue;
            }
            int slot = entry.getKey();
            if (slot >= 0 && slot < inv.getSize()) {
                inv.setItem(slot, createShopItemDisplay(item, player, balance, currencySingular, currencyPlural));
            }
        }
    }

    public void reload() {
        guiFramework.reload();
    }
}