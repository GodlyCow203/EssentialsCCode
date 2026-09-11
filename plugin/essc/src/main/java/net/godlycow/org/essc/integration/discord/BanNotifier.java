package net.godlycow.org.essc.integration.discord;

import net.godlycow.org.essc.EssentialsC;

import java.awt.Color;
import java.util.UUID;

public class BanNotifier extends DiscordNotifier {

    public BanNotifier(EssentialsC plugin, DiscordSRVHook hook) {
        super(plugin, hook);
    }

    public void notifyBan(UUID targetUUID, String targetName, String reason, String banner, long expires) {
        if (!plugin.getConfigManager().isDiscordBanEnabled()) {
            return;
        }

        String channelName = plugin.getConfigManager().getDiscordSRVPunishmentsChannelName();
        Color color = parseColor(plugin.getConfigManager().getDiscordBanColor());
        String avatarUrl = getAvatarUrl(targetUUID);

        if (expires > 0) {
            long duration = expires - System.currentTimeMillis();
            sendEmbedWithAvatar(channelName, "Player Banned", color, avatarUrl,
                    new EmbedField("Player", targetName, true),
                    new EmbedField("Banned By", banner, true),
                    new EmbedField("Duration", formatDuration(duration), true),
                    new EmbedField("Expires", "<t:" + (expires / 1000) + ":R>", true),
                    new EmbedField("Reason", reason != null && !reason.isEmpty() ? reason : "No reason provided", false)
            );
        } else {
            sendEmbedWithAvatar(channelName, "Player Banned", color, avatarUrl,
                    new EmbedField("Player", targetName, true),
                    new EmbedField("Banned By", banner, true),
                    new EmbedField("Type", "Permanent", true),
                    new EmbedField("Reason", reason != null && !reason.isEmpty() ? reason : "No reason provided", false)
            );
        }
    }

    @Override
    protected String getSuccessMessage(String title) {
        return "Ban embed sent to Discord";
    }
}