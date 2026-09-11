package net.godlycow.org.essc.command.auction;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.auction.gui.AhGuiManager;
import net.godlycow.org.essc.modules.auction.gui.AhItemFactory;
import net.godlycow.org.essc.modules.auction.AhSoundManager;
import net.godlycow.org.essc.command.Command;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.*;

public class AhCommand extends Command {
    private final AhGuiManager guiManager;
    private final AhSoundManager soundManager;

    public AhCommand(EssentialsC plugin, AhGuiManager guiManager) {
        super(plugin, "ah", "essentialsc.ah.use", true, 0, "command.usage.ah");
        this.guiManager = guiManager;
        this.soundManager = guiManager.getSoundManager();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!checkEnabled(sender)) return true;
        Player player = (Player) sender;

        if (args.length == 0) {
            guiManager.openMainGui(player, 1);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "sell", "s" -> handleSell(player, args);
            case "cancel", "c" -> handleCancel(player, args);
            case "expired", "e" -> guiManager.openExpiredGui(player, 1);
            case "listings", "l", "my" -> guiManager.openListingsGui(player, 1);
            case "notifications", "notif", "notify" -> handleNotifications(player);
            case "reload", "rl" -> handleReload(player);
            case "help", "?" -> sendUsage(player);
            default -> {
                try {
                    guiManager.openMainGui(player, Integer.parseInt(sub));
                } catch (NumberFormatException e) {
                    player.sendMessage(lang.get(player, "ah.invalid_subcommand"));
                    soundManager.playError(player);
                }
            }
        }
        return true;
    }

    private boolean checkEnabled(CommandSender sender) {
        if (!plugin.getConfigManager().isAHEnabled()) {
            sender.sendMessage(lang.get(sender, "ah.disabled"));
            return false;
        }
        if (plugin.getAuctionManager() == null) {
            sender.sendMessage(lang.get(sender, "ah.not_loaded"));
            return false;
        }
        return true;
    }

    private void handleNotifications(Player player) {
        if (!player.hasPermission("essentialsc.ah.notifications")) {
            player.sendMessage(lang.get(player, "error.no_permission"));
            soundManager.playError(player);
            return;
        }

        boolean current = plugin.getAuctionManager().isNotificationsEnabled(player.getUniqueId());
        boolean newValue = !current;
        plugin.getAuctionManager().setNotificationsEnabled(player.getUniqueId(), newValue);
        player.sendMessage(lang.get(player, newValue
                ? "ah.notifications.enabled"
                : "ah.notifications.disabled"));
        soundManager.playClick(player);
    }

    private void handleSell(Player player, String[] args) {
        if (!player.hasPermission("essentialsc.ah.sell")) {
            player.sendMessage(lang.get(player, "error.no_permission"));
            soundManager.playError(player);
            return;
        }

        if (args.length < 2) {
            player.sendMessage(lang.get(player, "command.usage.ah_sell"));
            return;
        }

        BigDecimal price = parsePrice(player, args[1]);
        if (price == null) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(lang.get(player, "ah.no_item"));
            soundManager.playError(player);
            return;
        }

        if (!validatePriceLimits(player, price)) return;

        long duration = plugin.getConfigManager().getAHDuration();
        StringBuilder failReason = new StringBuilder();

        plugin.getAuctionManager().createAuction(player, item, price, duration, failReason)
                .thenAccept(success -> {
                    player.getScheduler().run(plugin, task -> {
                        if (success) {
                            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                            player.sendMessage(lang.get(player, "ah.listed", Map.of(
                                    "price", plugin.getEconomyManager().format(price),
                                    "duration", String.valueOf(duration / 3600000)
                            )));
                            soundManager.playSuccess(player);
                        } else {
                            String reason = failReason.toString();
                            String messageKey = switch (reason) {
                                case "max_auctions" -> "ah.max_auctions_reached";
                                case "enchanted_books_disabled" -> "ah.sell.enchanted_books_disabled";
                                case "material_blacklisted" -> "ah.sell.material_blacklisted";
                                default -> "ah.invalid_subcommand";
                            };
                            player.sendMessage(lang.get(player, messageKey));
                            soundManager.playError(player);
                        }
                    }, null);
                });
    }

    private BigDecimal parsePrice(Player player, String input) {
        String raw = input.trim();
        BigDecimal multiplier = BigDecimal.ONE;

        if (!raw.isEmpty() && Character.isLetter(raw.charAt(raw.length() - 1))) {
            switch (Character.toLowerCase(raw.charAt(raw.length() - 1))) {
                case 'k' -> multiplier = new BigDecimal("1000");
                case 'm' -> multiplier = new BigDecimal("1000000");
                case 'b' -> multiplier = new BigDecimal("1000000000");
                case 't' -> multiplier = new BigDecimal("1000000000000");
                default -> {
                    player.sendMessage(lang.get(player, "ah.invalid_price"));
                    soundManager.playError(player);
                    return null;
                }
            }
            raw = raw.substring(0, raw.length() - 1);
        }

        try {
            BigDecimal price = new BigDecimal(raw);
            if (price.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            return price.multiply(multiplier);
        } catch (NumberFormatException e) {
            player.sendMessage(lang.get(player, "ah.invalid_price"));
            soundManager.playError(player);
            return null;
        }
    }

    private boolean validatePriceLimits(Player player, BigDecimal price) {
        BigDecimal min = plugin.getConfigManager().getAHMinPrice();
        BigDecimal max = plugin.getConfigManager().getAHMaxPrice();

        if (!player.hasPermission("essentialsc.ah.bypass.price.min") && price.compareTo(min) < 0) {
            player.sendMessage(lang.get(player, "ah.price_too_low",
                    Map.of("min", plugin.getEconomyManager().format(min))));
            soundManager.playError(player);
            return false;
        }

        if (max != null && !player.hasPermission("essentialsc.ah.bypass.price.max") && price.compareTo(max) > 0) {
            player.sendMessage(lang.get(player, "ah.price_too_high",
                    Map.of("max", plugin.getEconomyManager().format(max))));
            soundManager.playError(player);
            return false;
        }
        return true;
    }

    private void handleCancel(Player player, String[] args) {
        if (!player.hasPermission("essentialsc.ah.cancel")) {
            player.sendMessage(lang.get(player, "error.no_permission"));
            soundManager.playError(player);
            return;
        }

        if (args.length < 2) {
            player.sendMessage(lang.get(player, "command.usage.ah_cancel"));
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(lang.get(player, "ah.invalid_id"));
            soundManager.playError(player);
            return;
        }

        plugin.getAuctionManager().cancelAuction(player, id).thenAccept(success -> {
            player.getScheduler().run(plugin, task -> {
                if (success) {
                    player.sendMessage(lang.get(player, "ah.cancelled"));
                    soundManager.playCancel(player);
                } else {
                    player.sendMessage(lang.get(player, "ah.not_your_auction"));
                    soundManager.playError(player);
                }
            }, null);
        });
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("essentialsc.ah.reload")) {
            player.sendMessage(lang.get(player, "error.no_permission"));
            soundManager.playError(player);
            return;
        }
        plugin.getAuctionManager().reload();
        guiManager.reload();
        player.sendMessage(lang.get(player, "ah.reloaded"));
        soundManager.playSuccess(player);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essentialsc.ah.use")) return Collections.emptyList();
        if (!plugin.getConfigManager().isAHEnabled()) return Collections.emptyList();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("sell", "expired", "listings", "help"));
            if (sender.hasPermission("essentialsc.ah.cancel")) subs.add("cancel");
            if (sender.hasPermission("essentialsc.ah.notifications")) subs.add("notifications");
            if (sender.hasPermission("essentialsc.ah.reload")) subs.add("reload");
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("cancel") &&
                sender instanceof Player player && sender.hasPermission("essentialsc.ah.cancel")) {
            return plugin.getAuctionManager().getPlayerAuctions(player.getUniqueId()).stream()
                    .map(a -> String.valueOf(a.getId()))
                    .filter(id -> id.startsWith(args[1]))
                    .toList();
        }

        return Collections.emptyList();
    }

    public void openMainGui(Player player, int page) {
        guiManager.openMainGui(player, page);
    }

    public void openExpiredGui(Player player) {
        guiManager.openExpiredGui(player, 1);
    }

    public void openExpiredGui(Player player, int page) {
        guiManager.openExpiredGui(player, page);
    }

    public void openListingsGui(Player player, int page) {
        guiManager.openListingsGui(player, page);
    }

    public void openHistoryTypeGui(Player player) {
        guiManager.openHistoryTypeGui(player);
    }

    public void openSellHistoryGui(Player player, int page) {
        guiManager.openSellHistoryGui(player, page);
    }

    public void openBuyHistoryGui(Player player, int page) {
        guiManager.openBuyHistoryGui(player, page);
    }

    public AhSoundManager getSoundManager() {
        return soundManager;
    }

    public AhItemFactory getItemFactory() {
        return guiManager.getItemFactory();
    }
}