package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.modules.shop.sell.SellManager;
import net.godlycow.org.essc.util.FormatUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuickSellCommand extends Command {

    public QuickSellCommand(EssentialsC plugin) {
        super(plugin, "quicksell", "essentialsc.quicksell.hand", true, 0);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("inventory")) {
            if (!player.hasPermission("essentialsc.quicksell.inventory")) {
                player.sendMessage(lang.get(player, "error.no_permission"));
                return true;
            }

            SellManager.SellResult result = plugin.getSellManager().sellInventory(player);

            if (!result.isSuccess()) {
                player.sendMessage(lang.get(player, "sell.error.no-sellable-items"));
                return true;
            }

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("amount", FormatUtil.formatNumber(result.getAmount()));
            placeholders.put("worth", FormatUtil.formatNumberWithDecimals(result.getPrice()));
            placeholders.put("currency", result.getPrice() == 1.0 ?
                    plugin.getConfigManager().getShopCurrencySingular() :
                    plugin.getConfigManager().getShopCurrencyPlural());

            player.sendMessage(lang.get(player, "quicksell.success.inventory", placeholders));
            return true;
        }

        if (!player.hasPermission("essentialsc.quicksell.hand")) {
            player.sendMessage(lang.get(player, "error.no_permission"));
            return true;
        }

        SellManager.SellResult result = plugin.getSellManager().sellHand(player);

        if (!result.isSuccess()) {
            player.sendMessage(lang.get(player, "sell.error.no-sellable-items"));
            return true;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", FormatUtil.formatNumber(result.getAmount()));
        placeholders.put("worth", FormatUtil.formatNumberWithDecimals(result.getPrice()));
        placeholders.put("currency", result.getPrice() == 1.0 ?
                plugin.getConfigManager().getShopCurrencySingular() :
                plugin.getConfigManager().getShopCurrencyPlural());

        player.sendMessage(lang.get(player, "quicksell.success.hand", placeholders));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("hand", "inventory").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}