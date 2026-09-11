package net.godlycow.org.essc.integration.discord;

import net.godlycow.org.essc.EssentialsC;

import java.awt.Color;
import java.util.UUID;

public class HomeNotifier extends DiscordNotifier {

    public HomeNotifier(EssentialsC plugin, DiscordSRVHook hook) {
        super(plugin, hook);
    }

    public void notifyHomeSet(UUID playerUUID, String playerName, String homeName, String worldName, int homeCount, int maxHomes) {
        if (!plugin.getConfigManager().isDiscordHomeEnabled()) {
            return;
        }

        String channelName = plugin.getConfigManager().getDiscordSRVPunishmentsChannelName();
        Color color = parseColor(plugin.getConfigManager().getDiscordHomeColor());
        String avatarUrl = getAvatarUrl(playerUUID);

        String limitText = maxHomes == Integer.MAX_VALUE ? "Unlimited" : homeCount + "/" + maxHomes;

        sendEmbedWithAvatar(channelName, "Home Set", color, avatarUrl,
                new EmbedField("Player", playerName, true),
                new EmbedField("Home Name", homeName, true),
                new EmbedField("World", worldName, true),
                new EmbedField("Home Count", limitText, true)
        );
    }

    @Override
    protected String getSuccessMessage(String title) {
        return "Home set embed sent to Discord";
    }
}