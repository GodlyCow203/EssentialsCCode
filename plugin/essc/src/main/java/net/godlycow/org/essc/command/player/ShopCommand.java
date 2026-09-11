package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ShopCommand extends Command {

    public ShopCommand(EssentialsC plugin) {
        super(plugin, "shop", "essentialsc.shop", true, 0, "command.usage.shop");
    }

    public static void unregisterCommand() {
        try {
            CommandMap commandMap = Bukkit.getServer().getCommandMap();

            Field knownCommandsField = null;
            Class<?> clazz = commandMap.getClass();
            while (clazz != null && knownCommandsField == null) {
                try {
                    knownCommandsField = clazz.getDeclaredField("knownCommands");
                } catch (NoSuchFieldException ignored) {
                    clazz = clazz.getSuperclass();
                }
            }

            if (knownCommandsField == null) {
                Bukkit.getLogger().warning("[EssentialsC] Could not locate knownCommands field to unregister /shop");
                return;
            }

            knownCommandsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, org.bukkit.command.Command> knownCommands =
                    (Map<String, org.bukkit.command.Command>) knownCommandsField.get(commandMap);

            knownCommands.remove("shop");
            knownCommands.remove("essentialsc:shop");

        } catch (IllegalAccessException e) {
            Bukkit.getLogger().warning("[EssentialsC] Failed to unregister /shop command: " + e.getMessage());
        }
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length == 0) {
            plugin.getShopManager().openMainShop(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("essentialsc.shop.admin")) {
                player.sendMessage(lang.get(player, "error.no_permission"));
                return true;
            }

            plugin.getShopManager().reload();
            player.sendMessage(lang.get(player, "shop.reload-success"));
            plugin.debug("Shop reloaded by " + player.getName());
            return true;
        }

        String category = args[0];
        int page = 1;

        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
                if (page < 1) page = 1;
            } catch (NumberFormatException ignored) {}
        }

        plugin.getShopManager().openCategory(player, category, page);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> completions = new java.util.ArrayList<>();
            completions.add("reload");
            if (plugin.getShopManager() != null) {
                completions.addAll(plugin.getShopManager().getCategories().keySet());
            }
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}