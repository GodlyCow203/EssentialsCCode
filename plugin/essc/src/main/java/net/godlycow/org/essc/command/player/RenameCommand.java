package net.godlycow.org.essc.command.player;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.command.Command;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class RenameCommand extends Command {
    private final MiniMessage miniMessage;
    private final Pattern tagPattern = Pattern.compile("<[^>]+>");
    private List<String> blacklist;
    private boolean blacklistEnabled;
    private int maxLength;
    private int minLength;
    private boolean normalizeEnabled;
    private boolean stripColorsEnabled;

    public RenameCommand(EssentialsC plugin) {
        super(plugin, "rename", "essentialsc.rename", true, 1, "command.usage.rename");
        this.miniMessage = plugin.getMiniMessage();
        loadConfig();
    }

    public void loadConfig() {
        this.blacklistEnabled = plugin.getConfigManager().isRenameBlacklistEnabled();
        this.blacklist = plugin.getConfigManager().getRenameBlacklistWords();
        this.maxLength = plugin.getConfigManager().getRenameMaxLength();
        this.minLength = plugin.getConfigManager().getRenameMinLength();
        this.normalizeEnabled = plugin.getConfigManager().isRenameNormalizeEnabled();
        this.stripColorsEnabled = plugin.getConfigManager().isRenameStripColorsEnabled();
        plugin.debug("Loaded rename config: max=" + maxLength + ", min=" + minLength +
                ", normalize=" + normalizeEnabled + ", blacklist=" + blacklist.size() + " words");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        String newName = String.join(" ", args);

        String checkString = stripColorsEnabled ? tagPattern.matcher(newName).replaceAll("") : newName;

        if (checkString.length() > maxLength) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("max", String.valueOf(maxLength));
            placeholders.put("current", String.valueOf(checkString.length()));
            player.sendMessage(lang.get(player, "rename.too_long", placeholders));
            return true;
        }

        if (checkString.length() < minLength) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("min", String.valueOf(minLength));
            placeholders.put("current", String.valueOf(checkString.length()));
            player.sendMessage(lang.get(player, "rename.too_short", placeholders));
            return true;
        }

        if (blacklistEnabled && !player.hasPermission("essentialsc.rename.bypass")) {
            String normalizedName = normalizeEnabled ? normalizeString(checkString) : checkString.toLowerCase();

            for (String word : blacklist) {
                String checkWord = normalizeEnabled ? normalizeString(word) : word.toLowerCase();

                if (checkWord.length() > 0 && normalizedName.contains(checkWord)) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("word", word);
                    player.sendMessage(lang.get(player, "rename.blacklisted", placeholders));
                    plugin.debug(player.getName() + " tried to use blacklisted word: " + word +
                            " | Input: " + newName + " | Normalized: " + normalizedName);
                    return true;
                }
            }
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(lang.get(player, "rename.no_item"));
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            player.sendMessage(lang.get(player, "rename.cannot_rename"));
            return true;
        }

        Component displayName = miniMessage.deserialize(newName);
        meta.displayName(displayName);
        item.setItemMeta(meta);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("name", newName);
        player.sendMessage(lang.get(player, "rename.success", placeholders));
        plugin.debug(player.getName() + " renamed item to: " + newName);

        return true;
    }

    private String normalizeString(String input) {
        if (input == null) return "";

        String normalized = input.toLowerCase();

        normalized = normalized.replace("0", "o")
                .replace("1", "i")
                .replace("3", "e")
                .replace("4", "a")
                .replace("5", "s")
                .replace("7", "t")
                .replace("2", "z");

        normalized = normalized.replace("@", "a")
                .replace("$", "s")
                .replace("!", "i")
                .replace("|", "i")
                .replace("+", "t")
                .replace("(", "c")
                .replace("[", "c")
                .replace("{", "c")
                .replace(")", "c")
                .replace("]", "c")
                .replace("}", "c");

        return normalized.replaceAll("[^a-z0-9]", "");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}