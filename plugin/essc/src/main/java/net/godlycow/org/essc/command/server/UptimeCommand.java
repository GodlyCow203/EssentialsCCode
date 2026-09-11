package net.godlycow.org.essc.command.server;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class UptimeCommand extends Command {

    public UptimeCommand(EssentialsC plugin) {
        super(plugin, "uptime", "essentialsc.uptime", false, 0, "command.usage.uptime");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();

        long days = TimeUnit.MILLISECONDS.toDays(uptimeMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(uptimeMillis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMillis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(uptimeMillis) % 60;

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("days", String.valueOf(days));
        placeholders.put("hours", String.valueOf(hours));
        placeholders.put("minutes", String.valueOf(minutes));
        placeholders.put("seconds", String.valueOf(seconds));

        sender.sendMessage(lang.get(sender, "uptime.message", placeholders));

        plugin.debug("Uptime checked by " + sender.getName() + ": " +
                days + "d " + hours + "h " + minutes + "m " + seconds + "s");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}