package net.godlycow.org.essc.test;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.scoreboard.ScoreboardManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredListener;

public class ScoreboardDisableTestCommand implements CommandExecutor {

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

        boolean IsInConfigEnabled = plugin.getConfigManager().isScoreboardEnabled();
        if (IsInConfigEnabled == true){
            sender.sendMessage(mini.deserialize("<dark_gray>[<#FFBF00>Info<dark_gray>] <white>Scoreboard is <green>enabled"));
        } else {
            sender.sendMessage(mini.deserialize("<dark_gray>[<#FFBF00>Info<dark_gray>] <white>Scoreboard is <red>disabled"));

        }

        ScoreboardManager scoreboardManager = plugin.getScoreboardManager();
        if (scoreboardManager == null) {
            sender.sendMessage(mini.deserialize("<dark_gray>[<#FFBF00>Info<dark_gray>] <white>ScoreboardManager is <red>null"));
        } else {
            sender.sendMessage(mini.deserialize("<dark_gray>[<#FFBF00>Info<dark_gray>] <white>ScoreboardManager is <green>not null"));
        }

        boolean didWeFindAScoreboardListener = false;

        for (HandlerList handlerList : HandlerList.getHandlerLists()) {

            for (RegisteredListener listener: handlerList.getRegisteredListeners()) {

                if (listener.getPlugin().equals(plugin)) {
                    String listenerName = listener.getListener().getClass().getSimpleName();
                    if (listenerName.contains("Scoreboard")) {
                        sender.sendMessage(mini.deserialize("<dark_gray>[<#FFBF00>Info<dark_gray>] <white>Registered listener <listener>", Placeholder.parsed("listener", listenerName)));
                        didWeFindAScoreboardListener = true;
                    }
                }
            }
        }

        if (!didWeFindAScoreboardListener) {
            sender.sendMessage(mini.deserialize("<dark_gray>[<#FFBF00>Info<dark_gray>] <white>Did not find a registered Scoreboard Listener"));
        }

        sender.sendMessage(mini.deserialize("<#00A3FF>========================================"));
        if (!IsInConfigEnabled && scoreboardManager == null && !didWeFindAScoreboardListener) {
            sender.sendMessage(mini.deserialize("<dark_gray>[<#FFBF00>Info<dark_gray>] <white>Scoreboard is fully <red>disabled"));
        } else if (IsInConfigEnabled) {
            sender.sendMessage(mini.deserialize("<dark_gray>[<#FFBF00>Info<dark_gray>] <white>Scoreboard is <green>enabled <white>in config"));
        } else {
            sender.sendMessage(mini.deserialize("<dark_gray>[<#FFBF00>Info<dark_gray>] <white>Scoreboard is <yellow>partially active"));
        }
        sender.sendMessage(mini.deserialize("<#00A3FF>========================================"));

        return true;
    }
}
