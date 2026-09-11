package net.godlycow.org.essc.integration.discord;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.ListenerPriority;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.kit.Kit;
import org.bukkit.Bukkit;

import java.util.UUID;

public class DiscordSRVHook {

    private final EssentialsC plugin;
    private DiscordSRV discordSRV;
    private JDA jda;
    private boolean hooked = false;

    private final BanNotifier banNotifier;
    private final KickNotifier kickNotifier;
    private final MuteNotifier muteNotifier;
    private final KitNotifier kitNotifier;
    private final HomeNotifier homeNotifier;
    private final HomeDeleteNotifier homeDeleteNotifier;
    private final BanIpNotifier banIpNotifier;


    public DiscordSRVHook(EssentialsC plugin) {
        this.plugin = plugin;
        this.banNotifier = new BanNotifier(plugin, this);
        this.kickNotifier = new KickNotifier(plugin, this);
        this.muteNotifier = new MuteNotifier(plugin, this);
        this.kitNotifier = new KitNotifier(plugin, this);
        this.homeNotifier = new HomeNotifier(plugin, this);
        this.homeDeleteNotifier = new HomeDeleteNotifier(plugin, this);
        this.banIpNotifier = new BanIpNotifier(plugin, this);
    }

    public void init() {
        if (!plugin.getConfigManager().isDiscordSRVEnabled()) {
            plugin.debug("DiscordSRV integration disabled in config");
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("DiscordSRV") == null) {
            plugin.debug("DiscordSRV not found, skipping Discord integration.");
            return;
        }

        DiscordSRV.api.subscribe(this);
        plugin.debug("DiscordSRV hook registered, waiting for Discord connection...");
    }

    public void shutdown() {
        if (hooked) {
            DiscordSRV.api.unsubscribe(this);
        }
    }

    @Subscribe(priority = ListenerPriority.NORMAL)
    public void onDiscordReady(DiscordReadyEvent event) {
        this.discordSRV = DiscordSRV.getPlugin();
        this.jda = DiscordSRV.getPlugin().getJda();
        this.hooked = true;
        plugin.debug("DiscordSRV connected successfully! Discord integration active.");

        validateChannels();
    }

    public boolean isHooked() {
        return hooked && jda != null;
    }

    public TextChannel getChannel(String channelName) {
        if (!isHooked()) return null;

        String channelId = discordSRV.getChannels().get(channelName);
        if (channelId == null) {
            plugin.debug("DiscordSRV channel name '" + channelName + "' not found in DiscordSRV config");
            return null;
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            plugin.debug("Discord channel ID '" + channelId + "' not found. Bot may not have access.");
        }

        return channel;
    }

    private void validateChannels() {
        String punishmentsChannel = plugin.getConfigManager().getDiscordSRVPunishmentsChannelName();
        if (getChannel(punishmentsChannel) == null) {
            plugin.getLogger().warning("DiscordSRV channel '" + punishmentsChannel + "' not found! " +
                    "Check your DiscordSRV config.yml Channels section.");
        }
    }

    public void sendBanEmbed(UUID targetUUID, String targetName, String reason, String banner, long expires) {
        if (isHooked()) {
            banNotifier.notifyBan(targetUUID, targetName, reason, banner, expires);
        }
    }

    public void sendKickEmbed(UUID targetUUID, String targetName, String reason, String kicker) {
        if (isHooked()) {
            kickNotifier.notifyKick(targetUUID, targetName, reason, kicker);
        }
    }

    public void sendMuteEmbed(UUID targetUUID, String targetName, String reason, String muter, long expires) {
        if (isHooked()) {
            muteNotifier.notifyMute(targetUUID, targetName, reason, muter, expires);
        }
    }

    public void sendKitClaimEmbed(UUID playerUUID, String playerName, Kit kit) {
        if (isHooked()) {
            kitNotifier.notifyKitClaim(playerUUID, playerName, kit);
        }
    }

    public void sendHomeSetEmbed(UUID playerUUID, String playerName, String homeName, String worldName, int homeCount, int maxHomes) {
        if (isHooked()) {
            homeNotifier.notifyHomeSet(playerUUID, playerName, homeName, worldName, homeCount, maxHomes);
        }
    }

    public void sendHomeDeleteEmbed(UUID playerUUID, String playerName, String homeName, int remainingHomes, int maxHomes) {
        if (isHooked()) {
            homeDeleteNotifier.notifyHomeDelete(playerUUID, playerName, homeName, remainingHomes, maxHomes);
        }
    }

    public void sendBanIpEmbed(String ip, String targetName, String reason, String banner, long expires) {
        if (isHooked()) {
            banIpNotifier.notifyBanIp(ip, targetName, reason, banner, expires);
        }
    }
}