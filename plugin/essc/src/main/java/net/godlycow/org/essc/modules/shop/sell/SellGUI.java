package net.godlycow.org.essc.modules.shop.sell;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.util.ComponentHelper;
import net.godlycow.org.essc.util.FormatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellGUI {
    private final EssentialsC plugin;
    private final SellManager sellManager;
    private final Player player;
    private final MiniMessage mm;
    private Inventory inv;
    private boolean processed = false;

    private static final int CONFIRM_SLOT = 47;
    private static final int CANCEL_SLOT = 51;
    private static final int[] INPUT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };
    private static final int[] BORDER_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 17,
            18, 26,
            27, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44,
            45, 46, 48, 49, 50, 52, 53
    };

    public SellGUI(EssentialsC plugin, SellManager sellManager, Player player) {
        this.plugin = plugin;
        this.sellManager = sellManager;
        this.player = player;
        this.mm = MiniMessage.miniMessage();
    }

    public void open() {
        String title = plugin.getConfigManager().getSellGUITitle();
        int size = plugin.getConfigManager().getSellGUISize();

        inv = Bukkit.createInventory(new SellHolder(), size, mm.deserialize(title));

        fillBorders();
        fillEmptySlots();
        updateButtons();

        player.openInventory(inv);

        if (plugin.getConfigManager().isSellGUISounds()) {
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
        }
    }

    private void fillBorders() {
        Material borderMaterial;
        try {
            borderMaterial = Material.valueOf(plugin.getConfigManager().getSellGUIBorderMaterial().toUpperCase());
        } catch (Exception e) {
            borderMaterial = Material.GRAY_STAINED_GLASS_PANE;
        }

        ItemStack border = new ItemStack(borderMaterial);
        ItemMeta meta = border.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            border.setItemMeta(meta);
        }

        for (int slot : BORDER_SLOTS) {
            inv.setItem(slot, border);
        }
    }

    private void fillEmptySlots() {
        if (!plugin.getConfigManager().isSellGUIFillEmpty()) {
            return;
        }

        Material fillMaterial;
        try {
            fillMaterial = Material.valueOf(plugin.getConfigManager().getSellGUIFillMaterial().toUpperCase());
        } catch (Exception e) {
            fillMaterial = Material.BLACK_STAINED_GLASS_PANE;
        }

        ItemStack filler = new ItemStack(fillMaterial);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            filler.setItemMeta(meta);
        }

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null && !isInputSlot(i) && !isConfirmSlot(i) && !isCancelSlot(i)) {
                inv.setItem(i, filler);
            }
        }
    }

    public void updateButtons() {
        double totalWorth = calculateTotalWorth();
        boolean hasItems = totalWorth > 0;

        ItemStack confirmButton = new ItemStack(hasItems ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta confirmMeta = confirmButton.getItemMeta();

        if (hasItems) {
            confirmMeta.displayName(ComponentHelper.noItalic(plugin.getLanguageManager().get(player, "sell.gui.confirm.name")));
            String currency = totalWorth == 1.0 ?
                    plugin.getConfigManager().getShopCurrencySingular() :
                    plugin.getConfigManager().getShopCurrencyPlural();

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("amount", String.valueOf(countItems()));
            placeholders.put("worth", FormatUtil.formatNumberWithDecimals(totalWorth));
            placeholders.put("currency", currency);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(plugin.getLanguageManager().get(player, "sell.gui.confirm.lore.items", placeholders));
            lore.add(plugin.getLanguageManager().get(player, "sell.gui.confirm.lore.worth", placeholders));
            lore.add(Component.empty());
            lore.add(plugin.getLanguageManager().get(player, "sell.gui.confirm.lore.action"));
            confirmMeta.lore(ComponentHelper.noItalic(lore));
        } else {
            confirmMeta.displayName(ComponentHelper.noItalic(plugin.getLanguageManager().get(player, "sell.gui.confirm.name")));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(plugin.getLanguageManager().get(player, "sell.gui.confirm.lore.empty"));
            confirmMeta.lore(ComponentHelper.noItalic(lore));
        }

        confirmButton.setItemMeta(confirmMeta);
        inv.setItem(CONFIRM_SLOT, confirmButton);

        ItemStack cancelButton = new ItemStack(Material.RED_DYE);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        cancelMeta.displayName(ComponentHelper.noItalic(plugin.getLanguageManager().get(player, "sell.gui.cancel.name")));

        List<Component> cancelLore = new ArrayList<>();
        cancelLore.add(Component.empty());
        cancelLore.add(plugin.getLanguageManager().get(player, "sell.gui.cancel.lore.action"));
        cancelMeta.lore(ComponentHelper.noItalic(cancelLore));

        cancelButton.setItemMeta(cancelMeta);
        inv.setItem(CANCEL_SLOT, cancelButton);
    }

    public double calculateTotalWorth() {
        double total = 0.0;
        for (int slot : INPUT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                total += sellManager.getItemWorth(item);
            }
        }
        return total;
    }

    public int countItems() {
        int count = 0;
        for (int slot : INPUT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                count += item.getAmount();
            }
        }
        return count;
    }

    public void processSale() {
        if (processed) return;
        processed = true;

        double totalWorth = calculateTotalWorth();

        if (totalWorth <= 0) {
            returnItems();
            if (plugin.getConfigManager().isSellGUISounds()) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
            player.sendMessage(plugin.getLanguageManager().get(player, "sell.error.no-sellable-items"));
            player.closeInventory();
            return;
        }

        int totalItems = 0;

        for (int slot : INPUT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType().isAir()) continue;

            SellManager.SellResult result = sellManager.sellItem(player, item);
            if (result.isSuccess()) {
                totalItems += result.getAmount();
                inv.setItem(slot, null);
            }
        }

        returnItems();

        if (plugin.getConfigManager().isSellGUISounds()) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", String.valueOf(totalItems));
        placeholders.put("worth", FormatUtil.formatNumberWithDecimals(totalWorth));
        placeholders.put("currency", totalWorth == 1.0 ?
                plugin.getConfigManager().getShopCurrencySingular() :
                plugin.getConfigManager().getShopCurrencyPlural());

        player.sendMessage(plugin.getLanguageManager().get(player, "sell.success.gui", placeholders));
        player.closeInventory();
    }

    public void cancel() {
        if (processed) return;
        processed = true;

        returnItems();

        if (plugin.getConfigManager().isSellGUISounds()) {
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f);
        }

        player.closeInventory();
    }

    public void onClose() {
        if (processed) return;
        processed = true;

        returnItems();
    }
    private void returnItems() {
        for (int slot : INPUT_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                player.getInventory().addItem(item).values().forEach(i ->
                        player.getWorld().dropItemNaturally(player.getLocation(), i));
            }
        }
    }

    public boolean isInputSlot(int slot) {
        for (int inputSlot : INPUT_SLOTS) {
            if (inputSlot == slot) return true;
        }
        return false;
    }

    public boolean isConfirmSlot(int slot) {
        return slot == CONFIRM_SLOT;
    }

    public boolean isCancelSlot(int slot) {
        return slot == CANCEL_SLOT;
    }

    public boolean isBorderSlot(int slot) {
        for (int borderSlot : BORDER_SLOTS) {
            if (borderSlot == slot) return true;
        }
        return false;
    }

    public Inventory getInventory() {
        return inv;
    }

    public boolean isProcessed() {
        return processed;
    }
}