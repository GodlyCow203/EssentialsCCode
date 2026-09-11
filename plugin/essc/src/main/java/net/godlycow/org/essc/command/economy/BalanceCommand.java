package net.godlycow.org.essc.command.economy;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BalanceCommand extends Command {

    public BalanceCommand(EssentialsC plugin) {
        super(plugin, "balance", "essentialsc.balance", false, 0, "command.usage.balance");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(lang.get(sender, "error.player_only"));
                return true;
            }
            showBalance(sender, player);
        } else {
            if (!sender.hasPermission("essentialsc.balance.others")) {
                sender.sendMessage(lang.get(sender, "error.no_permission"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(lang.get(sender, "error.player_not_found", Map.of("player", args[0])));
                return true;
            }
            showBalance(sender, target);
        }
        return true;
    }

    private void showBalance(CommandSender sender, Player target) {
        plugin.getEconomyManager().getBalance(target.getUniqueId()).thenAccept(balance -> {
            String formatted = plugin.getEconomyManager().format(balance);
            if (sender.equals(target)) {
                sender.sendMessage(lang.get(sender, "balance.self", Map.of("balance", formatted)));
            } else {
                sender.sendMessage(lang.get(sender, "balance.other",
                        Map.of("player", target.getName(), "balance", formatted)));
            }
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("essentialsc.balance.others")) {
            return null;
        }
        return Collections.emptyList();
    }
}