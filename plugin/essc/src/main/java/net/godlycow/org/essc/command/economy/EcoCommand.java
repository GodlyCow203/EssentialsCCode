package net.godlycow.org.essc.command.economy;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class EcoCommand extends Command {

    private static final String PERM_BASE = "essentialsc.eco";
    private static final String PERM_GIVE = PERM_BASE + ".give";
    private static final String PERM_TAKE = PERM_BASE + ".take";
    private static final String PERM_SET = PERM_BASE + ".set";
    private static final String PERM_RESET = PERM_BASE + ".reset";
    private static final String PERM_EVERYONE = PERM_BASE + ".everyone";

    public EcoCommand(EssentialsC plugin) {
        super(plugin, "eco", PERM_BASE + ".admin", false, 3, "command.usage.eco");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];
        boolean isEveryone = targetName.equalsIgnoreCase("@everyone") || targetName.equalsIgnoreCase("*");

        if (!hasActionPermission(sender, action)) {
            sender.sendMessage(lang.get(sender, "error.no_permission"));
            return true;
        }

        if (isEveryone && !sender.hasPermission(PERM_EVERYONE)) {
            sender.sendMessage(lang.get(sender, "error.no_permission"));
            return true;
        }

        if (action.equals("reset")) {
            if (isEveryone) {
                handleResetEveryone(sender);
            } else {
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    sender.sendMessage(lang.get(sender, "error.player_not_found", Map.of("player", targetName)));
                    return true;
                }
                handleReset(sender, target);
            }
            return true;
        }

        if (args.length < 3) {
            sendUsage(sender);
            return true;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(args[2]).setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) < 0 && !action.equals("set")) {
                sender.sendMessage(lang.get(sender, "error.negative_amount"));
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(lang.get(sender, "error.invalid_number", Map.of("input", args[2])));
            return true;
        }

        if (isEveryone) {
            switch (action) {
                case "give" -> handleGiveEveryone(sender, amount);
                case "take" -> handleTakeEveryone(sender, amount);
                case "set" -> handleSetEveryone(sender, amount);
                default -> sendUsage(sender);
            }
        } else {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(lang.get(sender, "error.player_not_found", Map.of("player", targetName)));
                return true;
            }
            switch (action) {
                case "give" -> handleGive(sender, target, amount);
                case "take" -> handleTake(sender, target, amount);
                case "set" -> handleSet(sender, target, amount);
                default -> sendUsage(sender);
            }
        }

        return true;
    }

    private boolean hasActionPermission(CommandSender sender, String action) {
        return switch (action) {
            case "give" -> sender.hasPermission(PERM_GIVE) || sender.hasPermission(PERM_BASE + ".admin");
            case "take" -> sender.hasPermission(PERM_TAKE) || sender.hasPermission(PERM_BASE + ".admin");
            case "set" -> sender.hasPermission(PERM_SET) || sender.hasPermission(PERM_BASE + ".admin");
            case "reset" -> sender.hasPermission(PERM_RESET) || sender.hasPermission(PERM_BASE + ".admin");
            default -> false;
        };
    }

    private void handleGive(CommandSender sender, OfflinePlayer target, BigDecimal amount) {
        plugin.getEconomyManager().hasAccount(target.getUniqueId()).thenCompose(hasAccount -> {
                    if (!hasAccount) {
                        return plugin.getEconomyManager().createAccount(target.getUniqueId(), target.getName());
                    }
                    return CompletableFuture.completedFuture(true);
                }).thenCompose(created -> plugin.getEconomyManager().deposit(target.getUniqueId(), amount))
                .thenAccept(success -> {
                    String formatted = plugin.getEconomyManager().format(amount);
                    String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString();

                    sender.sendMessage(lang.get(sender, "eco.give",
                            Map.of("amount", formatted, "player", targetName)));

                    if (target.isOnline() && target.getPlayer() != null) {
                        target.getPlayer().sendMessage(lang.get(target.getPlayer(), "eco.give.notify", Map.of("amount", formatted)));
                    }

                    plugin.debug(sender.getName() + " gave " + formatted + " to " + targetName);
                });
    }

    private void handleTake(CommandSender sender, OfflinePlayer target, BigDecimal amount) {
        plugin.getEconomyManager().withdraw(target.getUniqueId(), amount).thenAccept(success -> {
            String formatted = plugin.getEconomyManager().format(amount);
            String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString();

            if (success) {
                sender.sendMessage(lang.get(sender, "eco.take",
                        Map.of("amount", formatted, "player", targetName)));

                if (target.isOnline() && target.getPlayer() != null) {
                    target.getPlayer().sendMessage(lang.get(target.getPlayer(), "eco.take.notify", Map.of("amount", formatted)));
                }
            } else {
                sender.sendMessage(lang.get(sender, "eco.take.failed",
                        Map.of("player", targetName)));
            }
        });
    }

    private void handleSet(CommandSender sender, OfflinePlayer target, BigDecimal amount) {
        plugin.getEconomyManager().setBalance(target.getUniqueId(), amount).thenAccept(success -> {
            String formatted = plugin.getEconomyManager().format(amount);
            String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString();

            sender.sendMessage(lang.get(sender, "eco.set",
                    Map.of("amount", formatted, "player", targetName)));

            if (target.isOnline() && target.getPlayer() != null) {
                target.getPlayer().sendMessage(lang.get(target.getPlayer(), "eco.set.notify", Map.of("amount", formatted)));
            }

            plugin.debug(sender.getName() + " set " + targetName + "'s balance to " + formatted);
        });
    }

    private void handleReset(CommandSender sender, OfflinePlayer target) {
        BigDecimal starting = plugin.getEconomyManager().getStartingBalance();
        plugin.getEconomyManager().setBalance(target.getUniqueId(), starting).thenAccept(success -> {
            String formatted = plugin.getEconomyManager().format(starting);
            String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString();

            sender.sendMessage(lang.get(sender, "eco.reset",
                    Map.of("player", targetName, "balance", formatted)));

            if (target.isOnline() && target.getPlayer() != null) {
                target.getPlayer().sendMessage(lang.get(target.getPlayer(), "eco.reset.notify", Map.of("balance", formatted)));
            }
        });
    }

    private void handleGiveEveryone(CommandSender sender, BigDecimal amount) {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) {
            sender.sendMessage(lang.get(sender, "error.no_players_online"));
            return;
        }

        String formatted = plugin.getEconomyManager().format(amount);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for (Player target : players) {
            futures.add(plugin.getEconomyManager().deposit(target.getUniqueId(), amount));
            target.sendMessage(lang.get(target, "eco.give.notify", Map.of("amount", formatted)));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            sender.sendMessage(lang.get(sender, "eco.give.everyone",
                    Map.of("amount", formatted, "count", String.valueOf(players.size()))));
            plugin.debug(sender.getName() + " gave " + formatted + " to everyone (" + players.size() + " players)");
        });
    }

    private void handleTakeEveryone(CommandSender sender, BigDecimal amount) {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) {
            sender.sendMessage(lang.get(sender, "error.no_players_online"));
            return;
        }

        String formatted = plugin.getEconomyManager().format(amount);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        int[] successCount = {0};

        for (Player target : players) {
            CompletableFuture<Boolean> future = plugin.getEconomyManager().withdraw(target.getUniqueId(), amount)
                    .thenApply(success -> {
                        if (success) {
                            successCount[0]++;
                            target.sendMessage(lang.get(target, "eco.take.notify", Map.of("amount", formatted)));
                        }
                        return success;
                    });
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            sender.sendMessage(lang.get(sender, "eco.take.everyone",
                    Map.of("amount", formatted, "count", String.valueOf(successCount[0]))));
            plugin.debug(sender.getName() + " took " + formatted + " from everyone (success: " + successCount[0] + ")");
        });
    }

    private void handleSetEveryone(CommandSender sender, BigDecimal amount) {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) {
            sender.sendMessage(lang.get(sender, "error.no_players_online"));
            return;
        }

        String formatted = plugin.getEconomyManager().format(amount);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for (Player target : players) {
            futures.add(plugin.getEconomyManager().setBalance(target.getUniqueId(), amount));
            target.sendMessage(lang.get(target, "eco.set.notify", Map.of("amount", formatted)));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            sender.sendMessage(lang.get(sender, "eco.set.everyone",
                    Map.of("amount", formatted, "count", String.valueOf(players.size()))));
            plugin.debug(sender.getName() + " set everyone's balance to " + formatted + " (" + players.size() + " players)");
        });
    }

    private void handleResetEveryone(CommandSender sender) {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) {
            sender.sendMessage(lang.get(sender, "error.no_players_online"));
            return;
        }

        BigDecimal starting = plugin.getEconomyManager().getStartingBalance();
        String formatted = plugin.getEconomyManager().format(starting);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for (Player target : players) {
            futures.add(plugin.getEconomyManager().setBalance(target.getUniqueId(), starting));
            target.sendMessage(lang.get(target, "eco.reset.notify", Map.of("balance", formatted)));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            sender.sendMessage(lang.get(sender, "eco.reset.everyone",
                    Map.of("balance", formatted, "count", String.valueOf(players.size()))));
            plugin.debug(sender.getName() + " reset everyone's balance (" + players.size() + " players)");
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> actions = new ArrayList<>();
            if (sender.hasPermission(PERM_GIVE) || sender.hasPermission(PERM_BASE + ".admin")) actions.add("give");
            if (sender.hasPermission(PERM_TAKE) || sender.hasPermission(PERM_BASE + ".admin")) actions.add("take");
            if (sender.hasPermission(PERM_SET) || sender.hasPermission(PERM_BASE + ".admin")) actions.add("set");
            if (sender.hasPermission(PERM_RESET) || sender.hasPermission(PERM_BASE + ".admin")) actions.add("reset");
            return actions;
        } else if (args.length == 2) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission(PERM_EVERYONE) || sender.hasPermission(PERM_BASE + ".admin")) {
                completions.add("@everyone");
                completions.add("*");
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
            return completions;
        } else if (args.length == 3 && !args[0].equalsIgnoreCase("reset")) {
            return List.of("100", "500", "1000");
        }
        return Collections.emptyList();
    }
}