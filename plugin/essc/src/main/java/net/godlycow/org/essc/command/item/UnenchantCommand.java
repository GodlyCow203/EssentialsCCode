package net.godlycow.org.essc.command.item;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UnenchantCommand extends Command {

    public UnenchantCommand(EssentialsC plugin) {
        super(plugin, "unenchant", "essentialsc.unenchant", true, 0, "command.usage.unenchant");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType().isAir()) {
            player.sendMessage(lang.get(player, "enchant.no_item"));
            plugin.debug("Unenchant failed: " + player.getName() + " not holding item");
            return true;
        }

        if (args.length == 0) {
            int count = item.getEnchantments().size();
            item.getEnchantments().keySet().forEach(item::removeEnchantment);
            player.sendMessage(lang.get(player, "unenchant.success.all"));
            plugin.debug("Removed all " + count + " enchantments from " + item.getType() +
                    " for " + player.getName());
        } else {
            Enchantment enchantment = getEnchantment(args[0]);
            if (enchantment == null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("enchantment", args[0]);
                player.sendMessage(lang.get(player, "enchant.invalid", placeholders));
                plugin.debug("Unenchant failed: Invalid enchantment '" + args[0] + "'");
                return true;
            }

            if (!item.containsEnchantment(enchantment)) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("enchantment", getEnchantmentName(enchantment));
                player.sendMessage(lang.get(player, "unenchant.not_found", placeholders));
                plugin.debug("Unenchant failed: " + item.getType() + " doesn't have " +
                        getEnchantmentName(enchantment));
                return true;
            }

            item.removeEnchantment(enchantment);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("enchantment", getEnchantmentName(enchantment));
            player.sendMessage(lang.get(player, "unenchant.success.single", placeholders));
            plugin.debug("Removed " + getEnchantmentName(enchantment) + " from " +
                    item.getType() + " for " + player.getName());
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            Player player = (Player) sender;
            ItemStack item = player.getInventory().getItemInMainHand();

            return item.getEnchantments().keySet().stream()
                    .map(this::getEnchantmentName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private Enchantment getEnchantment(String name) {
        for (Enchantment e : Enchantment.values()) {
            if (getEnchantmentName(e).equalsIgnoreCase(name) ||
                    e.getKey().getKey().equalsIgnoreCase(name)) {
                return e;
            }
        }
        return null;
    }

    private String getEnchantmentName(Enchantment enchantment) {
        return enchantment.getKey().getKey().toLowerCase();
    }
}