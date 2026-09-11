package net.godlycow.org.essc.integration.discord;

import net.godlycow.org.essc.EssentialsC;

import java.awt.Color;
import java.util.UUID;

public class KickNotifier extends DiscordNotifier {

    public KickNotifier(EssentialsC plugin, DiscordSRVHook hook) {
        super(plugin, hook);
    }

    public void notifyKick(UUID targetUUID, String targetName, String reason, String kicker) {
        if (!plugin.getConfigManager().isDiscordKickEnabled()) {
            return;
        }

        String channelName = plugin.getConfigManager().getDiscordSRVPunishmentsChannelName();
        Color color = parseColor(plugin.getConfigManager().getDiscordKickColor());
        String avatarUrl = getAvatarUrl(targetUUID);

        sendEmbedWithAvatar(channelName, "Player Kicked", color, avatarUrl,
                new EmbedField("Player", targetName, true),
                new EmbedField("Kicked By", kicker, true),
                new EmbedField("Reason", reason != null && !reason.isEmpty() ? reason : "No reason provided", false)
        );
    }

    @Override
    protected String getSuccessMessage(String title) {
        return "Kick embed sent to Discord";
    }
}