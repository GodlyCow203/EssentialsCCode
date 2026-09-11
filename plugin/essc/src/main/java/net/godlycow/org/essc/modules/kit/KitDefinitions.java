package net.godlycow.org.essc.modules.kit;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.api.impl.kit.KitImpl;
import net.godlycow.org.essc.api.kit.event.KitLoadEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KitDefinitions {
    private final EssentialsC plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final ConcurrentHashMap<String, Kit> kits = new ConcurrentHashMap<>();
    private final File kitsFile;
    private FileConfiguration kitsConfig;

    public KitDefinitions(EssentialsC plugin) {
        this.plugin = plugin;
        this.kitsFile = new File(plugin.getDataFolder(), "kits.yml");
    }

    public void loadAll() {
        kits.clear();

        if (!kitsFile.exists()) {
            plugin.saveResource("kits.yml", false);
        }

        kitsConfig = YamlConfiguration.loadConfiguration(kitsFile);
        ConfigurationSection kitsSection = kitsConfig.getConfigurationSection("kits");

        if (kitsSection == null) {
            plugin.getLogger().warning("No kits defined in kits.yml");
            return;
        }

        for (String kitName : kitsSection.getKeys(false)) {
            try {
                ConfigurationSection kitSection = kitsSection.getConfigurationSection(kitName);
                if (kitSection == null) {
                    continue;
                }

                String displayName = kitSection.getString("display-name", kitName);
                String permission = "essentialsc.kit." + kitName.toLowerCase();
                long cooldown = kitSection.getLong("cooldown", 0);
                boolean oneTime = kitSection.getBoolean("one-time", false);
                boolean firstJoin = kitSection.getBoolean("first-join", false);
                int maxClaims = kitSection.getInt("max-claims", 0);
                String description = loadDescription( kitSection);
                boolean networkSync = kitSection.getBoolean("network-sync", false);
                int guiSlot = kitSection.getInt("gui-slot", -1);
                String guiIcon = kitSection.getString("gui-icon", null);
                int guiPage = kitSection.getInt("page", 1);

                List<ItemStack> items = new ArrayList<>();
                List<Map<?, ?>> itemsList = kitSection.getMapList("items");

                for (Map<?, ?> itemMap : itemsList) {
                    ItemStack item = loadItemFromMap(itemMap);
                    if (item != null) {
                        items.add(item);
                    }
                }

                Kit kit = new Kit(kitName.toLowerCase(), displayName, permission, cooldown,
                        oneTime, firstJoin, maxClaims, items, description, networkSync,
                        guiSlot, guiIcon, guiPage);
                kits.put(kitName.toLowerCase(), kit);

                registerPermission(permission);

                KitImpl apiKit = new KitImpl(kit);
                KitLoadEvent loadEvent = new KitLoadEvent(apiKit, kitsFile.getName());
                Bukkit.getPluginManager().callEvent(loadEvent);

                plugin.debug("Loaded kit: " + kitName + " (" + items.size() + " items)"
                        + (networkSync ? " [network-sync]" : ""));

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load kit '" + kitName + "': " + e.getMessage());
            }
        }

        plugin.debug("Loaded " + kits.size() + " kits");
    }

    private String loadDescription(ConfigurationSection section) {
        List<String> lines = section.getStringList("description");

        if (!lines.isEmpty()) {
            return String.join("\n", lines);
        }
        return section.getString("description", "");
    }

    private void registerPermission(String permission) {
        try {
            if (Bukkit.getPluginManager().getPermission(permission) == null) {
                Permission perm = new Permission(permission, "Access to kit " + permission.replace("essentialsc.kit.", ""), PermissionDefault.OP);
                Bukkit.getPluginManager().addPermission(perm);
                plugin.debug("Registered permission: " + permission);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register permission " + permission + ": " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private ItemStack loadItemFromMap(Map<?, ?> map) {
        try {
            Object typeObj = map.get("type");
            if (!(typeObj instanceof String typeName)) {
                return null;
            }

            org.bukkit.Material material = org.bukkit.Material.matchMaterial(typeName);
            if (material == null) {
                plugin.getLogger().warning("Invalid material: " + typeName);
                return null;
            }

            int amount = 1;
            Object amountObj = map.get("amount");
            if (amountObj instanceof Number n) {
                amount = n.intValue();
            }

            ItemStack item = new ItemStack(
                    material,
                    Math.min(amount, material.getMaxStackSize())
            );

            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return item;
            }

            Object nameObj = map.get("name");
            if (nameObj instanceof String name) {
                meta.displayName(mm.deserialize(name).decoration(TextDecoration.ITALIC, false));
            }

            Object loreObj = map.get("lore");
            if (loreObj instanceof List<?> loreList) {
                List<Component> loreComponents = new ArrayList<>();
                for (Object line : loreList) {
                    if (line instanceof String s) {
                        loreComponents.add(mm.deserialize(s).decoration(TextDecoration.ITALIC, false));
                    }
                }
                meta.lore(loreComponents);
            }

            Object enchObj = map.get("enchantments");
            if (enchObj instanceof Map<?, ?> enchMap) {
                EnchantmentStorageMeta bookMeta = meta instanceof EnchantmentStorageMeta storageMeta ? storageMeta : null;
                for (Map.Entry<?, ?> entry : enchMap.entrySet()) {
                    if (!(entry.getKey() instanceof String key)) {
                        continue;
                    }
                    if (!(entry.getValue() instanceof Number lvl)) {
                        continue;
                    }

                    @SuppressWarnings("deprecation") Enchantment enchant = Enchantment.getByName(key.toUpperCase());
                    if (enchant == null) {
                        continue;
                    }

                    if (bookMeta != null) {
                        bookMeta.addStoredEnchant(enchant, lvl.intValue(), true);
                    } else {
                        meta.addEnchant(enchant, lvl.intValue(), true);
                    }
                }
            }

            Object flagsObj = map.get("flags");
            if (flagsObj instanceof List<?> flags) {
                for (Object flag : flags) {
                    if (flag instanceof String s) {
                        try {
                            meta.addItemFlags(ItemFlag.valueOf(s.toUpperCase()));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }

            Object unbreakableObj = map.get("unbreakable");
            if (unbreakableObj instanceof Boolean unbreakable) {
                meta.setUnbreakable(unbreakable);
            }

            Object cmdObj = map.get("custom-model-data");
            if (cmdObj instanceof Number n) {
                meta.setCustomModelData(n.intValue());
            }

            Object nbtObj = map.get("nbt");
            if (nbtObj instanceof Map<?, ?> nbtMap) {
                PersistentDataContainer container = meta.getPersistentDataContainer();
                for (Map.Entry<?, ?> entry : nbtMap.entrySet()) {
                    if (!(entry.getKey() instanceof String key)) {
                        continue;
                    }

                    NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
                    Object value = entry.getValue();

                    if (value instanceof String s) {
                        container.set(namespacedKey, PersistentDataType.STRING, s);
                    } else if (value instanceof Integer i) {
                        container.set(namespacedKey, PersistentDataType.INTEGER, i);
                    } else if (value instanceof Double d) {
                        container.set(namespacedKey, PersistentDataType.DOUBLE, d);
                    } else if (value instanceof Number n) {
                        container.set(namespacedKey, PersistentDataType.LONG, n.longValue());
                    }
                }
            }

            Object potionTypeObj = map.get("potion-type");
            if (potionTypeObj instanceof String potionTypeStr && meta instanceof PotionMeta potionMeta) {
                PotionType potionType = PotionType.valueOf(potionTypeStr.toUpperCase());
                if (potionType != null) {
                    potionMeta.setBasePotionType(potionType);
                }
            }

            item.setItemMeta(meta);
            return item;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load item: " + e.getMessage());
            return null;
        }
    }

    public Kit getKit(String name) {
        Kit kit = kits.get(name.toLowerCase());
        return kit;
    }

    public java.util.Collection<Kit> getKits() {
        java.util.Collection<Kit> kitCollection = kits.values();
        return kitCollection;
    }

    public int getKitCount() {
        int count = kits.size();
        return count;
    }


    public void clear() {
        kits.clear();
    }

}