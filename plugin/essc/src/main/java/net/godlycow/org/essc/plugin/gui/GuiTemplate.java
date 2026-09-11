package net.godlycow.org.essc.plugin.gui;

import net.godlycow.org.essc.EssentialsC;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class GuiTemplate {
    private final String id;
    private final String rawTitle;
    private final int size;
    private final LinkedHashMap<String, GuiButton> items = new LinkedHashMap<>();

    public GuiTemplate(String id, FileConfiguration config) {
        this.id = id;
        this.rawTitle = config.getString("title", "<dark_gray>GUI");
        this.size = resolveSize(config.getInt("size", 54));

        var itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                var btnSection = itemsSection.getConfigurationSection(key);
                if (btnSection != null) {
                    items.put(key, new GuiButton(key, btnSection));
                }
            }
        }
    }

    private static int resolveSize(int raw) {
        int clamped = Math.max(9, Math.min(54, raw));
        int remainder = clamped % 9;
        if (remainder == 0) {
            return clamped;
        }
        int rounded = clamped + (9 - remainder);
        return Math.min(54, rounded);
    }

    public Component resolveTitle(Player player, EssentialsC plugin, Map<String, String> placeholders) {
        String text = rawTitle;
        if (text.startsWith("lang:")) {
            String key = text.substring(5);
            return plugin.getLanguageManager().get(player, key, placeholders);
        }
        if (placeholders != null) {
            for (var entry : placeholders.entrySet()) {
                text = text.replace("<" + entry.getKey() + ">", entry.getValue());
            }
        }
        return plugin.getMiniMessage().deserialize(text);
    }

    public Component resolveTitle(Player player, EssentialsC plugin) {
        return resolveTitle(player, plugin, null);
    }

    public String getId() {
        return id;
    }

    public int getSize() {
        return size;
    }

    public Map<String, GuiButton> getItems() {
        return Collections.unmodifiableMap(items);
    }

    public GuiButton getItem(String id) {
        return items.get(id);
    }

    public boolean hasItem(String id) {
        return items.containsKey(id);
    }
}