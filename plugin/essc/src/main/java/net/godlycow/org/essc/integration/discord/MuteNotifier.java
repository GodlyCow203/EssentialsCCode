package net.godlycow.org.essc.integration.discord;

import net.godlycow.org.essc.EssentialsC;

import java.awt.Color;
import java.util.UUID;

public class MuteNotifier extends DiscordNotifier {

    public MuteNotifier(EssentialsC plugin, DiscordSRVHook hook) {
        super(plugin, hook);
    }

    public void notifyMute(UUID targetUUID, String targetName, String reason, String muter, long expires) {
        if (!plugin.getConfigManager().isDiscordMuteEnabled()) {
            return;
        }

        String channelName = plugin.getConfigManager().getDiscordSRVPunishmentsChannelName();
        Color color = parseColor(plugin.getConfigManager().getDiscordMuteColor());
        String avatarUrl = getAvatarUrl(targetUUID);

        if (expires > 0) {
            long duration = expires - System.currentTimeMillis();
            sendEmbedWithAvatar(channelName, "Player Muted", color, avatarUrl,
                    new EmbedField("Player", targetName, true),
                    new EmbedField("Muted By", muter, true),
                    new EmbedField("Duration", formatDuration(duration), true),
                    new EmbedField("Expires", "<t:" + (expires / 1000) + ":R>", true),
                    new EmbedField("Reason", reason != null && !reason.isEmpty() ? reason : "No reason provided", false)
            );
        } else {
            sendEmbedWithAvatar(channelName, "Player Muted", color, avatarUrl,
                    new EmbedField("Player", targetName, true),
                    new EmbedField("Muted By", muter, true),
                    new EmbedField("Type", "Permanent", true),
                    new EmbedField("Reason", reason != null && !reason.isEmpty() ? reason : "No reason provided", false)
            );
        }
    }

    @Override
    protected String getSuccessMessage(String title) {
        return "Mute embed sent to Discord";
    }
}