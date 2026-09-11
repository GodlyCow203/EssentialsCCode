package net.godlycow.org.essc.language;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.godlycow.org.essc.storage.user.UserProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LanguageCommand extends Command {

    public LanguageCommand(EssentialsC plugin) {
        super(plugin, "language", null, true);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length == 0) {
            sender.sendMessage(getCurrentLanguage(player));
            showAvailableLanguages(sender);
            return true;
        }


        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "set" -> {
                if (args.length < 2) {
                    sendUsage(sender);
                    return true;
                }
                String langCode = args[1];
                setLanguage(player, langCode);
            }
            case "reset" -> {
                resetLanguage(player);
            }
            case "list" -> {
                showAvailableLanguages(sender);
            }
            case "help" -> {
                showHelp(sender);
            }
            default -> {
                if (isValidLanguage(subCommand)) {
                    setLanguage(player, subCommand);
                } else {
                    sender.sendMessage(lang.get(sender, "language.error.invalid_lang", Map.of("lang", subCommand)));
                }
            }
        }

        return true;
    }

    private Component getCurrentLanguage(Player player) {
        LanguageManager lm = plugin.getLanguageManager();
        String playerLang = lm.getPlayerLanguage(player.getUniqueId());

        String langStr = (playerLang != null) ? playerLang + " (custom)" : player.locale().toString() + " (auto)";

        return lm.get(player, "language.current").replaceText(t -> t.matchLiteral("<language>").replacement(langStr));
    }



    private void setLanguage(Player player, String langCode) {
        if (!isValidLanguage(langCode)) {
            player.sendMessage(lang.get(player, "language.error.not_found", Map.of("lang", langCode)));
            return;
        }

        plugin.getLanguageManager().setPlayerLanguage(player.getUniqueId(), langCode);

        UserProfile profile = plugin.getUserManager().getCachedProfile(player.getUniqueId());
        if (profile != null) {
            profile.setLanguageCode(langCode);
            plugin.getUserManager().saveAsync(profile); //save language to db
        }

        player.sendMessage(lang.get(player, "language.set.success", Map.of("language", langCode)));
        plugin.debug("Player " + player.getName() + " set language to: " + langCode);
    }

    private void resetLanguage(Player player) {
        plugin.getLanguageManager().removePlayerLanguage(player.getUniqueId());

        UserProfile profile = plugin.getUserManager().getCachedProfile(player.getUniqueId());
        if (profile != null) {
            profile.setLanguageCode(null);
            plugin.getUserManager().saveAsync(profile);// same thing here
        }

        player.sendMessage(lang.get(player, "language.reset.success", Map.of("language", player.locale().toString())));
        plugin.debug("Player " + player.getName() + " reset language to auto");
    }

    private void showAvailableLanguages(CommandSender sender) {
        List<String> available = getAvailableLanguages();
        String langList = String.join("<gray>, </gray><yellow>", available);
        sender.sendMessage(lang.get(sender, "language.list.header"));
        sender.sendMessage(lang.get(sender, "language.list.available", Map.of("languages", langList)));
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(lang.get(sender, "language.help.header"));
        sender.sendMessage(lang.get(sender, "language.help.set"));
        sender.sendMessage(lang.get(sender, "language.help.reset"));
        sender.sendMessage(lang.get(sender, "language.help.list"));
        sender.sendMessage(lang.get(sender, "language.help.current"));
    }

    private boolean isValidLanguage(String langCode) {
        File langFile = new File(plugin.getDataFolder(), "lang/" + langCode + ".json");
        return langFile.exists();
    }

    private List<String> getAvailableLanguages() {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists() || !langFolder.isDirectory()) {
            return new ArrayList<>();
        }

        File[] files = langFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return new ArrayList<>();

        return Arrays.stream(files)
                .map(f -> f.getName().replace(".json", ""))
                .collect(Collectors.toList());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("set", "reset", "list", "help"));
            completions.addAll(getAvailableLanguages());
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return getAvailableLanguages().stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return super.tabComplete(sender, args);
    }
}