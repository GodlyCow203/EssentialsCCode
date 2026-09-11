package net.godlycow.org.essc.integration.discord;

import net.godlycow.org.essc.EssentialsC;

import java.awt.Color;

public class BanIpNotifier extends DiscordNotifier {

    public BanIpNotifier(EssentialsC plugin, DiscordSRVHook hook) {
        super(plugin, hook);
    }

    public void notifyBanIp(String ip, String targetName, String reason, String banner, long expires) {
        if (!plugin.getConfigManager().isDiscordBanIpEnabled()) {
            return;
        }

        String channelName = plugin.getConfigManager().getDiscordSRVPunishmentsChannelName();
        Color color = parseColor(plugin.getConfigManager().getDiscordBanIpColor());

        String avatarUrl = "https://crafthead.net/helm/00000000-0000-0000-0000-000000000000";

        if (expires > 0) {
            long duration = expires - System.currentTimeMillis();
            sendEmbedWithAvatar(channelName, "IP Banned", color, avatarUrl,
                    new EmbedField("IP Address", "`" + ip + "`", true),
                    new EmbedField("Resolved From", targetName != null ? targetName : "Direct IP", true),
                    new EmbedField("Banned By", banner, true),
                    new EmbedField("Duration", formatDuration(duration), true),
                    new EmbedField("Expires", "<t:" + (expires / 1000) + ":R>", true),
                    new EmbedField("Type", "Temporary IP Ban", true),
                    new EmbedField("Reason", reason != null && !reason.isEmpty() ? reason : "No reason provided", false)
            );
        } else {
            sendEmbedWithAvatar(channelName, "IP Banned", color, avatarUrl,
                    new EmbedField("IP Address", "`" + ip + "`", true),
                    new EmbedField("Resolved From", targetName != null ? targetName : "Direct IP", true),
                    new EmbedField("Banned By", banner, true),
                    new EmbedField("Type", "Permanent IP Ban", true),
                    new EmbedField("Reason", reason != null && !reason.isEmpty() ? reason : "No reason provided", false)
            );
        }
    }

    @Override
    protected String getSuccessMessage(String title) {
        return "IP Ban embed sent to Discord";
    }
}