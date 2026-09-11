package net.godlycow.org.essc.modules.chat;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.plugin.config.EssConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class MentionHandler {

    private final EssentialsC plugin;
    private final EssConfig config;
    private final MiniMessage miniMessage;

    public MentionHandler(EssentialsC plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.miniMessage = plugin.getMiniMessage();
    }

    public Component applyMentions(Player sender, Component message) {
        if (!sender.hasPermission("essentialsc.chat.mention")) {
            return message;
        }

        String plainMessage = PlainTextComponentSerializer.plainText().serialize(message);
        String plainLower = plainMessage.toLowerCase();
        String mentionFormat = config.getChatMentionFormat();

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.equals(sender))
                continue;

            if (online.hasPermission("essentialsc.chat.mention.bypass"))
                continue;

            String matched = findMention(online, plainMessage, plainLower);
            if (matched == null)
                continue;

            online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);

            Component highlight = miniMessage.deserialize(
                    mentionFormat.replace("<player>", miniMessage.escapeTags(matched)));

            message = message.replaceText(b -> b.matchLiteral(matched).replacement(highlight));
        }

        return message;
    }

    private String findMention(Player player, String plainMessage, String plainLower) {
        String realName = player.getName();
        if (plainLower.contains(realName.toLowerCase())) {
            return realName;

        }

        String cachedNick = plugin.getNickManager() != null
                ? plugin.getNickManager().getCachedNickname(player.getUniqueId())
                : null;

        if (cachedNick != null && !cachedNick.isEmpty()) {
            String plainNick = PlainTextComponentSerializer.plainText().serialize(
                    miniMessage.deserialize(cachedNick));

            if (plainLower.contains(plainNick.toLowerCase())) {

                return plainNick;
            }
        }

        return null;
    }
}
