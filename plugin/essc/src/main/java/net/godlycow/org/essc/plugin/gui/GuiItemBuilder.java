package net.godlycow.org.essc.plugin.gui;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.util.ComponentHelper;
import net.godlycow.org.essc.util.SkullTextureUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class GuiItemBuilder {
    private final EssentialsC plugin;
    private final MiniMessage mm;

    public GuiItemBuilder(EssentialsC plugin) {
        this.plugin = plugin;
        this.mm = plugin.getMiniMessage();
    }

    public ItemStack build(GuiButton config, Player player) {
        if (config == null) {
            return new ItemStack(Material.AIR);
        }

        ItemStack item = new ItemStack(config.getMaterial(), config.getAmount());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        if (config.getMaterial() == Material.PLAYER_HEAD && meta instanceof SkullMeta skullMeta) {
            if (config.getSkullTexture() != null) {
                SkullTextureUtil.applyTexture(skullMeta, config.getSkullTexture(), plugin.getLogger());
            }
        }

        Component name = resolveText(config.getName(), player);
        meta.displayName(ComponentHelper.noItalic(name));

        if (!config.getLore().isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : config.getLore()) {
                lore.add(ComponentHelper.noItalic(resolveText(line, player)));
            }
            meta.lore(lore);
        }

        if (config.isHideAttributes()) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }
        if (config.isHideEnchants()) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        if (config.isGlow()) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        if (config.getCustomModelData() != null) {
            meta.setCustomModelData(config.getCustomModelData());
        }

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

    public ItemStack buildSimple(Material material, Component name, List<Component> lore, boolean hideAttributes) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.displayName(ComponentHelper.noItalic(name));
        if (lore != null && !lore.isEmpty()) {
            meta.lore(ComponentHelper.noItalic(lore));
        }
        if (hideAttributes) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }
        item.setItemMeta(meta);
        return item;
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
}