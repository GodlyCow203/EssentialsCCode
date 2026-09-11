package net.godlycow.org.essc.plugin.listener;

import net.godlycow.org.essc.EssentialsC;
import net.godlycow.org.essc.modules.punishment.PunishmentManager;
import net.godlycow.org.essc.util.DurationParser;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

public class BanListener implements Listener {

    private final EssentialsC plugin;
    private final PunishmentManager punishmentManager;

    public BanListener(EssentialsC plugin, PunishmentManager punishmentManager) {
        this.plugin = plugin;
        this.punishmentManager = punishmentManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String ip = event.getAddress().getHostAddress();

        if (punishmentManager.isBanned(uuid)) {
            PunishmentManager.BanEntry banEntry = punishmentManager.getBanEntry(uuid);
            Component kickMessage;
            if (banEntry != null) {
                Map<String, String> placeholders = Map.of(
                        "reason", banEntry.reason(),
                        "banner", banEntry.banner(),
                        "duration", formatDuration(banEntry.expires()),
                        "player", banEntry.name(),
                        "date", LocalDateTime.ofInstant(Instant.ofEpochMilli(banEntry.time()), ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        "id", banEntry.id()
                );
                kickMessage = plugin.getLanguageManager().get(null, "ban.screen_message", placeholders);
            } else {
                kickMessage = Component.text("You are banned from this server.");
            }
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickMessage);
            plugin.debug("Prevented banned player " + event.getName() + " from joining");
            return;
        }

        if (punishmentManager.isIpBanned(ip)) {
            PunishmentManager.IpBanEntry ipEntry = punishmentManager.getIpBanEntry(ip);
            Component kickMessage;
            if (ipEntry != null) {
                Map<String, String> placeholders = Map.of(
                        "reason", ipEntry.reason(),
                        "banner", ipEntry.banner(),
                        "duration", formatDuration(ipEntry.expires()),
                        "ip", ip
                );
                kickMessage = plugin.getLanguageManager().get(null, "banip.screen_message", placeholders);
            } else {
                kickMessage = Component.text("Your IP is banned from this server.");
            }
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickMessage);
            plugin.debug("Prevented IP-banned player " + event.getName() + " (" + ip + ") from joining");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (player.getAddress() != null) {
            String ip = player.getAddress().getAddress().getHostAddress();
            if (plugin.getUserManager() != null) {
                plugin.getUserManager().recordIp(player.getUniqueId(), ip);
            }
        }

        if (punishmentManager.isMuted(player.getUniqueId())
                && punishmentManager.hasOfflineMuteNotification(player.getUniqueId())) {
            PunishmentManager.MuteEntry entry = punishmentManager.getMuteEntry(player.getUniqueId());
            if (entry != null) {
                String durationStr = entry.expires() > 0
                        ? DurationParser.formatRemaining(entry.expires())
                        : "Permanent";
                player.sendMessage(plugin.getLanguageManager().get(player, "mute.join_notification", Map.of(
                        "reason", entry.reason(),
                        "muter", entry.muter(),
                        "duration", durationStr
                )));
                punishmentManager.clearOfflineMuteNotification(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player.getAddress() != null) {
            String ip = player.getAddress().getAddress().getHostAddress();
            if (plugin.getUserManager() != null) {
                plugin.getUserManager().recordIp(player.getUniqueId(), ip);
            }
        }
    }

    private String formatDuration(long expires) {
        if (expires <= 0) return "Permanent";
        long remaining = expires - System.currentTimeMillis();
        if (remaining <= 0) return "Expired";
        long days = remaining / (1000 * 60 * 60 * 24);
        long hours = (remaining / (1000 * 60 * 60)) % 24;
        long minutes = (remaining / (1000 * 60)) % 60;
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }
}