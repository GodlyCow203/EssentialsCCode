package net.godlycow.org.essc.modules.shop;

import org.bukkit.configuration.file.YamlConfiguration;

public class ShopMainConfig {

    private final int itemsPerPage;

    public ShopMainConfig(YamlConfiguration config) {
        this.itemsPerPage = config.getInt("items-per-page", 28);
    }

    public int getItemsPerPage() {
        return itemsPerPage;
    }
}