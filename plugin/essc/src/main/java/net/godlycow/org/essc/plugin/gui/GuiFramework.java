package net.godlycow.org.essc.plugin.gui;

import net.godlycow.org.essc.EssentialsC;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class GuiFramework {
    private final EssentialsC plugin;
    private final Map<String, GuiTemplate> templates = new HashMap<>();
    private final GuiItemBuilder itemBuilder;

    public GuiFramework(EssentialsC plugin) {
        this.plugin = plugin;
        this.itemBuilder = new GuiItemBuilder(plugin);
    }

    public void loadTemplates() {
        templates.clear();
        File guiDir = new File(plugin.getDataFolder(), "guis");
        if (!guiDir.exists()) {
            guiDir.mkdirs();
        }

        extractDefaultGuis(guiDir);

        File[] files = guiDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String id = file.getName().replace(".yml", "").toLowerCase();
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            templates.put(id, new GuiTemplate(id, config));
            plugin.debug("[GUI] Loaded template: " + id);
        }
    }

    private void extractDefaultGuis(File guiDir) {
        String[] defaultGuis = {
                "auction_main.yml",
                "auction_listings.yml",
                "auction_expired.yml",
                "auction_history_type.yml",
                "auction_sell_history.yml",
                "auction_buy_history.yml",
                "shop_main.yml",
                "shop_category.yml",
                "trash.yml",
                "kit_list.yml",
                "kit_preview.yml"
        };

        for (String guiName : defaultGuis) {
            File targetFile = new File(guiDir, guiName);
            if (!targetFile.exists()) {
                String resourcePath = "guis/" + guiName;
                try (InputStream resource = plugin.getResource(resourcePath)) {
                    if (resource != null) {
                        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                                new InputStreamReader(resource, StandardCharsets.UTF_8)
                        );
                        defaultConfig.save(targetFile);
                        plugin.debug("[GUI] Extracted default GUI config: " + guiName);
                    } else {
                        plugin.getLogger().warning("[GUI] Default GUI config not found in resources: " + resourcePath);
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("[GUI] Failed to extract " + guiName + ": " + e.getMessage());
                }
            }
        }
    }

    public GuiTemplate getTemplate(String id) {
        return templates.get(id != null ? id.toLowerCase() : null);
    }

    public boolean hasTemplate(String id) {
        return templates.containsKey(id != null ? id.toLowerCase() : null);
    }

    public GuiItemBuilder getItemBuilder() {
        return itemBuilder;
    }

    public void fillStaticItems(Inventory inv, String templateId, Player player) {
        GuiTemplate template = getTemplate(templateId);
        if (template == null) return;

        for (GuiButton button : template.getItems().values()) {
            if (button.isDynamic()) continue;
            ItemStack item = itemBuilder.build(button, player);
            for (int slot : button.getSlots()) {
                if (slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, item.clone());
                }
            }
        }
    }

    public void reload() {
        loadTemplates();
    }
}