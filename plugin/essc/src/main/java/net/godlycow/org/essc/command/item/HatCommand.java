package net.godlycow.org.essc.command.item;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.plugin.config.EssConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HatCommand extends Command {

    private List<String> blacklist;
    private boolean blacklistEnabled;
    private boolean allowBlocks;
    private boolean requireLore;
    private boolean requireName;

    public HatCommand(EssentialsC plugin) {
        super(plugin, "hat", "essentialsc.hat", true, 0, "command.usage.hat");
        reload();
    }

    public void reload() {
        EssConfig config = plugin.getConfigManager();
        blacklist = config.getHatBlacklistItems();
        blacklistEnabled = config.isHatBlacklistEnabled();
        allowBlocks = config.isHatAllowBlocks();
        requireLore = config.isHatRequireLore();
        requireName = config.isHatRequireName();
        plugin.debug("Hat command reloaded");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        PlayerInventory inventory = player.getInventory();
        ItemStack handItem = inventory.getItemInMainHand();
        ItemStack helmetItem = inventory.getHelmet();

        if (handItem.getType().isAir()) {
            player.sendMessage(lang.get(player, "hat.no_item"));
            plugin.debug("Hat failed: " + player.getName() + " not holding item");
            return true;
        }

        if (blacklistEnabled) {
            String itemName = handItem.getType().name().toLowerCase();
            for (String blacklisted : blacklist) {
                if (itemName.equals(blacklisted.toLowerCase())) {
                    player.sendMessage(lang.get(player, "hat.blacklisted"));
                    plugin.debug("Hat denied: " + player.getName() + " tried to use blacklisted item " + itemName);
                    return true;
                }
            }
        }

        if (!allowBlocks && handItem.getType().isBlock()) {
            if (!player.hasPermission("essentialsc.hat.blocks")) {
                player.sendMessage(lang.get(player, "hat.no_blocks"));
                plugin.debug("Hat denied: " + player.getName() + " lacks permission for blocks");
                return true;
            }
        }

        if (requireLore && !handItem.hasItemMeta()) {
            player.sendMessage(lang.get(player, "hat.no_lore"));
            plugin.debug("Hat denied: " + player.getName() + " item has no lore/meta");
            return true;
        }

        if (requireName && (!handItem.hasItemMeta() || !handItem.getItemMeta().hasDisplayName())) {
            player.sendMessage(lang.get(player, "hat.no_name"));
            plugin.debug("Hat denied: " + player.getName() + " item has no custom name");
            return true;
        }

        if (helmetItem != null && !helmetItem.getType().isAir()) {
            if (helmetItem.containsEnchantment(org.bukkit.enchantments.Enchantment.BINDING_CURSE)) {
                if (!player.hasPermission("essentialsc.hat.binding_bypass")) {
                    player.sendMessage(lang.get(player, "hat.binding_curse"));
                    plugin.debug("Hat denied: " + player.getName() + " has binding curse helmet");
                    return true;
                }
            }
        }

        inventory.setHelmet(handItem);
        inventory.setItemInMainHand(helmetItem);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("item", getItemName(handItem));
        player.sendMessage(lang.get(player, "hat.success", placeholders));
        plugin.debug(player.getName() + " put " + handItem.getType() + " on head");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    private String getItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        String name = item.getType().name().toLowerCase().replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}