package net.godlycow.org.essc.command.economy;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PayCommand extends Command {

    public PayCommand(EssentialsC plugin) {
        super(plugin, "pay", "essentialsc.pay", true, 2, "command.usage.pay");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            sender.sendMessage(lang.get(sender, "error.player_not_found", Map.of("player", args[0])));
            return true;
        }

        if (target.equals(player)) {
            sender.sendMessage(lang.get(sender, "pay.self"));
            return true;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(args[1]).setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                sender.sendMessage(lang.get(sender, "error.invalid_amount"));
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(lang.get(sender, "error.invalid_number", Map.of("input", args[1])));
            return true;
        }

        plugin.getEconomyManager().has(player.getUniqueId(), amount).thenAccept(hasFunds -> {
            if (!hasFunds) {
                player.sendMessage(lang.get(player, "pay.insufficient",
                        Map.of("amount", plugin.getEconomyManager().format(amount))));
                return;
            }

            plugin.getEconomyManager().withdraw(player.getUniqueId(), amount).thenAccept(success -> {
                if (success) {
                    plugin.getEconomyManager().deposit(target.getUniqueId(), amount).thenAccept(deposited -> {
                        player.getScheduler().run(plugin, task -> {
                            if (!deposited) {
                                plugin.getEconomyManager().deposit(player.getUniqueId(), amount);
                                player.sendMessage(lang.get(player, "error.internal"));
                                return;
                            }

                            String formatted = plugin.getEconomyManager().format(amount);
                            player.sendMessage(lang.get(player, "pay.sent",
                                    Map.of("amount", formatted, "player", target.getName())));
                            target.sendMessage(lang.get(target, "pay.received",
                                    Map.of("amount", formatted, "player", player.getName())));

                            plugin.debug(player.getName() + " paid " + formatted + " to " + target.getName());
                        }, null);
                    });
                }
            });
        });

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return null;
        } else if (args.length == 2) {
            return List.of("100", "1000", "10000");
        }
        return Collections.emptyList();
    }
}