package net.godlycow.org.essc.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LegacyColorConverter {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Pattern HEX_AMPERSAND = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_BUKKIT = Pattern.compile("&x([&][0-9A-Fa-f]){6}");
    private static final Pattern HEX_SECTION = Pattern.compile("§x([§][0-9A-Fa-f]){6}");

    private LegacyColorConverter() {}


    public static String toMiniMessage(String input) {
        if (input == null || input.isEmpty()) return input;
        String result = input;
        result = convertHexAmpersand(result);
        result = convertHexBukkit(result);
        result = convertHexSection(result);
        result = convertAmpersand(result);
        result = convertSection(result);
        return result;
    }


    public static Component toComponent(String input) {
        if (input == null) return Component.empty();
        return MM.deserialize(toMiniMessage(input));
    }


    public static String convertHexAmpersandToLegacy(String input) {
        if (input == null || !input.contains("&#")) return input;
        Matcher m = HEX_AMPERSAND.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String hex = m.group(1);
            StringBuilder legacy = new StringBuilder("&x");
            for (char c : hex.toCharArray()) {
                legacy.append("&").append(c);
            }
            m.appendReplacement(sb, legacy.toString());
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static String strip(String input) {
        if (input == null || input.isEmpty()) return input;
        String miniMessage = toMiniMessage(input);
        return miniMessage.replaceAll("<[^>]+>", "").replace("&&", "&");
    }

    public static String convertHexAmpersand(String input) {
        if (input == null || !input.contains("&#")) return input;
        Matcher m = HEX_AMPERSAND.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, "<#" + m.group(1) + ">");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static String convertBukkitHexToAmpersandHex(String input) {
        if (input == null || !input.contains("&x")) return input;
        Matcher m = HEX_BUKKIT.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String hex = m.group().replace("&x", "").replace("&", "");
            m.appendReplacement(sb, "&#" + hex);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static String convertHexBukkit(String input) {
        if (input == null || !input.contains("&x")) return input;
        Matcher m = HEX_BUKKIT.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String hex = m.group().replace("&x", "").replace("&", "");
            m.appendReplacement(sb, "<#" + hex + ">");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static String convertHexSection(String input) {
        if (input == null || !input.contains("§x")) return input;
        Matcher m = HEX_SECTION.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String hex = m.group().replace("§x", "").replace("§", "");
            m.appendReplacement(sb, "<#" + hex + ">");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static String convertAmpersand(String input) {
        if (input == null) return null;
        return input
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>").replace("&A", "<green>")
                .replace("&b", "<aqua>").replace("&B", "<aqua>")
                .replace("&c", "<red>").replace("&C", "<red>")
                .replace("&d", "<light_purple>").replace("&D", "<light_purple>")
                .replace("&e", "<yellow>").replace("&E", "<yellow>")
                .replace("&f", "<white>").replace("&F", "<white>")
                .replace("&k", "<obfuscated>").replace("&K", "<obfuscated>")
                .replace("&l", "<bold>").replace("&L", "<bold>")
                .replace("&m", "<strikethrough>").replace("&M", "<strikethrough>")
                .replace("&n", "<underlined>").replace("&N", "<underlined>")
                .replace("&o", "<italic>").replace("&O", "<italic>")
                .replace("&r", "<reset>").replace("&R", "<reset>")
                .replace("&&", "&");
    }

    public static String convertSection(String input) {
        if (input == null) return null;
        return input
                .replace("§0", "<black>")
                .replace("§1", "<dark_blue>")
                .replace("§2", "<dark_green>")
                .replace("§3", "<dark_aqua>")
                .replace("§4", "<dark_red>")
                .replace("§5", "<dark_purple>")
                .replace("§6", "<gold>")
                .replace("§7", "<gray>")
                .replace("§8", "<dark_gray>")
                .replace("§9", "<blue>")
                .replace("§a", "<green>").replace("§A", "<green>")
                .replace("§b", "<aqua>").replace("§B", "<aqua>")
                .replace("§c", "<red>").replace("§C", "<red>")
                .replace("§d", "<light_purple>").replace("§D", "<light_purple>")
                .replace("§e", "<yellow>").replace("§E", "<yellow>")
                .replace("§f", "<white>").replace("§F", "<white>")
                .replace("§k", "<obfuscated>").replace("§K", "<obfuscated>")
                .replace("§l", "<bold>").replace("§L", "<bold>")
                .replace("§m", "<strikethrough>").replace("§M", "<strikethrough>")
                .replace("§n", "<underlined>").replace("§N", "<underlined>")
                .replace("§o", "<italic>").replace("§O", "<italic>")
                .replace("§r", "<reset>").replace("§R", "<reset>");
    }

    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();

    public static Component fromLegacyAmpersand(String input) {
        return LEGACY_AMPERSAND.deserialize(input != null ? input : "");
    }

    public static Component fromLegacySection(String input) {
        return LEGACY_SECTION.deserialize(input != null ? input : "");
    }

    public static String toLegacySection(Component component) {
        return LEGACY_SECTION.serialize(component);
    }
}