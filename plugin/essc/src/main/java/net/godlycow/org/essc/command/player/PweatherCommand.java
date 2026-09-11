package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.WeatherType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PweatherCommand extends Command {

    public PweatherCommand(EssentialsC plugin) {
        super(plugin, "pweather", "essentialsc.pweather", true, 0, "command.usage.pweather");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length == 0) {
            if (player.getPlayerWeather() == null) {
                player.sendMessage(lang.get(player, "pweather.current_normal"));
            } else {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("weather", player.getPlayerWeather().name().toLowerCase());
                player.sendMessage(lang.get(player, "pweather.current", placeholders));
            }
            return true;
        }

        String input = args[0].toLowerCase();

        if (input.equals("reset") || input.equals("normal") || input.equals("r") || input.equals("clear")) {
            if (player.getPlayerWeather() == null) {
                player.sendMessage(lang.get(player, "pweather.already_normal"));
            } else {
                player.resetPlayerWeather();
                player.sendMessage(lang.get(player, "pweather.reset"));
                plugin.debug(player.getName() + " reset their weather");
            }
            return true;
        }

        WeatherType weather;
        String weatherName;

        switch (input) {
            case "sun", "sunny", "clear" -> {
                weather = WeatherType.CLEAR;
                weatherName = "clear";
            }
            case "rain", "rainy", "raining" -> {
                weather = WeatherType.DOWNFALL;
                weatherName = "rain";
            }
            case "storm", "thunder", "thunderstorm" -> {
                weather = WeatherType.DOWNFALL;
                weatherName = "storm";
            }
            default -> {
                player.sendMessage(lang.get(player, "pweather.invalid"));
                return true;
            }
        }

        player.setPlayerWeather(weather);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("weather", weatherName);
        player.sendMessage(lang.get(player, "pweather.set", placeholders));

        plugin.debug(player.getName() + " set weather to " + weatherName);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = List.of("clear", "rain", "storm", "reset", "sun", "normal");
            return options.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}