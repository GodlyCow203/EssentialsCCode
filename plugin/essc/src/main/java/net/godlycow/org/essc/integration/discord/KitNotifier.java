package net.godlycow.org.essc.integration.discord;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.kit.Kit;

import java.awt.Color;
import java.util.UUID;

public class KitNotifier extends DiscordNotifier {

    public KitNotifier(EssentialsC plugin, DiscordSRVHook hook) {
        super(plugin, hook);
    }

    public void notifyKitClaim(UUID playerUUID, String playerName, Kit kit) {
        if (!plugin.getConfigManager().isDiscordKitEnabled()) {
            return;
        }

        String channelName = plugin.getConfigManager().getDiscordSRVPunishmentsChannelName();
        Color color = parseColor(plugin.getConfigManager().getDiscordKitColor());
        String avatarUrl = getAvatarUrl(playerUUID);

        sendEmbedWithAvatar(channelName, "Kit Claimed", color, avatarUrl,
                new EmbedField("Player", playerName, true),
                new EmbedField("Kit", kit.getDisplayName(), true),
                new EmbedField("Items", String.valueOf(kit.getItems().size()), true),
                new EmbedField("One-Time", kit.isOneTime() ? "Yes" : "No", true)
        );
    }

    @Override
    protected String getSuccessMessage(String title) {
        return "Kit claim embed sent to Discord";
    }
}