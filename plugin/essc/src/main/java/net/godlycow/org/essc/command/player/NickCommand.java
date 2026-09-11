package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NickCommand extends Command {

    public NickCommand(EssentialsC plugin) {
        super(plugin, "nick", "essentialsc.nick", true, 0, "command.usage.nick");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (plugin.getBedrockUtil().isBedrockPlayer(player)) {
            player.sendMessage(lang.get(player, "nick.error.bedrock_not_supported"));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        // Single arg: /nick <name> or /nick off|clear
        if (args.length == 1) {
            String arg = args[0];
            if (arg.equalsIgnoreCase("off") || arg.equalsIgnoreCase("clear")) {
                return handleReset(player, player, true);
            }
            return handleSet(player, player, arg, true);
        }

        // Two+ args: /nick <target> <name> or /nick <target> off|clear
        if (!player.hasPermission("essentialsc.nick.others")) {
            player.sendMessage(lang.get(player, "error.no_permission"));
            return true;
        }

        Player target = plugin.getBedrockUtil().resolvePlayer(args[0]);
        if (target == null) {
            player.sendMessage(lang.get(player, "error.player_not_found",
                    Map.of("player", args[0])));
            return true;
        }

        if (plugin.getBedrockUtil().isBedrockPlayer(target)) {
            player.sendMessage(lang.get(player, "nick.error.bedrock_not_supported_other",
                    Map.of("player", target.getName())));
            return true;
        }

        String nickArg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (nickArg.equalsIgnoreCase("off") || nickArg.equalsIgnoreCase("clear")) {
            return handleReset(player, target, false);
        }

        return handleSet(player, target, nickArg, false);
    }

    // ── Set Nickname ────────────────────────────────────────────────

    private boolean handleSet(Player sender, Player target, String input, boolean self) {
        String processed = processInput(input, sender);
        if (!validate(sender, processed)) return true;

        if (self && !sender.getUniqueId().equals(target.getUniqueId())) {
            // This shouldn't happen, but safety check
        }

        UUID targetUuid = target.getUniqueId();

        if (plugin.getConfigManager().isNickUnique()) {
            plugin.getNickManager().isNicknameTaken(processed, targetUuid).thenAccept(taken -> {
                sender.getScheduler().run(plugin, task -> {
                    if (taken) {
                        sender.sendMessage(lang.get(sender, "nick.error.exists"));
                        return;
                    }
                    applyNick(sender, target, processed, self);
                }, null);
            });
        } else {
            applyNick(sender, target, processed, self);
        }

        return true;
    }

    private void applyNick(Player sender, Player target, String nickname, boolean self) {
        plugin.getNickManager().setNickname(target.getUniqueId(), nickname).thenRun(() -> {
            sender.getScheduler().run(plugin, task -> {
                plugin.getNickManager().applyNickname(target);
                String plainNick = PlainTextComponentSerializer.plainText().serialize(
                        plugin.getMiniMessage().deserialize(nickname));

                if (self) {
                    sender.sendMessage(lang.get(sender, "nick.success.self",
                            Map.of("nick", plainNick)));
                } else {
                    sender.sendMessage(lang.get(sender, "nick.success.other",
                            Map.of("target", target.getName(), "nick", plainNick)));
                    target.sendMessage(lang.get(target, "nick.success.set.by",
                            Map.of("admin", sender.getName(), "nick", plainNick)));
                }
            }, null);
        });
        return;
    }

    // ── Reset Nickname ──────────────────────────────────────────────

    private boolean handleReset(Player sender, Player target, boolean self) {
        if (!self && !sender.hasPermission("essentialsc.nick.reset")) {
            sender.sendMessage(lang.get(sender, "error.no_permission"));
            return true;
        }

        plugin.getNickManager().removeNickname(target.getUniqueId()).thenRun(() -> {
            sender.getScheduler().run(plugin, task -> {
                plugin.getNickManager().clearNickname(target);

                if (self) {
                    sender.sendMessage(lang.get(sender, "nick.success.reset.self"));
                } else {
                    sender.sendMessage(lang.get(sender, "nick.success.reset.other",
                            Map.of("target", target.getName())));
                    target.sendMessage(lang.get(target, "nick.success.reset.by",
                            Map.of("admin", sender.getName())));
                }
            }, null);
        });

        return true;
    }

    // ── Input Processing ────────────────────────────────────────────

    private String processInput(String input, Player player) {
        if (!player.hasPermission("essentialsc.nick.color")) {
            input = input.replaceAll("(?i)[&§][0-9a-f]", "")
                    .replaceAll("(?i)[&§]x([&§][0-9a-f]){6}", "");
        }
        if (!player.hasPermission("essentialsc.nick.format")) {
            input = input.replaceAll("(?i)[&§][klmnor]", "");
        }
        if (!player.hasPermission("essentialsc.nick.minimessage")) {
            input = plugin.getMiniMessage().escapeTags(input);
        }
        return input;
    }

    // ── Validation ──────────────────────────────────────────────────

    private boolean validate(Player player, String nickname) {
        String plain = PlainTextComponentSerializer.plainText().serialize(
                plugin.getMiniMessage().deserialize(nickname));

        int min = plugin.getConfigManager().getNickMinLength();
        int max = plugin.getConfigManager().getNickMaxLength();

        if (plain.length() < min) {
            player.sendMessage(lang.get(player, "nick.error.too_short",
                    Map.of("min", String.valueOf(min), "current", String.valueOf(plain.length()))));
            return false;
        }

        if (plain.length() > max) {
            player.sendMessage(lang.get(player, "nick.error.too_long",
                    Map.of("max", String.valueOf(max), "current", String.valueOf(plain.length()))));
            return false;
        }

        if (plugin.getConfigManager().isNickBlacklistEnabled()) {
            String check = plugin.getConfigManager().isNickNormalizeEnabled()
                    ? plain.toLowerCase() : plain;
            for (String word : plugin.getConfigManager().getNickBlacklistWords()) {
                if (check.contains(word.toLowerCase())) {
                    player.sendMessage(lang.get(player, "nick.error.blacklisted",
                            Map.of("word", word)));
                    return false;
                }
            }
        }

        return true;
    }

    // ── Tab Complete ────────────────────────────────────────────────

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("essentialsc.nick.others")) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
