package net.godlycow.org.essc.plugin.gui;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GuiButton {
    private final String id;
    private final Material material;
    private final Material availableMaterial;
    private final Material unavailableMaterial;
    private final String name;
    private final List<String> lore;
    private final List<Integer> slots;
    private final boolean glow;
    private final boolean hideAttributes;
    private final boolean hideEnchants;
    private final String skullTexture;
    private final int amount;
    private final Integer customModelData;
    private final String action;
    private final boolean decoration;
    private final boolean dynamic;

    public GuiButton(String id, ConfigurationSection section) {
        this.id = id;
        this.material = parseMaterial(section.getString("material", "STONE"), id);
        this.availableMaterial = parseMaterialOrNull(section.getString("available-material", null), id);
        this.unavailableMaterial = parseMaterialOrNull(section.getString("unavailable-material", null), id);
        this.name = section.getString("name", " ");
        this.lore = section.getStringList("lore");
        this.slots = parseSlots(section);
        this.glow = section.getBoolean("glow", false);
        this.hideAttributes = section.getBoolean("hide-attributes", true);
        this.hideEnchants = section.getBoolean("hide-enchants", true);
        this.skullTexture = section.getString("skull-texture", null);
        this.amount = Math.max(1, section.getInt("amount", 1));
        this.customModelData = section.contains("model-data") ? section.getInt("model-data") : null;
        this.action = section.getString("action", null);
        this.decoration = section.getBoolean("decoration", false);
        this.dynamic = section.getBoolean("dynamic", false);
    }

    private static Material parseMaterial(String name, String buttonId) {
        if (name == null || name.isBlank()) {
            return Material.STONE;
        }
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("[EssentialsC] [GUI] Unknown material '" + name + "' for button '" + buttonId + "', defaulting to STONE.");
            return Material.STONE;
        }
    }

    private static Material parseMaterialOrNull(String name, String buttonId) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("[EssentialsC] [GUI] Unknown material '" + name + "' for button '" + buttonId + "', ignoring.");
            return null;
        }
    }

    private static List<Integer> parseSlots(ConfigurationSection section) {
        List<Integer> list = new ArrayList<>();
        if (section.contains("slot")) {
            list.add(section.getInt("slot"));
        }
        if (section.contains("slots")) {
            for (int slot : section.getIntegerList("slots")) {
                list.add(slot);
            }
        }
        return list;
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public Material getAvailableMaterial() {
        return availableMaterial;
    }

    public Material getUnavailableMaterial() {
        return unavailableMaterial;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return Collections.unmodifiableList(lore);
    }

    public List<Integer> getSlots() {
        return Collections.unmodifiableList(slots);
    }

    public boolean isGlow() {
        return glow;
    }

    public boolean isHideAttributes() {
        return hideAttributes;
    }

    public boolean isHideEnchants() {
        return hideEnchants;
    }

    public String getSkullTexture() {
        return skullTexture;
    }

    public int getAmount() {
        return amount;
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public String getAction() {
        return action;
    }

    public boolean isDecoration() {
        return decoration;
    }

    public boolean isDynamic() {
        return dynamic;
    }
}