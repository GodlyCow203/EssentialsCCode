package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PtimeCommand extends Command {

    public PtimeCommand(EssentialsC plugin) {
        super(plugin, "ptime", "essentialsc.ptime", true, 0, "command.usage.ptime");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length == 0) {
            if (player.isPlayerTimeRelative()) {
                player.resetPlayerTime();
                player.sendMessage(lang.get(player, "ptime.reset"));
            } else {
                player.sendMessage(lang.get(player, "ptime.current",
                        Map.of("time", String.valueOf(player.getPlayerTime()))));
            }
            return true;
        }

        String input = args[0].toLowerCase();

        if (input.equals("reset") || input.equals("normal") || input.equals("r")) {
            player.resetPlayerTime();
            player.sendMessage(lang.get(player, "ptime.reset"));
            plugin.debug(player.getName() + " reset their time");
            return true;
        }

        long time;
        boolean relative = true;

        switch (input) {
            case "day", "morning" -> time = 1000;
            case "noon", "midday" -> time = 6000;
            case "night", "evening" -> time = 13000;
            case "midnight" -> time = 18000;
            case "dawn", "sunrise" -> time = 23000;
            case "dusk", "sunset" -> time = 12000;
            default -> {
                try {
                    time = Long.parseLong(input);
                    if (time < 0 || time > 24000) {
                        player.sendMessage(lang.get(player, "ptime.invalid"));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(lang.get(player, "ptime.invalid"));
                    return true;
                }
            }
        }

        if (args.length > 1) {
            String flag = args[1].toLowerCase();
            if (flag.equals("locked") || flag.equals("l") || flag.equals("fixed") || flag.equals("f")) {
                relative = false;
            }
        }

        player.setPlayerTime(time, relative);

        var typeComponent = relative ? lang.get(player, "ptime.relative") : lang.get(player, "ptime.locked");
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("time", String.valueOf(time));
        placeholders.put("type", plugin.getMiniMessage().serialize(typeComponent));

        player.sendMessage(lang.get(player, "ptime.set", placeholders));

        plugin.debug(player.getName() + " set time to " + time + " (relative: " + relative + ")");
        return true;
    }


    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = List.of("day", "night", "noon", "midnight", "dawn", "dusk", "reset", "1000", "6000", "13000", "18000");
            return options.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2) {
            return List.of("locked", "relative").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}