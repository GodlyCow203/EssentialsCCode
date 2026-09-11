package net.godlycow.org.essc.modules.shop;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopCategory {
    private final String id;
    private String displayName;
    private Material icon;
    private String textureUrl;
    private List<String> lore;
    private int slot;
    private String fileName;
    private Map<Integer, Map<Integer, ShopItem>> pages;
    private boolean enabled;
    private String permission;

    public ShopCategory(String id) {
        this.id = id;
        this.lore = new ArrayList<>();
        this.pages = new HashMap<>();
        this.enabled = true;
        this.fileName = id + ".yml";
    }

    public void addItem(ShopItem item) {
        int page = item.getPage();
        pages.computeIfAbsent(page, k -> new HashMap<>()).put(item.getSlot(), item);
    }

    public Map<Integer, ShopItem> getPageItems(int page) {
        return pages.getOrDefault(page, new HashMap<>());
    }

    public int getMaxPage() {
        return pages.keySet().stream().max(Integer::compare).orElse(1);
    }

    public boolean hasPage(int page) {
        return pages.containsKey(page);
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Material getIcon() { return icon; }
    public void setIcon(Material icon) { this.icon = icon; }
    public String getTextureUrl() { return textureUrl; }
    public void setTextureUrl(String textureUrl) { this.textureUrl = textureUrl; }
    public List<String> getLore() { return lore; }
    public void setLore(List<String> lore) { this.lore = lore; }
    public int getSlot() { return slot; }
    public void setSlot(int slot) { this.slot = slot; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
}