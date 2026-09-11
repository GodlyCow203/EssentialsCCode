package net.godlycow.org.essc.modules.chat;

import me.clip.placeholderapi.PlaceholderAPI;
import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.plugin.config.EssConfig;
import net.godlycow.org.essc.util.LegacyColorConverter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFormatter {

    private final EssentialsC plugin;
    private final EssConfig config;
    private final MiniMessage miniMessage;

    private LuckPerms luckPerms;
    private boolean luckPermsEnabled;

    public ChatFormatter(EssentialsC plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.miniMessage = plugin.getMiniMessage();
    }

    public void reload(LuckPerms luckPerms, boolean luckPermsEnabled) {
        this.luckPerms = luckPerms;
        this.luckPermsEnabled = luckPermsEnabled;
    }

    public boolean isLuckPermsEnabled() {
        return luckPermsEnabled;
    }


    public Component formatMessage(Player player, String raw) {

        raw = applyFormatPermissions(player, raw);

        List<String> urls = new ArrayList<>();
        Matcher urlMatcher = Pattern.compile(
                "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+").matcher(raw);
        while (urlMatcher.find()) {
            urls.add(urlMatcher.group());
        }

        String placeholderRaw = raw;
        for (int i = 0; i < urls.size(); i++) {
            placeholderRaw = placeholderRaw.replace(urls.get(i), "\u0000LINK_" + i + "\u0000");
        }

        Component message = buildComponent(player, placeholderRaw);
        message = processLinks(player, message, urls);

        return message;
    }

    private String applyFormatPermissions(Player player, String input) {
        if (!player.hasPermission("essentialsc.chat.color")) {
            return stripAllColors(input);
        }

        if (!player.hasPermission("essentialsc.chat.format.bold")) {
            input = Pattern.compile("(?i)[&§]l|</?(?:bold|b)>").matcher(input).replaceAll("");
        }
        if (!player.hasPermission("essentialsc.chat.format.italic")) {
            input = Pattern.compile("(?i)[&§]o|</?(?:italic|i|em)>").matcher(input).replaceAll("");
        }
        if (!player.hasPermission("essentialsc.chat.format.underline")) {
            input = Pattern.compile("(?i)[&§]n|</?(?:underlined|underline|u)>").matcher(input).replaceAll("");
        }
        if (!player.hasPermission("essentialsc.chat.format.strikethrough")) {
            input = Pattern.compile("(?i)[&§]m|</?(?:strikethrough|st)>").matcher(input).replaceAll("");
        }
        if (!player.hasPermission("essentialsc.chat.format.obfuscated")) {
            input = Pattern.compile("(?i)[&§]k|</?(?:obfuscated|obf)>").matcher(input).replaceAll("");
        }

        return input;
    }

    private String stripAllColors(String input) {
        input = input.replaceAll("(?i)[&§][0-9a-fk-or]", "");
        input = input.replaceAll("&#[A-Fa-f0-9]{6}", "");
        input = input.replaceAll("<[^>]+>", "");
        return input;
    }

    private Component buildComponent(Player player, String raw) {
        if (!player.hasPermission("essentialsc.chat.color")) {
            return Component.text(raw);
        }

        boolean canMiniMessage = player.hasPermission("essentialsc.chat.minimessage");
        boolean canRgb = player.hasPermission("essentialsc.chat.rgbcodes");
        boolean canLegacy = player.hasPermission("essentialsc.chat.legacycodes");

        if (canMiniMessage) {
            if (canRgb || canLegacy) {
                return miniMessage.deserialize(LegacyColorConverter.toMiniMessage(raw));
            }
            return miniMessage.deserialize(raw);
        }

        if (canRgb || canLegacy) {
            String converted = LegacyColorConverter.convertBukkitHexToAmpersandHex(raw);
            return LegacyComponentSerializer.legacyAmpersand().deserialize(converted);
        }

        return Component.text(raw);
    }


    private Component processLinks(Player player, Component message, List<String> urls) {
        if (urls.isEmpty())

            return message;

        List<String> whitelist = config.getChatLinkWhitelist();
        Component removalComponent = miniMessage.deserialize(config.getChatLinkRemovalMessage());

        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            String token = "\u0000LINK_" + i + "\u0000";
            if (player.hasPermission("essentialsc.chat.links")) {
                Component linked = Component.text(url)
                        .clickEvent(ClickEvent.openUrl(url))
                        .color(NamedTextColor.AQUA)
                        .decorate(TextDecoration.UNDERLINED);
                message = message.replaceText(b -> b.matchLiteral(token).replacement(linked));
            } else {
                boolean whitelisted = whitelist.stream().anyMatch(url::contains);
                if (!whitelisted) {
                    message = message.replaceText(b -> b.matchLiteral(token).replacement(removalComponent));
                } else {
                    message = message.replaceText(b -> b.matchLiteral(token).replacement(Component.text(url)));
                }
            }
        }

        return message;
    }

    public Component buildChatLine(Player player, Component message) {
        CachedMetaData meta = luckPerms.getPlayerAdapter(Player.class).getMetaData(player);

        String format = plugin.getConfig().getString(
                "luckperms.group-formats." + meta.getPrimaryGroup());
        if (format == null) {
            format = plugin.getConfig().getString(
                    "luckperms.chat-format", "<DISPLAYNAME> &7&raquo; &f<MESSAGE>");
        }

        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            format = PlaceholderAPI.setPlaceholders(player, format);
        }

        //build all components placeholders before format processing
        String lpPrefix = meta.getPrefix() != null ?
                meta.getPrefix() : "";
        String lpSuffix = meta.getSuffix() != null ?
                meta.getSuffix() : "";

        Component prefixComp = lpPrefix.isEmpty()
                ? Component.empty()
                : LegacyColorConverter.toComponent(lpPrefix);
        Component suffixComp = lpSuffix.isEmpty()
                ? Component.empty()
                : LegacyColorConverter.toComponent(lpSuffix);
        Component displayName = buildDisplayName(player, meta);

        format = format
                .replace("<PREFIX>", "\u0000PREFIX\u0000")
                .replace("<SUFFIX>", "\u0000SUFFIX\u0000")
                .replace("<USERNAME>", miniMessage.escapeTags(player.getName()))
                .replace("<GROUP>", miniMessage.escapeTags(meta.getPrimaryGroup()))
                .replace("<DISPLAYNAME>", "\u0000DISPLAYNAME\u0000")
                .replace("<MESSAGE>", "\u0000MESSAGE\u0000");

        //deserialize
        Component result = miniMessage.deserialize(LegacyColorConverter.toMiniMessage(format));

        result = result.replaceText(b -> b.matchLiteral("\u0000PREFIX\u0000").replacement(prefixComp));
        result = result.replaceText(b -> b.matchLiteral("\u0000SUFFIX\u0000").replacement(suffixComp));
        result = result.replaceText(b -> b.matchLiteral("\u0000DISPLAYNAME\u0000").replacement(displayName));
        result = result.replaceText(b -> b.matchLiteral("\u0000MESSAGE\u0000").replacement(message));

        return result;
    }

    private Component buildDisplayName(Player player, CachedMetaData meta) {
        String lpPrefix = meta.getPrefix() != null ? meta.getPrefix() : "";
        String lpSuffix = meta.getSuffix() != null ? meta.getSuffix() : "";

        String cachedNick = plugin.getNickManager() != null
                ? plugin.getNickManager().getCachedNickname(player.getUniqueId())
                : null;

        //build name component
        Component nameComponent;
        if (cachedNick != null && !cachedNick.isEmpty()) {
            Component nickComponent = miniMessage.deserialize(cachedNick);
            String indicator = config.getNickIndicator();
            if (!indicator.isEmpty()) {
                nickComponent = Component.text(indicator).append(nickComponent);
            }
            nameComponent = nickComponent;
        } else {
            nameComponent = Component.text(player.getName());
        }

        Component prefixComponent = lpPrefix.isEmpty()
                ? Component.empty()
                : LegacyColorConverter.toComponent(lpPrefix);

        Component suffixComponent = lpSuffix.isEmpty()
                ? Component.empty()
                : LegacyColorConverter.toComponent(lpSuffix);

        Component displayName = prefixComponent.append(nameComponent).append(suffixComponent);

        if (config.isNickShowRealnameOnHover() && cachedNick != null && !cachedNick.isEmpty()) {
            String plainNick = PlainTextComponentSerializer.plainText().serialize(
                    miniMessage.deserialize(cachedNick));
            String hoverText = config.getNickHoverFormat()
                    .replace("<realname>", miniMessage.escapeTags(player.getName()))
                    .replace("<nick>", miniMessage.escapeTags(plainNick))
                    .replace("<prefix>", miniMessage.escapeTags(lpPrefix))
                    .replace("<suffix>", miniMessage.escapeTags(lpSuffix));
            displayName = displayName.hoverEvent(
                    HoverEvent.showText(miniMessage.deserialize(hoverText)));
        }

        //suggest msg command
        if (config.isNickClickSuggestMsg()) {
            displayName = displayName.clickEvent(
                    ClickEvent.suggestCommand("/msg " + player.getName() + " "));
        }

        return displayName;
    }
}
