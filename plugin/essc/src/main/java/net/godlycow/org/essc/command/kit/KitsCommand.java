package net.godlycow.org.essc.command.kit;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.modules.kit.Kit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KitsCommand extends Command {

    public KitsCommand(EssentialsC plugin) {
        super(plugin, "kits", null, false, 0);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showList(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "list" -> showList(sender);
            case "reload" -> {
                if (!sender.hasPermission("essentialsc.kits.admin")) {
                    sender.sendMessage(lang.get(sender, "error.no_permission"));
                    return true;
                }
                plugin.getKitManager().reload();
                sender.sendMessage(lang.get(sender, "kits.reload.success"));
            }
            case "help" -> showHelp(sender);
            default -> sender.sendMessage(lang.get(sender, "kits.error.unknown"));
        }

        return true;
    }

    private void showList(CommandSender sender) {
        if (plugin.getConfigManager().isKitGuiMode() && sender instanceof Player player
                && plugin.getKitGuiManager() != null) {
            plugin.getKitGuiManager().openKitList(player, 1);
            return;
        }

        var kits = plugin.getKitManager().getKits();

        if (kits.isEmpty()) {
            sender.sendMessage(lang.get(sender, "kits.list.no_kits"));
            return;
        }

        sender.sendMessage(lang.get(sender, "kits.list.header", Map.of("count", String.valueOf(kits.size()))));

        for (Kit kit : kits) {
            boolean hasPerm = !(sender instanceof Player) ||
                    plugin.getKitManager().hasPermission((Player) sender, kit);
            boolean canClaim = !(sender instanceof Player) ||
                    plugin.getKitManager().canClaim((Player) sender, kit);

            String status = hasPerm ? (canClaim ? "<green>✔</green>" : "<yellow>⌛</yellow>") : "<red>✖</red>";

            if (sender instanceof Player player && player.hasPermission("essentialsc.kits.admin")) {
                sender.sendMessage(lang.get(sender, "kits.list.entry.admin",
                        Map.of("status", status, "kit", kit.getDisplayName(), "name", kit.getName(),
                                "perm", kit.getPermission())));
            } else {
                sender.sendMessage(lang.get(sender, "kits.list.entry",
                        Map.of("status", status, "kit", kit.getDisplayName(), "name", kit.getName())));
            }
        }
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(lang.get(sender, "kits.help.header"));
        sender.sendMessage(lang.get(sender, "kits.help.list"));
        sender.sendMessage(lang.get(sender, "kits.help.claim"));
        sender.sendMessage(lang.get(sender, "kits.help.cooldown"));
        if (sender.hasPermission("essentialsc.kits.admin")) {
            sender.sendMessage(lang.get(sender, "kits.help.reload"));
            sender.sendMessage(lang.get(sender, "kits.help.debug"));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> subs = new java.util.ArrayList<>(List.of("list", "help"));
            if (sender.hasPermission("essentialsc.kits.admin")) {
                subs.add("reload");
            }
            return subs.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }
}