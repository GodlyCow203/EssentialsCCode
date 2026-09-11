package net.godlycow.org.essc.command.admin;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RealNameCommand extends Command {

    public RealNameCommand(EssentialsC plugin) {
        super(plugin, "realname", "essentialsc.realname", false, 1, "command.usage.realname");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        String query = args[0];

        Player target = null;
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            String display = online.getDisplayName();
            if (display.equalsIgnoreCase(query) || online.getName().equalsIgnoreCase(query)) {
                target = online;
                break;
            }
        }

        if (target != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("nick", target.getDisplayName());
            placeholders.put("real", target.getName());
            sender.sendMessage(lang.get(sender, "realname.success", placeholders));
            plugin.debug(sender.getName() + " looked up realname for: " + query);
            return true;
        }

        plugin.getNickManager().getUUIDByNickname(query).thenAccept(opt -> {
            plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
                if (opt.isPresent()) {
                    UUID uuid = opt.get();
                    String name = plugin.getServer().getOfflinePlayer(uuid).getName();
                    if (name != null) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("nick", query);
                        placeholders.put("real", name);
                        sender.sendMessage(lang.get(sender, "realname.success", placeholders));
                    } else {
                        sender.sendMessage(lang.get(sender, "realname.error.not_found"));
                    }
                } else {
                    sender.sendMessage(lang.get(sender, "realname.error.not_found"));
                }
            });
        });

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getDisplayName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}