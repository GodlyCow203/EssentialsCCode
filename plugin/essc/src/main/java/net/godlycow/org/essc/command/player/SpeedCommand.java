package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpeedCommand extends Command {

    public SpeedCommand(EssentialsC plugin) {
        super(plugin, "speed", "essentialsc.speed", true, 1, "command.usage.speed");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        String subCommand = args[0].toLowerCase();
        Map<String, String> placeholders = new HashMap<>();

        switch (subCommand) {
            case "reset":
                if (!player.hasPermission("essentialsc.speed.reset")) {
                    player.sendMessage(lang.get(player, "error.no_permission"));
                    plugin.debug("Denied: " + player.getName() + " lacks permission essentialsc.speed.reset");
                    return true;
                }
                player.setWalkSpeed(0.2f);
                player.setFlySpeed(0.1f);
                player.sendMessage(lang.get(player, "speed.reset.success"));
                plugin.debug("Reset speed for " + player.getName());
                break;

            case "walkspeed":
                if (!player.hasPermission("essentialsc.speed.walk")) {
                    player.sendMessage(lang.get(player, "error.no_permission"));
                    plugin.debug("Denied: " + player.getName() + " lacks permission essentialsc.speed.walk");
                    return true;
                }
                if (args.length < 2) {
                    sendUsage(player);
                    return true;
                }
                try {
                    float speed = Float.parseFloat(args[1]);
                    player.setWalkSpeed(speed);
                    placeholders.put("speed", String.valueOf(speed));
                    player.sendMessage(lang.get(player, "speed.walk.success", placeholders));
                    plugin.debug("Set walk speed to " + speed + " for " + player.getName());
                } catch (NumberFormatException e) {
                    player.sendMessage(lang.get(player, "speed.invalid_number"));
                } catch (IllegalArgumentException e) {
                    player.sendMessage(lang.get(player, "speed.invalid_range"));
                }
                break;

            case "flyspeed":
                if (!player.hasPermission("essentialsc.speed.fly")) {
                    player.sendMessage(lang.get(player, "error.no_permission"));
                    plugin.debug("Denied: " + player.getName() + " lacks permission essentialsc.speed.fly");
                    return true;
                }
                if (args.length < 2) {
                    sendUsage(player);
                    return true;
                }
                try {
                    float speed = Float.parseFloat(args[1]);
                    player.setFlySpeed(speed);
                    placeholders.put("speed", String.valueOf(speed));
                    player.sendMessage(lang.get(player, "speed.fly.success", placeholders));
                    plugin.debug("Set fly speed to " + speed + " for " + player.getName());
                } catch (NumberFormatException e) {
                    player.sendMessage(lang.get(player, "speed.invalid_number"));
                } catch (IllegalArgumentException e) {
                    player.sendMessage(lang.get(player, "speed.invalid_range"));
                }
                break;

            default:
                sendUsage(player);
                break;
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("reset", "walkspeed", "flyspeed").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("walkspeed") || sub.equals("flyspeed")) {
                return List.of("0.1", "0.2", "0.5", "1.0").stream()
                        .filter(s -> s.startsWith(args[1]))
                        .toList();
            }
        }
        return Collections.emptyList();
    }
}