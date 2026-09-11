package net.godlycow.org.essc.command.admin;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GamemodeCommand extends Command {
    
    public GamemodeCommand(EssentialsC plugin) {
        super(plugin, "gamemode", null, true, 0, "command.usage.gm");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        return execute(sender, "gamemode", args);
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;
        GameMode targetMode;
        Player target;
        int targetArgIndex;

        targetMode = getModeFromLabel(label);

        if (targetMode != null) {
            targetArgIndex = 0;
        } else {
            if (args.length < 1) {
                sendUsage(sender);
                return true;
            }
            targetMode = parseGameMode(args[0]);
            if (targetMode == null) {
                player.sendMessage(lang.get(player, "gamemode.invalid", Map.of("input", args[0])));
                return true;
            }
            targetArgIndex = 1;
        }

        String modePermission = "essentialsc.gamemode." + targetMode.name().toLowerCase();
        if (!player.hasPermission(modePermission) && !player.hasPermission("essentialsc.gamemode")) {
            player.sendMessage(lang.get(player, "error.no_permission"));
            return true;
        }

        target = resolveTarget(player, args, targetArgIndex);
        if (target == null) {
            return true;
        }

        if (target != player && !player.hasPermission(modePermission + ".others") && !player.hasPermission("essentialsc.gamemode.others")) {
            player.sendMessage(lang.get(player, "error.no_permission"));
            return true;
        }

        target.setGameMode(targetMode);

        String modeName = gameModeName(targetMode);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("mode", modeName);
        placeholders.put("target", target.getName());

        if (target == player) {
            player.sendMessage(lang.get(player, "gamemode.changed", placeholders));
        } else {
            player.sendMessage(lang.get(player, "gamemode.changed.other", placeholders));
            Map<String, String> targetPlaceholders = new HashMap<>();
            targetPlaceholders.put("mode", modeName);
            targetPlaceholders.put("player", player.getName());
            target.sendMessage(lang.get(target, "gamemode.changed.by", targetPlaceholders));
        }

        plugin.debug(player.getName() + " set gamemode of " + target.getName() + " to " + targetMode.name());
        return true;
    }

    private GameMode getModeFromLabel(String label) {
        return switch (label.toLowerCase()) {
            case "gms" -> GameMode.SURVIVAL;
            case "gmc" -> GameMode.CREATIVE;
            case "gmsp" -> GameMode.SPECTATOR;
            case "gma" -> GameMode.ADVENTURE;
            default -> null;
        };
    }

    private Player resolveTarget(Player sender, String[] args, int argIndex) {
        if (args.length > argIndex) {
            Player found = plugin.getServer().getPlayer(args[argIndex]);
            if (found == null) {
                sender.sendMessage(lang.get(sender, "error.player_not_found", Map.of("player", args[argIndex])));
                return null;
            }
            return found;
        }
        return sender;
    }

    private GameMode parseGameMode(String input) {
        return switch (input.toLowerCase()) {
            case "survival", "s", "0" -> GameMode.SURVIVAL;
            case "creative", "c", "1" -> GameMode.CREATIVE;
            case "adventure", "a", "2" -> GameMode.ADVENTURE;
            case "spectator", "sp", "3" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    private String gameModeName(GameMode mode) {
        return switch (mode) {
            case SURVIVAL -> "Survival";
            case CREATIVE -> "Creative";
            case ADVENTURE -> "Adventure";
            case SPECTATOR -> "Spectator";
        };
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return tabComplete(sender, "gamemode", args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String label, String[] args) {
        GameMode mode = getModeFromLabel(label);

        if (mode != null) {
            if (args.length == 1) {
                String modePermission = "essentialsc.gamemode." + mode.name().toLowerCase();
                if (sender.hasPermission(modePermission + ".others") || sender.hasPermission("essentialsc.gamemode.others")) {
                    return plugin.getServer().getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                            .toList();
                }
            }
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> modes = Arrays.asList("survival", "creative", "adventure", "spectator");
            return modes.stream()
                    .filter(m -> m.startsWith(args[0].toLowerCase()))
                    .filter(m -> sender.hasPermission("essentialsc.gamemode." + m) || sender.hasPermission("essentialsc.gamemode"))
                    .toList();
        }

        if (args.length == 2) {
            GameMode targetMode = parseGameMode(args[0]);
            if (targetMode != null) {
                String modePermission = "essentialsc.gamemode." + targetMode.name().toLowerCase();
                if (sender.hasPermission(modePermission + ".others") || sender.hasPermission("essentialsc.gamemode.others")) {
                    return plugin.getServer().getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .toList();
                }
            }
        }

        return Collections.emptyList();
    }
}
