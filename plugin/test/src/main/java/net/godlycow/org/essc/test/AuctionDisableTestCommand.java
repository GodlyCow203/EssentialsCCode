package net.godlycow.org.essc.test;

import net.godlycow.org.essc.CommandRegistration;
import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.auction.AuctionManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredListener;

public class AuctionDisableTestCommand implements CommandExecutor {

    MiniMessage mini = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var esscPlugin = Bukkit.getPluginManager().getPlugin("EssentialsC");

        if (esscPlugin == null) {
            sender.sendMessage(mini.deserialize("<dark_gray>[<#F54927>Error<dark_gray>] <white>EssC not found"));
            return true;
        }

        if (!(esscPlugin instanceof EssentialsC plugin)) {
            sender.sendMessage(mini.deserialize("<dark_gray>[<#F54927>Error<dark_gray>] <white>Instance not found"));
            return true;
        }

        sender.sendMessage(mini.deserialize("<#00A3FF>========================================"));

        boolean configEnabled = plugin.getConfigManager().isAHEnabled();
        if (configEnabled) {
            sender.sendMessage(mini.deserialize("<dark_gray>[<#FFBF00>Info<dark_gray>] <white>Auction House is <green>enabled <white>in config"));
        } else {
            sender.sendMessage(mini.deserialize("<dark_gray>[<#FFBF00>Info<dark_gray>] <white>Auction House is <red>disabled <white>in config"));
        }

        AuctionManager auctionManager = plugin.getAuctionManager();
        if (auctionManager == null) {
            sender.sendMessage(mini.deserialize("<dark_gray>[<#FFBF00>Info<dark_gray>] <white>AuctionManager is <red>null"));
        } else {
            sender.sendMessage(mini.deserialize("<dark_gray>[<#FFBF00>Info<dark_gray>] <white>AuctionManager is <green>not null"));
        }

        boolean ahCommandRegistered = CommandRegistration.isOwnedByE("ah");
        if (ahCommandRegistered) {
            sender.sendMessage(mini.deserialize("<dark_gray>[<#FFBF00>Info<dark_gray>] <white>AH command is <green>registered"));
        } else {
            sender.sendMessage(mini.deserialize("<dark_gray>[<#FFBF00>Info<dark_gray>] <white>AH command is <red>not registered"));
        }

        boolean foundAhListener = false;

        for (HandlerList handlerList : HandlerList.getHandlerLists()) {
            for (RegisteredListener listener : handlerList.getRegisteredListeners()) {
                if (listener.getPlugin().equals(plugin)) {
                    String listenerName = listener.getListener().getClass().getSimpleName();
                    if (listenerName.contains("Auction") || listenerName.contains("Ah")) {
                        sender.sendMessage(mini.deserialize(
                                "<dark_gray>[<#FFBF00>Info<dark_gray>] <white>Registered listener <listener>",
                                Placeholder.parsed("listener", listenerName)));
                        foundAhListener = true;
                    }
                }
            }
        }

        if (!foundAhListener) {
            sender.sendMessage(mini.deserialize("<dark_gray>[<#FFBF00>Info<dark_gray>] <white>Did not find any registered Auction House listeners"));
        }

        sender.sendMessage(mini.deserialize("<#00A3FF>========================================"));
        if (!configEnabled && auctionManager == null && !ahCommandRegistered && !foundAhListener) {
            sender.sendMessage(mini.deserialize("<dark_gray>[<#FFBF00>Info<dark_gray>] <white>Auction House is fully <red>disabled"));
        } else if (configEnabled) {
            sender.sendMessage(mini.deserialize("<dark_gray>[<#FFBF00>Info<dark_gray>] <white>Auction House is <green>enabled <white>in config"));
        } else {
            sender.sendMessage(mini.deserialize("<dark_gray>[<#FFBF00>Info<dark_gray>] <white>Auction House is <yellow>partially active"));
        }
        sender.sendMessage(mini.deserialize("<#00A3FF>========================================"));

        return true;
    }
}
