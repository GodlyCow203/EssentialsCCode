package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.modules.shop.ShopItem;
import net.godlycow.org.essc.util.FormatUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorthCommand extends Command {

    public WorthCommand(EssentialsC plugin) {
        super(plugin, "worth", "essentialsc.worth.hand", true, 0);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("inventory")) {
            if (!player.hasPermission("essentialsc.worth.inventory")) {
                player.sendMessage(lang.get(player, "error.no_permission"));
                return true;
            }

            double worth = plugin.getSellManager().calculateInventoryWorth(player);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("worth", FormatUtil.formatNumberWithDecimals(worth));
            placeholders.put("currency", worth == 1.0 ?
                    plugin.getConfigManager().getShopCurrencySingular() :
                    plugin.getConfigManager().getShopCurrencyPlural());

            player.sendMessage(lang.get(player, "worth.inventory.header"));
            player.sendMessage(lang.get(player, "worth.inventory.total", placeholders));
            return true;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            player.sendMessage(lang.get(player, "worth.hand.empty"));
            return true;
        }

        double worth = plugin.getSellManager().getItemWorth(hand);
        //use the items shop config name
        ShopItem shopItem = plugin.getSellManager().findMatchingShopItem(hand);
        String itemName = shopItem != null && shopItem.getDisplayName() != null
                ? shopItem.getDisplayName()
                : hand.getType().name().toLowerCase();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("item", itemName);
        placeholders.put("amount", FormatUtil.formatNumber(hand.getAmount()));
        placeholders.put("worth", FormatUtil.formatNumberWithDecimals(worth));
        placeholders.put("currency", worth == 1.0 ?
                plugin.getConfigManager().getShopCurrencySingular() :
                plugin.getConfigManager().getShopCurrencyPlural());

        player.sendMessage(lang.get(player, "worth.hand.header", placeholders));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("inventory").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}