package net.godlycow.org.essc.integration.discord;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import net.godlycow.org.essc.EssentialsC;

import java.awt.Color;
import java.time.Instant;

public abstract class DiscordNotifier {

    protected final EssentialsC plugin;
    protected final DiscordSRVHook hook;

    public DiscordNotifier(EssentialsC plugin, DiscordSRVHook hook) {
        this.plugin = plugin;
        this.hook = hook;
    }

    protected void sendEmbed(String channelName, String title, Color color, EmbedField... fields) {
        TextChannel channel = hook.getChannel(channelName);
        if (channel == null) {
            plugin.debug("Could not resolve Discord channel for: " + channelName);
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setColor(color)
                .setTimestamp(Instant.now());

        for (EmbedField field : fields) {
            embed.addField(field.name(), field.value(), field.inline());
        }

        channel.sendMessageEmbeds(embed.build()).queue(
                success -> plugin.debug(getSuccessMessage(title)),
                error -> plugin.getLogger().warning("Failed to send " + title + " embed: " + error.getMessage())
        );
    }

    protected void sendEmbedWithAvatar(String channelName, String title, Color color, String avatarUrl, EmbedField... fields) {
        TextChannel channel = hook.getChannel(channelName);
        if (channel == null) {
            plugin.debug("Could not resolve Discord channel for: " + channelName);
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setColor(color)
                .setTimestamp(Instant.now());

        for (EmbedField field : fields) {
            embed.addField(field.name(), field.value(), field.inline());
        }

        if (plugin.getConfigManager().isDiscordSRVShowAvatar() && avatarUrl != null) {
            embed.setThumbnail(avatarUrl);
        }

        channel.sendMessageEmbeds(embed.build()).queue(
                success -> plugin.debug(getSuccessMessage(title)),
                error -> plugin.getLogger().warning("Failed to send " + title + " embed: " + error.getMessage())
        );
    }

    protected Color parseColor(String hex) {
        try {
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            return Color.decode("0x" + hex);
        } catch (NumberFormatException e) {
            return Color.GRAY;
        }
    }

    protected String formatDuration(long millis) {
        if (millis <= 0) return "Permanent";

        long days = millis / (1000 * 60 * 60 * 24);
        long hours = (millis / (1000 * 60 * 60)) % 24;
        long minutes = (millis / (1000 * 60)) % 60;

        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    protected String getAvatarUrl(java.util.UUID uuid) {
        return plugin.getConfigManager().getDiscordSRVAvatarUrl()
                .replace("{uuid}", uuid.toString().replace("-", ""));
    }

    protected abstract String getSuccessMessage(String title);

    protected record EmbedField(String name, String value, boolean inline) {}
}