package net.godlycow.org.essc.integration.placeholderapi;

import net.godlycow.org.essc.EssentialsC;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class NickPlaceholders {

    private final EssentialsC plugin;

    public NickPlaceholders(EssentialsC plugin) {
        this.plugin = plugin;
    }

    public String onRequest(Player player, String identifier) {
        if (!identifier.startsWith("nick_")) {
            return null;
        }

        return switch (identifier.toLowerCase()) {
            case "nick_name" -> getNickOrFallback(player);
            case "nick_plain" -> getNickPlain(player);
            case "nick_has_nick" -> String.valueOf(plugin.getNickManager().getCachedNickname(player.getUniqueId()) != null);
            case "nick_formatted" -> getNickFormatted(player);
            default -> null;
        };
    }

    private String getNickOrFallback(Player player) {
        String nick = plugin.getNickManager().getCachedNickname(player.getUniqueId());
        return nick != null ? nick : player.getName();
    }

    private String getNickFormatted(Player player) {
        String nick = plugin.getNickManager().getCachedNickname(player.getUniqueId());
        if (nick == null) {
            return player.getName();
        }
        return LegacyComponentSerializer.legacySection().serialize(
                plugin.getMiniMessage().deserialize(nick)
        );
    }

    private String getNickPlain(Player player) {
        String nick = plugin.getNickManager().getCachedNickname(player.getUniqueId());
        if (nick == null) {
            return player.getName();
        }
        return PlainTextComponentSerializer.plainText().serialize(
                plugin.getMiniMessage().deserialize(nick)
        );
    }

    public static List<String> getPlaceholderList() {
        List<String> list = new ArrayList<>();

        list.add("%essc_nick_name% - Returns the player's nickname in MiniMessage format, or their username if none is set");
        list.add("%essc_nick_plain% - Returns the player's nickname as plain text (tags stripped), or their username if none is set");
        list.add("%essc_nick_has_nick% - Returns true if the player has a nickname set, false otherwise");

        return list;
    }
}