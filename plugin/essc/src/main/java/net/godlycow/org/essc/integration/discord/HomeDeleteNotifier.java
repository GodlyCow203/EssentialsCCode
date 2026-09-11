package net.godlycow.org.essc.integration.discord;

import net.godlycow.org.essc.EssentialsC;

import java.awt.Color;
import java.util.UUID;

public class HomeDeleteNotifier extends DiscordNotifier {

    public HomeDeleteNotifier(EssentialsC plugin, DiscordSRVHook hook) {
        super(plugin, hook);
    }

    public void notifyHomeDelete(UUID playerUUID, String playerName, String homeName, int remainingHomes, int maxHomes) {
        if (!plugin.getConfigManager().isDiscordHomeDeleteEnabled()) {
            return;
        }

        String channelName = plugin.getConfigManager().getDiscordSRVPunishmentsChannelName();
        Color color = parseColor(plugin.getConfigManager().getDiscordHomeDeleteColor());
        String avatarUrl = getAvatarUrl(playerUUID);

        String limitText = maxHomes == Integer.MAX_VALUE ? "Unlimited" : remainingHomes + "/" + maxHomes;

        sendEmbedWithAvatar(channelName, "Home Deleted", color, avatarUrl,
                new EmbedField("Player", playerName, true),
                new EmbedField("Home Name", homeName, true),
                new EmbedField("Remaining Homes", limitText, true)
        );
    }

    @Override
    protected String getSuccessMessage(String title) {
        return "Home delete embed sent to Discord";
    }
}