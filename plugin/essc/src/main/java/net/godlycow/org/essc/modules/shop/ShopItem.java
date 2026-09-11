package net.godlycow.org.essc.modules.shop;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.util.SkullTextureUtil;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopItem {
    private final String id;
    private Material material;
    private int amount;
    private String displayName;
    private List<String> lore;
    private Map<Enchantment, Integer> enchantments;
    private List<ItemFlag> itemFlags;
    private String textureUrl;
    private String base64Texture;
    private String skullOwner;
    private boolean glow;
    private Map<String, String> nbtData;

    private double buyPrice;
    private double sellPrice;
    private boolean buyable;
    private boolean sellable;
    private int slot;
    private int page;
    private String permission;
    private int stock;
    private int maxStack;
    private List<String> commands;
    private String category;

    private boolean spawner;
    private String spawnerType;

    private boolean enchantedBook;
    private Map<Enchantment, Integer> storedEnchantments;

    public ShopItem(String id) {
        this.id = id;
        this.lore = new ArrayList<>();
        this.enchantments = new HashMap<>();
        this.itemFlags = new ArrayList<>();
        this.nbtData = new HashMap<>();
        this.commands = new ArrayList<>();
        this.amount = 1;
        this.buyable = true;
        this.sellable = true;
        this.stock = -1;
        this.maxStack = 64;
        this.page = 1;
        this.spawner = false;
        this.spawnerType = "PIG";
        this.enchantedBook = false;
        this.storedEnchantments = new HashMap<>();
    }

    public ItemStack createItemStack() {
        Material itemMaterial = this.material;
        if (spawner && itemMaterial != Material.SPAWNER) {
            itemMaterial = Material.SPAWNER;
        }

        if (enchantedBook && itemMaterial != Material.ENCHANTED_BOOK) {
            itemMaterial = Material.ENCHANTED_BOOK;
        }

        ItemStack item = new ItemStack(itemMaterial, amount);
        ItemMeta meta = item.getItemMeta();

        if (displayName != null) {
            meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize(displayName)
                    .decoration(TextDecoration.ITALIC, false));
        }

        if (!lore.isEmpty()) {
            List<net.kyori.adventure.text.Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        .deserialize(line)
                        .decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(loreComponents);
        }

        if (glow) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        if (!nbtData.isEmpty()) {
            for (Map.Entry<String, String> entry : nbtData.entrySet()) {
                NamespacedKey key = new NamespacedKey(
                        JavaPlugin.getPlugin(EssentialsC.class),
                        entry.getKey()
                );
                meta.getPersistentDataContainer().set(
                        key,
                        PersistentDataType.STRING,
                        entry.getValue()
                );
            }
        }

        enchantments.forEach((ench, level) -> meta.addEnchant(ench, level, true));
        itemFlags.forEach(meta::addItemFlags);

        if (enchantedBook || itemMaterial == Material.ENCHANTED_BOOK) {
            if (meta instanceof EnchantmentStorageMeta bookMeta) {
                storedEnchantments.forEach((ench, level) -> {
                    bookMeta.addStoredEnchant(ench, level, true);
                });
            }
        }

        if (spawner || itemMaterial == Material.SPAWNER) {
            if (meta instanceof BlockStateMeta blockMeta) {
                if (blockMeta.getBlockState() instanceof CreatureSpawner cs) {
                    try {
                        EntityType entityType = EntityType.valueOf(spawnerType.toUpperCase());
                        cs.setSpawnedType(entityType);
                    } catch (IllegalArgumentException e) {
                        cs.setSpawnedType(EntityType.PIG);
                    }
                    blockMeta.setBlockState(cs);
                }
                blockMeta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                item.setItemMeta(blockMeta);
                return item;
            }
        }

        if (itemMaterial == Material.PLAYER_HEAD && (textureUrl != null || base64Texture != null || skullOwner != null)) {
            SkullMeta skullMeta = (SkullMeta) meta;
            if (skullOwner != null) {
                skullMeta.setOwningPlayer(org.bukkit.Bukkit.getOfflinePlayer(skullOwner));
            } else {
                String textureValue = textureUrl != null ? textureUrl : base64Texture;
                SkullTextureUtil.applyTexture(skullMeta, textureValue,
                        JavaPlugin.getPlugin(EssentialsC.class).getLogger());
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createGiveItem(int totalAmount) {
        Material itemMaterial = this.material;
        if (spawner) {
            itemMaterial = Material.SPAWNER;
        }
        if (enchantedBook) {
            itemMaterial = Material.ENCHANTED_BOOK;
        }

        ItemStack item = new ItemStack(itemMaterial, totalAmount);

        if (spawner || itemMaterial == Material.SPAWNER) {
            ItemMeta rawMeta = item.getItemMeta();
            if (rawMeta instanceof BlockStateMeta blockMeta) {
                if (blockMeta.getBlockState() instanceof CreatureSpawner cs) {
                    try {
                        cs.setSpawnedType(EntityType.valueOf(spawnerType.toUpperCase()));
                    } catch (IllegalArgumentException ignored) {
                        cs.setSpawnedType(EntityType.PIG);
                    }
                    blockMeta.setBlockState(cs);
                }
                blockMeta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                NamespacedKey spawnerKey = new NamespacedKey(
                        JavaPlugin.getPlugin(EssentialsC.class), "essc_spawner_type"
                );
                blockMeta.getPersistentDataContainer().set(
                        spawnerKey, PersistentDataType.STRING, spawnerType.toUpperCase()
                );
                item.setItemMeta(blockMeta);
            }
            return item;
        }

        if (enchantedBook || itemMaterial == Material.ENCHANTED_BOOK) {
            ItemMeta rawMeta = item.getItemMeta();
            if (rawMeta instanceof EnchantmentStorageMeta bookMeta) {
                storedEnchantments.forEach((ench, level) -> bookMeta.addStoredEnchant(ench, level, true));
                item.setItemMeta(bookMeta);
            }
            return item;
        }

        if (!enchantments.isEmpty()) {
            ItemMeta rawMeta = item.getItemMeta();
            if (rawMeta != null) {
                enchantments.forEach((ench, level) -> rawMeta.addEnchant(ench, level, true));
                item.setItemMeta(rawMeta);
            }
        }

        return item;
    }

    public ItemStack createComparisonItem(int amount) {
        Material itemMaterial = this.material;
        if (spawner && itemMaterial != Material.SPAWNER) {
            itemMaterial = Material.SPAWNER;
        }
        if (enchantedBook && itemMaterial != Material.ENCHANTED_BOOK) {
            itemMaterial = Material.ENCHANTED_BOOK;
        }

        ItemStack item = new ItemStack(itemMaterial, amount);
        ItemMeta meta = item.getItemMeta();

        if (enchantedBook || itemMaterial == Material.ENCHANTED_BOOK) {
            if (meta instanceof EnchantmentStorageMeta bookMeta) {
                storedEnchantments.forEach((ench, level) -> {
                    bookMeta.addStoredEnchant(ench, level, true);
                });
                item.setItemMeta(bookMeta);
                return item;
            }
        }

        if (spawner || itemMaterial == Material.SPAWNER) {
            if (meta instanceof BlockStateMeta blockMeta) {
                if (blockMeta.getBlockState() instanceof CreatureSpawner cs) {
                    try {
                        EntityType entityType = EntityType.valueOf(spawnerType.toUpperCase());
                        cs.setSpawnedType(entityType);
                        blockMeta.setBlockState(cs);
                    } catch (IllegalArgumentException e) {
                        cs.setSpawnedType(EntityType.PIG);
                        blockMeta.setBlockState(cs);
                    }
                    item.setItemMeta(blockMeta);
                    return item;
                }
            }
        }

        if (!enchantments.isEmpty()) {
            enchantments.forEach((ench, level) -> meta.addEnchant(ench, level, true));
            item.setItemMeta(meta);
            return item;
        }

        return item;
    }



    public ItemStack createDisplayItem(double playerBalance,
                                       net.kyori.adventure.text.Component buyLine,
                                       net.kyori.adventure.text.Component sellLine,
                                       net.kyori.adventure.text.Component stockLine,
                                       net.kyori.adventure.text.Component leftClick,
                                       net.kyori.adventure.text.Component rightClick,
                                       net.kyori.adventure.text.Component shiftClick) {
        ItemStack item = createItemStack();
        ItemMeta meta = item.getItemMeta();

        List<net.kyori.adventure.text.Component> newLore = new ArrayList<>();

        if (buyable) {
            newLore.add(buyLine);
        }

        if (sellable) {
            newLore.add(sellLine);
        }

        if (stock != -1) {
            newLore.add(stockLine);
        }

        newLore.add(net.kyori.adventure.text.Component.empty());

        if (meta.lore() != null) {
            newLore.addAll(meta.lore());
        }

        newLore.add(leftClick);
        newLore.add(rightClick);
        newLore.add(shiftClick);

        meta.lore(newLore);
        item.setItemMeta(meta);
        return item;
    }


    public String getId() {
        return id;
    }
    public Material getMaterial() {
        return material;
    }
    public void setMaterial(Material material) {
        this.material = material;
    }
    public int getAmount() {
        return amount;
    }
    public void setAmount(int amount) {
        this.amount = amount;
    }
    public String getDisplayName() {
        return displayName;
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public List<String> getLore() {
        return lore;
    }
    public void setLore(List<String> lore) {
        this.lore = lore;
    }
    public double getBuyPrice() {
        return buyPrice;
    }
    public void setBuyPrice(double buyPrice) {
        this.buyPrice = buyPrice;
    }
    public double getSellPrice() {
        return sellPrice;
    }
    public void setSellPrice(double sellPrice) {
        this.sellPrice = sellPrice;
    }
    public boolean isBuyable() {
        return buyable;
    }
    public void setBuyable(boolean buyable) {
        this.buyable = buyable;
    }
    public boolean isSellable() {
        return sellable;
    }
    public void setSellable(boolean sellable) {
        this.sellable = sellable;
    }
    public int getSlot() {
        return slot;
    }
    public void setSlot(int slot) {
        this.slot = slot;
    }
    public int getPage() {
        return page;
    }
    public void setPage(int page) {
        this.page = page;
    }
    public String getPermission() {
        return permission;
    }
    public void setPermission(String permission) {
        this.permission = permission;
    }
    public int getStock() {
        return stock;
    }
    public void setStock(int stock) {
        this.stock = stock;
    }
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public void setTextureUrl(String textureUrl) {
        this.textureUrl = textureUrl;
    }
    public void setSkullOwner(String skullOwner) {
        this.skullOwner = skullOwner;
    }
    public void setGlow(boolean glow) {
        this.glow = glow;
    }
    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }
    public void addEnchantment(Enchantment enchantment, int level) {
        this.enchantments.put(enchantment, level);
    }
    public List<String> getCommands() {
        return commands;
    }
    public void setCommands(List<String> commands) {
        this.commands = commands;
    }
    public int getMaxStack() {
        return maxStack;
    }
    public void setMaxStack(int maxStack) {
        this.maxStack = maxStack;
    }
    public boolean isSpawner() {
        return spawner;
    }
    public void setSpawner(boolean spawner) {
        this.spawner = spawner;
    }
    public String getSpawnerType() {
        return spawnerType;
    }
    public void setSpawnerType(String spawnerType) {
        this.spawnerType = spawnerType;
    }
    public boolean isEnchantedBook() {
        return enchantedBook;
    }
    public void setEnchantedBook(boolean enchantedBook) {
        this.enchantedBook = enchantedBook;
    }
    public Map<Enchantment, Integer> getStoredEnchantments() {
        return storedEnchantments;
    }
    public void addStoredEnchantment(Enchantment enchantment, int level) {
        this.storedEnchantments.put(enchantment, level);
    }
}